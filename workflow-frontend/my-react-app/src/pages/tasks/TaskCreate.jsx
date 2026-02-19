import { NavLink } from "react-router-dom";
import TaskForm from "../../components/tasks/TaskForm";
import "../../css/tasks/TaskCreate.css";

export default function TaskCreate() {
  return (
    <div className="taskcreate">
      {/* 상단 헤더: 제목 + 목록으로 이동 링크 */}
      <div className="taskcreate__header">
        <h2 className="taskcreate__title">업무 작성</h2>
        <NavLink to="/tasks" className="taskcreate__back">
          목록으로
        </NavLink>
      </div>

      {/* 본문: TaskForm 컴포넌트 */}
      <div className="taskcreate__content">
        {/* mode="create" => 생성 모드 */}
        <TaskForm mode="create" />
      </div>
    </div>
  );
}
