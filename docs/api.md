# REST API

Base URL: `/api`. Authenticated routes require `Authorization: Bearer <jwt>`.

## Auth

| Method | Path | Access |
|---|---|---|
| POST | `/auth/login` | Public `{ username, password }` → `{ token, user }` |
| GET | `/me` | Current user + roles + permissions |
| GET | `/health` | Public |

## Identity

| Method | Path | Permission |
|---|---|---|
| GET | `/users` | `USER_MANAGE` |
| GET | `/directory` | `ACTIVITY_ASSIGN` or `USER_MANAGE` |
| POST | `/users` | `USER_MANAGE` |
| PUT | `/users/{id}` | `USER_MANAGE` |
| GET | `/roles` | `USER_MANAGE` or `ROLE_MANAGE` |
| POST | `/roles` | `ROLE_MANAGE` |
| PUT | `/roles/{id}` | `ROLE_MANAGE` |
| GET | `/permissions` | `ROLE_MANAGE` |

## Masters

| Resource | GET | POST / PUT |
|---|---|---|
| `/aircraft-types` | `AIRCRAFT_VIEW` | `AIRCRAFT_MANAGE` |
| `/aircraft` | `AIRCRAFT_VIEW` | `AIRCRAFT_MANAGE` |
| `/components` | `COMPONENT_VIEW` | `COMPONENT_MANAGE` |
| `/check-types` | `TASK_VIEW` | `TASK_MANAGE` |
| `/tasks` | `TASK_VIEW` | `TASK_MANAGE` |
| `/schedules` | `SCHEDULE_VIEW` | `SCHEDULE_MANAGE` |

## Work orders

| Method | Path | Permission |
|---|---|---|
| GET | `/dashboard` | `DASHBOARD_VIEW` |
| GET | `/activities` | `ACTIVITY_VIEW` (`?state=` `&aircraftId=`) |
| GET | `/activities/queue` | `ACTIVITY_WORK` |
| GET | `/activities/{id}` | `ACTIVITY_VIEW` |
| POST | `/activities` | `ACTIVITY_CREATE` |
| PUT | `/activities/{id}` | create / work / assign |
| POST | `/activities/{id}/assign` | `ACTIVITY_ASSIGN` `{ userId, roleOnJob }` |
| POST | `/activities/{id}/transition` | Role-checked by workflow `{ toState, comment }` |
| POST | `/activities/{id}/logs` | `ACTIVITY_WORK` |
| POST | `/schedules/{id}/generate` | `SCHEDULE_MANAGE` — open a pending work order from a due interval |
| GET | `/reports/compliance` | `REPORT_VIEW` (text/plain) |
| GET | `/agent/status` | `ACTIVITY_VIEW` — `{ configured, model, maxSteps }` (never returns the API key) |
| POST | `/agent/ask` | `ACTIVITY_VIEW` `{ message, apply }` — hangar coordinator lists, **creates**, assigns, and transitions work in plain language. `apply: true` writes changes. |

The agent runs as the logged-in user. `apply: false` is a dry run.

## Inbox & audit

| Method | Path | Permission |
|---|---|---|
| GET | `/notifications` | `NOTIFICATION_VIEW` |
| POST | `/notifications/{id}/read` | owner |
| POST | `/notifications/read-all` | owner |
| GET | `/audit-logs` | `AUDIT_VIEW` (`page`, `size`, `entityType`, `action`) |

Errors: `{ timestamp, status, message }`.
