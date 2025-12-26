import TaskColumn from './TaskColumn'
import './TaskBoard.css'

const TaskBoard = ({ tasks, onTaskUpdate, viewMode, onEditStart, onEditEnd }) => {
  const columns = [
    {
      status: 'TO_DO',
      label: 'To Do',
      color: '#4caf50',
      tasks: tasks.TO_DO || [],
    },
    {
      status: 'IN_PROGRESS',
      label: 'In Progress',
      color: '#ff9800',
      tasks: tasks.IN_PROGRESS || [],
    },
    {
      status: 'DONE',
      label: 'Completed',
      color: '#4caf50',
      tasks: tasks.DONE || [],
    },
  ]

  return (
    <div className={`task-board ${viewMode}`}>
      {columns.map((column) => (
        <TaskColumn
          key={column.status}
          status={column.status}
          label={column.label}
          color={column.color}
          tasks={column.tasks}
          onTaskUpdate={onTaskUpdate}
          onEditStart={onEditStart}
          onEditEnd={onEditEnd}
        />
      ))}
    </div>
  )
}

export default TaskBoard

