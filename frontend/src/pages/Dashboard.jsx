import { useState, useEffect } from 'react'
import api from '../services/api'
import AddSubscriptionForm from '../components/AddSubscriptionForm'
import PaymentIntegration from '../components/PaymentIntegration'
import SubscriptionList from '../components/SubscriptionList'
import UpcomingReminders from '../components/UpcomingReminders'
import MonthlyExpense from '../components/MonthlyExpense'
import Notifications from '../components/Notifications'
import PremiumAnalytics from '../components/PremiumAnalytics'

export default function Dashboard() {
  const [dashboard, setDashboard] = useState(null)
  const [recommendations, setRecommendations] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [showPayments, setShowPayments] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [search, setSearch] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('ALL')
  const [maxPrice, setMaxPrice] = useState('')
  const [renewBefore, setRenewBefore] = useState('')

  const fetchDashboard = async () => {
    setError('')
    try {
      const { data } = await api.get('/subscriptions/dashboard')
      setDashboard(data)
    } catch (err) {
      setError(err.code === 'ECONNREFUSED' ? 'Backend not reachable. Start it first.' : 'Failed to load dashboard.')
      setDashboard({ subscriptions: [], totalMonthlyExpense: 0, upcomingReminders: [] })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchDashboard()
    fetchRecommendations()
  }, [])

  const fetchRecommendations = async () => {
    try {
      const { data } = await api.get('/subscriptions/recommendations')
      setRecommendations(data?.suggestions || [])
    } catch {
      setRecommendations([])
    }
  }

  const handleSubscriptionAdded = () => {
    setShowForm(false)
    fetchDashboard()
  }

  const handleSubscriptionUpdated = () => {
    setEditingId(null)
    fetchDashboard()
  }

  const handleSubscriptionDeleted = async (id) => {
    try {
      await api.delete(`/subscriptions/${id}`)
      fetchDashboard()
    } catch (err) {
      alert('Failed to delete subscription')
    }
  }

  const handleEdit = (id) => {
    setEditingId(id)
    setShowForm(true)
  }

  const subscriptions = dashboard?.subscriptions || []
  const uniqueCategories = ['ALL', ...new Set(subscriptions.map((s) => s.category).filter(Boolean))]
  const filteredSubscriptions = subscriptions.filter((s) => {
    const q = search.trim().toLowerCase()
    const textMatch = !q || `${s.name} ${s.category || ''}`.toLowerCase().includes(q)
    const categoryMatch = categoryFilter === 'ALL' || (s.category || '') === categoryFilter
    const priceMatch = !maxPrice || parseFloat(s.amount) <= parseFloat(maxPrice)
    const renewalMatch = !renewBefore || (s.nextRenewalDate && new Date(s.nextRenewalDate) <= new Date(renewBefore))
    return textMatch && categoryMatch && priceMatch && renewalMatch
  })

  const exportCsv = () => {
    const rows = [
      ['Name', 'Category', 'Amount', 'Billing Cycle', 'Next Renewal'],
      ...filteredSubscriptions.map((s) => [
        s.name,
        s.category || '',
        s.amount,
        s.billingCycle,
        s.nextRenewalDate || '',
      ]),
    ]
    const csv = rows.map((r) => r.map((v) => `"${String(v).replaceAll('"', '""')}"`).join(',')).join('\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'subscriptions-report.csv'
    a.click()
    URL.revokeObjectURL(url)
  }

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600 dark:text-gray-400">Loading your subscriptions...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 p-4 md:p-8">
      <div className="max-w-7xl mx-auto space-y-6">
        {error && (
          <div className="bg-red-100 dark:bg-red-900 border border-red-400 text-red-700 dark:text-red-300 px-4 py-3 rounded-lg" role="alert">
            {error}
          </div>
        )}

        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
          <h1 className="text-3xl font-bold text-slate-100">Dashboard</h1>
          <div className="flex gap-3">
            <button
              onClick={() => setShowPayments(true)}
              className="btn-secondary"
            >
              💳 Connect Payment
            </button>
            <button
              onClick={() => { setShowForm(true); setEditingId(null); }}
              className="btn-primary"
            >
              + Add Subscription
            </button>
          </div>
        </div>

        <Notifications />

        <PremiumAnalytics
          subscriptions={dashboard?.subscriptions || []}
          reminders={dashboard?.upcomingReminders || []}
        />

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <MonthlyExpense amount={dashboard?.totalMonthlyExpense || 0} />
          <UpcomingReminders reminders={dashboard?.upcomingReminders || []} />
        </div>

        <section className="card">
          <h2 className="text-xl font-semibold mb-4 text-gray-900 dark:text-white">Your Subscriptions</h2>
          <div className="grid grid-cols-1 md:grid-cols-5 gap-3 mb-4">
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search subscriptions..."
              className="input-field md:col-span-2"
            />
            <select value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)} className="input-field">
              {uniqueCategories.map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
            <input
              type="number"
              min="0"
              value={maxPrice}
              onChange={(e) => setMaxPrice(e.target.value)}
              placeholder="Max price"
              className="input-field"
            />
            <input
              type="date"
              value={renewBefore}
              onChange={(e) => setRenewBefore(e.target.value)}
              className="input-field"
            />
          </div>
          <div className="flex justify-end mb-3">
            <button onClick={exportCsv} className="btn-secondary">Export CSV</button>
          </div>
          <SubscriptionList
            subscriptions={filteredSubscriptions}
            onEdit={handleEdit}
            onDelete={handleSubscriptionDeleted}
          />
        </section>

        <section className="rounded-xl border border-slate-700 bg-slate-800/60 p-5">
          <h3 className="font-semibold text-slate-100 mb-2">Smart Suggestion</h3>
          {(recommendations.length ? recommendations : ['AI suggestions will appear after more activity.']).map((msg, idx) => (
            <p key={idx} className="text-sm text-slate-300 mb-1">{msg}</p>
          ))}
        </section>

        {showForm && (
          <AddSubscriptionForm
            onClose={() => { setShowForm(false); setEditingId(null); }}
            onSuccess={editingId ? handleSubscriptionUpdated : handleSubscriptionAdded}
            editId={editingId}
            subscriptions={dashboard?.subscriptions || []}
          />
        )}

        {showPayments && (
          <PaymentIntegration
            onClose={() => setShowPayments(false)}
            onSuccess={fetchDashboard}
          />
        )}
      </div>
    </div>
  )
}
