import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import "./index.css";

// =========================
// React 앱 진입점
// - ReactDOM.createRoot: React 18+ 루트 생성
// - BrowserRouter: HTML5 history API 기반 라우팅
// - App: 앱 전체 컴포넌트 (라우팅 포함)
// =========================
ReactDOM.createRoot(document.getElementById("root")).render(
  // StrictMode: 개발 모드에서 추가 검사 (주석 처리됨)
  // <React.StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  // </React.StrictMode>
);
