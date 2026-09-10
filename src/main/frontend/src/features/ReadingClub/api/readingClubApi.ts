import api, {type SadariRequestConfig} from "@/app/api/axios";
import {
  assertResultDataSuccess,
  type PageData,
  type ResultData,
} from "@/app/api/resultData";
import type {PublicReportSortType} from "@/features/Book/api/bookApi";
import type {BookSearchResultType} from "@/features/Book/types/book.type";
import type {
  ClubApplication, ClubChatMessage,
  ClubBookVotePage, ClubCreateParams, ClubInvitation, ClubMemberExit, ClubMemberProfile,
  ClubReadingCreateParams,
  ClubReadingCreateResult, ClubReadingGoalResult, ClubReadingHistory,
  ClubReadingRoundReportPage, ClubReadingUpdateParams, InviteCandidate,
  OwnerElection, ReadingClub, SentClubInvitation
} from "@/features/ReadingClub/types/club.type.ts";

export type * from "@/features/ReadingClub/types/club.type.ts";

/**
 * 활성 일반 모임원의 자진 탈퇴
 * @param clubNumb 모임 번호
 * @return 처리 응답
 */
export const delMembershipApi = async (clubNumb: number) => {
  const response = await api.delete(`/reading-clubs/${clubNumb}/memberships`);

  return assertResultDataSuccess(response.data);
};

/**
 * 다음 도서 투표 후보 및 정책 상태 요청
 * @param clubNumb 모임 번호
 */
export const getClubBookRecommApi = async (clubNumb: number): Promise<ClubBookVotePage> => {
  const response = await api.get(`/reading-clubs/${clubNumb}/book-recommendations`);

  return assertResultDataSuccess(response.data).data as ClubBookVotePage;
};

/**
 * 다음 도서 후보 등록
 * @param clubNumb 모임 번호
 * @param book 도서 정보
 */
export const createClubBookRecommApi = async (
  clubNumb: number,
  book: BookSearchResultType,
): Promise<number> => {

  // 검색 도서 계약을 서버 도서 DTO 계약으로 변환해 추천 등록
  const response = await api.post(`/reading-clubs/${clubNumb}/book-recommendations`, {
    bookTitl: book.title,
    bookAthr: book.author,
    bookPubl: book.publisher,
    bookIsbn: book.isbn,
    langCode: book.langCode,
    bookCvim: book.image,
    bookDesc: book.description,
    publDate: book.pubdate,
  });

  return assertResultDataSuccess(response.data).data as number;
};

/**
 * 도서 추천 삭제(취소)
 * @param clubNumb 모임 번호
 * @param recmNumb 추천 번호
 */
export const deleteClubBookRecommApi = async (clubNumb: number, recmNumb: number) => {
  const response = await api.delete(`/reading-clubs/${clubNumb}/book-recommendations/${recmNumb}`);

  return assertResultDataSuccess(response.data);
};

/**
 * 다음 도서 투표
 * @param clubNumb 모임 번호
 * @param recmNumb 추천 번호
 */
export const updateClubBookVoteApi = async (clubNumb: number, recmNumb: number) => {
  const response = await api.put(`/reading-clubs/${clubNumb}/book-vote`, {recmNumb});

  return assertResultDataSuccess(response.data);
};

export const getOwnerElectionApi = async (clubNumb: number): Promise<OwnerElection> => {
  // 시작 시점 유권자에게 공개되는 진행 중 모임장 선거를 요청함
  const response = await api.get(`/reading-clubs/${clubNumb}/owner-election`);
  // 공통 성공 응답에서 선거와 후보 목록을 반환함
  return assertResultDataSuccess(response.data).data as OwnerElection;
};

export const updateOwnerVoteApi = async (clubNumb: number, userNumb: number) => {
  // 서버가 현재 투표와 후보 자격을 다시 결정하도록 후보 번호만 전달함
  const response = await api.put(`/reading-clubs/${clubNumb}/owner-election/vote`, {userNumb});
  // 공통 성공 응답을 반환함
  return assertResultDataSuccess(response.data);
};

/**
 * 모임 독서 등록
 * @param clubNumb 모임 번호
 * @param params 선택 도서와 목표 독서 기간
 * @return 생성된 모임 독서 회차 번호
 */
export const createClubReadingApi = async (
  clubNumb: number,
  params: ClubReadingCreateParams,
): Promise<ClubReadingCreateResult> => {

  const response = await api.post(`/reading-clubs/${clubNumb}/setBook`, params);

  return assertResultDataSuccess(response.data).data as ClubReadingCreateResult;
};

/**
 * 현재 모임 독서의 도서와 목표 기간을 수정
 * @param clubNumb 모임 번호
 * @param rondNumb 수정할 회차 번호
 * @param params 선택 도서와 목표 독서 기간
 * @return 수정된 모임 독서 회차 번호
 */
export const updateClubReadingApi = async (
  clubNumb: number,
  rondNumb: number,
  params: ClubReadingUpdateParams,
): Promise<ClubReadingCreateResult> => {

  const response = await api.put(
    `/reading-clubs/${clubNumb}/${rondNumb}/updateClub`,
    params,
  );

  return assertResultDataSuccess(response.data).data as ClubReadingCreateResult;
};

/**
 * 모임 독서 회차 조기 마감
 * @param clubNumb 모임 번호
 * @param rondNumb 마감할 회차 번호
 * @return 완료된 모임 독서 회차 번호
 */
export const completeClubReadingApi = async (
  clubNumb: number,
  rondNumb: number,
): Promise<ClubReadingCreateResult> => {
  const response = await api.put(`/reading-clubs/${clubNumb}/readings/${rondNumb}/completion`);
  return assertResultDataSuccess(response.data).data as ClubReadingCreateResult;
};

/**
 * 내 모임 목록 조회
 */
export const getMyClubListApi = async (): Promise<ReadingClub[]> => {
  const response = await api.get("/reading-clubs/mine");
  return (assertResultDataSuccess(response.data).data as ReadingClub[] | undefined) ?? [];
};

/**
 * 공개 모임 검색
 * @param keyword 모임 검색어
 */
export const getFindClubListApi = async (keyword: string): Promise<ReadingClub[]> => {
  // 검색어를 Query Parameter로 전달
  const response = await api.get("/reading-clubs", {params: {keyword}});
  return (assertResultDataSuccess(response.data).data as ReadingClub[] | undefined) ?? [];
};

/**
 * 모임 상세 조회
 * @param clubNumb 모임 번호
 */
export const getClubDtlApi = async (clubNumb: number): Promise<ReadingClub> => {
  const response = await api.get(`/reading-clubs/${clubNumb}`);
  return assertResultDataSuccess(response.data).data as ReadingClub;
};

/**
 * 활성 모임원 프로필 조회
 * @param clubNumb 모임 번호
 * @return 활성 모임원과 프로필 이미지 경로 목록
 * @throws 모임 상세 조회 실패 또는 접근 권한이 없을 때 발생
 */
export const getClubMemberListApi = async (clubNumb: number): Promise<ClubMemberProfile[]> => {
  const response = await api.get(`/reading-clubs/${clubNumb}/members`);
  return (assertResultDataSuccess(response.data).data as ClubMemberProfile[] | undefined) ?? [];
};

/**
 * 모임 독서 목표 결과 조회
 *
 * @param clubNumb 모임 번호
 * @param rondNumb 조회할 완료 회차 번호(생략하면 최신 회차)
 * @return 모임 독서 목표 결과(결과 없을 때 Null)
 */
export const getReadingGoalResultApi = async (
  clubNumb: number,
  rondNumb?: number,
): Promise<ClubReadingGoalResult | null> => {
  const requestPath = rondNumb === undefined
    ? `/reading-clubs/${clubNumb}/reading-result`
    : `/reading-clubs/${clubNumb}/readings/${rondNumb}/result`;

  const response = await api.get(requestPath);

  return (assertResultDataSuccess(response.data).data as ClubReadingGoalResult | undefined) ?? null;
};

/**
 * 팝업형 독서 회차 결과 읽음 확인 처리
 * @param clubNumb 모임 번호
 * @param rondNumb 확인한 완료 회차 번호
 */
export const uptReadingResultApi = async (clubNumb: number, rondNumb: number): Promise<void> => {
  const response = await api.patch(`/reading-clubs/${clubNumb}/readings/${rondNumb}/result-confirmation`);
  assertResultDataSuccess(response.data);
};

/**
 * 이전 독서 기록 조회
 * @param clubNumb 모임 번호
 * @param page 조회할 페이지 번호
 * @return 종료 회차 도서와 목표 달성 집계 페이지
 */
export const getClubReadingHistoryApi = async (
  clubNumb: number,
  page: number,
): Promise<PageData<ClubReadingHistory>> => {
  const response = await api.get<ResultData<PageData<ClubReadingHistory>>>(
    `/reading-clubs/${clubNumb}/readings`,
    {params: {page}},
  );

  return assertResultDataSuccess(response.data).data ?? {
    list: [],
    page,
    hasNext: false,
  };
};

/**
 * 모임 독서 회차의 독후감 조회
 * @param clubNumb 모임 번호
 * @param rondNumb 모임 독서 회차 번호
 * @param sortType 독후감 정렬 코드
 * @param page 조회할 페이지 번호
 * @return 회차 도서 정보와 완료 독후감 페이지 응답
 */
export const getRoundReportsApi = async (
  clubNumb: number,
  rondNumb: number,
  sortType: PublicReportSortType,
  page: number,
): Promise<ResultData<ClubReadingRoundReportPage>> => {
  const response = await api.get<ResultData<ClubReadingRoundReportPage>>(
    `/reading-clubs/${clubNumb}/readings/${rondNumb}/reports`,
    {params: {sortType, page}},
  );
  return assertResultDataSuccess(response.data);
};

/**
 * 모임 개설
 * @param params 모임 생성 입력값
 */
export const createClubApi = async (params: ClubCreateParams): Promise<ReadingClub> => {
  const response = await api.post("/reading-clubs", params);
  return assertResultDataSuccess(response.data).data as ReadingClub;
};

/**
 * 모임 가입 / 승인 신청
 * @param clubNumb 모임 번호
 * @param answerList 승인 질문 답변
 */
export const joinClubApi = async (clubNumb: number, answerList: string[]): Promise<ReadingClub> => {
  const response = await api.post(`/reading-clubs/${clubNumb}/memberships`, {answerList});
  return assertResultDataSuccess(response.data).data as ReadingClub;
};

/**
 * 모임 가입 승인 전 가입 신청 취소
 * @param clubNumb 모임 번호
 */
export const cancelClubApplicationApi = async (clubNumb: number) => {
  const response = await api.delete(`/reading-clubs/${clubNumb}/applications`);
  return assertResultDataSuccess(response.data);
};

/**
 * 모임장의 맞팔 초대 후보를 조회
 * @param clubNumb 모임 번호
 */
export const getInviteCandidateListApi = async (clubNumb: number): Promise<InviteCandidate[]> => {
  const response = await api.get(`/reading-clubs/${clubNumb}/invitation-candidates`);
  return (assertResultDataSuccess(response.data).data as InviteCandidate[] | undefined) ?? [];
};

/**
 * 모임 정보를 수정
 * @param clubNumb 모임 번호
 * @param params 모임 수정 입력값
 */
export const uptClubApi = async (clubNumb: number, params: ClubCreateParams): Promise<ReadingClub> => {
  const response = await api.put(`/reading-clubs/${clubNumb}`, params);
  return assertResultDataSuccess(response.data).data as ReadingClub;
};

/**
 * 모임 삭제
 * @param clubNumb 모임 번호
 */
export const delClubApi = async (clubNumb: number) => {
  const response = await api.delete(`/reading-clubs/${clubNumb}`);
  return assertResultDataSuccess(response.data);
};

/**
 * 보낸 초대 조회
 * @param clubNumb 모임 번호
 */
export const getSentInvitationListApi = async (clubNumb: number): Promise<SentClubInvitation[]> => {
  const response = await api.get(`/reading-clubs/${clubNumb}/invitations/sent`);
  return (assertResultDataSuccess(response.data).data as SentClubInvitation[] | undefined) ?? [];
};

/**
 * 모임 초대 발송 (맞팔 사용자 한정)
 * @param clubNumb 모임 번호
 * @param userNumbList 초대 대상 사용자 번호
 */
export const inviteClubUsersApi = async (clubNumb: number, userNumbList: number[]) => {
  const response = await api.post(`/reading-clubs/${clubNumb}/invitations`, {userNumbList});
  return assertResultDataSuccess(response.data);
};

/**
 * 모임 초대 취소
 * @param clubNumb 모임 번호
 * @param userNumb 초대 대상 사용자 번호
 */
export const cancelSentInvitationApi = async (clubNumb: number, userNumb: number) => {
  const response = await api.delete(`/reading-clubs/${clubNumb}/invitations/${userNumb}`);
  return assertResultDataSuccess(response.data);
};

/**
 * 받은 모임 초대 조회
 */
export const getClubInvitationListApi = async (): Promise<ClubInvitation[]> => {
  // 유효한 받은 초대 목록을 요청함
  const response = await api.get("/reading-clubs/invitations/received");
  // 받은 초대 목록을 반환함
  return (assertResultDataSuccess(response.data).data as ClubInvitation[] | undefined) ?? [];
};

/**
 * 받은 모임 초대 수락
 * @param clubNumb 모임 번호
 */
export const acceptClubInvitationApi = async (clubNumb: number): Promise<ReadingClub> => {
  const response = await api.put(`/reading-clubs/${clubNumb}/invitations/received`);
  return assertResultDataSuccess(response.data).data as ReadingClub;
};

/**
 * 받은 초대 거절
 * @param clubNumb 모임 번호
 */
export const declineClubInvitationApi = async (clubNumb: number) => {
  const response = await api.delete(`/reading-clubs/${clubNumb}/invitations/received`);
  return assertResultDataSuccess(response.data);
};

/**
 * 모임의 승인 대기 신청 조회
 * @param clubNumb 모임 번호
 */
export const getClubApplicationListApi = async (clubNumb: number): Promise<ClubApplication[]> => {
  const response = await api.get(`/reading-clubs/${clubNumb}/applications`);
  return (assertResultDataSuccess(response.data).data as ClubApplication[] | undefined) ?? [];
};

/**
 * 일반 멤버 퇴장 및 재가입 차단
 * @param clubNumb 모임 번호
 * @param userNumb 퇴장 대상 사용자 번호
 */
export const exitClubMemberApi = async (
  clubNumb: number,
  userNumb: number,
  kickRson: string,
) => {
  const response = await api.delete(`/reading-clubs/${clubNumb}/members/${userNumb}`, {
    data: {kickRson},
  });
  return assertResultDataSuccess(response.data);
};

/**
 * 모임 채팅 조회
 * @param clubNumb 모임 번호
 * @param afterChatNumb 마지막으로 받은 채팅 번호
 */
export const getClubChatListApi = async (
  clubNumb: number,
  afterChatNumb?: number,
): Promise<ClubChatMessage[]> => {
  const response = await api.get(`/reading-clubs/${clubNumb}/chats`, {
    params: {afterChatNumb},
  });
  return (assertResultDataSuccess(response.data).data as ClubChatMessage[] | undefined) ?? [];
};

/**
 * 모임원의 마지막 읽은 채팅 번호를 갱신
 * @author SeungHyeon.Kang
 * @param clubNumb 모임 번호
 * @param chatNumb 마지막으로 확인한 채팅 번호
 * @param viewId 브라우저 채팅 화면별 식별값
 * @param viewing 현재 화면 표시 여부
 * @return 남은 안 읽은 알림 수
 */
export const uptClubChatReadApi = async (
  clubNumb: number,
  chatNumb?: number,
  viewId?: string,
  viewing?: boolean,
): Promise<number> => {
  const requestConfig: SadariRequestConfig = {skipBlockingOperation: true};
  const response = await api.patch(
    `/reading-clubs/${clubNumb}/chats/read`,
    {chatNumb, viewId, viewing},
    requestConfig,
  );
  const data = assertResultDataSuccess(response.data).data as {unreadCnt: number};
  return data.unreadCnt;
};

/**
 * 모임 채팅 전송
 * @param clubNumb 모임 번호
 * @param chatCntn 채팅 본문
 * @param clntUuid 클라이언트 중복 방지 키
 */
export const createClubChatApi = async (
  clubNumb: number,
  chatCntn: string,
  clntUuid: string,
): Promise<ClubChatMessage> => {
  const requestConfig: SadariRequestConfig = {skipBlockingOperation: true};
  const response = await api.post(
    `/reading-clubs/${clubNumb}/chats`,
    {chatCntn, clntUuid},
    requestConfig,
  );
  return assertResultDataSuccess(response.data).data as ClubChatMessage;
};

/**
 * 모임 멤버 퇴장 내역 조회
 * @param clubNumb 모임 번호
 */
export const getMemberExitListApi = async (clubNumb: number): Promise<ClubMemberExit[]> => {
  const response = await api.get(`/reading-clubs/${clubNumb}/members/exits`);
  return (assertResultDataSuccess(response.data).data as ClubMemberExit[] | undefined) ?? [];
};

/**
 * 퇴장 회원의 재가입 제한을 해제
 * @param clubNumb 모임 번호
 * @param userNumb 대상 사용자 번호
 */
export const delMemberRestrictionApi = async (clubNumb: number, userNumb: number) => {
  const response = await api.delete(`/reading-clubs/${clubNumb}/members/${userNumb}/restriction`);
  return assertResultDataSuccess(response.data);
};

/**
 * 모임 가입 신청 승인 또는 거절
 * @param clubNumb 모임 번호
 * @param applNumb 신청 번호
 * @param joinStat 처리 상태
 */
export const decideClubApplicationApi = async (
  clubNumb: number,
  applNumb: number,
  joinStat: "APPROVED" | "REJECTED",
) => {
  const response = await api.put(`/reading-clubs/${clubNumb}/applications/${applNumb}`, {joinStat});
  return assertResultDataSuccess(response.data);
};
