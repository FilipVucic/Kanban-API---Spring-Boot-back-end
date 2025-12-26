import { createContext, useContext, useState, useEffect } from 'react'
import { authAPI } from '../services/api'

const AuthContext = createContext(null)

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = localStorage.getItem('token')
    const username = localStorage.getItem('username')
    if (token && username) {
      setUser({ username, token })
    }
    setLoading(false)
  }, [])

  const getErrorMessage = (error) => {
    // Extract error message from API response
    if (error.response?.data?.message) {
      return error.response.data.message
    }
    // Check for validation errors
    if (error.response?.data?.validationErrors) {
      const validationErrors = Object.values(error.response.data.validationErrors)
      return validationErrors.join(', ')
    }
    // Fallback to error message or default
    return error.message || 'An unexpected error occurred'
  }

  const login = async (username, password) => {
    try {
      const response = await authAPI.login(username, password)
      localStorage.setItem('token', response.token)
      localStorage.setItem('username', response.username)
      setUser({ username: response.username, token: response.token })
      return { success: true }
    } catch (error) {
      return {
        success: false,
        error: getErrorMessage(error),
      }
    }
  }

  const register = async (username, password) => {
    try {
      const response = await authAPI.register(username, password)
      localStorage.setItem('token', response.token)
      localStorage.setItem('username', response.username)
      setUser({ username: response.username, token: response.token })
      return { success: true }
    } catch (error) {
      return {
        success: false,
        error: getErrorMessage(error),
      }
    }
  }

  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, register, logout, loading }}>
      {children}
    </AuthContext.Provider>
  )
}

