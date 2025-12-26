import { useState } from 'react'
import TaskCard from './TaskCard'
import TaskModal from './TaskModal'
import { taskAPI } from '../services/api'
import './TaskColumn.css'

const TaskColumn = ({ status, label, color, tasks, onTaskUpdate, onEditStart, onEditEnd }) => {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingTask, setEditingTask] = useState(null)

  const handleAddTask = () => {
    setEditingTask(null)
    setIsModalOpen(true)
    if (onEditStart) onEditStart() // Pause WebSocket updates
  }

  const handleEditTask = (task) => {
    setEditingTask(task)
    setIsModalOpen(true)
    if (onEditStart) onEditStart() // Pause WebSocket updates
  }

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

  const handleSaveTask = async (taskData) => {
    try {
      if (editingTask) {
        await taskAPI.update(editingTask.id, {
          ...taskData,
          version: editingTask.version,
        })
      } else {
        await taskAPI.create(taskData)
      }
      setIsModalOpen(false)
      setEditingTask(null)
      if (onEditEnd) onEditEnd() // Resume WebSocket updates and process queued events
      onTaskUpdate()
    } catch (error) {
      console.error('Failed to save task:', error)
      const errorMessage = getErrorMessage(error)
      alert(errorMessage)
    }
  }

  const handleDeleteTask = async (taskId) => {
    if (window.confirm('Are you sure you want to delete this task?')) {
      try {
        await taskAPI.delete(taskId)
        onTaskUpdate()
      } catch (error) {
        console.error('Failed to delete task:', error)
        const errorMessage = getErrorMessage(error)
        alert(errorMessage)
      }
    }
  }

  const getProgress = (task) => {
    if (task.status === 'DONE') return 'Done'
    // Mock progress calculation - you can enhance this based on your needs
    return '0%'
  }

  return (
    <>
      <div className="task-column">
        <div className="column-header">
          <div className="column-title">
            <span className="status-dot" style={{ backgroundColor: color }}></span>
            <span className="column-label">{label}</span>
            <span className="task-count">({tasks.length})</span>
          </div>
          <button className="add-task-btn" onClick={handleAddTask}>
            +
          </button>
        </div>
        <div className="tasks-list">
          {tasks.map((task) => (
            <TaskCard
              key={task.id}
              task={task}
              onEdit={handleEditTask}
              onDelete={handleDeleteTask}
              progress={getProgress(task)}
            />
          ))}
          {tasks.length === 0 && (
            <div className="empty-column">No tasks in this column</div>
          )}
        </div>
      </div>
      {isModalOpen && (
        <TaskModal
          task={editingTask}
          status={status}
          onClose={() => {
            setIsModalOpen(false)
            setEditingTask(null)
            if (onEditEnd) onEditEnd() // Clear editing state when modal closes
          }}
          onSave={handleSaveTask}
        />
      )}
    </>
  )
}

export default TaskColumn

