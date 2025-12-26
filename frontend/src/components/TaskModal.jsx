import { useState, useEffect } from 'react'
import './TaskModal.css'

const TaskModal = ({ task, status, onClose, onSave }) => {
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [taskStatus, setTaskStatus] = useState(status)
  const [priority, setPriority] = useState('MEDIUM')

  useEffect(() => {
    if (task) {
      setTitle(task.title || '')
      setDescription(task.description || '')
      setTaskStatus(task.status || status)
      setPriority(task.priority || 'MEDIUM')
    } else {
      setTitle('')
      setDescription('')
      setTaskStatus(status)
      setPriority('MEDIUM')
    }
  }, [task, status])

  const handleSubmit = (e) => {
    e.preventDefault()
    onSave({
      title,
      description,
      status: taskStatus,
      priority,
    })
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>{task ? 'Edit Task' : 'Create New Task'}</h2>
          <button className="close-btn" onClick={onClose}>
            ×
          </button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="title">Title *</label>
            <input
              id="title"
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
              placeholder="Enter task title"
            />
          </div>
          <div className="form-group">
            <label htmlFor="description">Description</label>
            <textarea
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows="4"
              placeholder="Enter task description"
            />
          </div>
          <div className="form-group">
            <label htmlFor="status">Status *</label>
            <select
              id="status"
              value={taskStatus}
              onChange={(e) => setTaskStatus(e.target.value)}
              required
            >
              <option value="TO_DO">To Do</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="DONE">Done</option>
            </select>
          </div>
          <div className="form-group">
            <label htmlFor="priority">Priority *</label>
            <select
              id="priority"
              value={priority}
              onChange={(e) => setPriority(e.target.value)}
              required
            >
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
            </select>
          </div>
          <div className="modal-actions">
            <button type="button" className="cancel-btn" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="save-btn">
              {task ? 'Update' : 'Create'} Task
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default TaskModal

