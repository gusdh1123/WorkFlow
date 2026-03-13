// CSS import
import "../../css/tasks/TaskMain.css";

// React Router 관련 훅
import { NavLink, useSearchParams } from "react-router-dom";

// React 훅
import { useEffect, useState, useCallback, useMemo } from "react";

// API 호출용 모듈
import { api } from "../../api/api";

// 인증 정보 hook
import { useAuth } from "../../auth/hooks/useAuth";

// 업무 범위와 상태 상수
import { SCOPES, STATUSES } from "../../constants/taskOptions";

// visibility 코드 → 문자열 라벨 변환
import { visibilityLabel } from "../../utils/taskUtils";

// 날짜 처리 관련 유틸
import { parseLocalDate, ddayLabel, dueClass, formatListDateTime } from "../../utils/dateUtils";

// 서버 응답이 없거나 배열만 반환될 때 기본 구조
const EMPTY_PAGE = {
  content: [],        // 현재 페이지 업무 리스트
  page: 0,            // 현재 페이지 번호
  size: 9,            // 페이지 항목 수
  totalElements: 0,   // 전체 업무 수
  totalPages: 0,      // 전체 페이지 수
  first: true,        // 첫 페이지 여부
  last: true,         // 마지막 페이지 여부
};

// /**
//  * 유튜브 URL → 썸네일 URL 변환
//  * iframe 대신 리스트 카드에서는 이미지 미리보기로 사용
//  * @param url - 유튜브 영상 URL
//  * @returns 썸네일 이미지 URL
//  */
// const getVideoThumbnail = (url) => {
//   try {
//     // https://www.youtube.com/watch?v=ID
//     const vid = new URL(url).searchParams.get("v") || url.split("/").pop();
//     return vid ? `https://img.youtube.com/vi/${vid}/0.jpg` : null;
//   } catch {
//     return null;
//   }
// };

export default function Tasks() {
  // 로그인 사용자 정보 가져오기
  const { user } = useAuth();

  // URL 쿼리 파라미터 읽기/쓰기
  const [sp, setSp] = useSearchParams();

  // URL에서 scope, status, page 추출
  const qpScope = sp.get("scope") || "all";  
  const qpStatus = sp.get("status") || "";   
  const qpPageRaw = sp.get("page");          
  const qpDept = sp.get("dept") || "";      

  // page는 문자열 → 숫자로 변환, 유효하지 않으면 0으로 초기화
  const qpPage = useMemo(() => {
    const n = Number(qpPageRaw);
    return Number.isFinite(n) && n >= 0 ? n : 0;
  }, [qpPageRaw]);

  // URL에서 받은 scope/status → 실제 화면에서 사용 가능한 값으로 정규화
  const normalizedScope = useMemo(
    () => (SCOPES.includes(qpScope) ? qpScope : "all"),
    [qpScope]
  );
  const normalizedStatus = useMemo(
    () => (qpStatus === "" || STATUSES.includes(qpStatus) ? qpStatus : ""),
    [qpStatus]
  );
  const normalizedDept = useMemo(() => qpDept, [qpDept]);

  // 화면에서 선택한 scope/status/page 상태
  const [scope, setScope] = useState(normalizedScope);
  const [status, setStatus] = useState(normalizedStatus);
  const [page, setPage] = useState(qpPage);
  const [deptId, setDeptId] = useState(normalizedDept);

  // 로딩 상태 관리
  const [loading, setLoading] = useState(false);

  // 프론트에서 제공할 정렬 옵션, 초기값은 생성일 내림차순
  const [sort, setSort] = useState("createdAtDesc");

  // 서버에서 받아온 페이지 전체 데이터 상태
  const [pageData, setPageData] = useState({ ...EMPTY_PAGE });

  // size와 totalPages는 pageData 기준
  const size = pageData.size;          
  const totalPages = pageData.totalPages; 

  // URL 변경 시 state 동기화
  useEffect(() => {
    setScope(normalizedScope);
    setStatus(normalizedStatus);
    setDeptId(normalizedDept);
    setPage(qpPage);
  }, [normalizedScope, normalizedStatus, normalizedDept, qpPage]);

  /**
   * URL 쿼리 업데이트
   * @param next - 변경할 쿼리 객체
   * @param replace - history replace 여부
   */
  const updateParams = useCallback(
    (next, { replace = true } = {}) => {
      const cur = Object.fromEntries(sp.entries());
      const merged = { ...cur, ...next };
      Object.keys(merged).forEach((k) => {
        if (merged[k] === "" || merged[k] == null) delete merged[k];
      });
      setSp(merged, { replace });
    },
    [sp, setSp]
  );

  // 업무 범위 변경
  const onChangeScope = (v) => {
    const nextScope = SCOPES.includes(v) ? v : "all";
    setScope(nextScope);
    setPage(0); 
    updateParams({ scope: nextScope, page: 0 });
  };

  // 업무 상태 변경
  const onChangeStatus = (v) => {
    const nextStatus = v && STATUSES.includes(v) ? v : "";
    setStatus(nextStatus);
    setPage(0); 
    updateParams({ status: nextStatus, page: 0 });
  };

  // 부서별 변경
  const onChangeDept = (id) => {
    setDeptId(id);
    setPage(0);
    updateParams({ dept: id, page: 0 });
  };

  // 페이지 변경 시 URL 동기화
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

  const hasImageInHtml = (html = "") => /<img\b/i.test(html);
  const hasVideoInHtml = (html = "") => /<iframe\b/i.test(html);

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

  const fetchTasks = useCallback(async () => {
    setLoading(true);
    try {
      const params = { scope: scope || "all", page, size };
      if (status) params.status = status;
      if (deptId) params.deptId = deptId;
      const res = await api.get("/api/tasks", { params });
      const normalized = normalizePageResponse(res.data);
      setPageData(normalized);
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
  }, [scope, status, page, size, deptId, updateParams]);

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

  const goFirst = () => { if (!pageData.first) setPage(0); };
  const goPrev = () => { if (!pageData.first) setPage((p) => Math.max(0, p - 1)); };
  const goNext = () => { if (!pageData.last) setPage((p) => Math.min(totalPages - 1, p + 1)); };
  const goLast = () => { if (!pageData.last) setPage(Math.max(0, totalPages - 1)); };
  const goPage = (p) => setPage(p);

  const getAttachSummary = (t) => {
    const atts = Array.isArray(t?.attachments) ? t.attachments : [];
    const count = Number.isFinite(t?.attachmentsCount) ? t.attachmentsCount : atts.length;
    const hasAnyFile = count > 0;

    const hasImageFile = atts.some((a) => {
      const ct = (a?.contentType || "").toLowerCase();
      const name = (a?.originalFilename || "").toLowerCase();
      return ct.startsWith("image/") || /\.(png|jpg|jpeg|webp|gif)$/i.test(name);
    });

    const hasVideoFile = atts.some((a) => {
      const ct = (a?.contentType || "").toLowerCase();
      const name = (a?.originalFilename || "").toLowerCase();
      return ct.startsWith("video/") || /\.(mp4|mov|webm|mkv|avi)$/i.test(name);
    });

    const rawHtml = t?.description || "";
    const hasImgInDesc = hasImageInHtml(rawHtml);
    const hasVideoInDesc = hasVideoInHtml(rawHtml);

    // // 추가: 카드용 미리보기용 썸네일 URL 반환
    // let previewImage = null;
    // if (hasImgInDesc) {
    //   const doc = new DOMParser().parseFromString(rawHtml, "text/html");
    //   const img = doc.querySelector("img");
    //   if (img) previewImage = img.src;
    // } else if (hasVideoInDesc) {
    //   const doc = new DOMParser().parseFromString(rawHtml, "text/html");
    //   const iframe = doc.querySelector("iframe");
    //   // if (iframe) previewImage = getVideoThumbnail(iframe.src); // 유튜브 썸네일
    // }

    return {
      count,
      hasAnyFile,
      hasImage: hasImageFile || hasImgInDesc,
      hasVideo: hasVideoFile || hasVideoInDesc,
      // previewImage, // 추가: 카드에서 보여줄 미리보기
    };
  };

  // TODO: departments 배열 API 호출/상수 필요
  const departments = [
    { id: "1", name: "운영팀" },
    { id: "2", name: "개발팀" },
    { id: "3", name: "디자인팀" },
  ];

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

            <button
              type="button"
              className={`tasks__tab ${scope === "private" ? "is-active" : ""}`}
              onClick={() => onChangeScope("private")}
            >
              개인 업무
            </button>

          </div>

          <div className="tasks__filters">
            <div className="tasks__filters">
            {user?.role == "ADMIN" && !["team", "created", "assigned"].includes(scope) && (
            <select className="tasks__select" value={deptId} onChange={(e) => onChangeDept(e.target.value)}>
              <option value="">전체 부서</option>
              {departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
            </select>
            )}

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
      </div>

      <div className="tasks__divider" />

      {loading && <div className="tasks__state">불러오는 중...</div>}
      {!loading && sortedTasks.length === 0 && <div className="tasks__state">업무가 없습니다.</div>}

      <ul className="tasks__grid">
        {sortedTasks.map((t) => {
          const descText = stripHtml(t.description || "");
          const descForRender = descText ? descText : "\u00A0";

          const att = getAttachSummary(t);

          const createdLabel = `${t.createdByName ?? "-"}${
            t.createdByDepartmentName ? ` (${t.createdByDepartmentName})` : ""
          }`;

          const assigneeLabel = t.assigneeName
            ? `${t.assigneeName}${t.assigneeDepartmentName ? ` (${t.assigneeDepartmentName})` : ""}`
            : "-";

          const dday = t.dueDate && t.status !== "DONE" && t.status !== "CANCELED" ? ddayLabel(t.dueDate) : null;

          return (
            <li
              key={t.id}
              className={`tasks__card tasks__card--${(t.status || "").toLowerCase()} ${dueClass(t.dueDate, t.status)}`}
            >
              <NavLink to={`/tasks/${t.id}`} className="tasks__cardLink">
                <div className="tasks__cardTop">
                  <strong className="tasks__cardTitle">{t.title}</strong>

                  {/* 첨부 아이콘 */}
                  <div className="tasks__cardIcons">
                    {/* 📎 첨부파일 있으면 */}
                    {att.hasAnyFile && (
                      <span className="tasks__att tasks__att--file" title='첨부파일 포함'>
                        📎
                      </span>
                    )}

                    {/* 📷 이미지 있으면 */}
                    {att.hasImage && (
                      <span className="tasks__att tasks__att--img" title="이미지 포함">
                        📷
                      </span>
                    )}

                    {/* 🎬 영상 있으면 */}
                    {att.hasVideo && (
                      <span className="tasks__att tasks__att--video" title="영상 포함">
                        🎬
                      </span>
                    )}
                  </div>
                </div>

                <div className={`tasks__desc ${descText ? "" : "is-empty"}`}>{descForRender}</div>

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
                  <span className={`tasks__badge tasks__badge--${(t.status || "").toLowerCase()}`}>{t.status}</span>

                  <span className="tasks__badge tasks__badge--visibility">{visibilityLabel(t.visibility)}</span>

                  <span
                    className={`tasks__badge tasks__badge--priority tasks__badge--priority-${(t.priority || "").toLowerCase()}`}
                  >
                    중요도: {t.priority ?? "-"}
                  </span>

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
