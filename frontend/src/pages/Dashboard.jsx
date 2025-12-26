import { useState, useEffect, useCallback } from 'react'
import Sidebar from '../components/Sidebar'
import Header from '../components/Header'
import TaskBoard from '../components/TaskBoard'
import { taskAPI } from '../services/api'
import { useWebSocket } from '../hooks/useWebSocket'
import './Dashboard.css'

const Dashboard = () => {
  const [tasks, setTasks] = useState([])
  const [loading, setLoading] = useState(true)
  const [viewMode, setViewMode] = useState('grid')
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false)
  const [isEditingTask, setIsEditingTask] = useState(false)
  const [queuedEvents, setQueuedEvents] = useState([])

  useEffect(() => {
    loadTasks()
  }, [])

  const loadTasks = async () => {
    try {
      setLoading(true)
      const data = await taskAPI.getAll()
      setTasks(data)
    } catch (error) {
      console.error('Failed to load tasks:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleTaskUpdate = () => {
    loadTasks()
  }

  // Process a single WebSocket event
  const processTaskEvent = useCallback((event, currentTasks) => {
    const taskList = Array.isArray(currentTasks) ? currentTasks : []
    
    switch (event.type) {
      case 'CREATED':
        // Add new task if it doesn't already exist
        if (event.task && !taskList.find((t) => t.id === event.task.id)) {
          return [...taskList, event.task]
        }
        return taskList

      case 'UPDATED':
        // Update existing task
        if (event.task) {
          return taskList.map((t) =>
            t.id === event.task.id ? event.task : t
          )
        }
        return taskList

      case 'DELETED':
        // Remove deleted task
        if (event.taskId) {
          return taskList.filter((t) => t.id !== event.taskId)
        }
        return taskList

      default:
        return taskList
    }
  }, [])

  // Handle WebSocket task events
  const handleTaskEvent = useCallback((event) => {
    console.log('Received task event:', event)
    
    // If editing, queue the event instead of processing it
    if (isEditingTask) {
      console.log('Queuing WebSocket event while editing:', event)
      setQueuedEvents((prev) => [...prev, event])
      return
    }
    
    // Process event immediately
    setTasks((currentTasks) => processTaskEvent(event, currentTasks))
  }, [isEditingTask, processTaskEvent])

  // Process queued events when editing ends
  useEffect(() => {
    if (!isEditingTask && queuedEvents.length > 0) {
      console.log(`Processing ${queuedEvents.length} queued WebSocket events`)
      setTasks((currentTasks) => {
        let updatedTasks = currentTasks
        // Process all queued events in order
        queuedEvents.forEach((event) => {
          updatedTasks = processTaskEvent(event, updatedTasks)
        })
        return updatedTasks
      })
      setQueuedEvents([]) // Clear the queue
    }
  }, [isEditingTask, queuedEvents, processTaskEvent])

  // Connect to WebSocket for real-time updates
  const { isConnected } = useWebSocket(handleTaskEvent)

  const tasksByStatus = {
    TO_DO: tasks.filter((t) => t.status === 'TO_DO'),
    IN_PROGRESS: tasks.filter((t) => t.status === 'IN_PROGRESS'),
    DONE: tasks.filter((t) => t.status === 'DONE'),
  }

  return (
    <div className="dashboard">
      <Sidebar isMobileMenuOpen={isMobileMenuOpen} onClose={() => setIsMobileMenuOpen(false)} />
      <div className="dashboard-main">
        <Header
          viewMode={viewMode}
          onViewModeChange={setViewMode}
          onMobileMenuToggle={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
        />
        {loading ? (
          <div className="loading">
            Loading tasks...
            {isConnected && <span className="ws-indicator">🟢 Live</span>}
          </div>
        ) : (
          <>
            {isConnected && (
              <div className="ws-status">
                <span className="ws-indicator">🟢</span>
                <span>Real-time updates active</span>
              </div>
            )}
            <TaskBoard
              tasks={tasksByStatus}
              onTaskUpdate={handleTaskUpdate}
              viewMode={viewMode}
              onEditStart={() => setIsEditingTask(true)}
              onEditEnd={() => setIsEditingTask(false)}
            />
          </>
        )}
      </div>
    </div>
  )
}

export default Dashboard

