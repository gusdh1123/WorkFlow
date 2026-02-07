import { useNavigate } from "react-router-dom";
import { useAuth } from "./useAuth";

export function useLogout() {
  const navigate = useNavigate();
  const { setAccessToken } = useAuth();

  return async function logout() {
    try {
      await fetch("http://localhost:8081/api/logout", {
        method: "POST",
        credentials: "include",
      });
    } finally {
      // accessToken 메모리 제거
      setAccessToken(null);
      navigate("/login", { replace: true });
    }
  };
}