import { useEffect, useState, type DragEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { ErrorBanner, HelpTip, PageHeader, PriorityBadge } from '../components/Ui'
import type { ActivitySummary } from '../types'
import { errorMessage } from '../types'

const COLUMNS = [
  { state: 'ASSIGNED', label: 'Assigned' },
  { state: 'IN_PROGRESS', label: 'In progress' },
  { state: 'ON_HOLD', label: 'On hold' },
  { state: 'COMPLETED', label: 'Completed' },
] as const

export default function QueuePage() {
  const [items, setItems] = useState<ActivitySummary[]>([])
  const [error, setError] = useState('')
  const [dragging, setDragging] = useState<number | null>(null)
  const navigate = useNavigate()

  async function refresh() {
    setItems(await api<ActivitySummary[]>('/api/activities/queue'))
  }

  useEffect(() => {
    void refresh().catch((e) => setError(errorMessage(e)))
  }, [])

  async function move(activityId: number, toState: string) {
    const current = items.find((i) => i.id === activityId)
    if (!current || current.state === toState) return
    try {
      await api(`/api/activities/${activityId}/transition`, {
        method: 'POST',
        body: JSON.stringify({ toState, comment: 'Moved from work queue' }),
      })
      await refresh()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  function onDrop(column: string, event: DragEvent) {
    event.preventDefault()
    const id = Number(event.dataTransfer.getData('text/plain') || dragging)
    if (id) void move(id, column)
    setDragging(null)
  }

  return (
    <div>
      <PageHeader
        title="My work queue"
        subtitle={
          <>
            Drag cards between columns or use quick actions.
            <HelpTip text="Technicians can start, hold, or complete assigned jobs. Drop a card on a column to change its workflow state. Open a card for findings and history." />
          </>
        }
      />
      <ErrorBanner error={error} />
      <div className="grid gap-3 lg:grid-cols-4">
        {COLUMNS.map((col) => {
          const cards = items.filter((i) => i.state === col.state)
          return (
            <section
              key={col.state}
              className="min-h-[220px] rounded-xl border border-dashed border-slate-200 bg-slate-50/80 p-3"
              onDragOver={(e) => e.preventDefault()}
              onDrop={(e) => onDrop(col.state, e)}
            >
              <h2 className="mb-3 flex items-center justify-between text-sm font-semibold text-navy-900">
                {col.label}
                <span className="rounded-full bg-white px-2 text-xs text-slate-500">{cards.length}</span>
              </h2>
              <div className="space-y-2">
                {cards.map((item) => (
                  <article
                    key={item.id}
                    draggable
                    onDragStart={(e) => {
                      setDragging(item.id)
                      e.dataTransfer.setData('text/plain', String(item.id))
                    }}
                    className="card cursor-grab p-3 active:cursor-grabbing"
                  >
                    <button type="button" className="w-full text-left" onClick={() => navigate(`/activities/${item.id}`)}>
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-sm font-semibold">{item.activityNumber}</span>
                        <PriorityBadge priority={item.priority} />
                      </div>
                      <div className="mt-1 text-xs text-slate-600">{item.aircraftRegistration} · {item.title}</div>
                      <div className="mt-1 text-[11px] text-slate-400">Due {item.dueAt ? new Date(item.dueAt).toLocaleString() : 'n/a'}</div>
                    </button>
                    <div className="mt-2 flex flex-wrap gap-1">
                      {item.state === 'ASSIGNED' && <button type="button" className="btn-primary text-[11px] px-2 py-1" onClick={(e) => { e.stopPropagation(); void move(item.id, 'IN_PROGRESS') }}>Start</button>}
                      {item.state === 'IN_PROGRESS' && <button type="button" className="btn-primary text-[11px] px-2 py-1" onClick={(e) => { e.stopPropagation(); void move(item.id, 'COMPLETED') }}>Complete</button>}
                      {item.state === 'IN_PROGRESS' && <button type="button" className="btn-secondary text-[11px] px-2 py-1" onClick={(e) => { e.stopPropagation(); void move(item.id, 'ON_HOLD') }}>Hold</button>}
                      {item.state === 'ON_HOLD' && <button type="button" className="btn-primary text-[11px] px-2 py-1" onClick={(e) => { e.stopPropagation(); void move(item.id, 'IN_PROGRESS') }}>Resume</button>}
                    </div>
                  </article>
                ))}
              </div>
            </section>
          )
        })}
      </div>
    </div>
  )
}
