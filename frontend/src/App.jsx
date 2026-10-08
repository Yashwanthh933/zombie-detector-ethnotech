import { useCallback, useState } from 'react'
import './index.css'
import Dashboard from './pages/Dashboard'
import Resources from './pages/Resources'
import Alerts from './pages/Alerts'
import AuditTrail from './pages/AuditTrail'
import Policies from './pages/Policies'
import Login from './pages/Login'
import Toast from './components/Toast'
import { AuthProvider } from './context/AuthContext'
import { useAuth } from './context/auth'
import usePolicyStream from './hooks/usePolicyStream'
import logo from '/logo.svg'

const TABS = [
    { key: 'dashboard', label: 'Dashboard', icon: '☁️' },
    { key: 'resources', label: 'Resources', icon: '🖥️' },
    { key: 'alerts', label: 'Alerts', icon: '⚠️' },
    { key: 'audit', label: 'Audit Trail', icon: '📋' },
    { key: 'policies', label: 'Policies', icon: '⚙️' }
]

function Main() {
    const { user, logout } = useAuth()
    const [tab, setTab] = useState('dashboard')
    const [policyVersion, setPolicyVersion] = useState(0)
    const [toast, setToast] = useState(null)

    // Pushed from the backend the moment an admin saves a policy change.
    usePolicyStream(true, (policy) => {
        setPolicyVersion(v => v + 1)
        setToast({
            title: 'Detection policy updated',
            body: `${policy.updatedBy || 'An admin'} changed the policy: CPU < ${policy.cpuThreshold}%, ` +
                `${policy.idleWindowMinutes} min window, ${policy.gracePeriodSeconds}s grace.`
        })
    })

    const closeToast = useCallback(() => setToast(null), [])
    const isAdmin = user.role === 'ADMIN'

    return (
        <div className="min-h-screen bg-linear-to-br from-sky-50 via-blue-50/30 to-indigo-50/50 text-slate-800">
            <header className="bg-white/80 backdrop-blur-md border-b border-sky-100 shadow-xs sticky top-0 z-50">
                <div className="max-w-7xl mx-auto px-6 py-4">
                    <div className="flex flex-wrap items-center justify-between gap-3">
                        <div className="flex items-center gap-3">
                            <div className="w-10 h-10 rounded-2xl bg-linear-to-br from-sky-400 to-blue-600 flex items-center justify-center text-white text-xl shadow-md shadow-sky-200 overflow-hidden">
                                <img src={logo} alt="FinOps Sentinel logo" className="w-full h-full object-contain" />
                            </div>
                            <div>
                                <h1 className="text-lg font-black text-slate-800 tracking-tight">FinOps Sentinel</h1>
                                <p className="text-[11px] text-slate-400 font-semibold uppercase tracking-wider">Zombie VM Detector</p>
                            </div>
                        </div>

                        <nav className="flex flex-wrap gap-1.5 bg-slate-100/80 rounded-2xl p-1.5 shadow-inner border border-slate-200/50">
                            {TABS.map(({ key, label, icon }) => (
                                <button
                                    key={key}
                                    onClick={() => setTab(key)}
                                    className={`
                                        flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold transition-all duration-300 cursor-pointer
                                        ${tab === key
                                            ? 'bg-white text-sky-700 shadow-sm shadow-sky-200/50 scale-102'
                                            : 'text-slate-500 hover:text-slate-700 hover:bg-white/40'
                                        }
                                    `}
                                >
                                    <span className="text-sm">{icon}</span>
                                    {label}
                                </button>
                            ))}
                        </nav>

                        <div className="flex items-center gap-3">
                            <div className="flex items-center gap-2 bg-emerald-50 border border-emerald-200/80 rounded-full px-3 py-1.5 shadow-2xs">
                                <span className="relative flex h-2.5 w-2.5">
                                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                                    <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500"></span>
                                </span>
                                <span className="text-xs font-bold text-emerald-800">Live</span>
                            </div>
                            <div className="flex items-center gap-2 bg-white border border-slate-200 rounded-full pl-3 pr-1.5 py-1 shadow-2xs">
                                <span className="text-xs font-semibold text-slate-600 max-w-40 truncate">{user.email}</span>
                                <span className={`text-[10px] font-extrabold uppercase tracking-wider px-2 py-0.5 rounded-full border ${isAdmin ? 'bg-indigo-50 text-indigo-700 border-indigo-200' : 'bg-slate-100 text-slate-600 border-slate-200'}`}>
                                    {user.role}
                                </span>
                                <button
                                    onClick={logout}
                                    className="text-xs font-bold text-slate-500 hover:text-rose-600 px-2 py-1 rounded-full cursor-pointer"
                                >
                                    Sign out
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </header>

            <main className="max-w-7xl mx-auto px-6 py-8">
                <div className="animate-fadeIn">
                    {tab === 'dashboard' && <Dashboard />}
                    {tab === 'resources' && <Resources />}
                    {tab === 'alerts' && <Alerts />}
                    {tab === 'audit' && <AuditTrail />}
                    {tab === 'policies' && <Policies policyVersion={policyVersion} />}
                </div>
            </main>

            <Toast toast={toast} onClose={closeToast} />
        </div>
    )
}

function Shell() {
    const { user, ready } = useAuth()

    if (!ready) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <p className="text-slate-400 font-medium text-sm">Loading…</p>
            </div>
        )
    }
    return user ? <Main /> : <Login />
}

function App() {
    return (
        <AuthProvider>
            <Shell />
        </AuthProvider>
    )
}

export default App
