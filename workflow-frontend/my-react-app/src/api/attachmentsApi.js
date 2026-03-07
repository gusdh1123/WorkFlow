import { api } from "./api";

// 첨부파일 업로드 (taskId에 귀속)
// - value에서 file.file 객체만 서버로 FormData 전송
export async function uploadTaskAttachments(taskId, value) {
  const formData = new FormData();

  // 새로 선택된 파일만 append
  const newFiles = (value || []).filter(f => !f.isExisting);
  newFiles.forEach(f => formData.append("files", f.file)); // 반드시 File 객체

  if (newFiles.length === 0) return []; // 새 파일 없으면 업로드 스킵

  const res = await api.post(`/api/tasks/${taskId}/attachments`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });

  return res.data; // 서버에서 AttachmentResponse 배열 반환
}

// 첨부 삭제(soft delete)
// - 실제 삭제 X, isDeleted=true 처리
export async function deleteAttachment(attachmentId) {
  return api.delete(`/api/attachments/${attachmentId}`);
}

// Content-Disposition에서 filename 파싱 (filename* 우선)
// - 다운로드 시 브라우저가 인식할 파일명을 헤더에서 추출
function parseFilenameFromContentDisposition(cd) {
  if (!cd) return null;

  // filename*=UTF-8''... 형식 처리 (RFC 5987)
  const mStar = cd.match(/filename\*\s*=\s*UTF-8''([^;]+)/i);
  if (mStar?.[1]) {
    try {
      return decodeURIComponent(mStar[1].trim().replace(/(^"|"$)/g, "")); // URL 디코딩
    } catch (e) {
      console.log(e);
    }
  }

  // filename="..." 형식 처리
  const m = cd.match(/filename\s*=\s*"?([^"]+)"?/i);
  if (m?.[1]) return m[1].trim();

  return null; // 파일명 못 찾음
}

// 첨부 다운로드 (원본명 강제 + 헤더 기반 보험)
// - blob으로 받아서 a 태그를 사용해 다운로드 처리
// - 헤더에 filename 없으면 originalFilename 사용
export async function downloadAttachment(attachmentId, originalFilename) {
  const res = await api.get(`/api/attachments/${attachmentId}/download`, {
    responseType: "blob", // 바이너리 데이터 수신
  });

  const blob = new Blob([res.data], {
    type: res.headers["content-type"] || "application/octet-stream", // MIME 타입 지정
  });

  // Content-Disposition 헤더에서 파일명 추출
  const cd = res.headers["content-disposition"];
  const headerName = parseFilenameFromContentDisposition(cd);

  // 화면에서 넘긴 원본명
  const finalName = headerName || originalFilename || "file";

  // Blob → ObjectURL 생성
  const url = window.URL.createObjectURL(blob);

  // a 태그 생성 후 클릭으로 다운로드
  const a = document.createElement("a");
  a.href = url;
  a.download = finalName; // 다운로드 파일명 지정
  document.body.appendChild(a);
  a.click();
  a.remove();

  // 메모리 해제
  window.URL.revokeObjectURL(url);
}
