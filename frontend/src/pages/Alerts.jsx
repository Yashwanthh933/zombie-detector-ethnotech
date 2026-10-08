import { useState, useEffect, useCallback } from 'react'
import { apiFetch, readError } from '../api'

function Alerts() {
    const [alerts, setAlerts] = useState([])
    const [error, setError] = useState('')

    const refresh = useCallback(async () => {
        try {
            const res = await apiFetch('/alerts')
            if (res.ok) setAlerts(await res.json())
        } catch (err) {
            console.error('Failed to load backend alerts', err)
        }
    }, [])

    useEffect(() => {
        refresh()
        const id = setInterval(refresh, 5000)
        return () => clearInterval(id)
    }, [refresh])

    const ack = async (id) => {
        setError('')
        try {
            const res = await apiFetch(`/alerts/${id}/ack`, { method: 'POST' })
            if (!res.ok) setError(await readError(res, 'Could not acknowledge the alert.'))
        } catch {
            setError('Cannot reach the server.')
        }
        refresh()
    }

    const active = alerts.filter(a => !a.acknowledged)
    const acknowledged = alerts.filter(a => a.acknowledged)

    const severityColors = {
        CRITICAL: 'border-rose-400 bg-rose-50/80',
        WARNING: 'border-amber-400 bg-amber-50/80',
        INFO: 'border-sky-400 bg-sky-50/80'
    }

    return (
        <div className="space-y-6 animate-fadeIn">
            {error && <p role="alert" className="text-xs font-semibold text-rose-700 bg-rose-50 border border-rose-200 rounded-xl px-3 py-2">{error}</p>}
            <div>
                <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                    <span className="text-2xl">🔔</span> Active System Alerts
                    <span className="ml-2 text-xs font-bold text-sky-700 bg-sky-50 border border-sky-200 px-3 py-0.5 rounded-full">
                        {active.length} Active
                    </span>
                </h3>
                <div className="mt-4 space-y-3">
                    {active.length === 0 && (
                        <div className="bg-white/80 backdrop-blur-md rounded-2xl border border-emerald-100 p-8 text-center shadow-sm">
                            <p className="text-4xl mb-2">✨</p>
                            <p className="text-slate-700 font-bold">All clear! No pending alerts.</p>
                            <p className="text-xs text-slate-400 mt-1">Your infrastructure resources are healthy and running efficiently.</p>
                        </div>
                    )}
                    {active.map(a => (
                        <div
                            key={a.id}
                            className={`
                                bg-white/90 backdrop-blur-md rounded-2xl border-l-4 p-5 flex items-center justify-between
                                transition-all hover:shadow-md animate-slideIn border-y border-r border-slate-100
                                ${severityColors[a.severity] || 'border-slate-300'}
                            `}
                        >
                            <div>
                                <div className="flex items-center gap-2">
                                    <p className="font-bold text-slate-800">{a.title}</p>
                                    <span className="text-[10px] font-extrabold px-2 py-0.5 rounded uppercase tracking-wider bg-white shadow-2xs text-slate-600 border border-slate-200">
                                        {a.severity}
                                    </span>
                                </div>
                                <p className="text-sm text-slate-600 mt-1">{a.message}</p>
                                <p className="text-xs text-slate-400 mt-1 font-mono">{a.timestamp?.replace('T', ' ').slice(0, 19)}</p>
                            </div>
                            <button
                                onClick={() => ack(a.id)}
                                className="px-4 py-2 rounded-xl bg-sky-500 text-white text-xs font-semibold hover:bg-sky-600 transition-all hover:scale-105 shadow-sm shadow-sky-200 cursor-pointer"
                            >
                                Acknowledge
                            </button>
                        </div>
                    ))}
                </div>
            </div>

            {acknowledged.length > 0 && (
                <div>
                    <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                        <span className="text-2xl">✅</span> Acknowledged History
                        <span className="ml-2 text-xs font-bold text-slate-500 bg-slate-100 px-3 py-0.5 rounded-full">
                            {acknowledged.length}
                        </span>
                    </h3>
                    <div className="mt-4 space-y-2.5">
                        {acknowledged.map(a => (
                            <div
                                key={a.id}
                                className="bg-white/50 backdrop-blur-xs rounded-xl border border-slate-200 p-4 flex items-center justify-between opacity-75 hover:opacity-100 transition-opacity"
                            >
                                <div>
                                    <p className="font-semibold text-slate-700 text-sm">{a.title}</p>
                                    <p className="text-xs text-slate-400 mt-0.5">{a.message}</p>
                                </div>
                                <span className="text-xs text-emerald-700 bg-emerald-50 border border-emerald-200 px-3 py-1 rounded-full font-medium">✓ Acknowledged</span>
                            </div>
                        ))}
                    </div>
                </div>
            )}
        </div>
    )
}

export default Alerts