# Policy and Contract Fit Analysis

## Purpose

Use this playbook when the user asks Trace whether cloud spend fits company policy, contract terms, negotiated rates, commitments, budget expectations, approval rules, or organizational ownership expectations.

Trace's goal is to compare actual billing behavior against tenant-specific business context.

## Required Inputs

Trace should resolve or receive:

- Tenant or owner user ID.
- Dataset ID.
- Billing period.
- Optional provider, service, account, team, or resource filter.
- Tenant-specific contracts and policies from S3.
- PostgreSQL billing records for the selected dataset and period.

## Required Context Retrieval

Retrieve tenant knowledge related to:

- Negotiated rates.
- Volume discount tiers.
- Enterprise discount programs.
- Reserved instance, savings plan, or committed-use commitments.
- Team budgets.
- Approval thresholds.
- Tagging and ownership policies.
- Chargeback or cost center rules.
- Expected seasonal exceptions.
- Renewal dates and contract boundaries.

Trace should quote or summarize the relevant tenant policy only when the retrieved context supports it.

## Required Database Evidence

Trace should query PostgreSQL for billing evidence relevant to the policy or contract question.

Useful evidence includes:

- Spend by provider and service.
- Spend by account name.
- Top charged resources.
- Usage fields such as compute hours, storage GB used, and API requests.
- Current period totals.
- Historical comparison, if prior datasets are available.
- Existing alarms tied to the same dataset and period.

SQL must be scoped by tenant ownership, `dataset_id`, and `billing_period`.

## Analysis Procedure

1. Identify the contract or policy rule being evaluated.
2. Retrieve the matching tenant-specific policy or contract context.
3. Query actual billing records for the selected period.
4. Compare actual billing behavior to the documented expectation.
5. Classify each result:
   - `COMPLIANT`
   - `POTENTIAL_ISSUE`
   - `NON_COMPLIANT`
   - `INSUFFICIENT_CONTEXT`
6. Explain the evidence behind the classification.
7. Recommend follow-up actions.

## Common Checks

Apply these checks when supported by tenant knowledge:

- Actual service rate appears above contracted rate.
- Spend exceeds team or account budget threshold.
- Service usage violates approved service policy.
- Resource/account ownership is missing or unclear.
- Spend increase is expected due to documented seasonal pattern.
- Spend increase conflicts with a documented migration or decommissioning plan.
- Discounts, credits, or commitments appear missing.
- Required tagging or cost center mapping is absent from available data.

## Output Contract

Return a structured assessment.

Each assessment should include:

- Policy or contract area.
- Classification.
- Provider, service, account, or team.
- Billing evidence.
- Tenant knowledge evidence.
- Financial impact, if supportable.
- Explanation.
- Recommended next action.
- Missing context.

## Example Answer Shape

```text
Short answer:
S3 spend appears to be a potential contract fit issue for the selected period.

Assessment:
1. S3 contracted rate comparison
   - Classification: POTENTIAL_ISSUE
   - Evidence from billing: S3 storage spend was $12,000 for the period.
   - Evidence from tenant knowledge: contracts.md lists an expected S3 rate of $0.021/GB.
   - Interpretation: Actual spend appears higher than expected for the reported storage usage.
   - Next action: Validate whether the affected storage class is covered by the negotiated rate.

Missing context:
- The billing data does not expose every pricing dimension needed to prove a billing error.
```

## Guardrails

- Do not declare a legal contract breach.
- Use "potential issue" unless the evidence is complete.
- Do not invent contract terms or policy thresholds.
- Do not assume all services are covered by a negotiated rate.
- Do not treat missing data as non-compliance.
- Clearly separate billing evidence from tenant policy evidence.
