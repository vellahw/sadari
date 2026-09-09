import { getApiErrorMessage } from "@/app/api/resultData";
import { sweetConfirm, sweetError } from "@/app/lib/sweetAlert/sweetAlert";
import { message } from "@/app/messages/message";
import { formatDashedDateToDot, formatDateValue } from "@/app/utils/dateUtil";
import BackgroundImage from "@/components/BackgroundImage/BackgroundImage";
import { FullscreenImageButton } from "@/components/ImageViewer/FullscreenImageViewer";
import InfiniteScrollTrigger from "@/components/InfiniteScroll/InfiniteScrollTrigger";
import Loading from "@/components/Loading/Loading";
import AnimatedReportContent from "@/components/ReportList/AnimatedReportContent";
import * as reportListStyles from "@/components/ReportList/ReportListView.css";
import SearchMatchText from "@/components/Search/SearchMatchText/SearchMatchText";
import * as stickyStyles from "@/components/Search/StickySearchBar/StickySearchBar.css";
import { useStickySearch } from "@/components/Search/StickySearchBar/useStickySearch";
import UserActionMenu from "@/components/UserActionMenu/UserActionMenu";
import type { SafetyReportTarget } from "@/components/UserActionMenu/userActionMenu.types";
import {
  setPublicReportLikeApi,
  setReportTranslationApi,
} from "@/features/Book/api/bookApi";
import {
  REPORT_STATUS_DONE,
  REPORT_STATUS_STOP,
} from "@/features/Book/constants/reportForm";
import { REPORT_CONTENT_PREVIEW_LENGTH } from "@/features/Book/utils/reportListView";
import { getFeedPageApi, getFeedTargetApi, type FeedItem } from "@/features/Feed/api/feedApi";
import type { ReplyTargetType } from "@/features/reply/types/reply.types";
import ReplySheet from "@/features/reply/ReplySheet";
import LikeUserListButton from "@/features/Social/components/LikeUserListButton";
import * as userListStyles from "@/features/Social/components/LikeUserListButton.css";
import {
  delSocialFollowApi,
  getUserSearchPageApi,
  setSocialFollowApi,
  type FollowUser,
} from "@/features/Social/api/socialApi";
import { isFollowedByMe } from "@/features/Social/utils/followStatus";
import ProfileImage, { DEFAULT_PROFILE_IMAGE } from "@/features/User/components/ProfileImage";
import { clsx } from "clsx";
import {
  type ChangeEvent,
  type FormEvent,
  type ReactNode,
  type SyntheticEvent,
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import * as homeStyles from "../Home/Home.css";
import * as styles from "./FeedPage.css";

/**
 * 독서 상태 코드에 대응하는 공통 독후감 카드의 배지 스타일을 반환함
 *
 * @author HanWon.Jang
 * @param reptStat 피드 독후감의 독서 상태 코드
 * @return 독서 상태별 배지 클래스명
 */
const getStatusClassName = (reptStat?: FeedItem["reptStat"]): string => {
  // 완독한 독후감은 브랜드 색상의 완료 배지를 사용함
  if (reptStat === REPORT_STATUS_DONE) {
    // 다른 사람 독후감 카드와 같은 완독 배지 클래스를 반환함
    return reportListStyles.statusDone;
  }

  // 독서를 중단한 독후감은 회색의 중단 배지를 사용함
  if (reptStat === REPORT_STATUS_STOP) {
    // 다른 사람 독후감 카드와 같은 중단 배지 클래스를 반환함
    return reportListStyles.statusStopped;
  }

  // 나머지 상태에는 다른 사람 독후감 카드와 같은 독서 중 배지를 반환함
  return reportListStyles.statusReading;
};

type FeedLikeDetail = Pick<FeedItem, "likeCnt" | "likeYsno">;

// 알림 링크로 직접 조회할 수 있는 피드 대상 유형
const FEED_TARGET_TYPES: ReplyTargetType[] = ["REPORT", "PROFILE_IMAGE", "BACKGROUND_IMAGE"];
// 연속 입력마다 서버 조회가 중복되지 않도록 적용할 사용자 검색 대기 시간
const USER_SEARCH_DELAY_MS = 250;

/**
 * 피드 대상 식별값이 현재 갱신 대상과 일치하는지 판정함
 *
 * @author HanWon.Jang
 * @param candidate 비교할 피드 항목
 * @param target 갱신 대상 피드 항목
 * @return 피드 유형과 대상 번호가 모두 같으면 true
 */
const isSameFeedTarget = (candidate: FeedItem, target: FeedItem): boolean => {
  // 유형과 번호가 모두 일치해야 같은 좋아요 대상으로 판정함
  return candidate.tagtType === target.tagtType && candidate.tagtNumb === target.tagtNumb;
};

/**
 * 지정한 피드 대상의 좋아요 수와 로그인 사용자 좋아요 여부를 불변 배열로 갱신함
 *
 * @author HanWon.Jang
 * @param current 현재 화면에 누적된 피드 목록
 * @param target 좋아요 상태를 갱신할 피드 항목
 * @param detail 화면에 반영할 좋아요 수와 여부
 * @return 지정한 대상만 좋아요 상태가 변경된 새 피드 목록
 */
const getUpdatedLikeItems = (
  current: FeedItem[],
  target: FeedItem,
  detail: FeedLikeDetail,
): FeedItem[] => {
  /**
   * 현재 순회 항목이 좋아요 대상이면 전달받은 서버 또는 낙관적 상태를 병합함
   *
   * @author HanWon.Jang
   * @param candidate 현재 순회 중인 피드 항목
   * @return 대상 항목이면 좋아요 상태가 갱신된 항목이며 아니면 기존 항목
   */
  const updateCandidate = (candidate: FeedItem): FeedItem => {
    // 다른 피드 대상은 기존 상태를 그대로 유지함
    if (!isSameFeedTarget(candidate, target)) {
      // 현재 대상과 일치하지 않는 피드 항목을 반환함
      return candidate;
    }

    // 현재 대상에는 검증된 좋아요 상태를 병합해 새 객체로 반환함
    return { ...candidate, ...detail };
  };

  // React 상태 불변성을 유지하도록 대상 항목만 새 객체로 치환함
  return current.map(updateCandidate);
};

/**
 * 최초 페이지는 목록을 교체하고 추가 페이지는 기존 목록 뒤에 연결함
 *
 * @author HanWon.Jang
 * @param current 현재 화면에 누적된 목록
 * @param incoming 서버가 반환한 페이지 목록
 * @param targetPage 조회한 페이지 번호
 * @return 페이지 위치에 맞게 구성한 새 목록
 */
const getMergedPageItems = <T,>(
  current: T[],
  incoming: T[],
  targetPage: number,
): T[] => {
  // 최초 페이지는 이전 조회 결과를 남기지 않고 서버 목록으로 교체함
  if (targetPage === 1) {
    // 첫 페이지에서 받은 피드 목록을 반환함
    return incoming;
  }

  // 추가 페이지는 기존 목록과 서버 목록을 새 배열로 연결해 반환함
  return [...current, ...incoming];
};

/**
 * 지정한 독후감 피드의 본문 펼침 상태를 불변 객체로 반전함
 *
 * @author HanWon.Jang
 * @param current 피드 대상별 현재 펼침 상태
 * @param tagtNumb 펼침 상태를 변경할 피드 대상 번호
 * @return 지정한 피드의 펼침 상태만 반전된 새 객체
 */
const getToggledReports = (
  current: Record<number, boolean>,
  tagtNumb: number,
): Record<number, boolean> => {
  // 다른 피드의 펼침 상태는 유지하고 선택한 대상만 반전해 반환함
  return {
    ...current,
    [tagtNumb]: !current[tagtNumb],
  };
};

/**
 * 독후감 본문 펼침 상태에 맞는 접근성 동작 문구를 반환함
 *
 * @author HanWon.Jang
 * @param isExpanded 독후감 본문 펼침 여부
 * @return 현재 상태에서 실행할 접기 또는 펼치기 문구
 */
const getExpandActionLabel = (isExpanded: boolean): string => {
  // 펼쳐진 본문에는 내용을 접는 동작 문구를 제공함
  if (isExpanded) {
    // "접기"
    return message("frontend.common.collapse");
  }

  // "펼치기"
  return message("frontend.book.publicReports.expand");
};

/**
 * 로그인 사용자 본인과 팔로잉 사용자의 공개 독후감 및 사진 변경 활동을 카드 목록으로 표시함
 *
 * @author HanWon.Jang
 * @return 본인과 팔로잉 사용자의 활동 피드 화면
 */
const FeedPage = () => {
  // 피드 카드의 프로필 및 도서 화면 이동에 공통 라우터 함수를 사용함
  const navigate = useNavigate();
  // 홈과 같은 검색 영역의 실제 고정 상태를 공통 감지 로직으로 조회함
  const { isSticky, sentinelRef } = useStickySearch();
  // 알림 링크에 포함된 원본 콘텐츠 유형과 번호를 조회함
  const [searchParams] = useSearchParams();
  // 서버에서 페이지 단위로 받은 피드 항목을 화면 목록 상태로 관리함
  const [items, setItems] = useState<FeedItem[]>([]);
  // 알림 링크로 직접 연 피드 항목을 일반 페이지 목록과 독립적으로 관리함
  const [focusedItem, setFocusedItem] = useState<FeedItem | null>(null);
  // 마지막으로 조회에 성공한 피드 페이지 번호를 관리함
  const [page, setPage] = useState(1);
  // 목록 하단에서 다음 피드 페이지를 조회할 수 있는지 관리함
  const [hasNext, setHasNext] = useState(false);
  // 최초 조회와 추가 조회에서 중복 요청을 차단할 로딩 상태를 관리함
  const [isLoading, setIsLoading] = useState(true);
  // 최초 피드 조회 실패 시 화면에 표시할 안전한 오류 문구를 관리함
  const [error, setError] = useState("");
  // 댓글 목록을 열어 확인할 현재 피드 항목을 관리함
  const [replyItem, setReplyItem] = useState<FeedItem | null>(null);
  // 독후감 피드별 본문 펼침 여부를 대상 번호 기준으로 관리함
  const [expandedReports, setExpandedReports] = useState<Record<number, boolean>>({});
  // 독후감 번호별로 최초 조회한 번역문을 현재 화면에서 재사용함
  const [translations, setTranslations] = useState<Record<number, string>>({});
  // 피드 카드별 원문 또는 번역문 표시 상태를 관리함
  const [translatedReports, setTranslatedReports] = useState<Record<number, boolean>>({});
  // 같은 화면에서 중복 번역 요청을 막기 위해 처리 중인 독후감 번호를 관리함
  const [pendingReportNumb, setPendingReportNumb] = useState<number>();
  // 피드 상단 검색 입력에 표시할 닉네임 검색어를 관리함
  const [userKeyword, setUserKeyword] = useState("");
  // 현재 자동 검색 결과에 적용한 닉네임 검색어를 관리함
  const [appliedUserKeyword, setAppliedUserKeyword] = useState("");
  // 관계 우선순위가 적용된 활성 사용자 검색 결과를 관리함
  const [searchUsers, setSearchUsers] = useState<FollowUser[]>([]);
  // 마지막으로 조회에 성공한 사용자 검색 페이지 번호를 관리함
  const [userPage, setUserPage] = useState(1);
  // 사용자 검색 결과의 다음 페이지 존재 여부를 관리함
  const [hasNextUser, setHasNextUser] = useState(false);
  // 사용자 검색 첫 페이지의 조회 진행 상태를 관리함
  const [isUserLoading, setIsUserLoading] = useState(false);
  // 사용자 검색 추가 페이지의 조회 진행 상태를 관리함
  const [isNextUserLoading, setIsNextUserLoading] = useState(false);
  // 관계 변경이 진행 중인 검색 사용자 번호를 관리함
  const [updatingUserNumb, setUpdatingUserNumb] = useState<number | null>(null);
  // 같은 피드 대상의 좋아요 요청이 동시에 실행되지 않도록 진행 키를 보관함
  const pendingLikeKeysRef = useRef(new Set<string>());
  // 이전 검색 응답이 최신 검색 결과를 덮지 않도록 요청 순번을 보관함
  const userSearchRequestRef = useRef(0);
  // 비동기 관계 변경 뒤 같은 검색어가 유지되는지 확인할 최신 검색어를 보관함
  const appliedUserKeywordRef = useRef("");
  // 알림 링크가 전달한 피드 대상 유형 문자열을 조회함
  const targetTypeParam = searchParams.get("tagtType");
  // 알림 링크가 전달한 피드 대상 번호를 숫자로 변환함
  const targetNumbParam = Number(searchParams.get("tagtNumb"));
  // 알림이 지정한 댓글 번호를 안전한 양수 정수로 변환함
  const requestedReplyNumb = Number(searchParams.get("replNumb"));
  const focusReplNumb = Number.isSafeInteger(requestedReplyNumb) && requestedReplyNumb > 0
    ? requestedReplyNumb
    : undefined;
  // 허용된 유형과 양의 번호가 모두 있으면 단건 피드 조회 대상으로 판정함
  const hasFeedTarget = FEED_TARGET_TYPES.includes(targetTypeParam as ReplyTargetType)
    && Number.isSafeInteger(targetNumbParam)
    && targetNumbParam > 0;

  /**
   * 요청한 피드 페이지를 조회해 최초 목록 또는 추가 목록으로 화면에 반영함
   *
   * @author HanWon.Jang
   * @param targetPage 조회할 피드 페이지 번호
   * @return 피드 페이지 반영 완료 Promise
   */
  const loadPage = useCallback(async (targetPage: number): Promise<void> => {
    // 피드 조회 중 중복 추가 요청을 차단하고 공통 로딩 상태를 표시함
    setIsLoading(true);
    // 새 조회가 시작되면 이전 오류 문구를 제거함
    setError("");

    // 피드 조회 성공과 실패를 화면 상태별로 분리해 처리함
    try {
      // 인증 사용자 기준으로 공개 범위가 적용된 피드 페이지를 조회함
      const data = await getFeedPageApi(targetPage);

      /**
       * 서버 페이지를 최초 또는 추가 조회 위치에 맞춰 현재 피드 목록과 병합함
       *
       * @author HanWon.Jang
       * @param current 현재 화면에 누적된 피드 목록
       * @return 조회한 페이지가 반영된 새 피드 목록
       */
      const mergeCurrentItems = (current: FeedItem[]): FeedItem[] => {
        // 페이지 위치에 맞게 기존 목록과 서버 목록을 결합해 반환함
        return getMergedPageItems(current, data.list, targetPage);
      };

      // 첫 페이지는 교체하고 추가 페이지는 기존 목록 뒤에 연결함
      setItems(mergeCurrentItems);
      // 마지막으로 조회에 성공한 서버 페이지 번호를 저장함
      setPage(data.page);
      // 서버가 판정한 다음 페이지 존재 여부를 저장함
      setHasNext(data.hasNext);
    }

    // 피드 조회 실패 시 기존 목록을 유지하고 안전한 화면 문구를 설정함
    catch (loadError) {
      // "피드를 불러오지 못했어요."
      const fallbackMessage = message("frontend.feed.loadFailed");
      // 원시 오류 대신 서버 또는 공통 피드 조회 실패 문구를 선택함
      const errorMessage = getApiErrorMessage(loadError, fallbackMessage);
      // 최초 조회 실패 화면에 검증된 오류 문구를 표시함
      setError(errorMessage);
    }

    // 성공과 실패 모두 현재 피드 조회의 로딩 상태를 종료함
    finally {
      // 다음 피드 페이지를 조회할 수 있도록 로딩 상태를 해제함
      setIsLoading(false);
    }
  }, []);

  /**
   * 피드 화면 진입 시 중복 초기화 없이 첫 페이지 조회를 시작함
   *
   * @author HanWon.Jang
   * @return 반환값이 없음
   */
  const loadInitialPage = useCallback((): void => {
    // 화면 최초 렌더링에서 첫 피드 페이지를 비동기로 조회함
    void loadPage(1);
  }, [loadPage]);

  // 화면 진입과 피드 조회 함수 변경 시 첫 페이지를 다시 조회함
  useEffect(loadInitialPage, [loadInitialPage]);

  /**
   * 알림 링크가 지정한 피드 한 건을 조회하고 해당 댓글 목록을 자동으로 엶
   *
   * @author SeungHyeon.Kang
   * @return 알림 대상 피드 조회 완료 Promise
   */
  const loadTargetFeed = useCallback(async (): Promise<void> => {
    // 유효한 대상 식별값이 없는 일반 피드 진입은 단건 조회를 실행하지 않음
    if (!hasFeedTarget) {
      // 이전 알림 대상 강조 상태를 제거하고 일반 피드 목록만 유지함
      setFocusedItem(null);
      // 알림 대상 쿼리가 제거되면 자동으로 열었던 댓글 목록도 닫음
      setReplyItem(null);
      return;
    }

    // 허용 유형 검사로 검증된 문자열을 댓글 대상 유형으로 사용함
    const targetType = targetTypeParam as ReplyTargetType;

    // 대상 조회 성공과 만료 또는 접근 제한 실패를 분리해 처리함
    try {
      // 팔로우 여부와 무관하게 현재 공개 상태를 검증한 알림 이동 대상 피드 한 건을 조회함
      const targetItem = await getFeedTargetApi(targetType, targetNumbParam);
      // 페이지 위치와 무관하게 알림 대상 카드를 목록 맨 앞에서 확인할 수 있도록 저장함
      setFocusedItem(targetItem);
      // 알림을 누른 사용자가 즉시 댓글을 확인할 수 있도록 대상 댓글 목록을 엶
      setReplyItem(targetItem);
    }

    // 교체되거나 공개 범위에서 제외된 대상이면 일반 피드는 유지하고 안전한 안내만 표시함
    catch (targetError) {
      // 만료된 대상 카드가 화면에 남지 않도록 직접 조회 상태를 초기화함
      setFocusedItem(null);
      // "이 알림의 피드를 열 수 없어요."
      const targetUnavailableTitle = message("frontend.feed.targetUnavailable");
      // "삭제되었거나 더 이상 볼 수 없는 소식이에요."
      const targetUnavailableMessage = message("frontend.feed.targetUnavailableDetail");
      // 원시 오류 대신 서버 또는 대상 만료 안내 문구를 선택함
      const targetErrorMessage = getApiErrorMessage(targetError, targetUnavailableMessage);
      // 사용자가 일반 피드 화면에 머문 상태에서 대상 조회 실패 원인을 안내함
      await sweetError(targetUnavailableTitle, targetErrorMessage);
    }
  }, [hasFeedTarget, targetNumbParam, targetTypeParam]);

  /**
   * 알림 대상 식별값이 변경될 때 단건 피드 조회를 시작함
   *
   * @author SeungHyeon.Kang
   * @return 반환값이 없음
   */
  const startTargetFeedLoad = useCallback((): void => {
    // 유효한 알림 대상이면 비동기 단건 조회를 시작함
    void loadTargetFeed();
  }, [loadTargetFeed]);

  // 일반 진입 또는 알림 대상 변경에 맞춰 직접 조회 상태와 댓글 목록을 갱신함
  useEffect(startTargetFeedLoad, [startTargetFeedLoad]);

  /**
   * 닉네임 검색어에 해당하는 활성 사용자 페이지를 관계 우선순위와 함께 조회함
   *
   * @author HanWon.Jang
   * @param keyword 적용할 닉네임 검색어
   * @param targetPage 조회할 사용자 검색 페이지 번호
   * @return 사용자 검색 페이지 반영 완료 Promise
   */
  const loadUserPage = useCallback(async (keyword: string, targetPage: number): Promise<void> => {
    // 현재 요청보다 늦게 끝나는 이전 응답을 구분할 검색 순번을 발급함
    const requestId = ++userSearchRequestRef.current;

    // 첫 페이지와 추가 페이지의 로딩 상태를 분리해 기존 결과 표시 여부를 결정함
    if (targetPage === 1) {
      // 새 검색의 첫 페이지 로딩 상태를 표시함
      setIsUserLoading(true);
    }

    // 추가 페이지는 기존 사용자 목록을 유지한 채 하단 로딩 상태만 표시함
    else {
      // 사용자 검색 결과 하단에 추가 조회 상태를 표시함
      setIsNextUserLoading(true);
    }

    // 사용자 검색 성공과 실패 및 오래된 응답을 분리해 처리함
    try {
      // 활성 상태와 로그인 사용자 관계가 적용된 닉네임 검색 페이지를 조회함
      const data = await getUserSearchPageApi(keyword, targetPage);

      // 더 최신 검색이 시작됐으면 현재 응답으로 화면을 덮지 않음
      if (requestId !== userSearchRequestRef.current) {
        // 오래된 검색 응답의 화면 반영을 종료함
        return;
      }

      /**
       * 첫 검색 페이지는 교체하고 추가 페이지는 기존 활성 사용자 목록 뒤에 연결함
       *
       * @author HanWon.Jang
       * @param current 현재 화면에 누적된 활성 사용자 목록
       * @return 조회한 페이지가 반영된 새 활성 사용자 목록
       */
      const mergeCurrentUsers = (current: FollowUser[]): FollowUser[] => {
        // 페이지 위치에 맞게 기존 사용자와 새 검색 결과를 결합해 반환함
        return getMergedPageItems(current, data.list, targetPage);
      };

      // 관계 우선순위가 적용된 현재 사용자 페이지를 목록에 반영함
      setSearchUsers(mergeCurrentUsers);
      // 마지막으로 조회에 성공한 사용자 검색 페이지 번호를 저장함
      setUserPage(data.page);
      // 서버가 판정한 사용자 검색 다음 페이지 여부를 저장함
      setHasNextUser(data.hasNext);
    }

    // 최신 사용자 검색 요청만 안전한 공통 오류 문구로 안내함
    catch (searchError) {
      // 더 최신 검색이 진행 중이면 이전 요청의 오류를 사용자에게 표시하지 않음
      if (requestId !== userSearchRequestRef.current) {
        // 오래된 검색 오류의 안내를 종료함
        return;
      }

      // "사용자 검색에 실패했어요."
      const fallbackMessage = message("frontend.feed.userSearch.failed");
      // 원시 오류 대신 서버 또는 공통 사용자 검색 실패 문구를 선택함
      const errorMessage = getApiErrorMessage(searchError, fallbackMessage);
      // "조회에 실패했습니다."
      await sweetError(message("frontend.alert.loadFailedTitle"), errorMessage);
    }

    // 최신 검색 요청만 해당 페이지의 로딩 상태를 종료함
    finally {
      // 오래된 요청은 현재 검색의 로딩 상태를 변경하지 않음
      if (requestId !== userSearchRequestRef.current) {
        // 최신 요청이 로딩 상태를 정리하도록 종료함
        return;
      }

      // 첫 페이지 조회가 끝나면 검색 결과 영역의 로딩 상태를 해제함
      if (targetPage === 1) {
        // 사용자 검색 첫 페이지 로딩 상태를 해제함
        setIsUserLoading(false);
      }

      // 추가 페이지 조회가 끝나면 하단 로딩 상태를 해제함
      else {
        // 사용자 검색 추가 페이지 로딩 상태를 해제함
        setIsNextUserLoading(false);
      }
    }
  }, []);

  /**
   * 닉네임 입력이 한 글자 이상이면 짧게 대기한 뒤 사용자 검색 결과를 자동으로 조회함
   *
   * @author HanWon.Jang
   * @return 다음 입력 또는 화면 해제 시 예약 검색을 취소할 정리 함수
   */
  const startLiveUserSearch = useCallback((): (() => void) | undefined => {
    // 검색어 양끝 공백을 제거해 화면과 서버에 같은 검색 조건을 적용함
    const normalizedKeyword = userKeyword.trim();
    // 현재 입력어를 검색 결과 표시 조건으로 즉시 저장함
    setAppliedUserKeyword(normalizedKeyword);
    // 비동기 관계 변경도 최신 입력어와 일치할 때만 결과를 다시 조회하도록 저장함
    appliedUserKeywordRef.current = normalizedKeyword;
    // 이전 검색 응답이 새 입력 결과를 덮지 못하도록 즉시 무효화함
    userSearchRequestRef.current += 1;
    // 새 입력은 이전 사용자 결과와 페이지 상태를 초기화함
    setSearchUsers([]);
    // 새 검색의 성공 페이지를 첫 페이지 기준으로 초기화함
    setUserPage(1);
    // 새 검색 전에는 다음 페이지가 없는 상태로 초기화함
    setHasNextUser(false);
    // 새 첫 페이지 검색에서는 추가 페이지 로딩 상태를 제거함
    setIsNextUserLoading(false);

    // 입력값이 비어 있으면 서버 요청 없이 일반 피드를 바로 표시함
    if (!normalizedKeyword) {
      // 일반 피드에서는 사용자 검색 로딩 상태를 표시하지 않음
      setIsUserLoading(false);
      // 예약할 검색이 없으므로 Effect 정리 함수를 반환하지 않음
      return undefined;
    }

    // 입력 직후 검색 결과 영역에 첫 페이지 조회 상태를 표시함
    setIsUserLoading(true);

    /**
     * 입력 대기가 끝난 현재 닉네임으로 활성 사용자 첫 페이지 조회를 시작함
     *
     * @author HanWon.Jang
     * @return 반환값이 없음
     */
    const loadCurrentKeyword = (): void => {
      // 한 글자 이상의 최신 닉네임으로 관계순 사용자 검색을 실행함
      void loadUserPage(normalizedKeyword, 1);
    };

    // 빠른 연속 입력을 한 번의 사용자 검색 요청으로 합침
    const timerId = window.setTimeout(loadCurrentKeyword, USER_SEARCH_DELAY_MS);

    /**
     * 다음 입력 또는 화면 해제 시 아직 실행되지 않은 사용자 검색을 취소함
     *
     * @author HanWon.Jang
     * @return 반환값이 없음
     */
    const cancelPendingSearch = (): void => {
      // 최신 입력만 조회되도록 이전 입력의 예약 타이머를 해제함
      window.clearTimeout(timerId);
    };

    // React Effect가 다음 입력 전에 예약된 이전 검색을 취소하도록 정리 함수를 반환함
    return cancelPendingSearch;
  }, [loadUserPage, userKeyword]);

  // 한 글자 이상의 닉네임 입력 변경에 맞춰 사용자 결과를 자동 갱신함
  useEffect(startLiveUserSearch, [startLiveUserSearch]);

  /**
   * 피드 사용자 검색 입력값을 현재 입력 상태에 반영함
   *
   * @author HanWon.Jang
   * @param event 닉네임 검색 입력 변경 이벤트
   * @return 반환값이 없음
   */
  const handleUserKeyword = (event: ChangeEvent<HTMLInputElement>): void => {
    // 사용자가 입력한 닉네임 검색어를 검색 입력에 표시함
    setUserKeyword(event.target.value);
  };

  /**
   * 자동 사용자 검색 폼 제출 시 브라우저 페이지 이동을 막음
   *
   * @author HanWon.Jang
   * @param event 사용자 검색 폼 제출 이벤트
   * @return 반환값이 없음
   */
  const handleUserSearch = (event: FormEvent<HTMLFormElement>): void => {
    // 브라우저의 폼 이동 없이 현재 피드 화면에서 검색 결과를 전환함
    event.preventDefault();
  };

  /**
   * 현재 사용자 검색의 다음 페이지를 조회함
   *
   * @author HanWon.Jang
   * @return 반환값이 없음
   */
  const loadMoreUser = (): void => {
    // 검색어가 없거나 마지막 페이지 또는 추가 조회 중이면 중복 요청하지 않음
    if (!appliedUserKeyword || !hasNextUser || isNextUserLoading) {
      // 현재 사용자 검색 목록과 페이지 상태를 유지함
      return;
    }

    // 마지막 성공 페이지 다음의 활성 사용자 검색 결과를 이어서 조회함
    void loadUserPage(appliedUserKeyword, userPage + 1);
  };

  /**
   * 검색 사용자의 현재 관계에 맞춰 팔로우하거나 언팔로우한 뒤 관계순 목록을 다시 조회함
   *
   * @author HanWon.Jang
   * @param user 관계를 변경할 검색 사용자
   * @return 팔로우 관계와 검색 정렬 갱신 완료 Promise
   */
  const handleUserFollow = async (user: FollowUser): Promise<void> => {
    // 다른 관계 변경이 진행 중이거나 본인 행이면 추가 조작을 허용하지 않음
    if (updatingUserNumb !== null || user.meYsno === "Y") {
      // 현재 사용자 검색 결과와 관계 상태를 유지함
      return;
    }

    // 팔로잉과 친구 상태는 로그인 사용자가 만든 관계의 삭제 대상으로 판정함
    const isFollowing = isFollowedByMe(user.followStatName);
    // 관계 변경을 시작한 검색어를 저장해 다른 검색 결과를 이전 응답으로 덮지 않게 함
    const relationKeyword = appliedUserKeyword;

    // 기존 팔로우 관계를 삭제하기 전에 마이페이지 관계 목록과 같은 확인을 받음
    if (isFollowing) {
      // "언팔로우하시겠어요?"
      const result = await sweetConfirm({
        title: message("frontend.social.unfollow.title"),
        // "팔로잉 목록에서 삭제돼요."
        text: message("frontend.social.unfollow.text"),
        // "언팔로우"
        confirmButtonText: message("frontend.social.unfollow.confirm"),
        // "취소"
        cancelButtonText: message("frontend.common.cancel"),
      });

      // 사용자가 취소한 경우 기존 관계와 검색 순서를 유지함
      if (!result.isConfirmed) {
        // 팔로우 관계 변경 없이 종료함
        return;
      }
    }

    // 같은 사용자에 대한 중복 관계 변경을 막도록 처리 중 번호를 저장함
    setUpdatingUserNumb(user.userNumb);

    // 관계 변경 성공 시 서버 정렬을 다시 적용하고 실패 시 기존 목록을 유지함
    try {
      // 현재 관계에 맞춰 팔로우 등록 또는 삭제 API를 호출함
      if (isFollowing) {
        // 로그인 사용자가 만든 현재 팔로우 관계를 삭제함
        await delSocialFollowApi(user.userNumb);
      }

      // 팔로우하지 않은 검색 사용자는 새 관계를 등록함
      else {
        // 로그인 사용자가 검색 사용자를 팔로우하도록 관계를 등록함
        await setSocialFollowApi(user.userNumb);
      }

      // 관계 변경 중에도 같은 검색어가 유지된 경우에만 서버 정렬을 다시 적용함
      if (appliedUserKeywordRef.current === relationKeyword) {
        // 변경된 관계 우선순위와 버튼명을 서버 조회로 다시 확정함
        await loadUserPage(relationKeyword, 1);
      }
    }

    // 관계 변경 실패 시 기존 검색 목록을 유지하고 공통 오류 문구로 안내함
    catch (followError) {
      // "수정에 실패했습니다."
      await sweetError(
        message("frontend.alert.updateFailedTitle"),
        getApiErrorMessage(followError, /* "다시 시도해주세요." */ message("frontend.common.tryAgain")),
      );
    }

    // 성공과 실패 모두 관계 변경 진행 상태를 해제함
    finally {
      // 검색 목록의 관계 버튼을 다시 조작할 수 있도록 처리 중 번호를 초기화함
      setUpdatingUserNumb(null);
    }
  };

  /**
   * 도서 표지 이미지 요청이 실패하면 공통 대체 이미지를 한 번만 적용함
   *
   * @author HanWon.Jang
   * @param event 로드에 실패한 이미지 이벤트
   * @return 반환값이 없음
   */
  const handleImageError = (event: SyntheticEvent<HTMLImageElement>): void => {
    // 오류가 발생한 실제 이미지 요소를 대체 경로 적용 대상으로 사용함
    const failedImage = event.currentTarget;
    // 도서 표지 오류 시 프로젝트 공통 대체 이미지를 사용함
    const fallbackImage = "/img/common/no-image.png";

    // 공통 대체 이미지까지 실패한 경우 같은 경로를 반복 요청하지 않음
    if (failedImage.getAttribute("src") === fallbackImage) {
      // 현재 대체 이미지 상태를 유지함
      return;
    }

    // 도서 표지와 배경사진을 공통 대체 이미지로 한 번만 교체함
    failedImage.src = fallbackImage;
  };

  /**
   * 피드 카드의 좋아요 상태를 즉시 반영하고 서버가 반환한 최종 값으로 확정함
   *
   * @author HanWon.Jang
   * @param item 좋아요 상태를 변경할 피드 항목
   * @return 좋아요 상태 확정 완료 Promise
   */
  const handleLike = async (item: FeedItem): Promise<void> => {
    // 피드 유형과 대상 번호를 결합해 대상별 중복 요청 차단 키를 생성함
    const pendingKey = `${item.tagtType}:${item.tagtNumb}`;

    // 같은 피드 대상의 좋아요 요청이 진행 중이면 중복 토글을 차단함
    if (pendingLikeKeysRef.current.has(pendingKey)) {
      // 진행 중인 좋아요 요청을 유지하고 추가 입력을 무시함
      return;
    }

    // 서버 응답 전에 반전된 좋아요 상태를 화면에 즉시 표시함
    const optimisticDetail = {
      likeCnt: Math.max(0, item.likeCnt + (item.likeYsno === "Y" ? -1 : 1)),
      likeYsno: item.likeYsno === "Y" ? "N" as const : "Y" as const,
    };
    // 같은 대상의 추가 좋아요 입력을 차단하도록 진행 키를 등록함
    pendingLikeKeysRef.current.add(pendingKey);

    /**
     * 서버 응답 전에 현재 피드 목록에 낙관적 좋아요 상태를 반영함
     *
     * @author HanWon.Jang
     * @param current 현재 화면에 누적된 피드 목록
     * @return 선택한 대상의 좋아요 상태가 반전된 새 피드 목록
     */
    const applyOptimisticLike = (current: FeedItem[]): FeedItem[] => {
      // 선택한 대상에 계산된 낙관적 좋아요 상태를 반영해 반환함
      return getUpdatedLikeItems(current, item, optimisticDetail);
    };

    // 서버 응답을 기다리는 동안 사용자가 누른 좋아요 상태를 즉시 표시함
    setItems(applyOptimisticLike);
    // 알림으로 직접 연 카드가 같은 대상이면 낙관적 좋아요 상태를 함께 반영함
    setFocusedItem((current) => current && isSameFeedTarget(current, item)
      ? { ...current, ...optimisticDetail }
      : current);

    // 좋아요 저장 성공과 실패를 낙관적 화면 상태에 맞춰 분리해 처리함
    try {
      // 피드 대상 유형과 번호를 서버에 전달해 좋아요 최종 상태를 확정함
      const result = await setPublicReportLikeApi({ tagtType: item.tagtType, tagtNumb: item.tagtNumb });
      // 서버가 반환한 좋아요 수와 로그인 사용자 좋아요 여부를 사용함
      const detail = result.data;

      /**
       * 서버가 반환한 값이 있는 항목만 현재 낙관적 좋아요 상태에 병합함
       *
       * @author HanWon.Jang
       * @param current 현재 화면에 누적된 피드 목록
       * @return 서버가 확정한 좋아요 상태가 반영된 새 피드 목록
       */
      const applyServerLike = (current: FeedItem[]): FeedItem[] => {
        // 응답 필드가 없으면 현재 낙관적 값을 유지해 불완전한 응답의 역변경을 막음
        const serverDetail = {
          likeCnt: detail?.likeCnt ?? optimisticDetail.likeCnt,
          likeYsno: detail?.likeYsno ?? optimisticDetail.likeYsno,
        };
        // 서버가 확정한 좋아요 상태를 선택한 피드 대상에 반영해 반환함
        return getUpdatedLikeItems(current, item, serverDetail);
      };

      // 서버가 확정한 값으로 화면의 낙관적 상태를 보정함
      setItems(applyServerLike);
      // 알림으로 직접 연 카드가 같은 대상이면 서버가 확정한 좋아요 상태를 함께 반영함
      setFocusedItem((current) => current && isSameFeedTarget(current, item)
        ? { ...current, likeCnt: detail?.likeCnt ?? optimisticDetail.likeCnt,
            likeYsno: detail?.likeYsno ?? optimisticDetail.likeYsno }
        : current);
    }

    // 핵심 좋아요 요청 실패 시 클릭 전 상태를 복원하고 안전한 오류를 안내함
    catch (likeError) {
      /**
       * 좋아요 저장 실패 시 선택한 피드 대상을 클릭 전 상태로 복원함
       *
       * @author HanWon.Jang
       * @param current 현재 화면에 누적된 피드 목록
       * @return 선택한 대상의 좋아요 상태가 복원된 새 피드 목록
       */
      const restorePreviousLike = (current: FeedItem[]): FeedItem[] => {
        // 클릭 전 좋아요 수와 여부를 선택한 피드 대상에 다시 반영해 반환함
        return getUpdatedLikeItems(current, item, item);
      };

      // 핵심 좋아요 요청이 실패한 경우에만 클릭 전 상태로 되돌림
      setItems(restorePreviousLike);
      // 알림으로 직접 연 카드가 같은 대상이면 클릭 전 좋아요 상태로 복원함
      setFocusedItem((current) => current && isSameFeedTarget(current, item)
        ? { ...current, likeCnt: item.likeCnt, likeYsno: item.likeYsno }
        : current);
      // "좋아요 처리에 실패했어요."
      const likeFailedTitle = message("frontend.feed.likeFailed");
      // "다시 시도해주세요."
      const retryMessage = message("frontend.common.tryAgain");
      // 원시 오류 대신 서버 또는 공통 재시도 문구를 선택함
      const likeErrorMessage = getApiErrorMessage(likeError, retryMessage);
      // 좋아요 상태를 복원한 뒤 사용자에게 안전한 실패 문구를 표시함
      await sweetError(likeFailedTitle, likeErrorMessage);
    }

    // 성공과 실패 모두 같은 대상의 다음 좋아요 입력을 허용함
    finally {
      // 성공과 실패 모두에서 같은 대상의 다음 좋아요 입력을 허용함
      pendingLikeKeysRef.current.delete(pendingKey);
    }
  };

  /**
   * 사진 변경 피드 유형에 맞는 활동 문구를 반환함
   *
   * @author HanWon.Jang
   * @param item 활동 유형을 포함한 피드 항목
   * @return 프로필 또는 배경사진 변경 설명이며 사진 변경 피드가 아니면 빈 문자열
   */
  const getImageActivityText = (item: FeedItem): string => {
    // 프로필 사진 변경 활동에는 전용 문구를 표시함
    if (item.tagtType === "PROFILE_IMAGE") {
      // "프로필 사진을 변경했어요"
      return message("frontend.feed.profileChanged");
    }

    // 배경사진 변경 활동에는 전용 문구를 표시함
    if (item.tagtType === "BACKGROUND_IMAGE") {
      // "배경사진을 변경했어요"
      return message("frontend.feed.backgroundChanged");
    }

    // 사진 변경 이외의 피드에는 활동 문구를 제공하지 않음
    return "";
  };

  /**
   * 지정한 독후감 피드 본문의 펼침 상태를 반대로 전환함
   *
   * @author HanWon.Jang
   * @param tagtNumb 펼침 상태를 변경할 피드 대상 번호
   * @return 반환값이 없음
   */
  const toggleReportContent = (tagtNumb: number): void => {
    /**
     * 현재 피드별 펼침 상태에서 지정한 독후감 대상만 반전함
     *
     * @author HanWon.Jang
     * @param current 피드 대상별 현재 펼침 상태
     * @return 지정한 피드의 펼침 상태가 반전된 새 객체
     */
    const toggleCurrentReport = (current: Record<number, boolean>): Record<number, boolean> => {
      // 다른 피드 상태를 유지하며 선택한 대상만 반전해 반환함
      return getToggledReports(current, tagtNumb);
    };

    // 선택한 독후감 피드 본문의 펼침 상태를 갱신함
    setExpandedReports(toggleCurrentReport);
  };

  /**
   * 피드 최초 조회 실패 상태에서 첫 페이지를 다시 요청함
   *
   * @author HanWon.Jang
   * @return 반환값이 없음
   */
  const retryFeed = (): void => {
    // 기존 오류 문구를 초기화하는 첫 페이지 조회를 다시 시작함
    void loadPage(1);
  };

  /**
   * 현재 마지막 성공 페이지 다음의 피드 목록을 추가로 요청함
   *
   * @author HanWon.Jang
   * @return 반환값이 없음
   */
  const loadMoreFeed = (): void => {
    // 무한 스크롤 도달 시 마지막 성공 페이지의 다음 번호를 조회함
    void loadPage(page + 1);
  };

  /**
   * 현재 열려 있는 피드 댓글 목록 대상을 해제함
   *
   * @author HanWon.Jang
   * @return 반환값이 없음
   */
  const closeReplySheet = (): void => {
    // 댓글 목록을 닫고 선택된 피드 대상 상태를 초기화함
    setReplyItem(null);
  };

  /**
   * 번역 캐시를 조회하거나 생성한 뒤 피드의 원문과 번역문 표시 상태를 전환함
   *
   * @author HanWon.Jang
   * @param item 번역 표시 상태를 변경할 독후감 피드
   * @return 반환값이 없음
   */
  const handleTranslation = async (item: FeedItem): Promise<void> => {
    // 독후감 번호가 없는 사진 피드와 불완전한 응답은 번역 요청에서 제외함
    if (!item.reptNumb) {
      return;
    }
    // 유효성 검사를 통과한 독후감 번호를 비동기 상태 갱신에서도 같은 값으로 사용함
    const reptNumb = item.reptNumb;

    // 현재 화면에 번역문이 있으면 외부 요청 없이 원문과 번역문만 전환함
    if (translations[reptNumb]) {
      // 선택한 피드 카드의 표시 상태만 반전함
      setTranslatedReports((current) => ({
        ...current,
        [reptNumb]: !current[reptNumb],
      }));
      return;
    }

    // 같은 독후감의 처리 중 요청은 중복 전송하지 않음
    if (pendingReportNumb === reptNumb) {
      return;
    }

    // 번역 요청이 끝날 때까지 선택한 카드의 버튼을 비활성화함
    setPendingReportNumb(reptNumb);

    try {
      // 서버가 공개 범위와 월간 한도를 재검증한 번역문을 조회함
      const translation = await setReportTranslationApi(reptNumb);
      // 번역문을 독후감 번호 기준으로 저장해 이후 전환에 재사용함
      setTranslations((current) => ({
        ...current,
        [reptNumb]: translation.trnsCntn,
      }));
      // 최초 번역 성공 직후 해당 피드 카드에 번역문을 표시함
      setTranslatedReports((current) => ({ ...current, [reptNumb]: true }));
    }

    // 서버 실패 응답은 원시 예외 없이 공통 메시지로 안내함
    catch (translationError) {
      // 번역 실패 원인을 서버 메시지 또는 공통 재시도 문구로 표시함
      await sweetError(
        message("frontend.report.translation.failedTitle"),
        getApiErrorMessage(translationError, message("frontend.common.tryAgain")),
      );
    }

    // 성공과 실패 모두 다음 번역 요청을 허용함
    finally {
      // 처리 중인 독후감 번호를 초기화함
      setPendingReportNumb(undefined);
    }
  };

  /**
   * 활성 사용자 검색 결과 한 건을 닉네임과 한줄소개 및 관계 버튼 행으로 렌더링함
   *
   * @author HanWon.Jang
   * @param user 렌더링할 활성 사용자 검색 결과
   * @return 마이페이지 관계 목록과 같은 사용자 정보 행
   */
  const renderSearchUser = (user: FollowUser): ReactNode => {
    /**
     * 검색 사용자의 공개 프로필 화면으로 이동함
     *
     * @author HanWon.Jang
     * @return 반환값이 없음
     */
    const moveUserProfile = (): void => {
      // 본인 여부에 맞는 프로필 경로로 이동함
      navigate(user.meYsno === "Y" ? "/mypage/profile" : `/social/profile/${user.userNumb}`);
    };

    /**
     * 현재 검색 사용자의 관계 버튼 처리를 비동기로 시작함
     *
     * @author HanWon.Jang
     * @return 반환값이 없음
     */
    const changeUserFollow = (): void => {
      // 선택한 사용자의 현재 관계에 맞춰 팔로우 또는 언팔로우를 시작함
      void handleUserFollow(user);
    };

    // 활성 사용자 검색 결과의 프로필 정보와 관계 버튼 행을 반환함
    return (
      /* 활성 사용자 검색 개별 항목 영역 */
      <div className={userListStyles.item} key={user.userNumb}>
        {/* 검색 사용자 프로필 정보 영역 */}
        <button
          className={userListStyles.profileButton}
          type="button"
          onClick={moveUserProfile}
        >
          <ProfileImage
            className={userListStyles.avatar}
            src={user.porfPath}
            alt={user.userNick ?? /* "닉네임" */ message("frontend.profile.nick")}
          />
          <span className={userListStyles.text}>
            <strong className={userListStyles.name}>
              <SearchMatchText text={user.userNick} keyword={appliedUserKeyword} />
            </strong>
            <span className={userListStyles.intro}>
              {user.intrCntn
                || /* "한줄 소개를 등록해보세요." */ message("frontend.profile.intro.empty")}
            </span>
          </span>
        </button>
        {/* 검색 사용자 관계 확인과 변경 영역 */}
        {user.meYsno !== "Y" ? (
          <button
            className={userListStyles.statusButton}
            data-follow-status={user.followStatName}
            type="button"
            disabled={updatingUserNumb === user.userNumb}
            onClick={changeUserFollow}
          >
            {user.followStatName}
          </button>
        ) : null}
      </div>
    );
  };

  /**
   * 피드 유형별 미디어와 독후감 정보 및 교류 기능을 포함한 카드 한 건을 렌더링함
   *
   * @author HanWon.Jang
   * @param item 렌더링할 피드 항목
   * @return 피드 유형에 맞게 구성된 카드 요소
   */
  const renderFeedItem = (item: FeedItem): ReactNode => {
    // 독후감 본문 앞뒤 공백을 제거해 빈 내용과 펼침 기준을 정확히 판정함
    const originalContent = item.reptCntn?.trim() ?? "";
    // 번역 보기 상태이면 현재 화면에 저장한 번역문을 사용하고 나머지는 원문을 사용함
    const reportContent = item.reptNumb && translatedReports[item.reptNumb]
      ? translations[item.reptNumb] ?? originalContent
      : originalContent;
    // 공통 미리보기 길이를 초과한 독후감에만 펼침 기능을 제공함
    const isLongContent = reportContent.length > REPORT_CONTENT_PREVIEW_LENGTH;
    // 저장된 대상별 펼침 상태를 boolean 값으로 보정함
    const isExpanded = Boolean(expandedReports[item.tagtNumb]);
    // 피드 대상 유형별 카드 영역을 선택할 판정값을 계산함
    const isReportFeed = item.tagtType === "REPORT";
    const isProfileImageFeed = item.tagtType === "PROFILE_IMAGE";
    const isBackgroundImageFeed = item.tagtType === "BACKGROUND_IMAGE";
    const isImageFeed = isProfileImageFeed || isBackgroundImageFeed;
    // "프로필 사진"
    const profileTargetContent = message("frontend.userReport.target.profileImage");
    // "배경사진"
    const backgroundTargetContent = message("frontend.userReport.target.backgroundImage");
    // 피드 유형에 맞는 신고 화면이 열리도록 신고 대상 유형을 변환함
    const complaintTargetType = isReportFeed
      ? "REPORT"
      : isProfileImageFeed
        ? "PROFILE"
        : "BACKGROUND";
    // 이미지 신고는 현재 사진을 소유한 사용자 번호를 사용하고 독후감은 독후감 번호를 사용함
    const complaintTargetNumb = isReportFeed ? item.tagtNumb : item.userNumb;
    // 신고 화면에 현재 카드 유형에 맞는 독후감 본문 또는 이미지 유형을 표시함
    const complaintTargetContent = isReportFeed
      ? reportContent
      : isProfileImageFeed
        ? profileTargetContent
        : backgroundTargetContent;
    // 공통 신고 메뉴가 대상별 신고 화면 이동 상태로 사용할 정보를 구성함
    const complaintTarget: SafetyReportTarget = {
      targetType: complaintTargetType,
      targetNumb: complaintTargetNumb,
      reportNumb: isReportFeed ? item.tagtNumb : undefined,
      userNumb: item.userNumb,
      userNick: item.userNick,
      content: complaintTargetContent,
    };
    // 피드 작성자와 사진 활동에 동일하게 표시할 발생 날짜를 계산함
    const activityDateLabel = new Date(item.activityDate).toLocaleDateString();
    // 독후감 번호가 있는 정상 피드는 도서 정보 상세로 이동하고 누락된 예외 데이터는 도서 검색으로 이동함
    const bookInfoPath = item.reptNumb
      ? `/book/info/${item.reptNumb}`
      : "/book/search";
    // 책 표지와 제목에서 도서검색을 즉시 실행할 제목 검색어를 정규화함
    const bookTitleKeyword = item.bookTitl?.trim() ?? "";
    // 본인 독후감은 상세로 이동하고 타인 독후감은 제목 기반 도서 검색으로 이동함
    const bookTargetPath = item.meYsno === "Y" && item.reptNumb
      ? `/report/detail/${item.reptNumb}`
      : "/book/search";
    // 저자 검색 링크는 공백을 제거한 실제 저자명이 있을 때만 표시함
    const bookAuthorKeyword = item.bookAthr?.trim() ?? "";
    // 펼침 버튼의 현재 동작을 보조기기에 전달할 문구를 조회함
    const expandActionLabel = getExpandActionLabel(isExpanded);
    // 펼침 상태에 맞는 공통 독후감 화살표 스타일을 선택함
    const expandArrowClass = isExpanded
      ? reportListStyles.expandArrowOpen
      : reportListStyles.expandArrow;
    // "좋아요"
    const likeActionLabel = message("frontend.feed.likeAction");
    // "댓글 보기"
    const viewCommentsLabel = message("frontend.book.publicReports.viewComments");
    // 프로필 사진 피드는 사용자 기본 이미지를 사용하고 배경사진 피드는 도서 공통 대체 이미지를 사용함
    const imageFallback = isProfileImageFeed
      ? DEFAULT_PROFILE_IMAGE
      : "/img/common/no-image.png";
    // 원본 경로를 우선 사용하고 없으면 화면용 경로와 유형별 대체 이미지 순서로 보정함
    const imageSource = item.contentImagePath || item.contentImageDisplayPath || imageFallback;

    /**
     * 현재 피드 작성자의 공개 프로필 화면으로 이동함
     *
     * @author HanWon.Jang
     * @return 반환값이 없음
     */
    const moveAuthorProfile = (): void => {
      // 본인 여부에 맞는 프로필 경로로 이동함
      navigate(item.meYsno === "Y" ? "/mypage/profile" : `/social/profile/${item.userNumb}`);
    };

    /**
     * 현재 독후감 피드 본문의 펼침 상태를 반대로 전환함
     *
     * @author HanWon.Jang
     * @return 반환값이 없음
     */
    const toggleCurrentContent = (): void => {
      // 현재 카드의 안정적인 피드 대상 번호로 펼침 상태를 변경함
      toggleReportContent(item.tagtNumb);
    };

    /**
     * 현재 피드 대상의 좋아요 상태 변경을 비동기로 시작함
     *
     * @author HanWon.Jang
     * @return 반환값이 없음
     */
    const toggleCurrentLike = (): void => {
      // 현재 카드의 좋아요 상태를 낙관적으로 반영하고 서버 결과로 확정함
      void handleLike(item);
    };

    /**
     * 현재 피드 대상의 댓글 목록을 확인할 수 있도록 선택 상태를 설정함
     *
     * @author HanWon.Jang
     * @return 반환값이 없음
     */
    const openCurrentReplies = (): void => {
      // 댓글 목록에 현재 피드 유형과 대상 번호를 전달하도록 선택 항목을 저장함
      setReplyItem(item);
    };

    /**
     * 현재 독후감 피드의 원문과 번역문 표시를 비동기로 전환함
     *
     * @author HanWon.Jang
     * @return 반환값이 없음
     */
    const toggleCurrentTranslation = (): void => {
      // 현재 피드의 캐시 조회 또는 번역 생성을 시작함
      void handleTranslation(item);
    };

    // 피드 유형에 맞는 미디어와 교류 기능을 포함한 카드 한 건을 반환함
    return (
      /* 피드 개별 활동 카드 영역 */
      <article className={styles.card} key={`${item.tagtType}-${item.tagtNumb}`}>
        {/* 피드 작성자 프로필 이동 영역 */}
        <header className={clsx(styles.cardHeader, isReportFeed && styles.reportHeader)}>
          <button className={styles.authorButton} type="button" onClick={moveAuthorProfile}>
            {/* 활동 작성자의 프로필 사진과 닉네임 영역 */}
            <span className={styles.authorIdentity}>
              <ProfileImage className={styles.avatar} src={item.porfPath} alt="" />
              <span className={styles.authorName}>{item.userNick}</span>
            </span>
          </button>
          {/* 다른 사용자의 피드 신고 및 차단 더보기 메뉴 영역 */}
          {item.meYsno === "N" ? (
            <div className={styles.actionMenuWrap}>
              <UserActionMenu
                userNick={item.userNick}
                reportTarget={complaintTarget}
                triggerIconClassName={styles.actionMenuTriggerIcon}
              />
            </div>
          ) : null}
        </header>

        {/* 독후감 도서 표지와 제목 및 저자 정보 영역 */}
        {isReportFeed ? (
          <div className={styles.reportMediaRow}>
            {/* 본인 여부에 따라 독후감 상세 또는 도서 검색으로 이동하는 표지 영역 */}
            <Link
              className={styles.reportCoverLink}
              to={bookTargetPath}
              state={{ initialSearchKeyword: bookTitleKeyword }}
            >
              <img
                className={styles.reportMedia}
                src={item.bookCvim || "/img/common/no-image.png"}
                alt={item.bookTitl ?? ""}
                onError={handleImageError}
              />
            </Link>
            {/* 도서 제목과 저자 및 독후감 상태 영역 */}
            <div className={styles.mediaInfo}>
              {/* 도서 제목과 저자 영역 */}
              <div className={styles.bookIdentity}>
                <Link
                  className={styles.bookInfoLink}
                  to={bookTargetPath}
                  state={{ initialSearchKeyword: bookTitleKeyword }}
                >
                  <span className={styles.title}>{item.bookTitl}</span>
                </Link>
                {/* 저자명이 있으면 해당 이름으로 도서를 검색하는 링크를 표시함 */}
                {bookAuthorKeyword ? (
                  <Link
                    className={styles.authorSearchLink}
                    to="/book/search"
                    state={{ initialSearchKeyword: bookAuthorKeyword }}
                  >
                    {item.bookAthr}
                  </Link>
                ) : null}
              </div>
              {/* 독후감 공개 날짜 영역 */}
              <span className={styles.activityDate}>
                {formatDashedDateToDot(formatDateValue(new Date(item.activityDate)))}
              </span>
              {/* 평점 또는 독서 상태가 있는 독후감의 도서 정보 이동 영역 */}
              {item.reptGrde || item.reptStatName ? (
                <Link className={styles.bookInfoLink} to={bookInfoPath}>
                  <span className={styles.ratingStatusRow}>
                    {/* 독후감 독서 상태 표시 영역 */}
                    {item.reptStatName ? (
                      <span className={getStatusClassName(item.reptStat)}>{item.reptStatName}</span>
                    ) : null}
                    {/* 독후감 평점 표시 영역 */}
                    {item.reptGrde ? (
                      <span className={styles.rating}>
                        <img className={styles.ratingIcon} src="/img/icons/icon-star-rate.svg" alt="" aria-hidden="true" />
                        {item.reptGrde}
                      </span>
                    ) : null}
                  </span>
                </Link>
              ) : null}
            </div>
          </div>
        ) : null}

        {/* 프로필 또는 배경사진과 사진 변경 설명 영역 */}
        {isImageFeed ? (
          <FullscreenImageButton
            className={styles.backgroundMediaButton}
            source={imageSource}
            fallbackSource={imageFallback}
            alt={isProfileImageFeed
              ? /* "프로필 사진" */ message("frontend.imageViewer.profileAlt")
              : /* "배경사진" */ message("frontend.imageViewer.backgroundAlt")}
          >
            {/* 프로필 또는 배경사진 영역 */}
            <span className={styles.backgroundMediaWrap}>
              {/* 프로필 사진 변경 피드는 배경사진과 같은 크기의 사진으로 표시함 */}
              {isProfileImageFeed ? (
                <ProfileImage
                  className={styles.backgroundMedia}
                  src={item.contentImageDisplayPath || item.contentImagePath}
                  alt={/* "프로필 사진" */ message("frontend.imageViewer.profileAlt")}
                />
              ) : (
                <BackgroundImage
                  source={item.contentImageDisplayPath || item.contentImagePath || "/img/common/no-image.png"}
                  imageClassName={styles.backgroundMedia}
                  alt={/* "배경사진" */ message("frontend.imageViewer.backgroundAlt")}
                  fallbackSource="/img/common/no-image.png"
                />
              )}
            </span>
            {/* 사진 변경 유형과 발생 날짜를 사진 아래 빈 공간의 오른쪽에 표시함 */}
            <span className={styles.imageActivity}>
              {getImageActivityText(item)} · {activityDateLabel}
            </span>
          </FullscreenImageButton>
        ) : null}

        {/* 독후감 본문과 펼침 제어 영역 */}
        {reportContent ? (
          <div className={styles.contentSection}>
            {/* 본인 독후감 본문만 도서 정보 상세로 이동하고 타인 독후감과 다른 활동 본문은 정적으로 표시함 */}
            {item.tagtType === "REPORT" && item.meYsno === "Y" ? (
              <Link className={styles.reportContentLink} to={bookInfoPath}>
                <AnimatedReportContent
                  content={reportContent}
                  expanded={isExpanded || !isLongContent}
                  previewHeight={54}
                />
              </Link>
            ) : (
              <AnimatedReportContent
                content={reportContent}
                expanded={isExpanded || !isLongContent}
                previewHeight={54}
              />
            )}
            {/* 긴 독후감 본문의 펼침 또는 접기 버튼 영역 */}
            {isLongContent ? (
              <button
                className={reportListStyles.expandButton}
                type="button"
                aria-label={expandActionLabel}
                onClick={toggleCurrentContent}
              >
                <img
                  className={expandArrowClass}
                  src="/img/icons/arrow-bottom.svg"
                  alt=""
                  aria-hidden="true"
                />
              </button>
            ) : null}
          </div>
        ) : null}

        {/* 피드 좋아요와 댓글 교류 영역 */}
        <footer className={styles.actions}>
          {isReportFeed && item.trnsAvaiYsno === "Y" ? (
            <button
              className={styles.translationButton}
              type="button"
              disabled={pendingReportNumb === item.reptNumb}
              onClick={toggleCurrentTranslation}
            >
              {pendingReportNumb === item.reptNumb
                ? message("frontend.report.translation.loading")
                : message(item.reptNumb && translatedReports[item.reptNumb]
                  ? "frontend.report.translation.original"
                  : "frontend.report.translation.view")}
            </button>
          ) : <span />}
          <div className={styles.reactionActions}>
            {/* 좋아요 변경과 좋아요 사용자 목록 영역 */}
            <div className={styles.likeActionGroup}>
              <button
                className={styles.likeIconButton}
                type="button"
                aria-label={likeActionLabel}
                onClick={toggleCurrentLike}
              >
                <img
                  className={styles.icon}
                  src={item.likeYsno === "Y" ? "/img/icons/icon-heart-fill.svg" : "/img/icons/icon-heart.svg"}
                  alt=""
                />
              </button>
              <LikeUserListButton
                className={styles.likeCountButton}
                tagtType={item.tagtType}
                tagtNumb={item.tagtNumb}
                countLabel={item.likeCnt}
              />
            </div>
            {/* 댓글 목록 열기 영역 */}
            <button
              className={styles.commentButton}
              type="button"
              aria-label={viewCommentsLabel}
              onClick={openCurrentReplies}
            >
              <img className={styles.icon} src="/img/icons/icon-comment.svg" alt="" />
              {item.replCnt}
            </button>
          </div>
        </footer>
      </article>
    );
  };

  // 알림 대상과 페이지 목록에서 같은 카드는 한 번만 표시하도록 화면 목록을 구성함
  const visibleItems = focusedItem
    ? [focusedItem, ...items.filter((item) => !isSameFeedTarget(item, focusedItem))]
    : items;
  // 한 글자 이상의 닉네임이 입력되면 피드 카드 대신 활성 사용자 검색 결과를 표시함
  const hasUserSearch = Boolean(appliedUserKeyword);
  // 피드 첫 페이지가 아직 없을 때만 검색 입력 아래에 최초 로딩 상태를 표시함
  const isFeedInitialLoading = isLoading && visibleItems.length === 0;

  // 유형별 카드와 공통 교류 동작을 포함한 피드 화면을 반환함
  return (
    <main className={styles.page}>
      {/* 본인과 팔로잉 사용자의 공개 활동 피드 전체 영역 */}
      {/* 피드 사용자 검색 영역이 실제 고정되는 시점을 감지하는 화면 경계 */}
      <span ref={sentinelRef} className={stickyStyles.sentinel} aria-hidden="true" />
      {/* 홈과 같은 위치와 스타일로 고정되는 닉네임 검색 입력 영역 */}
      <form
        className={clsx(
          homeStyles.searchBar,
          styles.userSearchBar,
          stickyStyles.surface,
          isSticky && stickyStyles.stuck,
        )}
        onSubmit={handleUserSearch}
      >
        <label className={homeStyles.searchLabel}>
          <span className={homeStyles.hiddenLabel}>
            {/* "닉네임 검색" */}
            {message("frontend.feed.userSearch.label")}
          </span>
          <input
            className={homeStyles.searchInput}
            type="search"
            maxLength={25}
            value={userKeyword}
            placeholder={/* "닉네임 검색" */ message("frontend.feed.userSearch.label")}
            onChange={handleUserKeyword}
          />
          <button
            className={homeStyles.searchButton}
            type="submit"
            aria-label={/* "검색" */ message("frontend.common.search")}
          >
            <svg className={homeStyles.searchIcon} viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="M10.8 5.2a5.6 5.6 0 1 1 0 11.2 5.6 5.6 0 0 1 0-11.2Z"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.8"
              />
              <path
                d="m15 15 4 4"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
              />
            </svg>
          </button>
        </label>
      </form>

      {/* 한 글자 이상의 닉네임 검색어가 있으면 활성 사용자와 관계 버튼 목록을 표시함 */}
      {hasUserSearch ? (
        <section className={styles.userSearchResults}>
          {/* 활성 사용자 검색 첫 페이지 로딩 영역 */}
          {isUserLoading ? (
            <Loading
              title={/* "목록 조회 중" */ message("frontend.common.loadingList")}
              isFullScreen={false}
              isCompact
            />
          ) : null}

          {/* 활성 사용자 검색 결과가 없는 상태 안내 영역 */}
          {!isUserLoading && searchUsers.length === 0 ? (
            <p className={userListStyles.empty}>
              {/* "검색된 사용자가 없어요." */}
              {message("frontend.feed.userSearch.empty")}
            </p>
          ) : null}

          {/* 닉네임과 한줄소개 및 관계 버튼이 포함된 활성 사용자 목록 영역 */}
          {!isUserLoading ? (
            <div className={styles.userSearchList}>
              {searchUsers.map(renderSearchUser)}
            </div>
          ) : null}

          {/* 활성 사용자 검색 다음 페이지 자동 조회와 추가 로딩 영역 */}
          <InfiniteScrollTrigger
            hasNext={!isUserLoading && hasNextUser}
            isLoading={isNextUserLoading}
            onLoadMore={loadMoreUser}
          />
        </section>
      ) : (
        <>
          {/* 피드 최초 조회 로딩 영역 */}
          {isFeedInitialLoading ? <Loading isFullScreen={false} /> : null}

          {/* 피드 최초 조회 실패와 재시도 영역 */}
          {error && visibleItems.length === 0 ? (
            <div className={styles.error}>
              {error}
              <br />
              <button className={styles.retry} type="button" onClick={retryFeed}>
                {/* "다시 시도" */}
                {message("frontend.common.retry")}
              </button>
            </div>
          ) : null}

          {/* 본인과 팔로잉 공개 활동이 없는 피드 빈 상태 영역 */}
          {!isLoading && !error && visibleItems.length === 0 ? (
            <p className={styles.empty}>
              {/* "아직 표시할 공개 소식이 없어요.\n내 독후감과 사진 변경, 팔로잉 소식이 여기에 표시됩니다." */}
              {message("frontend.feed.empty")}
            </p>
          ) : null}

          {/* 페이지 단위로 누적되는 피드 카드 목록 영역 */}
          <div className={styles.list}>
            {visibleItems.map(renderFeedItem)}
          </div>

          {/* 다음 피드 페이지 자동 조회와 추가 로딩 영역 */}
          <InfiniteScrollTrigger hasNext={hasNext} isLoading={isLoading} onLoadMore={loadMoreFeed}>
            <Loading isFullScreen={false} />
          </InfiniteScrollTrigger>
        </>
      )}

      {/* 선택한 피드 대상의 댓글 목록 영역 */}
      {replyItem ? (
        <ReplySheet
          report={{ reptNumb: replyItem.tagtNumb, userNick: replyItem.userNick }}
          tagtType={replyItem.tagtType}
          focusReplNumb={focusReplNumb}
          onClose={closeReplySheet}
        />
      ) : null}
    </main>
  );
};

export default FeedPage;
