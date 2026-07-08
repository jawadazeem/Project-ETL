---
task_type: EXECUTIVE_SUMMARY
version: 1
canonical_source: s3_full_document
tenant_context_required: true
billing_data_required: true
output_format: executive_markdown
---

# Executive Summary

## Goal

Create a concise leadership-ready FinOps summary that explains spend, drivers, risks, and decisions needed.

## Use When

The user asks for an executive summary, leadership update, board-ready overview, monthly FinOps summary, or high-level cost narrative.

## Required Inputs

- Tenant or owner user ID.
- Dataset ID.
- Billing period.
- Optional audience or tone.

## Required Sources

1. Tenant context: org structure, budgets, ownership, contracts, known migrations, business events, seasonal patterns.
2. Billing data: total spend, provider/service/account breakdowns, highest charges, historical comparison when available.
3. Supporting data: alarms, audits, forecasts, recommendations when available.

## Procedure

1. Establish total spend for the period.
2. Identify the top drivers and material changes.
3. Use tenant context to explain why changes may have happened.
4. Highlight risks, exceptions, policy concerns, or audit findings.
5. Highlight the strongest optimization opportunities.
6. Recommend leadership decisions or follow-ups.

## Output Contract

Return exactly these sections:

1. Executive takeaway.
2. Spend overview.
3. Primary cost drivers.
4. Risks and exceptions.
5. Optimization opportunities.
6. Recommended actions.
7. Data gaps or assumptions.

## Style Rules

- Keep it concise.
- Use numbers where available.
- Avoid raw SQL.
- Avoid vague terms like "significant" unless paired with amount, percentage, or ranking.
- Make the recommended actions decision-oriented.

## Guardrails

- Do not invent business events, owners, budgets, or contract terms.
- Do not hide missing context.
- Do not overstate certainty.
- Do not turn the response into a technical report unless asked.
