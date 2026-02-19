// 날짜/시간 관련 유틸

// "YYYY-MM-DD" (LocalDate) 전용 파서
// 로컬 기준으로 자정(00:00:00)에 해당하는 Date 객체 생성
// 입력이 없거나 잘못된 문자열이면 null 반환
export const parseLocalDate = (s) => {
  if (!s) return null;
  const d = new Date(`${s}T00:00:00`);
  return Number.isNaN(d.getTime()) ? null : d;
};

// 오늘 0시 생성
// 현재 시간 기준으로 '오늘' 자정 기준 Date 객체 반환
export const startOfToday = () => {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate());
};

// 오늘 기준 D-Day 계산
// dueDateStr: "YYYY-MM-DD" 문자열
// 반환: 오늘 기준으로 남은 일수 (양수), 지난 날짜면 음수, invalid → null
export const daysDiffFromToday = (dueDateStr) => {
  const due = parseLocalDate(dueDateStr);
  if (!due) return null;
  const ms = due.getTime() - startOfToday().getTime();
  return Math.round(ms / (1000 * 60 * 60 * 24));
};

// D-Day / 마감 상태 라벨

// D-DAY / D-n / OVERDUE n d
// 오늘이면 "D-DAY", 남은 날이면 "D-n", 지난 날이면 "OVERDUE n d"
export const ddayLabel = (dueDateStr) => {
  const diff = daysDiffFromToday(dueDateStr);
  if (diff === null) return null;
  if (diff === 0) return "D-DAY";
  if (diff > 0) return `D-${diff}`;
  return `OVERDUE ${Math.abs(diff)}d`;
};

// 마감 임박 / 초과 상태 CSS 클래스

// DONE/CANCELED 상태는 클래스 없음
// OVERDUE → is-overdue
// 오늘 → is-dday
// 1~2일 남음 → is-soon
// 그 외 → ""
export const dueClass = (dueDateStr, status) => {
  if (status === "DONE" || status === "CANCELED") return "";
  const diff = daysDiffFromToday(dueDateStr);
  if (diff === null) return "";
  if (diff < 0) return "is-overdue";
  if (diff === 0) return "is-dday";
  if (diff <= 2) return "is-soon";
  return "";
};

// 리스트 / 디테일용 날짜 포맷

// isoString: ISO 날짜 문자열 ("YYYY-MM-DDTHH:mm:ss" 등)
// options:
//   showTime: true → HH:mm 포함, false → 날짜만
//   relativeDays: true → 최근 7일 내 상대 날짜 표시, false → 절대 날짜만
export const formatDateTime = (isoString, options = {}) => {
  if (!isoString) return "-";

  const { showTime = true, relativeDays = true } = options;

  const d = new Date(isoString);
  if (Number.isNaN(d.getTime())) return isoString; // 잘못된 문자열이면 그대로 반환

  const now = new Date();
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const startOfThatDay = new Date(d.getFullYear(), d.getMonth(), d.getDate());

  // 오늘 기준 차이 일수 계산
  const diffDays = Math.floor((startOfToday - startOfThatDay) / (24 * 60 * 60 * 1000));

  const pad = (n) => String(n).padStart(2, "0");
  const hh = pad(d.getHours());
  const mm = pad(d.getMinutes());
  const yyyy = d.getFullYear();
  const mmth = pad(d.getMonth() + 1);
  const dd = pad(d.getDate());

  // 상대 날짜 표시
  if (diffDays === 0) return showTime ? `오늘 ${hh}:${mm}` : "오늘";
  if (diffDays === 1) return showTime ? `어제 ${hh}:${mm}` : "어제";
  if (relativeDays && diffDays > 1 && diffDays <= 7)
    return showTime ? `${diffDays}일 전 ${hh}:${mm}` : `${diffDays}일 전`;

  // 7일 이상 → 절대 날짜 표시
  return showTime ? `${yyyy}.${mmth}.${dd} ${hh}:${mm}` : `${yyyy}.${mmth}.${dd}`;
};

// 기존 호환용 래퍼
export const formatRelativeDateTime = (iso) =>
  formatDateTime(iso, { showTime: true, relativeDays: true });

export const formatListDateTime = (iso) =>
  formatDateTime(iso, { showTime: true, relativeDays: true });
