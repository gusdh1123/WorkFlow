import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api/api";
import TaskEditor from "./TaskEditor";
import "../css/TaskForm.css";

export default function TaskForm({mode="create"}) {
  const nav = useNavigate();

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");

  const [status, setStatus] = useState("TODO");
  const [priority, setPriority] = useState("MEDIUM");

  const PRIORITIES = ["LOW", "MEDIUM", "HIGH"];

  const isEdit = mode === "edit";
  const statusOptions = isEdit ? status : ["TODO"];
  const ALL_STATUS = [
  "TODO",
  "IN_PROGRESS",
  "REVIEW",
  "DONE",
  "ON_HOLD",
  "CANCELED"
  ];

  // 오늘 기준 +7일 (기본 마감일)
  const defaultToday = () => {
    const day = new Date();
    day.setDate(day.getDate() + 7);
    return day.toISOString().split("T")[0];
  };

  // minDate: 오늘
  const minDate = (() => new Date().toISOString().split("T")[0])();

  // maxDate: 10년 뒤
  const maxDate = (() => {
    const day = new Date();
    day.setFullYear(day.getFullYear() + 10);
    return day.toISOString().split("T")[0];
  })();

  const [dueDate, setDueDate] = useState(defaultToday());

  const [visibility, setVisibility] = useState("DEPARTMENT");

  const [assigneeId, setAssigneeId] = useState("");
  const [users, setUsers] = useState([]);

  const [errors, setErrors] = useState({});

  useEffect(() => {
    api.get("/api/user/assigneelist")
      .then((res) => setUsers(res.data))
      .catch((e) => console.error("유저 목록 불러오기 실패", e));
  }, []);

  const validateClient = () => {
    if (!title.trim()) return "제목을 입력해 주세요.";
    if (title.length > 200) return "제목은 200자까지만 작성 가능합니다.";
    if (!status) return "상태를 선택해 주세요.";
    if (!visibility) return "공개 범위를 선택해 주세요.";
    return null;
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setErrors({});

    const msg = validateClient();
    if (msg) return alert(msg);

    const payload = {
      title: title.trim(),
      description: description || null,
      status: statusOptions,
      priority: priority ? priority : null,
      visibility,
      dueDate: dueDate ? dueDate : null,
      assigneeId: assigneeId ? Number(assigneeId) : null,
    };

    try {
      await api.post("/api/tasks/create", payload);
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
    <form onSubmit={onSubmit} className="taskform">
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

      {/* 상태/우선순위/마감일/담당자/공개범위 */}
      <div className="taskform__row taskform__row--5">
        <div className="taskform__section">
          <label className="taskform__label">상태</label>
          <select
            className="taskform__select"
            value={status}
            onChange={(e) => setStatus(e.target.value)}
          >
            {statusOptions.map((s) => (
              <option key={s} value={s}>
              {s}
              </option>
            ))}
          </select>
          {errors.status && <div className="taskform__error">{errors.status}</div>}
        </div>

        <div className="taskform__section">
          <label className="taskform__label">우선순위</label>
          <select
            className="taskform__select"
            value={priority}
            onChange={(e) => setPriority(e.target.value)}
          >
            {PRIORITIES.map((p) =>(
              <option key={p} value={p}>
                {p}
              </option>
            ))}
          </select>
        </div>

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

        {/* 공개 범위: 세그먼트 버튼 */}
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

      <div className="taskform__section">
        <label className="taskform__label">업무 내용</label>
        <div className="taskform__editor">
          <TaskEditor value={description} onChange={setDescription} />
        </div>
      </div>

      <div className="taskform__actions">
        <button type="submit" className="taskform__btn taskform__btn--primary">
          저장
        </button>
        <button type="button" className="taskform__btn" onClick={() => nav("/tasks")}>
          취소
        </button>
      </div>
    </form>
  );
}
