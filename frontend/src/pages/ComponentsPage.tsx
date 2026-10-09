import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../api/client'
import { ErrorBanner, Modal, PageHeader } from '../components/Ui'
import { useAuth } from '../auth/AuthContext'
import type { AircraftView, ComponentView } from '../types'
import { errorMessage } from '../types'

type ComponentForm = {
  partNumber: string
  serialNumber: string
  name: string
  category: string
  aircraftId: string
  status: string
}

const emptyForm: ComponentForm = {
  partNumber: '', serialNumber: '', name: '', category: 'ENGINE', aircraftId: '', status: 'INSTALLED',
}

export default function ComponentsPage() {
  const { can } = useAuth()
  const [rows, setRows] = useState<ComponentView[]>([])
  const [aircraft, setAircraft] = useState<AircraftView[]>([])
  const [error, setError] = useState('')
  const [editing, setEditing] = useState<ComponentView | null>(null)
  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState<ComponentForm>(emptyForm)

  async function load() {
    const [c, a] = await Promise.all([api<ComponentView[]>('/api/components'), api<AircraftView[]>('/api/aircraft')])
    setRows(c)
    setAircraft(a)
  }
  useEffect(() => { void load().catch((e) => setError(errorMessage(e))) }, [])

  function openCreate() {
    setForm({ ...emptyForm, aircraftId: String(aircraft[0]?.id ?? '') })
    setCreating(true)
    setEditing(null)
  }

  function openEdit(row: ComponentView) {
    setForm({
      partNumber: row.partNumber,
      serialNumber: row.serialNumber,
      name: row.name,
      category: row.category,
      aircraftId: String(row.aircraftId ?? ''),
      status: row.status,
    })
    setEditing(row)
    setCreating(false)
  }

  async function save(e: FormEvent) {
    e.preventDefault()
    try {
      const body = JSON.stringify({ ...form, aircraftId: Number(form.aircraftId) })
      if (editing) await api(`/api/components/${editing.id}`, { method: 'PUT', body })
      else await api('/api/components', { method: 'POST', body })
      setCreating(false)
      setEditing(null)
      await load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  const fields: Array<keyof Pick<ComponentForm, 'partNumber' | 'serialNumber' | 'name'>> = ['partNumber', 'serialNumber', 'name']
  const modalOpen = creating || editing !== null

  return (
    <div>
      <PageHeader title="Components" subtitle="Serialized parts with life limits"
        actions={can('COMPONENT_MANAGE') ? <button className="btn-primary" type="button" onClick={openCreate}>Add component</button> : null} />
      <ErrorBanner error={error} />
      <div className="card overflow-x-auto">
        <table className="w-full min-w-[700px] text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase text-slate-500"><tr><th className="px-4 py-3">P/N</th><th>S/N</th><th>Name</th><th>Aircraft</th><th>Status</th><th>Hours</th></tr></thead>
          <tbody>{rows.map((r) => (
            <tr key={r.id} className={`border-t ${can('COMPONENT_MANAGE') ? 'cursor-pointer hover:bg-slate-50' : ''}`} onClick={() => can('COMPONENT_MANAGE') && openEdit(r)}>
              <td className="px-4 py-3">{r.partNumber}</td><td>{r.serialNumber}</td><td>{r.name}</td><td>{r.aircraftRegistration}</td><td>{r.status}</td><td>{r.accumulatedHours}</td>
            </tr>
          ))}</tbody>
        </table>
      </div>
      {modalOpen && (
        <Modal title={editing ? 'Edit component' : 'Add component'} onClose={() => { setCreating(false); setEditing(null) }}>
          <form onSubmit={save} className="grid gap-3">
            {fields.map((k) => (
              <input key={k} className="input" placeholder={k} value={form[k]} onChange={(e) => setForm({ ...form, [k]: e.target.value })} required />
            ))}
            <select className="input" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
              {['ENGINE', 'LANDING_GEAR', 'AVIONICS', 'APU', 'OTHER'].map((c) => <option key={c}>{c}</option>)}
            </select>
            <select className="input" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
              {['INSTALLED', 'REMOVED', 'IN_REPAIR', 'QUARANTINE'].map((s) => <option key={s}>{s}</option>)}
            </select>
            <select className="input" value={form.aircraftId} onChange={(e) => setForm({ ...form, aircraftId: e.target.value })}>
              {aircraft.map((a) => <option key={a.id} value={a.id}>{a.registration}</option>)}
            </select>
            <div className="flex justify-end gap-2">
              <button type="button" className="btn-secondary" onClick={() => { setCreating(false); setEditing(null) }}>Cancel</button>
              <button className="btn-primary">Save</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
