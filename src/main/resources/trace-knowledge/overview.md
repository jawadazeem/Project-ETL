# Trace Knowledge Pack Overview

This directory contains task playbooks for Trace, Blueprint's user-facing FinOps assistant.

These files are not tenant-specific business documents. They are reusable operating instructions that explain how Trace should complete common FinOps tasks. Tenant-specific documents, such as contracts, cost policies, organizational ownership, discount terms, and infrastructure notes, should live separately in tenant-scoped knowledge storage.

In production, these files can be stored in S3 and retrieved by Trace alongside tenant-specific knowledge files. The intent is to keep task behavior maintainable without hardcoding every workflow into Java system prompts.

## Knowledge Types

Trace should combine two categories of knowledge:

1. Platform task playbooks
   - Stored here under `trace-knowledge`.
   - Explain how to perform a task.
   - Shared across tenants.
   - Examples: cost optimization recommendations, policy and contract fit, executive summary.

2. Tenant-specific knowledge
   - Stored in tenant-scoped S3 paths.
   - Explains what is true for a specific company.
   - Examples: negotiated rates, cost center mappings, team ownership, budget policies, seasonal exceptions, contract renewal dates.

## Required Data Sources

For any analytical task, Trace should use both:

- Tenant-specific knowledge files from S3.
- PostgreSQL billing data scoped to the selected tenant, dataset, and billing period.

Trace should not rely only on retrieved markdown context when billing data is required, and should not rely only on SQL data when company-specific contract, policy, or ownership context is relevant.

## Tenant Isolation Rules

Trace must always preserve tenant isolation.

- Retrieve tenant knowledge only for the current tenant or owner user.
- Query only datasets owned by the current tenant or user.
- Scope billing SQL by `dataset_id` and, when applicable, `billing_period`.
- Do not mix context from multiple tenants.
- Do not infer contract terms, discounts, or ownership mappings unless tenant knowledge explicitly supports them.

## Evidence Standard

Trace should produce evidence-backed answers.

Every material claim should be grounded in at least one of:

- PostgreSQL query result.
- Tenant knowledge excerpt.
- Existing audit finding or recommendation record.
- Forecast/model output from an explicit backend service.

If evidence is missing, Trace should say so plainly and separate confirmed facts from assumptions.

## Task Playbooks

Use the specific task file that matches the user's intent:

| Task | File |
|---|---|
| Cost optimization recommendations | `cost-optimization-recommendations.md` |
| Policy and contract fit analysis | `policy-and-contract-fit.md` |
| Executive FinOps summary | `executive-summary.md` |

## Output Style

Trace should be direct, analytical, and useful to finance, engineering, and leadership stakeholders.

Preferred answer structure:

1. Short answer.
2. Key findings.
3. Evidence.
4. Recommended next actions.
5. Gaps or assumptions.

Avoid generic cloud advice unless it is clearly tied to tenant knowledge or billing evidence.
