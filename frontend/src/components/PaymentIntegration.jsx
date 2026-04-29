import { useState, useEffect } from 'react'
import api from '../services/api'

export default function PaymentIntegration({ onClose, onSuccess }) {
  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(false)
  const [syncingId, setSyncingId] = useState(null)
  const [error, setError] = useState('')
  const [accountType, setAccountType] = useState('PHONEPE')
  const [showConnect, setShowConnect] = useState(false)
  const [showHistory, setShowHistory] = useState(false)
  const [history, setHistory] = useState([])
  const [otpCode, setOtpCode] = useState('')
  const [otpAccountId, setOtpAccountId] = useState(null)
  const [otpHint, setOtpHint] = useState('')

  const providerMeta = {
    PHONEPE: { label: 'PhonePe', icon: '🟣' },
    GOOGLE_PAY: { label: 'Google Pay', icon: '🟢' },
    PAYTM: { label: 'Paytm', icon: '🔵' },
    UPI: { label: 'UPI', icon: '💠' },
    BANK: { label: 'Bank', icon: '🏦' },
  }

  useEffect(() => {
    fetchAccounts()
    const interval = setInterval(() => fetchAccounts(true), 4000)
    return () => clearInterval(interval)
  }, [])

  const fetchAccounts = async (silent = false) => {
    try {
      const { data } = await api.get('/payments/accounts')
      setAccounts(data)
    } catch (err) {
      if (!silent) setError('Failed to fetch connected accounts')
    }
  }

  const fetchSyncHistory = async () => {
    try {
      const { data } = await api.get('/payments/sync-history')
      setHistory(data)
      setShowHistory(true)
    } catch (err) {
      setError('Failed to fetch sync history')
    }
  }

  const handleConnect = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await api.post('/payments/connect', {
        accountType,
        accountIdentifier: `user_${Date.now()}`, // Simulated
        accessToken: 'simulated_token', // In production, use OAuth flow
        refreshToken: 'simulated_refresh'
      })
      await fetchAccounts()
      setShowConnect(false)
      if (onSuccess) onSuccess()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to connect account')
    } finally {
      setLoading(false)
    }
  }

  const handleOtpRequest = async (accountId) => {
    setError('')
    try {
      const { data } = await api.post(`/payments/sync/${accountId}/request-otp`)
      setOtpAccountId(accountId)
      setOtpHint(data.otp ? `Demo OTP: ${data.otp}` : 'OTP sent successfully')
    } catch (err) {
      setError(err.response?.data?.message || 'Could not generate OTP')
    }
  }

  const verifyOtp = async () => {
    if (!otpAccountId || !otpCode.trim()) return
    setError('')
    try {
      await api.post(`/payments/sync/${otpAccountId}/verify-otp`, { code: otpCode })
      setOtpHint('OTP verified. You can sync now.')
      setOtpCode('')
    } catch (err) {
      setError(err.response?.data?.message || 'OTP verification failed')
    }
  }

  const handleSync = async (accountId) => {
    setSyncingId(accountId)
    setError('')
    try {
      await api.post(`/payments/sync/${accountId}`)
      await fetchAccounts(true)
      await fetchSyncHistory()
      if (onSuccess) onSuccess()
    } catch (err) {
      setError(err.response?.data?.message || 'Sync failed')
    } finally {
      setSyncingId(null)
    }
  }

  const toggleAutoSync = async (accountId, enabled) => {
    try {
      await api.patch(`/payments/accounts/${accountId}/auto-sync`, { enabled })
      fetchAccounts(true)
    } catch (err) {
      setError('Failed to update auto sync')
    }
  }

  const statusClass = (status) => ({
    CONNECTED: 'bg-emerald-900/40 text-emerald-300 border-emerald-700/60',
    SYNCING: 'bg-blue-900/40 text-blue-300 border-blue-700/60',
    FAILED: 'bg-red-900/40 text-red-300 border-red-700/60',
    EXPIRED: 'bg-amber-900/40 text-amber-300 border-amber-700/60',
  }[status || 'CONNECTED'])

  return (
    <div className="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="w-full max-w-4xl max-h-[90vh] overflow-y-auto rounded-2xl border border-white/10 bg-slate-900/85 shadow-2xl p-6 text-slate-100">
        <div className="flex justify-between items-center mb-6">
          <div>
            <h2 className="text-2xl font-bold tracking-tight">Payment Accounts</h2>
            <p className="text-sm text-slate-400">Securely connect and sync your payment apps</p>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-200 text-2xl">×</button>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-red-900/35 border border-red-500/40 text-red-200 rounded-lg">
            {error}
          </div>
        )}

        {otpAccountId && (
          <div className="mb-4 rounded-lg border border-indigo-500/40 bg-indigo-900/20 p-4">
            <p className="text-sm text-indigo-200 mb-2">OTP required before sync.</p>
            {otpHint && <p className="text-xs text-indigo-300 mb-3">{otpHint}</p>}
            <div className="flex gap-2">
              <input
                value={otpCode}
                onChange={(e) => setOtpCode(e.target.value)}
                placeholder="Enter 6-digit OTP"
                className="input-field bg-slate-800/70 border-slate-600 text-slate-100"
              />
              <button type="button" onClick={verifyOtp} className="btn-primary">Verify</button>
            </div>
          </div>
        )}

        {!showConnect ? (
          <>
            <div className="mb-5 flex flex-wrap gap-3">
              <button onClick={() => setShowConnect(true)} className="btn-primary">
                + Connect Account
              </button>
              <button onClick={fetchSyncHistory} className="btn-secondary">
                View Sync History
              </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {accounts.length === 0 ? (
                <p className="text-slate-400 text-center py-8 col-span-full">No accounts connected</p>
              ) : (
                accounts.map((acc) => (
                  <div
                    key={acc.id}
                    className="rounded-xl border border-slate-700/70 bg-slate-800/55 p-4 shadow-lg transition-all hover:-translate-y-0.5 hover:shadow-blue-900/20"
                  >
                    <div className="flex items-start justify-between">
                      <div className="space-y-2">
                        <h3 className="font-semibold text-lg flex items-center gap-2">
                          <span>{providerMeta[acc.accountType]?.icon || '💳'}</span>
                          <span>{providerMeta[acc.accountType]?.label || acc.accountType}</span>
                        </h3>
                        <span className={`inline-flex items-center rounded-full border px-2 py-1 text-xs font-medium ${statusClass(acc.syncStatus)}`}>
                          {acc.syncStatus || 'CONNECTED'}
                        </span>
                        <p className="text-xs text-slate-400">
                          Last sync: {acc.lastSync ? new Date(acc.lastSync).toLocaleString() : 'Never'}
                        </p>
                        <p className="text-xs text-slate-400">
                          Last transaction: {acc.lastTransactionAt ? new Date(acc.lastTransactionAt).toLocaleString() : 'No transactions yet'}
                        </p>
                        {acc.lastSyncError && (
                          <p className="text-xs text-rose-300">Error: {acc.lastSyncError}</p>
                        )}
                      </div>
                      <div className="flex flex-col gap-2 items-end">
                        <button
                          onClick={() => handleOtpRequest(acc.id)}
                          className="text-xs px-3 py-1 rounded-lg bg-indigo-600/80 hover:bg-indigo-500"
                        >
                          Request OTP
                        </button>
                        <button
                          onClick={() => handleSync(acc.id)}
                          className="btn-secondary text-sm min-w-[100px] flex items-center justify-center gap-2"
                          disabled={syncingId === acc.id || acc.syncStatus === 'SYNCING'}
                        >
                          {(syncingId === acc.id || acc.syncStatus === 'SYNCING') && (
                            <span className="inline-block h-4 w-4 border-2 border-slate-300 border-t-transparent rounded-full animate-spin" />
                          )}
                          {(syncingId === acc.id || acc.syncStatus === 'SYNCING') ? 'Syncing' : 'Sync Now'}
                        </button>
                        {acc.syncStatus === 'FAILED' && (
                          <button
                            onClick={() => handleSync(acc.id)}
                            className="text-xs text-rose-300 hover:text-rose-200"
                          >
                            Retry
                          </button>
                        )}
                      </div>
                    </div>
                    <div className="mt-4 flex items-center justify-between rounded-lg bg-slate-900/70 p-3">
                      <span className="text-sm text-slate-300">Auto Sync</span>
                      <button
                        type="button"
                        onClick={() => toggleAutoSync(acc.id, !acc.autoSync)}
                        className={`relative inline-flex h-6 w-11 items-center rounded-full transition ${acc.autoSync ? 'bg-emerald-500' : 'bg-slate-600'}`}
                      >
                        <span className={`inline-block h-4 w-4 transform rounded-full bg-white transition ${acc.autoSync ? 'translate-x-6' : 'translate-x-1'}`} />
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </>
        ) : (
          <form onSubmit={handleConnect} className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-2 text-slate-200">Payment Provider</label>
              <select
                value={accountType}
                onChange={(e) => setAccountType(e.target.value)}
                className="input-field bg-slate-800/70 border-slate-600 text-slate-100"
                required
              >
                <option value="PHONEPE">PhonePe</option>
                <option value="GOOGLE_PAY">Google Pay</option>
                <option value="PAYTM">Paytm</option>
                <option value="UPI">UPI</option>
                <option value="BANK">Bank</option>
              </select>
            </div>
            <p className="text-sm text-slate-400">
              Note: In production, this would use OAuth2 to securely connect your account.
              For now, this is a simulated connection.
            </p>
            <div className="flex gap-3">
              <button type="submit" className="btn-primary" disabled={loading}>
                {loading ? 'Connecting...' : 'Connect'}
              </button>
              <button type="button" onClick={() => setShowConnect(false)} className="btn-secondary">
                Cancel
              </button>
            </div>
          </form>
        )}

        {showHistory && (
          <div className="mt-6 rounded-xl border border-slate-700 bg-slate-800/60 p-4">
            <div className="flex justify-between items-center mb-3">
              <h3 className="font-semibold">Sync History</h3>
              <button onClick={() => setShowHistory(false)} className="text-xs text-slate-400 hover:text-slate-200">
                Hide
              </button>
            </div>
            <div className="space-y-2 max-h-56 overflow-y-auto">
              {history.length === 0 ? (
                <p className="text-sm text-slate-400">No sync history yet.</p>
              ) : history.map((item) => (
                <div key={item.id} className="rounded-lg bg-slate-900/70 p-3 text-sm">
                  <div className="flex justify-between">
                    <span className="font-medium">{providerMeta[item.paymentAccountType]?.label || item.paymentAccountType || 'Account'}</span>
                    <span className={`text-xs ${item.status === 'FAILED' ? 'text-rose-300' : item.status === 'SYNCING' ? 'text-blue-300' : 'text-emerald-300'}`}>
                      {item.status}
                    </span>
                  </div>
                  <p className="text-slate-400 text-xs mt-1">{item.details || item.errorMessage || 'No details'}</p>
                  <p className="text-slate-500 text-xs mt-1">{item.createdAt ? new Date(item.createdAt).toLocaleString() : ''}</p>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
