/**
 * fileName       : book.type
 * author         : HanWon.Jang
 * date           : 2026-04-02
 * description    : 독후감 관련 타입 정의
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-04-02       HanWon.Jang       최초 생성
 * 2026-08-16       SeungHyeon.Kang   도서 인기 검색어 응답 타입 추가
 * 2026-09-08       HanWon.Jang       언어별 외부 도서 검색 결과 타입 반영
 * 2026-09-11       HanWon.Jang       진행 중인 모임 회차 연결 여부 추가
 */

/**
 * 언어별 외부 도서 검색 결과를 화면 계약으로 변환한 타입
 */
export interface BookSearchResultType {
  // 기간별 인기 도서에서만 전달되는 1부터 시작하는 순위
  rank?: number;
  // 기간별 인기 도서에서만 전달되는 고유 독후감 작성자 수
  reportCount?: number;
  // 기간별 인기 도서에서만 전달되는 읽는 중 제외 전체 평균 별점
  ratingAverage?: number | string | null;
  // 책 제목
  title: string;
  // 저자
  author: string;
  // 출판사
  publisher: string;
  // 책 isbn
  isbn: string;
  // 검색 공급자가 반환한 도서 정보 언어 코드
  langCode?: "ko" | "en";
  // 책 표지 이미지
  image: string;
  // 원본 표지 로드 실패 시 사용할 공급자 썸네일 이미지
  thumbnailImage?: string;
  // 책 소개 내용
  description: string;
  // 출간일
  pubdate: string;
}

// 인기 도서 화면에서 선택할 수 있는 현재 집계 기간
export type PopularBookPeriodType = "weekly" | "monthly" | "yearly";

/**
 * 최근 고유 회원 검색 수 기준 도서 인기 검색어 타입
 */
export interface PopularSearchKeywordType {
  // 비속어와 개인정보형 문자열을 제외한 최근 검색 순위
  rank: number;
  // 검색 입력과 즉시 조회에 사용할 정규화된 검색어
  keyword: string;
}

/**
 * 언어별 외부 도서 검색 페이지와 다음 조회 상태 타입
 */
export interface BookSearchPageType {
  // 현재 언어의 외부 API에서 한 번에 조회한 도서 목록
  bookList: BookSearchResultType[];
  // 현재 응답이 외부 도서 검색의 마지막 페이지인지 나타내는 값
  end: boolean;
  // 다음 공급자 페이지를 조회할 검색 결과 시작 위치
  nextStart?: number | null;
}

/**
 * "책 타입"
 */
export interface BookDtoType {
  bookDto: {
    // 책 제목
    bookTitl: string;
    // 저자
    bookAthr: string;
    // 출판사
    bookPubl: string;
    // 책 isbn
    bookIsbn: string;
    // 책 표지 이미지
    bookCvim: string;
    // 책 소개 내용
    bookDesc: string;
    // 공개 독후감 평균 별점
    bookAvgGrde?: number | string | null;
  };
}

/**
 * 독서 진행 상태 타입
 * (완독/읽는중/중단)
 */
export type ReadingStatusType = string;

/**
 * "독후감" 타입
 */
export interface ReportDtoType {
  // 조회 응답에 포함되는 독후감 작성자 사용자 번호이며 등록 요청에서는 생략함
  userNumb?: number;
  // 독서 진행 상태
  reptStat: ReadingStatusType;
  reptStatName?: string;
  // 독서 시작일
  reptStdt: string;
  // 진행 중인 모임 독서에 연결되어 독서 시작일을 수정할 수 없는지 여부
  reptStdtLocked?: boolean;
  // 진행 중인 모임 회차에서 자동 생성된 독후감인지 여부
  clubReadingReport?: boolean;
  // 독서 종료일
  reptEndt: string;
  // 별점
  reptGrde: string;
  // 책장 색상
  reptColr: string;
  reptColrName?: string;
  pubcYsno?: "Y" | "N";
  pubcYsnoName?: string;
  likeCnt?: number;
  likeYsno?: "Y" | "N";
  // 독후감 작성자의 좋아요 알림 사용 여부
  likeAlimYsno?: "Y" | "N";
  // 독후감 작성자의 댓글 및 답글 알림 사용 여부
  replyAlimYsno?: "Y" | "N";

  // 댓글 수
  replCnt?: number
  // 독후감 내용
  reptCntn: string;

  bookTitl: string;
  // 저자
  bookAthr: string;
  // 출판사
  bookPubl: string;
  // 책 isbn
  bookIsbn: string;
  // 선택한 도서 정보와 독후감 원문의 언어 코드
  langCode?: "ko" | "en";
  // 책 표지 이미지
  bookCvim: string;
  // 책 소개 내용
  bookDesc: string;
  // 출간일
  publDate: string;
  // 평균 별점
  bookAvgGrde?: number | string | null;
  // 다른 탭이나 기기의 선행 수정 여부를 확인할 원본 내용 해시
  editVersion?: string;
}

/**
 * 독후감 수정 타입
 */
export interface uptReportType {
  reptNumb: number;

  data: {
    // 독서 진행 상태
    reptStat: ReadingStatusType;
    // 독서 시작일
    reptStdt: string;
    // 독서 종료일
    reptEndt: string;
    // 별점
    reptGrde: string;
    // 책장 색상
    reptColr: string;
    pubcYsno: "Y" | "N";
    // 독후감 내용
    reptCntn: string;
    // 상세 조회 시 받은 원본 내용 해시
    editVersion: string;
  };
}

// 기록 후 백엔드 응답
export interface AddBookResponse {
  code: number;
  message?: string;
  data: number; // reptNumb
}

// 공개된 독후감 타입
export interface PublicReportType {
  reptNumb: number;
  langCode?: "ko" | "en";
  trnsAvaiYsno?: "Y" | "N";
  userNumb: number;
  userNick: string;
  porfPath?: string;
  bookNumb: number;
  reptStat: "READ" | "DONE" | "STOP";
  reptStatName?: string;
  reptGrde: string;
  reptCntn: string;
  pubcYsno: "Y" | "N";
  likeCnt?: number;
  replCnt?: number;
  likeYsno?: "Y" | "N";
  followYsno?: "Y" | "N";
  commentCnt?: number;
}

// 알림 직접 진입에서 공개 독후감 카드와 도서 머리말을 함께 구성하는 타입
export interface PublicReportTargetType extends PublicReportType {
  bookTitl: string;
  bookAthr: string;
  bookIsbn: string;
  bookCvim?: string;
  bookAvgGrde?: number | string | null;
}

// 독후감 상세보기 타입
export interface ReportDetailType extends ReportDtoType {
  // 상세 조회에서는 작성자 본인 독후감만 반환하므로 사용자 번호가 항상 필요함
  userNumb: number;
  // 상세 수정 요청에 반드시 포함할 원본 내용 해시
  editVersion: string;
}

// 홈화면에 보이는 독후감 타입
export interface HomeBookType {
  reptNumb: number;
  bookNumb: number;
  bookTitl: string;
  bookCvim?: string;
  reptStdt?: string;
  reptEndt?: string;
  reptGrde?: string;
  reptColr?: string;
  reptColrName?: string;
  readingYn?: "Y" | "N";
}

// 독후감 수정 시 파라미터 타입
export type SetReportParamsType = {
  // reptNumb: number; // 독후감 번호
  data: uptReportType; // 수정 데이터
};
