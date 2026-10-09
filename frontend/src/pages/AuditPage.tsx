import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { ErrorBanner, PageHeader } from '../components/Ui'
import type { AuditView, PageResult } from '../types'
import { errorMessage } from '../types'

export default function AuditPage() {
  const [page, setPage] = useState<PageResult<AuditView>>({ content: [], number: 0, totalPages: 0 })
  const [entityType, setEntityType] = useState('')
  const [action, setAction] = useState('')
  const [error, setError] = useState('')

  async function load(p = 0) {
    const q = new URLSearchParams({ page: String(p), size: '25' })
    if (entityType) q.set('entityType', entityType)
    if (action) q.set('action', action)
    setPage(await api<PageResult<AuditView>>(`/api/audit-logs?${q}`))
  }
  useEffect(() => { void load(0).catch((e) => setError(errorMessage(e))) }, [entityType, action])

  return (
    <div>
      <PageHeader title="Audit trail" subtitle="Read-only history of create, update, assign, and approve actions" />
      <ErrorBanner error={error} />
      <div className="mb-4 flex flex-wrap gap-2">
        <input className="input max-w-xs" placeholder="Entity type" value={entityType} onChange={(e) => setEntityType(e.target.value)} />
        <input className="input max-w-xs" placeholder="Action" value={action} onChange={(e) => setAction(e.target.value)} />
      </div>
      <div className="card overflow-x-auto">
        <table className="w-full min-w-[900px] text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase text-slate-500">
            <tr><th className="px-4 py-3">When</th><th>Actor</th><th>Action</th><th>Entity</th><th>Before</th><th>After</th></tr>
          </thead>
          <tbody>
            {page.content.map((r) => (
              <tr key={r.id} className="border-t align-top">
                <td className="whitespace-nowrap px-4 py-3 text-xs">{new Date(r.createdAt).toLocaleString()}</td>
                <td>{r.actorUsername}</td>
                <td>{r.action}</td>
                <td>{r.entityType} #{r.entityId}</td>
                <td className="max-w-xs truncate font-mono text-[11px]">{r.beforeValue}</td>
                <td className="max-w-xs truncate font-mono text-[11px]">{r.afterValue}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="mt-3 flex gap-2">
        <button className="btn-secondary" type="button" disabled={page.number <= 0} onClick={() => void load(page.number - 1)}>Previous</button>
        <button className="btn-secondary" type="button" disabled={page.number + 1 >= page.totalPages} onClick={() => void load(page.number + 1)}>Next</button>
      </div>
    </div>
  )
}
