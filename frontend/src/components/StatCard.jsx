function StatCard({ label, value, icon, tone = 'info', subtitle }) {
    const toneStyles = {
        success: 'bg-emerald-50/70 border-emerald-100 text-emerald-900',
        info: 'bg-sky-50/70 border-sky-100 text-sky-900',
        warning: 'bg-amber-50/70 border-amber-100 text-amber-900',
        danger: 'bg-rose-50/70 border-rose-100 text-rose-900'
    }

    const iconStyles = {
        success: 'bg-emerald-500 text-white shadow-emerald-200',
        info: 'bg-sky-500 text-white shadow-sky-200',
        warning: 'bg-amber-500 text-white shadow-amber-200',
        danger: 'bg-rose-500 text-white shadow-rose-200'
    }

    const isImageIcon = typeof icon === 'string' && /^(\/|https?:\/\/|\.\.?\/).+\.(svg|png|jpe?g|webp|gif)$/i.test(icon)

    return (
        <div className={`rounded-2xl border p-5 backdrop-blur-md shadow-sm transition-all duration-300 hover:shadow-md hover:-translate-y-1 ${toneStyles[tone] || toneStyles.info}`}>
            <div className="flex items-center justify-between">
                <div>
                    <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">{label}</p>
                    <p className="text-2xl font-bold tracking-tight text-slate-800 mt-1">{value}</p>
                </div>
                <div className={`w-12 h-12 rounded-2xl flex items-center justify-center text-xl shadow-lg overflow-hidden ${iconStyles[tone] || iconStyles.info}`}>
                    {isImageIcon ? <img src={icon} alt="" className="w-full h-full object-contain" /> : icon}
                </div>
            </div>
            {subtitle && (
                <div className="mt-3 pt-3 border-t border-slate-200/50 flex items-center justify-between text-xs font-medium text-slate-500">
                    <span>{subtitle}</span>
                </div>
            )}
        </div>
    )
}

export default StatCard