import "../../css/tasks/TaskEditor.css";
import { useMemo, useRef, useEffect } from "react";
import ReactQuill from "react-quill-new";
import Quill from "quill";
import "react-quill-new/dist/quill.snow.css";
import { uploadEditorImage } from "../../api/uploads"; // ❗ delete API 제거 (즉시 삭제 금지)
import { validateEditorImage } from "../../utils/fileUtils";
import { setupQuill } from "../../editor/quillSetup";

setupQuill();

// TaskEditor 컴포넌트
// - value: 에디터 내용
// - onChange: 내용 변경 시 호출
// - mode: "create" 또는 "edit"
export default function TaskEditor({
  value,
  onChange,
  onChangeRemovedImages, // 저장 시 삭제 처리 위해 부모로 전달
}) {
  const quillRef = useRef(null);

  // 삭제된 이미지 누적 (즉시 삭제 X, 기록만)
  const removedImagesRef = useRef(new Set());

  // 이미지 업로드 핸들러
  const imageHandler = () => {
    const input = document.createElement("input");
    input.type = "file";
    input.accept = "image/*";
    input.click();

    input.onchange = async () => {
      const file = input.files?.[0];
      if (!file) return;

      const check = validateEditorImage(file);
      if (!check.ok) {
        alert(check.message);
        return;
      }

      try {
        // temp 업로드 유지
        const imageUrl = await uploadEditorImage(file);

        const editor = quillRef.current?.getEditor();
        if (!editor) return;

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
      modules: ["Resize", "DisplaySize", "Toolbar"],
    },
  }), []);

  // 이미지 삭제 감지 (서버 삭제 금지, 기록만)
  useEffect(() => {
    const editor = quillRef.current?.getEditor();
    if (!editor) return;

    const parser = new DOMParser();

    const handleTextChange = () => {
      const currentImages = new Set();

      const editorHTML = editor.root.innerHTML;
      const doc = parser.parseFromString(editorHTML, "text/html");

      doc.querySelectorAll("img").forEach(img => {
        currentImages.add(img.src);
      });

      if (value) {
        const prevDoc = parser.parseFromString(value, "text/html");
        prevDoc.querySelectorAll("img").forEach(img => {
          if (!currentImages.has(img.src)) {
            removedImagesRef.current.add(img.src);
          }
        });
      }

      // 부모로 전달 (저장 시 사용)
      if (onChangeRemovedImages) {
        onChangeRemovedImages([...removedImagesRef.current]);
      }
    };

    editor.on("text-change", handleTextChange);

    return () => {
      editor.off("text-change", handleTextChange);
    };
  }, [value, onChangeRemovedImages]);

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