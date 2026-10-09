INSERT INTO permissions (code, name, resource, description) VALUES
('DASHBOARD_VIEW', 'View dashboard', 'dashboard', 'Access operational dashboard'),
('AIRCRAFT_VIEW', 'View aircraft', 'aircraft', 'Read aircraft master data'),
('AIRCRAFT_MANAGE', 'Manage aircraft', 'aircraft', 'Create and update aircraft masters'),
('COMPONENT_VIEW', 'View components', 'components', 'Read component master data'),
('COMPONENT_MANAGE', 'Manage components', 'components', 'Create and update components'),
('TASK_VIEW', 'View tasks', 'tasks', 'Read maintenance task cards'),
('TASK_MANAGE', 'Manage tasks', 'tasks', 'Create and update task cards and check types'),
('SCHEDULE_VIEW', 'View schedules', 'schedules', 'Read recurring maintenance schedules'),
('SCHEDULE_MANAGE', 'Manage schedules', 'schedules', 'Create and update recurring schedules'),
('ACTIVITY_VIEW', 'View activities', 'activities', 'Read maintenance activities'),
('ACTIVITY_CREATE', 'Create activities', 'activities', 'Open maintenance requests and plans'),
('ACTIVITY_ASSIGN', 'Assign activities', 'activities', 'Assign technicians and resources'),
('ACTIVITY_WORK', 'Work activities', 'activities', 'Log work and update in-progress jobs'),
('ACTIVITY_APPROVE', 'Approve completion', 'activities', 'Approve technician completion'),
('ACTIVITY_VERIFY', 'Verify activities', 'activities', 'Quality verification of completed work'),
('ACTIVITY_ESCALATE', 'Escalate activities', 'activities', 'Escalate or reassign overdue/blocked work'),
('REPORT_VIEW', 'View reports', 'reports', 'Generate maintenance and compliance reports'),
('NOTIFICATION_VIEW', 'View notifications', 'notifications', 'Access notification center'),
('AUDIT_VIEW', 'View audit logs', 'audit', 'Read-only compliance audit trail'),
('USER_MANAGE', 'Manage users', 'users', 'Create users and assign roles'),
('ROLE_MANAGE', 'Manage roles', 'roles', 'Create roles and map permissions');

INSERT INTO roles (code, name, description, system_role) VALUES
('ADMIN', 'Administrator', 'Platform administration, users, roles, and full operational visibility', TRUE),
('QUALITY_ASSURANCE', 'Quality Assurance', 'Independent verification, compliance documentation, and audit access', TRUE),
('MAINTENANCE_MANAGER', 'Maintenance Manager', 'Planning, assignment, approval of completion, and station oversight', TRUE),
('SUPERVISOR', 'Maintenance Supervisor', 'Shift supervision, reassignment, escalation, and work release', TRUE),
('FLIGHT_ENGINEER', 'Flight Engineer', 'Defect reporting, aircraft status visibility, and technical coordination', TRUE),
('MAINTENANCE_TECHNICIAN', 'Maintenance Technician', 'Assigned work execution, findings, and task card close-out', TRUE);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code = 'ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'DASHBOARD_VIEW','AIRCRAFT_VIEW','COMPONENT_VIEW','TASK_VIEW','SCHEDULE_VIEW',
    'ACTIVITY_VIEW','ACTIVITY_VERIFY','REPORT_VIEW','NOTIFICATION_VIEW','AUDIT_VIEW'
) WHERE r.code = 'QUALITY_ASSURANCE';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'DASHBOARD_VIEW','AIRCRAFT_VIEW','AIRCRAFT_MANAGE','COMPONENT_VIEW','COMPONENT_MANAGE',
    'TASK_VIEW','TASK_MANAGE','SCHEDULE_VIEW','SCHEDULE_MANAGE','ACTIVITY_VIEW','ACTIVITY_CREATE',
    'ACTIVITY_ASSIGN','ACTIVITY_APPROVE','ACTIVITY_ESCALATE','REPORT_VIEW','NOTIFICATION_VIEW'
) WHERE r.code = 'MAINTENANCE_MANAGER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'DASHBOARD_VIEW','AIRCRAFT_VIEW','COMPONENT_VIEW','TASK_VIEW','SCHEDULE_VIEW',
    'ACTIVITY_VIEW','ACTIVITY_ASSIGN','ACTIVITY_WORK','ACTIVITY_APPROVE','ACTIVITY_ESCALATE',
    'REPORT_VIEW','NOTIFICATION_VIEW'
) WHERE r.code = 'SUPERVISOR';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'DASHBOARD_VIEW','AIRCRAFT_VIEW','COMPONENT_VIEW','TASK_VIEW','SCHEDULE_VIEW',
    'ACTIVITY_VIEW','ACTIVITY_CREATE','REPORT_VIEW','NOTIFICATION_VIEW'
) WHERE r.code = 'FLIGHT_ENGINEER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'DASHBOARD_VIEW','AIRCRAFT_VIEW','COMPONENT_VIEW','TASK_VIEW',
    'ACTIVITY_VIEW','ACTIVITY_WORK','NOTIFICATION_VIEW'
) WHERE r.code = 'MAINTENANCE_TECHNICIAN';

-- BCrypt hash for Password123! (cost 10)
INSERT INTO users (username, email, password_hash, full_name, phone, station, active) VALUES
('admin', 'admin@amos.aero', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Alex Rivera', '+971500000001', 'DXB', TRUE),
('qa', 'qa@amos.aero', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Priya Shah', '+971500000002', 'DXB', TRUE),
('manager', 'manager@amos.aero', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'James Okonkwo', '+971500000003', 'DXB', TRUE),
('supervisor', 'supervisor@amos.aero', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Sofia Mendes', '+971500000004', 'DXB', TRUE),
('engineer', 'engineer@amos.aero', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Daniel Chen', '+971500000005', 'DXB', TRUE),
('tech1', 'tech1@amos.aero', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Omar Haddad', '+971500000006', 'DXB', TRUE),
('tech2', 'tech2@amos.aero', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Elena Petrova', '+971500000007', 'AUH', TRUE);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON
    (u.username = 'admin' AND r.code = 'ADMIN') OR
    (u.username = 'qa' AND r.code = 'QUALITY_ASSURANCE') OR
    (u.username = 'manager' AND r.code = 'MAINTENANCE_MANAGER') OR
    (u.username = 'supervisor' AND r.code = 'SUPERVISOR') OR
    (u.username = 'engineer' AND r.code = 'FLIGHT_ENGINEER') OR
    (u.username IN ('tech1','tech2') AND r.code = 'MAINTENANCE_TECHNICIAN');

INSERT INTO aircraft_types (icao_code, manufacturer, model, description) VALUES
('A320', 'Airbus', 'A320-200', 'Narrow-body twin-engine'),
('A321', 'Airbus', 'A321neo', 'Stretched narrow-body'),
('B777', 'Boeing', '777-300ER', 'Long-range wide-body'),
('B787', 'Boeing', '787-9', 'Composite wide-body');

INSERT INTO aircraft (registration, aircraft_type_id, serial_number, status, total_flight_hours, total_cycles, base_station, in_service_date)
SELECT 'A6-AMS', id, 'MSN-1001', 'IN_SERVICE', 18420.5, 9650, 'DXB', DATE '2016-03-12' FROM aircraft_types WHERE icao_code = 'A320';
INSERT INTO aircraft (registration, aircraft_type_id, serial_number, status, total_flight_hours, total_cycles, base_station, in_service_date)
SELECT 'A6-AMR', id, 'MSN-1002', 'IN_SERVICE', 22110.0, 11240, 'DXB', DATE '2014-08-01' FROM aircraft_types WHERE icao_code = 'A320';
INSERT INTO aircraft (registration, aircraft_type_id, serial_number, status, total_flight_hours, total_cycles, base_station, in_service_date)
SELECT 'A6-WIDE', id, 'MSN-3001', 'IN_SERVICE', 31200.4, 4890, 'DXB', DATE '2012-11-20' FROM aircraft_types WHERE icao_code = 'B777';
INSERT INTO aircraft (registration, aircraft_type_id, serial_number, status, total_flight_hours, total_cycles, base_station, in_service_date)
SELECT 'A6-DREAM', id, 'MSN-4001', 'AOG', 9800.0, 2105, 'AUH', DATE '2019-05-04' FROM aircraft_types WHERE icao_code = 'B787';

INSERT INTO components (part_number, serial_number, name, category, aircraft_id, status, life_limit_hours, life_limit_cycles, accumulated_hours, accumulated_cycles)
SELECT 'CFM56-5B4', 'ENG-L-1001', 'Left engine', 'ENGINE', id, 'INSTALLED', 25000, NULL, 18420.5, 9650 FROM aircraft WHERE registration = 'A6-AMS';
INSERT INTO components (part_number, serial_number, name, category, aircraft_id, status, life_limit_hours, life_limit_cycles, accumulated_hours, accumulated_cycles)
SELECT 'LG-MLG-A320', 'LG-2001', 'Main landing gear', 'LANDING_GEAR', id, 'INSTALLED', NULL, 20000, 18420.5, 9650 FROM aircraft WHERE registration = 'A6-AMS';
INSERT INTO components (part_number, serial_number, name, category, aircraft_id, status, life_limit_hours, life_limit_cycles, accumulated_hours, accumulated_cycles)
SELECT 'GE90-115B', 'ENG-L-3001', 'Left engine', 'ENGINE', id, 'INSTALLED', 30000, NULL, 31200.4, 4890 FROM aircraft WHERE registration = 'A6-WIDE';

INSERT INTO check_types (code, name, description, typical_downtime_hours) VALUES
('A', 'A-check', 'Light line maintenance, typically every 400-600 FH', 12),
('B', 'B-check', 'Intermediate check (often folded into A-check packages)', 24),
('C', 'C-check', 'Heavy base maintenance, typically every 18-24 months', 240),
('D', 'D-check', 'Structural heavy check / overhaul', 1440),
('DAILY', 'Daily check', 'Pre-flight / daily inspection', 2),
('WEEKLY', 'Weekly check', 'Weekly line inspection', 4),
('UNSCHE', 'Unscheduled', 'Defect, AOG, or opportunistic work', NULL);

INSERT INTO maintenance_tasks (task_card, title, description, check_type_id, aircraft_type_id, ata_chapter, estimated_hours, skill)
SELECT 'A320-A-12-001', 'A-check zonal inspection', 'Walk-around, fluid levels, emergency equipment', ct.id, at.id, '12', 8.0, 'LINE'
FROM check_types ct, aircraft_types at WHERE ct.code = 'A' AND at.icao_code = 'A320';
INSERT INTO maintenance_tasks (task_card, title, description, check_type_id, aircraft_type_id, ata_chapter, estimated_hours, skill)
SELECT 'A320-32-210', 'MLG visual and NDT', 'Landing gear inspection and lubrication', ct.id, at.id, '32', 6.0, 'STRUCTURES'
FROM check_types ct, aircraft_types at WHERE ct.code = 'C' AND at.icao_code = 'A320';
INSERT INTO maintenance_tasks (task_card, title, description, check_type_id, aircraft_type_id, ata_chapter, estimated_hours, skill)
SELECT 'B777-71-110', 'Engine boroscope', 'Hot section inspection', ct.id, at.id, '71', 4.5, 'ENGINES'
FROM check_types ct, aircraft_types at WHERE ct.code = 'A' AND at.icao_code = 'B777';
INSERT INTO maintenance_tasks (task_card, title, description, check_type_id, aircraft_type_id, ata_chapter, estimated_hours, skill)
SELECT 'GEN-UNS-001', 'Pilot reported defect', 'Unscheduled troubleshooting', ct.id, NULL, '05', 2.0, 'AVIONICS'
FROM check_types ct WHERE ct.code = 'UNSCHE';

INSERT INTO recurring_schedules (aircraft_id, task_id, check_type_id, interval_hours, interval_days, interval_cycles, last_performed_at, last_hours, last_cycles, next_due_at, next_due_hours, next_due_cycles, active)
SELECT a.id, t.id, ct.id, 500, NULL, NULL, NOW() - INTERVAL '18 days', 17920, 9400, NOW() + INTERVAL '4 days', 18420.5 + 80, NULL, TRUE
FROM aircraft a, maintenance_tasks t, check_types ct
WHERE a.registration = 'A6-AMS' AND t.task_card = 'A320-A-12-001' AND ct.code = 'A';

INSERT INTO recurring_schedules (aircraft_id, task_id, check_type_id, interval_hours, interval_days, interval_cycles, last_performed_at, last_hours, last_cycles, next_due_at, next_due_hours, next_due_cycles, active)
SELECT a.id, t.id, ct.id, NULL, 730, NULL, NOW() - INTERVAL '700 days', 14000, 8000, NOW() + INTERVAL '20 days', NULL, NULL, TRUE
FROM aircraft a, maintenance_tasks t, check_types ct
WHERE a.registration = 'A6-AMS' AND t.task_card = 'A320-32-210' AND ct.code = 'C';

INSERT INTO maintenance_activities (
    activity_number, title, description, aircraft_id, check_type_id, task_id, state, priority, due_at,
    planned_start, planned_end, requested_by_id, assigned_to_id, station
)
SELECT 'WO-1001', 'A6-AMS A-check package', 'Scheduled A-check', a.id, ct.id, t.id, 'IN_PROGRESS', 'HIGH', NOW() + INTERVAL '2 days',
       NOW() - INTERVAL '4 hours', NOW() + INTERVAL '8 hours', u1.id, u2.id, 'DXB'
FROM aircraft a, check_types ct, maintenance_tasks t, users u1, users u2
WHERE a.registration = 'A6-AMS' AND ct.code = 'A' AND t.task_card = 'A320-A-12-001'
  AND u1.username = 'manager' AND u2.username = 'tech1';

INSERT INTO maintenance_activities (
    activity_number, title, description, aircraft_id, check_type_id, task_id, parent_id, state, priority, due_at,
    requested_by_id, assigned_to_id, station
)
SELECT 'WO-1001-1', 'Cabin emergency equipment', 'Parallel cabin task under A-check', a.id, ct.id, t.id, p.id, 'ASSIGNED', 'NORMAL', NOW() + INTERVAL '2 days',
       u1.id, u3.id, 'DXB'
FROM aircraft a, check_types ct, maintenance_tasks t, maintenance_activities p, users u1, users u3
WHERE a.registration = 'A6-AMS' AND ct.code = 'A' AND t.task_card = 'A320-A-12-001'
  AND p.activity_number = 'WO-1001' AND u1.username = 'manager' AND u3.username = 'tech2';

INSERT INTO maintenance_activities (
    activity_number, title, description, aircraft_id, check_type_id, task_id, state, priority, due_at,
    requested_by_id, station
)
SELECT 'WO-1002', 'A6-DREAM AOG — pack 2 inop', 'Unscheduled ECS defect, aircraft on ground', a.id, ct.id, t.id, 'PENDING', 'AOG', NOW() + INTERVAL '6 hours',
       u.id, 'AUH'
FROM aircraft a, check_types ct, maintenance_tasks t, users u
WHERE a.registration = 'A6-DREAM' AND ct.code = 'UNSCHE' AND t.task_card = 'GEN-UNS-001' AND u.username = 'engineer';

INSERT INTO maintenance_activities (
    activity_number, title, description, aircraft_id, check_type_id, task_id, state, priority, due_at,
    requested_by_id, assigned_to_id, station, actual_end
)
SELECT 'WO-0998', 'A6-WIDE engine boroscope', 'Completed A-check engine task awaiting QA', a.id, ct.id, t.id, 'COMPLETED', 'NORMAL', NOW() - INTERVAL '1 day',
       u1.id, u2.id, 'DXB', NOW() - INTERVAL '2 hours'
FROM aircraft a, check_types ct, maintenance_tasks t, users u1, users u2
WHERE a.registration = 'A6-WIDE' AND ct.code = 'A' AND t.task_card = 'B777-71-110'
  AND u1.username = 'supervisor' AND u2.username = 'tech1';

INSERT INTO activity_assignments (activity_id, user_id, role_on_job, assigned_by_id)
SELECT a.id, u.id, 'LEAD', m.id FROM maintenance_activities a, users u, users m
WHERE a.activity_number = 'WO-1001' AND u.username = 'tech1' AND m.username = 'manager';

INSERT INTO activity_logs (activity_id, author_id, log_type, findings, actions_taken, hours_spent)
SELECT a.id, u.id, 'PROGRESS', 'Hydraulics within limits. Cabin oxygen bottles due calibration.', 'Opened zonal panels 191/192. Started lubrication.', 2.5
FROM maintenance_activities a, users u WHERE a.activity_number = 'WO-1001' AND u.username = 'tech1';

INSERT INTO workflow_transitions (activity_id, from_state, to_state, actor_id, comment)
SELECT a.id, 'PENDING', 'ASSIGNED', u.id, 'Assigned to line team DXB'
FROM maintenance_activities a, users u WHERE a.activity_number = 'WO-1001' AND u.username = 'manager';
INSERT INTO workflow_transitions (activity_id, from_state, to_state, actor_id, comment)
SELECT a.id, 'ASSIGNED', 'IN_PROGRESS', u.id, 'Started work'
FROM maintenance_activities a, users u WHERE a.activity_number = 'WO-1001' AND u.username = 'tech1';
