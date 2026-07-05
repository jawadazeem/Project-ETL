---
task_type: COST_OPTIMIZATION
version: 1
canonical_source: s3_full_document
tenant_context_required: true
billing_data_required: true
output_format: structured_markdown
---

# Cost Optimization Recommendations

## Goal

Produce prioritized, evidence-backed cloud cost optimization recommendations.

## Use When

The user asks to reduce spend, find waste, identify savings opportunities, rank optimizations, or run a FinOps cost optimization analysis.

## Required Inputs

- Tenant or owner user ID.
- Dataset ID.
- Billing period.
- Optional provider, service, account, team, or resource filter.

## Required Sources

1. Tenant context: contracts, cost policies, ownership maps, approved services, commitments, seasonal exceptions.
2. Billing data: spend by provider, service, account, resource, usage fields, current period, historical periods if available.
3. Supporting data: alarms, forecasts, audit findings, prior recommendations when available.

## Procedure

1. Identify the largest cost drivers for the selected period.
2. Retrieve tenant context relevant to those drivers.
3. Separate expected spend from suspicious or optimizable spend.
4. Detect candidate categories:
   - rightsizing
   - idle or underused resource
   - storage lifecycle
   - discount or commitment mismatch
   - unowned or poorly tagged spend
   - provider or service consolidation
   - unexpected growth
   - policy violation
5. Estimate savings only when evidence supports it.
6. Rank by impact, confidence, and implementation effort.
7. Assign owner or team only when tenant context supports it.

## Output Contract

Return:

1. Short answer.
2. Ranked recommendations.
3. Evidence summary.
4. Next actions.
5. Gaps and assumptions.

Each recommendation must include:

- title
- category
- provider/service/account/resource when known
- estimated savings when supportable
- confidence: `LOW`, `MEDIUM`, or `HIGH`
- billing evidence
- tenant context evidence
- recommended next action

## Guardrails

- Do not claim guaranteed savings.
- Do not recommend deletion without human review.
- Do not invent owners, contract rates, budgets, or thresholds.
- Do not treat forecasts as facts.
- If utilization data is missing, label rightsizing as preliminary.
