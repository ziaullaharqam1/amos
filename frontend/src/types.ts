export interface UserView {
  id: number
  username: string
  email: string
  fullName: string
  phone: string | null
  station: string | null
  active: boolean
  roles: string[]
  permissions: string[]
}

export interface AuthResponse {
  token: string
  user: UserView
}

export interface RoleView {
  id: number
  code: string
  name: string
  description: string | null
  systemRole: boolean
  permissions: string[]
}

export interface PermissionView {
  id: number
  code: string
  name: string
  resource: string
  description: string | null
}

export interface AircraftTypeView {
  id: number
  icaoCode: string
  manufacturer: string
  model: string
  description: string | null
}

export interface AircraftView {
  id: number
  registration: string
  aircraftTypeId: number
  aircraftType: string
  serialNumber: string
  status: string
  totalFlightHours: number
  totalCycles: number
  baseStation: string | null
  inServiceDate: string | null
}

export interface ComponentView {
  id: number
  partNumber: string
  serialNumber: string
  name: string
  category: string
  aircraftId: number | null
  aircraftRegistration: string | null
  status: string
  lifeLimitHours: number | null
  lifeLimitCycles: number | null
  accumulatedHours: number
  accumulatedCycles: number
}

export interface CheckTypeView {
  id: number
  code: string
  name: string
  description: string | null
  typicalDowntimeHours: number | null
}

export interface TaskView {
  id: number
  taskCard: string
  title: string
  description: string | null
  checkTypeId: number | null
  checkType: string | null
  aircraftTypeId: number | null
  aircraftType: string | null
  ataChapter: string | null
  estimatedHours: number | null
  skill: string | null
}

export interface ScheduleView {
  id: number
  aircraftId: number
  aircraftRegistration: string
  taskId: number | null
  taskCard: string | null
  checkTypeId: number | null
  checkType: string | null
  intervalHours: number | null
  intervalDays: number | null
  intervalCycles: number | null
  lastPerformedAt: string | null
  nextDueAt: string | null
  nextDueHours: number | null
  nextDueCycles: number | null
  active: boolean
}

export interface ActivitySummary {
  id: number
  activityNumber: string
  title: string
  aircraftRegistration: string
  checkType: string | null
  state: string
  priority: string
  dueAt: string | null
  assignedTo: string | null
  station: string | null
  parentId: number | null
}

export interface AssignmentView {
  id: number
  userId: number
  fullName: string
  roleOnJob: string | null
  assignedAt: string
}

export interface LogView {
  id: number
  author: string
  logType: string
  findings: string | null
  actionsTaken: string | null
  hoursSpent: number | null
  createdAt: string
}

export interface TransitionView {
  id: number
  fromState: string
  toState: string
  actor: string
  comment: string | null
  createdAt: string
}

export interface ActivityDetail {
  id: number
  activityNumber: string
  title: string
  description: string | null
  aircraftId: number
  aircraftRegistration: string
  checkTypeId: number | null
  checkType: string | null
  taskId: number | null
  taskCard: string | null
  componentId: number | null
  parentId: number | null
  scheduleId: number | null
  state: string
  priority: string
  dueAt: string | null
  plannedStart: string | null
  plannedEnd: string | null
  actualStart: string | null
  actualEnd: string | null
  assignedToId: number | null
  assignedTo: string | null
  requestedById: number | null
  requestedBy: string | null
  verifiedById: number | null
  verifiedBy: string | null
  station: string | null
  findings: string | null
  actionsTaken: string | null
  allowedTransitions: string[]
  assignments: AssignmentView[]
  logs: LogView[]
  history: TransitionView[]
  children: ActivitySummary[]
}

export interface NotificationView {
  id: number
  title: string
  body: string
  type: string
  channel: string
  entityType: string | null
  entityId: string | null
  readAt: string | null
  deliveryStatus: string
  createdAt: string
}

export interface AuditView {
  id: number
  actorUsername: string | null
  action: string
  entityType: string
  entityId: string | null
  beforeValue: string | null
  afterValue: string | null
  createdAt: string
}

export interface PageResult<T> {
  content: T[]
  number: number
  totalPages: number
}

export interface DashboardMetrics {
  openActivities: number
  inProgress: number
  overdue: number
  awaitingQa: number
  unreadNotifications: number
  dueSoon: ActivitySummary[]
  aog: ActivitySummary[]
}

export interface AgentStatus {
  configured: boolean
  model: string
  maxSteps: number
}

export interface AgentStep {
  tool: string
  arguments: string
  result: string
}

export interface AgentReply {
  answer: string
  applied: boolean
  steps: AgentStep[]
}

export function errorMessage(error: unknown): string {
  if (error instanceof Error) return error.message
  return 'Request failed'
}
