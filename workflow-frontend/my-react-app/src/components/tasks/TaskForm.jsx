import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../../api/api";
import TaskEditor from "./TaskEditor";
import "../../css/tasks/TaskForm.css";

import { PRIORITIES, STATUSES } from "../../constants/taskOptions";
import AttachmentInput from "../attachments/AttachmentInput";
import { uploadTaskAttachments } from "../../api/attachmentsApi";

// TaskForm 컴포넌트
// - 업무 생성 / 수정 폼
// - 제목, 상태, 우선순위, 마감일, 담당자, 공개 범위, 내용, 첨부파일 포함
export default function TaskForm({ mode = "create" }) {

  const nav = useNavigate(); // 페이지 이동용

  // 기본 필드 state
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [status, setStatus] = useState("TODO");
  const [priority, setPriority] = useState("MEDIUM");

  const isEdit = mode === "edit";

  // 상태 옵션
  // - 생성 시 TODO만, 수정 시 전체 상태 허용
  const statusOptions = isEdit ? STATUSES : ["TODO"];

  // 기본 마감일: 오늘 +7일
  const defaultToday = () => {
    const day = new Date();
    day.setDate(day.getDate() + 7);
    return day.toISOString().split("T")[0];
  };

  // 날짜 제한
  const minDate = new Date().toISOString().split("T")[0]; // 오늘
  const maxDate = (() => {
    const day = new Date();
    day.setFullYear(day.getFullYear() + 10);
    return day.toISOString().split("T")[0];
  })();

  const [dueDate, setDueDate] = useState(defaultToday());

  // 공개 범위
  const [visibility, setVisibility] = useState("DEPARTMENT");

  // 담당자
  const [assigneeId, setAssigneeId] = useState("");
  const [users, setUsers] = useState([]);

  // 검증 에러
  const [errors, setErrors] = useState({});

  // 첨부파일 state (검증 통과한 파일만)
  const [attachFiles, setAttachFiles] = useState([]);

  // 담당자 목록 로딩 (마운트 시)
  useEffect(() => {
    api.get("/api/user/assigneelist")
      .then((res) => setUsers(res.data))
      .catch((e) => console.error("유저 목록 불러오기 실패", e));
  }, []);

  // 클라이언트 측 기본 검증
  const validateClient = () => {
    if (!title.trim()) return "제목을 입력해 주세요.";
    if (title.length > 200) return "제목은 200자까지만 작성 가능합니다.";
    if (!status) return "상태를 선택해 주세요.";
    if (!visibility) return "공개 범위를 선택해 주세요.";
    return null;
  };

  // 폼 제출
  const onSubmit = async (e) => {
    e.preventDefault();
    setErrors({});

    // 클라이언트 검증
    const msg = validateClient();
    if (msg) return alert(msg);

    // payload 구성
    const payload = {
      title: title.trim(),
      description: description || null,
      status,
      priority: priority || null,
      visibility,
      dueDate: dueDate || null,
      assigneeId: assigneeId ? Number(assigneeId) : null,
    };

    try {
      // 1) 업무 생성
      const res = await api.post("/api/tasks/create", payload);

      // 2) 첨부파일 업로드
      const taskId = res.data?.id;
      try {
        if (taskId && attachFiles.length > 0) {
          await uploadTaskAttachments(taskId, attachFiles);
        }
      } catch (e) {
        console.error("첨부 업로드 실패", e);
        alert(
          "업무는 저장됐지만 첨부 업로드에 실패했습니다. 상세에서 다시 올려주세요."
        );
      }

      // 생성 완료 후 목록 페이지 이동
      nav("/tasks");

    } catch (err) {
      const data = err.response?.data;
      if (data?.errors) {
        setErrors(data.errors);
        return alert(data.message || "입력값 확인!");
      }
      alert(data?.message || "저장 실패 (콘솔 확인)");
    }
  };

  return (
    <div className="taskform__stack">

      <form onSubmit={onSubmit} className="taskform">

        {/* 제목 */}
        <div className="taskform__section">
          <label className="taskform__label">제목</label>
          <input
            className="taskform__input"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="제목"
          />
          {errors.title && <div className="taskform__error">{errors.title}</div>}
        </div>

        {/* 상태 / 우선순위 / 마감일 / 담당자 / 공개 범위 */}
        <div className="taskform__row taskform__row--5">

          {/* 상태 */}
          <div className="taskform__section">
            <label className="taskform__label">상태</label>
            <select
              className="taskform__select"
              value={status}
              onChange={(e) => setStatus(e.target.value)}
            >
              {statusOptions.map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
            {errors.status && <div className="taskform__error">{errors.status}</div>}
          </div>

          {/* 우선순위 */}
          <div className="taskform__section">
            <label className="taskform__label">우선순위</label>
            <select
              className="taskform__select"
              value={priority}
              onChange={(e) => setPriority(e.target.value)}
            >
              {PRIORITIES.map((p) => (
                <option key={p} value={p}>{p}</option>
              ))}
            </select>
          </div>

          {/* 마감일 */}
          <div className="taskform__section">
            <label className="taskform__label">마감일</label>
            <input
              className="taskform__input"
              type="date"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
              min={minDate}
              max={maxDate}
            />
          </div>

          {/* 담당자 */}
          <div className="taskform__section">
            <label className="taskform__label">담당자</label>
            <select
              className="taskform__select"
              value={assigneeId}
              onChange={(e) => setAssigneeId(e.target.value)}
            >
              <option value="">미지정</option>
              {users.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.name} ({u.department})
                </option>
              ))}
            </select>
          </div>

          {/* 공개 범위 */}
          <div className="taskform__section">
            <label className="taskform__label">공개 범위</label>

            <div className="taskform__segment" role="tablist" aria-label="공개 범위">
              <button
                type="button"
                className={visibility === "PUBLIC" ? "is-active" : ""}
                onClick={() => setVisibility("PUBLIC")}
              >
                전사
              </button>
              <button
                type="button"
                className={visibility === "DEPARTMENT" ? "is-active" : ""}
                onClick={() => setVisibility("DEPARTMENT")}
              >
                부서
              </button>
              <button
                type="button"
                className={visibility === "PRIVATE" ? "is-active" : ""}
                onClick={() => setVisibility("PRIVATE")}
              >
                개인
              </button>
            </div>
          </div>

        </div>

        {/* 업무 내용 */}
        <div className="taskform__section">
          <label className="taskform__label">업무 내용</label>
          <div className="taskform__editor">
            <TaskEditor value={description} onChange={setDescription} />
          </div>
        </div>

        {/* 제출 / 취소 버튼 */}
        <div className="taskform__actions">
          <button type="submit" className="taskform__btn taskform__btn--primary">
            저장
          </button>
          <button type="button" className="taskform__btn" onClick={() => nav("/tasks")}>
            취소
          </button>
        </div>

      </form>

      {/* 첨부파일 입력 */}
      <AttachmentInput value={attachFiles} onChange={setAttachFiles} />

      {/* 선택된 파일 목록 표시 */}
      {attachFiles.length > 0 && (
        <div className="taskform taskform--attach">
          <div className="taskform__section">
            <div className="taskform__label">선택된 파일</div>
            <ul style={{ margin: 0, paddingLeft: 18 }}>
              {attachFiles.map((f) => (
                <li key={`${f.name}-${f.size}`}>{f.name}</li>
              ))}
            </ul>
          </div>
        </div>
      )}

    </div>
  );
}

// TaskForm 역할 요약
// - 업무 생성/수정 폼
// - 클라이언트 검증 포함
// - 첨부파일 업로드
// - TaskEditor 포함
// - 상태, 우선순위, 마감일, 담당자, 공개 범위 선택 가능
