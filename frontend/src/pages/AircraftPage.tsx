import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../api/client'
import { ErrorBanner, HelpTip, Modal, PageHeader } from '../components/Ui'
import { useAuth } from '../auth/AuthContext'
import type { AircraftTypeView, AircraftView } from '../types'
import { errorMessage } from '../types'

const emptyAircraft = {
  registration: '', aircraftTypeId: '', serialNumber: '', status: 'IN_SERVICE',
  totalFlightHours: 0, totalCycles: 0, baseStation: 'DXB',
}

export default function AircraftPage() {
  const { can } = useAuth()
  const [tab, setTab] = useState<'fleet' | 'types'>('fleet')
  const [rows, setRows] = useState<AircraftView[]>([])
  const [types, setTypes] = useState<AircraftTypeView[]>([])
  const [error, setError] = useState('')
  const [editing, setEditing] = useState<AircraftView | null>(null)
  const [creating, setCreating] = useState(false)
  const [typeForm, setTypeForm] = useState({ icaoCode: '', manufacturer: '', model: '', description: '' })
  const [form, setForm] = useState(emptyAircraft)

  async function load() {
    const [a, t] = await Promise.all([api<AircraftView[]>('/api/aircraft'), api<AircraftTypeView[]>('/api/aircraft-types')])
    setRows(a)
    setTypes(t)
  }
  useEffect(() => { void load().catch((e) => setError(errorMessage(e))) }, [])

  function openCreate() {
    setForm({ ...emptyAircraft, aircraftTypeId: String(types[0]?.id ?? '') })
    setCreating(true)
    setEditing(null)
  }

  function openEdit(row: AircraftView) {
    setForm({
      registration: row.registration,
      aircraftTypeId: String(row.aircraftTypeId),
      serialNumber: row.serialNumber,
      status: row.status,
      totalFlightHours: row.totalFlightHours,
      totalCycles: row.totalCycles,
      baseStation: row.baseStation ?? 'DXB',
    })
    setEditing(row)
    setCreating(false)
  }

  async function save(e: FormEvent) {
    e.preventDefault()
    try {
      const payload = {
        ...form,
        aircraftTypeId: Number(form.aircraftTypeId),
        totalFlightHours: Number(form.totalFlightHours),
        totalCycles: Number(form.totalCycles),
      }
      if (editing) await api(`/api/aircraft/${editing.id}`, { method: 'PUT', body: JSON.stringify(payload) })
      else await api('/api/aircraft', { method: 'POST', body: JSON.stringify(payload) })
      setCreating(false)
      setEditing(null)
      await load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  async function saveType(e: FormEvent) {
    e.preventDefault()
    try {
      await api('/api/aircraft-types', { method: 'POST', body: JSON.stringify(typeForm) })
      setTypeForm({ icaoCode: '', manufacturer: '', model: '', description: '' })
      await load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  const modalOpen = creating || editing !== null

  return (
    <div>
      <PageHeader
        title="Aircraft fleet"
        subtitle={<>Master records used for planning and airworthiness tracking<HelpTip text="Keep registration, hours, and cycles current. Recurring schedules and work orders depend on this master data." /></>}
        actions={
          <>
            <div className="flex rounded-lg border bg-white p-0.5 text-sm">
              <button type="button" className={`rounded-md px-3 py-1 ${tab === 'fleet' ? 'bg-navy-900 text-white' : ''}`} onClick={() => setTab('fleet')}>Fleet</button>
              <button type="button" className={`rounded-md px-3 py-1 ${tab === 'types' ? 'bg-navy-900 text-white' : ''}`} onClick={() => setTab('types')}>Types</button>
            </div>
            {tab === 'fleet' && can('AIRCRAFT_MANAGE') && <button className="btn-primary" type="button" onClick={openCreate}>Add aircraft</button>}
          </>
        }
      />
      <ErrorBanner error={error} />
      {tab === 'fleet' ? (
        <div className="card overflow-x-auto">
          <table className="w-full min-w-[700px] text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase text-slate-500"><tr><th className="px-4 py-3">Reg</th><th>Type</th><th>MSN</th><th>Status</th><th>FH</th><th>Cycles</th><th>Base</th></tr></thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id} className={`border-t border-slate-100 ${can('AIRCRAFT_MANAGE') ? 'cursor-pointer hover:bg-slate-50' : ''}`} onClick={() => can('AIRCRAFT_MANAGE') && openEdit(r)}>
                  <td className="px-4 py-3 font-semibold">{r.registration}</td>
                  <td>{r.aircraftType}</td><td>{r.serialNumber}</td><td>{r.status}</td>
                  <td>{r.totalFlightHours}</td><td>{r.totalCycles}</td><td>{r.baseStation}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="grid gap-4 lg:grid-cols-2">
          <div className="card overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 text-xs uppercase text-slate-500"><tr><th className="px-4 py-3">ICAO</th><th>Manufacturer</th><th>Model</th></tr></thead>
              <tbody>
                {types.map((t) => (
                  <tr key={t.id} className="border-t"><td className="px-4 py-3 font-semibold">{t.icaoCode}</td><td>{t.manufacturer}</td><td>{t.model}</td></tr>
                ))}
              </tbody>
            </table>
          </div>
          {can('AIRCRAFT_MANAGE') && (
            <form className="card grid gap-3 p-4" onSubmit={saveType}>
              <h2 className="font-display font-bold">Add aircraft type</h2>
              <input className="input" placeholder="ICAO (A320)" value={typeForm.icaoCode} onChange={(e) => setTypeForm({ ...typeForm, icaoCode: e.target.value })} required />
              <input className="input" placeholder="Manufacturer" value={typeForm.manufacturer} onChange={(e) => setTypeForm({ ...typeForm, manufacturer: e.target.value })} required />
              <input className="input" placeholder="Model" value={typeForm.model} onChange={(e) => setTypeForm({ ...typeForm, model: e.target.value })} required />
              <textarea className="input" placeholder="Description" value={typeForm.description} onChange={(e) => setTypeForm({ ...typeForm, description: e.target.value })} />
              <button className="btn-primary">Save type</button>
            </form>
          )}
        </div>
      )}
      {modalOpen && (
        <Modal title={editing ? 'Edit aircraft' : 'Add aircraft'} onClose={() => { setCreating(false); setEditing(null) }}>
          <form onSubmit={save} className="grid gap-3">
            <input className="input" placeholder="Registration" value={form.registration} onChange={(e) => setForm({ ...form, registration: e.target.value })} required />
            <select className="input" value={form.aircraftTypeId} onChange={(e) => setForm({ ...form, aircraftTypeId: e.target.value })}>
              {types.map((t) => <option key={t.id} value={t.id}>{t.icaoCode}</option>)}
            </select>
            <input className="input" placeholder="Serial number" value={form.serialNumber} onChange={(e) => setForm({ ...form, serialNumber: e.target.value })} required />
            <select className="input" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
              {['IN_SERVICE', 'AOG', 'MAINTENANCE', 'RETIRED'].map((s) => <option key={s}>{s}</option>)}
            </select>
            <div className="grid grid-cols-2 gap-2">
              <input className="input" type="number" placeholder="Flight hours" value={form.totalFlightHours} onChange={(e) => setForm({ ...form, totalFlightHours: Number(e.target.value) })} />
              <input className="input" type="number" placeholder="Cycles" value={form.totalCycles} onChange={(e) => setForm({ ...form, totalCycles: Number(e.target.value) })} />
            </div>
            <input className="input" placeholder="Base station" value={form.baseStation} onChange={(e) => setForm({ ...form, baseStation: e.target.value })} />
            <div className="flex justify-end gap-2"><button type="button" className="btn-secondary" onClick={() => { setCreating(false); setEditing(null) }}>Cancel</button><button className="btn-primary">Save</button></div>
          </form>
        </Modal>
      )}
    </div>
  )
}
