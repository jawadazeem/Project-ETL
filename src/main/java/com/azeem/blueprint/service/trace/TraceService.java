/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.trace;

import com.azeem.blueprint.exception.core.TraceResponseInvalidException;
import com.azeem.blueprint.model.trace.SqlResponse;
import com.azeem.blueprint.model.trace.TraceResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

// TODO: Completely redesign how this functions. It must use OrgContext and playbooks to operate.
//       Should be delegating tasks to each smaller service, which then retrieves its respective
//        playbook and crafts a prompt for Trace, which this service consumes and sends to his API
// via SDK
@Service
public class TraceService {
  private static final Logger log = LoggerFactory.getLogger(TraceService.class);
  private final ChatModel chatModel;
  private final SchemaService schemaService;
  private final QueryExecutionService queryExecutionService;
  private final SqlValidationService sqlValidationService;
  private final ObjectMapper objectMapper;

  public TraceService(
      @Qualifier("ollamaChatModel") ChatModel chatModel,
      SchemaService schemaService,
      QueryExecutionService queryExecutionService,
      SqlValidationService sqlValidationService,
      ObjectMapper objectMapper) {
    this.chatModel = chatModel;
    this.schemaService = schemaService;
    this.queryExecutionService = queryExecutionService;
    this.sqlValidationService = sqlValidationService;
    this.objectMapper = objectMapper;
  }

  public TraceResponse ask(String promptText, UUID datasetId, String currentPeriod) {

    SqlResponse sqlResponse = generateResponse(promptText, datasetId, currentPeriod);

    if (!sqlValidationService.isValidSql(sqlResponse)) {
      throw new TraceResponseInvalidException("Unsafe SQL detected");
    }

    List<Map<String, Object>> results = queryExecutionService.executeQuery(sqlResponse);

    Prompt explanationPrompt =
        new Prompt(
            List.of(
                new SystemMessage("You are Trace, a cloud cost analyst."),
                new UserMessage("Question: " + promptText),
                new UserMessage("SQL: " + sqlResponse.getSql()),
                new UserMessage("Results: " + results)));

    return new TraceResponse(
        chatModel.call(explanationPrompt).getResult().getOutput().getText(),
        sqlResponse.getSql(),
        sqlResponse.getReasoning());
  }

  /**
   * @param promptText The prompt the user submits to Trace
   * @return SqlResponse object
   */
  private SqlResponse generateResponse(String promptText, UUID datasetId, String currentPeriod) {
    Prompt prompt = createPrompt(promptText, datasetId, currentPeriod);

    ChatResponse response = chatModel.call(prompt);
    String json = response.getResult().getOutput().getText();

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

  private Prompt createPrompt(String promptText, UUID datasetId, String currentPeriod) {
    String schema = schemaService.getSchema();

    return new Prompt(
        List.of(
            new SystemMessage(
                """
                You are a PostgreSQL 16 query generator. Read-only access only.
                Return ONLY valid JSON format: {"sql": "<query>", "reasoning": "<short explanation>"}
                Do not include markdown markdown formatting, comments, or extra text.

                All queries MUST include: WHERE dataset_id = '%s' AND billing_period = '%s'

                Schema:
                %s
                (Note: 'dummy-data' is a valid billing_period for demo purposes.)
                """
                    .formatted(datasetId.toString(), currentPeriod, schema)),
            new UserMessage(promptText)));
  }
}
