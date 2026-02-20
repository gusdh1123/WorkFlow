import "../../css/tasks/TaskEditor.css";
import { useMemo, useRef } from "react";
import ReactQuill from "react-quill-new";
import Quill from "quill";
import "react-quill-new/dist/quill.snow.css";
import VideoBlot from "../../editor/blots/VideoBlot";
import { uploadEditorImage } from "../../api/uploads";
import { validateEditorImage } from "../../utils/fileUtils";

export default function TaskEditor({ value, onChange }) {
  const quillRef = useRef(null);

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
    resize: {
      modules: ["Resize", "DisplaySize", "Toolbar"],
      embedTags: ["IFRAME", "IMG"],
    },
    imageResize: {
      parchment: Quill.import("parchment"),
      modules: ["Resize", "DisplaySize", "Toolbar"],
    },
  }), []);

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