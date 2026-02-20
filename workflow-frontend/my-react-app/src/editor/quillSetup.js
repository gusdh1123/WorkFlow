// quillSetup.js
// - Quill 전역 설정
// - 이미지 + iframe 리사이즈 모듈 등록
// - 중복 등록 방지
// - ESM / CommonJS 대응

import Quill from "quill";
import ImageResize from "quill-image-resize-module-rebuild";
import ResizeModule from "quill-resize-module";
import VideoBlot from "./blots/VideoBlot";

const QuillCtor = Quill?.default ?? Quill;

export function setupQuill() {
  if (!QuillCtor) return;
  if (QuillCtor.__registered) return;

  // 이미지 전용 리사이즈 모듈
  QuillCtor.register("modules/imageResize", ImageResize);

  // iframe 포함 resize 모듈
  QuillCtor.register("modules/resize", ResizeModule);

  // 비디오 블롯 등록
  QuillCtor.register(VideoBlot, true);

  QuillCtor.__registered = true;
}