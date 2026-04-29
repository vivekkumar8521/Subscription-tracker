function toNumber(value) {
  const n = parseFloat(value)
  return Number.isFinite(n) ? n : 0
}

function formatMoney(value) {
  return `$${toNumber(value).toFixed(2)}`
}

function BarRow({ label, value, max, color = 'bg-indigo-500' }) {
  const width = max > 0 ? Math.max(6, Math.round((value / max) * 100)) : 0
  return (
    <div className="space-y-1">
      <div className="flex justify-between text-xs text-slate-300">
        <span>{label}</span>
        <span>{formatMoney(value)}</span>
      </div>
      <div className="h-2 rounded-full bg-slate-700">
        <div className={`h-2 rounded-full ${color}`} style={{ width: `${width}%` }} />
      </div>
    </div>
  )
}

export default function PremiumAnalytics({ subscriptions = [], reminders = [] }) {
  const totalActive = subscriptions.length
  const monthlySpend = subscriptions.reduce((sum, s) => sum + toNumber(s.amount), 0)
  const upcomingRenewals = reminders.length
  const expiredPlans = subscriptions.filter((s) => s.nextRenewalDate && new Date(s.nextRenewalDate) < new Date()).length

  const categoryMap = subscriptions.reduce((acc, s) => {
    const key = s.category || 'Other'
    acc[key] = (acc[key] || 0) + toNumber(s.amount)
    return acc
  }, {})
  const categoryData = Object.entries(categoryMap).map(([label, value]) => ({ label, value }))
  const maxCategory = Math.max(0, ...categoryData.map((i) => i.value))

  const monthMap = subscriptions.reduce((acc, s) => {
    if (!s.createdAt) return acc
    const d = new Date(s.createdAt)
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
    acc[key] = (acc[key] || 0) + toNumber(s.amount)
    return acc
  }, {})
  const monthData = Object.entries(monthMap).sort((a, b) => a[0].localeCompare(b[0])).slice(-6)
  const maxMonth = Math.max(0, ...monthData.map(([, v]) => v))

  const renewalTrend = reminders.reduce((acc, s) => {
    if (!s.nextRenewalDate) return acc
    const d = new Date(s.nextRenewalDate)
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
    acc[key] = (acc[key] || 0) + 1
    return acc
  }, {})
  const renewalData = Object.entries(renewalTrend).sort((a, b) => a[0].localeCompare(b[0])).slice(-6)
  const maxRenewal = Math.max(1, ...renewalData.map(([, v]) => v))

  const cards = [
    { label: 'Total Active Subscriptions', value: totalActive, accent: 'text-indigo-300' },
    { label: 'Monthly Spending', value: formatMoney(monthlySpend), accent: 'text-emerald-300' },
    { label: 'Upcoming Renewals', value: upcomingRenewals, accent: 'text-amber-300' },
    { label: 'Expired Plans', value: expiredPlans, accent: 'text-rose-300' },
  ]

  return (
    <section className="space-y-4">
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {cards.map((card) => (
          <article key={card.label} className="rounded-xl border border-slate-700 bg-slate-800/60 p-4 shadow-lg">
            <p className="text-xs text-slate-400">{card.label}</p>
            <p className={`mt-2 text-2xl font-semibold ${card.accent}`}>{card.value}</p>
          </article>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <article className="rounded-xl border border-slate-700 bg-slate-800/60 p-4">
          <h3 className="font-semibold mb-3 text-slate-100">Monthly Expense Graph</h3>
          <div className="space-y-2">
            {monthData.length === 0 ? <p className="text-sm text-slate-400">No monthly data</p> : monthData.map(([m, v]) => (
              <BarRow key={m} label={m} value={v} max={maxMonth} color="bg-indigo-500" />
            ))}
          </div>
        </article>

        <article className="rounded-xl border border-slate-700 bg-slate-800/60 p-4">
          <h3 className="font-semibold mb-3 text-slate-100">Category-wise Spending</h3>
          <div className="space-y-2">
            {categoryData.length === 0 ? <p className="text-sm text-slate-400">No category data</p> : categoryData.map((item) => (
              <BarRow key={item.label} label={item.label} value={item.value} max={maxCategory} color="bg-cyan-500" />
            ))}
          </div>
        </article>

        <article className="rounded-xl border border-slate-700 bg-slate-800/60 p-4">
          <h3 className="font-semibold mb-3 text-slate-100">Renewal Trends</h3>
          <div className="space-y-2">
            {renewalData.length === 0 ? <p className="text-sm text-slate-400">No renewal trend data</p> : renewalData.map(([m, v]) => (
              <div key={m} className="flex items-center justify-between text-sm">
                <span className="text-slate-300">{m}</span>
                <div className="flex items-center gap-2">
                  <div className="h-2 w-24 rounded-full bg-slate-700 overflow-hidden">
                    <div className="h-2 bg-violet-500" style={{ width: `${Math.max(8, (v / maxRenewal) * 100)}%` }} />
                  </div>
                  <span className="text-slate-300">{v}</span>
                </div>
              </div>
            ))}
          </div>
        </article>
      </div>
    </section>
  )
}
