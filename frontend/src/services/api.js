import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Add token to requests
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Handle 401 errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export const authAPI = {
  login: async (username, password) => {
    const response = await api.post('/auth/login', { username, password })
    return response.data
  },
  register: async (username, password) => {
    const response = await api.post('/auth/register', { username, password })
    return response.data
  },
}

export const taskAPI = {
  getAll: async (status = null) => {
    const params = status ? { status } : {}
    const response = await api.get('/api/tasks', { params })
    // Handle HATEOAS PagedModel response structure
    // The backend returns: { _embedded: { taskResponseList: [...] }, page: {...} }
    if (response.data._embedded) {
      // Check for taskResponseList (HATEOAS collection name)
      const collectionName = Object.keys(response.data._embedded)[0]
      if (collectionName) {
        return response.data._embedded[collectionName]
      }
    }
    // Fallback for direct array or content array
    if (Array.isArray(response.data)) {
      return response.data
    }
    if (response.data.content && Array.isArray(response.data.content)) {
      return response.data.content
    }
    return []
  },
  getById: async (id) => {
    const response = await api.get(`/api/tasks/${id}`)
    return response.data
  },
  create: async (taskData) => {
    const response = await api.post('/api/tasks', taskData)
    return response.data
  },
  update: async (id, taskData) => {
    const response = await api.put(`/api/tasks/${id}`, taskData)
    return response.data
  },
  patch: async (id, taskData) => {
    const response = await api.patch(`/api/tasks/${id}`, taskData)
    return response.data
  },
  delete: async (id) => {
    await api.delete(`/api/tasks/${id}`)
  },
}

export default api

