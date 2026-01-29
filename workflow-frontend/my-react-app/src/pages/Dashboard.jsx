import { useEffect, useState } from 'react'

export default function Dashboard() {
  const kpis = ['TODO', 'IN_PROGRESS', 'REVIEW', 'DONE', 'ON_HOLD', 'CANCELED']
  const [counts, setCounts] = useState({})

  useEffect(() => {
    fetch('/api/kpi')
      .then((res) => res.json())
      .then((data) => {
        // data 예시: { TODO: 3, IN_PROGRESS: 2, ... }
        setCounts(data)
      })
      .catch((err) => {
        console.error('KPI 불러오기 실패', err)
      })
  }, [])

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
          <div className="muted">(필터: 상태/담당자/기간/키워드)</div>
        </div>

        <div className="card bigCard">
          <div className="cardTitle">Activity Log</div>
          <div className="muted">(상태/담당자/마감일 변경 이력)</div>
        </div>
      </section>
    </div>
  )
}
