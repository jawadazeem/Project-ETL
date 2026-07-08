---
task_type: AUDIT
version: 1
canonical_source: s3_full_document
tenant_context_required: true
billing_data_required: true
output_format: audit_findings
---

# Audit

## Goal

Identify durable FinOps audit findings from billing data, tenant policy, tenant contracts, ownership expectations, and known operational context.

## Use When

The user asks to run an audit, find issues, check governance, review risk, or produce findings for Trace to explain later.

## Required Inputs

- Tenant or owner user ID.
- Dataset ID.
- Billing period.
- Optional audit scope: provider, service, account, team, policy area.

## Required Sources

1. Tenant context: policies, budgets, contracts, tagging rules, ownership maps, approved services, exceptions.
2. Billing data: spend, usage, provider, service, account, resource, period totals, historical comparison when available.
3. Existing records: alarms, prior audit findings, forecasts, and recommendations when available.

## Procedure

1. Determine the audit scope.
2. Retrieve tenant policy and contract context relevant to the scope.
3. Query scoped billing evidence.
4. Check for:
   - policy violations
   - contract fit issues
   - unexplained spend increases
   - missing ownership or tagging
   - anomalous high charges
   - commitment or discount mismatch
   - repeated alarms or unresolved findings
5. Classify severity: `LOW`, `MEDIUM`, `HIGH`.
6. Assign confidence: `LOW`, `MEDIUM`, `HIGH`.
7. Produce findings that can be stored and explained later.

## Output Contract

Return:

1. Audit summary.
2. Findings.
3. Evidence.
4. Recommended remediation.
5. Follow-up questions.

Each finding must include:

- title
- severity
- confidence
- affected provider/service/account/resource
- billing evidence
- tenant context evidence
- risk explanation
- recommended owner if known
- remediation step

## Guardrails

- Do not present uncertain findings as confirmed violations.
- Do not invent policy, contract, ownership, or budget data.
- Do not create legal conclusions.
- Do not mix tenants.
- If evidence is incomplete, classify the finding as `LOW` confidence or `INSUFFICIENT_CONTEXT`.
