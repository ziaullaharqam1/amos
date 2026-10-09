CREATE TABLE roles (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(64) NOT NULL UNIQUE,
    name            VARCHAR(128) NOT NULL,
    description     TEXT,
    system_role     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE permissions (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(64) NOT NULL UNIQUE,
    name            VARCHAR(128) NOT NULL,
    resource        VARCHAR(64) NOT NULL,
    description     TEXT
);

CREATE TABLE role_permissions (
    role_id         BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id   BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(64) NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    phone           VARCHAR(32),
    station         VARCHAR(64),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE user_roles (
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id         BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE aircraft_types (
    id              BIGSERIAL PRIMARY KEY,
    icao_code       VARCHAR(16) NOT NULL UNIQUE,
    manufacturer    VARCHAR(128) NOT NULL,
    model           VARCHAR(128) NOT NULL,
    description     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE aircraft (
    id                  BIGSERIAL PRIMARY KEY,
    registration        VARCHAR(16) NOT NULL UNIQUE,
    aircraft_type_id    BIGINT NOT NULL REFERENCES aircraft_types(id),
    serial_number       VARCHAR(64) NOT NULL UNIQUE,
    status              VARCHAR(32) NOT NULL DEFAULT 'IN_SERVICE',
    total_flight_hours  NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_cycles        INTEGER NOT NULL DEFAULT 0,
    base_station        VARCHAR(64),
    in_service_date     DATE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE components (
    id                  BIGSERIAL PRIMARY KEY,
    part_number         VARCHAR(64) NOT NULL,
    serial_number       VARCHAR(64) NOT NULL,
    name                VARCHAR(255) NOT NULL,
    category            VARCHAR(64) NOT NULL,
    aircraft_id         BIGINT REFERENCES aircraft(id),
    status              VARCHAR(32) NOT NULL DEFAULT 'INSTALLED',
    life_limit_hours    NUMERIC(12,2),
    life_limit_cycles   INTEGER,
    accumulated_hours   NUMERIC(12,2) NOT NULL DEFAULT 0,
    accumulated_cycles  INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (part_number, serial_number)
);

CREATE TABLE check_types (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(32) NOT NULL UNIQUE,
    name            VARCHAR(128) NOT NULL,
    description     TEXT,
    typical_downtime_hours INTEGER,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE maintenance_tasks (
    id                  BIGSERIAL PRIMARY KEY,
    task_card           VARCHAR(64) NOT NULL UNIQUE,
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    check_type_id       BIGINT REFERENCES check_types(id),
    aircraft_type_id    BIGINT REFERENCES aircraft_types(id),
    ata_chapter         VARCHAR(16),
    estimated_hours     NUMERIC(8,2),
    skill               VARCHAR(64),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE recurring_schedules (
    id                  BIGSERIAL PRIMARY KEY,
    aircraft_id         BIGINT NOT NULL REFERENCES aircraft(id),
    task_id             BIGINT REFERENCES maintenance_tasks(id),
    check_type_id       BIGINT REFERENCES check_types(id),
    interval_hours      NUMERIC(12,2),
    interval_days       INTEGER,
    interval_cycles     INTEGER,
    last_performed_at   TIMESTAMPTZ,
    last_hours          NUMERIC(12,2),
    last_cycles         INTEGER,
    next_due_at         TIMESTAMPTZ,
    next_due_hours      NUMERIC(12,2),
    next_due_cycles     INTEGER,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE maintenance_activities (
    id                  BIGSERIAL PRIMARY KEY,
    activity_number     VARCHAR(32) NOT NULL UNIQUE,
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    aircraft_id         BIGINT NOT NULL REFERENCES aircraft(id),
    check_type_id       BIGINT REFERENCES check_types(id),
    task_id             BIGINT REFERENCES maintenance_tasks(id),
    component_id        BIGINT REFERENCES components(id),
    parent_id           BIGINT REFERENCES maintenance_activities(id),
    schedule_id         BIGINT REFERENCES recurring_schedules(id),
    state               VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    priority            VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    due_at              TIMESTAMPTZ,
    planned_start       TIMESTAMPTZ,
    planned_end         TIMESTAMPTZ,
    actual_start        TIMESTAMPTZ,
    actual_end          TIMESTAMPTZ,
    requested_by_id     BIGINT REFERENCES users(id),
    assigned_to_id      BIGINT REFERENCES users(id),
    verified_by_id      BIGINT REFERENCES users(id),
    station             VARCHAR(64),
    findings            TEXT,
    actions_taken       TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE activity_assignments (
    id              BIGSERIAL PRIMARY KEY,
    activity_id     BIGINT NOT NULL REFERENCES maintenance_activities(id) ON DELETE CASCADE,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    role_on_job     VARCHAR(64),
    assigned_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    assigned_by_id  BIGINT REFERENCES users(id)
);

CREATE TABLE activity_logs (
    id              BIGSERIAL PRIMARY KEY,
    activity_id     BIGINT NOT NULL REFERENCES maintenance_activities(id) ON DELETE CASCADE,
    author_id       BIGINT NOT NULL REFERENCES users(id),
    log_type        VARCHAR(32) NOT NULL DEFAULT 'PROGRESS',
    findings        TEXT,
    actions_taken   TEXT,
    hours_spent     NUMERIC(8,2),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE workflow_transitions (
    id              BIGSERIAL PRIMARY KEY,
    activity_id     BIGINT NOT NULL REFERENCES maintenance_activities(id) ON DELETE CASCADE,
    from_state      VARCHAR(32) NOT NULL,
    to_state        VARCHAR(32) NOT NULL,
    actor_id        BIGINT NOT NULL REFERENCES users(id),
    comment         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    actor_id        BIGINT REFERENCES users(id),
    actor_username  VARCHAR(64),
    action          VARCHAR(32) NOT NULL,
    entity_type     VARCHAR(64) NOT NULL,
    entity_id       VARCHAR(64),
    before_value    TEXT,
    after_value     TEXT,
    ip_address      VARCHAR(64),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE notifications (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(255) NOT NULL,
    body            TEXT NOT NULL,
    type            VARCHAR(64) NOT NULL,
    channel         VARCHAR(16) NOT NULL DEFAULT 'IN_APP',
    entity_type     VARCHAR(64),
    entity_id       VARCHAR(64),
    read_at         TIMESTAMPTZ,
    delivery_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    delivered_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_aircraft_type ON aircraft(aircraft_type_id);
CREATE INDEX idx_aircraft_status ON aircraft(status);
CREATE INDEX idx_components_aircraft ON components(aircraft_id);
CREATE INDEX idx_components_status ON components(status);
CREATE INDEX idx_tasks_check_type ON maintenance_tasks(check_type_id);
CREATE INDEX idx_schedules_aircraft ON recurring_schedules(aircraft_id);
CREATE INDEX idx_schedules_next_due ON recurring_schedules(next_due_at);
CREATE INDEX idx_activities_aircraft ON maintenance_activities(aircraft_id);
CREATE INDEX idx_activities_state ON maintenance_activities(state);
CREATE INDEX idx_activities_assigned ON maintenance_activities(assigned_to_id);
CREATE INDEX idx_activities_due ON maintenance_activities(due_at);
CREATE INDEX idx_activities_parent ON maintenance_activities(parent_id);
CREATE INDEX idx_assignments_user ON activity_assignments(user_id);
CREATE INDEX idx_activity_logs_activity ON activity_logs(activity_id);
CREATE INDEX idx_transitions_activity ON workflow_transitions(activity_id);
CREATE INDEX idx_audit_created ON audit_logs(created_at);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_actor ON audit_logs(actor_id);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, read_at);
CREATE INDEX idx_notifications_created ON notifications(created_at);
CREATE INDEX idx_users_station ON users(station);
