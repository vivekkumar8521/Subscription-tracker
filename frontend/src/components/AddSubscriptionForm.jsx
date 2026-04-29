import { useState, useEffect } from 'react'
import api from '../services/api'

const BILLING_CYCLES = ['MONTHLY', 'QUARTERLY', 'YEARLY', 'WEEKLY']
const CATEGORIES = ['OTT', 'Music', 'Cloud', 'Gaming', 'Education', 'Productivity', 'Finance', 'Other']

export default function AddSubscriptionForm({ onClose, onSuccess, editId, subscriptions }) {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [amount, setAmount] = useState('')
  const [billingCycle, setBillingCycle] = useState('MONTHLY')
  const [startDate, setStartDate] = useState(() => {
    const d = new Date()
    return d.toISOString().slice(0, 10)
  })
  const [category, setCategory] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const subscription = editId ? subscriptions?.find((s) => s.id === editId) : null

  useEffect(() => {
    if (subscription) {
      setName(subscription.name)
      setDescription(subscription.description || '')
      setAmount(subscription.amount?.toString() || '')
      setBillingCycle(subscription.billingCycle || 'MONTHLY')
      setStartDate(
        subscription.startDate
          ? new Date(subscription.startDate).toISOString().slice(0, 10)
          : new Date().toISOString().slice(0, 10)
      )
      setCategory(subscription.category || '')
    } else {
      setName('')
      setDescription('')
      setAmount('')
      setBillingCycle('MONTHLY')
      setStartDate(new Date().toISOString().slice(0, 10))
      setCategory('')
      setError('')
    }
  }, [subscription, editId])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const payload = {
        name,
        description: description || undefined,
        amount: parseFloat(amount),
        billingCycle,
        startDate: startDate || new Date().toISOString().slice(0, 10),
        category: category || undefined,
      }
      if (editId) {
        await api.put(`/subscriptions/${editId}`, payload)
      } else {
        await api.post('/subscriptions', payload)
      }
      onSuccess()
    } catch (err) {
      const data = err.response?.data
      const msg = data?.message || data?.error
      setError(msg || (err.code === 'ECONNREFUSED' ? 'Backend not reachable.' : 'Failed to save.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4" onClick={onClose}>
      <div className="card max-w-lg w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white">
            {editId ? 'Edit Subscription' : 'Add Subscription'}
          </h2>
          <button type="button" className="text-gray-500 hover:text-gray-700 text-2xl" onClick={onClose}>
            ×
          </button>
        </div>
        <form onSubmit={handleSubmit} className="space-y-4">
          {error && (
            <div className="bg-red-100 dark:bg-red-900 border border-red-400 text-red-700 dark:text-red-300 px-4 py-3 rounded-lg text-sm">
              {error}
            </div>
          )}
          <div>
            <label htmlFor="name" className="block text-sm font-medium mb-2 text-gray-700 dark:text-gray-300">
              Name *
            </label>
            <input
              id="name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Netflix"
              required
              className="input-field"
            />
          </div>
          <div>
            <label htmlFor="description" className="block text-sm font-medium mb-2 text-gray-700 dark:text-gray-300">
              Description
            </label>
            <textarea
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Optional notes"
              rows={2}
              className="input-field"
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="amount" className="block text-sm font-medium mb-2 text-gray-700 dark:text-gray-300">
                Amount ($) *
              </label>
              <input
                id="amount"
                type="number"
                step="0.01"
                min="0"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                required
                className="input-field"
              />
            </div>
            <div>
              <label htmlFor="billingCycle" className="block text-sm font-medium mb-2 text-gray-700 dark:text-gray-300">
                Billing cycle
              </label>
              <select
                id="billingCycle"
                value={billingCycle}
                onChange={(e) => setBillingCycle(e.target.value)}
                className="input-field"
              >
                {BILLING_CYCLES.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="startDate" className="block text-sm font-medium mb-2 text-gray-700 dark:text-gray-300">
                Start date
              </label>
              <input
                id="startDate"
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="input-field"
              />
            </div>
            <div>
              <label htmlFor="category" className="block text-sm font-medium mb-2 text-gray-700 dark:text-gray-300">
                Category
              </label>
              <select
                id="category"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="input-field"
              >
                <option value="">Select category</option>
                {CATEGORIES.map((c) => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            </div>
          </div>
          <div className="flex gap-3 pt-4">
            <button type="button" className="btn-secondary flex-1" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn-primary flex-1" disabled={loading}>
              {loading ? 'Saving...' : editId ? 'Update' : 'Add Subscription'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
