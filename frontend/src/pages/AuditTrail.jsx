import { useState, useEffect } from 'react'

const API = 'http://localhost:8080/api'

function AuditTrail() {
    const [events, setEvents] = useState([])

    useEffect(() => {
        const fetchAudit = async () => {
            try {
                const r = await fetch(`${API}/audit`)
                if (r.ok) setEvents(await r.json())
            } catch (err) {
                console.error('Failed fetching backend audit trail', err)
            }
        }
        fetchAudit()
        const id = setInterval(fetchAudit, 5000)
        return () => clearInterval(id)
    }, [])

    const actionColors = {
        FLAGGED: 'bg-amber-100 text-amber-800 border-amber-200',
        RECOVERED: 'bg-emerald-100 text-emerald-800 border-emerald-200',
        STOPPED: 'bg-rose-100 text-rose-800 border-rose-200',
        OVERRIDE: 'bg-sky-100 text-sky-800 border-sky-200'
    }

    return (
        <div className="animate-fadeIn">
            <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                    <span className="text-2xl">📋</span> System Audit Trail
                    <span className="ml-2 text-xs font-semibold text-slate-500 bg-slate-100 px-3 py-0.5 rounded-full">
                        {events.length} events logged
                    </span>
                </h3>
                <span className="text-xs text-emerald-700 bg-emerald-50 px-3 py-1 rounded-full border border-emerald-200 font-medium">
                    Live · updates every 5s
                </span>
            </div>

            <div className="bg-white/80 backdrop-blur-md rounded-2xl border border-sky-100 overflow-hidden shadow-sm">
                <div className="max-h-[600px] overflow-y-auto custom-scrollbar">
                    {events.map((e, idx) => (
                        <div
                            key={e.id || idx}
                            className={`
                                flex items-center justify-between px-5 py-3.5 border-b border-slate-100
                                hover:bg-sky-50/50 transition-colors duration-150 animate-slideIn
                                ${idx % 2 === 0 ? 'bg-white/40' : 'bg-slate-50/30'}
                            `}
                            style={{ animationDelay: `${idx * 20}ms` }}
                        >
                            <div className="flex items-center gap-4">
                                <span className="text-xs text-slate-400 font-mono w-44 flex-shrink-0">
                                    {e.timestamp?.replace('T', ' ').slice(0, 19)}
                                </span>
                                <span className={`
                                    px-3 py-0.5 rounded-full text-xs font-bold uppercase tracking-wider border shadow-2xs
                                    ${actionColors[e.action] || 'bg-slate-100 text-slate-700 border-slate-200'}
                                `}>
                                    {e.action?.toLowerCase()}
                                </span>
                                <span className="text-sm font-bold text-slate-700">{e.targetNodeId}</span>
                            </div>
                            <span className="text-xs text-slate-500 truncate max-w-md font-medium">
                                {e.details}
                            </span>
                        </div>
                    ))}
                    {events.length === 0 && (
                        <div className="text-center py-12">
                            <p className="text-slate-400 font-medium text-sm">No backend audit records found.</p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}

export default AuditTrail