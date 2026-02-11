import "../css/Tasks.css";
import { NavLink } from "react-router-dom";
import { useEffect, useState, useCallback } from "react";
import { api } from "../api/api";

export default function Tasks() {
  const [scope, setScope] = useState("all"); 
  const [status, setStatus] = useState("");  
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(false);

  // paging state
  const [page, setPage] = useState(0);   
  const [size] = useState(9);           
  const [totalPages, setTotalPages] = useState(0);

  // 백엔드 응답이 Page면 content만 뽑기 (유지)
  const normalizeTasks = (data) => {
    if (Array.isArray(data)) return data;
    if (Array.isArray(data?.content)) return data.content;
    if (Array.isArray(data?.tasks)) return data.tasks;
    return [];
  };

  // 페이지 버튼 범위 만들기
  const buildPageButtons = (current, total) => {
    if (total <= 1) return [0];

    const maxButtons = 7;
    const half = Math.floor(maxButtons / 2);

    let start = Math.max(0, current - half);
    let end = Math.min(total - 1, current + half);

    const count = end - start + 1;
    if (count < maxButtons) {
      const 부족 = maxButtons - count;
      start = Math.max(0, start - 부족);
      end = Math.min(total - 1, end + (maxButtons - (end - start + 1)));
    }

    const pages = [];
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  };

  // HTML -> 텍스트
  const stripHtml = (html = "") => {
    const doc = new DOMParser().parseFromString(html, "text/html");
    return (doc.body.textContent || "").trim();
  };

  const ellipsis = (s, n = 120) => (s.length > n ? s.slice(0, n) + "…" : s);

  const fetchTasks = useCallback(async () => {
    setLoading(true);
    try {
      const params = {};
      if (scope) params.scope = scope;
      if (status) params.status = status;

      params.page = page;
      params.size = size;

      const res = await api.get("/api/tasks", { params });

      // 확인용
      // console.log("GET /api/tasks params =", params);
      // console.log("GET /api/tasks res.data =", res.data);

      const list = normalizeTasks(res.data);
      setTasks(list);

      // totalPages 저장
      setTotalPages(res.data?.totalPages ?? 0);
    } catch (e) {
      console.error("목록 조회 실패", e);
      setTasks([]);
      setTotalPages(0);
    } finally {
      setLoading(false);
    }
  }, [scope, status, page, size]);

  // scope/status 바뀌면 0페이지로 리셋
  useEffect(() => {
    setPage(0);
  }, [scope, status]);

  useEffect(() => {
    fetchTasks();
  }, [fetchTasks]);

  const pageButtons = buildPageButtons(page, totalPages);

  return (
    <div className="tasks">
      <div className="tasks__header">
        <div className="tasks__titleRow">
          <h2 className="tasks__title">Tasks</h2>
          <NavLink className="tasks__create" to="/tasks/taskcreate">
            + 작성
          </NavLink>
        </div>

        <div className="tasks__controls">
          <div className="tasks__tabs" role="tablist" aria-label="업무 범위">
            <button
              type="button"
              className={`tasks__tab ${scope === "all" ? "is-active" : ""}`}
              onClick={() => setScope("all")}
            >
              전체 업무
            </button>

            <button
              type="button"
              className={`tasks__tab ${scope === "created" ? "is-active" : ""}`}
              onClick={() => setScope("created")}
            >
              내가 만든 업무
            </button>

            <button
              type="button"
              className={`tasks__tab ${scope === "assigned" ? "is-active" : ""}`}
              onClick={() => setScope("assigned")}
            >
              담당 업무
            </button>
          </div>

          <select
            className="tasks__select"
            value={status}
            onChange={(e) => setStatus(e.target.value)}
          >
            <option value="">전체</option>
            <option value="TODO">TODO</option>
            <option value="IN_PROGRESS">IN_PROGRESS</option>
            <option value="REVIEW">REVIEW</option>
            <option value="DONE">DONE</option>
            <option value="ON_HOLD">ON_HOLD</option>
            <option value="CANCELED">CANCELED</option>
          </select>
        </div>
      </div>

      <div className="tasks__divider" />

      {loading && <div className="tasks__state">불러오는 중...</div>}
      {!loading && tasks.length === 0 && (
        <div className="tasks__state">업무가 없습니다.</div>
      )}

      <ul className="tasks__grid">
        {tasks.map((t) => {
          const descText = ellipsis(stripHtml(t.description || ""));

          const createdLabel =
            `${t.createdByName ?? "-"}${t.createdByDepartment ? ` (${t.createdByDepartment})` : ""}`;

          const assigneeLabel =
            t.assigneeName
              ? `${t.assigneeName}${t.assigneeDepartment ? ` (${t.assigneeDepartment})` : ""}`
              : "-";

          return (
            <li key={t.id} className="tasks__card">
              <div className="tasks__cardTop">
                <strong className="tasks__cardTitle">{t.title}</strong>
                <span className={`tasks__badge tasks__badge--${(t.status || "").toLowerCase()}`}>
                  {t.status}
                </span>
              </div>

              {descText && <div className="tasks__desc">{descText}</div>}

              <div className="tasks__meta">
                <span>priority: {t.priority ?? "-"}</span>
                <span className="tasks__dot">·</span>
                <span>due: {t.dueDate ?? "-"}</span>
              </div>

              <div className="tasks__meta">
                <span>작성: {createdLabel}</span>
                <span className="tasks__dot">·</span>
                <span>담당: {assigneeLabel}</span>
              </div>
            </li>
          );
        })}
      </ul>

      {/* Pagination UI */}
      {totalPages > 1 && (
        <div className="tasks__pagination" aria-label="페이지 이동">
          <button
            type="button"
            className="tasks__pageBtn"
            onClick={() => setPage(0)}
            disabled={page === 0}
          >
            «
          </button>

          <button
            type="button"
            className="tasks__pageBtn"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
          >
            ‹
          </button>

          {pageButtons[0] > 0 && <span className="tasks__pageEllipsis">…</span>}

          {pageButtons.map((p) => (
            <button
              key={p}
              type="button"
              className={`tasks__pageBtn ${p === page ? "is-active" : ""}`}
              onClick={() => setPage(p)}
            >
              {p + 1}
            </button>
          ))}

          {pageButtons[pageButtons.length - 1] < totalPages - 1 && (
            <span className="tasks__pageEllipsis">…</span>
          )}

          <button
            type="button"
            className="tasks__pageBtn"
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
          >
            ›
          </button>

          <button
            type="button"
            className="tasks__pageBtn"
            onClick={() => setPage(totalPages - 1)}
            disabled={page >= totalPages - 1}
          >
            »
          </button>
        </div>
      )}
    </div>
  );
}
