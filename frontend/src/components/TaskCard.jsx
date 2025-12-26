import './TaskCard.css'

const TaskCard = ({ task, onEdit, onDelete, progress }) => {
  const getPriorityColor = (priority) => {
    switch (priority) {
      case 'HIGH':
        return '#764ba2'
      case 'MEDIUM':
        return '#4caf50'
      case 'LOW':
        return '#ff5722'
      default:
        return '#999'
    }
  }

  const getPriorityLabel = (priority) => {
    switch (priority) {
      case 'HIGH':
        return 'High'
      case 'MEDIUM':
        return 'Medium'
      case 'LOW':
        return 'Low'
      default:
        return priority
    }
  }

  const priorityColor = getPriorityColor(task.priority)
  const priorityLabel = getPriorityLabel(task.priority)

  return (
    <div className="task-card" onClick={() => onEdit(task)}>
      <div className="task-tag" style={{ backgroundColor: priorityColor }}>
        {priorityLabel}
      </div>
      <h3 className="task-title">{task.title}</h3>
      {task.description && (
        <p className="task-description">{task.description}</p>
      )}
      <div className="task-progress">
        {progress === 'Done' ? (
          <span className="progress-done">Done</span>
        ) : (
          <span className="progress-percent">{progress}</span>
        )}
      </div>
      <div className="task-footer">
        <div className="task-assignees">
          <div className="assignee-avatar">👤</div>
          <div className="assignee-avatar">👤</div>
          <div className="assignee-more">+3</div>
        </div>
        <div className="task-stats">
          <span className="stat-item">
            💬 <span className="stat-value">11</span>
          </span>
          <span className="stat-item">
            👁️ <span className="stat-value">187</span>
          </span>
        </div>
      </div>
      <div className="task-actions">
        <button
          className="action-btn edit-btn"
          onClick={(e) => {
            e.stopPropagation()
            onEdit(task)
          }}
        >
          ✏️
        </button>
        <button
          className="action-btn delete-btn"
          onClick={(e) => {
            e.stopPropagation()
            onDelete(task.id)
          }}
        >
          🗑️
        </button>
      </div>
    </div>
  )
}

export default TaskCard

