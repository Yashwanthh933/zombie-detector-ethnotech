import { useEffect } from 'react'

/** Small dismissible notification, auto-closes after a few seconds. */
function Toast({ toast, onClose }) {
    useEffect(() => {
        if (!toast) return undefined
        const id = setTimeout(onClose, 9000)
        return () => clearTimeout(id)
    }, [toast, onClose])

    if (!toast) return null

    return (
        <div role="status" className="fixed bottom-6 right-6 z-[100] max-w-sm animate-fadeIn">
            <div className="bg-white rounded-2xl border border-sky-200 shadow-xl shadow-sky-100 p-4 flex gap-3">
                <span className="text-xl">🔔</span>
                <div className="flex-1">
                    <p className="text-sm font-bold text-slate-800">{toast.title}</p>
                    {toast.body && <p className="text-xs text-slate-500 mt-0.5">{toast.body}</p>}
                </div>
                <button onClick={onClose} aria-label="Dismiss" className="text-slate-400 hover:text-slate-600 text-lg leading-none cursor-pointer">×</button>
            </div>
        </div>
    )
}

export default Toast
