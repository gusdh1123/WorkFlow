import { Routes, Route } from "react-router-dom";
import MainLayout from "./layouts/MainLayout";
import Dashboard from "./pages/dashboard/Dashboard";
import Login from "./pages/auth/Login";
import AuthProvider from "./pages/auth/AuthProvider";
import ProtectedRoute from "./pages/auth/ProtectedRoute";
import Tasks from "./pages/tasks/Tasks";
import TaskCreate from "./pages/tasks/TaskCreate";

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        {/* 보호 라우트 */}
        <Route element={<ProtectedRoute />}>
          <Route element={<MainLayout />}>
            <Route path="/" element={<Dashboard />} />
            <Route path="/tasks" element={<Tasks />} />
            <Route path="/tasks/taskcreate" element={<TaskCreate />} />
          </Route>
        </Route>

        {/* 공개 라우트 */}
        <Route path="/login" element={<Login />} />
      </Routes>
    </AuthProvider>
  );
}
