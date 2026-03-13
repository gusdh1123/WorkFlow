import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../../api/api";
import TaskEditor from "./TaskEditor";
import "../../css/tasks/TaskForm.css";

import { PRIORITIES, STATUSES } from "../../constants/taskOptions";
import AttachmentInput from "../attachments/AttachmentInput";
import { uploadTaskAttachments } from "../../api/attachmentsApi"; // 첨부파일 업로드 함수
import { useAuth } from "../../auth/hooks/useAuth";
import { userFromToken } from "../../auth/utils/userFromToken.js";

// TaskForm 컴포넌트
// - 업무 생성 / 수정 폼
// - 제목, 상태, 우선순위, 마감일, 담당자, 공개 범위, 내용, 첨부파일 포함
// - initialData를 받아서 Edit 모드에서 state 초기화에 사용
export default function TaskForm({ mode = "create", initialData }) {
  const nav = useNavigate(); // 페이지 이동용
  const isEdit = mode === "edit"; // edit 모드 여부

  const { accessToken } = useAuth();
  const loginUser = accessToken ? userFromToken(accessToken) : null;

  // 상수 / 함수 정의
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

  // 상태(state) 정의
  const [title, setTitle] = useState(initialData?.title || ""); // 제목
  const [description, setDescription] = useState(initialData?.description || ""); // 글 내용
  const [status, setStatus] = useState(initialData?.status || "TODO"); // 상태
  const [priority, setPriority] = useState(initialData?.priority || "MEDIUM"); // 중요도
  const [dueDate, setDueDate] = useState(initialData?.dueDate || defaultToday()); // 날짜
  const [visibility, setVisibility] = useState(initialData?.visibility || "DEPARTMENT"); // 부서
  const [assigneeId, setAssigneeId] = useState(initialData?.assigneeId?.toString() || ""); // 담당자
  const [reason, setReason] = useState(""); // 수정 사유
  const [users, setUsers] = useState([]); // 담당자 목록
  const [errors, setErrors] = useState({}); // 검증 에러

  // 첨부파일 초기값 처리
  // - Edit 모드이면 서버 첨부파일을 AttachmentInput/AttachmentList가 기대하는 형태로 변환
  const [attachFiles, setAttachFiles] = useState(
    isEdit && initialData?.attachments
      ? initialData.attachments.map(att => ({
          id: att.id, // 서버 첨부파일 ID
          name: att.originalFilename, // 표시할 파일명
          size: att.sizeBytes || 0, // 서버에서 size 없으면 0
          file: null, // 실제 File 객체는 없음
          url: att.url || null, // 필요 시 미리보기용
          isExisting: true, // 기존 서버 파일 표시용
        }))
      : []
  );

  // 삭제 예정 파일 관리
  const [deletedFiles, setDeletedFiles] = useState([]); // 삭제 예정 파일 리스트, 저장 시 서버 반영

  // 로그 기록용 삭제 ID
  const [deletedAttachmentIds, setDeletedAttachmentIds] = useState([]);

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
    if (isEdit && !reason.trim()) return "수정 사유를 입력하세요.";
    return null;
  };

  // 첨부파일 삭제 핸들러 (저장 시 반영)
  const handleAttachmentDelete = (att) => {
    if (!att?.id || !att.isExisting) {
      // 새로 선택한 파일이면 프론트에서만 제거
      setAttachFiles(prev => prev.filter(f => f !== att));
      return;
    }

    // 기존 파일은 삭제 예정 목록에 추가
    setDeletedFiles(prev => [...prev, att]); // 삭제 예정 상태에 저장

    // 로그 기록용
    setDeletedAttachmentIds(prev => [...prev, att.id]);

    setAttachFiles(prev => prev.filter(f => f.id !== att.id)); // UI에서 제거
  };

  // 상태 옵션 계산 (권한/상태 전이)
  const getStatusOptions = () => {
    if (!initialData) return ["TODO"];

    const userRole = loginUser?.role;
    const isCreatorOrAssignee =
      Number(initialData.createdById) === Number(loginUser?.id) ||
      (initialData.assigneeId && Number(initialData.assigneeId) === Number(loginUser?.id));

    const currentStatus = initialData.status;

    // 관리자 / CEO
    if (userRole === "ADMIN" || userRole === "CEO") {
      return STATUSES; // 전부 가능
    }

    // 부서장 (본인 부서 업무)
    if (
      userRole === "MANAGER" &&
      loginUser?.department?.toLowerCase() === initialData.workDepartmentName?.toLowerCase()
    ) {
      return STATUSES; // 모든 상태 가능
    }

    // 작성자 / 담당자
    if (isCreatorOrAssignee) {
      switch (currentStatus) {
        case "TODO":
          return ["TODO", "IN_PROGRESS", "ON_HOLD"];
        case "IN_PROGRESS":
          return ["IN_PROGRESS", "TODO", "ON_HOLD", "REVIEW"];
        case "REVIEW":
          return ["REVIEW", "IN_PROGRESS"];
        case "ON_HOLD":
          return ["ON_HOLD", "TODO", "IN_PROGRESS"];
        default:
          return [currentStatus]; // DONE, CANCELED 등은 변경 불가
      }
    }

    // 나머지는 상태 변경 불가
    return [currentStatus];
  };

  // 폼 제출 핸들러
  const onSubmit = async (e) => {
    e.preventDefault();
    setErrors({});

    // 클라이언트 검증
    const msg = validateClient();
    if (msg) return alert(msg);


    console.log(initialData);
    // payload 구성
    const payload = {
      title: title.trim(),
      description: description || null,
      status,
      priority: priority || null,
      visibility,
      dueDate: dueDate || null,
      assigneeId: assigneeId ? Number(assigneeId) : null,
      reason: isEdit ? reason.trim() : null, // 수정 사유 포함
      version: initialData?.version, // 버전 추가 낙관적 락
    };

    try {
      let res;
      let taskId = initialData?.id;

      if (isEdit && taskId) {
        // 새로 추가된 첨부 아이디 담기
        let addedAttachmentIds = []; // 추가 첨부 ID

        const filesToUpload = attachFiles.filter(f => f.file instanceof File);

        if (filesToUpload.length > 0) {
          // 첨부 업로드 (수정 모드에서 단일 처리)
          const uploaded = await uploadTaskAttachments(taskId, filesToUpload);

          // attachFiles 상태 업데이트 (업로드 완료 후 서버 ID 반영)
          setAttachFiles(prev => [
            ...prev.filter(f => f.isExisting),
            ...uploaded.map(u => ({
              id: u.id,
              name: u.originalFilename,
              size: u.sizeBytes,
              file: null,
              url: u.url,
              isExisting: true,
            })),
          ]);

          addedAttachmentIds = uploaded.map(a => a.id);
        }

        // 수정 모드 PUT 요청 (업로드 + 삭제 포함)
        res = await api.put(`/api/tasks/${taskId}`, {
          ...payload,
          addedAttachmentIds, // 추가 첨부 아이디
          deletedAttachmentIds, // 삭제 첨부 아이디
        });

      } else {
        // 생성 모드: POST 요청
        res = await api.post("/api/tasks/create", payload);
        taskId = res.data?.id;

        // 생성 모드에서 새 첨부파일 업로드
        const filesToUpload = attachFiles.filter(f => f.file instanceof File);
        if (taskId && filesToUpload.length > 0) {
          await uploadTaskAttachments(taskId, filesToUpload);
        }
      }

      // 삭제 예정 파일 서버 반영
      if (taskId && deletedFiles.length > 0) {
        await api.post(`/api/tasks/${taskId}/attachments/delete`, {
          ids: deletedFiles.map(f => f.id),
        });
      }

      // 완료 후 목록 페이지 이동
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

  // 직급 한글로 변환
  const roleMap = {
    ADMIN: "관리자",
    USER: "사원",
    MANAGER: "팀장",
  };

  const departmentNameMap = {
    Operations: "운영팀",
    Development: "개발팀",
    Design: "디자인팀",
  };

  // 담당자 목록 필터링 (권한 기반)
  const getAssignableUsers = () => {
    if (!loginUser) return [];

    // 관리자면 전사 모두
    if (loginUser.role === "ADMIN") return users;

    // 팀장 / 일반 유저는 자기 팀 내 사람만
    return users.filter(u => u.department === loginUser.department);
  };

  // JSX 렌더링
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
              disabled={!isEdit} // 편집 모드만 활성화
            >
              {getStatusOptions().map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
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
                <option key={p} value={p}>
                  {p}
                </option>
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
              {getAssignableUsers().map((u) => (
                <option key={u.id} value={u.id}>
                  {u.name} {roleMap[u.role] || ""} ({departmentNameMap[u.department] || ""})
                </option>
              ))}
            </select>
          </div>

          {/* 공개 범위 */}
          <div className="taskform__section">
            <label className="taskform__label">공개 범위</label>
            <div className="taskform__segment" role="tablist" aria-label="공개 범위">
              {/* 전사 버튼: 일반 유저는 선택 불가 */}
              {loginUser.role !== "USER" && (
              <button
                type="button"
                className={visibility === "PUBLIC" ? "is-active" : ""}
                onClick={() => setVisibility("PUBLIC")}
              >
                전사
              </button>
              )}

              {/* 부서 버튼 */}
              <button
                type="button"
                className={visibility === "DEPARTMENT" ? "is-active" : ""}
                onClick={() => setVisibility("DEPARTMENT")}
              >
                부서
              </button>
              
              {/* 개인 버튼 */}
              <button
                type="button"
                className={visibility === "PRIVATE" ? "is-active" : ""}
                onClick={() => setVisibility("PRIVATE")}
              >
                개인
              </button>
            </div>
          </div>

          {isEdit && (
            <div className="taskform__section taskform__section--reason">
              <label className="taskform__label">수정 사유</label>
              <textarea
                className="taskform__input"
                rows={2}
                maxLength={50}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="
수정 사유를 입력하세요.(최대 50자)"
              />
            </div>
          )}
        </div>

        {/* 업무 내용 */}
        <div className="taskform__section">
          <label className="taskform__label">업무 내용</label>
          <div className="taskform__editor">
            <TaskEditor value={description} onChange={setDescription} mode={mode} /* 모드 전달 */ />
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
      <AttachmentInput value={attachFiles} onChange={setAttachFiles} onDelete={handleAttachmentDelete} />
    </div>
  );
}