import "../../css/tasks/TaskEditor.css";

import { useMemo, useRef } from "react";
import ReactQuill from "react-quill-new";
import Quill from "quill";
import "react-quill-new/dist/quill.snow.css";

import { setupQuill } from "../../editor/quillSetup";
import { uploadEditorImage } from "../../api/uploads";
import { validateEditorImage } from "../../utils/fileUtils";

// Quill 모듈 전역 등록
// - 중복 등록 방지 포함
setupQuill();

// TaskEditor 컴포넌트
// - ReactQuill 기반 리치 텍스트 에디터
// - 이미지 업로드 + 검증 + 삽입 지원
export default function TaskEditor({ value, onChange }) {

  const quillRef = useRef(null); // 에디터 인스턴스 참조

  // 이미지 업로드 핸들러
  // - 툴바 이미지 버튼 클릭 시 실행
  // - 파일 선택, 검증, 서버 업로드, 에디터 삽입
  const imageHandler = () => {

    const input = document.createElement("input");
    input.type = "file";
    input.accept = "image/*"; // 이미지 전용
    input.click();

    input.onchange = async () => {

      const file = input.files?.[0];
      if (!file) return;

      // 이미지 검증 (파일 크기, 확장자 등)
      const check = validateEditorImage(file);
      if (!check.ok) {
        alert(check.message);
        return;
      }

      try {

        // 서버 업로드 → URL 반환
        const imageUrl = await uploadEditorImage(file);
        if (!imageUrl) {
          alert("이미지 업로드 실패");
          return;
        }

        // Quill 에디터 가져오기
        const editor = quillRef.current?.getEditor();
        if (!editor) return;

        // 커서 위치 가져오기
        const range = editor.getSelection();
        const index = range ? range.index : editor.getLength();

        // 이미지 삽입 후 커서 이동
        editor.insertEmbed(index, "image", imageUrl, "user");
        editor.setSelection(index + 1, 0);

      } catch (e) {

        console.error("이미지 업로드 실패:", e);
        alert("이미지 업로드 실패");

      }
    };
  };

  // Quill 모듈 설정
  // - toolbar: 버튼 구성 + imageHandler 연결
  // - imageResize: 이미지 크기 조절 모듈
  const modules = useMemo(
    () => ({
      toolbar: {
        container: [
          [{ header: [1, 2, 3, false] }],
          ["bold", "italic", "underline", "strike"],
          [{ color: [] }, { background: [] }],
          [{ list: "ordered" }, { list: "bullet" }],
          [{ indent: "-1" }, { indent: "+1" }],
          [{ align: [] }],
          ["blockquote", "code-block"],
          ["link", "image", "video", "code"],
          ["clean"],
        ],
        handlers: { image: imageHandler },
      },

      imageResize: {
        parchment: Quill.import("parchment"),
        modules: ["Resize", "DisplaySize", "Toolbar"],
      },
    }),
    []
  );

  return (
    <ReactQuill
      ref={quillRef}   // 에디터 참조 연결
      theme="snow"      // 기본 스노우 테마
      value={value}     // 에디터 내용
      onChange={onChange} // 내용 변경 시 부모 콜백
      modules={modules} // 모듈 설정
    />
  );
}

// TaskEditor 역할 요약
// - ReactQuill 기반 에디터
// - 이미지 업로드/검증 후 삽입
// - 커서 위치에 맞춰 이미지 삽입
// - toolbar, imageResize 모듈 적용
