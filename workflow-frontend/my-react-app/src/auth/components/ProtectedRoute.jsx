import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";

// 보호된 라우트 컴포넌트
// - 로그인된 사용자만 접근 가능
// - accessToken 없으면 로그인 페이지로 리다이렉트
export default function ProtectedRoute() {
  const { accessToken } = useAuth(); // 커스텀 훅에서 현재 accessToken 가져오기

  // accessToken 없으면 로그인 페이지로 이동
  if (!accessToken) {
    return <Navigate to="/login" replace />; // replace: 히스토리 스택에 로그인 페이지 덮어쓰기
  }

  // accessToken이 존재하면 하위 라우트(Outlet) 렌더링
  return <Outlet />;
}

// 사용 예:
// <Route element={<ProtectedRoute />}>
//   <Route path="/dashboard" element={<Dashboard />} />
// </Route>
