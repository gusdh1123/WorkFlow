import { api } from "./api";

// 에디터 이미지 업로드
// - file: 업로드할 단일 File 객체
// - FormData로 변환 후 multipart/form-data로 전송
// - 서버에서 반환하는 URL을 반환
export async function uploadEditorImage(file) {
  const formData = new FormData();
  formData.append("file", file); // FormData에 파일 추가

  // POST 요청: /api/uploads/images
  const res = await api.post("/api/uploads/images", formData, {
    headers: { "Content-Type": "multipart/form-data" }, // 반드시 multipart/form-data
  });

  // 서버에서 반환한 URL (클라이언트에서 접근 가능한 경로)
  return res.data?.url;
}

// 에디터 이미지 삭제
// - url: 삭제할 이미지 URL (/uploads/...)
// - module: 서버에서 사용하는 모듈명 ("tasks", "profile" 등)
export async function deleteEditorImage(url, module = "tasks") {
  try {
    await api.post("/api/uploads/images/delete", null, {
      params: { url, module },
    });
    return true;
  } catch (err) {
    console.error("이미지 삭제 실패", url, err);
    return false;
  }
}
