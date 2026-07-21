function StatusBadge({ status }) {
    const config = {
        RUNNING: { bg: 'bg-emerald-100', text: 'text-emerald-800', dot: 'bg-emerald-500' },
        FLAGGED: { bg: 'bg-amber-100', text: 'text-amber-800', dot: 'bg-amber-500' },
        STOPPED: { bg: 'bg-rose-100', text: 'text-rose-800', dot: 'bg-rose-500' }
    }

    const style = config[status] || config.RUNNING

    return (
        <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold shadow-2xs ${style.bg} ${style.text}`}>
            <span className={`w-2 h-2 rounded-full ${style.dot} animate-pulse`}></span>
            {status?.toLowerCase()}
        </span>
    )
}

export default StatusBadge