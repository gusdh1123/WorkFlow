import "../../css/tasks/TaskDetail.css";
import { useEffect, useState, useRef } from "react";
import { useParams, useNavigate, NavLink } from "react-router-dom";
import { api } from "../../api/api.js";

import { visibilityLabel } from "../../utils/taskUtils";
import { formatRelativeDateTime, ddayLabel } from "../../utils/dateUtils";

// 토큰 정보 가져오기
import { userFromToken } from "../../auth/utils/userFromToken.js";
import { useAuth } from "../../auth/hooks/useAuth";

// 이미지 모달
import ImageModal from "../../components/common/ImageModal";

// 첨부 목록
import AttachmentList from "../../components/attachments/AttachmentList";

// HTML 태그 제거 함수
const stripHtml = (html) => {
  if (!html) return "";
  const tmp = document.createElement("DIV");
  tmp.innerHTML = html;
  return tmp.textContent || tmp.innerText || "";
};

// 내용/제목 줄임 + 길이 체크
const trimText = (text) => (text && text.length > 67 ? text.slice(0, 67) + "…" : text);

export default function TaskDetail() {
  const { id } = useParams();
  const nav = useNavigate();

  const [task, setTask] = useState(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");
  const [auditLogs, setAuditLogs] = useState([]);
  const [showAllLogs, setShowAllLogs] = useState([]); // 카드별 더보기 상태
  const [showAllAuditLogs, setShowAllAuditLogs] = useState(false); // 전체 Audit Log 더보기 상태

  const { accessToken } = useAuth();
  const loginUser = accessToken ? userFromToken(accessToken) : null;

  const descRef = useRef(null);

  const [imgModal, setImgModal] = useState({ open: false, src: "", alt: "" });
  const closeModal = () => setImgModal({ open: false, src: "", alt: "" });

  // 이미지 클릭 시 모달 오픈
  useEffect(() => {
    const el = descRef.current;
    if (!el) return;

    const onClick = (e) => {
      const target = e.target;
      if (target && target.tagName === "IMG") {
        setImgModal({
          open: true,
          src: target.getAttribute("src") || "",
          alt: target.getAttribute("alt") || "",
        });
      }
    };

    el.addEventListener("click", onClick);
    return () => el.removeEventListener("click", onClick);
  }, [task?.description]);

  // Task + Audit Log 조회
  useEffect(() => {
    const controller = new AbortController();

    (async () => {
      setLoading(true);
      setErr("");

      try {
        const res = await api.get(`/api/tasks/${id}`, { signal: controller.signal });
        setTask(res.data);

        const logRes = await api.get(`/api/audit/${id}/audit-logs`, { signal: controller.signal });
        setAuditLogs(logRes.data);

        setShowAllLogs(logRes.data.map(() => false));
      } catch (e) {
        if (e.name === "CanceledError" || e.code === "ERR_CANCELED") return;

        const msg =
          e?.response?.status === 404
            ? "업무를 찾을 수 없습니다."
            : e?.response?.status === 401
            ? "로그인이 필요합니다."
            : "조회에 실패했습니다.";

        setErr(msg);
      } finally {
        setLoading(false);
      }
    })();

    return () => controller.abort();
  }, [id]);

  // useMemo 제거
  const createdAtLabel = formatRelativeDateTime(task?.createdAt);
  const deletedAtLabel = formatRelativeDateTime(task?.deletedAt);
  const favoriteCreatedAtLabel = formatRelativeDateTime(task?.favoriteCreatedAt);

  // useMemo 제거
  const dday = (() => {
    if (!task?.dueDate) return "-";
    if (task?.status === "DONE") return "완료";
    if (task?.status === "CANCELED") return "취소됨";
    return ddayLabel(task.dueDate);
  })();
  
  // useMemo 제거
  const attachments = Array.isArray(task?.attachments)
    ? task.attachments
    : [];

  const onAttachmentDeleted = (deletedId) => {
    setTask((prev) => {
      if (!prev) return prev;
      const next = { ...prev };
      const list = Array.isArray(next.attachments) ? next.attachments : [];
      next.attachments = list.filter((x) => x.id !== deletedId);
      return next;
    });
  };

  if (loading) return <div className="taskdetail__state">불러오는 중...</div>;

  if (err)
    return (
      <div className="taskdetail__state">
        <div className="taskdetail__error">{err}</div>
        <button className="taskdetail__btn taskdetail__btn--ghost" onClick={() => nav(-1)}>
          뒤로가기
        </button>
      </div>
    );

  if (!task) return null;

  const priorityKey = (task?.priority || "").toLowerCase();

  const canEdit =
    task &&
    loginUser &&
    (Number(task.createdById) === Number(loginUser.id) ||
      (task.assigneeId && Number(task.assigneeId) === Number(loginUser.id)) ||
      loginUser.role === "ADMIN" ||
      (loginUser.role === "MANAGER" &&
        loginUser.department?.trim().toLowerCase() === task.workDepartmentName?.trim().toLowerCase()));

  const handleDelete = async () => {
    const reason = window.prompt("삭제 사유를 입력하세요:");
    if (!reason) return;
    if (!window.confirm("정말로 삭제하시겠습니까?")) return;

    try {
      await api.delete(`/api/tasks/${task.id}`, { data: { reason } });
      alert("업무가 삭제되었습니다.");
      nav("/tasks");
    } catch (error) {
      console.error(error);
      alert("삭제에 실패했습니다.");
    }
  };

  const visibleAuditLogs = showAllAuditLogs ? auditLogs : auditLogs.slice(0, 3);

const departments = [
    { id: "Operations", name: "운영팀" },
    { id: "Development", name: "개발팀" },
    { id: "Design", name: "디자인팀" },
  ];

// 복구
const handleRestore = async (taskId) => {
  const reason = prompt("복구 사유를 입력하세요:");
  if (reason === null) return;
  if (reason.trim() === "") {
    alert("사유를 입력해야 합니다.");
    return;
  }
  if (!confirm("이 업무를 복구하시겠습니까?")) return;

  try {
    // 복구 요청
    await api.post(`/api/tasks/${taskId}/restore`, { reason });
    alert("복구 완료");

    // Task + Audit Log 재조회
    const [taskRes, logRes] = await Promise.all([
      api.get(`/api/tasks/${taskId}`),
      api.get(`/api/audit/${taskId}/audit-logs`)
    ]);

    setTask(taskRes.data);
    setAuditLogs(logRes.data);
    setShowAllLogs(logRes.data.map(() => false));
  } catch (e) {
    console.error(e);
    alert("복구 실패");
  }
};

// 즐겨찾기 토글
const toggleFavorite = async (taskId) => {
  // UI에서 즉시 반영 (낙관적 업데이트)
  setTask(prev => prev ? { ...prev, favorite: !prev.favorite } : prev);

  try {
    await api.post(`/api/favorite/${taskId}`);
  } catch (e) {
    console.error("즐겨찾기 실패", e.response?.data || e);
    // 서버 실패 시 롤백
    setTask(prev => prev ? { ...prev, favorite: !prev.favorite } : prev);
    alert("즐겨찾기 변경 실패");
  }
};

// console.log(task);

  return (
    <div className="taskdetail">
      <div className="taskdetail__card taskdetail__card--header">
        <div className="taskdetail__topRow">
          <div>
            <div className="taskdetail__eyebrow">작성일: {createdAtLabel}</div>
            {task.deleted && (
            <div className="taskdetail__eyebrow taskdetail__deleted">삭제일: {deletedAtLabel}</div>
            )}
            <h2 className="taskdetail__title">
              {/* 즐겨찾기 버튼 */}
              {!task.deleted && (
                <button
                  type="button"
                  className={`tasks__favoriteBtn ${task.favorite ? "is-active" : ""}`}
                  onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                  toggleFavorite(task.id);
                }}
                title={task.favorite ? "즐겨찾기 해제" : "즐겨찾기 등록"}
              >
                ★
              </button>
              )} {task.title}
            </h2>
          </div>

          <div className="taskdetail__actions">
            <NavLink className="taskdetail__btn taskdetail__btn--ghost" to="/tasks">
              목록으로
            </NavLink>

             {/* 복구 버튼 */}
             {task.deleted ? (
              <button
                type="button"
                className="tasks__restoreBtn--inline"
                onClick={(e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    handleRestore(task.id);
                  }}
               >
                복구
              </button>
            ) : (
              <>
                {canEdit && (
                  <NavLink className="taskdetail__btn" to={`/tasks/${id}/edit`} state={{ task }}>
                    수정
                  </NavLink>
                )}
                {canEdit && (
                 <button type="button" className="taskdetail__btn taskdetail__btn--danger" onClick={handleDelete}>
                   삭제
                  </button>
                )}
             </>
           )}
          </div>
        </div>

        <div className="taskdetail__badges">
          <span className={`taskdetail__badge taskdetail__badge--${(task.status || "").toLowerCase()}`}>
            {task.status ?? "-"}
          </span>

          <span className="taskdetail__badge taskdetail__badge--visibility">{visibilityLabel(task.visibility)}</span>

          <span className={`taskdetail__badge taskdetail__badge--priority taskdetail__badge--priority-${priorityKey}`}>{task.priority ?? "-"}</span>

          {task.dueDate && <span className="taskdetail__badge taskdetail__badge--due">마감: {task.dueDate}</span>}

          {dday && <span className="taskdetail__badge taskdetail__badge--dday">{dday}</span>}
        </div>
      </div>

      <div className="taskdetail__grid">
        <div className="taskdetail__card">
          {task.description ? (
            <div ref={descRef} className="taskdetail__desc" dangerouslySetInnerHTML={{ __html: task.description }} />
          ) : (
            <div className="taskdetail__empty">설명이 없습니다.</div>
          )}

          <AttachmentList attachments={attachments} onDeleted={onAttachmentDeleted} readOnly />
        </div>

        <ImageModal open={imgModal.open} src={imgModal.src} alt={imgModal.alt} onClose={closeModal} />

        <div className="taskdetail__metaColumn">
          <h3 className="taskdetail__card taskdetail__card--title">업무 정보</h3>

          <div className="taskdetail__card taskdetail__card--meta">
            <div className="taskdetail__metaRow">
              <div className="taskdetail__metaKey">작성자</div>
              <div className="taskdetail__metaVal">{task.createdByName ?? task.creatorName ?? "-"}({departments.find(d => d.id === (task.workDepartmentName ?? "-"))?.name ?? "-"})</div>
            </div>
            <div className="taskdetail__metaRow">
              <div className="taskdetail__metaKey">담당자</div>
              <div className="taskdetail__metaVal">{task.assigneeName ?? "-"}({departments.find(d => d.id === (task.assigneeDepartmentName ?? "-"))?.name ?? "-"})</div>
            </div>
            <div className="taskdetail__metaRow">
              <div className="taskdetail__metaKey">마감일</div>
              <div className="taskdetail__metaVal">
                {task.dueDate ?? "-"} {dday && `(${dday})`}
              </div>
            </div>
            <div className="taskdetail__metaRow">
              <div className="taskdetail__metaKey">작성일</div>
              <div className="taskdetail__metaVal">{createdAtLabel}</div>
            </div>
          {task.deleted && (
          <div className="taskdetail__metaRow">
            <div className="taskdetail__metaKey taskdetail__deleted">삭제일</div>
            <div className="taskdetail__metaVal taskdetail__deleted">{deletedAtLabel}
            </div>
          </div>
          )}
          {task.favorite && (
          <div className="taskdetail__metaRow">
            <div className="taskdetail__metaKey">즐겨찾기 등록</div>
            <div className="taskdetail__metaVal">{favoriteCreatedAtLabel}
            </div>
          </div>
          )}
          </div>

          {visibleAuditLogs.length > 0 &&(
          <h3 className="taskdetail__card taskdetail__card--title">Audit Log</h3>
          )}
          {visibleAuditLogs.map((log, idx) => (
            <div key={idx} className="taskdetail__card taskdetail__card--log">
              <div className={`taskdetail__logItem ${log.isRecent ? "taskdetail__logItem--recent" : ""}`}>
                <div className="taskdetail__metaRow">
                  <div className="taskdetail__metaKey">행위자</div>
                  <div className="taskdetail__metaVal">{log.actorName}</div>
                </div>

                <div className="taskdetail__metaRow">
                  <div className="taskdetail__metaKey">작업 시간</div>
                  <div className="taskdetail__metaVal">{formatRelativeDateTime(log.modifiedAt)}</div>
                </div>

                {/* 수정 필드 출력 */}
                {(() => {
                  const fields = Array.isArray(log.changes) ? log.changes : [];
                  const showAll = showAllLogs[idx];

                  return (showAll ? fields : fields.slice(0, 3)).map((change, i) => {
                    const field = change.field;
                    let oldVal = field === "description" ? stripHtml(change.beforeValue) : change.beforeValue;
                    let newVal = field === "description" ? stripHtml(change.afterValue) : change.afterValue;

                    let label = "";
                    let valueText = "";

                    if (field === "attachment_add") {
                      label = "첨부파일 추가";
                      valueText = `${newVal ?? ""}`;
                    } else if (field === "attachment_delete") {
                      label = "첨부파일 삭제";
                      valueText = `${oldVal ?? ""}`;
                    } else {
                      const fieldNameMap = {
                        title: "제목 변경",
                        description: "내용 변경",
                        assignee_id: "담당자 변경",
                        priority: "우선순위 변경",
                        visibility: "공개 범위 변경",
                        due_date: "마감일 변경",
                        status: "진행도 변경",
                        deleted: "업무 삭제",
                        restore: "업무 복구",
                        create: "업무 생성",
                      };
                      label = fieldNameMap[field] ? `${fieldNameMap[field]}` : "수정 필드";

                      // 로그 안에서 제목/내용만 길이 체크 후 trimText 적용
                      if (field === "title" || field === "description") {
                        const isLong =
                          (oldVal?.length || 0) > 30 || (newVal?.length || 0) > 30;
                        if (!showAll && isLong) {
                          oldVal = trimText(oldVal);
                          newVal = trimText(newVal);
                        }
                      }

                      valueText = `${oldVal ?? ""} → ${newVal ?? ""}`;
                    }

                    return (
                      <div key={i} className="taskdetail__metaRow">
                        <div className="taskdetail__metaKey">{label}</div>
                        <div className="taskdetail__metaVal">{valueText}</div>
                      </div>
                    );
                  });
                })()}

                {/* 수정 사유 */}
                {log.reason && (
                  <div className="taskdetail__metaRow">
                    <div className="taskdetail__metaKey">사유</div>
                    <div className="taskdetail__metaVal">{log.reason}</div>
                  </div>
                )}
              </div>

              {/* 카드별 더보기 버튼: 기존 3개 초과 + title/description 30자 이상이면 버튼 표시 */}
              {(log.changes?.length > 3 ||
                log.changes?.some(
                  (c) =>
                    (c.field === "title" || c.field === "description") &&
                    ((stripHtml(c.beforeValue || "")?.length || 0) > 30 ||
                      (stripHtml(c.afterValue || "")?.length || 0) > 30)
                )) && (
                <button
                  className="taskdetail__btn taskdetail__btn--ghost taskdetail__btn--metaRight"
                  onClick={() => {
                    const newState = [...showAllLogs];
                    newState[idx] = !newState[idx];
                    setShowAllLogs(newState);
                  }}
                >
                  {showAllLogs[idx] ? "접기" : "더보기"}
                </button>
              )}
            </div>
          ))}

          {/* 전체 Audit Log 더보기 버튼 */}
          {auditLogs.length > 3 && (
            <button
              className="taskdetail__btn taskdetail__btn--ghost"
              onClick={() => setShowAllAuditLogs(!showAllAuditLogs)}
            >
              {showAllAuditLogs ? "접기" : "더보기"}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}