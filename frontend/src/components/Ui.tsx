import type { ReactNode } from 'react'

const STATE_STYLES: Record<string, string> = {
  PENDING: 'bg-slate-100 text-slate-700',
  ASSIGNED: 'bg-sky-100 text-sky-800',
  IN_PROGRESS: 'bg-amber-100 text-amber-800',
  ON_HOLD: 'bg-orange-100 text-orange-800',
  COMPLETED: 'bg-indigo-100 text-indigo-800',
  VERIFIED: 'bg-emerald-100 text-emerald-800',
  REJECTED: 'bg-rose-100 text-rose-800',
  CANCELLED: 'bg-slate-200 text-slate-600',
  ESCALATED: 'bg-red-100 text-red-800',
}

const PRIORITY_STYLES: Record<string, string> = {
  LOW: 'bg-slate-100 text-slate-600',
  NORMAL: 'bg-slate-100 text-slate-700',
  HIGH: 'bg-amber-100 text-amber-800',
  AOG: 'bg-red-600 text-white',
}

export function StateBadge({ state }: { state?: string | null }) {
  return (
    <span className={`badge ${STATE_STYLES[state ?? ''] ?? 'bg-slate-100 text-slate-700'}`}>
      {state?.replaceAll('_', ' ')}
    </span>
  )
}

export function PriorityBadge({ priority }: { priority?: string | null }) {
  return <span className={`badge ${PRIORITY_STYLES[priority ?? ''] ?? 'bg-slate-100'}`}>{priority}</span>
}

export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: ReactNode; actions?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <h1 className="font-display text-2xl font-bold text-navy-900">{title}</h1>
        {subtitle && <div className="mt-1 text-sm text-slate-500">{subtitle}</div>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  )
}

export function Empty({ title, hint }: { title: string; hint?: string }) {
  return (
    <div className="card p-10 text-center text-slate-500">
      <p className="font-semibold text-slate-700">{title}</p>
      {hint && <p className="mt-1 text-sm">{hint}</p>}
    </div>
  )
}

export function ErrorBanner({ error }: { error?: string | null }) {
  if (!error) return null
  return <div className="mb-4 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-800">{error}</div>
}

export function HelpTip({ text }: { text: string }) {
  return (
    <span className="group relative ml-1 inline-flex align-middle">
      <span className="flex h-4 w-4 cursor-help items-center justify-center rounded-full bg-slate-200 text-[10px] font-bold text-slate-600">?</span>
      <span className="pointer-events-none absolute left-0 top-6 z-20 hidden w-64 rounded-lg bg-navy-900 px-3 py-2 text-xs font-normal leading-snug text-white shadow-lg sm:left-1/2 sm:-translate-x-1/2 group-hover:block group-focus-within:block">
        {text}
      </span>
    </span>
  )
}

export function Modal({ title, children, onClose }: { title: string; children: ReactNode; onClose: () => void }) {
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-navy-950/40 p-4 sm:items-center">
      <div className="card w-full max-w-lg p-5">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="font-display text-lg font-bold text-navy-900">{title}</h2>
          <button className="btn-ghost px-2" onClick={onClose} type="button">Close</button>
        </div>
        {children}
      </div>
    </div>
  )
}
