// Quill 에디터 전역 설정
// - 이미지 리사이즈 모듈 등록
// - 중복 등록 방지

import Quill from "quill";
import ImageResize from "quill-image-resize-module-rebuild";

// Quill 생성자 호환 처리 (ESM 번들 / CommonJS 차이)
const QuillCtor = Quill?.default ?? Quill;

// Quill 모듈 전역 등록 (1회만)
export function setupQuill() {

  if (!QuillCtor) return;          // Quill 불러오기 실패 시 중단
  if (QuillCtor.__registered) return; // 이미 등록됐으면 중단

  // 이미지 리사이즈 모듈 등록
  QuillCtor.register("modules/imageResize", ImageResize);

  // 등록 완료 표시
  QuillCtor.__registered = true;
}
