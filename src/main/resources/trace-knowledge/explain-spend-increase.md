---
task_type: EXPLAIN_SPEND_INCREASE
version: 1
canonical_source: s3_full_document
tenant_context_required: true
billing_data_required: true
output_format: variance_analysis
---

# Explain Spend Increase

## Goal

Explain why cloud spend increased for a selected period, provider, service, account, team, or resource.

## Use When

The user asks why spend went up, what changed, which cost drivers increased, whether growth was expected, or who owns the increase.

## Required Inputs

- Tenant or owner user ID.
- Dataset ID.
- Current billing period.
- Comparison period when available.
- Optional provider, service, account, team, or resource filter.

## Required Sources

1. Billing data: current spend, comparison spend, deltas by provider/service/account/resource, usage fields.
2. Tenant context: ownership, budgets, expected launches, migrations, seasonal events, approved scaling, contract changes.
3. Supporting data: alarms, forecasts, audits, and recommendations when available.

## Procedure

1. Establish the current spend and comparison baseline.
2. Calculate absolute and percentage change.
3. Decompose the increase by provider, service, account, and resource where possible.
4. Check whether usage metrics explain the charge increase.
5. Retrieve tenant context for known events, ownership, policies, and exceptions.
6. Classify drivers:
   - expected business activity
   - usage growth
   - pricing or discount issue
   - policy or ownership issue
   - anomaly requiring investigation
   - insufficient context
7. Identify the most likely explanation and confidence level.

## Output Contract

Return:

1. Short answer.
2. Main drivers.
3. Billing evidence.
4. Tenant context evidence.
5. Likely explanation.
6. Recommended next actions.
7. Missing context.

Each driver must include:

- provider/service/account/resource when known
- spend delta
- percentage change when supportable
- evidence
- owner or team if known
- explanation category
- confidence

## Guardrails

- Do not infer causality from correlation alone.
- Do not invent business events or owners.
- If no comparison period is available, say the answer is current-period driver analysis, not true variance.
- Separate confirmed causes from plausible explanations.
