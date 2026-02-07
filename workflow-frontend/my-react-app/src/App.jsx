import { Routes, Route } from 'react-router-dom'
import MainLayout from './layouts/MainLayout'
import Dashboard from './pages/dashboard/Dashboard';
import Login from './pages/auth/Login'
import { AuthProvider } from "./pages/auth/AuthProvider";

export default function App() {

  return (
    <AuthProvider>
    <Routes>
      <Route element={<MainLayout />}>
        <Route path="/" element={<Dashboard />} />
      </Route>
      <Route path="/login" element={<Login />}></Route>
    </Routes>
    </AuthProvider>
  )
}