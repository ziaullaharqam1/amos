import { useState } from 'react'
import { api } from '../api/client'
import { ErrorBanner, PageHeader } from '../components/Ui'
import { errorMessage } from '../types'

export default function ReportsPage() {
  const [text, setText] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function generate() {
    setBusy(true)
    setError('')
    try {
      setText(await api<string>('/api/reports/compliance'))
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  function download() {
    const blob = new Blob([text], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `amos-compliance-${new Date().toISOString().slice(0, 10)}.txt`
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div>
      <PageHeader title="Compliance reports" subtitle="Snapshot of open work, overdue items, and workflow states"
        actions={
          <>
            <button className="btn-primary" type="button" onClick={() => void generate()} disabled={busy}>{busy ? 'Generating…' : 'Generate'}</button>
            {text && <button className="btn-secondary" type="button" onClick={download}>Download</button>}
          </>
        } />
      <ErrorBanner error={error} />
      <pre className="card max-h-[70vh] overflow-auto whitespace-pre-wrap p-4 font-mono text-xs text-slate-700">
        {text || 'Generate a report to review current airworthiness workload.'}
      </pre>
    </div>
  )
}
