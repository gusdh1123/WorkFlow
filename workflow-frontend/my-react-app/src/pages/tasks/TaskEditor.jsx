import { useMemo, useRef } from "react";
import ReactQuill from "react-quill-new";
import "react-quill-new/dist/quill.snow.css";
import { api } from "../api/api";

export default function TaskEditor({ value, onChange }) {
  const quillRef = useRef(null);

  const imageHandler = async () => {
    const input = document.createElement("input");
    input.type = "file";
    input.accept = "image/*";
    input.click();

    input.onchange = async () => {
      const file = input.files?.[0];
      if (!file) return;

      const formData = new FormData();
      formData.append("file", file);

      try {
        // 서버 업로드 (JWT는 api 인터셉터가 붙여줌)
        const res = await api.post("/api/upload", formData, {
          headers: { "Content-Type": "multipart/form-data" },
        });

        // 서버가 { url: "http://..." } 형태로 준다고 가정
        const imageUrl = res.data.url;

        const editor = quillRef.current?.getEditor();
        if (!editor) return;

        const range = editor.getSelection(true) || { index: editor.getLength() };
        editor.insertEmbed(range.index, "image", imageUrl);
        editor.setSelection(range.index + 1);
      } catch (e) {
        console.error("이미지 업로드 실패:", e);
        alert("이미지 업로드 실패");
      }
    };
  };

  const modules = useMemo(
    () => ({
      toolbar: {
        container: [
          [{ header: [1, 2, 3, false] }],
          ["bold", "italic", "underline", "strike"],
          [{ list: "ordered" }, { list: "bullet" }],
          ["link", "image"],
          ["clean"],
        ],
        handlers: { image: imageHandler },
      },
    }),
    []
  );

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
