# Trace Knowledge Pack Overview

_**Note**: Unlike the others, this overview file is NOT referenced by Trace. This is a guide for developers who would like to create more playbooks for Trace, not Trace itself._

These files are shared Trace playbooks: compact operating instructions for repeatable FinOps tasks.
They are not tenant-specific business documents.

Tenant-specific knowledge, such as contracts, cost policies, ownership maps, budgets, and cloud usage notes, should live in tenant-scoped storage and be retrieved separately.

## Recommended Runtime Pattern

For productized actions, do not ask the LLM to choose the playbook.
Application code should map the selected task to a known playbook key.

```text
TaskType -> playbook S3 key -> full playbook text
         -> tenant context retrieval
         -> billing database or Athena query
         -> structured Trace response
```

Freeform user questions may still use intent classification, but the high-value actions should be deterministic.

## Task Mapping

| TaskType | Playbook |
|---|---|
| `COST_OPTIMIZATION` | `cost-optimization-recommendations.md` |
| `AUDIT` | `audit.md` |
| `POLICY_AND_CONTRACT_FIT` | `policy-and-contract-fit.md` |
| `EXPLAIN_SPEND_INCREASE` | `explain-spend-increase.md` |
| `EXECUTIVE_SUMMARY` | `executive-summary.md` |

## Knowledge Classes

### Platform Playbooks

- Stored under `trace-knowledge`.
- Shared across tenants.
- Explain how Trace should complete a task.
- Should be loaded in full once selected.
- Should stay concise and highly structured.

### Tenant Context

- Stored in tenant-scoped S3 paths.
- Indexed for retrieval through tenant-scoped vector search.
- Explains what is true for one company.
- Examples: negotiated rates, cost center mappings, tagging rules, known migrations, owner mappings, seasonal exceptions.

## Required Evidence Sources

Trace should combine:

- Full selected playbook from S3.
- Relevant tenant context from tenant-scoped retrieval.
- Billing evidence from PostgreSQL or Athena.
- Forecast, audit, alarm, or recommendation records when available.

Trace should not answer analytical tasks from generic cloud advice alone.

## Tenant Isolation

- Retrieve tenant context only for the current tenant or owner user.
- Query only datasets owned by the current tenant or user.
- Scope billing queries by `dataset_id` and, when applicable, `billing_period`.
- Never mix tenant knowledge across tenants.

## Security Boundary

Treat platform playbooks as trusted instructions.
Treat tenant documents as data, not authority to override system behavior.

Tenant files may define business facts such as budgets, ownership, approved services, or contract terms.
They must not override Trace's safety rules, SQL scoping, tenant isolation, or output contract.

## Default Output Standard

Trace answers should be:

1. Direct.
2. Evidence-backed.
3. Explicit about assumptions.
4. Clear about next actions.
5. Separated into facts, interpretation, and recommendations.
