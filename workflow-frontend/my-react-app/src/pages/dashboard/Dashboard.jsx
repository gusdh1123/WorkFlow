import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/useAuth";
import { api } from "../api/api.js";

export default function Dashboard() {
  const nav = useNavigate();
  const { accessToken } = useAuth();

  const kpis = ["TODO", "IN_PROGRESS", "REVIEW", "DONE", "ON_HOLD", "CANCELED"];

  const [counts, setCounts] = useState({});
  const [scope, setScope] = useState("assigned"); // assigned | created

  useEffect(() => {
    if (!accessToken) return;

    api
      .get("/api/kpi")
      .then((res) => setCounts(res.data))
      .catch((e) => console.error("KPI 불러오기 실패", e));
  }, [accessToken]);

  // KPI 클릭 -> Tasks로 이동 + 필터 전달
  const handleKpiClick = (status) => {
    nav(`/tasks?status=${encodeURIComponent(status)}&scope=${encodeURIComponent(scope)}`);
  };

  return (
    <div className="dashboard__grid">
      {/* 스코프 선택 */}
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

      {/* KPI Cards */}
      <section className="kpi__row">
        {kpis.map((k) => (
          <div
            key={k}
            className={`card ${k}`}
            onClick={() => handleKpiClick(k)}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => {
              if (e.key === "Enter" || e.key === " ") handleKpiClick(k);
            }}
            style={{ cursor: "pointer" }}
          >
            <div className="card__title">{k}</div>
            <div className="kpi__value">{counts?.[scope]?.[k] ?? 0}</div>
          </div>
        ))}
      </section>

      {/* 아래 영역 */}
      <section className="two__col">
        <div className="card card__tasks">
          <div className="card__title">My Tasks Table</div>
          <div className="muted">
            <p>My Tasks Table</p>
            <p>My Tasks Table</p>
            <p>My Tasks Table</p>
            <p>My Tasks Table</p>
          </div>
        </div>

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
