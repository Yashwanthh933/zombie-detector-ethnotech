import { useState, useEffect } from 'react'

const API = 'http://localhost:8080/api'

function Policies() {
    const [policy, setPolicy] = useState(null)
    const [saved, setSaved] = useState(false)
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        fetch(`${API}/policy`)
            .then(r => r.json())
            .then(data => {
                setPolicy(data)
                setLoading(false)
            })
            .catch(() => {
                setPolicy({
                    cpuThreshold: 15,
                    idleWindowMinutes: 30,
                    minSamplesRequired: 5,
                    recoveryStrikesRequired: 3,
                    gracePeriodSeconds: 60
                })
                setLoading(false)
            })
    }, [])

    if (loading) {
        return (
            <div className="bg-white/80 backdrop-blur-md rounded-2xl border border-sky-100 p-12 text-center shadow-sm">
                <p className="text-slate-400 font-medium">Loading policy configuration parameters...</p>
            </div>
        )
    }

    const update = (field, value) => setPolicy({ ...policy, [field]: value })

    const save = async () => {
        await fetch(`${API}/policy`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(policy)
        })
        setSaved(true)
        setTimeout(() => setSaved(false), 3000)
    }

    const fields = [
        { key: 'cpuThreshold', label: 'CPU Threshold', suffix: '%', step: 0.5, min: 0, max: 100 },
        { key: 'idleWindowMinutes', label: 'Idle Window', suffix: 'minutes', step: 1, min: 1, max: 120 },
        { key: 'minSamplesRequired', label: 'Min Samples Required', suffix: 'samples', step: 1, min: 1, max: 20 },
        { key: 'recoveryStrikesRequired', label: 'Recovery Strikes Required', suffix: 'strikes', step: 1, min: 1, max: 10 },
        { key: 'gracePeriodSeconds', label: 'Grace Period', suffix: 'seconds', step: 1, min: 5, max: 300 }
    ]

    return (
        <div className="max-w-2xl animate-fadeIn">
            <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                    <span className="text-2xl">⚙️</span> Governance Policies
                </h3>
                <span className="text-xs text-emerald-700 bg-emerald-50 px-3 py-1 rounded-full border border-emerald-200 font-medium">
                    Live configuration
                </span>
            </div>

            <div className="bg-white/80 backdrop-blur-md rounded-2xl border border-sky-100 p-6 shadow-sm space-y-5">
                {fields.map(({ key, label, suffix, step, min, max }) => (
                    <div key={key} className="flex flex-col">
                        <label className="text-sm font-semibold text-slate-700 mb-1.5 flex justify-between">
                            <span>{label}</span>
                            <span className="text-sky-600 font-mono text-xs font-bold">{policy[key] || 0} {suffix}</span>
                        </label>
                        <div className="flex items-center gap-4">
                            <input
                                type="range"
                                min={min}
                                max={max}
                                step={step}
                                value={policy[key] || 0}
                                onChange={e => update(key, parseFloat(e.target.value))}
                                className="w-full h-2 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-sky-500"
                            />
                            <input
                                type="number"
                                value={policy[key] || 0}
                                onChange={e => update(key, parseFloat(e.target.value))}
                                className="w-20 px-3 py-1.5 rounded-xl border border-slate-200 text-center text-sm font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-sky-400 focus:border-transparent bg-white shadow-2xs"
                                step={step}
                                min={min}
                                max={max}
                            />
                        </div>
                    </div>
                ))}

                <button
                    onClick={save}
                    className={`
                        w-full mt-3 py-3.5 rounded-xl text-white font-bold transition-all duration-300 shadow-md shadow-sky-100
                        ${saved
                            ? 'bg-emerald-500 hover:bg-emerald-600 shadow-emerald-100'
                            : 'bg-linear-to-r from-sky-500 to-blue-600 hover:shadow-lg hover:scale-[1.01]'
                        }
                    `}
                >
                    {saved ? '✅ Policy Saved Successfully!' : '💾 Save Policy Settings'}
                </button>

                {saved && (
                    <p className="text-center text-xs text-emerald-600 animate-fadeIn font-semibold">
                        Policy updated successfully! Sentinel detector will use new threshold values.
                    </p>
                )}
            </div>
        </div>
    )
}

export default Policies