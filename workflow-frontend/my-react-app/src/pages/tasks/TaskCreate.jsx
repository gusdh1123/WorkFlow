import { NavLink } from "react-router-dom";
import TaskForm from "./TaskForm";
import "../css/TaskCreate.css";

export default function TaskCreate() {
  return (
    <div className="taskcreate">
      <div className="taskcreate__header">
        <NavLink to="/tasks" className="taskcreate__back">목록으로</NavLink>
        <h2 className="taskcreate__title">업무 작성</h2>
      </div>

      <div className="taskcreate__content">
        <TaskForm />
      </div>
    </div>
  );
}
