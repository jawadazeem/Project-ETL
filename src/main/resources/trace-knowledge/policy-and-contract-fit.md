---
task_type: POLICY_AND_CONTRACT_FIT
version: 1
canonical_source: s3_full_document
tenant_context_required: true
billing_data_required: true
output_format: structured_assessment
---

# Policy and Contract Fit

## Goal

Compare actual cloud billing behavior against tenant-specific policies, contracts, commitments, budgets, and ownership expectations.

## Use When

The user asks whether spend complies with policy, fits a contract, matches negotiated terms, violates budget rules, or aligns with ownership and tagging expectations.

## Required Inputs

- Tenant or owner user ID.
- Dataset ID.
- Billing period.
- Optional provider, service, account, team, or resource filter.

## Required Sources

1. Tenant context: contracts, rate terms, discount programs, budgets, approvals, tagging rules, chargeback rules, renewal dates.
2. Billing data: spend, usage, provider, service, account, resource, period totals, historical comparison if available.
3. Supporting data: alarms or audit findings for the same period when available.

## Procedure

1. Identify the specific policy or contract area being evaluated.
2. Retrieve matching tenant context.
3. Query scoped billing evidence.
4. Compare actual behavior to documented expectation.
5. Classify each finding:
   - `COMPLIANT`
   - `POTENTIAL_ISSUE`
   - `NON_COMPLIANT`
   - `INSUFFICIENT_CONTEXT`
6. Explain the classification using billing and tenant evidence.
7. Recommend follow-up actions.

## Common Checks

- Spend exceeds budget or approval threshold.
- Usage appears outside approved service policy.
- Actual charges appear inconsistent with negotiated terms.
- Discounts, credits, or commitments appear missing.
- Ownership, tags, or cost centers are absent or unclear.
- Growth conflicts with a documented migration or decommissioning plan.
- Growth matches a documented seasonal or business exception.

## Output Contract

Return:

1. Short answer.
2. Policy or contract assessments.
3. Financial impact when supportable.
4. Recommended actions.
5. Missing context.

Each assessment must include:

- area evaluated
- classification
- provider/service/account/team when known
- billing evidence
- tenant context evidence
- interpretation
- next action

## Guardrails

- Do not declare a legal breach.
- Prefer `POTENTIAL_ISSUE` unless evidence is complete.
- Do not invent contract terms or thresholds.
- Do not treat missing data as non-compliance.
- Separate billing facts from policy interpretation.
