import { useAuth } from '../context/AuthContext'
import './Header.css'

const Header = ({ viewMode, onViewModeChange, onMobileMenuToggle }) => {

  return (
    <header className="header">
      <div className="header-top">
        <div className="header-left">
          <button className="mobile-menu-btn" onClick={onMobileMenuToggle}>
            ☰
          </button>
          <div className="breadcrumbs">
            <span>S</span>
            <span>›</span>
            <span>Dashboard</span>
            <span>›</span>
            <span>Project</span>
            <span>›</span>
            <span className="breadcrumb-active">Project PlanetX</span>
          </div>
        </div>
        <div className="header-right">
          <button className="icon-btn">
            <span>🔍</span>
          </button>
          <div className="team-avatars">
            <div className="avatar">👤</div>
            <div className="avatar">👤</div>
            <div className="avatar">👤</div>
            <div className="avatar-more">+3</div>
          </div>
          <button className="invite-btn">Invite +</button>
          <div className="view-controls">
            <button
              className={`view-btn ${viewMode === 'grid' ? 'active' : ''}`}
              onClick={() => onViewModeChange('grid')}
            >
              Grid View
            </button>
            <button
              className={`view-btn ${viewMode === 'list' ? 'active' : ''}`}
              onClick={() => onViewModeChange('list')}
            >
              List View
            </button>
            <button className="icon-btn">🔽</button>
            <button className="icon-btn">🔽</button>
          </div>
          <button className="export-btn">
            <span>📥</span> Export Data
          </button>
        </div>
      </div>
      <div className="header-bottom">
        <div className="project-header">
          <div className="project-icon">🌍</div>
          <h1 className="project-title">Project PlanetX</h1>
        </div>
        <div className="header-tabs">
          <button className="tab active">By Status</button>
          <button className="tab">By Total Tasks (12)</button>
          <button className="tab">Tasks Due</button>
        </div>
      </div>
    </header>
  )
}

export default Header

