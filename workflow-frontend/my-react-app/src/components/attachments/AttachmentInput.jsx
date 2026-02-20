import "../../css/tasks/AttachmentInput.css";

import { useMemo, useRef, useState } from "react";
import { validateAttachments, formatBytes } from "../../utils/fileUtils";
import {
  ATTACH_MAX_FILES,
  ATTACH_MAX_FILE_BYTES,
  ATTACH_MAX_TOTAL_BYTES,
  ATTACH_ALLOWED_EXTS,
} from "../../constants/uploadLimits";

// AttachmentInput 컴포넌트
// - 드래그&드롭 + 파일 선택 + 검증 + 리스트 표시 + 삭제
// - value: 현재 선택된 File 배열
// - onChange: value 변경 콜백
export default function AttachmentInput({ value = [], onChange }) {
  const inputRef = useRef(null);  // 숨겨진 파일 input 접근용 ref
  const [error, setError] = useState(""); // 검증 오류 메시지 상태

  // 선택된 파일 총 용량 계산
  const totalBytes = useMemo(
    () => (value || []).reduce((sum, f) => sum + (f?.size || 0), 0),
    [value]
  );

  // 파일 선택창 열기
  const openPicker = () => inputRef.current?.click();

  // 파일 적용 함수
  const applyFiles = (incomingFileList) => {
    const incoming = Array.from(incomingFileList || []);
    if (incoming.length === 0) return;

    // 기존 파일 + 신규 파일 합치기
    const merged = [...(value || []), ...incoming];

    // 파일 검증
    // - 최대 개수, 개별/총 용량, 확장자
    const check = validateAttachments(merged, {
      maxFiles: ATTACH_MAX_FILES,
      maxFileBytes: ATTACH_MAX_FILE_BYTES,
      maxTotalBytes: ATTACH_MAX_TOTAL_BYTES,
      allowedExts: ATTACH_ALLOWED_EXTS,
    });

    // 검증 실패 시 오류 상태 설정
    if (!check.ok) {
      setError(check.message);
      return;
    }

    // 검증 성공 → 오류 초기화 + 부모 콜백
    setError("");
    onChange?.(check.files);
  };

  // 파일 input onChange
  const onPick = (e) => {
    applyFiles(e.target.files);
    e.target.value = ""; // 같은 파일 재선택 가능하도록 초기화
  };

  // 특정 인덱스 파일 제거
  const removeAt = (idx) => {
    const next = (value || []).filter((_, i) => i !== idx);
    setError("");
    onChange?.(next);
  };

  // 전체 파일 삭제
  const clearAll = () => {
    setError("");
    onChange?.([]);
  };

  // 드래그&드롭 처리
  const onDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    applyFiles(e.dataTransfer.files);
  };

  // 드래그 오버 시 브라우저 기본 동작 방지
  const onDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
  };

  return (
    <div className="taskform taskform--attach">
      <div className="taskform__section">
        <label className="taskform__label">첨부 파일</label>

        {/* 숨겨진 파일 input */}
        <input
          ref={inputRef}
          className="taskform__file"
          type="file"
          multiple
          accept={ATTACH_ALLOWED_EXTS.map((e) => `.${e}`).join(",")}
          onChange={onPick}
          style={{ display: "none" }}
        />

        {/* 드래그&드롭 영역 */}
        <div
          className="attach__drop"
          onClick={openPicker}
          onDrop={onDrop}
          onDragOver={onDragOver}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => {
            if (e.key === "Enter" || e.key === " ") openPicker();
          }}
          title="클릭 또는 드래그&드롭"
        >
          <div className="attach__dropTitle">클릭 또는 드래그&드롭</div>
          <div className="attach__dropHelp">
            최대 {ATTACH_MAX_FILES}개 / 개별 {formatBytes(ATTACH_MAX_FILE_BYTES)} / 총합{" "}
            {formatBytes(ATTACH_MAX_TOTAL_BYTES)}
          </div>
          <div className="attach__dropHelp">허용 확장자: {ATTACH_ALLOWED_EXTS.join(", ")}</div>
        </div>

        {/* 검증 오류 표시 */}
        {error && <div className="taskform__error">{error}</div>}

        {/* 선택된 파일 리스트 */}
        {(value?.length ?? 0) > 0 && (
          <div className="attach__listWrap">
            <div className="attach__summary">
              <span>
                {value.length}개 선택됨 (총 {formatBytes(totalBytes)})
              </span>

              {/* 전체 삭제 버튼 */}
              <button type="button" className="attach__clear" onClick={clearAll}>
                전체 삭제
              </button>
            </div>

            <ul className="attach__list">
              {value.map((f, idx) => (
                <li key={`${f.name}-${f.size}-${idx}`} className="attach__item">
                  <div className="attach__meta">
                    <div className="attach__name">{f.name}</div>
                    <div className="attach__size">{formatBytes(f.size)}</div>
                  </div>

                  {/* 개별 파일 삭제 버튼 */}
                  <button
                    type="button"
                    className="attach__remove"
                    onClick={() => removeAt(idx)}
                    aria-label="파일 삭제"
                  >
                    ✕
                  </button>
                </li>
              ))}
            </ul>
          </div>
        )}

        {/* 프론트 검증 안내 */}
        <div className="taskform__help">파일 형식 및 용량은 서비스 정책에 따라 제한될 수 있습니다.</div>
      </div>
    </div>
  );
}

// AttachmentInput 역할 요약
// - 사용자가 파일 선택/드래그&드롭 가능
// - 프론트에서 파일 개수/용량/확장자 검증
// - 선택된 파일 리스트 표시 + 삭제
// - 부모 컴포넌트와 value/onChange로 상태 공유
