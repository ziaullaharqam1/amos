import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { Empty, ErrorBanner, PageHeader } from '../components/Ui'
import type { NotificationView } from '../types'
import { errorMessage } from '../types'

export default function NotificationsPage() {
  const [items, setItems] = useState<NotificationView[]>([])
  const [error, setError] = useState('')
  const navigate = useNavigate()

  async function load() {
    setItems(await api<NotificationView[]>('/api/notifications'))
  }
  useEffect(() => { void load().catch((e) => setError(errorMessage(e))) }, [])

  async function read(id: number) {
    await api(`/api/notifications/${id}/read`, { method: 'POST' })
    await load()
  }

  async function readAll() {
    await api('/api/notifications/read-all', { method: 'POST' })
    await load()
  }

  return (
    <div>
      <PageHeader title="Notification center" subtitle="Assignments, due reminders, and workflow changes"
        actions={<button className="btn-secondary" type="button" onClick={() => void readAll()}>Mark all read</button>} />
      <ErrorBanner error={error} />
      {items.length === 0 ? <Empty title="Inbox is empty" hint="Kafka-backed events land here as in-app messages." /> : (
        <div className="space-y-2">
          {items.map((n) => (
            <button key={n.id} type="button" className={`card w-full p-4 text-left ${n.readAt ? 'opacity-70' : 'ring-1 ring-accent-500/30'}`}
              onClick={async () => {
                if (!n.readAt) await read(n.id)
                if (n.entityType === 'MaintenanceActivity' && n.entityId) navigate(`/activities/${n.entityId}`)
              }}>
              <div className="flex items-start justify-between gap-3">
                <div>
                  <div className="text-sm font-semibold text-navy-900">{n.title}</div>
                  <div className="mt-1 text-sm text-slate-600">{n.body}</div>
                  <div className="mt-2 text-xs text-slate-400">{n.type} · {new Date(n.createdAt).toLocaleString()} · {n.deliveryStatus}</div>
                </div>
                {!n.readAt && <span className="badge bg-accent-500 text-navy-950">Unread</span>}
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
