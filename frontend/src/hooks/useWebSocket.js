import { useEffect, useRef, useCallback } from 'react'
import { useAuth } from '../context/AuthContext'
import webSocketService from '../services/websocket'

const TASK_TOPIC = '/topic/tasks'

export const useWebSocket = (onTaskEvent) => {
  const { user } = useAuth()
  const onTaskEventRef = useRef(onTaskEvent)

  // Keep callback ref up to date
  useEffect(() => {
    onTaskEventRef.current = onTaskEvent
  }, [onTaskEvent])

  const handleTaskEvent = useCallback((event) => {
    if (onTaskEventRef.current) {
      onTaskEventRef.current(event)
    }
  }, [])

  useEffect(() => {
    // Only connect if user is authenticated
    if (!user) {
      webSocketService.disconnect()
      return
    }

    const handleConnect = () => {
      console.log('WebSocket connected, subscribing to tasks...')
      webSocketService.subscribe(TASK_TOPIC, handleTaskEvent)
    }

    const handleError = (error) => {
      console.error('WebSocket error:', error)
    }

    const handleDisconnect = () => {
      console.log('WebSocket disconnected')
    }

    // Connect to WebSocket
    webSocketService.connect(handleConnect, handleError, handleDisconnect)

    // Cleanup on unmount or when user changes
    return () => {
      webSocketService.unsubscribe(TASK_TOPIC)
      // Don't disconnect completely as it might be used by other components
      // webSocketService.disconnect()
    }
  }, [user, handleTaskEvent])

  return {
    isConnected: webSocketService.isConnected(),
  }
}

