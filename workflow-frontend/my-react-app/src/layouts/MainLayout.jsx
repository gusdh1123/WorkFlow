import { Outlet, NavLink, useLocation} from "react-router-dom";
import { useEffect /*, useState */ } from "react";
import "./MainLayout.css";
import MainLogo from "../assets/images/Logo.png";
import { useLogout } from "../pages/auth/useLogout";
import { useAuth } from "../pages/auth/useAuth";

export default function MainLayout() {

   const logout = useLogout();
   const { user } = useAuth();

   const { pathname } = useLocation();
   console.log("현재 경로:", pathname);

   const titleMap = {
    "/": "Dashboard",
    "/tasks": "Tasks"
   }

   const title = titleMap[pathname] ?? "WorkFlow";

   useEffect(() => {
   document.title = title;
   }, [title]);

  return (
    <div className="appShell">
      <aside className="sidebar">
        <div className="sidebarTitle"><img className="MainLogo" src={MainLogo} alt="MainLogo"/></div>
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
          <div className="headerTitle">{title}</div>
          <div className="headerActions">
            <button className="ghostBtn">Search</button>
            {/* <button className="ghostBtn">Profile</button> */}
            <button className="ghostBtn">Notifications</button>
            <button className="ghostBtn">{user?.name}님</button>
            <button onClick={logout} className="ghostBtn">logout</button>
          </div>
        </header>

        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  )

}
