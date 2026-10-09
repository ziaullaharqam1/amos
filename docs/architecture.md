# Architecture

AMOS is a layered Spring Boot service with a React operations console. Transactional work (work orders, masters, RBAC) lives in PostgreSQL. Notifications and audit copies are published to Kafka so delivery and compliance analysis are decoupled from hangar workflows.

```
React (Tailwind)  →  REST / JWT  →  Spring services
                                      ├─ WorkflowEngine (state + permission)
                                      ├─ Workflow agent (Grok + Java tools)
                                      ├─ PostgreSQL (source of truth)
                                      └─ Kafka topics ams.notifications / ams.audit
```

## Roles (aviation MRO)

| Role | Typical duty | Workflow rights |
|---|---|---|
| Maintenance Technician | Execute assigned task cards | Start / hold / complete, log findings |
| Flight Engineer | Defect reports, aircraft status | Create requests, view fleet |
| Supervisor | Shift control | Assign, reassign, escalate, approve |
| Maintenance Manager | Planning and station load | Schedule, assign, approve, masters |
| Quality Assurance | Independent release | Verify completed work, audit logs |
| Admin | Platform | Users, roles, all screens |

Permissions are screen- and API-level (`ACTIVITY_VERIFY`, `AUDIT_VIEW`, …). Spring `@PreAuthorize` enforces the same codes the UI uses to hide navigation.

## Workflow

`PENDING → ASSIGNED → IN_PROGRESS → COMPLETED → VERIFIED`

Branches: `ON_HOLD`, `ESCALATED`, `REJECTED` (back to work), `CANCELLED`.

Parallel work is a parent work order with child activities (`parent_id`). Escalation is manual (permission `ACTIVITY_ESCALATE`) or scheduled when a job is overdue.

## Agentic workflow

A Java `WorkflowAgentService` drives work orders with tools (list/assign/transition). By default it uses a **free local coordinator** (`GROK_PROVIDER=free`). Set `GROK_PROVIDER=xai` and `GROK_API_KEY` to use Grok; if xAI returns a credits/permission error, the app falls back to the local model. The coordinator handles **manager** (create, assign, escalate), **technician/engineer** (start, complete, hold), and **QA** (verify, reject) in spoken English. Mutating tools still go through `ActivityService` (RBAC and legal state jumps). Email notifications send through Gmail SMTP (`MAIL_USERNAME` / `MAIL_PASSWORD`). The UI is a **Workflow agent** chat.

## Notifications

`NotificationService.emit` writes in-app and email/SMS channel rows, then publishes `NotificationEvent` to `ams.notifications`. A Kafka consumer logs fan-out for downstream systems. Email uses Spring Mail when configured; otherwise it is logged.

## Audit

Every create/update/assign/approve records before/after JSON in `audit_logs` and publishes `AuditEvent` to `ams.audit`. Only `AUDIT_VIEW` (Admin, QA) can read the log API.

## Recurring maintenance

`recurring_schedules` stores FH / calendar-day / cycle intervals. Planners can open a work order from a schedule; a background job also opens jobs when `next_due_at` is within two days. QA verification rolls the next due date/hours/cycles forward. Reminders go to managers when due dates approach.
