// src/layouts/MainLayout.jsx
import { Outlet, NavLink } from 'react-router-dom'
import './MainLayout.css'

export default function MainLayout() {
  return (
    <div className="appShell">
      <aside className="sidebar">
        <div className="sidebarTitle">Sidebar</div>
        <nav className="sidebarNav">
          <NavLink to="/" end className={({ isActive }) => `navItem ${isActive ? 'active' : ''}`}>
            Dashboard
          </NavLink>
          <NavLink to="/tasks" className={({ isActive }) => `navItem ${isActive ? 'active' : ''}`}>
            My Tasks
          </NavLink>
          <NavLink to="/projects" className={({ isActive }) => `navItem ${isActive ? 'active' : ''}`}>
            Projects
          </NavLink>
          <NavLink to="/team" className={({ isActive }) => `navItem ${isActive ? 'active' : ''}`}>
            Team
          </NavLink>
          <NavLink to="/calendar" className={({ isActive }) => `navItem ${isActive ? 'active' : ''}`}>
            Calendar
          </NavLink>
          <NavLink to="/reports" className={({ isActive }) => `navItem ${isActive ? 'active' : ''}`}>
            Reports
          </NavLink>
          <NavLink to="/settings" className={({ isActive }) => `navItem ${isActive ? 'active' : ''}`}>
            Settings
          </NavLink>
        </nav>
      </aside>

      <div className="mainCol">
        <header className="header">
          <div className="headerTitle">Header</div>
          <div className="headerActions">
            <button className="ghostBtn">Search</button>
            <button className="ghostBtn">Notifications</button>
            <button className="ghostBtn">Profile</button>
          </div>
        </header>

        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
