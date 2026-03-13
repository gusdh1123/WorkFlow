import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../auth/hooks/useAuth";
import { api } from "../../api/api";
import "../../css/dashboard/Dashboard.css";

export default function Dashboard() {
  const nav = useNavigate(); // 페이지 이동 훅
  const { accessToken } = useAuth(); // 로그인 토큰 + 사용자 정보

  const kpis = ["TODO", "IN_PROGRESS", "REVIEW", "DONE", "ON_HOLD", "CANCELED"];
  const [counts, setCounts] = useState({});
  const [scope, setScope] = useState("assigned");
  const [myTasks, setMyTasks] = useState([]);
  const [selectedTaskId, setSelectedTaskId] = useState(null);
  const [activityLog, setActivityLog] = useState([]);
  const [showAllLogs, setShowAllLogs] = useState(false);

  // 각 로그별 변경 필드 전체보기 상태
  const [showFullText, setShowFullText] = useState([]);

  const departmentNameMap = {
    Operations: "운영팀",
    Development: "개발팀",
    Design: "디자인팀",
  };

  // KPI + 내 업무 불러오기
  useEffect(() => {
    if (!accessToken) return;

    api.get("/api/kpi")
      .then((res) => setCounts(res.data))
      .catch((e) => console.error("KPI 불러오기 실패", e));

    api.get(`/api/tasks?scope=${scope}&page=0&size=10`)
      .then((res) => setMyTasks(res.data.content))
      .catch((e) => console.error("내 업무 불러오기 실패", e));
  }, [accessToken, scope]);

  // 첫 번째 업무 자동 선택 후 활동 로그 불러오기
  useEffect(() => {
    if (myTasks.length > 0) {
      const firstTaskId = myTasks[0].id;

      Promise.resolve().then(() => setSelectedTaskId(firstTaskId));

      api.get(`/api/audit/${firstTaskId}/audit-logs`)
        .then((res) => setActivityLog(res.data))
        .catch((err) => console.error("Audit Log 불러오기 실패", err));
    }
  }, [myTasks]);

  // activityLog 변경 시 로그별 전체보기 초기화
  useEffect(() => {
  Promise.resolve().then(() => {
    setShowFullText(activityLog.map(() => false));
  });
}, [activityLog]);

  // KPI 카드 클릭 시 이동
  const handleKpiClick = (status) => {
    nav(`/tasks?status=${encodeURIComponent(status)}&scope=${encodeURIComponent(scope)}`);
  };

  // 업무 클릭 시 선택 및 활동 로그 불러오기
  const handleTaskClick = (taskId) => {
    setSelectedTaskId(taskId);
    api.get(`/api/audit/${taskId}/audit-logs`)
      .then((res) => setActivityLog(res.data))
      .catch((err) => console.error("Audit Log 불러오기 실패", err));
  };

  const displayedLogs = showAllLogs ? activityLog : activityLog.slice(0, 3);

  // HTML 태그 제거
  function stripHtml(html) {
    if (!html) return "";
    const div = document.createElement("div");
    div.innerHTML = html;
    return div.textContent || div.innerText || "";
  }

  // 긴 텍스트 줄이기 (trim)
  function trimText(text, length = 30) {
    if (!text) return "";
    return text.length > length ? text.slice(0, length) + "…" : text;
  }

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
                    <th>우선순위</th>
                    <th>마감일</th>
                    <th>남은 기간</th>
                    <th>담당자</th>
                    <th>담당 부서</th>
                    <th>작성자</th>
                    <th>작성자 부서</th>
                    <th>작성일</th>
                  </tr>
                </thead>
                <tbody>
                  {myTasks.map((t) => {
                    const dueDate = t.dueDate ? new Date(t.dueDate) : null;
                    const createdAt = t.createdAt ? new Date(t.createdAt) : null;
                    const today = new Date();
                    const remaining =
                      dueDate && !t.is_deleted
                        ? Math.ceil((dueDate - today) / (1000 * 60 * 60 * 24))
                        : "-";

                    return (
                      <tr
                        key={t.id}
                        onClick={() => handleTaskClick(t.id)}
                        className={t.id === selectedTaskId ? "selected" : ""}
                        style={{ cursor: "pointer" }}
                      >
                        <td>{t.title}</td>
                        <td>{t.status}</td>
                        <td>{t.priority}</td>
                        <td>{dueDate?.toLocaleDateString() || "-"}</td>
                        <td>{remaining !== "-" ? `${remaining}일 남음` : "-"}</td>
                        <td>{t.assigneeName || "-"}</td>
                        <td>{departmentNameMap[t.workDepartmentName] || "-"}</td>
                        <td>{t.createdByName || "-"}</td>
                        <td>{departmentNameMap[t.ownerDepartmentName] || "-"}</td>
                        <td>{createdAt?.toLocaleDateString() || "-"}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>
        </div>

        {/* 활동 로그 카드 */}
        <div className="card card__activity">
          <div className="card__title">Activity Log</div>
          <div className="muted">
            {activityLog.length === 0 ? (
              <p className="muted" style={{padding: "3px 0" }}>
                활동 내역이 없습니다.
              </p>
            ) : (
              <div className="activity__logs">
                {displayedLogs.map((a, idx) => (
                  <div key={idx} className="activity__logItem">
                    {/* 헤더: 수정자 + 시간 */}
                    <div className="activity__logHeader">
                      <strong>수정자: {a.actorName}</strong>
                      <span className="activity__logTime">
                        {new Date(a.modifiedAt).toLocaleString()}
                      </span>
                    </div>

                    {/* 변경 내용 */}
                    <div className="activity__logChanges">
                      {a.changes.map((c) => {
                        const fieldNameMap = {
                          title: "제목",
                          description: "내용",
                          assignee_id: "담당자",
                          priority: "우선순위",
                          visibility: "공개 범위",
                          due_date: "마감일",
                          status: "진행도",
                          attachment_add: "첨부파일 추가",
                          attachment_delete: "첨부파일 삭제",
                        };

                        const label = fieldNameMap[c.field] || "수정 필드";

                        let beforeVal = c.field === "description" ? stripHtml(c.beforeValue) : c.beforeValue;
                        let afterVal = c.field === "description" ? stripHtml(c.afterValue) : c.afterValue;

                        // 로그별 더보기 적용
                        const isLong =
                          (beforeVal?.length || 0) > 30 || (afterVal?.length || 0) > 30;
                        if (isLong && !showFullText[idx]) {
                          beforeVal = trimText(beforeVal);
                          afterVal = trimText(afterVal);
                        }

                        return (
                          <div
                            key={c.field}
                            className={`activity__logChange ${showFullText[idx] ? "expanded" : ""}`}
                          >
                            <span className="activity__field">{label}</span>:
                            <span className="activity__before">{beforeVal ?? ""}</span>
                            <span className="activity__arrow">→</span>
                            <span className="activity__after">{afterVal ?? ""}</span>
                          </div>
                        );
                      })}
                    </div>

                    {/* 사유 */}
                    {a.reason && <div className="activity__logReason">사유: {a.reason}</div>}

                    {/* 각 로그별 더보기 버튼 (최하단으로 이동) */}
                    {a.changes.some(c => 
                      (stripHtml(c.beforeValue)?.length || 0) > 30 || 
                      (stripHtml(c.afterValue)?.length || 0) > 30
                    ) && (
                      <button
                        className="btn-more"
                        onClick={() => {
                          const newState = [...showFullText];
                          newState[idx] = !newState[idx];
                          setShowFullText(newState);
                        }}
                        style={{ marginTop: "8px" }}
                      >
                        {showFullText[idx] ? "접기" : "더보기"}
                      </button>
                    )}
                  </div>
                ))}
              </div>
            )}

            {/* 전체 로그 더보기 버튼 */}
            {activityLog.length > 3 && (
               <div className="activity__logs__footer">
              <button
                onClick={() => setShowAllLogs(prev => !prev)}
                type="button"
                className="btn-more"
                style={{ marginTop: "12px" }}
              >
                {showAllLogs ? "접기" : "더보기"}
              </button>
              </div>
            )}
          </div>
        </div>

      </section>
    </div>
  );
}