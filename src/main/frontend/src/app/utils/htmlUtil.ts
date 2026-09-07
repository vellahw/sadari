/**
 * src/main/frontend/src/app/utils/htmlUtil.ts 파일의 프론트엔드 화면, API, 훅 또는 유틸 로직을 담당함
 *
 * @author HanWon.Jang
 */
export function stripHtmlTags(value?: string) {

  return value?.replace(/<[^>]*>/g, "") ?? "";
}

/**
 * 도서 검색 API가 여러 저자를 ^ 구분자로 내려주는 경우 화면과 저장값에 그대로 노출되지 않도록 정리함
 *
 * @author HanWon.Jang
 * @param value 도서 검색 API 또는 서버에서 받은 저자 문자열
 * @return HTML 태그와 ^ 구분자를 제거한 저자 문자열
 */
export const normalizeBookAuthor = (value?: string) => {

  // 외부 도서 API의 구분자를 치환한 뒤 문자열 양끝의 불필요한 공백을 제거한 저자명 반환
  return stripHtmlTags(value).replace(/\s*\^\s*/g, ", ").trim();
};
