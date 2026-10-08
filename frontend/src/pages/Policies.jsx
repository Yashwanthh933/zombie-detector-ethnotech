import { useState, useEffect, useCallback } from 'react'
import { apiFetch, readError } from '../api'
import { useAuth } from '../context/auth'

const FIELDS = [
    { key: 'cpuThreshold', label: 'CPU Threshold', suffix: '%', step: 0.5, min: 0, max: 100 },
    { key: 'idleWindowMinutes', label: 'Idle Window', suffix: 'minutes', step: 1, min: 1, max: 120 },
    { key: 'minSamplesRequired', label: 'Min Samples Required', suffix: 'samples', step: 1, min: 1, max: 20 },
    { key: 'recoveryStrikesRequired', label: 'Recovery Strikes Required', suffix: 'strikes', step: 1, min: 1, max: 10 },
    { key: 'gracePeriodSeconds', label: 'Grace Period', suffix: 'seconds', step: 1, min: 5, max: 300 }
]

// policyVersion increments whenever the backend pushes a "policy-updated" event,
// so every open client reloads the new values without waiting for a page refresh.
function Policies({ policyVersion = 0 }) {
    const { user } = useAuth()
    const isAdmin = user?.role === 'ADMIN'

    const [policy, setPolicy] = useState(null)
    const [loadError, setLoadError] = useState('')
    const [saveError, setSaveError] = useState('')
    const [saved, setSaved] = useState(false)
    const [saving, setSaving] = useState(false)
    const [dirty, setDirty] = useState(false)

    const load = useCallback(async () => {
        try {
            const res = await apiFetch('/policy')
            if (!res.ok) { setLoadError(await readError(res, 'Could not load the policy.')); return }
            setPolicy(await res.json())
            setLoadError('')
            setDirty(false)
        } catch {
            setLoadError('Cannot reach the server.')
        }
    }, [])

    useEffect(() => { load() }, [load, policyVersion])

    if (loadError) {
        return (
            <div className="bg-white/80 rounded-2xl border border-rose-200 p-10 text-center shadow-sm">
                <p className="text-rose-700 font-semibold text-sm">{loadError}</p>
                <button onClick={load} className="mt-3 px-4 py-2 rounded-xl bg-sky-500 text-white text-xs font-semibold cursor-pointer">Retry</button>
            </div>
        )
    }

    if (!policy) {
        return (
            <div className="bg-white/80 backdrop-blur-md rounded-2xl border border-sky-100 p-12 text-center shadow-sm">
                <p className="text-slate-400 font-medium">Loading policy configuration parameters...</p>
            </div>
        )
    }

    const update = (field, value) => {
        setPolicy({ ...policy, [field]: Number.isNaN(value) ? 0 : value })
        setDirty(true)
        setSaved(false)
    }

    const save = async () => {
        setSaveError('')
        setSaving(true)
        try {
            const res = await apiFetch('/policy', { method: 'PUT', body: JSON.stringify(policy) })
            if (res.ok) {
                setPolicy(await res.json())
                setDirty(false)
                setSaved(true)
                setTimeout(() => setSaved(false), 3000)
            } else if (res.status === 403) {
                setSaveError('Only admins can change the policy.')
            } else {
                setSaveError(await readError(res, 'Could not save the policy.'))
            }
        } catch {
            setSaveError('Cannot reach the server.')
        }
        setSaving(false)
    }

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

            {!isAdmin && (
                <p className="mb-4 text-xs font-semibold text-slate-600 bg-slate-50 border border-slate-200 rounded-xl px-4 py-3">
                    These detection settings apply to everyone and can only be changed by an admin.
                    You&apos;ll be notified here when they change.
                </p>
            )}

            <div className="bg-white/80 backdrop-blur-md rounded-2xl border border-sky-100 p-6 shadow-sm space-y-5">
                {FIELDS.map(({ key, label, suffix, step, min, max }) => (
                    <div key={key} className="flex flex-col">
                        <label className="text-sm font-semibold text-slate-700 mb-1.5 flex justify-between">
                            <span>{label}</span>
                            <span className="text-sky-600 font-mono text-xs font-bold">{policy[key]} {suffix}</span>
                        </label>
                        <div className="flex items-center gap-4">
                            <input
                                type="range"
                                min={min}
                                max={max}
                                step={step}
                                value={policy[key]}
                                disabled={!isAdmin}
                                onChange={e => update(key, parseFloat(e.target.value))}
                                className="w-full h-2 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-sky-500 disabled:opacity-50 disabled:cursor-not-allowed"
                            />
                            <input
                                type="number"
                                value={policy[key]}
                                disabled={!isAdmin}
                                onChange={e => update(key, parseFloat(e.target.value))}
                                className="w-20 px-3 py-1.5 rounded-xl border border-slate-200 text-center text-sm font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-sky-400 focus:border-transparent bg-white shadow-2xs disabled:bg-slate-50 disabled:text-slate-500"
                                step={step}
                                min={min}
                                max={max}
                            />
                        </div>
                    </div>
                ))}

                {policy.updatedAt && (
                    <p className="text-xs text-slate-400">
                        Last updated {policy.updatedAt.replace('T', ' ').slice(0, 19)}{policy.updatedBy ? ` by ${policy.updatedBy}` : ''}
                    </p>
                )}

                {isAdmin && (
                    <>
                        <button
                            onClick={save}
                            disabled={saving || !dirty}
                            className={`
                                w-full mt-1 py-3.5 rounded-xl text-white font-bold transition-all duration-300 shadow-md shadow-sky-100
                                disabled:opacity-60 disabled:cursor-not-allowed cursor-pointer
                                ${saved
                                    ? 'bg-emerald-500 hover:bg-emerald-600 shadow-emerald-100'
                                    : 'bg-linear-to-r from-sky-500 to-blue-600 hover:shadow-lg'
                                }
                            `}
                        >
                            {saving ? 'Saving…' : saved ? '✅ Policy Saved Successfully!' : '💾 Save Policy Settings'}
                        </button>

                        {saveError && (
                            <p role="alert" className="text-center text-xs font-semibold text-rose-700 bg-rose-50 border border-rose-200 rounded-xl px-3 py-2">{saveError}</p>
                        )}
                        {saved && (
                            <p className="text-center text-xs text-emerald-600 animate-fadeIn font-semibold">
                                Saved. All connected users have been notified and the detector will use the new values.
                            </p>
                        )}
                    </>
                )}
            </div>
        </div>
    )
}

export default Policies
