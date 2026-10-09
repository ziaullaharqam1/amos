import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { useEffect, useState } from 'react'
import { api } from '../api/client'
import type { NotificationView } from '../types'

const links = [
  { to: '/', label: 'Dashboard', perm: 'DASHBOARD_VIEW' },
  { to: '/activities', label: 'Work orders', perm: 'ACTIVITY_VIEW' },
  { to: '/queue', label: 'My queue', perm: 'ACTIVITY_WORK' },
  { to: '/aircraft', label: 'Aircraft', perm: 'AIRCRAFT_VIEW' },
  { to: '/components', label: 'Components', perm: 'COMPONENT_VIEW' },
  { to: '/tasks', label: 'Task cards', perm: 'TASK_VIEW' },
  { to: '/schedules', label: 'Schedules', perm: 'SCHEDULE_VIEW' },
  { to: '/reports', label: 'Reports', perm: 'REPORT_VIEW' },
  { to: '/agent', label: 'Workflow agent', perm: 'ACTIVITY_VIEW' },
  { to: '/users', label: 'Users & roles', perm: 'USER_MANAGE' },
  { to: '/audit', label: 'Audit', perm: 'AUDIT_VIEW' },
] as const

export default function AppLayout() {
  const { user, can, logout } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [unread, setUnread] = useState(0)

  useEffect(() => {
    let alive = true
    async function load() {
      try {
        const items = await api<NotificationView[]>('/api/notifications')
        if (alive) setUnread(items.filter((n) => !n.readAt).length)
      } catch {
        /* ignore */
      }
    }
    void load()
    const t = setInterval(() => void load(), 20000)
    return () => {
      alive = false
      clearInterval(t)
    }
  }, [])

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="sticky top-0 z-40 border-b border-navy-800 bg-navy-900 text-white">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-3 px-4 py-3">
          <div className="flex items-center gap-3">
            <button className="rounded-lg p-2 lg:hidden" onClick={() => setOpen(!open)} aria-label="Menu" type="button">☰</button>
            <button onClick={() => navigate('/')} className="flex items-center gap-2" type="button">
              <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent-500 font-display text-sm font-bold text-navy-950">AM</span>
              <div className="text-left">
                <div className="font-display text-sm font-bold tracking-wide">AMOS</div>
                <div className="text-[11px] text-slate-300">Aircraft Maintenance</div>
              </div>
            </button>
          </div>
          <div className="flex items-center gap-2">
            <button onClick={() => navigate('/notifications')} className="relative rounded-lg px-3 py-1.5 text-sm hover:bg-navy-800" type="button">
              Inbox
              {unread > 0 && <span className="absolute -right-1 -top-1 rounded-full bg-accent-400 px-1.5 text-[10px] font-bold text-navy-950">{unread}</span>}
            </button>
            <div className="hidden text-right sm:block">
              <div className="text-sm font-semibold">{user?.fullName}</div>
              <div className="text-[11px] text-slate-300">{user?.roles.join(', ')} · {user?.station}</div>
            </div>
            <button className="rounded-lg bg-navy-800 px-3 py-1.5 text-sm" type="button" onClick={() => { logout(); navigate('/login') }}>Sign out</button>
          </div>
        </div>
      </header>
      <div className="mx-auto flex max-w-7xl">
        <aside className={`${open ? 'block' : 'hidden'} w-full border-b border-slate-200 bg-white lg:block lg:w-56 lg:border-b-0 lg:border-r`}>
          <nav className="flex flex-col p-3">
            {links.filter((l) => can(l.perm)).map((l) => (
              <NavLink key={l.to} to={l.to} end={l.to === '/'} onClick={() => setOpen(false)}
                className={({ isActive }) => `rounded-lg px-3 py-2 text-sm font-medium ${isActive ? 'bg-accent-500/15 text-navy-900' : 'text-slate-600 hover:bg-slate-100'}`}>
                {l.label}
              </NavLink>
            ))}
          </nav>
        </aside>
        <main className="min-w-0 flex-1 p-4 sm:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
