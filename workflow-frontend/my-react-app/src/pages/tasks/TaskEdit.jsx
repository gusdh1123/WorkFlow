import { useEffect, useState } from "react";
import { useLocation, useParams } from "react-router-dom";
import { api } from "../../api/api"
import TaskForm from "../../components/tasks/TaskForm";
import { NavLink } from "react-router-dom";
import "../../css/tasks/TaskEdit.css";


export default function TaskEdit() {

  // 디테일 페이지에서 받은 정보 가져오기
  const { id } = useParams();
  const location = useLocation();

  const initialTask =
    location.state?.task?.id === Number(id)
      ? location.state.task
      : null;

  const [task, setTask] = useState(initialTask);

  useEffect(() => {

    // state로 받은 데이터 있으면 fetch안함
    // Fetch: 원격 서버 등 외부에서 데이터를 가져오거나(Fetch), 웹 브라우저에서 네트워크 요청을 비동기적으로 처리하기 위해 사용하는 자바스크립트 내장 API
    // 이전의 XMLHttpRequest를 대체하며, Promise 기반으로 동작하여 비동기 처리(Ajax)를 깔끔하게 작성할 수 있게 해줌.
    if (initialTask) return;

  const fetchTask = async () => {
    const res = await api.get(`/api/tasks/${id}`);
    setTask(res.data);
  };

  fetchTask();

}, [id, initialTask]);

  if(!task) return <div>불러오는 중...</div>;

  // TaskForm 컴포넌트를 edit 모드로 렌더링
  // - mode="edit" : 기존 Task 데이터를 불러와 수정 가능
  // - TaskForm 내부에서 API 호출, 검증, 제출 처리 모두 담당
return (
  <div className="taskcreate">
    {/* 상단 헤더: 제목 + 목록으로 이동 링크 */}
    <div className="taskcreate__header">
      <h2 className="taskcreate__title">업무 수정</h2>
      <NavLink to="/tasks" className="taskcreate__back">
        목록으로
      </NavLink>
    </div>

    <div className="taskcreate__content">
      <TaskForm mode="edit" initialData={task} />
  </div>
  </div>
);
}
