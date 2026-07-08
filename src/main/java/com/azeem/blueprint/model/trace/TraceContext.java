/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.model.trace;

import java.util.UUID;

/**
 * Created from context aggregated in {@code ContextAggregatorService} and used in {@code
 * TracePromptBuilder} to build a prompt.
 */
public record TraceContext(
    String playbook,
    UUID datasetId,
    String currentBillingPeriod,
    UUID ownerUserId,
    String orgContext,
    String schema) {}
