import {
  EDITOR_IMAGE_MAX_BYTES,
  ATTACH_MAX_FILE_BYTES,
  ATTACH_MAX_TOTAL_BYTES,
  ATTACH_MAX_FILES,
  ATTACH_ALLOWED_EXTS,
} from "../constants/uploadLimits";

// 바이트 단위 -> 보기 좋은 문자열 변환
// 예: 1024 -> "1.0KB", 1536 -> "1.5KB"
export const formatBytes = (bytes) => {
  if (!Number.isFinite(bytes)) return "-"; // 숫자가 아니면 "-"
  const units = ["B", "KB", "MB", "GB"];
  let n = bytes;
  let i = 0;
  while (n >= 1024 && i < units.length - 1) {
    n /= 1024;
    i += 1;
  }
  return `${n.toFixed(i === 0 ? 0 : 1)}${units[i]}`; // B는 소수점 없음, KB 이상은 1자리
};

// 파일 관련 유틸

// 파일 확장자 추출
// file: File 객체
// 반환: 소문자 확장자 (예: "jpg"), 확장자가 없으면 ""
export const getFileExt = (file) => {
  const name = file?.name || "";
  const idx = name.lastIndexOf(".");
  if (idx < 0) return "";
  return name.slice(idx + 1).toLowerCase();
};

// 이미지 여부 체크
// file.type이 "image/"로 시작하면 true
export const isImageFile = (file) => !!file?.type?.startsWith("image/");

// 에디터 이미지 검증
// file: 업로드할 파일
// maxBytes: 최대 허용 용량 (기본 EDITOR_IMAGE_MAX_BYTES)
// 반환: { ok: true } | { ok: false, message: "에러 메시지" }
export const validateEditorImage = (file, maxBytes = EDITOR_IMAGE_MAX_BYTES) => {
  if (!file) return { ok: false, message: "파일이 없습니다." };
  if (!isImageFile(file)) return { ok: false, message: "이미지 파일만 업로드 가능합니다." };
  if (file.size > maxBytes)
    return { ok: false, message: `이미지는 ${formatBytes(maxBytes)} 이하만 업로드 가능합니다.` };
  return { ok: true };
};

// 첨부파일 검증 (여러 파일)
// files: FileList 또는 File 배열
// opts:
//   maxFiles: 최대 파일 개수
//   maxFileBytes: 개별 파일 최대 용량
//   maxTotalBytes: 전체 합 최대 용량
//   allowedExts: 허용 확장자 배열
// 반환: { ok: true, files: [...] } | { ok: false, message: "에러 메시지" }
export const validateAttachments = (files, opts = {}) => {
  const {
    maxFiles = ATTACH_MAX_FILES,
    maxFileBytes = ATTACH_MAX_FILE_BYTES,
    maxTotalBytes = ATTACH_MAX_TOTAL_BYTES,
    allowedExts = ATTACH_ALLOWED_EXTS,
  } = opts;

  const arr = Array.from(files || []);
  if (arr.length === 0) return { ok: true, files: [] }; // 첨부 파일 없으면 OK
  if (arr.length > maxFiles)
    return { ok: false, message: `첨부파일은 최대 ${maxFiles}개까지 가능합니다.` };

  let total = 0;
  for (const f of arr) {
    const ext = getFileExt(f);

    // 확장자 체크
    if (!ext || !allowedExts.includes(ext))
      return {
        ok: false,
        message: `허용되지 않는 확장자입니다: ${f.name} (허용: ${allowedExts.join(", ")})`,
      };

    // 개별 용량 체크
    if (f.size > maxFileBytes)
      return {
        ok: false,
        message: `파일은 개별 ${formatBytes(maxFileBytes)} 이하만 가능합니다: ${f.name}`,
      };

    total += f.size;
  }

  // 전체 용량 체크
  if (total > maxTotalBytes)
    return { ok: false, message: `첨부파일 총합은 ${formatBytes(maxTotalBytes)} 이하만 가능합니다.` };

  return { ok: true, files: arr };
};
