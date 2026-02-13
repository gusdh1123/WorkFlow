import '../css/Login.css'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from './useAuth';
import { api } from "../api/api";
import LoginLogo from "../../assets/images/Logo.png";

function Login()  {
  const navigate = useNavigate();
  const { setAccessToken } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const loginHandle = async () => {
    try {
      const res = await api.post("/api/login", { email, password });
      setAccessToken(res.data.accessToken);
      navigate("/");
    } catch (err) {
      console.error(err);
      alert("아이디 또는 비밀번호가 틀렸습니다.");
    }
  };

  const submitHandle = (e) => {
    e.preventDefault(); // 새로고침 방지
    loginHandle();
  };

  return (
    <div className='login__page'>
      <div className='login__box'>
        <img className="login__logo" src={LoginLogo} alt="LoginLogo"/>

        <form onSubmit={submitHandle} className='login__form'>
          <input
            className='email'
            type='email'
            placeholder='이메일'
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />

          <input
            className='password'
            type='password'
            placeholder='비밀번호'
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          <button type="submit" className='login__btn'>
            로그인
          </button>
        </form>
      </div>
    </div>
  ); 
}

export default Login;
