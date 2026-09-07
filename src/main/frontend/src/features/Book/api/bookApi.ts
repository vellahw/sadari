/**
 * 도서 검색과 독후감 업무에 필요한 API 요청을 처리함
 *
 * @author HanWon.Jang
 */
import api, { type SadariRequestConfig } from "@/app/api/axios";
import {
  assertResultDataSuccess,
  type PageData,
  type ResultData,
} from "@/app/api/resultData";
import {
  AddBookResponse,
  ReportDetailType,
  ReportDtoType,
  uptReportType,
} from "../types/book.type";
import type { HomeBookType } from "../types/book.type";
import type { PublicReportType } from "../types/book.type";
import type { PublicReportTargetType } from "../types/book.type";

export type PublicReportSortType =
  | "RELATION_DESC"
  | "LATEST_DESC"
  | "GRADE_DESC"
  | "LIKE_DESC";

export type BookCoverColor = {
  reptColr: string;
  reptColrName: string;
};

export type ExistingReportByIsbn = {
  // 동일 ISBN으로 가장 최근에 작성한 독후감 번호
  reptNumb: number;
  // 기존 독후감과 연결된 도서 ISBN
  bookIsbn: string;
};

export type ReportTranslation = {
  reptNumb: number;
  langCode: "ko" | "en";
  trnsCntn: string;
  cached: boolean;
};

/**
 * 신뢰된 도서 검색 표지 대표색과 가장 가까운 책장 색상 코드를 조회함
 *
 * @author HanWon.Jang
 * @param bookCvim 대표색을 분석할 도서 검색 표지 URL
 * @return 표지 대표색과 가장 가까운 BOOK_COLR 코드
 * @throws API 요청 또는 비동기 처리 실패 시 발생
 */
export const getBookCoverColorApi = async (
  bookCvim: string,
): Promise<BookCoverColor | undefined> => {

  const res = await api.post<ResultData<BookCoverColor>>(
    "/book/cover-color",
    { bookCvim },
  );
  // 표지 대표색 분석 성공 응답의 책장 색상 데이터를 반환함
  return assertResultDataSuccess(res.data).data;
};

/**
 * set Report 정보를 설정하거나 등록함
 *
 * @author HanWon.Jang
 * @param data data 입력값
 * @return 반환값이 없음
 * @throws API 요청 또는 비동기 처리 실패 시 발생
 */
export const setReportApi = async (
  data: ReportDtoType,
): Promise<AddBookResponse> => {

  const res = await api.post("/book/setReport", data);
  return assertResultDataSuccess(res.data);
};

/**
 * get Detail 정보를 조회함
 *
 * @author HanWon.Jang
 * @param bookNumb book Numb 입력값
 * @return 처리 결과
 * @throws API 요청 또는 비동기 처리 실패 시 발생
 */
export const getDetailApi = async (bookNumb: number) => {

  const res = await api.get<ResultData<ReportDetailType>>(
    `/book/getBookdetail/${bookNumb}`,
  );
  return assertResultDataSuccess(res.data);
};

/**
 * get Public Reports By Isbn 정보를 조회함
 *
 * @author HanWon.Jang
 * @param isbn isbn 입력값
 * @param sortType 공개 독후감 정렬 코드
 * @param reptStat 공개 독후감 상태 필터
 * @param page 조회할 페이지 번호
 * @return 처리 결과
 * @throws API 요청 또는 비동기 처리 실패 시 발생
 */
export const getPublicReportsByIsbnApi = async (
  isbn: string,
  sortType: PublicReportSortType,
  reptStat: string,
  page: number,
): Promise<ResultData<PageData<PublicReportType>>> => {

  // ISBN과 정렬 및 상태와 현재 페이지를 서버 조회 조건으로 전달함
  const res = await api.get<ResultData<PageData<PublicReportType>>>(
    "/book/publicReports/by-isbn",
    {
      params: { isbn, sortType, reptStat, page },
    },
  );
  // 검증된 공개 독후감 페이지 응답을 반환함
  return assertResultDataSuccess(res.data);
};

/** 알림이 지정한 공개 독후감 한 건과 연결 도서 정보를 조회함 */
export const getPublicReportTargetApi = async (
  reptNumb: number,
): Promise<ResultData<PublicReportTargetType>> => {
  const res = await api.get<ResultData<PublicReportTargetType>>(
    `/book/publicReports/${reptNumb}`,
  );
  return assertResultDataSuccess(res.data);
};

/**
 * 공개 독후감을 현재 사용자 표시 언어로 번역하고 서버 캐시를 재사용함
 *
 * @author HanWon.Jang
 * @param reptNumb 번역할 공개 독후감 번호
 * @return 번역문과 캐시 사용 여부
 * @throws API 요청 또는 공통 응답 검증 실패 시 발생
 */
export const setReportTranslationApi = async (
  reptNumb: number,
): Promise<ReportTranslation> => {
  // 공개 범위와 월간 사용량을 서버에서 다시 검증하는 번역 요청을 전송함
  const response = await api.post<ResultData<ReportTranslation>>(
    `/book/translations/${reptNumb}`,
  );
  // 공통 성공 응답으로 검증된 번역 데이터만 화면에 반환함
  return assertResultDataSuccess(response.data).data as ReportTranslation;
};

/**
 * get Book Rating Average By Isbn 정보를 조회함
 *
 * @author HanWon.Jang
 * @param isbn isbn 입력값
 * @return 처리 결과
 * @throws API 요청 또는 비동기 처리 실패 시 발생
 */
export const getBookRatingAvgApi = async (isbn: string) => {

  const res = await api.get(
    `/book/ratingAverage/by-isbn?isbn=${encodeURIComponent(isbn)}`,
  );
  return assertResultDataSuccess(res.data);
};

/**
 * 로그인 사용자가 동일 ISBN으로 가장 최근에 작성한 독후감을 조회함
 *
 * @author HanWon.Jang
 * @param isbn 기존 독후감을 조회할 도서 ISBN
 * @return 동일 ISBN의 최근 독후감 조회 응답
 * @throws API 요청 또는 비동기 처리 실패 시 발생
 */
export const getMyReportByIsbnApi = async (isbn: string) => {
  // ISBN을 안전하게 전달하여 로그인 사용자의 최근 독후감을 조회함
  const res = await api.get<ResultData<ExistingReportByIsbn | undefined>>(
    `/book/reports/by-isbn?isbn=${encodeURIComponent(isbn)}`,
  );
  // 공통 응답 검증을 통과한 최근 독후감 조회 결과를 반환함
  return assertResultDataSuccess(res.data);
};

export type LikeTargetParams = {
  tagtType: string;
  tagtNumb: number;
};

export type LikeDetail = {
  likeCnt: number;
  likeYsno: "Y" | "N";
};

export type ReportAlimType = "like" | "reply";

export type UptReportAlimParams = {
  reptNumb: number;
  alimType: ReportAlimType;
  useYsno: "Y" | "N";
};

export type ReportAlimResponse = {
  reptNumb: number;
  alimType: ReportAlimType;
  useYsno: "Y" | "N";
};

/**
 * set Public Report Like 정보를 설정하거나 등록함
 *
 * @author HanWon.Jang
 * @param data data 입력값
 * @return 반환값이 없음
 * @throws API 요청 또는 비동기 처리 실패 시 발생
 */
export const setPublicReportLikeApi = (
  data: LikeTargetParams,
): Promise<ResultData<LikeDetail>> => {
  const requestConfig: SadariRequestConfig = {
    skipBlockingOperation: true,
  };

  // 즉시 반응형 좋아요는 전역 차단 로딩 없이 CSRF가 적용된 기존 API 인스턴스로 요청함
  return api.post<ResultData<LikeDetail>>("/social/like", data, requestConfig).then((res) => {

    return assertResultDataSuccess(res.data);
  });
};

export type BookListParams = {
  bookKeyword?: string;
  sortType?: string;
  page?: number;
};

/**
 * 로그인 사용자가 작성한 독후감의 유형별 알림 사용 여부를 변경함
 *
 * @author SeungHyeon.Kang
 * @param params 독후감 번호, 알림 유형과 변경할 사용 여부
 * @return 변경된 독후감 알림 설정
 * @throws API 요청 또는 응답 검증 실패 시 발생
 */
export const uptReportAlimApi = async (
  params: UptReportAlimParams,
): Promise<ResultData<ReportAlimResponse>> => {
  // URL에는 독후감과 알림 유형을, 본문에는 변경할 사용 여부만 전달함
  const res = await api.put<ResultData<ReportAlimResponse>>(
    `/book/${params.reptNumb}/notification-settings/${params.alimType}`,
    { useYsno: params.useYsno },
  );
  // 공통 응답 검증을 통과한 변경 결과를 반환함
  return assertResultDataSuccess(res.data);
};

/**
 * get List 정보를 조회함
 *
 * @author HanWon.Jang
 * @param params params 입력값
 * @return 처리 결과
 * @throws API 요청 또는 비동기 처리 실패 시 발생
 */
export const getListApi = async (
  params: BookListParams = {},
): Promise<ResultData<PageData<HomeBookType>>> => {

  const res = await api.get<ResultData<PageData<HomeBookType>>>(
    "/book/getBookList",
    { params },
  );
  // 검증된 홈 독후감 페이지 응답을 반환함
  return assertResultDataSuccess(res.data);
};

/**
 * upt Report 정보를 수정함
 *
 * @author HanWon.Jang
 * @param props props 입력값
 * @return 반환값이 없음
 * @throws API 요청 또는 비동기 처리 실패 시 발생
 */
export const uptReportApi = async ({
  reptNumb,
  data,
}: uptReportType): Promise<AddBookResponse> => {

  const res = await api.put(`/book/uptReport/${reptNumb}`, data);
  return assertResultDataSuccess(res.data);
};

export type UptReptStatusGradeParams = {
  reptNumb: number;
  data: {
    reptStat: string;
    reptGrde: string;
    pubcYsno: "Y" | "N";
    reptEndt?: string;
  };
};

/**
 * 마이페이지에서 독서 상태와 별점 및 공개 여부를 빠르게 수정함
 *
 * @author HanWon.Jang
 * @param props props 입력값
 * @return 반환값이 없음
 * @throws API 요청 또는 비동기 처리 실패 시 발생
 */
export const uptReptStatusGradeApi = async ({
  reptNumb,
  data,
}: UptReptStatusGradeParams): Promise<AddBookResponse> => {

  const res = await api.put(`/book/uptReport/status-grade/${reptNumb}`, data);
  return assertResultDataSuccess(res.data);
};

/**
 * del Report 정보를 삭제함
 *
 * @author HanWon.Jang
 * @param reptNumb rept Numb 입력값
 * @return 반환값이 없음
 * @throws API 요청 또는 비동기 처리 실패 시 발생
 */
export const delReportApi = async (reptNumb: number) => {

  const res = await api.delete(`/book/delReport/${reptNumb}`);
  return assertResultDataSuccess(res.data);
};
