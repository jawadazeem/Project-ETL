/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.trace;

import com.azeem.blueprint.exception.core.TraceResponseInvalidException;
import com.azeem.blueprint.model.trace.SqlResponse;
import com.azeem.blueprint.model.trace.TraceResponse;
import com.azeem.blueprint.service.dataset.DatasetService;
import com.azeem.blueprint.service.trace.sql.QueryExecutionService;
import com.azeem.blueprint.service.trace.sql.SchemaService;
import com.azeem.blueprint.service.trace.sql.SqlValidationService;
import com.azeem.blueprint.service.trace.task.TaskQueryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * This class is the orchestrator of validating and sending a prompt, as well as validating and
 * retrieving the result to Trace, the LLM
 */
@Service
public class AiExecutionGateway {
  private static final Logger log = LoggerFactory.getLogger(AiExecutionGateway.class);
  private final ChatModel chatModel;
  private final SchemaService schemaService;
  private final OrgKnowledgeRetrievalService orgKnowledgeRetrievalService;
  private final DatasetService datasetService;
  private final TaskQueryService taskQueryService;
  private final QueryExecutionService queryExecutionService;
  private final SqlValidationService sqlValidationService;
  private final ObjectMapper objectMapper;

  public AiExecutionGateway(
      @Qualifier("ollamaChatModel") ChatModel chatModel,
      SchemaService schemaService,
      OrgKnowledgeRetrievalService orgKnowledgeRetrievalService,
      DatasetService datasetService,
      TaskQueryService taskQueryService,
      QueryExecutionService queryExecutionService,
      SqlValidationService sqlValidationService,
      ObjectMapper objectMapper) {
    this.chatModel = chatModel;
    this.schemaService = schemaService;
    this.taskQueryService = taskQueryService;
    this.datasetService = datasetService;
    this.orgKnowledgeRetrievalService = orgKnowledgeRetrievalService;
    this.queryExecutionService = queryExecutionService;
    this.sqlValidationService = sqlValidationService;
    this.objectMapper = objectMapper;
  }

  public TraceResponse ask(String promptText, UUID currentDatasetId, String currentPeriod) {

    SqlResponse sqlResponse = generateResponse(promptText, currentDatasetId, currentPeriod, 0.3);

    // If the SQL is unsafe, try 1 more time with lower temperature before giving up
    if (!sqlValidationService.isValidSql(sqlResponse)) {
      log.error(
          "Unsafe SQL detected in the first pass. Trying once again with a lower temperature.");
      SqlResponse secondSqlResponse =
          generateResponse(promptText, currentDatasetId, currentPeriod, 0.0);
      if (!sqlValidationService.isValidSql(secondSqlResponse)) {
        throw new TraceResponseInvalidException("Unsafe SQL detected the second time.");
      }
    }

    List<Map<String, Object>> results = queryExecutionService.executeQuery(sqlResponse);

    /*
     * A highly structured, customized, prompt is sent to the LLM once again. This time it is to
     * generate an explanation of the results.
     */
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
  private SqlResponse generateResponse(
      String promptText, UUID datasetId, String currentPeriod, double temperature) {
    Prompt prompt = createPrompt(promptText, datasetId, currentPeriod, temperature);

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

  /*
   * Handling unrecognized prompts by catching an exception isn't the best way to go about it,
   * since these prompts will be fairly common. Even though the most commonly asked prompts are covered,
   * there should be another agent that determines if a given prompt is semantically and/or logically
   * similar to one of the hardcoded ones.
   */
  private Prompt createPrompt(
      String promptText, UUID datasetId, String currentPeriod, double temperature) {
    String schema = schemaService.getSchema();

    String playbook = taskQueryService.getPlaybook(promptText);

    UUID ownerUserId = datasetService.getOwnerId(datasetId);
    List<Document> docs = orgKnowledgeRetrievalService.retrieveFilteredDocuments(ownerUserId);

    String orgContext = docs.stream().map(Document::toString).collect(Collectors.joining("\n\n"));

    return new Prompt(
        List.of(
            new SystemMessage(
                """
                Follow these instructions:
                %s

                As a part of your job, you are to generate one or more PostgreSQL 16 queries. Read-only access only.
                Return ONLY valid JSON format: {"sql": "<query>", "reasoning": "<short explanation>"}
                Do not include markdown markdown formatting, comments, or extra text.

                The current dataset is %s, and the current period is %s. These are subject to increase
                before the next bill cycle.

                To restrict queries to a specific user, ALL queries on the dataset table MUST include:
                SELECT br.* FROM billing_records br
                JOIN datasets d ON br.dataset_id = d.id
                WHERE d.owner_user_id = %s;

                And on the Alarms table:
                SELECT a.* FROM alarms a
                JOIN datasets d ON a.dataset_id = d.id
                WHERE d.owner_user_id = %s;

                Tenant's Organization Context:
                %s

                Schema:
                %s
                (Note: 'dummy-data' is a valid billing_period for demo purposes.)
                """
                    .formatted(
                        playbook,
                        datasetId.toString(),
                        currentPeriod,
                        ownerUserId,
                        ownerUserId,
                        orgContext,
                        schema)),
            new UserMessage(promptText)),
        GoogleGenAiChatOptions.builder().temperature(temperature).build());
  }
}
