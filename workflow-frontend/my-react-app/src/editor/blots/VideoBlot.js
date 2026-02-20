import Quill from "quill";

const QuillCtor = Quill?.default ?? Quill;
const BlockEmbed = QuillCtor.import("blots/block/embed");

/**
 * 퍼가기 iframe, 일반 URL, youtu.be URL을 모두 안전한 embed URL로 변환
 * @param {string} url - 유튜브 퍼가기 iframe src 또는 일반 URL
 * @returns {string} embed용 URL
 */
function convertYoutubeUrl(url) {
  try {
    // iframe src에서 URL만 뽑기
    if (url.startsWith("<iframe")) {
      const srcMatch = url.match(/src=["']([^"']+)["']/i);
      if (srcMatch) url = srcMatch[1];
    }

    const parsed = new URL(url);
    // youtu.be 짧은 링크
    if (parsed.hostname.includes("youtu.be")) return `https://www.youtube.com/embed/${parsed.pathname.slice(1)}`;
    // 일반 유튜브 URL
    if (parsed.searchParams.has("v")) return `https://www.youtube.com/embed/${parsed.searchParams.get("v")}`;
  } catch {
    // URL 파싱 실패 시 원본 반환
  }
  return url;
}

class VideoBlot extends BlockEmbed {
  static blotName = "video";
  static tagName = "div"; // 블록 형태로 삽입
  static className = "ql-video-wrapper";

  static create(value) {
    const wrapper = super.create();
    wrapper.style.position = "relative";
    wrapper.style.margin = "12px 0";

    // value가 객체인지 확인 (src, width, height 가능)
    let src = value;
    let width = "640px";
    let height = "360px";

    if (typeof value === "object") {
      src = value.src;
      width = value.width || width;
      height = value.height || height;
    }

    wrapper.style.width = width;
    wrapper.style.height = height;

    // iframe 생성
    const iframe = document.createElement("iframe");
    iframe.setAttribute("src", convertYoutubeUrl(src)); // 자동 변환
    iframe.setAttribute("frameborder", "0");
    iframe.setAttribute("allowfullscreen", "true");
    iframe.setAttribute("allow", "autoplay; fullscreen");
    Object.assign(iframe.style, {
      width: "100%",
      height: "100%",
      display: "block",
      borderRadius: "8px",
    });

    wrapper.appendChild(iframe);
    return wrapper;
  }

  static value(node) {
    const iframe = node.querySelector("iframe");
    return {
      src: iframe?.getAttribute("src"),
      width: node.style.width,
      height: node.style.height,
    };
  }
}

// Quill에 등록
Quill.register(VideoBlot, true);
export default VideoBlot;