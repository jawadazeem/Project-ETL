/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.trace;

import com.azeem.blueprint.model.trace.TraceContext;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

@Service
public class TracePromptBuilder {
  private static final Logger log = LoggerFactory.getLogger(TracePromptBuilder.class);

  public Prompt build(String userPromptText, TraceContext traceContext, double temperature) {
    String systemPrompt =
        """
        You are Trace, Blueprint's cloud billing and FinOps analyst.

        Follow this selected playbook exactly:
        %s

        Your current job is to generate one PostgreSQL 16 read-only SQL query that gathers the data
        needed to answer the user's question. Do not answer the user yet.

        Return ONLY valid JSON:
        {"sql": "<single SELECT query>", "reasoning": "<short explanation>"}

        Hard constraints:
        - Generate exactly one SELECT query.
        - Do not use INSERT, UPDATE, DELETE, DROP, ALTER, CREATE, TRUNCATE, MERGE, CALL, COPY, or comments.
        - Scope the query to the current dataset id: %s.
        - Scope the query to the current owner user id: %s.
        - If the user asks about a billing period, use current billing period: %s.
        - Join datasets when needed to enforce owner_user_id.
        - Prefer aggregation for summaries; do not SELECT * unless the user asks for raw rows.
        - Use LIMIT for row-returning queries.

        Tenant organization context retrieved for this prompt:
        %s

        Relational database schema:
        %s

        Demo note: 'dummy-data' is a valid billing_period for demo purposes.
        """
            .formatted(
                traceContext.playbook(),
                traceContext.datasetId(),
                traceContext.ownerUserId(),
                blankFallback(
                    traceContext.currentBillingPeriod(), "No current period was supplied."),
                blankFallback(traceContext.orgContext(), "No tenant context was retrieved."),
                traceContext.schema());

    Prompt prompt =
        new Prompt(
            List.of(new SystemMessage(systemPrompt), new UserMessage(userPromptText)),
            ChatOptions.builder().temperature(temperature).build());

    log.info("Successfully constructed Trace SQL prompt for dataset {}", traceContext.datasetId());

    return prompt;
  }

  public Prompt buildExplanationPrompt(
      String userPromptText,
      TraceContext traceContext,
      String sql,
      String sqlReasoning,
      Object queryResults,
      double temperature) {
    String systemPrompt =
        """
        You are Trace, Blueprint's cloud billing and FinOps analyst.

        Use the selected playbook, tenant context, SQL reasoning, SQL query, and query results to
        answer the user. Be concise, evidence-backed, and explicit about assumptions.

        Selected playbook:
        %s

        Tenant organization context:
        %s

        SQL reasoning:
        %s

        SQL:
        %s

        Query results:
        %s
        """
            .formatted(
                traceContext.playbook(),
                blankFallback(traceContext.orgContext(), "No tenant context was retrieved."),
                blankFallback(sqlReasoning, "No SQL reasoning was provided."),
                sql,
                queryResults);

    return new Prompt(
        List.of(new SystemMessage(systemPrompt), new UserMessage(userPromptText)),
        ChatOptions.builder().temperature(temperature).build());
  }

  private String blankFallback(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
