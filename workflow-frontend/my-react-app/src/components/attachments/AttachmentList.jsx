import "../../css/tasks/TaskDetail.css";
import { useMemo, useState } from "react";
import { formatBytes } from "../../utils/fileUtils";
import { downloadAttachment, deleteAttachment } from "../../api/attachmentsApi";

// 파일 타입에 따른 아이콘 반환 함수
// - 이미지, 문서, 압축, 텍스트 등 기본 매핑
// - 알 수 없는 확장자는 📎
const fileIcon = (name = "") => {
  const ext = name.split(".").pop()?.toLowerCase();

  if (!ext) return "📄";

  if (["png", "jpg", "jpeg", "webp", "gif"].includes(ext)) return "🖼️";
  if (["pdf"].includes(ext)) return "📕";
  if (["doc", "docx"].includes(ext)) return "📘";
  if (["xls", "xlsx", "csv"].includes(ext)) return "📗";
  if (["ppt", "pptx"].includes(ext)) return "📙";
  if (["zip"].includes(ext)) return "🗜️";
  if (["txt"].includes(ext)) return "📄";

  return "📎";
};

// AttachmentList 컴포넌트
// - Task 상세에서 첨부파일 표시
// - 다운로드, 삭제(soft delete) 기능 제공
// - attachments: AttachmentResponse 배열
// - onDeleted: 삭제 후 부모 콜백
export default function AttachmentList({
  attachments = [],
  onDeleted,
  readOnly = true, // 기본값 false
}) {
  // attachments가 배열이 아닐 경우 대비
  const list = useMemo(
    () => (Array.isArray(attachments) ? attachments : []),
    [attachments]
  );

  // 현재 삭제/처리 중인 attachmentId
  const [busyId, setBusyId] = useState(null);

  // 파일 다운로드
  const onDownload = (att) => {
    if (!att?.id) return;
    downloadAttachment(att.id, att.originalFilename);
  };

  // 파일 삭제(soft delete)
  const onDelete = async (att) => {
    if (!att?.id) return;

    // 삭제 확인
    const ok = window.confirm(
      `첨부파일을 삭제할까요?\n${att.originalFilename || ""}`
    );
    if (!ok) return;

    try {
      setBusyId(att.id); // 삭제 중 표시
      await deleteAttachment(att.id);
      onDeleted?.(att.id); // 삭제 후 부모 콜백
    } catch (e) {
      // HTTP 상태 코드별 에러 메시지
      const msg =
        e?.response?.status === 403
          ? "삭제 권한이 없습니다."
          : e?.response?.status === 404
          ? "이미 삭제되었거나 파일이 없습니다."
          : "첨부파일 삭제에 실패했습니다.";

      alert(msg);
    } finally {
      setBusyId(null); // 처리 완료
    }
  };

  return (
    <div className="taskdetail__attach">
      {/* 헤더: 제목 + 첨부 개수 */}
      <div className="taskdetail__attachHead">
        <div className="taskdetail__attachTitle">첨부 파일</div>
        <div className="taskdetail__attachCount">{list.length}개</div>
      </div>

      {/* 첨부 파일 없음 안내 */}
      {list.length === 0 ? (
        <div className="taskdetail__empty" style={{ marginTop: 10 }}>
          첨부파일이 없습니다.
        </div>
      ) : (
        <ul className="taskdetail__attachList">
          {list.map((att) => (
            <li key={att.id} className="taskdetail__attachItem">
              {/* 파일 메타 정보: 이름, 아이콘, 크기, MIME */}
              <div className="taskdetail__attachMeta">
                <div className="taskdetail__attachName">
                  <span className="taskdetail__attachIcon">
                    {fileIcon(att.originalFilename)}
                  </span>
                  {att.originalFilename || "file"}
                </div>

                <div className="taskdetail__attachSub">
                  {formatBytes(att.sizeBytes)}{" "}
                  {att.contentType ? `· ${att.contentType}` : ""}
                </div>
              </div>

              {/* 액션 버튼: 다운로드, 삭제 */}
              <div className="taskdetail__attachActions">
                <button
                  type="button"
                  className="taskdetail__btn taskdetail__btn--ghost"
                  onClick={() => onDownload(att)}
                >
                  다운로드
                </button>
            
            {!readOnly && (
                <button
                  type="button"
                  className="taskdetail__btn taskdetail__btn--danger"
                  disabled={busyId === att.id} // 삭제 중 비활성화
                  onClick={() => onDelete(att)}
                >
                  {busyId === att.id ? "삭제중..." : "삭제"}
                </button>
            )}
              </div>
            </li>
          ))}
        </ul>
      )}

      {/* 프론트 안내: soft delete */}
      <div className="taskform__help" style={{ marginTop: 10 }}>
        삭제된 항목은 휴지통에서 확인 및 복구할 수 있습니다.
      </div>
    </div>
  );
}

// AttachmentList 역할 요약
// - Task 상세에 첨부파일 리스트 표시
// - 다운로드, 삭제 기능 제공
// - 삭제는 soft delete 방식
// - 파일 타입별 아이콘 표시
