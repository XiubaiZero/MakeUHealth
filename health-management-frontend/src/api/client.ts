import axios from 'axios'

const AUTH_SESSION_KEY = 'health-management-auth-session'

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim() || '/api'

const apiClient = axios.create({
  baseURL: apiBaseUrl,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const message =
      error.response?.data?.error ||
      error.response?.data?.message ||
      error.response?.data?.detail ||
      error.response?.data?.title ||
      error.message ||
      'Request failed'

    const normalizedError = new Error(message) as Error & { status?: number }
    normalizedError.status = error.response?.status
    return Promise.reject(normalizedError)
  },
)

apiClient.interceptors.request.use((config) => {
  const raw = localStorage.getItem(AUTH_SESSION_KEY)
  let token = ''
  if (raw) {
    try {
      const session = JSON.parse(raw) as { token?: string }
      token = session.token || ''
    } catch {
      token = ''
    }
  }
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export default apiClient
