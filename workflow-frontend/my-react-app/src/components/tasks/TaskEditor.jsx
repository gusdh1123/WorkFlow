import "../../css/tasks/TaskEditor.css";
import { useMemo, useRef, useEffect } from "react";
import ReactQuill from "react-quill-new";
import Quill from "quill";
import "react-quill-new/dist/quill.snow.css";
import { uploadEditorImage, deleteEditorImage } from "../../api/uploads"; // 삭제 API 추가
import { validateEditorImage } from "../../utils/fileUtils";
import { setupQuill } from "../../editor/quillSetup";

setupQuill();

// TaskEditor 컴포넌트
// - value: 에디터 내용
// - onChange: 내용 변경 시 호출
// - mode: "create" 또는 "edit", edit 모드에서만 삭제 시 서버 호출
export default function TaskEditor({ value, onChange, mode = "create" }) {
  const quillRef = useRef(null);
  const isEdit = mode === "edit"; // edit 모드 여부

  // 이미지 업로드 핸들러
  const imageHandler = () => {
    const input = document.createElement("input");
    input.type = "file";
    input.accept = "image/*";
    input.click();

    input.onchange = async () => {
      const file = input.files?.[0];
      if (!file) return;

      // 용량/확장자 검증
      const check = validateEditorImage(file);
      if (!check.ok) {
        alert(check.message);
        return;
      }

      try {
        // 서버 tmp 업로드
        const imageUrl = await uploadEditorImage(file);
        const editor = quillRef.current?.getEditor();
        if (!editor) return;

        // 현재 커서 위치 또는 끝에 삽입
        const range = editor.getSelection();
        const index = range ? range.index : editor.getLength();
        editor.insertEmbed(index, "image", imageUrl, "user");
        editor.setSelection(index + 1, 0);
      } catch (e) {
        console.error("이미지 업로드 실패:", e);
        alert("이미지 업로드 실패");
      }
    };
  };

  // 유튜브 영상 삽입 핸들러
  const videoHandler = () => {
    const url = prompt("유튜브 링크를 입력하세요");
    if (!url) return;

    const editor = quillRef.current?.getEditor();
    if (!editor) return;
    const range = editor.getSelection();
    const index = range ? range.index : editor.getLength();

    // 초기 크기 지정 가능
    const value = { src: url, width: "640px", height: "360px" };
    editor.insertEmbed(index, "video", value, "user");
    editor.setSelection(index + 1, 0);
  };

  // Quill 모듈 설정
  const modules = useMemo(() => ({
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
      handlers: { image: imageHandler, video: videoHandler },
    },
    imageResize: {
      // parchment: Quill.import("parchment"),
      modules: ["Resize", "DisplaySize", "Toolbar"],
    },
  }), []);

  // Edit 모드일 때만 삭제 감지
  useEffect(() => {
    if (!isEdit) return; // 생성 모드면 서버 삭제 X

    const editor = quillRef.current?.getEditor();
    if (!editor) return;

    const parser = new DOMParser();
    let oldImages = new Set();

    // 초기 value에서 이미지 URL 수집
    const doc = parser.parseFromString(value || "", "text/html");
    doc.querySelectorAll("img").forEach(img => oldImages.add(img.src));

    // text-change 이벤트로 이미지 삭제 감지
    const handleTextChange = async () => {
      const currentImages = new Set();
      const editorHTML = editor.root.innerHTML;
      const doc = parser.parseFromString(editorHTML, "text/html");
      doc.querySelectorAll("img").forEach(img => currentImages.add(img.src));

      // 삭제된 이미지만 서버 요청
      const removed = [...oldImages].filter(url => !currentImages.has(url));
      for (const url of removed) {
        try {
          await deleteEditorImage(url, "tasks"); // 모듈명 "tasks" 전달
        } catch (e) {
          console.error("삭제 실패", url, e);
        }
      }

      oldImages = currentImages; // 최신 상태 갱신
    };

    editor.on("text-change", handleTextChange);

    return () => {
      editor.off("text-change", handleTextChange);
    };
  }, [isEdit, value]);

  return (
    <ReactQuill
      ref={quillRef}
      theme="snow"
      value={value}
      onChange={onChange}
      modules={modules}
    />
  );
}