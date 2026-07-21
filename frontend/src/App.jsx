import { useState } from 'react'
import './index.css'
import Dashboard from './pages/Dashboard'
import Alerts from './pages/Alerts'
import AuditTrail from './pages/AuditTrail'
import Policies from './pages/Policies'
import logo from '/logo.svg'

function App() {
  const [tab, setTab] = useState('dashboard')

  const tabs = [
    { key: 'dashboard', label: 'Dashboard', icon: '☁️' },
    { key: 'alerts', label: 'Alerts', icon: '⚠️' },
    { key: 'audit', label: 'Audit Trail', icon: '📋' },
    { key: 'policies', label: 'Policies', icon: '⚙️' }
  ]

  return (
    <div className="min-h-screen bg-linear-to-br from-sky-50 via-blue-50/30 to-indigo-50/50 text-slate-800">
      {/* Header */}
      <header className="bg-white/80 backdrop-blur-md border-b border-sky-100 shadow-xs sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            {/* Brand */}
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-2xl bg-linear-to-br from-sky-400 to-blue-600 flex items-center justify-center text-white text-xl shadow-md shadow-sky-200 overflow-hidden">
                <img src={logo} alt="FinOps Sentinel logo" className="w-full h-full object-contain" />
              </div>
              <div>
                <h1 className="text-lg font-black text-slate-800 tracking-tight">FinOps Sentinel</h1>
                <p className="text-[11px] text-slate-400 font-semibold uppercase tracking-wider">Zombie VM Detector</p>
              </div>
            </div>

            {/* Navigation Tabs */}
            <nav className="flex gap-1.5 bg-slate-100/80 rounded-2xl p-1.5 shadow-inner border border-slate-200/50">
              {tabs.map(({ key, label, icon }) => (
                <button
                  key={key}
                  onClick={() => setTab(key)}
                  className={`
                    flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-bold transition-all duration-300 cursor-pointer
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

            {/* Live Status Indicator */}
            <div className="flex items-center gap-2 bg-emerald-50 border border-emerald-200/80 rounded-full px-4 py-2 shadow-2xs">
              <span className="relative flex h-2.5 w-2.5">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500"></span>
              </span>
              <span className="text-xs font-bold text-emerald-800">Live</span>
              <span className="text-[11px] text-slate-400 font-medium">· 5s sync</span>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="max-w-7xl mx-auto px-6 py-8">
        <div className="animate-fadeIn">
          {tab === 'dashboard' && <Dashboard />}
          {tab === 'alerts' && <Alerts />}
          {tab === 'audit' && <AuditTrail />}
          {tab === 'policies' && <Policies />}
        </div>
      </main>
    </div>
  )
}

export default App