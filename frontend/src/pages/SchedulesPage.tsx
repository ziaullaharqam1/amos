import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { ErrorBanner, HelpTip, Modal, PageHeader } from '../components/Ui'
import { useAuth } from '../auth/AuthContext'
import type { ActivityDetail, AircraftView, CheckTypeView, ScheduleView, TaskView } from '../types'
import { errorMessage } from '../types'

export default function SchedulesPage() {
  const { can } = useAuth()
  const navigate = useNavigate()
  const [rows, setRows] = useState<ScheduleView[]>([])
  const [aircraft, setAircraft] = useState<AircraftView[]>([])
  const [tasks, setTasks] = useState<TaskView[]>([])
  const [checks, setChecks] = useState<CheckTypeView[]>([])
  const [error, setError] = useState('')
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState({
    aircraftId: '', taskId: '', checkTypeId: '', intervalHours: '', intervalDays: '', intervalCycles: '', nextDueAt: '', active: true,
  })

  async function load() {
    const [s, a, t, c] = await Promise.all([
      api<ScheduleView[]>('/api/schedules'),
      api<AircraftView[]>('/api/aircraft'),
      api<TaskView[]>('/api/tasks'),
      api<CheckTypeView[]>('/api/check-types'),
    ])
    setRows(s); setAircraft(a); setTasks(t); setChecks(c)
    setForm((f) => ({ ...f, aircraftId: String(a[0]?.id ?? ''), checkTypeId: String(c[0]?.id ?? '') }))
  }
  useEffect(() => { void load().catch((e) => setError(errorMessage(e))) }, [])

  async function save(e: FormEvent) {
    e.preventDefault()
    try {
      await api('/api/schedules', {
        method: 'POST',
        body: JSON.stringify({
          aircraftId: Number(form.aircraftId),
          taskId: form.taskId ? Number(form.taskId) : null,
          checkTypeId: form.checkTypeId ? Number(form.checkTypeId) : null,
          intervalHours: form.intervalHours ? Number(form.intervalHours) : null,
          intervalDays: form.intervalDays ? Number(form.intervalDays) : null,
          intervalCycles: form.intervalCycles ? Number(form.intervalCycles) : null,
          nextDueAt: form.nextDueAt ? new Date(form.nextDueAt).toISOString() : null,
          active: form.active,
        }),
      })
      setOpen(false)
      await load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  async function generate(id: number) {
    try {
      const wo = await api<ActivityDetail>(`/api/schedules/${id}/generate`, { method: 'POST' })
      navigate(`/activities/${wo.id}`)
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  return (
    <div>
      <PageHeader title="Recurring schedules" subtitle={<>Due on flight hours, calendar days, or cycles. <HelpTip text="Open work order creates a pending job for this interval. The next due date advances when QA verifies that job. A background job also opens due schedules automatically." /></>}
        actions={can('SCHEDULE_MANAGE') ? <button className="btn-primary" type="button" onClick={() => setOpen(true)}>Add schedule</button> : null} />
      <ErrorBanner error={error} />
      <div className="card overflow-x-auto">
        <table className="w-full min-w-[860px] text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase text-slate-500">
            <tr><th className="px-4 py-3">Aircraft</th><th>Task</th><th>Check</th><th>Interval</th><th>Next due</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.id} className="border-t border-slate-100">
                <td className="px-4 py-3 font-semibold">{r.aircraftRegistration}</td>
                <td>{r.taskCard || '—'}</td>
                <td>{r.checkType}</td>
                <td>{[r.intervalHours && `${r.intervalHours} FH`, r.intervalDays && `${r.intervalDays} days`, r.intervalCycles && `${r.intervalCycles} cyc`].filter(Boolean).join(' / ') || '—'}</td>
                <td>{r.nextDueAt ? new Date(r.nextDueAt).toLocaleString() : '—'}</td>
                <td>{r.active ? 'Active' : 'Inactive'}</td>
                <td className="px-3 py-2 text-right">
                  {can('SCHEDULE_MANAGE') && (
                    <button type="button" className="btn-secondary text-xs" onClick={() => void generate(r.id)}>Open work order</button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {open && (
        <Modal title="Recurring interval" onClose={() => setOpen(false)}>
          <form onSubmit={save} className="grid gap-3">
            <select className="input" value={form.aircraftId} onChange={(e) => setForm({ ...form, aircraftId: e.target.value })}>
              {aircraft.map((a) => <option key={a.id} value={a.id}>{a.registration}</option>)}
            </select>
            <select className="input" value={form.checkTypeId} onChange={(e) => setForm({ ...form, checkTypeId: e.target.value })}>
              {checks.map((c) => <option key={c.id} value={c.id}>{c.code}</option>)}
            </select>
            <select className="input" value={form.taskId} onChange={(e) => setForm({ ...form, taskId: e.target.value })}>
              <option value="">Task card (optional)</option>
              {tasks.map((t) => <option key={t.id} value={t.id}>{t.taskCard}</option>)}
            </select>
            <div className="grid grid-cols-3 gap-2">
              <input className="input" placeholder="Hours" value={form.intervalHours} onChange={(e) => setForm({ ...form, intervalHours: e.target.value })} />
              <input className="input" placeholder="Days" value={form.intervalDays} onChange={(e) => setForm({ ...form, intervalDays: e.target.value })} />
              <input className="input" placeholder="Cycles" value={form.intervalCycles} onChange={(e) => setForm({ ...form, intervalCycles: e.target.value })} />
            </div>
            <input type="datetime-local" className="input" value={form.nextDueAt} onChange={(e) => setForm({ ...form, nextDueAt: e.target.value })} />
            <div className="flex justify-end gap-2">
              <button type="button" className="btn-secondary" onClick={() => setOpen(false)}>Cancel</button>
              <button className="btn-primary">Save</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
