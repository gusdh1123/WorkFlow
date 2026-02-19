// 메인 레이아웃 컴포넌트
// - 앱 전체 구조: 사이드바 + 헤더 + 메인 콘텐츠 영역
// - React Router Outlet 사용, 페이지별 렌더링 영역
// - 사용자 정보, 로그아웃, 경로별 타이틀 처리

import { Outlet, NavLink, useLocation } from "react-router-dom";
import { useEffect } from "react";
import "../css/layout/MainLayout.css";
import MainLogo from "../assets/images/Logo.png";
import { useLogout } from "../auth/hooks/useLogout";
import { useAuth } from "../auth/hooks/useAuth";

export default function MainLayout() {

  const logout = useLogout();  // 로그아웃 훅
  const { user } = useAuth();  // 전역 인증 상태

  const { pathname } = useLocation(); // 현재 경로 확인
  console.log("현재 경로:", pathname);

  // 경로별 헤더 타이틀 매핑
  const titleMap = {
    "/": "Dashboard",
    "/tasks/**": "Tasks"
  };
  const title = titleMap[pathname] ?? "WorkFlow"; // 매칭 없으면 기본값

  // 경로 변경 시 문서 타이틀 갱신
  useEffect(() => {
    document.title = title;
  }, [title]);

  return (
    <div className="app__shell">

      <div className="app__container">

        {/* 사이드바 */}
        <aside className="sidebar">
          <div className="sidebar__title">
            <img className="main__logo" src={MainLogo} alt="MainLogo"/>
          </div>

          <nav className="sidebar__nav">
            {/* NavLink: 현재 경로에 active 스타일 적용 */}
            <NavLink to="/" className="nav__item">Dashboard</NavLink>
            <NavLink to="/tasks" className="nav__item">My Tasks</NavLink>
            <NavLink to="/projects" className="nav__item">Projects</NavLink>
            <NavLink to="/team" className="nav__item">Team</NavLink>
            <NavLink to="/calendar" className="nav__item">Calendar</NavLink>
            <NavLink to="/reports" className="nav__item">Reports</NavLink>
            <NavLink to="/settings" className="nav__item">Settings</NavLink>
          </nav>
        </aside>

        {/* 메인 컬럼 */}
        <div className="main__col">
          <div className="main__wrap">

            {/* 헤더 */}
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

            {/* 페이지별 컨텐츠 렌더링 */}
            <main className="content">
              <Outlet />
            </main>

          </div>
        </div>

      </div>
    </div>
  );
}

// MainLayout 역할 요약
// - 앱 전체 레이아웃 담당
// - 사이드바: 페이지 이동용 링크
// - 헤더: 페이지 타이틀 + 사용자 정보 + 액션 버튼
// - Outlet: 라우터 페이지 렌더링 영역
// - useLocation + useEffect: 경로별 문서 타이틀 관리
