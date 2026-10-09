import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { ErrorBanner, PageHeader, PriorityBadge, StateBadge } from '../components/Ui'
import type { ActivityDetail, UserView } from '../types'
import { errorMessage } from '../types'

export default function ActivityDetailPage() {
  const { id } = useParams()
  const [item, setItem] = useState<ActivityDetail | null>(null)
  const [error, setError] = useState('')
  const [comment, setComment] = useState('')
  const [log, setLog] = useState({ findings: '', actionsTaken: '', hoursSpent: '' })
  const [users, setUsers] = useState<UserView[]>([])
  const [assignee, setAssignee] = useState('')
  const { can } = useAuth()
  const navigate = useNavigate()

  async function load() {
    try {
      setItem(await api<ActivityDetail>(`/api/activities/${id}`))
    } catch (e) {
      setError(errorMessage(e))
    }
  }
  useEffect(() => { void load() }, [id])
  useEffect(() => {
    if (can('USER_MANAGE') || can('ACTIVITY_ASSIGN')) {
      api<UserView[]>('/api/directory').then(setUsers).catch(() => {})
    }
  }, [])

  async function transition(toState: string) {
    try {
      setItem(await api<ActivityDetail>(`/api/activities/${id}/transition`, { method: 'POST', body: JSON.stringify({ toState, comment }) }))
      setComment('')
    } catch (e) {
      setError(errorMessage(e))
    }
  }
  async function assign() {
    try {
      setItem(await api<ActivityDetail>(`/api/activities/${id}/assign`, { method: 'POST', body: JSON.stringify({ userId: Number(assignee), roleOnJob: 'LEAD' }) }))
    } catch (e) {
      setError(errorMessage(e))
    }
  }
  async function addLog(e: FormEvent) {
    e.preventDefault()
    try {
      setItem(await api<ActivityDetail>(`/api/activities/${id}/logs`, {
        method: 'POST',
        body: JSON.stringify({ ...log, hoursSpent: log.hoursSpent ? Number(log.hoursSpent) : null }),
      }))
      setLog({ findings: '', actionsTaken: '', hoursSpent: '' })
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  if (!item) return error ? <ErrorBanner error={error} /> : <p>Loading…</p>

  return (
    <div>
      <PageHeader title={`${item.activityNumber} · ${item.aircraftRegistration}`}
        subtitle={item.title}
        actions={<div className="flex items-center gap-2"><PriorityBadge priority={item.priority} /><StateBadge state={item.state} /></div>} />
      <ErrorBanner error={error} />
      <div className="grid gap-4 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          <section className="card p-4">
            <h2 className="mb-2 font-display font-bold">Job card</h2>
            <p className="text-sm text-slate-600">{item.description || 'No additional description.'}</p>
            <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
              <div><dt className="text-xs text-slate-500">Check</dt><dd>{item.checkType || '—'}</dd></div>
              <div><dt className="text-xs text-slate-500">Task card</dt><dd>{item.taskCard || '—'}</dd></div>
              <div><dt className="text-xs text-slate-500">Station</dt><dd>{item.station || '—'}</dd></div>
              <div><dt className="text-xs text-slate-500">Due</dt><dd>{item.dueAt ? new Date(item.dueAt).toLocaleString() : '—'}</dd></div>
              <div><dt className="text-xs text-slate-500">Assignee</dt><dd>{item.assignedTo || 'Unassigned'}</dd></div>
              <div><dt className="text-xs text-slate-500">Requested by</dt><dd>{item.requestedBy || '—'}</dd></div>
            </dl>
          </section>
          {can('ACTIVITY_WORK') && (
            <form className="card space-y-3 p-4" onSubmit={addLog}>
              <h2 className="font-display font-bold">Log findings</h2>
              <textarea className="input" rows={3} placeholder="Findings" value={log.findings} onChange={(e) => setLog({ ...log, findings: e.target.value })} />
              <textarea className="input" rows={3} placeholder="Actions taken" value={log.actionsTaken} onChange={(e) => setLog({ ...log, actionsTaken: e.target.value })} />
              <input className="input" placeholder="Hours spent" value={log.hoursSpent} onChange={(e) => setLog({ ...log, hoursSpent: e.target.value })} />
              <button className="btn-primary">Save log</button>
            </form>
          )}
          <section className="card p-4">
            <h2 className="mb-3 font-display font-bold">Work log</h2>
            {item.logs.length ? item.logs.map((l) => (
              <div key={l.id} className="mb-3 border-b border-slate-100 pb-3 text-sm last:border-0">
                <div className="font-semibold">{l.author} · {l.logType}</div>
                <div className="text-xs text-slate-400">{new Date(l.createdAt).toLocaleString()}</div>
                <p className="mt-1">{l.findings}</p>
                <p className="text-slate-600">{l.actionsTaken}</p>
              </div>
            )) : <p className="text-sm text-slate-500">No logs yet.</p>}
          </section>
        </div>
        <div className="space-y-4">
          <section className="card p-4">
            <h2 className="mb-3 font-display font-bold">Workflow</h2>
            <p className="mb-2 text-xs text-slate-500">Allowed next steps for your role.</p>
            <div className="flex flex-wrap gap-2">
              {item.allowedTransitions.map((s) => (
                <button key={s} className="btn-secondary text-xs" type="button" onClick={() => void transition(s)}>{s.replaceAll('_', ' ')}</button>
              ))}
            </div>
            <textarea className="input mt-3" rows={2} placeholder="Transition comment" value={comment} onChange={(e) => setComment(e.target.value)} />
          </section>
          {can('ACTIVITY_ASSIGN') && (
            <section className="card p-4">
              <h2 className="mb-3 font-display font-bold">Assign / reassign</h2>
              <select className="input mb-2" value={assignee} onChange={(e) => setAssignee(e.target.value)}>
                <option value="">Select technician</option>
                {users.map((u) => <option key={u.id} value={u.id}>{u.fullName} ({u.username})</option>)}
              </select>
              <button className="btn-primary w-full" disabled={!assignee} type="button" onClick={() => void assign()}>Assign</button>
              <p className="mt-2 text-xs text-slate-500">Use this for escalation recovery and shift hand-over.</p>
            </section>
          )}
          <section className="card p-4">
            <h2 className="mb-3 font-display font-bold">History</h2>
            {item.history.map((h) => (
              <div key={h.id} className="mb-2 text-xs">
                <span className="font-semibold">{h.fromState} → {h.toState}</span>
                <div className="text-slate-500">{h.actor} · {new Date(h.createdAt).toLocaleString()}</div>
                {h.comment && <div>{h.comment}</div>}
              </div>
            ))}
          </section>
          {item.children.length > 0 && (
            <section className="card p-4">
              <h2 className="mb-3 font-display font-bold">Parallel tasks</h2>
              {item.children.map((c) => (
                <button key={c.id} type="button" className="mb-2 block w-full rounded-lg border px-3 py-2 text-left text-sm" onClick={() => navigate(`/activities/${c.id}`)}>
                  {c.activityNumber} · {c.title} <StateBadge state={c.state} />
                </button>
              ))}
            </section>
          )}
        </div>
      </div>
    </div>
  )
}
