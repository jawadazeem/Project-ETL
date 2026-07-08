/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.trace;

import com.azeem.blueprint.model.trace.PromptRoute;
import com.azeem.blueprint.model.trace.TaskType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

@Service
public class PromptRouterService {
  public static final String CANNOT_RESPOND_MESSAGE =
      "Cannot Respond. Trace is only made for cloud billing, FinOps, cost optimization, audit, policy and contract fit, spend analysis, and Blueprint dataset questions.";

  private static final Logger log = LoggerFactory.getLogger(PromptRouterService.class);

  private final ChatModel chatModel;
  private final ObjectMapper objectMapper;

  public PromptRouterService(ChatModel chatModel, ObjectMapper objectMapper) {
    this.chatModel = chatModel;
    this.objectMapper = objectMapper;
  }

  public PromptRoute route(String promptText) {
    if (promptText == null || promptText.isBlank()) {
      return PromptRoute.rejected("Prompt was blank.");
    }

    Prompt routerPrompt =
        new Prompt(
            List.of(
                new SystemMessage(
                    """
                    You are Trace's prompt router.
                    Your only job is to decide whether a user prompt belongs to Blueprint's cloud billing and FinOps domain.

                    Allowed domain:
                    - cloud billing data questions
                    - FinOps analysis
                    - cost optimization
                    - audits and governance findings
                    - policy, contract, budget, ownership, and tagging fit
                    - spend increases or cost driver explanations
                    - executive summaries of cloud spend
                    - general questions about Blueprint's billing dataset, reports, forecasts, alarms, or Trace

                    If the prompt is unrelated, personal, entertainment, coding unrelated to Blueprint, medical, legal, politics, or general knowledge, reject it.

                    Return ONLY valid JSON:
                    {"canRespond": true, "taskType": "GENERAL", "reason": "short reason"}

                    Allowed taskType values:
                    COST_OPTIMIZATION
                    AUDIT
                    POLICY_AND_CONTRACT_FIT
                    EXPLAIN_SPEND_INCREASE
                    EXECUTIVE_SUMMARY
                    GENERAL

                    Routing rules:
                    - savings, waste, rightsizing, optimization -> COST_OPTIMIZATION
                    - audit, governance finding, control, violation -> AUDIT
                    - contract, policy, budget, ownership, tagging fit -> POLICY_AND_CONTRACT_FIT
                    - why spend rose, variance, cost growth -> EXPLAIN_SPEND_INCREASE
                    - leadership summary, executive update, board summary -> EXECUTIVE_SUMMARY
                    - other valid cloud billing or Blueprint questions -> GENERAL

                    If rejected, return:
                    {"canRespond": false, "taskType": "GENERAL", "reason": "outside Trace domain"}
                    """),
                new UserMessage(promptText)),
            ChatOptions.builder().temperature(0.0).build());

    String raw = chatModel.call(routerPrompt).getResult().getOutput().getText();
    if (raw == null || raw.isBlank()) {
      log.warn("Prompt router returned an empty response; rejecting prompt.");
      return PromptRoute.rejected("Router returned no decision.");
    }

    try {
      RouteDecision decision = objectMapper.readValue(cleanJson(raw), RouteDecision.class);
      if (!decision.canRespond()) {
        return PromptRoute.rejected(decision.reason());
      }
      TaskType taskType = decision.taskType() == null ? TaskType.GENERAL : decision.taskType();
      return PromptRoute.accepted(taskType, decision.reason());
    } catch (JsonProcessingException | IllegalArgumentException e) {
      log.warn("Prompt router returned an invalid routing decision: {}", raw, e);
      return PromptRoute.rejected("Router returned an invalid decision.");
    }
  }

  private String cleanJson(String rawJson) {
    return rawJson.replaceAll("```json", "").replaceAll("```", "").trim();
  }

  private record RouteDecision(boolean canRespond, TaskType taskType, String reason) {}
}
