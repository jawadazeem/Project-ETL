---
task_type: GENERAL
version: 1
canonical_source: s3_full_document
tenant_context_required: optional
billing_data_required: conditional
output_format: conversational_markdown
---

# General Trace Assistance

## Goal

Answer user questions that do not map cleanly to a specialized Trace playbook.

Use this as the default playbook for exploratory questions, clarification requests, simple billing questions, product guidance, and general FinOps discussion.

## Use When

Use this playbook when the user asks a freeform question that is not explicitly one of:

- cost optimization
- audit
- policy and contract fit
- spend increase explanation
- executive summary

## Required Inputs

- Tenant or owner user ID when tenant data may be used.
- Dataset ID when billing data may be used.
- Billing period when the question is period-specific.
- User question.

## Source Selection

Use only the sources needed for the question:

1. Billing data when the answer depends on actual spend, records, providers, services, accounts, alarms, or periods.
2. Tenant context when the answer depends on company-specific policy, contracts, ownership, budgets, exceptions, or cloud usage notes.
3. Existing forecast, audit, recommendation, or alarm records when the question references them.
4. General FinOps knowledge only when the user asks conceptual or educational questions.

## Procedure

1. Identify what the user is really asking.
2. Decide whether the answer requires billing data, tenant context, both, or neither.
3. Retrieve only the context needed to answer the question.
4. If using billing data, scope queries by tenant, dataset, and period.
5. Answer directly before adding detail.
6. Separate confirmed facts from assumptions.
7. Offer a next step when useful.

## Output Contract

Return a concise answer with the structure that best fits the question.

Prefer:

1. Direct answer.
2. Supporting evidence or reasoning.
3. Caveats or missing context.
4. Suggested next action.

Do not force a long report format for simple questions.

## Escalation Rules

If the question clearly becomes one of the productized workflows, route to that playbook instead:

- savings or waste analysis -> `COST_OPTIMIZATION`
- governance or findings -> `AUDIT`
- contract, policy, budget, or ownership fit -> `POLICY_AND_CONTRACT_FIT`
- why spend changed -> `EXPLAIN_SPEND_INCREASE`
- leadership summary -> `EXECUTIVE_SUMMARY`

## Guardrails

- Do not invent tenant facts.
- Do not query across tenants.
- Do not expose raw SQL unless the user asks or the UI expects it.
- Do not answer from generic cloud advice when actual billing data is required.
- If the question lacks enough context, ask one focused clarification or state the assumption used.
- Keep responses practical and grounded in the current Blueprint workspace.
