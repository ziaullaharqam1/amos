import { Fragment, useEffect, useMemo, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { Empty, ErrorBanner, HelpTip, Modal, PageHeader, PriorityBadge, StateBadge } from '../components/Ui'
import type { ActivityDetail, ActivitySummary, AircraftView, CheckTypeView, TaskView } from '../types'
import { errorMessage } from '../types'

interface ActivityForm {
  title: string
  description: string
  aircraftId: string
  checkTypeId: string
  taskId: string
  priority: string
  dueAt: string
  station: string
  parentId: string
}

export default function ActivitiesPage() {
  const [items, setItems] = useState<ActivitySummary[]>([])
  const [error, setError] = useState('')
  const [state, setState] = useState('')
  const [open, setOpen] = useState(false)
  const [aircraft, setAircraft] = useState<AircraftView[]>([])
  const [checks, setChecks] = useState<CheckTypeView[]>([])
  const [tasks, setTasks] = useState<TaskView[]>([])
  const [form, setForm] = useState<ActivityForm>({
    title: '', description: '', aircraftId: '', checkTypeId: '', taskId: '',
    priority: 'NORMAL', dueAt: '', station: 'DXB', parentId: '',
  })
  const { can } = useAuth()
  const navigate = useNavigate()

  async function load() {
    try {
      const q = state ? `?state=${state}` : ''
      setItems(await api<ActivitySummary[]>(`/api/activities${q}`))
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  useEffect(() => { void load() }, [state])
  useEffect(() => {
    Promise.all([
      api<AircraftView[]>('/api/aircraft'),
      api<CheckTypeView[]>('/api/check-types'),
      api<TaskView[]>('/api/tasks'),
    ]).then(([a, c, t]) => { setAircraft(a); setChecks(c); setTasks(t) }).catch(() => {})
  }, [])

  const grouped = useMemo(() => {
    const parents = items.filter((i) => !i.parentId)
    const children = items.filter((i) => i.parentId)
    return parents.map((p) => ({ ...p, kids: children.filter((c) => c.parentId === p.id) }))
  }, [items])

  async function create(e: FormEvent) {
    e.preventDefault()
    try {
      const created = await api<ActivityDetail>('/api/activities', {
        method: 'POST',
        body: JSON.stringify({
          ...form,
          aircraftId: Number(form.aircraftId),
          checkTypeId: form.checkTypeId ? Number(form.checkTypeId) : null,
          taskId: form.taskId ? Number(form.taskId) : null,
          parentId: form.parentId ? Number(form.parentId) : null,
          dueAt: form.dueAt ? new Date(form.dueAt).toISOString() : null,
        }),
      })
      setOpen(false)
      navigate(`/activities/${created.id}`)
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  return (
    <div>
      <PageHeader title="Work orders" subtitle={<>Schedule, assign, and track maintenance from request to QA release. <HelpTip text="Create a parent work order for a check visit, then add child jobs for parallel trades. Status filters apply to the full list." /></>}
        actions={can('ACTIVITY_CREATE') ? <button className="btn-primary" type="button" onClick={() => setOpen(true)}>New work order</button> : null} />
      <ErrorBanner error={error} />
      <div className="mb-4 flex gap-2 overflow-x-auto">
        {['', 'PENDING', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'ESCALATED'].map((s) => (
          <button key={s || 'all'} type="button" onClick={() => setState(s)} className={`rounded-full px-3 py-1 text-sm ${state === s ? 'bg-navy-900 text-white' : 'bg-white border text-slate-600'}`}>
            {s || 'All'}
          </button>
        ))}
      </div>
      <div className="card overflow-hidden">
        {grouped.length === 0 ? <Empty title="No work orders" /> : (
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-3 py-2">WO</th>
                <th className="px-3 py-2">Aircraft</th>
                <th className="px-3 py-2">Title</th>
                <th className="px-3 py-2">State</th>
                <th className="px-3 py-2">Priority</th>
                <th className="px-3 py-2">Assignee</th>
              </tr>
            </thead>
            <tbody>
              {grouped.map((row) => (
                <Fragment key={row.id}>
                  <tr onClick={() => navigate(`/activities/${row.id}`)} className="cursor-pointer border-t hover:bg-slate-50">
                    <td className="px-3 py-2 font-semibold">{row.activityNumber}</td>
                    <td className="px-3 py-2">{row.aircraftRegistration}</td>
                    <td className="px-3 py-2">{row.title}</td>
                    <td className="px-3 py-2"><StateBadge state={row.state} /></td>
                    <td className="px-3 py-2"><PriorityBadge priority={row.priority} /></td>
                    <td className="px-3 py-2">{row.assignedTo || '—'}</td>
                  </tr>
                  {row.kids.map((k) => (
                    <tr key={k.id} onClick={() => navigate(`/activities/${k.id}`)} className="cursor-pointer border-t bg-slate-50/60 hover:bg-slate-100">
                      <td className="px-3 py-2 pl-8 text-slate-500">{k.activityNumber}</td>
                      <td className="px-3 py-2">{k.aircraftRegistration}</td>
                      <td className="px-3 py-2">↳ {k.title}</td>
                      <td className="px-3 py-2"><StateBadge state={k.state} /></td>
                      <td className="px-3 py-2"><PriorityBadge priority={k.priority} /></td>
                      <td className="px-3 py-2">{k.assignedTo || '—'}</td>
                    </tr>
                  ))}
                </Fragment>
              ))}
            </tbody>
          </table>
        )}
      </div>
      {open && (
        <Modal title="Schedule maintenance" onClose={() => setOpen(false)}>
          <form onSubmit={create} className="space-y-3">
            <div>
              <label className="label">Title</label>
              <input className="input" required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
            </div>
            <div>
              <label className="label">Aircraft</label>
              <select className="input" required value={form.aircraftId} onChange={(e) => setForm({ ...form, aircraftId: e.target.value })}>
                <option value="">Select</option>
                {aircraft.map((a) => <option key={a.id} value={a.id}>{a.registration}</option>)}
              </select>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="label">Check type</label>
                <select className="input" value={form.checkTypeId} onChange={(e) => setForm({ ...form, checkTypeId: e.target.value })}>
                  <option value="">—</option>
                  {checks.map((c) => <option key={c.id} value={c.id}>{c.code}</option>)}
                </select>
              </div>
              <div>
                <label className="label">Priority</label>
                <select className="input" value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value })}>
                  {['LOW', 'NORMAL', 'HIGH', 'AOG'].map((p) => <option key={p}>{p}</option>)}
                </select>
              </div>
            </div>
            <div>
              <label className="label">Task card</label>
              <select className="input" value={form.taskId} onChange={(e) => setForm({ ...form, taskId: e.target.value })}>
                <option value="">—</option>
                {tasks.map((t) => <option key={t.id} value={t.id}>{t.taskCard} · {t.title}</option>)}
              </select>
            </div>
            <div>
              <label className="label">Parallel child of (optional)</label>
              <select className="input" value={form.parentId} onChange={(e) => setForm({ ...form, parentId: e.target.value })}>
                <option value="">Standalone</option>
                {items.filter((i) => !i.parentId).map((i) => <option key={i.id} value={i.id}>{i.activityNumber}</option>)}
              </select>
            </div>
            <div>
              <label className="label">Due</label>
              <input type="datetime-local" className="input" value={form.dueAt} onChange={(e) => setForm({ ...form, dueAt: e.target.value })} />
            </div>
            <button className="btn-primary w-full">Create</button>
          </form>
        </Modal>
      )}
    </div>
  )
}
