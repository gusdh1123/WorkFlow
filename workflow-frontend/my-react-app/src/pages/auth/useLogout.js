import { useNavigate } from "react-router-dom";
import { useAuth } from "./useAuth";
import { api, setApiAccessToken } from "../api/api";

export function useLogout() {
  const navigate = useNavigate();
  const { setAccessToken } = useAuth();

  return async function logout() {
    try {
      await api.post("/api/logout");
    }finally {
      // accessToken 메모리 제거
      setApiAccessToken(null); // axios Authorization 제거
      setAccessToken(null);    // React 상태 제거
      navigate("/login", { replace: true });
    }
  };
}
