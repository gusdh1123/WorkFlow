import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api/api";
import TaskEditor from "./TaskEditor";
import "../css/TaskForm.css";

export default function TaskForm() {
  const nav = useNavigate();

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");

  const [status, setStatus] = useState("TODO");
  const [priority, setPriority] = useState("");
  const [dueDate, setDueDate] = useState("");

  const [assigneeId, setAssigneeId] = useState(""); 
  const [users, setUsers] = useState([]);

  const [errors, setErrors] = useState({});

  useEffect(() => {
    api.get("/api/user/assigneelist")
      .then((res) => setUsers(res.data))
      .catch((e) => console.error("유저 목록 불러오기 실패", e));
  }, []);

  const validateClient = () => {
    if (!title.trim()) return "제목을 입력해줘";
    if (title.length > 200) return "제목은 200자까지 가능해";
    if (!status) return "상태를 선택해줘";
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
      status,
      priority: priority ? priority : null,
      dueDate: dueDate ? dueDate : null,
      assigneeId: assigneeId ? Number(assigneeId) : null,
    };

    try {
      await api.post("/api/tasks", payload);
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

      <div className="taskform__row taskform__row--4">
        <div className="taskform__section">
          <label className="taskform__label">상태</label>
          <select
            className="taskform__select"
            value={status}
            onChange={(e) => setStatus(e.target.value)}
          >
            <option value="TODO">TODO</option>
            <option value="IN_PROGRESS">IN_PROGRESS</option>
            <option value="REVIEW">REVIEW</option>
            <option value="DONE">DONE</option>
            <option value="ON_HOLD">ON_HOLD</option>
            <option value="CANCELED">CANCELED</option>
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
            <option value="">선택</option>
            <option value="LOW">LOW</option>
            <option value="MEDIUM">MEDIUM</option>
            <option value="HIGH">HIGH</option>
          </select>
        </div>

        <div className="taskform__section">
          <label className="taskform__label">마감일</label>
          <input
            className="taskform__input"
            type="date"
            value={dueDate}
            onChange={(e) => setDueDate(e.target.value)}
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