/**
 * fileName       : ClubTypes
 * author         : Hanwon.Jang
 * date           : 2026-09-01
 * description    : 모임 관련 타입 정의 파일
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-09-01        Hanwon.Jang    최초 생성
 * 2026-09-04        SeungHyeon.Kang    모집·채팅·퇴장 타입 추가
 */
import {PageData} from "@/app/api/resultData.ts";
import {PublicReportType} from "@/features/Book/types/book.type.ts";

// 모임 도서 추천
export type ClubBookRecommendation = {
  recmNumb: number;
  bookNumb: number;
  bookTitl: string;
  bookAthr: string;
  bookPubl: string;
  bookIsbn: string;
  bookCvim?: string;
  bookDesc?: string;
  publDate?: string;
  userNick?: string;
  mineYsno: "Y" | "N";
  voteYsno: "Y" | "N";
  voteCnt: number;
};

// 모임 도서 투표 페이지
export type ClubBookVotePage = {
  candidateList: ClubBookRecommendation[];
  voteDeadline?: string;
  dDay?: number;
  canRecommend: boolean;
  hasRecommended: boolean;
  hasVoted: boolean;
};


export type ClubCategory = {
  intrCode: string;
  intrName?: string;
  intrCnam?: string;
  sortOrdr?: number;
};

export type ReadingClub = {
  clubNumb: number;
  ownrNumb?: number;
  ownrNick?: string;
  clubName: string;
  clubCntn: string | null;
  clubVisb: "PUBLIC" | "PRIVATE";
  joinType: "OPEN" | "APPROVAL" | "INVITE";
  clubStat: string;
  rcrtYsno: "Y" | "N";
  maxxMemb: number;
  memberCnt: number;
  invitedCnt: number;
  membStat?: "INVITED" | "ACTIVE" | "EXITED";
  membRole?: "OWNER" | "MEMBER";
  joinStat?: "PENDING" | "REJECTED";
  matchCnt?: number;
  currentRondNumb?: number;
  currentRondStat?: "SCHEDULED" | "READING";
  readingOrdr?: number;
  currentBookNumb?: number;
  currentBookTitl?: string;
  currentBookAthr?: string;
  currentBookCvim?: string;
  currentBookPubl?: string;
  currentBookIsbn?: string;
  currentBookDesc?: string;
  currentPublDate?: string;
  currentBookChangeAllowed?: boolean;
  currentGoalStdt?: string;
  currentGoalEndt?: string;
  currentReportStat?: "READ" | "DONE" | "STOP";
  currentReportNumb?: number;
  currentReportCnt?: number;
  currentGoalAchvCnt?: number;
  currentGoalMembCnt?: number;
  categoryList?: ClubCategory[];
  questionList?: string[];
  regiDate?: string;
};

export type ClubCreateParams = {
  clubName: string;
  clubCntn: string;
  clubVisb: "PUBLIC" | "PRIVATE";
  joinType: "OPEN" | "APPROVAL" | "INVITE";
  maxxMemb: number;
  categoryList: string[];
  questionList: string[];
};

export type InviteCandidate = {
  userNumb: number;
  userNick?: string;
  porfPath?: string;
  intrCntn?: string;
  intrText?: string;
};

export type SentClubInvitation = {
  userNumb: number;
  userNick?: string;
  porfPath?: string;
  intrText?: string;
  invtDate: string;
  exprDate: string;
};

export type ClubInvitation = {
  clubNumb: number;
  clubName: string;
  senderNick?: string;
  invtDate: string;
  exprDate: string;
};

export type ClubApplication = {
  clubNumb: number;
  applNumb: number;
  userNumb: number;
  userNick?: string;
  porfPath?: string;
  questionList: string[];
  answerList: string[];
  joinStat: string;
  applDate: string;
};

export type ClubMemberProfile = {
  userNumb: number;
  userNick?: string;
  porfPath?: string;
  membRole: "OWNER" | "MEMBER";
  mineYsno: "Y" | "N";
};

export type ClubMemberExit = {
  kickNumb: number;
  userNumb: number | null;
  userNick: string | null;
  porfPath: string | null;
  exitDate: string;
  kickRson: string;
  rlesDate: string | null;
  blocYsno: "Y" | "N";
};

export type ClubChatMessage = {
  chatNumb: number;
  clubNumb: number;
  userNumb: number | null;
  userNick: string | null;
  porfPath: string | null;
  chatType: "TEXT";
  chatCntn: string;
  clntUuid: string;
  mineYsno: "Y" | "N";
  unreadCnt: number;
  regiDate: string;
};

export type ClubReadingGoalResult = {
  clubNumb: number;
  rondNumb: number;
  readingOrdr: number;
  bookTitl: string;
  bookAthr?: string;
  bookCvim?: string;
  goalStdt: string;
  goalEndt: string;
  partCnt: number;
  goalAchvCnt: number;
  reportCnt: number;
  myGoalAchieved: boolean;
  achievementMemberList: ClubMemberProfile[];
};

export type ClubReadingHistory = {
  clubNumb: number;
  rondNumb: number;
  bookTitl: string;
  bookAthr?: string;
  bookCvim?: string;
  goalStdt: string;
  goalEndt: string;
  partCnt: number;
  goalAchvCnt: number;
};

export type ClubReadingRoundReportPage = {
  clubNumb: number;
  rondNumb: number;
  readingOrdr: number;
  bookTitl: string;
  bookAthr?: string;
  bookCvim?: string;
  ratingAverage?: number | string | null;
  reportPage: PageData<PublicReportType>;
};

export type ClubReadingCreateParams = {
  bookTitl: string;
  bookAthr: string;
  bookPubl: string;
  bookIsbn: string;
  langCode?: "ko" | "en";
  bookCvim: string;
  bookDesc: string;
  publDate: string;
  goalStdt: string;
  goalEndt: string;
  idemKeyx: string;
};

export type ClubReadingUpdateParams = Omit<ClubReadingCreateParams, "idemKeyx">;

export type ClubReadingCreateResult = {
  rondNumb: number;
};

export type OwnerElectionCandidate = {
  userNumb: number;
  userNick?: string;
  porfPath?: string;
  selected: boolean;
};

export type OwnerElection = {
  clubNumb: number;
  elctNumb: number;
  voteNumb: number;
  voteRoun: number;
  endxDate: string;
  canVote: boolean;
  voted: boolean;
  candidateList: OwnerElectionCandidate[];
};
