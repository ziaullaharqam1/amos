import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../api/client'
import { ErrorBanner, HelpTip, Modal, PageHeader } from '../components/Ui'
import { useAuth } from '../auth/AuthContext'
import type { AircraftTypeView, CheckTypeView, TaskView } from '../types'
import { errorMessage } from '../types'

export default function TasksPage() {
  const { can } = useAuth()
  const [tasks, setTasks] = useState<TaskView[]>([])
  const [checks, setChecks] = useState<CheckTypeView[]>([])
  const [types, setTypes] = useState<AircraftTypeView[]>([])
  const [error, setError] = useState('')
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState({
    taskCard: '', title: '', description: '', checkTypeId: '', aircraftTypeId: '', ataChapter: '', estimatedHours: '', skill: 'LINE',
  })

  async function load() {
    const [t, c, at] = await Promise.all([
      api<TaskView[]>('/api/tasks'),
      api<CheckTypeView[]>('/api/check-types'),
      api<AircraftTypeView[]>('/api/aircraft-types'),
    ])
    setTasks(t); setChecks(c); setTypes(at)
  }
  useEffect(() => { void load().catch((e) => setError(errorMessage(e))) }, [])

  async function save(e: FormEvent) {
    e.preventDefault()
    try {
      await api('/api/tasks', {
        method: 'POST',
        body: JSON.stringify({
          ...form,
          checkTypeId: form.checkTypeId ? Number(form.checkTypeId) : null,
          aircraftTypeId: form.aircraftTypeId ? Number(form.aircraftTypeId) : null,
          estimatedHours: form.estimatedHours ? Number(form.estimatedHours) : null,
        }),
      })
      setOpen(false)
      await load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  return (
    <div>
      <PageHeader title="Task cards & checks" subtitle={<>ATA task definitions and A/B/C/D check types <HelpTip text="A/B/C/D checks are standard airframe visit packages. Task cards map to ATA chapters and estimated man-hours." /></>}
        actions={can('TASK_MANAGE') ? <button className="btn-primary" type="button" onClick={() => setOpen(true)}>Add task card</button> : null} />
      <ErrorBanner error={error} />
      <div className="mb-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {checks.map((c) => (
          <div key={c.id} className="card p-4">
            <div className="font-display text-lg font-bold text-navy-900">{c.code}</div>
            <div className="text-sm text-slate-600">{c.name}</div>
            <div className="mt-1 text-xs text-slate-400">{c.typicalDowntimeHours ? `${c.typicalDowntimeHours}h downtime` : 'Unscheduled'}</div>
          </div>
        ))}
      </div>
      <div className="card overflow-x-auto">
        <table className="w-full min-w-[720px] text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase text-slate-500">
            <tr><th className="px-4 py-3">Card</th><th>Title</th><th>Check</th><th>Type</th><th>ATA</th><th>Hours</th><th>Skill</th></tr>
          </thead>
          <tbody>
            {tasks.map((t) => (
              <tr key={t.id} className="border-t border-slate-100">
                <td className="px-4 py-3 font-mono text-xs">{t.taskCard}</td>
                <td>{t.title}</td>
                <td>{t.checkType || '—'}</td>
                <td>{t.aircraftType || 'ALL'}</td>
                <td>{t.ataChapter}</td>
                <td>{t.estimatedHours}</td>
                <td>{t.skill}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {open && (
        <Modal title="New task card" onClose={() => setOpen(false)}>
          <form onSubmit={save} className="grid gap-3">
            <input className="input" placeholder="Task card ID" value={form.taskCard} onChange={(e) => setForm({ ...form, taskCard: e.target.value })} required />
            <input className="input" placeholder="Title" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required />
            <textarea className="input" placeholder="Description" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
            <select className="input" value={form.checkTypeId} onChange={(e) => setForm({ ...form, checkTypeId: e.target.value })}>
              <option value="">Check type</option>
              {checks.map((c) => <option key={c.id} value={c.id}>{c.code} — {c.name}</option>)}
            </select>
            <select className="input" value={form.aircraftTypeId} onChange={(e) => setForm({ ...form, aircraftTypeId: e.target.value })}>
              <option value="">Any aircraft type</option>
              {types.map((t) => <option key={t.id} value={t.id}>{t.icaoCode}</option>)}
            </select>
            <div className="grid grid-cols-2 gap-2">
              <input className="input" placeholder="ATA" value={form.ataChapter} onChange={(e) => setForm({ ...form, ataChapter: e.target.value })} />
              <input className="input" placeholder="Est. hours" value={form.estimatedHours} onChange={(e) => setForm({ ...form, estimatedHours: e.target.value })} />
            </div>
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
