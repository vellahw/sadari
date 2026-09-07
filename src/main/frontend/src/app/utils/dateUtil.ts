/**
 * src/main/frontend/src/app/utils/dateUtil.ts
 * 프론트엔드 화면, API, 훅 등 시스템 전반에서 공통으로 사용하는 날짜 변환 및 포맷팅 유틸리티임
 *
 * @author HanWon.Jang
 */
import { getMessageLocale } from "@/app/messages/message";

/**
 * 숫자를 전달받아 두 자리의 문자열로 패딩 처리함
 *
 * @author HanWon.Jang
 * @param value 패딩 처리를 수행할 10진수 숫자
 * @return 두 자리로 정렬되어 왼쪽에 0이 채워진 문자열 (예: 5 -> "05")
 */
export function padTwoDigits(value: number) {

  return String(value).padStart(2, "0");
}

/**
 * Date 객체를 YYYY-MM-DD 형식의 문자열로 변환함
 *
 * @author HanWon.Jang
 * @param date 포맷팅 대상 Date 객체
 * @return YYYY-MM-DD 형식의 날짜 문자열
 */
export function formatDateValue(date: Date) {

  return `${date.getFullYear()}-${padTwoDigits(date.getMonth() + 1)}-${padTwoDigits(date.getDate())}`;
}

/**
 * Date 객체를 YYYY-MM 형식의 문자열로 변환함
 *
 * @author HanWon.Jang
 * @param date 포맷팅 대상 Date 객체
 * @return YYYY-MM 형식의 년월 문자열
 */
export function formatYearMonthValue(date: Date) {

  return `${date.getFullYear()}-${padTwoDigits(date.getMonth() + 1)}`;
}

/**
 * yyyy-MM-dd 형식의 날짜 문자열을 yyyy.MM.dd 형식으로 변환함
 * 마이페이지와 소셜 프로필의 요약 목록처럼 짧은 날짜 표시가 필요한 영역에서 사용함
 *
 * @author HanWon.Jang
 * @param value yyyy-MM-dd 형식의 날짜 문자열
 * @return yyyy.MM.dd 형식의 날짜 문자열
 */
export function formatDashedDateToDot(value?: string) {

  if (!value) {
    return "";
  }

  const [year, month, day] = value.split("-");

  if (!year || !month || !day) {
    return value;
  }

  return `${year}.${month}.${day}`;
}

/**
 * 두 날짜 사이의 일수 차이를 계산함
 * 목표 기간 계산에서 시간대나 현재 시각의 영향을 줄이기 위해 로컬 자정 기준 날짜만 비교함
 *
 * @author HanWon.Jang
 * @param startValue 시작일 문자열
 * @param endValue 종료일 문자열
 * @return 시작일부터 종료일까지의 일수
 */
export function getDateDiffDays(startValue?: string, endValue?: string) {

  if (!startValue || !endValue) {
    return 0;
  }

  const startDate = parseDateValue(startValue);
  const endDate = parseDateValue(endValue);
  const startTime = new Date(
    startDate.getFullYear(),
    startDate.getMonth(),
    startDate.getDate(),
  ).getTime();
  const endTime = new Date(
    endDate.getFullYear(),
    endDate.getMonth(),
    endDate.getDate(),
  ).getTime();

  return Math.ceil((endTime - startTime) / 86400000);
}

/**
 * 오늘 기준 목표 종료일까지 남은 일수를 계산함
 * 종료일 당일부터는 남은 기간이 0일로 계산되어 목표기간 경과 안내 대상으로 처리됨
 *
 * @author HanWon.Jang
 * @param endValue 목표 종료일 문자열
 * @return 오늘부터 목표 종료일까지 남은 일수
 */
export function getRemainDaysUntil(endValue?: string) {

  return getDateDiffDays(formatDateValue(new Date()), endValue);
}

/**
 * 전체 목표기간을 100%로 보고 남은 기간 비율을 계산함
 * 남은 기간이 짧아질수록 낮은 비율을 반환하므로 진행 막대와 같은 색상 체계를 그대로 사용할 수 있음
 *
 * @author HanWon.Jang
 * @param startValue 목표 시작일 문자열
 * @param endValue 목표 종료일 문자열
 * @return 남은 기간 비율
 */
export function getRemainPeriodRate(startValue?: string, endValue?: string) {

  const totalDays = Math.max(1, getDateDiffDays(startValue, endValue));
  const remainDays = Math.max(0, getRemainDaysUntil(endValue));

  return Math.max(0, Math.min(100, Math.round((remainDays / totalDays) * 100)));
}

/**
 * 8자리 숫자 형태의 날짜 문자열을 해석하여 개별 연, 월, 일 정보 및 Date 객체로 변환함
 *
 * @author HanWon.Jang
 * @param value 파싱할 8자리 압축 날짜 문자열 (예: "20260715")
 * @return 연, 월, 일 정보 및 생성된 Date 객체를 포함하는 오브젝트 (유효하지 않은 입력값인 경우 null 반환)
 */
function parseCompactDateParts(value?: string) {
  // 전달된 값이 없는 경우 바로 연산을 중단하여 무의미한 정규식 파싱을 차단함
  if (!value) {
    return null;
  }

  const compactDate = value.replace(/\D/g, "");

  // 연월일 8자리(YYYYMMDD) 구성 요건을 만족하지 못하면 파싱 대상에서 제외함
  if (compactDate.length !== 8) {
    return null;
  }

  const year = Number(compactDate.slice(0, 4));
  const month = Number(compactDate.slice(4, 6));
  const day = Number(compactDate.slice(6, 8));
  const date = new Date(year, month - 1, day);

  // JS Date 객체의 오토롤백(예: 2월 31일 입력 시 3월 3일로 넘어가는 현상)을 필터링하여 실제 유효한 날짜인지 재검증함
  if (
      date.getFullYear() !== year ||
      date.getMonth() !== month - 1 ||
      date.getDate() !== day
  ) {
    return null;
  }

  return { year, month, day, date };
}

/**
 * 영어 서수 날짜 표기를 위한 접미사(st, nd, rd, th)를 구함
 *
 * @author HanWon.Jang
 * @param day 서수를 구할 일자 (1~31)
 * @return 영문 날짜용 서수 접미사
 */
function getEnglishOrdinalSuffix(day: number) {
  // 11일, 12일, 13일은 예외적으로 th 접미사를 사용하므로 사전 분기 차단함
  if (day >= 11 && day <= 13) {
    return "th";
  }

  // 일 단위 마지막 자릿수에 부합하는 영문 서수 전용 접미사를 반환함
  switch (day % 10) {
    case 1:
      return "st";
    case 2:
      return "nd";
    case 3:
      return "rd";
    default:
      return "th";
  }
}

/**
 * 8자리 날짜 문자열을 한국어 표준 날짜 표기 형식으로 변환함
 *
 * @author HanWon.Jang
 * @param value 8자리 날짜 문자열 (예: "20260715")
 * @return "YYYY년 MM월 DD일" 포맷의 한글 날짜 문자열 (파싱 불가 시 원본 반환)
 */
export function formatCompactDateToKorean(value?: string) {

  if (!value) {
    return "";
  }

  const parsedDate = parseCompactDateParts(value);

  if (!parsedDate) {
    return value;
  }

  return `${parsedDate.year}\uB144${parsedDate.month}\uC6D4${parsedDate.day}\uC77C`;
}

/**
 * 8자리 날짜 문자열을 영어 표준 날짜 표기 형식으로 변환함
 *
 * @author HanWon.Jang
 * @param value 8자리 날짜 문자열 (예: "20260715")
 * @return "Month DDth, YYYY" 포맷의 영문 날짜 문자열 (파싱 불가 시 원본 반환)
 */
export function formatCompactDateEnglish(value?: string) {

  if (!value) {
    return "";
  }

  const parsedDate = parseCompactDateParts(value);

  if (!parsedDate) {
    return value;
  }

  // 다국어 확장을 고려하여 바닐라 JS의 Intl API를 통해 영문 전체 월 명칭을 동적으로 획득함
  const monthName = new Intl.DateTimeFormat("en", { month: "long" }).format(
      parsedDate.date,
  );

  return `${monthName} ${parsedDate.day}${getEnglishOrdinalSuffix(parsedDate.day)}, ${parsedDate.year}`;
}

/**
 * 브라우저 로케일에 맞춰 8자리 압축 날짜 형식을 한글 또는 영문 날짜 형식으로 동적 포맷팅함
 *
 * @author HanWon.Jang
 * @param value 8자리 날짜 문자열 (예: "20260715")
 * @return 로케일 판단에 맞춰 변형된 로컬 날짜 문자열
 */
export const formatCompactDate = (value?: string) => {

  // 계정에서 선택한 표시 언어를 출간일 표기 기준으로 사용
  const locale = getMessageLocale();

  // 한국어 표시 설정에는 한글 날짜 표기 적용
  if (locale === "ko") {
    // 한국어 출간일 문자열 반환
    return formatCompactDateToKorean(value);
  }

  // 영어 표시 설정에는 영문 날짜 표기 적용
  return formatCompactDateEnglish(value);
};

/**
 * 하이픈으로 구분된 날짜 문자열을 Date 객체로 정적 파싱함
 *
 * @author HanWon.Jang
 * @param value "YYYY-MM-DD" 포맷의 날짜 문자열
 * @return 변환 완료된 Date 객체
 */
export function parseLocalDate(value: string) {

  const [year, month, date] = value.split("-").map(Number);
  return new Date(year, month - 1, date);
}

/**
 * 날짜 문자열을 파싱하되, 파싱에 실패하거나 빈 값일 경우 지정된 기본(Fallback) 날짜를 대체 반환함
 *
 * @author HanWon.Jang
 * @param value "YYYY-MM-DD" 포맷의 날짜 문자열 (Null 또는 빈 값 허용)
 * @param fallbackDate 파싱 불가 시 대체로 사용할 기본 Date 객체 (미지정 시 현재 시간 적용)
 * @return 파싱된 Date 객체 혹은 예외 fallback용 Date 객체
 */
export function parseDateValue(value?: string, fallbackDate = new Date()) {

  if (!value) {
    return fallbackDate;
  }

  const [year, month, day] = value.split("-").map(Number);

  // 연, 월, 일 요소 중 정상적인 데이터가 파싱되지 않을 경우 예외 복구 로직을 수행함
  if (!year || !month || !day) {
    return fallbackDate;
  }

  return new Date(year, month - 1, day);
}

/**
 * 두 Date 객체가 동일한 연, 월, 일에 속해있는지 논리 비교를 수행함
 *
 * @author HanWon.Jang
 * @param a 비교할 첫 번째 Date 객체
 * @param b 비교할 두 번째 Date 객체
 * @return 시간 정보에 상관없이 날짜(연/월/일)가 완전히 일치하면 true 반환
 */
export function isSameLocalDate(a: Date, b: Date) {

  return (
      a.getFullYear() === b.getFullYear() &&
      a.getMonth() === b.getMonth() &&
      a.getDate() === b.getDate()
  );
}
