import axios from 'axios'

// Backend on 8081 – works even if .env not loaded (restart frontend after adding .env)
const baseURL = import.meta.env.VITE_API_URL || 'http://localhost:8081/api'
const api = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export default api
