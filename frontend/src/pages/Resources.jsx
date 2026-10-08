import { useState, useEffect, useCallback } from 'react'
import { apiFetch, readError } from '../api'
import { useAuth } from '../context/auth'
import StatusBadge from '../components/StatusBadge'

// Typical hourly on-demand rates (USD), used only to pre-fill the form.
const PRESETS = {
    't2.micro': 0.0116,
    't2.large': 0.0928,
    'm5.large': 0.096,
    'c5.xlarge': 0.17
}

function Resources() {
    const { user } = useAuth()
    const isAdmin = user?.role === 'ADMIN'

    const [nodes, setNodes] = useState([])
    const [instanceType, setInstanceType] = useState('t2.micro')
    const [hourlyRate, setHourlyRate] = useState(String(PRESETS['t2.micro']))
    const [environment, setEnvironment] = useState('DEV')
    const [error, setError] = useState('')
    const [info, setInfo] = useState('')
    const [busy, setBusy] = useState(false)

    const refresh = useCallback(async () => {
        try {
            const res = await apiFetch('/nodes')
            if (res.ok) setNodes(await res.json())
        } catch (err) {
            console.error('Failed to load resources', err)
        }
    }, [])

    useEffect(() => {
        refresh()
        const id = setInterval(refresh, 5000)
        return () => clearInterval(id)
    }, [refresh])

    const onTypeChange = (value) => {
        setInstanceType(value)
        if (PRESETS[value] !== undefined) setHourlyRate(String(PRESETS[value]))
    }

    const add = async (e) => {
        e.preventDefault()
        setError('')
        setInfo('')
        const rate = Number(hourlyRate)
        if (!instanceType.trim()) { setError('Instance type is required.'); return }
        if (!Number.isFinite(rate) || rate < 0) { setError('Hourly rate must be a non-negative number.'); return }

        setBusy(true)
        try {
            const res = await apiFetch('/nodes', {
                method: 'POST',
                body: JSON.stringify({ instanceType: instanceType.trim(), hourlyRate: rate, environment })
            })
            if (res.ok) {
                const node = await res.json()
                setInfo(`Added ${node.nodeId}. Monitoring starts on the next simulator tick (~10s).`)
                refresh()
            } else {
                setError(await readError(res, 'Could not add the resource.'))
            }
        } catch {
            setError('Cannot reach the server.')
        }
        setBusy(false)
    }

    const remove = async (node) => {
        if (!window.confirm(`Stop monitoring ${node.nodeId}? Its metric history will be deleted.`)) return
        setError('')
        setInfo('')
        try {
            const res = await apiFetch(`/nodes/${node.nodeId}`, { method: 'DELETE' })
            if (res.ok) refresh()
            else setError(await readError(res, 'Could not remove the resource.'))
        } catch {
            setError('Cannot reach the server.')
        }
    }

    const inputClass = 'w-full px-3 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-400 bg-white'

    return (
        <div className="space-y-6 animate-fadeIn">
            <div className="bg-white/85 backdrop-blur-md rounded-2xl border border-sky-100 shadow-sm p-6">
                <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                    <span className="text-2xl">➕</span> Add a resource to monitor
                </h3>
                <p className="text-xs text-slate-500 mt-1">
                    No cloud account is connected, so you register the resource&apos;s details by hand and its CPU/traffic telemetry is <strong>simulated</strong>.
                    PROD resources are never stopped automatically.
                </p>

                <form onSubmit={add} className="mt-4 grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
                    <label className="block">
                        <span className="text-xs font-semibold text-slate-600">Instance type</span>
                        <input
                            list="instance-presets"
                            value={instanceType}
                            onChange={e => onTypeChange(e.target.value)}
                            maxLength={40}
                            className={`mt-1 ${inputClass}`}
                        />
                        <datalist id="instance-presets">
                            {Object.keys(PRESETS).map(t => <option key={t} value={t} />)}
                        </datalist>
                    </label>
                    <label className="block">
                        <span className="text-xs font-semibold text-slate-600">Hourly rate (USD)</span>
                        <input
                            type="number"
                            min="0"
                            step="0.0001"
                            value={hourlyRate}
                            onChange={e => setHourlyRate(e.target.value)}
                            className={`mt-1 ${inputClass}`}
                        />
                    </label>
                    <label className="block">
                        <span className="text-xs font-semibold text-slate-600">Environment</span>
                        <select value={environment} onChange={e => setEnvironment(e.target.value)} className={`mt-1 ${inputClass}`}>
                            <option value="DEV">DEV</option>
                            <option value="STAGING">STAGING</option>
                            <option value="PROD">PROD</option>
                        </select>
                    </label>
                    <button
                        type="submit"
                        disabled={busy}
                        className="py-2.5 rounded-xl text-white font-bold text-sm bg-linear-to-r from-sky-500 to-blue-600 hover:shadow-lg transition-all disabled:opacity-60 cursor-pointer"
                    >
                        {busy ? 'Adding…' : 'Add resource'}
                    </button>
                </form>

                {error && <p role="alert" className="mt-3 text-xs font-semibold text-rose-700 bg-rose-50 border border-rose-200 rounded-xl px-3 py-2">{error}</p>}
                {info && <p className="mt-3 text-xs font-semibold text-emerald-700 bg-emerald-50 border border-emerald-200 rounded-xl px-3 py-2">{info}</p>}
            </div>

            <div className="bg-white/85 backdrop-blur-md rounded-2xl border border-sky-100 shadow-sm p-6">
                <h3 className="text-sm font-bold text-slate-800 flex items-center gap-2 mb-4">
                    <span>🖥️</span> {isAdmin ? 'All resources' : 'Your resources'}
                    <span className="text-xs font-semibold text-slate-500 bg-slate-100 px-3 py-0.5 rounded-full">{nodes.length}</span>
                </h3>

                {nodes.length === 0 ? (
                    <div className="text-center py-10 bg-slate-50/50 rounded-xl border border-dashed border-slate-200">
                        <p className="text-slate-400 font-medium text-sm">Nothing monitored yet. Add your first resource above.</p>
                    </div>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="w-full text-sm">
                            <thead>
                                <tr className="text-left text-[11px] uppercase tracking-wider text-slate-400">
                                    <th className="py-2 pr-4">Resource</th>
                                    <th className="py-2 pr-4">Type</th>
                                    <th className="py-2 pr-4">Env</th>
                                    <th className="py-2 pr-4">Cost / month</th>
                                    <th className="py-2 pr-4">CPU</th>
                                    {isAdmin && <th className="py-2 pr-4">Owner</th>}
                                    <th className="py-2 pr-4">Status</th>
                                    <th className="py-2" />
                                </tr>
                            </thead>
                            <tbody>
                                {nodes.map(n => (
                                    <tr key={n.nodeId} className="border-t border-slate-100">
                                        <td className="py-2.5 pr-4 font-bold text-slate-700">{n.nodeId}</td>
                                        <td className="py-2.5 pr-4 text-slate-600">{n.instanceType}</td>
                                        <td className="py-2.5 pr-4 text-slate-600">{n.environment}</td>
                                        <td className="py-2.5 pr-4 text-slate-600">${(n.hourlyRate * 24 * 30).toFixed(2)}</td>
                                        <td className="py-2.5 pr-4 text-slate-600">{(n.avgCpuLoadLast15Min || 0).toFixed(1)}%</td>
                                        {isAdmin && <td className="py-2.5 pr-4 text-slate-500 text-xs">{n.ownerEmail || '—'}</td>}
                                        <td className="py-2.5 pr-4"><StatusBadge status={n.status} /></td>
                                        <td className="py-2.5 text-right">
                                            <button
                                                onClick={() => remove(n)}
                                                className="px-3 py-1 rounded-lg text-xs font-semibold text-rose-700 bg-rose-50 border border-rose-200 hover:bg-rose-100 cursor-pointer"
                                            >
                                                Remove
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    )
}

export default Resources
