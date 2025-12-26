import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const WS_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

class WebSocketService {
  constructor() {
    this.client = null
    this.subscriptions = new Map()
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 3000
    this.isConnecting = false
  }

  connect(onConnect, onError, onDisconnect) {
    if (this.isConnecting || (this.client && this.client.connected)) {
      return
    }

    this.isConnecting = true

    // Create SockJS connection
    const socket = new SockJS(`${WS_BASE_URL}/ws`)
    
    this.client = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: this.reconnectDelay,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => {
        // Uncomment for debugging: console.log('STOMP:', str)
      },
      onConnect: (frame) => {
        console.log('WebSocket connected:', frame)
        this.isConnecting = false
        this.reconnectAttempts = 0
        if (onConnect) onConnect()
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame)
        this.isConnecting = false
        if (onError) onError(frame)
      },
      onWebSocketClose: (event) => {
        console.log('WebSocket closed:', event)
        this.isConnecting = false
        if (onDisconnect) onDisconnect(event)
        // Only attempt reconnect if it wasn't a manual disconnect
        if (this.client) {
          this.attemptReconnect(onConnect, onError, onDisconnect)
        }
      },
      onDisconnect: () => {
        console.log('STOMP disconnected')
        this.isConnecting = false
      },
    })

    this.client.activate()
  }

  attemptReconnect(onConnect, onError, onDisconnect) {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Max reconnection attempts reached')
      return
    }

    this.reconnectAttempts++
    console.log(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`)

    setTimeout(() => {
      this.connect(onConnect, onError, onDisconnect)
    }, this.reconnectDelay * this.reconnectAttempts)
  }

  subscribe(topic, callback) {
    if (!this.client || !this.client.connected) {
      console.warn('WebSocket not connected. Cannot subscribe to:', topic)
      return null
    }

    const subscription = this.client.subscribe(topic, (message) => {
      try {
        const data = JSON.parse(message.body)
        callback(data)
      } catch (error) {
        console.error('Error parsing WebSocket message:', error)
      }
    })

    this.subscriptions.set(topic, subscription)
    console.log('Subscribed to:', topic)
    return subscription
  }

  unsubscribe(topic) {
    const subscription = this.subscriptions.get(topic)
    if (subscription) {
      subscription.unsubscribe()
      this.subscriptions.delete(topic)
      console.log('Unsubscribed from:', topic)
    }
  }

  disconnect() {
    if (this.client) {
      // Unsubscribe from all topics
      this.subscriptions.forEach((subscription, topic) => {
        subscription.unsubscribe()
      })
      this.subscriptions.clear()

      // Disconnect client
      if (this.client.connected) {
        this.client.deactivate()
      }
      this.client = null
      console.log('WebSocket disconnected')
    }
  }

  isConnected() {
    return this.client && this.client.connected
  }
}

// Export singleton instance
export const webSocketService = new WebSocketService()
export default webSocketService

