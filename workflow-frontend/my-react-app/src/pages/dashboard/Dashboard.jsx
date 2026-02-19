import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../auth/hooks/useAuth";
import { api } from "../../api/api";
import "../../css/dashboard/Dashboard.css";

export default function Dashboard() {
  const nav = useNavigate(); // 페이지 이동 훅
  const { accessToken } = useAuth(); // 로그인 토큰

  // KPI 상태 목록
  const kpis = ["TODO", "IN_PROGRESS", "REVIEW", "DONE", "ON_HOLD", "CANCELED"];

  // KPI 상태별 카운트
  const [counts, setCounts] = useState({});

  // 스코프 선택: assigned = 내 업무, created = 내가 만든 업무
  const [scope, setScope] = useState("assigned");

  // KPI 데이터 가져오기 (accessToken 있을 때만)
  useEffect(() => {
    if (!accessToken) return;

    api
      .get("/api/kpi") // 백엔드 KPI API 호출
      .then((res) => setCounts(res.data)) // 응답 저장
      .catch((e) => console.error("KPI 불러오기 실패", e));
  }, [accessToken]);

  // KPI 카드 클릭 시 Tasks 페이지로 이동 + 필터 전달
  const handleKpiClick = (status) => {
    nav(`/tasks?status=${encodeURIComponent(status)}&scope=${encodeURIComponent(scope)}`);
  };

  return (
    <div className="dashboard__grid">
      
      {/* 스코프 선택 버튼 */}
      <div className="kpi__tabs">
        <button
          className={scope === "assigned" ? "active" : ""}
          onClick={() => setScope("assigned")}
          type="button"
        >
          내 업무
        </button>

        <button
          className={scope === "created" ? "active" : ""}
          onClick={() => setScope("created")}
          type="button"
        >
          내가 만든 업무
        </button>
      </div>

      {/* KPI 카드 행 */}
      <section className="kpi__row">
        {kpis.map((k) => (
          <div
            key={k}
            className={`card ${k}`} // 카드 스타일 + 상태별 클래스
            onClick={() => handleKpiClick(k)}
            role="button"
            tabIndex={0} // 키보드 접근 가능
            onKeyDown={(e) => {
              if (e.key === "Enter" || e.key === " ") handleKpiClick(k);
            }}
            style={{ cursor: "pointer" }}
          >
            <div className="card__title">{k}</div>
            <div className="kpi__value">{counts?.[scope]?.[k] ?? 0}</div> {/* KPI 수치 표시 */}
          </div>
        ))}
      </section>

      {/* 아래 영역: 2열 레이아웃 */}
      <section className="two__col">

        {/* 내 업무 테이블 카드 */}
        <div className="card card__tasks">
          <div className="card__title">My Tasks Table</div>
          <div className="muted">
            <p>My Tasks Table</p>
            <p>My Tasks Table</p>
            <p>My Tasks Table</p>
            <p>My Tasks Table</p>
          </div>
        </div>

        {/* 활동 로그 카드 */}
        <div className="card card__activity">
          <div className="card__title">Activity Log</div>
          <div className="muted">
            <p>Activity Log</p>
            <p>Activity Log</p>
            <p>Activity Log</p>
          </div>
        </div>

      </section>
    </div>
  );
}
