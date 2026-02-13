import "../css/TaskDetail.css";
import { useEffect, useState, useMemo } from "react";
import { useParams, useNavigate, NavLink } from "react-router-dom";
import { api } from "../api/api";
import Tasks from "./Tasks";
import TaskEdit from "./TaskEdit";

// 공개 범위 라벨 변환
const visibilityLabel = (v) => {
  if (v === "PUBLIC") return "전사";
  if (v === "DEPARTMENT") return "부서";
  if (v === "PRIVATE") return "개인";
  return v ?? "-";
};

// 리스트에서 쓰던 오늘/어제 시간 표기 (디테일에서도 재사용)
const formatRelativeDateTime = (iso) => {

  // 값 없으면 "-"
  if (!iso) return "-";

  // ISO → Date 변환
  const d = new Date(iso);

  // Date 변환 실패 시 원본 반환
  if (Number.isNaN(d.getTime())) return iso;

  const now = new Date();

  // 오늘 0시
  const startOfToday = new Date(
    now.getFullYear(),
    now.getMonth(),
    now.getDate()
  );

  // 해당 날짜 0시
  const startOfThat = new Date(
    d.getFullYear(),
    d.getMonth(),
    d.getDate()
  );

  // 날짜 차이 계산
  const diffDays = Math.round(
    (startOfToday - startOfThat) / (1000 * 60 * 60 * 24)
  );

  const hh = String(d.getHours()).padStart(2, "0");
  const mm = String(d.getMinutes()).padStart(2, "0");

  if (diffDays === 0) return `오늘 ${hh}:${mm}`;
  if (diffDays === 1) return `어제 ${hh}:${mm}`;

  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, "0")}.${String(d.getDate()).padStart(2, "0")} ${hh}:${mm}`;
};

// "YYYY-MM-DD" (LocalDate) 전용 파서
// 로컬 기준 자정으로 처리
const parseLocalDate = (s) => {

  if (!s) return null;

  const d = new Date(`${s}T00:00:00`);

  // 잘못된 날짜면 null
  return Number.isNaN(d.getTime()) ? null : d;
};

// 오늘 0시 생성 함수
const startOfToday = () => {

  const now = new Date();

  return new Date(
    now.getFullYear(),
    now.getMonth(),
    now.getDate()
  );
};

// 오늘 기준 D-Day 계산
const daysDiffFromToday = (dueDateStr) => {

  const due = parseLocalDate(dueDateStr);

  if (!due) return null;

  const today = startOfToday();

  const ms = due.getTime() - today.getTime();

  // ms → 일수 변환
  return Math.round(ms / (1000 * 60 * 60 * 24));
};

// D-3 / D-DAY / OVERDUE 2d
const ddayLabel = (dueDateStr) => {

  const diff = daysDiffFromToday(dueDateStr);

  if (diff === null) return null;

  // 7일 넘게 남은 건 표시 안 함
  if (diff > 7) return null;

  if (diff === 0) return "D-DAY";

  if (diff > 0) return `D-${diff}`;

  // 마감 초과
  return `OVERDUE ${Math.abs(diff)}d`;
};

export default function TaskDetail() {

  const { id } = useParams();
  const nav = useNavigate();

  const [task, setTask] = useState(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

   // useEffect(() => {...},[id]);
    // 최초 렌더링 시 1번 실행
    // id 변경 시마다 실행
    // 실행 전에 이전 effect cleanup 먼저 실행
    // 마지막에 호출안하면 실행안됨(useEffect은 타이밍 트리거 일뿐)
    useEffect(() => {
        // 비동기 작업 취소하는 스위치
        // 작업중인데 페이지 넘어갈려고 하거나 할때 취소해서 방지해줌.
        const controller = new AbortController();

        (async () =>{
            // 로딩 중..
            // 로딩 중에는 클릭이 안됨. 
            // 이것 저것 방지하기 위해 사용
            // 안에 값 바끼면 자동으로 리액트가 리렌더
            setLoading(true);
            setErr("");
            try{
                const res = await api.get(`/api/tasks/${id}`,{
                    signal: controller.signal, // 스위치 연결
                }); // 여기서 값을 가져오고 res로 받아서..
                setTask(res.data); // 여기서 값을 꺼내서 미리 useState로 선언해둔 const [task, setTask] = useState(null); 공간에 값을 넣는 것임. res.data
            }catch (e) {
                // AbortController가 취소하면 그냥 무시(에러로 잡히니깐.)
                if(e.name === 'CanceledError' || e.code === "ERR_CANCELED") return;

                const msg = 
                e?.response?.status === 404 ? "업무를 찾을 수 없습니다." : e?.response?.status === 401 ? "로그인이 필요합니다." : "조회에 실패했습니다.";
                setErr(msg);
            } finally {
                // 로딩 끝..
                setLoading(false);
            }
        })(); // ()이게 호출하는 것임.

        // 마무리 함수 실행(return () => ), 페이지 이동/언마운트 시 요청 취소(controller.abort())
        return () => controller.abort();

    },[id]);

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

        <div className="taskdetail__card">

          <div className="taskdetail__sectionTitle">
            설명
          </div>

          {task.description ? (
            <div
              className="taskdetail__desc"
              dangerouslySetInnerHTML={{ __html: task.description }}
            />
          ) : (
            <div className="taskdetail__empty">
              설명이 없습니다.
            </div>
          )}

        </div>

      </div>

    </div>
  );
}
