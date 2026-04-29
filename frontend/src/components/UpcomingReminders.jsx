function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const today = new Date()
  const tomorrow = new Date(today)
  tomorrow.setDate(tomorrow.getDate() + 1)
  
  if (d.toDateString() === today.toDateString()) return 'Today'
  if (d.toDateString() === tomorrow.toDateString()) return 'Tomorrow'
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}

export default function UpcomingReminders({ reminders }) {
  if (!reminders?.length) {
    return (
      <div className="card">
        <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Upcoming (14 days)</h3>
        <p className="text-gray-500 dark:text-gray-400 text-center py-4">No upcoming renewals</p>
      </div>
    )
  }

  return (
    <div className="card">
      <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Upcoming (14 days)</h3>
      <ul className="space-y-3">
        {reminders.map((r) => (
          <li key={r.id} className="flex justify-between items-center p-3 bg-gray-50 dark:bg-gray-700 rounded-lg">
            <div className="flex-1">
              <span className="block text-sm font-medium text-gray-900 dark:text-white">{r.name}</span>
              <span className="block text-xs text-gray-500 dark:text-gray-400 mt-1">{formatDate(r.nextRenewalDate)}</span>
            </div>
            <span className="text-sm font-semibold text-gray-900 dark:text-white">${parseFloat(r.amount).toFixed(2)}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}
