import api from "@/app/api/axios";
import { assertResultDataSuccess } from "@/app/api/resultData";
import type { PageData, ResultData } from "@/app/api/resultData";
import type { ReplyTargetType } from "@/features/reply/types/reply.types";

export type FeedItem = {
  tagtType: ReplyTargetType;
  tagtNumb: number;
  userNumb: number;
  userNick: string;
  meYsno: "Y" | "N";
  porfPath?: string;
  activityDate: string;
  reptNumb?: number;
  langCode?: "ko" | "en";
  trnsAvaiYsno?: "Y" | "N";
  reptStat?: "READ" | "DONE" | "STOP";
  reptStatName?: string;
  reptGrde?: string;
  reptCntn?: string;
  bookNumb?: number;
  bookTitl?: string;
  bookAthr?: string;
  bookCvim?: string;
  contentImagePath?: string;
  contentImageDisplayPath?: string;
  likeCnt: number;
  likeYsno: "Y" | "N";
  replCnt: number;
};

/**
 * 로그인 사용자 본인과 팔로우하는 활성 사용자의 공개 활동 피드 한 페이지를 조회함
 *
 * @author HanWon.Jang
 * @param page 조회할 피드 페이지 번호
 * @return 본인과 팔로잉 피드 목록 및 현재 페이지와 다음 페이지 여부
 * @throws API 요청 또는 공통 응답 검증 실패 시 발생
 */
export const getFeedPageApi = async (page: number): Promise<PageData<FeedItem>> => {
  // 서버가 인증 사용자와 페이지 조건으로 제한한 본인 및 팔로잉 피드를 조회함
  const response = await api.get<ResultData<PageData<FeedItem>>>("/feed", { params: { page } });
  // 공통 성공 코드가 검증된 피드 페이지 데이터만 화면에 반환함
  return assertResultDataSuccess(response.data).data as PageData<FeedItem>;
};

/**
 * 알림 링크가 지정한 현재 공개 피드 대상 한 건을 조회함
 *
 * @author SeungHyeon.Kang
 * @param tagtType 조회할 피드 대상 유형
 * @param tagtNumb 조회할 피드 대상 번호
 * @return 댓글 목록을 열 대상 피드 항목
 * @throws API 요청 또는 공통 응답 검증 실패 시 발생
 */
export const getFeedTargetApi = async (
  tagtType: ReplyTargetType,
  tagtNumb: number,
): Promise<FeedItem> => {
  // 경로 식별값을 인코딩해 서버의 현재 공개 상태 검증이 적용된 피드 한 건을 조회함
  const response = await api.get<ResultData<FeedItem>>(
    `/feed/items/${encodeURIComponent(tagtType)}/${tagtNumb}`,
  );
  // 공통 성공 코드가 검증된 피드 항목만 화면에 반환함
  return assertResultDataSuccess(response.data).data as FeedItem;
};
