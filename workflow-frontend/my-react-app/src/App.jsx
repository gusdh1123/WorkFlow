import { Routes, Route } from "react-router-dom";
import MainLayout from "./layouts/MainLayout";
import Dashboard from "./pages/dashboard/Dashboard";
import Login from "./pages/login/Login";
import AuthProvider from "./auth/context/AuthProvider";
import ProtectedRoute from "./auth/components/ProtectedRoute";
import Tasks from "./pages/tasks/TaskMain";
import TaskCreate from "./pages/tasks/TaskCreate";
import TaskDetail from "./pages/tasks/TaskDetail";

export default function App() {
  return (
    // 인증 상태를 전역으로 제공하는 Context Provider
    <AuthProvider>
      <Routes>
        {/* =========================
            보호 라우트 (로그인 필요)
            ProtectedRoute 컴포넌트 내부에서 인증 확인 후
            인증 안 되면 로그인 페이지로 리다이렉트
        ========================= */}
        <Route element={<ProtectedRoute />}>
          {/* =========================
              공통 레이아웃 적용
              MainLayout 안에 Sidebar, Header 등 공통 UI 포함
              내부 Outlet에서 실제 페이지 렌더링
          ========================= */}
          <Route element={<MainLayout />}>
            {/* 대시보드 페이지 */}
            <Route path="/" element={<Dashboard />} />

            {/* 업무 목록 페이지 */}
            <Route path="/tasks" element={<Tasks />} />

            {/* 업무 생성 페이지 */}
            <Route path="/tasks/create" element={<TaskCreate />} />

            {/* 업무 상세 페이지 (URL 파라미터 :id 사용) */}
            <Route path="/tasks/:id" element={<TaskDetail />} />
          </Route>
        </Route>

        {/* =========================
            공개 라우트 (인증 불필요)
            누구나 접근 가능, 예: 로그인 페이지
        ========================= */}
        <Route path="/login" element={<Login />} />
      </Routes>
    </AuthProvider>
  );
}
