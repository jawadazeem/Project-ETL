/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.trace;

import com.azeem.blueprint.exception.core.TraceResponseInvalidException;
import com.azeem.blueprint.model.trace.PromptRoute;
import com.azeem.blueprint.model.trace.SqlResponse;
import com.azeem.blueprint.model.trace.TraceContext;
import com.azeem.blueprint.model.trace.TraceResponse;
import com.azeem.blueprint.service.trace.sql.QueryExecutionService;
import com.azeem.blueprint.service.trace.sql.SqlValidationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

/**
 * This class is the orchestrator of validating and sending a prompt, as well as validating and
 * retrieving the result to Trace, the LLM
 */
@Service
public class AiExecutionGateway {
  private static final Logger log = LoggerFactory.getLogger(AiExecutionGateway.class);
  private final ChatModel chatModel;
  private final PromptRouterService promptRouterService;
  private final ContextAggregatorService contextAggregatorService;
  private final TracePromptBuilder promptBuilder;
  private final QueryExecutionService queryExecutionService;
  private final SqlValidationService sqlValidationService;
  private final ObjectMapper objectMapper;

  public AiExecutionGateway(
      ChatModel chatModel,
      PromptRouterService promptRouterService,
      ContextAggregatorService contextAggregatorService,
      TracePromptBuilder tracePromptBuilder,
      QueryExecutionService queryExecutionService,
      SqlValidationService sqlValidationService,
      ObjectMapper objectMapper) {
    this.chatModel = chatModel;
    this.promptRouterService = promptRouterService;
    this.contextAggregatorService = contextAggregatorService;
    this.promptBuilder = tracePromptBuilder;
    this.queryExecutionService = queryExecutionService;
    this.sqlValidationService = sqlValidationService;
    this.objectMapper = objectMapper;
  }

  public TraceResponse ask(String promptText, UUID currentDatasetId, String currentPeriod) {
    PromptRoute route = promptRouterService.route(promptText);
    if (!route.canRespond()) {
      log.info("Trace rejected prompt. Reason: {}", route.reason());
      return new TraceResponse(PromptRouterService.CANNOT_RESPOND_MESSAGE, null, route.reason());
    }

    TraceContext traceContext =
        contextAggregatorService.getContext(
            promptText, currentDatasetId, currentPeriod, route.taskType());

    SqlResponse sqlResponse = generateResponse(promptText, traceContext, 0.3);

    // If the SQL is unsafe, try 1 more time with lower temperature before giving up
    if (!sqlValidationService.isValidSql(sqlResponse)) {
      log.error(
          "Unsafe SQL detected in the first pass. Trying once again with a lower temperature.");
      SqlResponse secondSqlResponse = generateResponse(promptText, traceContext, 0.0);
      if (!sqlValidationService.isValidSql(secondSqlResponse)) {
        throw new TraceResponseInvalidException("Unsafe SQL detected the second time.");
      }
      sqlResponse = secondSqlResponse;
    }

    List<Map<String, Object>> results = queryExecutionService.executeQuery(sqlResponse);

    /*
     * A highly structured, customized, prompt is sent to the LLM once again. This time it is to
     * generate an explanation of the results.
     */
    Prompt explanationPrompt =
        promptBuilder.buildExplanationPrompt(
            promptText,
            traceContext,
            sqlResponse.getSql(),
            sqlResponse.getReasoning(),
            results,
            0.2);

    return new TraceResponse(
        chatModel.call(explanationPrompt).getResult().getOutput().getText(),
        sqlResponse.getSql(),
        sqlResponse.getReasoning());
  }

  /**
   * @param promptText The prompt the user submits to Trace
   * @return SqlResponse object
   */
  private SqlResponse generateResponse(
      String promptText, TraceContext traceContext, double temperature) {
    Prompt prompt = promptBuilder.build(promptText, traceContext, temperature);

    ChatResponse response = chatModel.call(prompt);
    String rawJson = response.getResult().getOutput().getText();
    if (rawJson == null) {
      throw new TraceResponseInvalidException("The JSON String that Trace produced was empty.");
    }

    // Clean JSON of any backticks. LLMs notoriously wrap JSON in backticks.
    String json =
        rawJson
            .replaceAll("```json", "")
            .replaceAll("```", "")
            .replaceAll("^'|'$", "") // Strips leading/trailing single quotes if it spit out ''...''
            .trim();

    SqlResponse sqlResponse;

    try {
      sqlResponse = objectMapper.readValue(json, SqlResponse.class);
    } catch (JsonProcessingException e) {
      log.error(
          "There was a data format error with the Trace's response. Here was "
              + "Trace's response: {}",
          json);
      throw new TraceResponseInvalidException(
          "There was a data format error with the Trace's response.");
    }

    log.info("Trace generated: {}", sqlResponse);
    return sqlResponse;
  }
}
