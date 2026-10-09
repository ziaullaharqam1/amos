# Database schema

Flyway scripts: `backend/src/main/resources/db/migration/`.

- `V1__schema.sql` — tables and indexes  
- `V2__seed.sql` — roles, permissions, demo fleet, sample work orders  

## Core tables

| Table | Purpose |
|---|---|
| `users`, `roles`, `permissions`, `user_roles`, `role_permissions` | RBAC |
| `aircraft_types`, `aircraft`, `components` | Fleet and serialized parts |
| `check_types`, `maintenance_tasks` | A/B/C/D checks and task cards |
| `recurring_schedules` | FH / days / cycles due calculation |
| `maintenance_activities` | Work orders (`state`, `parent_id` for parallel jobs) |
| `activity_assignments`, `activity_logs` | Crew and findings |
| `workflow_transitions` | State history |
| `audit_logs` | Compliance (before/after) |
| `notifications` | In-app / email / SMS delivery |

Indexes cover activity state, assignee, due date, audit time, and unread notifications.

PostgreSQL 16 is the production dialect. Tests use H2 in PostgreSQL compatibility mode with the same Flyway scripts.
