# FinOps Zombie Resource Detector

A self-governing system that finds idle ("zombie") cloud resources, holds them through a review grace period, and stops them automatically, while **never auto-stopping production** without human sign-off. Multi-user: each person monitors only their own resources, and an admin controls the shared detection policy.

```
zombie-detector/
├── backend/    Spring Boot 4 API (Java, Spring Security + JWT, JPA, H2 or Postgres)
├── frontend/   React (Vite + Tailwind) dashboard
└── render.yaml Render Blueprint (API + Postgres)
```

## How it works

```
TrafficSimulator (sample per node every 10s) -> NodeMetric (time series)
        -> ZombieDetector (every 30s): window aggregate, AND rule (low CPU AND no traffic),
           min-sample guard, grace period, 3-clean-cycle hysteresis
        -> AuditEvent / Alert / ManagedNode.status
        -> React dashboard (5s polling) + live policy-change push (SSE)
```

Key decisions: time-series aggregation (not a cached snapshot), AND rule, hysteresis on recovery, PROD is only flagged/held (never auto-stopped), DB-backed live-editable policy, every state change audited.

## Roles and data isolation

| | USER | ADMIN |
|---|---|---|
| Sees nodes / alerts / audit / savings | only their own | everything (incl. 20 seeded demo nodes) |
| Add / remove resources, override a flagged node | own only (max 25) | any |
| Edit detection policy | no (read-only, notified on change) | yes (validated, audited) |
| Pause / resume the automation | no | yes |

Sign-up is open and always creates a USER. The admin account is created at startup from `ADMIN_EMAIL` / `ADMIN_PASSWORD`.

Resources are registered by hand (type, hourly rate, environment). There is no cloud connection, so their CPU/traffic telemetry is **simulated**.

## API

All routes need `Authorization: Bearer <jwt>` except `/api/auth/login`, `/api/auth/register`, `/api/health`.

| Endpoint | Method | Notes |
|---|---|---|
| `/api/auth/register`, `/login` | POST | emails are lower-cased; 10 attempts/min/IP |
| `/api/auth/me` | GET | current user + role |
| `/api/nodes` | GET / POST | scoped to caller; POST validated |
| `/api/nodes/{id}` | DELETE | owner or admin |
| `/api/nodes/{id}/override` | POST | owner or admin, 24h shield |
| `/api/savings`, `/api/savings/history` | GET | scoped; history is replayed from the audit trail |
| `/api/alerts`, `/api/alerts/{id}/ack` | GET / POST | scoped |
| `/api/audit` | GET | scoped |
| `/api/policy` | GET / PUT | PUT admin-only, validated, audited, broadcast |
| `/api/policy/stream?token=` | GET (SSE) | pushes `policy-updated` to every connected client |
| `/api/scheduler/status`, `/pause`, `/resume` | GET / POST | pause/resume admin-only |
| `/api/health` | GET | public liveness probe |

## Run locally

```bash
# backend (H2 in memory; data resets on restart)
cd backend && ./mvnw spring-boot:run          # http://localhost:8080
# default admin: admin@zombiedetector.local / ChangeMe123!  (dev only)

# frontend
cd frontend && cp .env.example .env && npm install && npm run dev   # http://localhost:5173
```

## Tests

```bash
cd backend  && ./mvnw test     # detection rules, policy validation, rate limiter, savings replay,
                               # and end-to-end security/tenant-isolation tests
cd frontend && npm test        # login/session/expiry, role-based UI, SSE toast, add-resource flows
cd frontend && npm run lint && npm run build
```

## Deploy

**Backend on Render (Docker + Postgres)** — New > Blueprint > pick this repo (uses `render.yaml`). Enter `ADMIN_EMAIL` and `ADMIN_PASSWORD` when prompted; `JWT_SECRET` is generated for you. The service refuses to start on Render if either secret is still the committed dev default.
Optional env: `CORS_EXTRA_ORIGINS` (custom frontend domain), `AUTH_RATE_LIMIT`.

**Frontend on Vercel** — import the repo, set *Root Directory* to `frontend`, and add the env var `VITE_API_URL=https://<your-render-service>.onrender.com/api`. (`*.vercel.app` is already allowed by CORS.)

Render's free web tier sleeps when idle (first request after a pause is slow) and free Postgres instances expire after a trial period; use paid plans if the data matters.

## Known limitations

- Telemetry is simulated; there is no real cloud integration (CPU + traffic boolean are the only signals).
- Timing is demo-compressed (seconds/minutes instead of hours/days) and metrics are retained only for the idle window plus a few minutes.
- Password-based accounts only: no email verification or password reset.
- JWTs last 1 hour and are not revocable; the auth rate limiter is per instance and in memory.
- Single backend instance assumed (SSE connections and rate limits live in memory).
- No reservation/savings-plan-aware cost normalization; no staggered termination.
