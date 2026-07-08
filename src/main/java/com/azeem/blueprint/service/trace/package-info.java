/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

/**
 *
 *
 * <h3>Provides classes for managing Trace, the Blueprint Bot.</h3>
 *
 * <p>Trace uses Retrieval-Augmented Generation (RAG) to retrieve user uploaded
 * organization-specific context from a vector database and combines it with a Gemini GenAI LLM
 * model to answer user requests. Rather than loading large cloud billing datasets directly into the
 * model, Trace generates and executes constrained SQL queries against the database to retrieve only
 * the data needed for a response.
 *
 * <p>Trace is guided by pre-uploaded "playbooks" stored in Amazon S3. Each playbook defines best
 * practices, instructions, and domain knowledge for a specific task, such as cost optimization,
 * security analysis, or resource recommendations. When handling a request, Trace retrieves the
 * appropriate playbook and follows its guidance to produce accurate, consistent, and
 * organization-specific responses.
 */
package com.azeem.blueprint.service.trace;
