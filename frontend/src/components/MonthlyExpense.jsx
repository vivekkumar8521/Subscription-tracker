export default function MonthlyExpense({ amount }) {
  const formatted = typeof amount === 'number' 
    ? amount.toFixed(2) 
    : (parseFloat(amount) || 0).toFixed(2)

  return (
    <div className="card">
      <div className="text-sm text-gray-600 dark:text-gray-400 mb-1">Total Monthly</div>
      <div className="text-4xl font-bold text-gray-900 dark:text-white mb-1">${formatted}</div>
      <div className="text-xs text-gray-500 dark:text-gray-500">recurring expense</div>
    </div>
  )
}
