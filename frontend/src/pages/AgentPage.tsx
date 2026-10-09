import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import { api } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { ErrorBanner, HelpTip, PageHeader } from '../components/Ui'
import type { AgentReply, AgentStatus, AgentStep } from '../types'
import { errorMessage } from '../types'

interface ChatTurn {
  role: 'user' | 'agent'
  text: string
  applied?: boolean
  steps?: AgentStep[]
}

function chipsFor(can: (code: string) => boolean) {
  const chips: string[] = ['What work is overdue or AOG?']
  if (can('ACTIVITY_CREATE')) {
    chips.push('Open a work order on A6-DREAM for pack 2 inop')
  }
  if (can('ACTIVITY_ASSIGN')) {
    chips.push('Assign pending jobs to tech1')
    chips.push('Escalate overdue work orders')
  }
  if (can('ACTIVITY_WORK')) {
    chips.push('Start my assigned work')
    chips.push('Complete in-progress jobs')
  }
  if (can('ACTIVITY_VERIFY')) {
    chips.push('Verify work waiting for QA')
    chips.push('Reject completed work that needs rework')
  }
  if (can('ACTIVITY_CREATE') && !can('ACTIVITY_WORK')) {
    chips.push('Show open AOG and pending defects')
  }
  return chips
}

export default function AgentPage() {
  const { user, can } = useAuth()
  const chips = useMemo(() => chipsFor(can), [can, user?.id])
  const [status, setStatus] = useState<AgentStatus | null>(null)
  const [message, setMessage] = useState('')
  const [apply, setApply] = useState(true)
  const [turns, setTurns] = useState<ChatTurn[]>([{
    role: 'agent',
    text: welcome(user?.fullName, user?.roles ?? []),
  }])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const endRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    api<AgentStatus>('/api/agent/status').then(setStatus).catch((e) => setError(errorMessage(e)))
  }, [])

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [turns, busy])

  async function ask(text: string) {
    const trimmed = text.trim()
    if (!trimmed || busy) return
    setBusy(true)
    setError('')
    setMessage('')
    setTurns((prev) => [...prev, { role: 'user', text: trimmed }])
    try {
      const reply = await api<AgentReply>('/api/agent/ask', {
        method: 'POST',
        body: JSON.stringify({
          message: trimmed,
          apply,
        }),
      })
      setTurns((prev) => [...prev, {
        role: 'agent',
        text: reply.answer,
        applied: reply.applied,
        steps: reply.steps,
      }])
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    void ask(message)
  }

  return (
    <div className="flex flex-col">
      <PageHeader
        title="Workflow agent"
        subtitle={<>Hangar coordinator for managers, technicians/engineers, and QA. Talk to it in plain language — it assigns work and moves status when you ask. <HelpTip text="Leave Apply on to actually assign and change status (still limited by your login). Uncheck for a preview. Follow-up in the chat, for example “assign that to tech1” or “go ahead”." /></>}
      />
      <ErrorBanner error={error} />
      {status && (
        <div className="mb-4 rounded-lg border border-slate-200 bg-white px-4 py-3 text-sm text-slate-600">
          {status.configured
            ? <>Ready as <span className="font-semibold text-navy-900">{status.model}</span>. Signed in as {user?.fullName} ({user?.roles.join(', ')}).</>
            : <>Set <code className="rounded bg-slate-100 px-1">GROK_PROVIDER=xai</code> to use Grok, or keep the free local coordinator.</>}
        </div>
      )}

      <div className="card mb-3 max-h-[28rem] overflow-y-auto p-4">
        <ol className="space-y-3">
          {turns.map((turn, i) => (
            <li key={i} className={`flex ${turn.role === 'user' ? 'justify-end' : 'justify-start'}`}>
              <div className={`max-w-[90%] rounded-2xl px-4 py-3 text-sm ${
                turn.role === 'user'
                  ? 'bg-navy-900 text-white'
                  : 'bg-slate-100 text-slate-800'
              }`}>
                <p className="whitespace-pre-wrap leading-relaxed">{turn.text}</p>
                {turn.role === 'agent' && turn.applied !== undefined && (
                  <p className="mt-2 text-xs text-slate-500">
                    {turn.applied ? 'Changes were written to the hangar board.' : 'Preview only — nothing was changed.'}
                  </p>
                )}
                {turn.steps && turn.steps.length > 0 && (
                  <details className="mt-2 text-xs text-slate-600">
                    <summary className="cursor-pointer font-semibold">What I did</summary>
                    <ol className="mt-2 space-y-1">
                      {turn.steps.map((step, s) => (
                        <li key={`${step.tool}-${s}`}>
                          {s + 1}. {step.arguments || step.tool}
                          {step.result ? ` — ${step.result}` : ''}
                        </li>
                      ))}
                    </ol>
                  </details>
                )}
              </div>
            </li>
          ))}
          {busy && (
            <li className="text-sm text-slate-500">Looking at the live work orders…</li>
          )}
        </ol>
        <div ref={endRef} />
      </div>

      <div className="mb-3 flex flex-wrap gap-2">
        {chips.map((ex) => (
          <button key={ex} type="button" className="btn-ghost text-xs" disabled={busy} onClick={() => void ask(ex)}>{ex}</button>
        ))}
      </div>

      <form onSubmit={onSubmit} className="card p-4">
        <label className="mb-2 block text-sm font-semibold text-navy-900">Your next instruction</label>
        <textarea
          className="mb-3 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm"
          rows={3}
          placeholder="e.g. Assign WO-1002 to tech1, then start it"
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault()
              void ask(message)
            }
          }}
        />
        <label className="mb-4 flex items-center gap-2 text-sm">
          <input type="checkbox" checked={apply} onChange={(e) => setApply(e.target.checked)} />
          Apply assignments, new work orders, and status changes (uncheck to preview)
        </label>
        <button className="btn-primary" type="submit" disabled={busy || !message.trim()}>{busy ? 'Working…' : 'Send'}</button>
      </form>
    </div>
  )
}

function welcome(name: string | undefined, roles: string[]) {
  const who = name ? `Hi ${name.split(' ')[0]}.` : 'Hi.'
  const duties: string[] = []
  if (roles.some((r) => ['MAINTENANCE_MANAGER', 'SUPERVISOR', 'ADMIN', 'FLIGHT_ENGINEER'].includes(r))) {
    duties.push('open new work orders')
  }
  if (roles.some((r) => ['MAINTENANCE_MANAGER', 'SUPERVISOR', 'ADMIN'].includes(r))) {
    duties.push('assign pending jobs', 'escalate overdue or AOG work')
  }
  if (roles.some((r) => ['MAINTENANCE_TECHNICIAN', 'FLIGHT_ENGINEER', 'SUPERVISOR', 'ADMIN', 'MAINTENANCE_MANAGER'].includes(r))) {
    duties.push('start assigned cards', 'complete work for QA')
  }
  if (roles.some((r) => ['QUALITY_ASSURANCE', 'ADMIN'].includes(r))) {
    duties.push('verify completed jobs', 'send work back for rework')
  }
  if (roles.includes('FLIGHT_ENGINEER')) {
    duties.push('review AOG and pending defects')
  }
  const able = duties.length ? ` I can ${duties.join(', ')}.` : ''
  return `${who} I am the hangar coordinator.${able} Tell me what you want done, or tap a suggestion below.`
}
