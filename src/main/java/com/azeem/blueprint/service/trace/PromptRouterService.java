/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.trace;

public class PromptRouterService {
  // TODO: Need to ensure the prompt the user is sending is a valid, billing data related
  //  prompt. If not, we must return an error. This is far less computationally damaging than
  //  sending all of the information needed for a correct prompt over to the LLM.
  //  This will deter users from asking personal questions.
  //   This should be called from the AiExecutionGateway class, which is the overall orchestrator

  // TODO: This will also decide dynamically which playbook the next agent should be given.
  // Hardcoded prompts are
  //  too backward. It's not 2005 anymore.
}
