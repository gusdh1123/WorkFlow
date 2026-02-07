import { useEffect, useState } from "react";
import { useAuth } from "../auth/useAuth";
import { apiFetch } from "../api/apiFetch"; 

export default function Dashboard() {

  const kpis = ['TODO', 'IN_PROGRESS', 'REVIEW', 'DONE', 'ON_HOLD', 'CANCELED']
  const [counts, setCounts] = useState({})
  const { accessToken, setAccessToken } = useAuth();

useEffect(() => {
    apiFetch("/api/kpi", accessToken, setAccessToken)
      .then((res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
      })
      .then(setCounts)
      .catch((e) => console.error("KPI 불러오기 실패", e));
  }, [accessToken, setAccessToken]);

  return (
    <div className="dashboardGrid">
      {/* KPI Cards */}
      <section className="kpiRow">
        {kpis.map((k) => (
          <div key={k} className="card">
            <div className="cardTitle">{k}</div>
            <div className="muted">{counts[k] ?? 0}</div>
          </div>
        ))}
      </section>

      {/* 아래 2컬럼 */}
      <section className="twoCol">
        <div className="card bigCard">
          <div className="cardTitle">My Tasks Table</div>
          <div className="muted">(필터: 상태/담당자/기간/키워드)
          </div>
        </div>

        <div className="card bigCard2">
          <div className="cardTitle">Activity Log</div>
          <div className="muted">(상태/담당자/마감일 변경 이력)
            <p>asdasdasdas</p>
            <p>asdasdasdas</p>
            <p>asdasdasdas</p>
            <p>asdasdasdas</p>
            <p>asdasdasdas</p>
            <p>asdasdasdas</p>
            <p>asdasdasdas</p>
            <p>asdasdasdas</p>
            <p>asdasdasdas</p>
            <p>asdasdasdas</p>
            <p>asdasdasdas</p>
            <p>asdasdasdas</p>
            <p>asdasdasdas</p>
            <p>asdasdasdas</p>
            <p>asdasdasdas</p>
            <p>asdasdasdas</p>
            <p>asdasdasdas</p>
            <p>asdasdasdas</p>
            <p>asdasdasdas</p>
            <p>asdasdasdas</p>
            <p>asdasdasdas</p>
            <p>asdasdasdas</p>
            <p>asdasdasdas</p>
            <p>asdasdasdas</p>
          </div>
        </div>
      </section>
    </div>
  )
}
