import "../css/Tasks.css";
import { NavLink, useSearchParams } from "react-router-dom";
import { useEffect, useState, useCallback, useMemo } from "react";
import { api } from "../api/api";
import { useAuth } from "../auth/useAuth";

const EMPTY_PAGE = {
  content: [],
  page: 0,
  size: 9,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
};

// 허용 값(쿼리 방어)
const SCOPES = ["all", "public", "team", "created", "assigned"];
const STATUSES = ["TODO", "IN_PROGRESS", "REVIEW", "DONE", "ON_HOLD", "CANCELED"];

export default function Tasks() {
  const { user } = useAuth(); // eslint-disable-line no-unused-vars

  // URL 쿼리
  const [sp, setSp] = useSearchParams();
  const qpScope = sp.get("scope") || "all";
  const qpStatus = sp.get("status") || "";
  const qpPageRaw = sp.get("page");

  const qpPage = useMemo(() => {
    const n = Number(qpPageRaw);
    return Number.isFinite(n) && n >= 0 ? n : 0;
  }, [qpPageRaw]);

  const normalizedScope = useMemo(() => (SCOPES.includes(qpScope) ? qpScope : "all"), [qpScope]);
  const normalizedStatus = useMemo(
    () => (qpStatus === "" || STATUSES.includes(qpStatus) ? qpStatus : ""),
    [qpStatus]
  );

  // scope/status/page는 URL을 따라가게
  const [scope, setScope] = useState(normalizedScope);
  const [status, setStatus] = useState(normalizedStatus);
  const [page, setPage] = useState(qpPage);

  const [loading, setLoading] = useState(false);

  // 정렬 UX (프론트 정렬)
  // createdAtDesc | dueDateAsc | dueDateDesc | priorityDesc
  const [sort, setSort] = useState("createdAtDesc");

  // PageResponse 전체를 상태로 관리
  const [pageData, setPageData] = useState({ ...EMPTY_PAGE });

  // size는 pageData 기준(원래 너 코드 유지)
  const size = pageData.size;
  const totalPages = pageData.totalPages;

  // URL이 바뀌면 state도 따라가게
  useEffect(() => {
    setScope(normalizedScope);
    setStatus(normalizedStatus);
    setPage(qpPage);
  }, [normalizedScope, normalizedStatus, qpPage]);

  // state 바꿀 때 URL도 업데이트
  const updateParams = useCallback(
    (next, { replace = true } = {}) => {
      const cur = Object.fromEntries(sp.entries());
      const merged = { ...cur, ...next };

      // 빈 값은 쿼리에서 제거
      Object.keys(merged).forEach((k) => {
        if (merged[k] === "" || merged[k] == null) delete merged[k];
      });

      setSp(merged, { replace });
    },
    [sp, setSp]
  );

  const onChangeScope = (v) => {
    const nextScope = SCOPES.includes(v) ? v : "all";
    setScope(nextScope);
    setPage(0);
    updateParams({ scope: nextScope, page: 0 });
  };

  const onChangeStatus = (v) => {
    const nextStatus = v && STATUSES.includes(v) ? v : "";
    setStatus(nextStatus);
    setPage(0);
    updateParams({ status: nextStatus, page: 0 });
  };

  // 페이지 이동 시 URL page도 같이 동기화
  useEffect(() => {
    updateParams({ page }, { replace: true });
  }, [page, updateParams]);

  const buildPageButtons = (current, total) => {
    if (total <= 1) return [0];

    const maxButtons = 7;
    const half = Math.floor(maxButtons / 2);

    let start = Math.max(0, current - half);
    let end = Math.min(total - 1, current + half);

    const count = end - start + 1;
    if (count < maxButtons) {
      const remaining = maxButtons - count;
      start = Math.max(0, start - remaining);
      end = Math.min(total - 1, end + (maxButtons - (end - start + 1)));
    }

    const pages = [];
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  };

  const stripHtml = (html = "") => {
    const doc = new DOMParser().parseFromString(html, "text/html");
    return (doc.body.textContent || "").trim();
  };

  const ellipsis = (s, n = 120) => (s.length > n ? s.slice(0, n) + "…" : s);

  const visibilityLabel = (v) => {
    if (v === "PUBLIC") return "전사";
    if (v === "DEPARTMENT") return "부서";
    if (v === "PRIVATE") return "개인";
    return v ?? "-";
  };

  const formatListDateTime = (isoString) => {
    if (!isoString) return "-";
    const d = new Date(isoString);
    const now = new Date();

    const pad = (n) => String(n).padStart(2, "0");

    const hh = pad(d.getHours());
    const mi = pad(d.getMinutes());

    const yyyy = d.getFullYear();
    const mm = pad(d.getMonth() + 1);
    const dd = pad(d.getDate());

    const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const startOfThatDay = new Date(d.getFullYear(), d.getMonth(), d.getDate());
    const diffDays = Math.floor((startOfToday - startOfThatDay) / (24 * 60 * 60 * 1000));

    if (diffDays === 0) return `오늘 ${hh}:${mi}`;
    if (diffDays === 1) return `어제 ${hh}:${mi}`;
    return `${yyyy}.${mm}.${dd}`;
  };

  // 마감 임박/초과 강조 + D-Day
  const parseLocalDate = (s) => {
    if (!s) return null;
    const d = new Date(`${s}T00:00:00`);
    return Number.isNaN(d.getTime()) ? null : d;
  };

  const startOfToday = () => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), now.getDate());
  };

  const daysDiffFromToday = (dueDateStr) => {
    const due = parseLocalDate(dueDateStr);
    if (!due) return null;
    const today = startOfToday();
    const ms = due.getTime() - today.getTime();
    return Math.round(ms / (1000 * 60 * 60 * 24));
  };

  // D-3 / D-DAY / OVERDUE 2d
  const ddayLabel = (dueDateStr) => {
    const diff = daysDiffFromToday(dueDateStr);
    if (diff === null) return null;

    // 7일 넘게 남은 건 라벨 숨김
    if (diff > 7) return null;

    if (diff === 0) return "D-DAY";
    if (diff > 0) return `D-${diff}`;
    return `OVERDUE ${Math.abs(diff)}d`;
  };

  // 마감 임박/초과 스타일 클래스
const dueClass = (dueDateStr, status) => {

  if (status === "DONE" || status === "CANCELED") return "";

  const diff = daysDiffFromToday(dueDateStr);
  if (diff === null) return "";
  if (diff < 0) return "is-overdue";
  if (diff === 0) return "is-dday";
  if (diff <= 2) return "is-soon";
  return "";
};

  const priorityRank = (p) => {
    if (p === "HIGH") return 3;
    if (p === "MEDIUM") return 2;
    if (p === "LOW") return 1;
    return 0;
  };

  const normalizePageResponse = (data) => {
    if (data && Array.isArray(data.content)) {
      return {
        content: data.content ?? [],
        page: Number.isFinite(data.page) ? data.page : 0,
        size: Number.isFinite(data.size) ? data.size : EMPTY_PAGE.size,
        totalElements: Number.isFinite(data.totalElements) ? data.totalElements : 0,
        totalPages: Number.isFinite(data.totalPages) ? data.totalPages : 0,
        first: typeof data.first === "boolean" ? data.first : (data.page ?? 0) === 0,
        last: typeof data.last === "boolean" ? data.last : false,
      };
    }

    if (Array.isArray(data)) {
      return { ...EMPTY_PAGE, content: data };
    }
    if (Array.isArray(data?.tasks)) {
      return { ...EMPTY_PAGE, content: data.tasks };
    }

    return { ...EMPTY_PAGE };
  };

  // useCallback: 함수를 기억하는 훅
  // 기본 구조: const 함수 = useCallback(() => {실행 코드}, [의존성]);
  // 리액트는 리렌더 될 때마다 함수를 새로 만들어서 불필요하가 낭비되는 일이 있음
  // 언제 쓰면 좋을까요?
  // 자식 컴포넌트에 함수 props 전달
  // React.memo 사용 중
  // useEffect dependency에 함수 들어갈 때
  // 렌더링 최적화 필요 시
  // 서버 조회: (scope/status/page/size) 기준으로 요청
  const fetchTasks = useCallback(async () => {
    setLoading(true);
    try {
      const params = {
        scope: scope || "all",
        page,
        size,
      };
      if (status) params.status = status;

      const res = await api.get("/api/tasks", { params });

      // normalizePageResponse: 어떤 응답이 와도 동일 구조로 만들어줌, 서버 응답 구조 정리기
      // 다른 구조로 오는 함수를 편하게 처리하기 위해서 생김
      // 매번 구조 다르게 처리할 필요가 없음
      const normalized = normalizePageResponse(res.data);
      setPageData(normalized);

      // 서버가 page를 0-based로 주는 게 확실하면 이거 유지.
      // 혹시 서버가 다른 page를 보정해서 내려주는 경우, 프론트 page도 따라가게:
      if (Number.isFinite(normalized.page) && normalized.page !== page) {
        setPage(normalized.page);
        updateParams({ page: normalized.page }, { replace: true });
      }
    } catch (e) {
      console.error("목록 조회 실패", e);
      setPageData({ ...EMPTY_PAGE, size });
    } finally {
      setLoading(false);
    }
  }, [scope, status, page, size, updateParams]);

  // scope/status/page 바뀌면 조회
  useEffect(() => {
    fetchTasks();
  }, [fetchTasks]);

  const pageButtons = useMemo(() => buildPageButtons(page, totalPages), [page, totalPages]);

  const sortedTasks = useMemo(() => {
    const arr = [...(pageData.content ?? [])];

    if (sort === "createdAtDesc") {
      return arr.sort((a, b) => {
        const da = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const db = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        if (db !== da) return db - da;
        return (b.id ?? 0) - (a.id ?? 0);
      });
    }

    if (sort === "dueDateAsc") {
      return arr.sort((a, b) => {
        const da = parseLocalDate(a.dueDate)?.getTime() ?? Number.POSITIVE_INFINITY;
        const db = parseLocalDate(b.dueDate)?.getTime() ?? Number.POSITIVE_INFINITY;
        return da - db;
      });
    }

    if (sort === "dueDateDesc") {
      return arr.sort((a, b) => {
        const da = parseLocalDate(a.dueDate)?.getTime() ?? Number.NEGATIVE_INFINITY;
        const db = parseLocalDate(b.dueDate)?.getTime() ?? Number.NEGATIVE_INFINITY;
        return db - da;
      });
    }

    if (sort === "priorityDesc") {
      return arr.sort((a, b) => priorityRank(b.priority) - priorityRank(a.priority));
    }

    return arr;
  }, [pageData.content, sort]);

  // 페이지 이동 함수들 (pageData.first/last 사용)
  const goFirst = () => {
    if (!pageData.first) setPage(0);
  };

  const goPrev = () => {
    if (!pageData.first) setPage((p) => Math.max(0, p - 1));
  };

  const goNext = () => {
    if (!pageData.last) setPage((p) => Math.min(totalPages - 1, p + 1));
  };

  const goLast = () => {
    if (!pageData.last) setPage(Math.max(0, totalPages - 1));
  };

  const goPage = (p) => setPage(p);

  return (
    <div className="tasks">
      <div className="tasks__header">
        <div className="tasks__titleRow">
          <h2 className="tasks__title">Tasks</h2>
          <NavLink className="tasks__create" to="/tasks/create">
            + 작성
          </NavLink>
        </div>

        <div className="tasks__controls">
          <div className="tasks__tabs" role="tablist" aria-label="업무 범위">
            <button
              type="button"
              className={`tasks__tab ${scope === "all" ? "is-active" : ""}`}
              onClick={() => onChangeScope("all")}
            >
              전체 업무
            </button>

            <button
              type="button"
              className={`tasks__tab ${scope === "public" ? "is-active" : ""}`}
              onClick={() => onChangeScope("public")}
            >
              전사 업무
            </button>

            <button
              type="button"
              className={`tasks__tab ${scope === "team" ? "is-active" : ""}`}
              onClick={() => onChangeScope("team")}
            >
              우리 팀 업무
            </button>

            <button
              type="button"
              className={`tasks__tab ${scope === "created" ? "is-active" : ""}`}
              onClick={() => onChangeScope("created")}
            >
              내가 만든 업무
            </button>

            <button
              type="button"
              className={`tasks__tab ${scope === "assigned" ? "is-active" : ""}`}
              onClick={() => onChangeScope("assigned")}
            >
              담당 업무
            </button>
          </div>

          <div className="tasks__filters">
            <select className="tasks__select" value={status} onChange={(e) => onChangeStatus(e.target.value)}>
              <option value="">전체</option>
              {STATUSES.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>

            <select className="tasks__select" value={sort} onChange={(e) => setSort(e.target.value)}>
              <option value="createdAtDesc">최신순</option>
              <option value="dueDateAsc">마감 임박순</option>
              <option value="dueDateDesc">마감 늦은순</option>
              <option value="priorityDesc">우선순위 높은순</option>
            </select>
          </div>
        </div>
      </div>

      <div className="tasks__divider" />

      {loading && <div className="tasks__state">불러오는 중...</div>}
      {!loading && sortedTasks.length === 0 && <div className="tasks__state">업무가 없습니다.</div>}

      <ul className="tasks__grid">
        {sortedTasks.map((t) => {
          const descText = ellipsis(stripHtml(t.description || ""));

          const createdLabel = `${t.createdByName ?? "-"}${
            t.createdByDepartmentName ? ` (${t.createdByDepartmentName})` : ""
          }`;

          const assigneeLabel = t.assigneeName
            ? `${t.assigneeName}${t.assigneeDepartmentName ? ` (${t.assigneeDepartmentName})` : ""}`
            : "-";

          // DONE이면 라벨 숨김
          const dday = t.dueDate && t.status !== "DONE" && t.status !== "CANCELED" ? ddayLabel(t.dueDate) : null;

          return (
            // DONE이면 마감 강조 클래스 제거
            <li key={t.id} className={`tasks__card tasks__card--${(t.status || "").toLowerCase()} ${dueClass(t.dueDate, t.status)}`}>
              <NavLink to={`/tasks/${t.id}`} className="tasks__cardLink">
                <div className="tasks__cardTop">
                  <strong className="tasks__cardTitle">{t.title}</strong>
                </div>

                {descText && <div className="tasks__desc">{descText}</div>}

                <div className="tasks__meta">
                  <span>마감일: {t.dueDate ?? "-"}</span>
                  <span className="tasks__dot">·</span>
                  <span>작성일: {formatListDateTime(t.createdAt)}</span>
                </div>

                <div className="tasks__meta">
                  <span>작성자: {createdLabel}</span>
                  <span className="tasks__dot">·</span>
                  <span>담당자: {assigneeLabel}</span>
                </div>

                <div className="tasks__badges">
                  <span className={`tasks__badge tasks__badge--${(t.status || "").toLowerCase()}`}>
                    {t.status}
                  </span>

                  <span className="tasks__badge tasks__badge--visibility">{visibilityLabel(t.visibility)}</span>

                  <span
                    className={`tasks__badge tasks__badge--priority tasks__badge--priority-${(t.priority || "")
                      .toLowerCase()}`}
                  >
                    중요도: {t.priority ?? "-"}
                  </span>

                  {/* DONE이면 자동으로 안 뜸 */}
                  {dday && <span className="tasks__badge tasks__badge--dday">{dday}</span>}
                </div>
              </NavLink>
            </li>
          );
        })}
      </ul>

      {totalPages > 1 && (
        <div className="tasks__pagination" aria-label="페이지 이동">
          <button type="button" className="tasks__pageBtn" onClick={goFirst} disabled={pageData.first}>
            «
          </button>

          <button type="button" className="tasks__pageBtn" onClick={goPrev} disabled={pageData.first}>
            ‹
          </button>

          {pageButtons[0] > 0 && <span className="tasks__pageEllipsis">…</span>}

          {pageButtons.map((p) => (
            <button
              key={p}
              type="button"
              className={`tasks__pageBtn ${p === page ? "is-active" : ""}`}
              onClick={() => goPage(p)}
            >
              {p + 1}
            </button>
          ))}

          {pageButtons[pageButtons.length - 1] < totalPages - 1 && <span className="tasks__pageEllipsis">…</span>}

          <button type="button" className="tasks__pageBtn" onClick={goNext} disabled={pageData.last}>
            ›
          </button>

          <button type="button" className="tasks__pageBtn" onClick={goLast} disabled={pageData.last}>
            »
          </button>
        </div>
      )}
    </div>
  );
}