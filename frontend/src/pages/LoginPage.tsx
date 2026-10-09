import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ErrorBanner } from '../components/Ui'
import { errorMessage } from '../types'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await login(username.trim(), password)
      navigate('/')
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="relative min-h-screen min-h-dvh overflow-hidden bg-navy-950">
      <img
        src="/login-aircraft.jpg"
        alt=""
        className="absolute inset-0 h-full w-full object-cover object-[70%_center] sm:object-center"
      />
      <div className="absolute inset-0 bg-gradient-to-r from-navy-950/80 via-navy-950/50 to-navy-950/20 sm:from-navy-950/75 sm:via-navy-950/40 sm:to-transparent" />
      <div className="absolute inset-0 bg-gradient-to-t from-navy-950/55 via-transparent to-navy-950/25" />

      <div className="relative z-10 mx-auto flex min-h-screen min-h-dvh w-full max-w-6xl flex-col justify-center gap-10 px-4 py-8 sm:px-8 lg:flex-row lg:items-center lg:justify-between lg:gap-16 lg:px-10">
        <div className="max-w-lg text-white lg:flex-1">
          <div className="mb-4 inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-accent-500 font-display text-lg font-bold text-navy-950 shadow-lg sm:h-14 sm:w-14 sm:text-xl">
            AM
          </div>
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent-400">Aircraft Maintenance</p>
          <h1 className="mt-2 font-display text-3xl font-bold leading-tight sm:text-4xl lg:text-5xl">
            Sign in to AMOS
          </h1>
          <p className="mt-3 max-w-md text-sm leading-relaxed text-white/80 sm:text-base">
            Plan, execute, and verify maintenance with role-based control — from the hangar to the flight line.
          </p>
        </div>

        <div className="w-full max-w-md lg:flex-shrink-0">
          <div className="rounded-2xl border border-white/20 bg-white/95 p-5 shadow-2xl backdrop-blur-md sm:p-8">
            <div className="mb-6">
              <h2 className="font-display text-xl font-bold text-navy-900 sm:text-2xl">Welcome back</h2>
              <p className="mt-1 text-sm text-slate-500">Enter your credentials to continue.</p>
            </div>
            <ErrorBanner error={error} />
            <form onSubmit={submit} className="space-y-4" noValidate>
              <div>
                <label htmlFor="login-username" className="label">Username</label>
                <input
                  id="login-username"
                  className="input"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  autoComplete="username"
                  autoFocus
                  required
                  placeholder="Enter your username"
                />
              </div>
              <div>
                <label htmlFor="login-password" className="label">Password</label>
                <div className="relative">
                  <input
                    id="login-password"
                    type={showPassword ? 'text' : 'password'}
                    className="input pr-20"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    autoComplete="current-password"
                    required
                    placeholder="Enter your password"
                  />
                  <button
                    type="button"
                    className="absolute inset-y-0 right-1 my-1 rounded-md px-2.5 text-xs font-semibold text-slate-500 hover:bg-slate-100 hover:text-navy-800"
                    onClick={() => setShowPassword((v) => !v)}
                    aria-pressed={showPassword}
                  >
                    {showPassword ? 'Hide' : 'Show'}
                  </button>
                </div>
              </div>
              <button className="btn-primary w-full py-2.5" disabled={busy || !username.trim() || !password}>
                {busy ? 'Signing in…' : 'Sign in'}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  )
}
