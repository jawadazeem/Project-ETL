# Cost Policies and Financial Rules — Azeem Corporation

## Approval Thresholds

| Monthly Cost | Approval Required | Approver |
|---|---|---|
| < $500 | None | Team lead auto-approved |
| $500 - $2,000 | Team lead | Engineering manager |
| $2,000 - $10,000 | Director | VP Engineering |
| $10,000 - $50,000 | VP Engineering | CTO |
| > $50,000 | CTO + CFO | Board notification required |

These thresholds apply to **new resource provisioning**, not existing running costs. Existing resources that drift above a threshold due to usage growth are handled through the anomaly review process (see below).

## Budget Rules

### Hard Limits

- **QA-Testing account:** $8,000/month hard cap. AWS Budget alarm triggers at 80%. At 100%, IAM policies restrict new resource creation. QA environments should be ephemeral — tear down after test runs.
- **Staging-Env account (GCP):** $5,000/month soft cap. No automated enforcement — flagged in weekly review. Staging should mirror production topology but at reduced scale.
- **DR-Recovery account (Azure):** $6,000/month soft cap. DR resources should remain idle except during failover tests (monthly) and actual incidents. Unexpected spikes indicate misconfigured replication.

### Soft Limits

- **Per-team monthly variance:** Any team exceeding their monthly budget by >15% triggers an automatic review request to the FinOps team.
- **Per-provider quarterly variance:** If any single provider's quarterly spend exceeds the prior quarter by >20%, a root cause analysis is required within 5 business days.
- **Per-account monthly variance:** Any individual account (e.g., Production-Main, Analytics-Platform) exceeding its prior-month spend by >25% triggers an alert to the account owner.

## Chargeback Model

- Cloud costs are charged back to teams monthly based on the `cost-center` tag.
- Untagged resources are charged to the team that owns the cloud account (see org-structure.md).
- Shared infrastructure costs (Shared-Services account across all three providers) are split proportionally across all teams by their compute spend ratio.
- Multi-provider accounts (e.g., Customer-Portal spans AWS, GCP, and Azure) are charged to the owning team regardless of provider — Backend Engineering owns all Customer-Portal costs.

## Reserved Instance and Savings Plan Policy

- **Minimum utilization target:** 85% for all reserved instances and committed use discounts.
- **Review cadence:** Monthly, led by Platform Engineering.
- **Underutilized RIs** (below 70% for 2 consecutive months): Must be evaluated for modification, exchange, or sale on the RI Marketplace.
- **New RI purchases** require a 6-month usage history showing consistent demand. No speculative reservations.
- **Current RI coverage:** EC2 fleet (Production-Main, Production-Secondary, Edge-Network, ML-Training), RDS (Production-Main, Finance-Systems, Partner-Integration), ElastiCache (Production-Main, Edge-Network, Marketing-Platform). See contracts.md for expiry dates.
- **GCP CUDs:** 3-year commitment covers Compute Engine across all GCP accounts. Monitor utilization via the GCP CUD utilization report — target >90%.

## Idle Resource Policy

Resources meeting any of the following criteria for 30 consecutive days are flagged for termination:

| Resource Type | Idle Criteria | Common Accounts |
|---|---|---|
| EC2 instances | Average CPU < 2%, no network activity | QA-Testing, ML-Training, Compliance-Audit |
| RDS instances | 0 connections for 30 days | Staging-Env, QA-Testing |
| EBS volumes | Unattached (no instance association) | All AWS accounts |
| ECS services | 0 running tasks | Production-Secondary, Edge-Network |
| EKS node groups | Average CPU < 5% across all nodes | DevOps-Central, Internal-Tools |
| ElastiCache clusters | 0 connections for 30 days | Marketing-Platform, Partner-Integration |
| Elastic IPs | Unassociated (no instance/ENI) | All AWS accounts |
| Load Balancers (ELB) | 0 healthy targets or 0 requests | QA-Testing, Shared-Services |
| Lambda functions | 0 invocations for 30 days | Customer-Portal, Marketing-Platform |
| GKE clusters | Average CPU < 5% across all nodes | Analytics-Platform, IoT-Platform |
| Cloud SQL instances | 0 connections for 30 days | Staging-Env, Finance-Systems |
| Cloud Spanner instances | 0 operations for 30 days | Customer-Portal, Data-Science |
| Dataflow jobs | No data processed for 30 days | Analytics-Platform, Edge-Network |
| Virtual Machines (Azure) | Average CPU < 2%, no network activity | DevOps-Central, Shared-Services |
| AKS node pools | Average CPU < 5% across all nodes | Mobile-Backend, Internal-Tools |
| Cosmos DB collections | 0 operations for 30 days | DR-Recovery, DevOps-Central |

**Process:** Flagged resources are reported in the weekly FinOps digest. Resource owners have 14 days to justify or terminate. After 14 days without response, Platform Engineering may terminate with 48-hour notice.

## Data Transfer Cost Rules

- **Cross-region transfers:** Discouraged. Any architecture requiring sustained cross-region data transfer (>100 GB/month) must be approved by Platform Engineering with a written justification. This applies especially to replication between Production-Main and Production-Secondary.
- **Egress to internet:** Standard egress. Large data exports (>1 TB) should use S3 Transfer Acceleration or CloudFront to reduce per-GB cost.
- **Cross-cloud transfers:** Strongly discouraged. Moving data between AWS and GCP (e.g., Data-Science account syncing between providers) should go through the shared S3 data lake, not direct API-to-API transfers. Azure DR-Recovery replication is exempted from this rule.
- **Partner data exchange:** Partner-Integration account data transfers to external partners should use S3 pre-signed URLs or Cloud Storage signed URLs, not sustained egress streams.

## Anomaly Review Process

1. Blueprint's alarm system detects threshold breaches and anomalies.
2. Alarms with severity HIGH are escalated to the relevant team lead within 4 hours.
3. Team lead investigates and posts a root cause in the FinOps Slack channel within 24 hours.
4. If the spend is justified, the team lead documents why and closes the alarm.
5. If the spend is unjustified, a remediation plan with timeline is required within 48 hours.
6. Unresolved HIGH alarms older than 72 hours are escalated to VP Engineering.

### Known High-Spend Accounts

The following accounts regularly generate high charges and should not trigger unnecessary alarms:

- **Production-Main:** Largest account across all providers. Expect $60,000-$80,000/month.
- **Analytics-Platform:** BigQuery on-demand scanning costs are variable. $15,000-$25,000/month is normal during quarter-end reporting.
- **ML-Training:** GPU instance costs spike during model training cycles (typically 1-2 weeks per month). Monthly range: $5,000-$30,000.
- **Security-Ops:** Microsoft Sentinel ingestion costs scale with log volume. Expect increases during incident investigations.

## Seasonal Adjustments

| Period | Expected Impact | Affected Accounts |
|---|---|---|
| November - December | +25-35% compute spend | Production-Main, Production-Secondary, Customer-Portal, Edge-Network |
| January | -15% (post-holiday normalization) | All accounts |
| March | +10% (fiscal year-end reporting) | Analytics-Platform, Finance-Systems, Data-Science |
| July - August | -10% (reduced transaction volume) | Production-Main, Customer-Portal, Mobile-Backend |
| Monthly (ML cycles) | +50-100% in ML-Training during training weeks | ML-Training |

These seasonal patterns should not trigger alarms. The FinOps team updates alarm thresholds quarterly to account for expected seasonal variation.

## Compliance Requirements

- **SOC 2:** All production cloud resources must have encryption at rest enabled. Unencrypted resources are a compliance violation, not just a cost issue. Applies to Production-Main, Production-Secondary, and Customer-Portal across all providers.
- **PCI DSS:** Payment processing workloads (in Finance-Systems) must run in isolated VPCs with no internet egress. These resources are more expensive due to isolation requirements — this is expected and should not be flagged as a cost anomaly.
- **Audit trail:** All Compliance-Audit account resources (Redshift, S3, EC2) must retain logs for 7 years. Storage costs in this account are expected to grow monotonically — do not flag as anomalous.
- **Data residency:** EU customer data must remain in eu-west-1 (AWS), europe-west1 (GCP), or West Europe (Azure). Cross-region replication for DR is permitted only to paired regions (eu-west-2, europe-west4, North Europe).
- **Microsoft Sentinel:** Security-Ops Azure account runs Microsoft Sentinel for SIEM. Log ingestion costs are compliance-mandated and should not be subject to cost-reduction pressure.
