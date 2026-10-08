import { useState, useEffect, useCallback } from 'react'
import {
    AreaChart, Area, XAxis, YAxis, CartesianGrid,
    Tooltip, ResponsiveContainer
} from 'recharts'
import StatCard from '../components/StatCard'
import StatusBadge from '../components/StatusBadge'
import vm from '/vm.svg'
import { apiFetch, readError } from '../api'
import { useAuth } from '../context/auth'

function Dashboard() {
    const { user } = useAuth()
    const isAdmin = user?.role === 'ADMIN'
    const [nodes, setNodes] = useState([])
    const [savings, setSavings] = useState({ stoppedCount: 0, monthlySavings: 0 })
    const [chartData, setChartData] = useState([])
    const [schedulerPaused, setSchedulerPaused] = useState(false)
    const [actionError, setActionError] = useState('')

    const refresh = useCallback(async () => {
        try {
            const [nodesRes, savingsRes, schedRes, chartRes] = await Promise.all([
                apiFetch('/nodes'),
                apiFetch('/savings'),
                apiFetch('/scheduler/status'),
                apiFetch('/savings/history')
            ])

            if (nodesRes.ok) setNodes(await nodesRes.json())
            if (savingsRes.ok) setSavings(await savingsRes.json())
            if (schedRes.ok) setSchedulerPaused((await schedRes.json()).paused)
            if (chartRes.ok) setChartData(await chartRes.json())
        } catch (err) {
            console.error('Backend connection error:', err)
        }
    }, [])

    useEffect(() => {
        refresh()
        const id = setInterval(refresh, 5000)
        return () => clearInterval(id)
    }, [refresh])

    const runAction = async (path, failMessage) => {
        setActionError('')
        try {
            const res = await apiFetch(path, { method: 'POST' })
            if (!res.ok) setActionError(await readError(res, failMessage))
        } catch {
            setActionError('Cannot reach the server.')
        }
        refresh()
    }

    const toggleScheduler = () =>
        runAction(`/scheduler/${schedulerPaused ? 'resume' : 'pause'}`, 'Could not change the scheduler state.')

    const override = (nodeId) =>
        runAction(`/nodes/${nodeId}/override`, 'Could not override this node.')

    const running = nodes.filter(n => n.status === 'RUNNING').length
    const flagged = nodes.filter(n => n.status === 'FLAGGED').length

    const monthlyCost = (n) => (n.hourlyRate || 0) * 24 * 30
    const avgCpu = nodes.length > 0
        ? (nodes.reduce((acc, n) => acc + (n.avgCpuLoadLast15Min || 0), 0) / nodes.length).toFixed(0)
        : '0'
    const runningCost = nodes.filter(n => n.status === 'RUNNING').reduce((acc, n) => acc + monthlyCost(n), 0)
    const atRiskCost = nodes.filter(n => n.status === 'FLAGGED').reduce((acc, n) => acc + monthlyCost(n), 0)
    const prodCount = nodes.filter(n => n.environment === 'PROD').length

    const chartRows = Array.isArray(chartData)
        ? chartData.map(entry => ({
            date: String(entry.date || '').slice(5), // MM-DD
            savings: Number(entry.savings) || 0,
            waste: Number(entry.waste) || 0
        }))
        : []
    const hasChartActivity = chartRows.some(r => r.savings > 0 || r.waste > 0)

    return (
        <div className="space-y-6 animate-fadeIn">
            {/* Stat Cards Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <StatCard
                    label="Monthly Savings"
                    value={`$${(savings.monthlySavings || 0).toFixed(2)}`}
                    icon="💰"
                    tone="success"
                    subtitle="Projected cloud optimization"
                />
                <StatCard
                    label="Running Nodes"
                    value={running}
                    icon="🟢"
                    tone="info"
                    subtitle={`${nodes.length} total monitored`}
                />
                <StatCard
                    label="Needs Review"
                    value={flagged}
                    icon="⚠️"
                    tone="warning"
                    subtitle="Flagged zombie nodes"
                />
                <StatCard
                    label="Stopped (Zombies)"
                    value={savings.stoppedCount}
                    icon={<div className="w-12 h-12 rounded-2xl bg-linear-to-br from-red-400 to-orange-300 flex items-center justify-center text-white text-xl shadow-md shadow-red-500-200 overflow-hidden">
                        <img src={vm} alt="FinOps Sentinel logo" className="w-full h-full object-contain" />
                    </div>}
                    tone='danger'
                    subtitle={`$${(((savings.monthlySavings || 0)) / 30).toFixed(2)}/day saved`}
                />
            </div>
            {/* Nodes Inventory List */}
            <div className="lg:col-span-2">
                <div className="bg-white/85 backdrop-blur-md rounded-2xl shadow-sm border border-sky-100 p-6 transition-all hover:shadow-md">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="text-sm font-bold text-slate-800 flex items-center gap-2">
                            <span>🖥️</span> Node Inventory Status
                        </h3>
                        {isAdmin ? (
                            <button
                                onClick={toggleScheduler}
                                className={`
                                        px-4 py-2 rounded-xl text-xs font-semibold transition-all duration-300 shadow-2xs cursor-pointer
                                        ${schedulerPaused
                                        ? 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100 border border-emerald-200'
                                        : 'bg-rose-50 text-rose-700 hover:bg-rose-100 border border-rose-200'
                                    }
                                    `}
                            >
                                {schedulerPaused ? '▶ Resume Scheduler' : '⏸ Pause Scheduler'}
                            </button>
                        ) : (
                            <span className={`px-3 py-1.5 rounded-xl text-xs font-semibold border ${schedulerPaused ? 'bg-amber-50 text-amber-700 border-amber-200' : 'bg-emerald-50 text-emerald-700 border-emerald-200'}`}>
                                {schedulerPaused ? 'Automation paused by admin' : 'Automation active'}
                            </span>
                        )}
                    </div>

                    {actionError && (
                        <p role="alert" className="mb-3 text-xs font-semibold text-rose-700 bg-rose-50 border border-rose-200 rounded-xl px-3 py-2">{actionError}</p>
                    )}

                    <div className="space-y-2.5 max-h-150 overflow-y-auto pr-1 custom-scrollbar">
                        {nodes.map((n, idx) => (
                            <div
                                key={n.nodeId}
                                className="flex items-center justify-between p-3.5 rounded-xl bg-slate-50/80 border border-slate-100 hover:bg-white hover:border-sky-200 hover:shadow-sm transition-all duration-200 animate-slideIn"
                                style={{ animationDelay: `${idx * 30}ms` }}
                            >
                                <div className="flex items-center gap-3">
                                    <span className="text-sm font-bold text-slate-800">{n.nodeId}</span>
                                    {n.environment === 'PROD' && (
                                        <span className="text-[10px] font-extrabold uppercase tracking-wider text-amber-700 bg-amber-50 px-2.5 py-0.5 rounded-full border border-amber-200">
                                            prod
                                        </span>
                                    )}
                                    <span className="text-xs text-slate-500 font-medium">{n.ownerEmail || '—'}</span>
                                    <span className="text-xs text-sky-700 bg-sky-50 border border-sky-100 px-2.5 py-0.5 rounded-full font-semibold">
                                        {n.avgCpuLoadLast15Min?.toFixed(1) || '0.0'}% CPU
                                    </span>
                                </div>
                                <div className="flex items-center gap-3">
                                    <StatusBadge status={n.status} />
                                    {n.status === 'FLAGGED' && (
                                        <button
                                            onClick={() => override(n.nodeId)}
                                            className="px-3 py-1 rounded-lg bg-sky-500 text-white text-xs font-semibold hover:bg-sky-600 transition-all hover:scale-105 shadow-sm shadow-sky-200 cursor-pointer"
                                        >
                                            Override
                                        </button>
                                    )}
                                </div>
                            </div>
                        ))}
                        {nodes.length === 0 && (
                            <div className="text-center py-12 bg-slate-50/50 rounded-xl border border-dashed border-slate-200">
                                <p className="text-slate-400 font-medium text-sm">No resources yet. Add one from the Resources tab to start monitoring it.</p>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* Chart Section */}
            <div className="bg-white/85 backdrop-blur-md rounded-2xl shadow-sm border border-sky-100 p-6 transition-all hover:shadow-md">
                <div className="flex items-center justify-between mb-4">
                    <div>
                        <h3 className="text-lg font-bold text-slate-800">Last 7 Days: Savings vs Idle Waste</h3>
                        <p className="text-sm text-slate-500">Daily cost (USD/day) of stopped nodes vs. flagged-but-still-running nodes</p>
                    </div>
                    <div className="flex items-center gap-4 text-sm font-medium">
                        <span className="flex items-center gap-2">
                            <span className="w-3 h-3 rounded-full bg-emerald-500 shadow-xs"></span>
                            <span className="text-slate-600">Savings</span>
                        </span>
                        <span className="flex items-center gap-2">
                            <span className="w-3 h-3 rounded-full bg-rose-400 shadow-xs"></span>
                            <span className="text-slate-600">Waste</span>
                        </span>
                    </div>
                </div>
                {hasChartActivity ? (
                    <ResponsiveContainer width="100%" height={280}>
                        <AreaChart data={chartRows} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
                            <defs>
                                <linearGradient id="colorSavings" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="5%" stopColor="#10b981" stopOpacity={0.4} />
                                    <stop offset="95%" stopColor="#10b981" stopOpacity={0.0} />
                                </linearGradient>
                                <linearGradient id="colorWaste" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="5%" stopColor="#fb7185" stopOpacity={0.4} />
                                    <stop offset="95%" stopColor="#fb7185" stopOpacity={0.0} />
                                </linearGradient>
                            </defs>
                            <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                            <XAxis dataKey="date" stroke="#94a3b8" fontSize={12} tickLine={false} />
                            <YAxis stroke="#94a3b8" fontSize={12} tickLine={false} />
                            <Tooltip
                                contentStyle={{ backgroundColor: 'rgba(255, 255, 255, 0.95)', borderRadius: '12px', border: '1px solid #e2e8f0', boxShadow: '0 10px 25px -5px rgba(0,0,0,0.05)' }}
                            />
                            <Area type="monotone" dataKey="savings" stroke="#10b981" strokeWidth={2.5} fillOpacity={1} fill="url(#colorSavings)" />
                            <Area type="monotone" dataKey="waste" stroke="#fb7185" strokeWidth={2.5} fillOpacity={1} fill="url(#colorWaste)" />
                        </AreaChart>
                    </ResponsiveContainer>
                ) : (
                    <div className="flex h-70 items-center justify-center rounded-xl border border-dashed border-slate-200 bg-slate-50/70 px-6 text-center">
                        <p className="text-sm text-slate-500">No savings history yet. Nodes that get flagged or stopped will show up here.</p>
                    </div>
                )}
            </div>

            {/* Live Snapshot & Node Inventory Section */}
            <div className="grid w-full">
                {/* Live Snapshot Widget (Connected to Backend Telemetry) */}
                <div className="lg:col-span-1 bg-white/85 backdrop-blur-md rounded-2xl shadow-sm border border-sky-100 p-6 flex flex-col justify-between transition-all hover:shadow-md">
                    <div>
                        <div className="flex items-center justify-between mb-4">
                            <h3 className="text-sm font-bold text-slate-800 flex items-center gap-2">
                                <span className="text-sky-500">⚡</span> Live Snapshot
                            </h3>
                            <span className="flex items-center gap-1.5 text-xs text-emerald-700 bg-emerald-50 border border-emerald-200 px-2.5 py-1 rounded-full font-medium">
                                <span className="relative flex h-2 w-2">
                                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                                    <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
                                </span>
                                Live
                            </span>
                        </div>
                        <div className="grid grid-cols-2 gap-3.5">
                            <div className="bg-sky-50/70 border border-sky-100 rounded-xl p-3.5 text-center shadow-2xs">
                                <p className="text-2xl font-black text-slate-800">{avgCpu}%</p>
                                <p className="text-xs text-slate-500 font-semibold mt-0.5">Avg CPU</p>
                            </div>
                            <div className="bg-indigo-50/70 border border-indigo-100 rounded-xl p-3.5 text-center shadow-2xs">
                                <p className="text-xl font-black text-slate-800">${runningCost.toFixed(0)}</p>
                                <p className="text-xs text-slate-500 font-semibold mt-0.5">Running cost / mo</p>
                            </div>
                            <div className="bg-amber-50/70 border border-amber-100 rounded-xl p-3.5 text-center shadow-2xs">
                                <p className="text-xl font-black text-slate-800">${atRiskCost.toFixed(0)}</p>
                                <p className="text-xs text-slate-500 font-semibold mt-0.5">At-risk cost / mo</p>
                            </div>
                            <div className="bg-cyan-50/70 border border-cyan-100 rounded-xl p-3.5 text-center shadow-2xs">
                                <p className="text-2xl font-black text-slate-800">{prodCount}</p>
                                <p className="text-xs text-slate-500 font-semibold mt-0.5">PROD nodes</p>
                            </div>
                        </div>
                    </div>
                    <div className="mt-4 pt-3 border-t border-slate-100 text-center">
                        <p className="text-xs text-slate-400 font-medium">Live · updates every 5s</p>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default Dashboard