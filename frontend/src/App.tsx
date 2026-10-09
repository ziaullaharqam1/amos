import { Navigate, Route, Routes } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from './auth/AuthContext'
import AppLayout from './layouts/AppLayout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import ActivitiesPage from './pages/ActivitiesPage'
import ActivityDetailPage from './pages/ActivityDetailPage'
import QueuePage from './pages/QueuePage'
import AircraftPage from './pages/AircraftPage'
import ComponentsPage from './pages/ComponentsPage'
import TasksPage from './pages/TasksPage'
import SchedulesPage from './pages/SchedulesPage'
import ReportsPage from './pages/ReportsPage'
import UsersPage from './pages/UsersPage'
import AuditPage from './pages/AuditPage'
import NotificationsPage from './pages/NotificationsPage'
import AgentPage from './pages/AgentPage'

function Guard({ children, perm }: { children: ReactNode; perm?: string }) {
  const { user, loading, can } = useAuth()
  if (loading) return <div className="p-10 text-slate-500">Loading…</div>
  if (!user) return <Navigate to="/login" replace />
  if (perm && !can(perm)) return <Navigate to="/" replace />
  return children
}

export default function App() {
  const { user, loading } = useAuth()
  return (
    <Routes>
      <Route path="/login" element={loading ? null : user ? <Navigate to="/" replace /> : <LoginPage />} />
      <Route path="/" element={<Guard><AppLayout /></Guard>}>
        <Route index element={<DashboardPage />} />
        <Route path="activities" element={<ActivitiesPage />} />
        <Route path="activities/:id" element={<ActivityDetailPage />} />
        <Route path="queue" element={<Guard perm="ACTIVITY_WORK"><QueuePage /></Guard>} />
        <Route path="aircraft" element={<AircraftPage />} />
        <Route path="components" element={<ComponentsPage />} />
        <Route path="tasks" element={<TasksPage />} />
        <Route path="schedules" element={<SchedulesPage />} />
        <Route path="reports" element={<Guard perm="REPORT_VIEW"><ReportsPage /></Guard>} />
        <Route path="agent" element={<Guard perm="ACTIVITY_VIEW"><AgentPage /></Guard>} />
        <Route path="users" element={<Guard perm="USER_MANAGE"><UsersPage /></Guard>} />
        <Route path="audit" element={<Guard perm="AUDIT_VIEW"><AuditPage /></Guard>} />
        <Route path="notifications" element={<NotificationsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
