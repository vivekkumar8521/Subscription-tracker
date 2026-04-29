import { useState, useEffect } from 'react'
import api from '../services/api'

export default function Notifications() {
  const [notifications, setNotifications] = useState([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [showAll, setShowAll] = useState(false)

  useEffect(() => {
    fetchNotifications()
    fetchUnreadCount()
    const interval = setInterval(() => {
      fetchUnreadCount()
    }, 30000) // Check every 30 seconds
    return () => clearInterval(interval)
  }, [])

  const fetchNotifications = async () => {
    try {
      const { data } = await api.get('/notifications')
      setNotifications(data)
    } catch (err) {
      // Silently fail - notifications are optional
    }
  }

  const fetchUnreadCount = async () => {
    try {
      const { data } = await api.get('/notifications/unread-count')
      setUnreadCount(data.count || 0)
    } catch (err) {
      // Silently fail
    }
  }

  const markAsRead = async (id) => {
    try {
      await api.put(`/notifications/${id}/read`)
      await fetchNotifications()
      await fetchUnreadCount()
    } catch (err) {
      console.error('Failed to mark as read:', err)
    }
  }

  const unread = notifications.filter(n => !n.isRead).slice(0, 3)

  if (unreadCount === 0 && !showAll) return null

  return (
    <div className="card">
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-lg font-semibold flex items-center gap-2">
          🔔 Notifications
          {unreadCount > 0 && (
            <span className="bg-red-500 text-white text-xs px-2 py-1 rounded-full">
              {unreadCount}
            </span>
          )}
        </h3>
        {notifications.length > 3 && (
          <button
            onClick={() => setShowAll(!showAll)}
            className="text-sm text-blue-600 hover:text-blue-700"
          >
            {showAll ? 'Show Less' : 'Show All'}
          </button>
        )}
      </div>
      <div className="space-y-2">
        {(showAll ? notifications : unread).map((notif) => (
          <div
            key={notif.id}
            className={`p-3 rounded-lg border ${
              notif.isRead
                ? 'bg-gray-50 dark:bg-gray-800 border-gray-200 dark:border-gray-700'
                : 'bg-blue-50 dark:bg-blue-900 border-blue-200 dark:border-blue-800'
            }`}
          >
            <div className="flex justify-between items-start">
              <div className="flex-1">
                <p className="font-medium text-sm">{notif.title}</p>
                <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">{notif.message}</p>
                <p className="text-xs text-gray-500 mt-1">
                  {new Date(notif.createdAt).toLocaleString()}
                </p>
              </div>
              {!notif.isRead && (
                <button
                  onClick={() => markAsRead(notif.id)}
                  className="text-xs text-blue-600 hover:text-blue-700 ml-2"
                >
                  Mark read
                </button>
              )}
            </div>
          </div>
        ))}
        {notifications.length === 0 && (
          <p className="text-gray-500 text-center py-4">No notifications</p>
        )}
      </div>
    </div>
  )
}
