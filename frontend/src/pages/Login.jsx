import { useState } from 'react'
import { useAuth } from '../context/auth'
import logo from '/logo.svg'

function Login() {
    const { login, register, notice } = useAuth()
    const [mode, setMode] = useState('login') // 'login' | 'register'
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState('')
    const [busy, setBusy] = useState(false)

    const isRegister = mode === 'register'

    const submit = async (e) => {
        e.preventDefault()
        setError('')
        setBusy(true)
        const result = isRegister
            ? await register(email.trim(), password)
            : await login(email.trim(), password)
        setBusy(false)
        if (!result.ok) setError(result.error)
    }

    const switchMode = () => {
        setMode(isRegister ? 'login' : 'register')
        setError('')
    }

    return (
        <div className="min-h-screen flex items-center justify-center px-4">
            <div className="w-full max-w-md animate-fadeIn">
                <div className="flex flex-col items-center mb-6">
                    <div className="w-14 h-14 rounded-2xl bg-linear-to-br from-sky-400 to-blue-600 flex items-center justify-center shadow-md shadow-sky-200 overflow-hidden">
                        <img src={logo} alt="FinOps Sentinel logo" className="w-full h-full object-contain" />
                    </div>
                    <h1 className="mt-3 text-2xl font-black text-slate-800 tracking-tight">FinOps Sentinel</h1>
                    <p className="text-[11px] text-slate-400 font-semibold uppercase tracking-wider">Zombie Resource Detector</p>
                </div>

                <form onSubmit={submit} className="bg-white/85 backdrop-blur-md rounded-2xl border border-sky-100 shadow-sm p-7 space-y-4">
                    <h2 className="text-lg font-bold text-slate-800">
                        {isRegister ? 'Create your account' : 'Sign in'}
                    </h2>

                    {notice && !error && (
                        <p className="text-xs font-semibold text-amber-700 bg-amber-50 border border-amber-200 rounded-xl px-3 py-2">{notice}</p>
                    )}

                    <label className="block">
                        <span className="text-xs font-semibold text-slate-600">Email</span>
                        <input
                            type="email"
                            required
                            autoComplete="email"
                            value={email}
                            onChange={e => setEmail(e.target.value)}
                            className="mt-1 w-full px-3 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-400 bg-white"
                        />
                    </label>

                    <label className="block">
                        <span className="text-xs font-semibold text-slate-600">Password</span>
                        <input
                            type="password"
                            required
                            minLength={isRegister ? 8 : undefined}
                            autoComplete={isRegister ? 'new-password' : 'current-password'}
                            value={password}
                            onChange={e => setPassword(e.target.value)}
                            className="mt-1 w-full px-3 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-400 bg-white"
                        />
                        {isRegister && <span className="text-[11px] text-slate-400">At least 8 characters.</span>}
                    </label>

                    {error && (
                        <p role="alert" className="text-xs font-semibold text-rose-700 bg-rose-50 border border-rose-200 rounded-xl px-3 py-2">{error}</p>
                    )}

                    <button
                        type="submit"
                        disabled={busy}
                        className="w-full py-3 rounded-xl text-white font-bold bg-linear-to-r from-sky-500 to-blue-600 hover:shadow-lg transition-all disabled:opacity-60 cursor-pointer disabled:cursor-wait"
                    >
                        {busy ? 'Please wait…' : isRegister ? 'Create account' : 'Sign in'}
                    </button>

                    <p className="text-center text-xs text-slate-500">
                        {isRegister ? 'Already have an account?' : 'New here?'}{' '}
                        <button type="button" onClick={switchMode} className="font-bold text-sky-600 hover:underline cursor-pointer">
                            {isRegister ? 'Sign in' : 'Create an account'}
                        </button>
                    </p>
                </form>
            </div>
        </div>
    )
}

export default Login
