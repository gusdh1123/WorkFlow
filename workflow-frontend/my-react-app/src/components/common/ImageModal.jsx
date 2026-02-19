import { useEffect } from "react";

// ImageModal 컴포넌트
// - 이미지 확대/모달 표시
// - ESC 키 또는 배경 클릭으로 닫기
// - 모달 열릴 때 스크롤 잠금
export default function ImageModal({ open, src, alt, onClose }) {

  // open 상태 변화 시 사이드 이펙트
  useEffect(() => {
    if (!open) return;

    // ESC 키 이벤트: 닫기
    const onKeyDown = (e) => {
      if (e.key === "Escape") onClose();
    };

    document.addEventListener("keydown", onKeyDown);

    // 모달 열리면 페이지 스크롤 잠금
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    // clean-up: 이벤트 제거 + 스크롤 복구
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.body.style.overflow = prevOverflow;
    };
  }, [open, onClose]);

  // 모달이 닫혀있으면 아무것도 렌더하지 않음
  if (!open) return null;

  return (
    // 배경 클릭 시 모달 닫기
    <div className="imgmodal__backdrop" onClick={onClose}>
      {/* 실제 이미지 패널: 클릭 시 이벤트 버블링 막음 */}
      <div className="imgmodal__panel" onClick={(e) => e.stopPropagation()}>
        {/* 닫기 버튼 */}
        <button
          type="button"
          className="imgmodal__close"
          onClick={onClose}
          aria-label="닫기"
        >
          ✕
        </button>

        {/* 모달 이미지 */}
        <img className="imgmodal__img" src={src} alt={alt || ""} />
      </div>
    </div>
  );
}

// ImageModal 역할 요약
// - open prop으로 표시 여부 제어
// - ESC 키, 배경 클릭, 닫기 버튼으로 닫기 가능
// - 모달 열릴 때 body 스크롤 잠금
