import { useAuth } from '../context/AuthContext'
import { Link, useLocation, useNavigate } from 'react-router-dom'

export default function Layout({ children }) {
  const { user, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const navItems = [
    { label: 'Dashboard', to: '/dashboard' },
    { label: 'Subscriptions', to: '/dashboard' },
    { label: 'Payments', to: '/dashboard' },
    { label: 'Analytics', to: '/dashboard' },
    { label: 'Connected Accounts', to: '/dashboard' },
    { label: 'Reports', to: '/dashboard' },
    { label: 'Settings', to: '/dashboard' },
  ]

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 md:flex">
      <aside className="md:w-64 border-r border-slate-800 bg-slate-900/70 backdrop-blur-md p-4">
        <Link to="/dashboard" className="flex items-center gap-2 text-xl font-bold text-indigo-300 hover:text-indigo-200 mb-6">
          <span className="text-2xl">◆</span>
          Subscription Tracker
        </Link>
        <nav className="space-y-2">
          {navItems.map((item) => (
            <Link
              key={item.label}
              to={item.to}
              className={`block rounded-lg px-3 py-2 text-sm transition ${
                location.pathname === item.to
                  ? 'bg-indigo-600/30 text-indigo-200 border border-indigo-500/40'
                  : 'text-slate-300 hover:bg-slate-800 hover:text-slate-100'
              }`}
            >
              {item.label}
            </Link>
          ))}
        </nav>
      </aside>

      <div className="flex-1">
        <header className="border-b border-slate-800 bg-slate-900/60 backdrop-blur-sm">
          <div className="px-4 sm:px-6 lg:px-8">
            <div className="flex justify-between items-center h-16">
              <div className="text-sm text-slate-400">Welcome back, {user?.username}</div>
              <button
                onClick={handleLogout}
                className="px-4 py-2 text-sm font-medium text-slate-200 bg-slate-800 hover:bg-slate-700 rounded-lg transition-colors"
              >
                Log out
              </button>
            </div>
          </div>
        </header>
        <main>{children}</main>
      </div>
    </div>
  )
}
