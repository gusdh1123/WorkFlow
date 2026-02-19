import "../../css/tasks/TaskDetail.css";
import { useEffect, useState, useMemo, useRef } from "react";
import { useParams, useNavigate, NavLink } from "react-router-dom";
import { api } from "../../api/api.js";

import { visibilityLabel } from "../../utils/taskUtils";
import { formatRelativeDateTime, ddayLabel } from "../../utils/dateUtils";

import ImageModal from "../../components/common/ImageModal";

// 첨부 모듈 분리
import AttachmentList from "../../components/attachments/AttachmentList";

export default function TaskDetail() {

  const { id } = useParams();
  const nav = useNavigate();

  const [task, setTask] = useState(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  // 모달 기능
  const descRef = useRef(null);

  const [imgModal, setImgModal] = useState({
    open: false,
    src: "",
    alt: "",
  });

  const closeModal = () => {
    setImgModal({ open: false, src: "", alt: "" });
  };

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

  // useEffect(() => {...},[id]);
  // 최초 렌더링 시 1번 실행
  // id 변경 시마다 실행
  // 실행 전에 이전 effect cleanup 먼저 실행
  // 마지막에 호출안하면 실행안됨(useEffect은 타이밍 트리거 일뿐)
  // 여기선 Task API 호출에 사용하며 AbortController로 요청 취소 가능
  useEffect(() => {
    const controller = new AbortController();

    (async () => {
      setLoading(true);
      setErr("");

      try {
        const res = await api.get(`/api/tasks/${id}`, {
          signal: controller.signal, // 스위치 연결
        });
        setTask(res.data);
      } catch (e) {
        if (e.name === "CanceledError" || e.code === "ERR_CANCELED") return;

        // 상태코드별 사용자 친화적 에러 메시지
        const msg =
          e?.response?.status === 404 ? "업무를 찾을 수 없습니다."
            : e?.response?.status === 401 ? "로그인이 필요합니다."
              : "조회에 실패했습니다.";

        setErr(msg);
      } finally {
        setLoading(false);
      }
    })();

    // 페이지 이동/언마운트 시 요청 취소
    return () => controller.abort();
  }, [id]);

  // 작성일 라벨
  const createdAtLabel = useMemo(
    () => formatRelativeDateTime(task?.createdAt),
    [task?.createdAt]
  );

  // DONE 상태면 D-Day 라벨 숨김
  const dday = useMemo(() => {
    if (!task?.dueDate) return null;

    // 완료/취소 업무는 마감 강조 불필요
    if (task?.status === "DONE" || task?.status === "CANCELED") return null;

    return ddayLabel(task.dueDate);
  }, [task?.dueDate, task?.status]);

  // 첨부 목록 (백엔드가 task.attachments로 내려준다는 전제)
  const attachments = useMemo(() => {
    const arr = task?.attachments || [];
    return Array.isArray(arr) ? arr : [];
  }, [task?.attachments]);

  // 첨부 삭제 후 화면 즉시 반영
  const onAttachmentDeleted = (deletedId) => {
    setTask((prev) => {
      if (!prev) return prev;
      const next = { ...prev };
      const list = Array.isArray(next.attachments) ? next.attachments : [];
      next.attachments = list.filter((x) => x.id !== deletedId);
      return next;
    });
  };

  if (loading)
    return <div className="taskdetail__state">불러오는 중...</div>;

  if (err)
    return (
      <div className="taskdetail__state">
        <div className="taskdetail__error">{err}</div>
        <button
          className="taskdetail__btn taskdetail__btn--ghost"
          onClick={() => nav(-1)}
        >
          뒤로가기
        </button>
      </div>
    );

  if (!task) return null;

  const priorityKey = (task?.priority || "").toLowerCase();

  return (
    <div className="taskdetail">

      {/* 헤더 카드 */}
      <div className="taskdetail__card taskdetail__card--header">

        <div className="taskdetail__topRow">

          <div>
            <div className="taskdetail__eyebrow">
              {createdAtLabel}
            </div>

            <h2 className="taskdetail__title">
              {task.title}
            </h2>
          </div>

          <div className="taskdetail__actions">
            <NavLink
              className="taskdetail__btn taskdetail__btn--ghost"
              to="/tasks"
            >
              목록으로
            </NavLink>

            <NavLink
              className="taskdetail__btn"
              to={`/tasks/${id}/edit`}
            >
              수정
            </NavLink>

            <button
              type="button"
              className="taskdetail__btn taskdetail__btn--danger"
            >
              삭제
            </button>
          </div>

        </div>

        {/* 상태, 범위, 중요도, 마감, D-Day */}
        <div className="taskdetail__badges">

          <span className={`taskdetail__badge taskdetail__badge--${(task.status || "").toLowerCase()}`}>
            {task.status ?? "-"}
          </span>

          <span className="taskdetail__badge taskdetail__badge--visibility">
            {visibilityLabel(task.visibility)}
          </span>

          <span className={`taskdetail__badge taskdetail__badge--priority taskdetail__badge--priority-${priorityKey}`}>
            중요도: {task.priority ?? "-"}
          </span>

          {task.dueDate && (
            <span className="taskdetail__badge taskdetail__badge--due">
              마감: {task.dueDate}
            </span>
          )}

          {/* DONE이면 자동으로 안 뜸 */}
          {dday && (
            <span className="taskdetail__badge taskdetail__badge--dday">
              {dday}
            </span>
          )}

        </div>

      </div>

      {/* 내용 카드 */}
      <div className="taskdetail__grid">

        {/* 왼쪽: 설명 + 첨부 */}
        <div className="taskdetail__card">

          {task.description ? (
            <div
              ref={descRef}
              className="taskdetail__desc"
              dangerouslySetInnerHTML={{ __html: task.description }}
            />
          ) : (
            <div className="taskdetail__empty">설명이 없습니다.</div>
          )}

          {/* 첨부파일 (모듈 분리 컴포넌트) */}
          <AttachmentList
            attachments={attachments}
            onDeleted={onAttachmentDeleted}
          />

        </div>

        {/* 모달 (공용 컴포넌트) */}
        <ImageModal
          open={imgModal.open}
          src={imgModal.src}
          alt={imgModal.alt}
          onClose={closeModal}
        />

        {/* 오른쪽: 정보 */}
        <div className="taskdetail__card taskdetail__card--meta">

          <div className="taskdetail__metaRow">
            <div className="taskdetail__metaKey">작성자</div>
            <div className="taskdetail__metaVal">
              {task.createdByName ?? task.creatorName ?? "-"}
            </div>
          </div>

          <div className="taskdetail__metaRow">
            <div className="taskdetail__metaKey">담당자</div>
            <div className="taskdetail__metaVal">
              {task.assigneeName ?? "-"}
            </div>
          </div>

          <div className="taskdetail__metaRow">
            <div className="taskdetail__metaKey">마감일</div>
            <div className="taskdetail__metaVal">
              {task.dueDate ?? "-"} {dday && `(${dday})`}
            </div>
          </div>

          <div className="taskdetail__metaRow">
            <div className="taskdetail__metaKey">작성일</div>
            <div className="taskdetail__metaVal">
              {createdAtLabel}
            </div>
          </div>

        </div>

      </div>

    </div>
  );
}