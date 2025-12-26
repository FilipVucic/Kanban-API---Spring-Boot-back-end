import { useAuth } from '../context/AuthContext'
import './Sidebar.css'

const Sidebar = ({ isMobileMenuOpen, onClose }) => {
  const { logout } = useAuth()

  const menuItems = [
    { icon: '📊', label: 'Dashboard' },
    { icon: '📈', label: 'Analytics' },
    { icon: '📅', label: 'Calendar' },
    { icon: '⚙️', label: 'Settings' },
  ]

  return (
    <>
      <div className={`sidebar-overlay ${isMobileMenuOpen ? 'open' : ''}`} onClick={onClose} />
      <aside className={`sidebar ${isMobileMenuOpen ? 'open' : ''}`}>
        <div className="sidebar-content">
          <div className="sidebar-logo">
            <div className="logo-square">S</div>
          </div>
          <nav className="sidebar-nav">
            {menuItems.map((item, index) => (
              <button key={index} className="nav-item" title={item.label}>
                <span className="nav-icon">{item.icon}</span>
              </button>
            ))}
          </nav>
          <div className="sidebar-footer">
            <div className="user-avatar" onClick={logout} title="Logout">
              <span>👤</span>
            </div>
          </div>
        </div>
      </aside>
    </>
  )
}

export default Sidebar

