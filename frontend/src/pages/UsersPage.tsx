import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../api/client'
import { ErrorBanner, Modal, PageHeader } from '../components/Ui'
import { useAuth } from '../auth/AuthContext'
import type { PermissionView, RoleView, UserView } from '../types'
import { errorMessage } from '../types'

export default function UsersPage() {
  const { can } = useAuth()
  const [users, setUsers] = useState<UserView[]>([])
  const [roles, setRoles] = useState<RoleView[]>([])
  const [permissions, setPermissions] = useState<PermissionView[]>([])
  const [error, setError] = useState('')
  const [userOpen, setUserOpen] = useState(false)
  const [roleEdit, setRoleEdit] = useState<RoleView | null>(null)
  const [form, setForm] = useState({
    username: '', email: '', password: 'Password123!', fullName: '', phone: '', station: 'DXB', active: true, roleIds: [] as string[],
  })

  async function load() {
    const [u, r] = await Promise.all([api<UserView[]>('/api/users'), api<RoleView[]>('/api/roles')])
    setUsers(u)
    setRoles(r)
    if (can('ROLE_MANAGE')) setPermissions(await api<PermissionView[]>('/api/permissions'))
  }
  useEffect(() => { void load().catch((e) => setError(errorMessage(e))) }, [])

  async function saveUser(e: FormEvent) {
    e.preventDefault()
    try {
      await api('/api/users', { method: 'POST', body: JSON.stringify({ ...form, roleIds: form.roleIds.map(Number) }) })
      setUserOpen(false)
      await load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  async function saveRole(e: FormEvent) {
    e.preventDefault()
    if (!roleEdit) return
    try {
      await api(`/api/roles/${roleEdit.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          code: roleEdit.code,
          name: roleEdit.name,
          description: roleEdit.description,
          permissionIds: permissions.filter((p) => roleEdit.permissions.includes(p.code)).map((p) => p.id),
        }),
      })
      setRoleEdit(null)
      await load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  return (
    <div>
      <PageHeader title="Users & roles" subtitle="Screen-level permissions mapped to aviation maintenance roles"
        actions={can('USER_MANAGE') ? <button className="btn-primary" type="button" onClick={() => setUserOpen(true)}>Invite user</button> : null} />
      <ErrorBanner error={error} />
      <div className="grid gap-6 lg:grid-cols-2">
        <section className="card overflow-x-auto">
          <h2 className="border-b px-4 py-3 font-display font-bold">People</h2>
          <table className="w-full text-left text-sm">
            <thead className="text-xs uppercase text-slate-500"><tr><th className="px-4 py-2">Name</th><th>Station</th><th>Roles</th></tr></thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id} className="border-t">
                  <td className="px-4 py-2"><div className="font-semibold">{u.fullName}</div><div className="text-xs text-slate-400">{u.username}</div></td>
                  <td>{u.station}</td>
                  <td className="text-xs">{u.roles.join(', ')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
        <section className="card p-4">
          <h2 className="mb-3 font-display font-bold">Roles</h2>
          <div className="space-y-2">
            {roles.map((r) => (
              <button key={r.id} type="button" className="w-full rounded-lg border px-3 py-2 text-left" onClick={() => can('ROLE_MANAGE') && setRoleEdit({ ...r, permissions: [...r.permissions] })}>
                <div className="font-semibold">{r.name} <span className="text-xs text-slate-400">{r.code}</span></div>
                <div className="text-xs text-slate-500">{r.description}</div>
                <div className="mt-1 text-[11px] text-slate-400">{r.permissions.length} permissions</div>
              </button>
            ))}
          </div>
        </section>
      </div>
      {userOpen && (
        <Modal title="New user" onClose={() => setUserOpen(false)}>
          <form onSubmit={saveUser} className="grid gap-3">
            <input className="input" placeholder="Full name" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required />
            <input className="input" placeholder="Username" value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} required />
            <input className="input" placeholder="Email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
            <input className="input" placeholder="Password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
            <select multiple className="input h-32" value={form.roleIds} onChange={(e) => setForm({ ...form, roleIds: [...e.target.selectedOptions].map((o) => o.value) })}>
              {roles.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
            </select>
            <div className="flex justify-end gap-2">
              <button type="button" className="btn-secondary" onClick={() => setUserOpen(false)}>Cancel</button>
              <button className="btn-primary">Create</button>
            </div>
          </form>
        </Modal>
      )}
      {roleEdit && (
        <Modal title={`Permissions · ${roleEdit.name}`} onClose={() => setRoleEdit(null)}>
          <form onSubmit={saveRole} className="grid gap-2">
            {permissions.map((p) => (
              <label key={p.id} className="flex items-start gap-2 text-sm">
                <input type="checkbox" checked={roleEdit.permissions.includes(p.code)} onChange={(e) => {
                  const next = e.target.checked
                    ? [...roleEdit.permissions, p.code]
                    : roleEdit.permissions.filter((c) => c !== p.code)
                  setRoleEdit({ ...roleEdit, permissions: next })
                }} />
                <span><span className="font-semibold">{p.code}</span><span className="block text-xs text-slate-500">{p.name}</span></span>
              </label>
            ))}
            <div className="mt-2 flex justify-end gap-2">
              <button type="button" className="btn-secondary" onClick={() => setRoleEdit(null)}>Cancel</button>
              <button className="btn-primary">Save role</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
