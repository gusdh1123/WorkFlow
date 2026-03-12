import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../auth/hooks/useAuth";
import { api } from "../../api/api";
import "../../css/dashboard/Dashboard.css";

export default function Dashboard() {
  const nav = useNavigate(); // 페이지 이동 훅
  const { accessToken } = useAuth(); // 로그인 토큰 + 사용자 정보

  // KPI 상태 목록
  const kpis = ["TODO", "IN_PROGRESS", "REVIEW", "DONE", "ON_HOLD", "CANCELED"];

  // KPI 상태별 카운트
  const [counts, setCounts] = useState({});

  // 스코프 선택: assigned = 내 업무, created = 내가 만든 업무
  const [scope, setScope] = useState("assigned");

  // My Tasks Table
  const [myTasks, setMyTasks] = useState([]);

  // // Activity Log
  // const [activityLog, setActivityLog] = useState([]);

  // KPI 데이터 가져오기 (accessToken 있을 때만)
  useEffect(() => {
    if (!accessToken) return;

    // KPI 카드
    api
      .get("/api/kpi") // 예: { assigned: { TODO: 3, DONE: 5 }, created: { ... } }
      .then((res) => setCounts(res.data))
      .catch((e) => console.error("KPI 불러오기 실패", e));

    // My Tasks Table (내 업무/내가 만든 업무)
    api
      .get(`/api/tasks?scope=${scope}&page=0&size=5`) // 최근 5개 Task
      .then((res) => setMyTasks(res.data.content))
      .catch((e) => console.error("내 업무 불러오기 실패", e));

    // Activity Log (최근 Task 변경 내역)
    // api
    //   .get("/api/tasks/activity?limit=5") // 최근 5개 이벤트
    //   .then((res) => setActivityLog(res.data))
    //   .catch((e) => console.error("Activity Log 불러오기 실패", e));
  }, [accessToken, scope]);

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

      {/* 아래 영역: 2열 레이아웃 */}
      <section className="two__col">

        {/* 내 업무 테이블 카드 */}
        <div className="card card__tasks">
          <div className="card__title">My Tasks Table</div>
          <div className="muted">
            {myTasks.length === 0 ? (
              <p>업무가 없습니다.</p>
            ) : (
              <table className="tasks__table">
                <thead>
                  <tr>
                    <th>제목</th>
                    <th>상태</th>
                    <th>마감일</th>
                    <th>담당자</th>
                  </tr>
                </thead>
                <tbody>
                  {myTasks.map((t) => (
                    <tr key={t.id} onClick={() => nav(`/tasks/${t.id}`)} style={{ cursor: "pointer" }}>
                      <td>{t.title}</td>
                      <td>{t.status}</td>
                      <td>{t.dueDate || "-"}</td>
                      <td>{t.assignee?.name || "-"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

        {/* 활동 로그 카드 */}
        <div className="card card__activity">
          <div className="card__title">Activity Log</div>
          <div className="muted">
            <p>활동 내역이 없습니다.</p>
            {/* {activityLog.length === 0 ? (
              <p>활동 내역이 없습니다.</p>
            ) : (
              <ul>
                {activityLog.map((a, idx) => (
                  <li key={idx}>
                    {a.userName}님이 "{a.taskTitle}" 업무를 {a.action} 했습니다.
                  </li>
                ))}
              </ul>
            )} */}
          </div>
        </div>

      </section>
    </div>
  );
}