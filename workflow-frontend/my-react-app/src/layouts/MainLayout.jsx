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
    <div className="app__shell">
      <div className="app__container">
       <aside className="sidebar">
        <div className="sidebar__title"><img className="main__logo" src={MainLogo} alt="MainLogo"/></div>
        <nav className="sidebar__nav">
          <NavLink to="/" end className={({ isActive }) => `nav__item ${isActive ? 'active' : ''}`}>
            Dashboard
          </NavLink>
          <NavLink to="/tasks" className={({ isActive }) => `nav__item ${isActive ? 'active' : ''}`}>
            My Tasks
          </NavLink>
          <NavLink to="/projects" className={({ isActive }) => `nav__item ${isActive ? 'active' : ''}`}>
            Projects
          </NavLink>
          <NavLink to="/team" className={({ isActive }) => `nav__item ${isActive ? 'active' : ''}`}>
            Team
          </NavLink>
          <NavLink to="/calendar" className={({ isActive }) => `nav__item ${isActive ? 'active' : ''}`}>
            Calendar
          </NavLink>
          <NavLink to="/reports" className={({ isActive }) => `nav__item ${isActive ? 'active' : ''}`}>
            Reports
          </NavLink>
          <NavLink to="/settings" className={({ isActive }) => `nav__item ${isActive ? 'active' : ''}`}>
            Settings
          </NavLink>
        </nav>
       </aside>

       <div className="main__col">
        <div className="main__wrap">
        <header className="header">
          <div className="header__title">{title}</div>
          <div className="header__actions">
            <button className="ghost__btn">Search</button>
            {/* <button className="ghostBtn">Profile</button> */}
            <button className="ghost__btn">Notifications</button>
            <button className="ghost__btn header__userBtn">{user?.name}님</button>
            <button onClick={logout} className="ghost__btn">logout</button>
          </div>
        </header>

        <main className="content">
          <Outlet />
        </main>
        </div>
       </div>
      </div>
    </div>
  )

}
