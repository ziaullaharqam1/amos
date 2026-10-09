import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { Empty, ErrorBanner, HelpTip, PageHeader, PriorityBadge, StateBadge } from '../components/Ui'
import type { DashboardMetrics } from '../types'
import { errorMessage } from '../types'

export default function DashboardPage() {
  const [data, setData] = useState<DashboardMetrics | null>(null)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    api<DashboardMetrics>('/api/dashboard').then(setData).catch((e) => setError(errorMessage(e)))
  }, [])

  if (error) return <ErrorBanner error={error} />
  if (!data) return <p className="text-slate-500">Loading dashboard…</p>

  const cards = [
    { label: 'Open work', value: data.openActivities, hint: 'Active pipeline' },
    { label: 'In progress', value: data.inProgress, hint: 'On the hangar floor' },
    { label: 'Overdue', value: data.overdue, hint: 'Needs escalation' },
    { label: 'Awaiting QA', value: data.awaitingQa, hint: 'Ready to verify' },
  ]

  return (
    <div>
      <PageHeader title="Operations dashboard" subtitle={<>Live status of the maintenance programme. <HelpTip text="Open work is anything not verified or cancelled. AOG items are aircraft-on-ground and should be assigned first." /></>} />
      <div className="mb-6 grid grid-cols-2 gap-3 lg:grid-cols-4">
        {cards.map((c) => (
          <div key={c.label} className="card p-4">
            <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">{c.label}</div>
            <div className="mt-1 font-display text-3xl font-bold text-navy-900">{c.value}</div>
            <div className="text-xs text-slate-400">{c.hint}</div>
          </div>
        ))}
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <section className="card p-4">
          <h2 className="mb-3 font-display font-bold text-navy-900">AOG / high priority</h2>
          {data.aog.length ? data.aog.map((a) => (
            <button key={a.id} type="button" onClick={() => navigate(`/activities/${a.id}`)} className="mb-2 flex w-full items-center justify-between rounded-lg border border-slate-100 px-3 py-2 text-left hover:bg-slate-50">
              <div>
                <div className="text-sm font-semibold">{a.activityNumber} · {a.aircraftRegistration}</div>
                <div className="text-xs text-slate-500">{a.title}</div>
              </div>
              <PriorityBadge priority={a.priority} />
            </button>
          )) : <Empty title="No AOG items" hint="Aircraft-on-ground events will appear here." />}
        </section>
        <section className="card p-4">
          <h2 className="mb-3 font-display font-bold text-navy-900">Due this week</h2>
          {data.dueSoon.length ? data.dueSoon.map((a) => (
            <button key={a.id} type="button" onClick={() => navigate(`/activities/${a.id}`)} className="mb-2 flex w-full items-center justify-between rounded-lg border border-slate-100 px-3 py-2 text-left hover:bg-slate-50">
              <div>
                <div className="text-sm font-semibold">{a.activityNumber} · {a.aircraftRegistration}</div>
                <div className="text-xs text-slate-500">{a.title}</div>
              </div>
              <StateBadge state={a.state} />
            </button>
          )) : <Empty title="Nothing due soon" />}
        </section>
      </div>
    </div>
  )
}
