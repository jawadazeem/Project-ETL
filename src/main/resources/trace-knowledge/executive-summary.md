# Executive FinOps Summary

## Purpose

Use this playbook when the user asks Trace to generate an executive summary, leadership update, board-ready overview, monthly FinOps summary, or high-level cloud cost narrative.

Trace's goal is to summarize cloud spend clearly for decision-makers while preserving enough evidence for engineering and finance follow-up.

## Required Inputs

Trace should resolve or receive:

- Tenant or owner user ID.
- Dataset ID.
- Billing period.
- Optional audience or tone.
- Tenant-specific knowledge files from S3.
- PostgreSQL billing records for the selected dataset and period.
- Existing alarms, audit findings, recommendations, or forecasts when available.

## Required Context Retrieval

Retrieve tenant knowledge related to:

- Organizational structure.
- Cost center and team ownership.
- Budget policies.
- Contract and discount context.
- Expected seasonal patterns.
- Known migrations or business events.
- Leadership-relevant cost narratives.

Trace should use tenant context to explain why costs changed, not merely that they changed.

## Required Database Evidence

Trace should query PostgreSQL for the selected period and, when available, historical comparison.

Useful evidence includes:

- Total cloud spend.
- Spend by provider.
- Spend by service.
- Highest charge records.
- Largest cost drivers.
- Existing alarms by severity.
- Forecasted spend from the prediction service, if available.
- Audit findings or optimization recommendations, if available.

SQL must be scoped by tenant ownership, `dataset_id`, and `billing_period`.

## Analysis Procedure

1. Establish total spend for the selected period.
2. Identify the top cost drivers.
3. Explain provider and service-level changes.
4. Bring in tenant context: contracts, budgets, ownership, known events, and seasonal expectations.
5. Highlight risks, anomalies, or audit findings.
6. Highlight optimization opportunities.
7. Summarize recommended leadership decisions or follow-ups.

## Output Contract

Return a concise executive summary with the following sections:

1. Executive takeaway.
2. Spend overview.
3. Primary cost drivers.
4. Risks and exceptions.
5. Optimization opportunities.
6. Recommended actions.
7. Data gaps or assumptions.

Use numbers where available. Avoid vague language such as "significant" unless paired with an amount, percentage, or ranking.

## Example Answer Shape

```text
Executive takeaway:
Cloud spend for the selected period was $450,000, led by AWS storage and GCP compute. The strongest action item is reviewing S3 lifecycle policy for Customer Portal.

Spend overview:
- AWS: $280,000
- GCP: $120,000
- Azure: $50,000

Primary cost drivers:
1. AWS S3 storage in Customer Portal.
2. GCP compute growth in ML Training.
3. API request growth in Partner Integration.

Risks and exceptions:
- S3 spend may be above the tenant's documented contracted rate.
- ML Training growth may be expected if tenant knowledge confirms an active training cycle.

Recommended actions:
1. Review S3 lifecycle policy.
2. Validate whether GCP compute growth is tied to approved ML workloads.
3. Ask Trace to run a cost optimization analysis.
```

## Guardrails

- Do not overstate certainty.
- Do not hide missing context.
- Do not invent business events, budgets, or owners.
- Do not include raw SQL unless the user asks for it.
- Do not produce a long technical report unless the user asks for detail.
- Keep leadership summaries crisp, but evidence-backed.
