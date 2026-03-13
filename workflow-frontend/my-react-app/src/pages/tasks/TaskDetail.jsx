import "../../css/tasks/TaskDetail.css";
import { useEffect, useState, useMemo, useRef } from "react";
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
const trimText = (text) => (text && text.length > 30 ? text.slice(0, 30) + "…" : text);

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

  const createdAtLabel = useMemo(() => formatRelativeDateTime(task?.createdAt), [task?.createdAt]);

  const dday = useMemo(() => {
    if (!task?.dueDate) return null;
    if (task?.status === "DONE" || task?.status === "CANCELED") return null;
    return ddayLabel(task.dueDate);
  }, [task?.dueDate, task?.status]);

  const attachments = useMemo(() => {
    const arr = task?.attachments || [];
    return Array.isArray(arr) ? arr : [];
  }, [task?.attachments]);

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
      await api.delete(`/api/tasks/${task.id}?reason=${encodeURIComponent(reason)}`);
      alert("업무가 삭제되었습니다.");
      nav("/tasks");
    } catch (error) {
      console.error(error);
      alert("삭제에 실패했습니다.");
    }
  };

  const visibleAuditLogs = showAllAuditLogs ? auditLogs : auditLogs.slice(0, 3);

  return (
    <div className="taskdetail">
      <div className="taskdetail__card taskdetail__card--header">
        <div className="taskdetail__topRow">
          <div>
            <div className="taskdetail__eyebrow">{createdAtLabel}</div>
            <h2 className="taskdetail__title">{task.title}</h2>
          </div>

          <div className="taskdetail__actions">
            <NavLink className="taskdetail__btn taskdetail__btn--ghost" to="/tasks">
              목록으로
            </NavLink>

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
          </div>
        </div>

        <div className="taskdetail__badges">
          <span className={`taskdetail__badge taskdetail__badge--${(task.status || "").toLowerCase()}`}>
            {task.status ?? "-"}
          </span>

          <span className="taskdetail__badge taskdetail__badge--visibility">{visibilityLabel(task.visibility)}</span>

          <span className={`taskdetail__badge taskdetail__badge--priority taskdetail__badge--priority-${priorityKey}`}>
            중요도: {task.priority ?? "-"}
          </span>

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
              <div className="taskdetail__metaVal">{task.createdByName ?? task.creatorName ?? "-"}</div>
            </div>
            <div className="taskdetail__metaRow">
              <div className="taskdetail__metaKey">담당자</div>
              <div className="taskdetail__metaVal">{task.assigneeName ?? "-"}</div>
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
          </div>

          {visibleAuditLogs.length > 0 &&(
          <h3 className="taskdetail__card taskdetail__card--title">Audit Log</h3>
          )}
          {visibleAuditLogs.map((log, idx) => (
            <div key={idx} className="taskdetail__card taskdetail__card--log">
              <div className={`taskdetail__logItem ${log.isRecent ? "taskdetail__logItem--recent" : ""}`}>
                <div className="taskdetail__metaRow">
                  <div className="taskdetail__metaKey">수정자</div>
                  <div className="taskdetail__metaVal">{log.actorName}</div>
                </div>

                <div className="taskdetail__metaRow">
                  <div className="taskdetail__metaKey">수정 시간</div>
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
                        title: "제목",
                        description: "내용",
                        assignee_id: "담당자",
                        priority: "우선순위",
                        visibility: "공개 범위",
                        due_date: "마감일",
                        status: "진행도",
                      };
                      label = fieldNameMap[field] ? `${fieldNameMap[field]} 변경` : "수정 필드";

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