# FinOps Zombie Resource Detector

A self-governing system that detects idle ("zombie") cloud resources, holds them through a review grace period, and stops them automatically — while treating production infrastructure as a special case that always requires human sign-off.

Built to model how real FinOps tools (AWS Trusted Advisor, Vantage, CloudHealth) approach cloud cost governance: **visibility → policy-based detection → safe, reversible remediation.**

## Why this exists

Cloud providers bill for what's *provisioned*, not what's *used*. Forgotten staging servers, abandoned load-test clusters, and idle storage volumes are one of the largest sources of avoidable cloud spend — commonly cited at 30-40% of total spend. This project simulates a small fleet of cloud nodes and builds the detection/remediation loop that real FinOps platforms use to find and eliminate that waste, safely.

## What it does

- Simulates a 20-node cloud fleet with realistic, rotating usage patterns (some nodes are idle-natured, some active, and which nodes fall into which group changes over time).
- Continuously evaluates each node against **persisted time-series metrics** (not a single cached reading) to decide if it's genuinely idle.
- Flags idle nodes, holds them through a **grace period** before acting — giving a human a window to intervene.
- Requires **several consecutive clean readings** before un-flagging a recovering node, preventing flapping on a single noisy metric.
- **Never auto-stops production nodes** — they're flagged and held for manual review instead, with an escalating alert if the hold drags on.
- Provides a **manual override** endpoint so a human can always take back control of any node.
- Logs every state change to a real, queryable **audit trail** (actor, action, target, outcome).
- Raises **alerts** for actionable conditions (a stuck PROD review, a successful cost-saving stop) with owner attribution.
- Exposes a **global pause switch** so the whole automation loop can be halted instantly.
- All detection thresholds are **DB-backed policy**, editable at runtime — no redeploy needed to retune sensitivity.

## Architecture

```
Cloud resources (simulated)
      │
      ▼
Telemetry agent (TrafficSimulator) ──writes raw samples──▶ NodeMetric (time-series, append-only)
                                                                  │
                                                                  ▼
                                                      Decision engine (ZombieDetector)
                                                      reads Policy, aggregates NodeMetric,
                                                      applies hysteresis, PROD safety
                                                                  │
                                       ┌──────────────────────────┼──────────────────────────┐
                                       ▼                          ▼                          ▼
                                  AuditEvent                   Alert                  ManagedNode.status
                                (every transition)      (owner-attributed,          (RUNNING/FLAGGED/STOPPED)
                                                          deduplicated)
                                                                                              │
                                                                                              ▼
                                                                                   React dashboard (polls every 5s)
                                                                                   Dashboard · Alerts · Audit · Policies
```

## Tech stack

**Backend:** Java 21, Spring Boot, Spring Data JPA, H2 (in-memory)
**Frontend:** React (Vite), plain CSS

## Project structure

```
.
├── backend/     Spring Boot API — detection engine, REST endpoints
└── frontend/    React dashboard
```

## Running it locally

**Backend** (starts on port 8080):
```bash
cd backend
./mvnw spring-boot:run
```

**Frontend** (starts on port 5173):
```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. The backend seeds 20 nodes on first boot and begins simulating usage immediately — give it a couple of minutes to accumulate enough metric history before nodes start getting flagged (this mirrors a real system needing a baseline before it can judge "idle").

## API reference

| Endpoint | Method | Purpose |
|---|---|---|
| `/api/nodes` | GET | List all nodes and their current state |
| `/api/savings` | GET | Aggregate count and $ of stopped nodes |
| `/api/nodes/{id}/override` | POST | Manually return a node to RUNNING, shielded from re-flagging for 24h |
| `/api/policy` | GET / PUT | View or update detection thresholds |
| `/api/audit` | GET | Full chronological event log |
| `/api/alerts` | GET | All alerts |
| `/api/alerts/{id}/ack` | POST | Acknowledge an alert |
| `/api/scheduler/status` | GET | Whether the automation loop is paused |
| `/api/scheduler/pause` / `/resume` | POST | Globally halt or resume detection |

## Known limitations (by design, not oversight)

This is a portfolio/capstone project, not a production system. Documented explicitly rather than hidden:

- **Simulated telemetry**, not real cloud metrics — the detection *logic* would work unchanged against real CloudWatch-style data; only the input source is simulated.
- **Single in-memory (H2) database** — no Postgres/Redis split; unnecessary at this scale.
- **Two signals only** (CPU, traffic boolean) — real tools also check active connections/sessions.
- **No authentication** on the API.
- **Demo-scale timing** — grace periods and evaluation cycles are compressed to seconds/minutes so behavior is observable quickly; production equivalents would run over hours.
