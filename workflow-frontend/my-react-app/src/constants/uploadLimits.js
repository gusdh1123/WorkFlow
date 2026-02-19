// 업로드 제한 상수
// 에디터와 첨부파일 업로드에서 공통으로 사용

// 에디터 이미지 최대 용량
export const EDITOR_IMAGE_MAX_BYTES = 5 * 1024 * 1024; // 5MB

// 첨부파일 개별/총합 용량 제한
export const ATTACH_MAX_FILE_BYTES = 20 * 1024 * 1024;  // 20MB (개별 파일)
export const ATTACH_MAX_TOTAL_BYTES = 50 * 1024 * 1024; // 50MB (전체 첨부 합계)
export const ATTACH_MAX_FILES = 10;                     // 최대 10개 파일

// 허용 확장자 화이트리스트
// 필요 시 추가 가능
export const ATTACH_ALLOWED_EXTS = [
  "pdf", "doc", "docx", "xls", "xlsx",
  "ppt", "pptx", "hwp", "txt", "csv",
  "zip", "png", "jpg", "jpeg", "webp",
];
