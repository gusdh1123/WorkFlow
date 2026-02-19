// 공개 범위 코드 → 화면 표시용 라벨 변환
// "PUBLIC" → "전사", "DEPARTMENT" → "부서", "PRIVATE" → "개인"
// 알 수 없는 값은 그대로 출력, 값이 없으면 "-"
export const visibilityLabel = (v) => {
  if (v === "PUBLIC") return "전사";
  if (v === "DEPARTMENT") return "부서";
  if (v === "PRIVATE") return "개인";
  return v ?? "-";
};
