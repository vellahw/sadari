import {message} from "@/app/messages/message";
import {getApiErrorMessage} from "@/app/api/resultData";
import {sweetConfirm, sweetError, sweetInfo, sweetWarning} from "@/app/lib/sweetAlert/sweetAlert";
import {runBlockingOperation} from "@/app/navigation/blockingOperation";
import {queryClient} from "@/app/query/queryClient";
import {
  formatDashedDateToDot,
  getRemainDaysUntil,
} from "@/app/utils/dateUtil";
import {useBodyScrollLock} from "@/app/utils/modalUtil";
import BackgroundImage from "@/components/BackgroundImage/BackgroundImage";
import {ActionButton} from "@/components/Button/ActionButton";
import {FullscreenImageButton} from "@/components/ImageViewer/FullscreenImageViewer";
import Loading from "@/components/Loading/Loading";
import InfiniteScrollTrigger from "@/components/InfiniteScroll/InfiniteScrollTrigger";
import * as modalControlStyles from "@/components/Modal/ModalControls.css";
import * as reportListStyles from "@/components/ReportList/ReportListView.css";
import {setPublicReportLikeApi} from "@/features/Book/api/bookApi";
import {
  getBookCoverImageSource,
  handleBookCoverImageError,
} from "@/features/Book/utils/bookCoverImage";
import {POPUP_CONTENT_KEYS} from "@/features/Popup/api/popupContentApi";
import {usePopupContent} from "@/features/Popup/hooks/usePopupContent";
import {parsePopupContentList} from "@/features/Popup/utils/popupContentUtil";
import ReplySheet from "@/features/reply/ReplySheet";
import {
  delSocialFollowApi,
  setSocialFollowApi,
  type FollowListType,
  type FollowUser,
} from "@/features/Social/api/socialApi";
import {CommentButton, LikeButton} from "@/features/Social/components/ReactionButtons";
import {useFollowListModal} from "@/features/Social/hooks/useFollowListModal";
import {isFollowedByMe} from "@/features/Social/utils/followStatus";
import * as userListStyles from "@/features/Social/components/LikeUserListButton.css";
import {
  copyPrevReadingGoalApi,
  delProfileImageDraftApi,
  getMonthlyReadingApi,
  getMyProfileApi,
  getProfileDraftListApi,
  setProfileImageDraftApi,
  updateReadingGoalApi,
  updateMyProfileApi,
  type MonthlyReadingSummary,
  type ImageReaction,
  type ReadingSummaryReport,
  type UserProfile,
  type ProfileImageDraft,
  type ProfileImageType,
} from "@/features/User/api/userApi";
import {getMyProfileOptions} from "@/features/User/hooks/useMyProfileQuery";
import ProfileImage, {
  DEFAULT_PROFILE_IMAGE,
  normalizeProfileImageSource,
} from "@/features/User/components/ProfileImage";
import {
  getReadingEndDateText,
  getReadingGradeText,
} from "@/features/User/utils/profileReadingFormat";
import {notifyUserProfileUpdated} from "@/features/User/lib/profileEvents";
import {getGoalProgressColor} from "@/features/User/utils/goalProgress";
import type {ChangeEvent, FormEvent, MouseEvent, ReactNode} from "react";
import {useEffect, useRef, useState} from "react";
import {createPortal} from "react-dom";
import {useNavigate, useSearchParams} from "react-router-dom";
import * as styles from "./ProfileEditPage.css";
import ReadingStatisticsSection from "./ReadingStatisticsSection";
import StepperButton from "@/components/Button/StepperButton/StepperButton";

const USER_NICK_MAX_LENGTH = 25;
const PROFILE_INTRO_MAX_LENGTH = 50;
const USER_NICK_REGEX = /^[A-Za-z0-9\uAC00-\uD7A3]+(?:[_-][A-Za-z0-9\uAC00-\uD7A3]+)*$/;
const USER_NICK_INPUT_REGEX = /[^A-Za-z0-9\uAC00-\uD7A3\u3131-\u318E\u1100-\u11FF\uA960-\uA97F\uD7B0-\uD7FF_-]/g;
const PROFILE_IMAGE_MAX_BYTES = 10 * 1024 * 1024;
const PROFILE_IMAGE_MIME_TYPES = new Set(["image/jpeg", "image/png"]);
type ReadingPeriod = "week" | "month" | "year";
type ProfileModalType = "currentReading" | "goal" | "goalHelp" | "followList";
type ProfileStatAction = FollowListType | "totalReadBook" | "receivedLike";

const GOAL_PERIODS: ReadingPeriod[] = ["week", "month", "year"];
const MODAL_CLOSE_DELAY_MS = 180;

const DEFAULT_GOAL_HELP_ITEMS = [
  // "주간 목표는 월요일부터 일요일까지를 한 주로 보고, 최대 1회까지 내릴 수 있습니다. 해당 주가 3일 남은 시점부터는 내릴 수 없습니다."
  message("frontend.profile.goal.helpWeek"),
  // "월간 목표는 최대 3회까지 내릴 수 있고, 해당 월이 7일 남은 시점부터는 내릴 수 없습니다."
  message("frontend.profile.goal.helpMonth"),
  // "연간 목표는 최대 5회까지 내릴 수 있고, 12월 1일부터는 내릴 수 없습니다."
  message("frontend.profile.goal.helpYear"),
  // "같은 목표 권수를 다시 저장하는 경우는 목표 내리기 횟수를 소모하지 않습니다."
  message("frontend.profile.goal.helpSameValue"),
] as const;

/**
 * 관리자 설정 또는 기본 목표 내리기 정책 문구를 목록 항목으로 표시함
 *
 * @author HanWon.Jang
 * @param goalHelpItem 화면에 표시할 목표 내리기 정책 문구
 * @return 목표 내리기 정책 목록 항목
 */
const renderGoalHelpItem = (goalHelpItem: string): ReactNode => {
  // 개별 목표 내리기 정책 문구를 안정적인 문자열 key와 함께 목록 항목으로 반환함
  return <li key={goalHelpItem}>{goalHelpItem}</li>;
};

const GOAL_COPY_LABELS: Record<ReadingPeriod, { current: string; previous: string; singular: string }> = {
  week: {
    current: /* "이번 주" */ message("frontend.profile.goal.copy.week.current"),
    previous: /* "지난 주" */ message("frontend.profile.goal.copy.week.previous"),
    singular: /* "이번주의" */ message("frontend.profile.goal.copy.week.singular"),
  },
  month: {
    current: /* "이번 달" */ message("frontend.profile.goal.copy.month.current"),
    previous: /* "지난 달" */ message("frontend.profile.goal.copy.month.previous"),
    singular: /* "이번 달의" */ message("frontend.profile.goal.copy.month.singular"),
  },
  year: {
    current: /* "올해" */ message("frontend.profile.goal.copy.year.current"),
    previous: /* "작년" */ message("frontend.profile.goal.copy.year.previous"),
    singular: /* "올해의" */ message("frontend.profile.goal.copy.year.singular"),
  },
};

/**
 * 닉네임 입력값에서 허용하지 않은 문자를 제거하고 최대 입력 길이를 제한함
 * 한글 조합 문자와 영문 및 숫자 외에는 언더바와 하이픈만 입력 상태에 반영함
 *
 * @author HanWon.Jang
 * @param value 사용자가 입력한 닉네임 원문
 * @return 허용 문자로 구성된 25자 이하 닉네임
 */
const normalizeUserNick = (value: string) =>
  value.replace(USER_NICK_INPUT_REGEX, "").slice(0, USER_NICK_MAX_LENGTH);

/**
 * 한줄 소개 입력값을 허용 길이 이하로 제한함
 * textarea의 maxLength와 별개로 상태 값도 제한해 브라우저별 입력 차이를 한 번 더 방어함
 *
 * @author HanWon.Jang
 * @param value 사용자가 입력한 한줄 소개 원문
 * @return 50자 이하로 제한한 한줄 소개
 */
const normalizeProfileIntro = (value: string) =>
  value.slice(0, PROFILE_INTRO_MAX_LENGTH);

/**
 * join Korean List 기능을 처리함
 *
 * @author HanWon.Jang
 * @param items items 입력값
 * @return 처리 결과
 */
const joinKoreanList = (items: string[]) => items.join(", ");

/**
 * get Copyable Previous Goal Periods 정보를 조회함
 *
 * @author HanWon.Jang
 * @param summary summary 입력값
 * @return 처리 결과
 */
const getCopyableGoalPeriods = (summary: MonthlyReadingSummary | null) =>
  GOAL_PERIODS.filter((period) => {

    if (!summary) {
      return false;
    }

    if (period === "week") {
      return !summary.weekGoalSet && Boolean(summary.previousWeekGoalCnt);
    }

    if (period === "month") {
      return !summary.monthGoalSet && Boolean(summary.previousMonthGoalCnt);
    }

    return !summary.yearGoalSet && Boolean(summary.previousYearGoalCnt);
  });

/**
 * get Previous Goal Count 정보를 조회함
 *
 * @author HanWon.Jang
 * @param summary summary 입력값
 * @param period period 입력값
 * @return 처리 결과
 */
const getPreviousGoalCount = (summary: MonthlyReadingSummary, period: ReadingPeriod) => {

  if (period === "week") {
    return summary.previousWeekGoalCnt ?? 0;
  }

  if (period === "month") {
    return summary.previousMonthGoalCnt ?? 0;
  }

  return summary.previousYearGoalCnt ?? 0;
};

/**
 * get Copy Previous Goal Confirm Text 정보를 조회함
 *
 * @author HanWon.Jang
 * @param summary summary 입력값
 * @param periods periods 입력값
 * @return 처리 결과
 */
const getCopyGoalConfirmText = (summary: MonthlyReadingSummary, periods: ReadingPeriod[]) => {

  if (periods.length === 1) {
    const period = periods[0];
    const count = getPreviousGoalCount(summary, period);
    const labels = GOAL_COPY_LABELS[period];
    return `${labels.singular} 독서 목표설정이 비어있습니다.\n${labels.previous} 목표 권수(${count}권)를 가져오시겠습니까?`;
  }

  const currentLabels = periods.map((period) => GOAL_COPY_LABELS[period].current);
  const previousLabels = periods.map((period) => {

    const labels = GOAL_COPY_LABELS[period];
    return `${labels.previous}(${getPreviousGoalCount(summary, period)}권)`;
  });

  return `${joinKoreanList(currentLabels)} 목표가 비어있습니다. ${joinKoreanList(previousLabels)} 목표를 가져오시겠습니까?`;
};

/**
 * 이전 기간 대비 완료 독서 변화량을 화면 표시용 문자열로 변환함
 * 양수에는 + 기호를 붙이고 0은 증감이 없는 상태로 그대로 표시함
 *
 * @author HanWon.Jang
 * @param diff 이전 기간 대비 완료 독서 권수 변화량
 * @return 변화량 표시 문자열
 */
const formatReadingDiff = (diff: number) => {

  if (diff > 0) {
    return `+${diff}`;
  }

  return String(diff);
};

/**
 * 현재 읽고 있는 책의 목표 독서기간을 팝업 표시용 문장으로 변환함
 * 시작일과 종료일이 모두 비어 있으면 책 정보 영역에 불필요한 빈 라벨이 나오지 않도록 빈 문자열을 반환함
 *
 * @author HanWon.Jang
 * @param report 목표 독서기간을 표시할 독후감 요약 정보
 * @return 목표 독서기간 표시 문구
 */
const getReadingPeriodText = (report: ReadingSummaryReport) => {

  const periodText = [
    formatDashedDateToDot(report.reptStdt),
    formatDashedDateToDot(report.reptEndt),
  ]
    .filter(Boolean)
    .join(" ~ ");

  return periodText
    ? /* "목표 독서기간: {0}" */ message("frontend.profile.currentReading.targetPeriod", [periodText])
    : "";
};

/**
 * 로그인 사용자의 프로필 사진, 배경 사진, 닉네임, 한줄 소개를 조회하고 수정함
 * 수정 모드에서는 화면을 전환하지 않고 기존 요소 위치에서 텍스트와 이미지만 편집할 수 있게 제공함
 *
 * @author SeungHyeon.Kang
 * @return 프로필 상세 및 수정 페이지 컴포넌트
 */
const ProfileEditPage = () => {

    const navigate = useNavigate();
    // 알림이 지정한 본인 사진과 댓글 위치를 마이페이지에서 해석함
    const [searchParams] = useSearchParams();
    const requestedTagtType = searchParams.get("tagtType");
    const requestedTagtNumb = Number(searchParams.get("tagtNumb"));
    const requestedReplNumb = Number(searchParams.get("replNumb"));
    const focusReplNumb = Number.isSafeInteger(requestedReplNumb) && requestedReplNumb > 0
      ? requestedReplNumb
      : undefined;
    const [profile, setProfile] = useState<UserProfile | null>(null);
    const [isEditMode, setIsEditMode] = useState(false);
    const [userNick, setUserNick] = useState("");
    const [intrCntn, setIntrCntn] = useState("");
    const [profileImageDraftToken, setProfileImageDraftToken] = useState<string | null>(null);
    const [backgroundImageDraftToken, setBackgroundImageDraftToken] = useState<string | null>(null);
    const [previewImage, setPreviewImage] = useState(DEFAULT_PROFILE_IMAGE);
    const [previewBackground, setPreviewBackground] = useState("");
    // 저장된 배경사진은 일반 화면에서 파생본을 사용하고 편집 중 임시 선택본은 서버 미리보기를 사용함
    const coverDisplaySource = backgroundImageDraftToken
      ? previewBackground
      : profile?.bgimDisplayPath || previewBackground;
    const [monthlySummary, setMonthlySummary] = useState<MonthlyReadingSummary | null>(null);
    const [currentReadingReport, setCurrentReadingReport] = useState<ReadingSummaryReport | null>(null);
    const [isGoalModalOpen, setIsGoalModalOpen] = useState(false);
    const [isGoalHelpModalOpen, setIsGoalHelpModalOpen] = useState(false);
    const [isFollowListScrolling, setIsFollowListScrolling] = useState(false);
    const [followUpdatingUserNumb, setFollowUpdatingUserNumb] = useState<number | null>(null);
    const [closingModal, setClosingModal] = useState<ProfileModalType | null>(null);
    const [weekGoalCnt, setWeekGoalCnt] = useState("");
    const [monthGoalCnt, setMonthGoalCnt] = useState("");
    const [yearGoalCnt, setYearGoalCnt] = useState("");
    const [expandedSummary, setExpandedSummary] = useState<Record<ReadingPeriod, boolean>>({
      week: false,
      month: false,
      year: false,
    });
    const [activeDiffTooltip, setActiveDiffTooltip] = useState<ReadingPeriod | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [isGoalSaving, setIsGoalSaving] = useState(false);
    const [imageLikeUpdatingType, setImageLikeUpdatingType] = useState<ImageReaction["tagtType"] | null>(null);
    const [replyTarget, setReplyTarget] = useState<ImageReaction | null>(null);
    const processedReplyRouteRef = useRef("");
    const {
      followListType,
      followUsers,
      isFollowListLoading,
      isNextFollowLoading,
      hasNextFollowUser,
      openFollowList,
      loadMoreFollow,
      closeFollowList,
      setFollowUsers,
    } = useFollowListModal({isMyProfile: true});
    const diffTooltipRefs = useRef<Record<ReadingPeriod, HTMLDivElement | null>>({
      week: null,
      month: null,
      year: null,
    });
    const followListScrollTimeoutRef = useRef<number | null>(null);
    // 목표 내리기 도움말에 표시할 관리자 설정 콘텐츠를 미리 조회함
    const {data: goalHelpContent} = usePopupContent(
      POPUP_CONTENT_KEYS.profileGoalDown,
    );
    // 목표 내리기 정책 JSON을 검증하고 조회 전이나 실패 시 현재 기본 문구를 유지함
    const goalHelpItems = parsePopupContentList(
      goalHelpContent?.contFirs,
      DEFAULT_GOAL_HELP_ITEMS,
    );
    // 열린 프로필 팝업 상태에 맞춰 배경 스크롤 잠금을 동기화함
    useBodyScrollLock(
      Boolean(currentReadingReport) || isGoalModalOpen || isGoalHelpModalOpen || Boolean(followListType),
    );

    // 알림이 요청한 사진 유형에 대응하는 현재 마이페이지 반응을 선택함
    const requestedReaction = requestedTagtType === "PROFILE_IMAGE"
      ? profile?.profileImageReaction
      : requestedTagtType === "BACKGROUND_IMAGE"
        ? profile?.backgroundImageReaction
        : null;
    // 알림의 파일 번호가 현재 본인 사진과 일치할 때만 자동 열기 대상으로 인정함
    const hasRequestedImage = Boolean(
      requestedReaction
      && Number.isSafeInteger(requestedTagtNumb)
      && requestedTagtNumb > 0
      && requestedReaction.tagtNumb === requestedTagtNumb,
    );
    const requestedRouteKey = `${requestedTagtType ?? ""}:${requestedTagtNumb}:${focusReplNumb ?? 0}`;

    // 사진 댓글 알림이면 현재 사진 검증이 끝난 뒤 댓글 바텀시트와 강조 위치를 한 번만 엶
    useEffect(() => {
      // 일반 마이페이지 진입 또는 사진 좋아요 알림은 전체 화면 사진만 열고 댓글 시트는 열지 않음
      if (!hasRequestedImage || !requestedReaction || !focusReplNumb) {
        // 댓글 자동 열기 상태를 변경하지 않고 종료함
        return;
      }

      // 같은 알림 경로를 이미 처리했으면 댓글 집계 갱신으로 시트를 다시 열지 않음
      if (processedReplyRouteRef.current === requestedRouteKey) {
        // 사용자가 닫은 댓글 시트 상태를 유지함
        return;
      }

      // 현재 알림 경로의 댓글 자동 열기를 처리했음을 기록함
      processedReplyRouteRef.current = requestedRouteKey;
      // 현재 본인 사진 댓글 시트를 열고 알림이 지정한 댓글을 강조하도록 대상을 설정함
      setReplyTarget(requestedReaction);
    }, [focusReplNumb, hasRequestedImage, requestedReaction, requestedRouteKey]);

    /**
     * 서버에서 받은 프로필 값을 화면 상태와 이미지 미리보기 상태에 함께 반영함
     * 저장 완료 후 파일 선택 상태를 비워 같은 파일을 다시 선택하더라도 정상적으로 반응하게 만듦
     *
     * @author HanWon.Jang
     * @param nextProfile 서버에서 조회하거나 저장 후 반환한 사용자 프로필 정보
     */
    const syncProfileState = (nextProfile: UserProfile) => {
      setProfile(nextProfile);
      setUserNick(nextProfile?.userNick ?? "");
      setIntrCntn(nextProfile?.intrCntn ?? "");
      setPreviewImage(nextProfile?.porfPath || DEFAULT_PROFILE_IMAGE);
      setPreviewBackground(nextProfile?.bgimPath || "");
      setProfileImageDraftToken(null);
      setBackgroundImageDraftToken(null);
    };

    /**
     * 현재 사용자 사진의 좋아요 상태를 서버 결과로 갱신함
     * 같은 사진에 대한 중복 요청을 막고 프로필 공유 캐시에도 변경 결과를 함께 반영함
     *
     * @author SeungHyeon.Kang
     * @param reaction 좋아요를 변경할 현재 사진 반응 정보
     */
    const handleImageLike = async (reaction: ImageReaction): Promise<void> => {
      // 같은 사진의 좋아요 요청이 진행 중이면 중복 토글을 실행하지 않음
      if (imageLikeUpdatingType === reaction.tagtType) {
        // 진행 중인 첫 요청 결과를 유지함
        return;
      }

      const reactionKey = reaction.tagtType === "PROFILE_IMAGE"
        ? "profileImageReaction"
        : "backgroundImageReaction";

      /**
       * 지정한 좋아요 상태를 같은 사진 반응에만 병합함
       *
       * @author SeungHyeon.Kang
       * @param current 현재 화면 또는 공유 캐시의 프로필 정보
       * @param detail 적용할 좋아요 수와 여부
       * @return 좋아요 상태가 반영된 프로필 정보
       */
      const mergeReaction = (
        current: UserProfile | undefined,
        detail: Pick<ImageReaction, "likeCnt" | "likeYsno">,
      ): UserProfile | undefined => {
        const currentReaction = current?.[reactionKey];

        // 화면 사이에 사진이 교체되었으면 이전 사진의 상태를 새 사진에 반영하지 않음
        if (!current || !currentReaction || currentReaction.tagtNumb !== reaction.tagtNumb) {
          return current;
        }

        // 지정한 좋아요 수와 여부만 현재 사진 반응에 병합함
        return {
          ...current,
          [reactionKey]: {
            ...currentReaction,
            ...detail,
          },
        };
      };

      const optimisticDetail: Pick<ImageReaction, "likeCnt" | "likeYsno"> = {
        likeCnt: Math.max(
          0,
          reaction.likeCnt + (reaction.likeYsno === "Y" ? -1 : 1),
        ),
        likeYsno: reaction.likeYsno === "Y" ? "N" : "Y",
      };

      // 현재 대상 버튼의 중복 요청을 막고 서버 응답 전에 화면과 공유 캐시를 즉시 변경함
      setImageLikeUpdatingType(reaction.tagtType);
      setProfile((current) =>
        mergeReaction(current ?? undefined, optimisticDetail) ?? null,
      );
      queryClient.setQueryData<UserProfile>(
        getMyProfileOptions().queryKey,
        (current) => mergeReaction(current, optimisticDetail),
      );

      try {
        // 범용 좋아요 API에 현재 사진 유형과 파일 번호를 전달함
        const result = await setPublicReportLikeApi({
          tagtType: reaction.tagtType,
          tagtNumb: reaction.tagtNumb,
        });
        // 서버가 확정한 값이 있으면 현재 화면과 공유 캐시의 낙관적 상태를 보정함
        const detail = result.data;

        if (detail) {
          setProfile((current) =>
            mergeReaction(current ?? undefined, detail) ?? null,
          );
          queryClient.setQueryData<UserProfile>(
            getMyProfileOptions().queryKey,
            (current) => mergeReaction(current, detail),
          );
        }
      }

        // 좋아요 요청 실패를 공통 안내 문구로 표시함
      catch (error) {
        const originalDetail: Pick<ImageReaction, "likeCnt" | "likeYsno"> = {
          likeCnt: reaction.likeCnt,
          likeYsno: reaction.likeYsno,
        };
        // 핵심 좋아요 요청 실패 시에만 화면과 공유 캐시를 클릭 전 상태로 원복함
        setProfile((current) =>
          mergeReaction(current ?? undefined, originalDetail) ?? null,
        );
        queryClient.setQueryData<UserProfile>(
          getMyProfileOptions().queryKey,
          (current) => mergeReaction(current, originalDetail),
        );
        await sweetError(
          /* "좋아요 처리에 실패했어요." */ message("frontend.feed.likeFailed"),
          getApiErrorMessage(error, /* "다시 시도해주세요." */ message("frontend.common.tryAgain")),
        );
      } finally {
        // 성공과 실패 모두에서 사진 좋아요 버튼을 다시 활성화함
        setImageLikeUpdatingType(null);
      }
    };

    /**
     * 사진 댓글 바텀시트를 닫고 최신 댓글 집계를 다시 조회함
     *
     * @author SeungHyeon.Kang
     */
    const handleImageReplyClose = async (): Promise<void> => {
      // 댓글 바텀시트를 먼저 닫아 본문 조작을 복구함
      setReplyTarget(null);

      try {
        // 댓글 등록과 삭제 결과가 반영된 최신 프로필 사진 집계를 조회함
        const nextProfile = (await getMyProfileApi()).data;
        // 마이페이지의 표시 상태에 최신 프로필과 사진 반응을 반영함
        syncProfileState(nextProfile);
        // 공유 프로필 캐시도 최신 서버 응답으로 교체함
        queryClient.setQueryData(getMyProfileOptions().queryKey, nextProfile);
      }

        // 댓글 창은 닫힌 상태로 유지하고 집계 재조회 실패만 안내함
      catch (error) {
        await sweetError(
          /* "조회에 실패했습니다." */ message("frontend.alert.loadFailedTitle"),
          getApiErrorMessage(error, /* "다시 시도해주세요." */ message("frontend.common.tryAgain")),
        );
      }
    };

    /**
     * 공통 좋아요와 댓글 버튼을 현재 사진의 반응 정보에 연결
     *
     * @author SeungHyeon.Kang
     * @param reaction 현재 사진의 좋아요와 댓글 집계
     * @param className 사진 위치에 맞는 반응 버튼 묶음 스타일
     * @return 좋아요와 댓글 버튼 묶음
     */
    const renderImageReactions = (reaction: ImageReaction, className: string): ReactNode => {
      /**
       * 현재 사진의 좋아요 상태 전환
       *
       * @author HanWon.Jang
       * @return 반환값 없음
       */
      const toggleImageLike = (): void => {
        // 현재 사진을 대상으로 기존 좋아요 처리 실행
        void handleImageLike(reaction);
      };

      /**
       * 현재 사진의 댓글 목록 열기
       *
       * @author HanWon.Jang
       * @return 반환값 없음
       */
      const openImageReplies = (): void => {
        // 댓글 목록의 대상을 현재 사진으로 설정
        setReplyTarget(reaction);
      };

      // 사진 위치별 배치에 공통 반응 버튼을 표시하는 영역
      return (
        <div className={className}>
          {/* 현재 사진 좋아요 변경과 좋아요 사용자 목록 영역 */}
          <LikeButton
            tagtType={reaction.tagtType}
            tagtNumb={reaction.tagtNumb}
            liked={reaction.likeYsno === "Y"}
            countLabel={reaction.likeCnt}
            disabled={imageLikeUpdatingType === reaction.tagtType}
            onClick={toggleImageLike}
          />
          {/* 현재 사진 댓글 목록 영역 */}
          <CommentButton count={reaction.replCnt} onClick={openImageReplies}/>
        </div>
      );
    };

    /**
     * 서버에 남아 있는 임시 이미지 선택본을 화면 미리보기와 저장 식별값에 반영함
     *
     * @author SeungHyeon.Kang
     * @param drafts 같은 로그인 사용자의 복원 가능한 임시 이미지 목록
     */
    const restoreProfileImageDrafts = (drafts: ProfileImageDraft[]) => {
      // 프로필과 배경 선택본을 각각 기존 사용자 정보 영역에만 반영함
      drafts.forEach((draft) => {
        if (draft.imageType === "PROFILE") {
          setProfileImageDraftToken(draft.draftToken);
          setPreviewImage(draft.previewDataUrl);
          return;
        }

        if (draft.imageType === "BACKGROUND") {
          setBackgroundImageDraftToken(draft.draftToken);
          setPreviewBackground(draft.previewDataUrl);
        }
      });

      // 앱 재시작으로 복원된 선택본이 있으면 사용자 정보 편집 상태를 함께 복원함
      if (drafts.length > 0) {
        setIsEditMode(true);
      }
    };

    useEffect(() => {

      let ignore = false;

      Promise.all([
        queryClient.fetchQuery(getMyProfileOptions()),
        getProfileDraftListApi().catch(() => []),
      ])
        .then(([nextProfile, drafts]) => {

          if (!ignore) {
            syncProfileState(nextProfile);
            restoreProfileImageDrafts(drafts);
          }
        })
        .finally(() => {

          if (!ignore) {
            setIsLoading(false);
          }
        });

      /**
       * 프로필과 독립적으로 독서 활동 요약을 조회함
       *
       * @author HanWon.Jang
       * @return 독서 활동 요약 조회 완료 Promise
       */
      const loadReadingSummary = async () => {

        // 독서 활동 조회 성공과 실패 및 종료 상태를 각각 처리함
        try {
          // 프로필 하단에 표시할 독서 활동 요약을 조회함
          const response = await getMonthlyReadingApi();

          // 컴포넌트가 유지되는 동안에만 독서 활동 요약을 화면 상태에 반영함
          if (!ignore) {
            // 조회한 독서 활동 요약을 프로필 하단 영역에 설정함
            setMonthlySummary(response.data as MonthlyReadingSummary);
          }
        }

          // 독서 활동 조회 실패를 사용자에게 안내함
        catch (error) {
          // 화면을 벗어난 뒤 발생한 응답은 사용자 알림을 띄우지 않음
          if (!ignore) {
            void sweetError(
              /* "조회에 실패했습니다." */ message("frontend.alert.loadFailedTitle"),
              getApiErrorMessage(error, /* "다시 시도해주세요." */ message("frontend.common.tryAgain")),
            );
          }
        }

      };

      // 프로필 조회와 병렬로 독서 활동 요약 조회를 시작함
      void loadReadingSummary();

      return () => {

        ignore = true;
      };
    }, []);

    /**
     * 이전 기간 대비 완료 독서량 말풍선이 열린 상태에서 다른 영역을 누르면 말풍선을 닫음
     * 비교 숫자와 말풍선 자체를 누르는 경우에는 같은 요소 안에서 발생한 클릭으로 판단해 닫지 않음
     *
     * @author HanWon.Jang
     * @return
     */
    useEffect(() => {
      /**
       * handle Document Pointer Down 사용자 동작을 처리함
       *
       * @author HanWon.Jang
       * @param event event 입력값
       * @return 반환값이 없음
       */
      const handleDocumentPointerDown = (event: PointerEvent) => {

        if (!activeDiffTooltip) {
          return;
        }

        const tooltipArea = diffTooltipRefs.current[activeDiffTooltip];
        const target = event.target;

        if (tooltipArea && target instanceof Node && tooltipArea.contains(target)) {
          return;
        }

        setActiveDiffTooltip(null);
      };

      document.addEventListener("pointerdown", handleDocumentPointerDown);

      return () => {

        document.removeEventListener("pointerdown", handleDocumentPointerDown);
      };
    }, [activeDiffTooltip]);

    useEffect(() => {

      return () => {

        if (followListScrollTimeoutRef.current) {
          window.clearTimeout(followListScrollTimeoutRef.current);
        }

      };
    }, []);

    /**
     * 이전 기간 대비 완료 독서 변화량 상세 문구를 info 알림으로 보여줌
     * 월간과 연간 비교 모두 같은 UI 패턴을 사용하므로 비교 단위별 메시지 key만 분기함
     *
     * @author HanWon.Jang
     * @param diff 이전 기간 대비 완료 독서 권수 변화량
     * @param period 비교 단위
     */
    const getReadingDiffMessage = (diff: number, period: ReadingPeriod) => {

      const diffCount = Math.abs(diff);
      const periodMessagePrefix =
        period === "week" ? "weeklyReading" : period === "month" ? "monthlyReading" : "yearlyReading";
      const messageKey =
        diff === 0
          ? `frontend.profile.${periodMessagePrefix}.diffSame`
          : `frontend.profile.${periodMessagePrefix}.${diff > 0 ? "diffMore" : "diffLess"}`;

      // "지난주보다 {0}권 더 읽었어요." 또는 "지난주보다 {0}권 덜 읽었어요." 또는 "지난주와 읽은 량이 동일해요."
      // "지난달보다 {0}권 더 읽었어요." 또는 "지난달보다 {0}권 덜 읽었어요." 또는 "지난달과 읽은 량이 동일해요."
      // "작년보다 {0}권 더 읽었어요." 또는 "작년보다 {0}권 덜 읽었어요." 또는 "작년과 읽은 량이 동일해요."
      return message(messageKey, [diffCount]);
    };

    /**
     * 마이페이지 활동 통계 클릭에 따라 안내 알림 또는 팔로우 목록을 엶
     *
     * @author HanWon.Jang
     * @param action 클릭한 활동 통계의 동작 유형
     * @param summary 안내에 사용할 마이페이지 활동 집계
     * @return 통계 안내 또는 팔로우 목록 열기가 끝난 Promise
     */
    const handleProfileStatClick = async (
      action: ProfileStatAction,
      summary: MonthlyReadingSummary,
    ): Promise<void> => {
      // 팔로우와 팔로워 통계는 기존 사용자 목록 모달을 엶
      if (action === "following" || action === "followers") {
        // 선택한 관계 유형의 사용자 목록을 조회해 표시함
        await handleFollowListOpen(action);
        // 팔로우 목록을 연 뒤 통계 안내 처리를 종료함
        return;
      }

      // 총 읽은 책 통계는 완료한 책 권수를 안내함
      if (action === "totalReadBook") {
        // "총 {0}권의 책을 끝까지 읽었어요!"
        await sweetInfo(message("frontend.profile.stats.totalReadBookAlert", [summary.totalReadBookCnt ?? 0]));
        // 총 읽은 책 안내를 표시한 뒤 처리를 종료함
        return;
      }

      // "독후감에 좋아요를 {0}개 받았어요"
      await sweetInfo(message("frontend.profile.stats.receivedLikeAlert", [summary.receivedLikeCnt ?? 0]));
    };

    /**
     * handle Reading Diff Click 사용자 동작을 처리함
     *
     * @author HanWon.Jang
     * @param diff diff 입력값
     * @param period period 입력값
     * @return 반환값이 없음
     */
    const handleReadingDiffClick = (diff: number, period: ReadingPeriod) => {

      setActiveDiffTooltip((prev) => (prev === period ? null : period));
    };

    /**
     * 사용자가 선택한 이미지를 서버 비공개 임시 저장소에 올리고 서버 미리보기만 화면에 반영함
     * 브라우저가 고해상도 원본을 디코딩하지 않아 모바일 PWA의 메모리 회수를 피함
     *
     * @author SeungHyeon.Kang
     * @param file 사용자가 선택한 이미지 파일
     * @param imageType 프로필 또는 배경 이미지 구분값
     */
    const applyImagePreview = async (
      file: File | undefined,
      imageType: ProfileImageType,
    ): Promise<void> => {
      // 앨범 선택이 취소된 경우 기존 임시 선택본과 미리보기를 유지함
      if (!file) {
        return;
      }

      // 서버와 동일한 MIME 형식과 파일 크기 범위를 벗어나면 업로드를 시작하지 않음
      if (!PROFILE_IMAGE_MIME_TYPES.has(file.type.toLowerCase())
        || file.size > PROFILE_IMAGE_MAX_BYTES) {
        void sweetWarning(
          /* "입력이 필요합니다." */ message("frontend.alert.inputRequired"),
          /* "JPG 또는 PNG 형식의 10MB 이하 이미지 파일만 선택해주세요." */ message("frontend.profile.imageOnly"),
        );
        return;
      }

      try {
        // 방향 보정과 해상도 검증 및 축소 처리를 서버에 위임함
        const draft = await setProfileImageDraftApi(file, imageType);

        if (imageType === "PROFILE") {
          // 서버가 반환한 작은 미리보기와 최종 저장용 임시 식별값만 브라우저 상태에 둠
          setProfileImageDraftToken(draft.draftToken);
          setPreviewImage(draft.previewDataUrl);
          return;
        }

        // 배경 이미지도 원본 File 객체 없이 서버 미리보기와 임시 식별값만 유지함
        setBackgroundImageDraftToken(draft.draftToken);
        setPreviewBackground(draft.previewDataUrl);
      } catch (error) {
        void sweetWarning(
          /* "이미지를 불러올 수 없습니다." */ message("frontend.profile.imagePreviewFailedTitle"),
          getApiErrorMessage(error, /* "선택한 이미지의 안전한 미리보기를 만들 수 없습니다. 다른 이미지를 선택해주세요." */ message("frontend.profile.imagePreviewFailed")),
        );
      }
    };

    /**
     * 앨범에서 선택한 배경 이미지 파일을 안전한 미리보기 처리로 전달함
     *
     * @author SeungHyeon.Kang
     * @param event 배경 이미지 파일 입력 변경 이벤트
     * @return 반환값이 없음
     */
    const handleBgImageChange = (event: ChangeEvent<HTMLInputElement>): void => {
      const file = event.currentTarget.files?.[0];
      // 같은 파일을 다시 선택해도 변경 이벤트가 발생하도록 브라우저 입력값을 비움
      event.currentTarget.value = "";
      // 검증과 축소가 끝난 배경 이미지만 화면 선택 상태에 반영함
      void applyImagePreview(file, "BACKGROUND");
    };

    /**
     * 앨범에서 선택한 프로필 이미지 파일을 안전한 미리보기 처리로 전달함
     *
     * @author SeungHyeon.Kang
     * @param event 프로필 이미지 파일 입력 변경 이벤트
     * @return 반환값이 없음
     */
    const handleProfileImageChange = (event: ChangeEvent<HTMLInputElement>): void => {
      const file = event.currentTarget.files?.[0];
      // 같은 파일을 다시 선택해도 변경 이벤트가 발생하도록 브라우저 입력값을 비움
      event.currentTarget.value = "";
      // 검증과 축소가 끝난 프로필 이미지만 화면 선택 상태에 반영함
      void applyImagePreview(file, "PROFILE");
    };

    /**
     * 독서 요약 행의 펼침 상태를 월간/연간 단위로 전환함
     * 같은 섹션 안에서 두 목록을 독립적으로 열 수 있어 사용자가 비교 중인 목록을 잃지 않게 함
     *
     * @author HanWon.Jang
     * @param period 열거나 닫을 독서 요약 기간 구분값
     */
    const handleReadingSummary = (period: ReadingPeriod) => {

      setExpandedSummary((prev) => ({
        ...prev,
        [period]: !prev[period],
      }));
    };

    /**
     * 요약 목록에서 선택한 책의 독후감 상세 화면으로 이동함
     * 백엔드가 내려준 reptNumb를 그대로 사용해 책 정보가 아닌 사용자의 독후감 상세로 연결함
     *
     * @author HanWon.Jang
     * @param reptNumb 이동할 독후감 번호
     */
    const handleSummaryReportClick = (reptNumb: number) => {

      navigate(`/report/detail/${reptNumb}`);
    };

    /**
     * 커스텀 모달을 닫을 때 fade-out 애니메이션이 끝난 뒤 실제 상태를 제거함
     * sweetAlert, 달력, selectBox 성격의 모달은 이 흐름을 사용하지 않고 각 컴포넌트의 기본 동작을 유지함
     *
     * @author HanWon.Jang
     * @param modal 닫을 마이페이지 커스텀 모달 구분값
     * @return fade-out 완료 Promise
     */
    const closeProfileModal = (modal: ProfileModalType) => {

      setClosingModal(modal);

      return new Promise<void>((resolve) => {

        window.setTimeout(() => {

          // 현재 읽는 책 안내 모달은 선택한 독후감 상태를 비워 화면에서 제거함
          if (modal === "currentReading") {
            // 닫기 애니메이션이 끝난 현재 읽는 책 안내 모달 상태를 초기화함
            setCurrentReadingReport(null);
          }

          if (modal === "goal") {
            setIsGoalModalOpen(false);
            setIsGoalHelpModalOpen(false);
          }

          if (modal === "goalHelp") {
            setIsGoalHelpModalOpen(false);
          }

          if (modal === "followList") {
            closeFollowList();
            setIsFollowListScrolling(false);
          }

          setClosingModal((current) => (current === modal ? null : current));
          resolve();
        }, MODAL_CLOSE_DELAY_MS);
      });
    };

    /**
     * 현재 읽고 있는 책을 눌렀을 때 수정 화면으로 연결하는 안내 모달을 엶
     *
     * @author HanWon.Jang
     * @param report 선택한 현재 읽고 있는 책 정보
     * @return 반환값이 없음
     */
    const handleCurrentReadingClick = (report: ReadingSummaryReport) => {

      // 이전 모달의 닫기 애니메이션 상태가 새 안내 모달에 남지 않게 초기화함
      setClosingModal(null);
      // 수정 화면으로 전달할 현재 읽는 책을 안내 모달 상태에 설정함
      setCurrentReadingReport(report);
    };

    /**
     * handle Follow List Open 사용자 동작을 처리함
     *
     * @author HanWon.Jang
     * @param type type 입력값
     * @return 반환값이 없음
     * @throws API 요청 또는 비동기 처리 실패 시 발생
     */
    const handleFollowListOpen = async (type: FollowListType) => {
      // 이전 모달의 닫기 상태를 초기화하고 공통 팔로우 목록 첫 페이지를 조회함
      setClosingModal(null);
      setIsFollowListScrolling(false);
      // 본인 팔로우 목록 모달을 열고 첫 서버 페이지를 조회함
      await openFollowList(type);
    };

    /**
     * handle Follow List Scroll 사용자 동작을 처리함
     *
     * @author HanWon.Jang
     * @return 반환값이 없음
     */
    const handleFollowListScroll = () => {

      setIsFollowListScrolling(true);

      if (followListScrollTimeoutRef.current) {
        window.clearTimeout(followListScrollTimeoutRef.current);
      }

      followListScrollTimeoutRef.current = window.setTimeout(() => {

        setIsFollowListScrolling(false);
        followListScrollTimeoutRef.current = null;
      }, 650);
    };

    /**
     * handle Follow List User Click 사용자 동작을 처리함
     *
     * @author HanWon.Jang
     * @param userNumb user Numb 입력값
     * @return 반환값이 없음
     */
    const handleFollowListUserClick = (userNumb: number) => {

      void closeProfileModal("followList").then(() => {

        navigate(`/social/profile/${userNumb}`);
      });
    };

    /**
     * 내 팔로우 목록 사용자의 현재 관계에 맞춰 팔로우 또는 언팔로우 API를 호출함
     *
     * @author HanWon.Jang
     * @param user 관계를 변경할 팔로우 목록 사용자
     * @return 팔로우 관계와 내 프로필 통계 갱신이 끝난 Promise
     * @throws 팔로우 또는 내 프로필 통계 요청 실패 시 발생
     */
    const handleFollowStatusClick = async (user: FollowUser) => {

      // 다른 관계 변경이 진행 중이거나 내 계정 행이면 추가 조작을 허용하지 않음
      if (followUpdatingUserNumb || user.meYsno === "Y") {
        // 현재 목록 상태를 유지하고 종료함
        return;
      }

      // 목록 사용자를 내가 팔로우 중인지 버튼명으로 판정함
      const isFollowing = isFollowedByMe(user.followStatName);

      // 팔로잉 또는 친구 상태를 해제하기 전에 사용자 확인을 받음
      if (isFollowing) {
        const result = await sweetConfirm({
          title: /* "언팔로우하시겠어요?" */ message("frontend.social.unfollow.title"),
          text: /* "팔로잉 목록에서 삭제돼요." */ message("frontend.social.unfollow.text"),
          confirmButtonText: /* "언팔로우" */ message("frontend.social.unfollow.confirm"),
          cancelButtonText: /* "취소" */ message("frontend.common.cancel"),
        });

        // 사용자가 취소하면 기존 관계를 유지함
        if (!result.isConfirmed) {
          // 팔로우 관계 변경 없이 종료함
          return;
        }
      }

      setFollowUpdatingUserNumb(user.userNumb);

      try {
        const response =
          isFollowing
            ? await delSocialFollowApi(user.userNumb)
            : await setSocialFollowApi(user.userNumb);

        setFollowUsers((prev) =>
          prev.map((item) =>
            item.userNumb === user.userNumb
              ? {...item, followStatName: response.data?.followStatName ?? item.followStatName}
              : item,
          ),
        );

        const summaryResponse = await getMonthlyReadingApi();
        setMonthlySummary(summaryResponse.data as MonthlyReadingSummary);
      } catch (error) {
        void sweetError(
          /* "수정에 실패했습니다." */ message("frontend.alert.updateFailedTitle"),
          getApiErrorMessage(error, /* "다시 시도해주세요." */ message("frontend.common.tryAgain")),
        );
      } finally {
        setFollowUpdatingUserNumb(null);
      }
    };

    /**
     * 선택한 현재 읽는 책의 상세 화면으로 이동하고 전체 편집을 바로 시작함
     *
     * @author HanWon.Jang
     * @return 반환값이 없음
     */
    const handleReadingEditClick = () => {

      // 선택한 현재 읽는 책이 없으면 잘못된 상세 경로로 이동하지 않음
      if (!currentReadingReport) {
        // 수정 화면 이동 없이 현재 모달을 유지함
        return;
      }

      // 선택한 독후감의 상세 화면이 처음부터 전체 편집 상태로 열리게 이동함
      navigate(`/report/detail/${currentReadingReport.reptNumb}`, {
        state: {startEditing: true},
      });
    };

    /**
     * 목표 설정 모달을 열 때 현재 저장된 목표값을 입력값에 반영함
     * 아직 목표가 없으면 빈 값으로 시작해 사용자가 직접 입력하도록 유도함
     *
     * @author HanWon.Jang
     * @return
     */
    const handleGoalModalOpen = async () => {

      let nextSummary = monthlySummary;
      const copyablePreviousGoalPeriods = getCopyableGoalPeriods(nextSummary);

      if (nextSummary && copyablePreviousGoalPeriods.length > 0) {
        const confirmResult = await sweetConfirm({
          title: /* "지난 목표를 가져올까요?" */ message("frontend.profile.goal.copyPreviousTitle"),
          text: getCopyGoalConfirmText(nextSummary, copyablePreviousGoalPeriods),
          confirmButtonText: /* "가져오기" */ message("frontend.profile.goal.copyPreviousConfirm"),
          cancelButtonText: /* "직접 설정" */ message("frontend.profile.goal.copyPreviousCancel"),
        });

        if (confirmResult.isConfirmed) {
          try {
            setIsGoalSaving(true);

            /**
             * 지난 독서 목표를 복사하고 현재 요약 상태에 반영함
             *
             * @author SeungHyeon.Kang
             * @return 복사한 월간 독서 요약 Promise
             * @throws 지난 목표 복사 또는 응답 검증에 실패하면 발생함
             */
            const copyPreviousGoal = async (): Promise<MonthlyReadingSummary> => {
              // 서버에서 복사된 지난 목표와 최신 독서 요약을 조회함
              const response = await copyPrevReadingGoalApi();
              // 이후 목표 입력에도 사용할 최신 요약을 반환값으로 보관함
              const copiedSummary = response.data as MonthlyReadingSummary;
              // 화면의 목표와 달성 현황을 복사 결과로 갱신함
              setMonthlySummary(copiedSummary);
              // 호출부에서 최신 목표 요약을 이어서 사용할 수 있도록 반환함
              return copiedSummary;
            };

            // 목표 복사 완료 후 처리 중 알림을 같은 저장 성공 알림으로 전환함
            nextSummary = await runBlockingOperation(copyPreviousGoal, {
              success: {
                /* "목표가 저장되었습니다." */ title: message("frontend.profile.goal.savedTitle"),
                /* "주간, 월간, 연간 독서 목표를 반영했습니다." */ text: message("frontend.profile.goal.saved"),
              },
            });
            return;
          } catch (error) {
            void sweetError(
              /* "수정에 실패했습니다." */ message("frontend.alert.updateFailedTitle"),
              getApiErrorMessage(error, /* "다시 시도해주세요." */ message("frontend.common.tryAgain")),
            );
            return;
          } finally {
            setIsGoalSaving(false);
          }
        }
      }

      setWeekGoalCnt(nextSummary?.weekGoalCnt ? String(nextSummary.weekGoalCnt) : "");
      setMonthGoalCnt(nextSummary?.monthGoalCnt ? String(nextSummary.monthGoalCnt) : "");
      setYearGoalCnt(nextSummary?.yearGoalCnt ? String(nextSummary.yearGoalCnt) : "");
      setClosingModal(null);
      setIsGoalModalOpen(true);
    };

    /**
     * 목표 입력값을 1 이상 숫자만 남긴 문자열로 정리함
     *
     * @author HanWon.Jang
     * @param value 사용자가 입력한 목표 권수
     * @return 숫자로만 구성된 목표 권수 문자열
     */
    const normalizeGoalCount = (value: string) =>
      value.replace(/[^0-9]/g, "").replace(/^0+/, "");

    /**
     * get Reading Remain Rate 정보를 조회함
     *
     * @author HanWon.Jang
     * @param remainDays remain Days 입력값
     * @return 처리 결과
     */
    const getReadingRemainRate = (remainDays: number) => {
      // 현재 읽고 있는 책의 남은 기간 색상은 전체 목표기간 비율이 아니라 남은 10일을 기준으로 판단함
      // 10일 이상 남으면 가장 여유 있는 색상, 0일에 가까워질수록 기존 색상 단계가 내려감
      return Math.max(0, Math.min(100, Math.round((Math.max(remainDays, 0) / 10) * 100)));
    };

    /**
     * 현재 읽고 있는 책의 목표 종료일까지 남은 기간 정보를 렌더링함
     * 전체 목표기간 대비 남은 비율을 색상 기준으로 사용해 기간이 가까워질수록 붉은 계열로 표시함
     *
     * @author HanWon.Jang
     * @param reports 현재 읽고 있는 독후감 목록
     * @return 현재 읽고 있는 책 섹션 JSX
     */
    const renderCurrentReports = (reports: ReadingSummaryReport[] = []) => {

      if (reports.length === 0) {
        return null;
      }

      return (
        /* 현재 읽고 있는 책 영역 */
        <section
          className={styles.monthlySummary}
          aria-label={/* "현재 읽고 있는 책" */ message("frontend.profile.currentReading.title")}
        >
          <div className={styles.currentReadingSection}>
            {/* 현재 읽고 있는 책 제목 영역 */}
            <h2 className={`${styles.currentReadingTitle} ${styles.myPageSectionTitle}`}>
              {/* "현재 읽고 있는 책" */ message("frontend.profile.currentReading.title")}
            </h2>
            {/* 현재 읽고 있는 책 목록 영역 */}
            <div className={styles.currentReadingList}>
              {reports.map((report) => {

                const remainDays = getRemainDaysUntil(report.reptEndt);
                const remainRate = getReadingRemainRate(remainDays);
                const remainColor = getGoalProgressColor(remainRate);
                const isExpired = remainDays <= 0;
                const content = (
                  <>
                    {/* 현재 읽고 있는 책 표지 영역 */}
                    <img
                      className={styles.readingSummaryCover}
                      src={getBookCoverImageSource(report.bookCvim)}
                      onError={handleBookCoverImageError}
                      alt=""
                    />
                    {/* 현재 읽고 있는 책 정보 영역 */}
                    <span className={`${styles.currentReadingText} ${styles.myPageCurrentReadingText}`}>
                    <span
                      className={styles.readingSummaryBookTitleButton}
                      role="link"
                      tabIndex={0}
                      onClick={(event) => {
                        // 제목은 카드의 수정 안내 동작과 분리하여 도서 정보 화면으로 바로 이동함
                        event.stopPropagation();
                        navigate(`/book/info/${report.reptNumb}`);
                      }}
                      onKeyDown={(event) => {

                        if (event.key !== "Enter" && event.key !== " ") {
                          return;
                        }

                        event.preventDefault();
                        event.stopPropagation();
                        navigate(`/book/info/${report.reptNumb}`);
                      }}
                    >
                      {report.bookTitl || /* "도서 정보가 없습니다." */ message("frontend.common.noBookInfo")}
                    </span>
                      {/* 저자 아래에 목표 종료일을 배치하고 오른쪽에 남은 기간을 표시하는 영역 */}
                      <span className={styles.currentReadingMeta}>
                      <span className={styles.currentReadingBookMetaGroup}>
                        {/* 현재 읽는 책의 저자명 */}
                        {report.bookAthr && (
                          <span className={styles.readingSummaryBookMeta}>{report.bookAthr}</span>
                        )}
                        {/* 현재 읽는 책의 목표 독서 종료일 */}
                        {report.reptEndt && (
                          <span className={styles.readingSummaryBookMeta}>
                            {/* "종료일 {0}" */}
                            {message("frontend.profile.currentReading.endDate", [
                              formatDashedDateToDot(report.reptEndt),
                            ])}
                          </span>
                        )}
                      </span>
                      <span
                        className={styles.currentReadingRemain}
                        style={{color: remainColor}}
                      >
                        {isExpired
                          ? /* "목표기간이 지났어요." */ message("frontend.profile.currentReading.expired")
                          : /* "남은 목표일 {0}일" */ message("frontend.profile.currentReading.remain", [remainDays])}
                      </span>
                    </span>
                  </span>
                  </>
                );

                return (
                  <button
                    className={styles.currentReadingButton}
                    key={report.reptNumb}
                    type="button"
                    onClick={() => handleCurrentReadingClick(report)}
                  >
                    {content}
                  </button>
                );
              })}
            </div>
          </div>
        </section>
      );
    };

    /**
     * render Profile Stats 화면 요소를 구성함
     *
     * @author HanWon.Jang
     * @param summary summary 입력값
     * @return 구성된 화면 요소
     */
    const renderProfileStats = (summary: MonthlyReadingSummary) => {

      const stats: Array<{
        label: string;
        value: string;
        action: ProfileStatAction;
      }> = [
        {
          label: /* "총 읽은 책" */ message("frontend.profile.stats.totalReadBook"),
          value: /* "{0}권" */ message("frontend.profile.stats.bookCount", [summary.totalReadBookCnt ?? 0]),
          action: "totalReadBook",
        },
        {
          label: /* "팔로우" */ message("frontend.common.following"),
          value: /* "{0}명" */ message("frontend.profile.stats.userCount", [summary.followingCnt ?? 0]),
          action: "following",
        },
        {
          label: /* "팔로워" */ message("frontend.common.followers"),
          value: /* "{0}명" */ message("frontend.profile.stats.userCount", [summary.followerCnt ?? 0]),
          action: "followers",
        },
        {
          label: /* "좋아요수" */ message("frontend.profile.stats.receivedLike"),
          value: /* "{0}개" */ message("frontend.profile.stats.likeCount", [summary.receivedLikeCnt ?? 0]),
          action: "receivedLike",
        },
      ];

      return (
        /* 프로필 활동 통계 영역 */
        <section className={styles.monthlySummary} aria-label={/* "내 활동" */ message("frontend.profile.stats.title")}>
          <div className={styles.myProfileStatsSummary}>
            {/* 총 읽은 책과 팔로우 및 좋아요 통계 영역 */}
            <div className={styles.goalAchievementGrid}>
              {stats.map((stat) => (
                /* 프로필 활동 통계 개별 항목 영역 */
                <div className={styles.goalAchievementItem} key={stat.label}>
                  <button
                    className={styles.profileStatsButton}
                    type="button"
                    onClick={() => void handleProfileStatClick(stat.action, summary)}
                  >
                    <span className={styles.goalAchievementLabel}>{stat.label}</span>
                    <strong className={styles.goalAchievementCount}>{stat.value}</strong>
                  </button>
                </div>
              ))}
            </div>
          </div>
        </section>
      );
    };

    /**
     * 목표 입력 모달에서 버튼 클릭으로 월별/연도별 목표 권수를 1권 단위로 증감함
     * 목표 권수는 저장 가능한 최소 단위가 1권이므로 감소 버튼을 반복해서 눌러도 1 미만으로 내려가지 않게 제한함
     *
     * @author HanWon.Jang
     * @param period 조정할 목표 기간
     * @param amount 증감할 권수
     * @return
     */
    const handleGoalCountStep = (period: ReadingPeriod, amount: number) => {

      const setGoalCnt =
        period === "week"
          ? setWeekGoalCnt
          : period === "month"
            ? setMonthGoalCnt
            : setYearGoalCnt;

      setGoalCnt((prev) => {

        const isEmptyGoalCount = prev.trim() === "";
        const currentCount = Number(prev);
        const nextCount = isEmptyGoalCount ? 1 : currentCount + amount;

        // 빈 입력에서 스테퍼를 처음 조작하면 증감 방향과 관계없이 최소 목표인 1권부터 시작함
        return String(Math.max(1, nextCount));
      });
    };

    /**
     * 월간/연간 목표 권수를 저장하고 저장 후 갱신된 요약 정보를 화면에 반영함
     *
     * @author HanWon.Jang
     * @return
     */
    /**
     * 목표 기간에 맞는 화면 라벨 메시지 key를 반환함
     * 같은 기간 분기값을 입력 카드, 제한 안내, 저장 전 검증에서 함께 사용해 화면 안내와 검증 기준이 어긋나지 않게 함
     *
     * @author HanWon.Jang
     * @param period 목표 기간 구분값
     * @return 기간 라벨 메시지 key
     */
    const getGoalPeriodLabelKey = (period: ReadingPeriod) => {

      if (period === "week") {
        return "frontend.profile.goal.weekLabel";
      }

      if (period === "month") {
        return "frontend.profile.goal.monthLabel";
      }

      return "frontend.profile.goal.yearLabel";
    };

    /**
     * 현재 모달 입력값 중 기간에 맞는 목표 권수를 숫자로 반환함
     * 빈 문자열은 Number 변환 시 0이 되므로 필수 입력 검증과 같은 기준으로 처리됨
     *
     * @author HanWon.Jang
     * @param period 목표 기간 구분값
     * @return 사용자가 입력한 목표 권수
     */
    const getGoalInputCount = (period: ReadingPeriod) => {

      if (period === "week") {
        return Number(weekGoalCnt);
      }

      if (period === "month") {
        return Number(monthGoalCnt);
      }

      return Number(yearGoalCnt);
    };

    /**
     * 서버에 저장되어 있던 기간별 목표 권수를 반환함
     * 저장된 값과 입력값이 다른 기간만 수정 제한 검증을 적용해야 같은 값을 다시 저장할 때 수정 횟수를 소모하지 않음
     *
     * @author HanWon.Jang
     * @param period 목표 기간 구분값
     * @return 서버에 저장된 목표 권수
     */
    const getSavedGoalCount = (period: ReadingPeriod) => {

      if (period === "week") {
        return monthlySummary?.weekGoalCnt ?? null;
      }

      if (period === "month") {
        return monthlySummary?.monthGoalCnt ?? null;
      }

      return monthlySummary?.yearGoalCnt ?? null;
    };

    /**
     * 기간별 목표가 이미 설정되어 있는지 확인함
     * 최초 설정은 수정 제한 대상이 아니므로 기존 목표가 있는 기간만 수정 제한 안내와 저장 전 차단에 사용함
     *
     * @author HanWon.Jang
     * @param period 목표 기간 구분값
     * @return 기존 목표가 있으면 true
     */
    const isGoalAlreadySet = (period: ReadingPeriod) => {

      if (period === "week") {
        return Boolean(monthlySummary?.weekGoalSet);
      }

      if (period === "month") {
        return Boolean(monthlySummary?.monthGoalSet);
      }

      return Boolean(monthlySummary?.yearGoalSet);
    };

    /**
     * 기간별로 앞으로 남은 목표 수정 횟수를 반환함
     * 값은 백엔드 제한 로직과 같은 기준으로 내려온 응답값을 사용해 화면 선검증과 서버 검증의 기준을 맞춤
     *
     * @author HanWon.Jang
     * @param period 목표 기간 구분값
     * @return 남은 수정 가능 횟수
     */
    const getGoalRemainUpdateCount = (period: ReadingPeriod) => {

      if (period === "week") {
        return monthlySummary?.weekGoalRemainUpdateCnt ?? 0;
      }

      if (period === "month") {
        return monthlySummary?.monthGoalRemainUpdateCnt ?? 0;
      }

      return monthlySummary?.yearGoalRemainUpdateCnt ?? 0;
    };

    /**
     * 목표 수정 제한 기간이 시작되기 전까지 남은 일수를 반환함
     * 0이면 이미 수정 가능 기간이 끝난 상태로 보고 저장 전에 사용자에게 안내함
     *
     * @author HanWon.Jang
     * @param period 목표 기간 구분값
     * @return 수정 가능 기간 남은 일수
     */
    const getGoalEditableRemainDays = (period: ReadingPeriod) => {

      if (period === "week") {
        return monthlySummary?.weekGoalEditableRemainDays ?? 0;
      }

      if (period === "month") {
        return monthlySummary?.monthGoalEditableRemainDays ?? 0;
      }

      return monthlySummary?.yearGoalEditableRemainDays ?? 0;
    };

    /**
     * 목표 기간이 마감 규칙 때문에 잠겨 있는지 반환함
     * 수정 횟수가 남아 있어도 기간이 잠긴 경우에는 프론트에서 먼저 저장을 차단함
     *
     * @author HanWon.Jang
     * @param period 목표 기간 구분값
     * @return 기간 제한으로 수정할 수 없으면 true
     */
    const isGoalUpdateLocked = (period: ReadingPeriod) => {

      if (period === "week") {
        return Boolean(monthlySummary?.weekGoalUpdateLocked);
      }

      if (period === "month") {
        return Boolean(monthlySummary?.monthGoalUpdateLocked);
      }

      return Boolean(monthlySummary?.yearGoalUpdateLocked);
    };

    /**
     * 모달 입력값이 기존 목표보다 낮아졌는지 확인함
     * 목표를 올리는 것은 언제든 허용되어야 하므로 낮아진 기간만 목표 내리기 제한 검증과 확인 alert 대상이 됨
     *
     * @author HanWon.Jang
     * @param period 목표 기간 구분값
     * @return 기존 목표보다 입력 목표가 낮으면 true
     */
    const isGoalDecreased = (period: ReadingPeriod) => {

      const savedGoalCount = getSavedGoalCount(period);
      return (
        isGoalAlreadySet(period) &&
        savedGoalCount !== null &&
        getGoalInputCount(period) < savedGoalCount
      );
    };

    /**
     * 저장 전 목표 내리기 제한에 걸리는 기간의 안내 문구를 반환함
     * 제한이 없는 기간은 빈 문자열을 반환해 저장 검증 루프에서 다음 기간을 계속 확인할 수 있게 함
     *
     * @author HanWon.Jang
     * @param period 목표 기간 구분값
     * @return 제한 안내 메시지
     */
    const getGoalEditBlockMessage = (period: ReadingPeriod) => {

      if (!isGoalDecreased(period)) {
        return "";
      }

      // "주간" 또는 "월간" 또는 "연간"
      const label = message(getGoalPeriodLabelKey(period));

      if (getGoalRemainUpdateCount(period) <= 0) {
        return /* "{0} 목표는 내리기 횟수를 모두 사용했습니다." */ message("frontend.profile.goal.downCountBlocked", [label]);
      }

      if (isGoalUpdateLocked(period)) {
        return /* "{0} 목표는 내리기 가능 기간이 지났습니다." */ message("frontend.profile.goal.downPeriodBlocked", [label]);
      }

      return "";
    };

    /**
     * 목표 기간명과 내리기 가능 횟수 및 남은 기간 안내
     *
     * @author HanWon.Jang
     * @param period 목표 기간 구분값
     * @return 목표 수정 제한 안내 JSX
     */
    const renderGoalLimitInfo = (period: ReadingPeriod) => {

      const remainUpdateCount = getGoalRemainUpdateCount(period);
      const remainDays = getGoalEditableRemainDays(period);
      const isLocked = isGoalUpdateLocked(period);
      const isUnset = !isGoalAlreadySet(period);
      const isDownClosed = !isUnset && (remainUpdateCount <= 0 || isLocked);

      return (
        /* 목표 권수 수정 가능 횟수와 제한 기간 안내 영역 */
        <div className={styles.goalLimitInfo}>
          {/* 목표 기간명과 수정 가능 상태 영역 */}
          <div className={styles.goalPeriodHeading}>
            <label htmlFor={`${period}-goal-count`}>
              {/* "주간" 또는 "월간" 또는 "연간" */}
              {message(getGoalPeriodLabelKey(period))}
            </label>
            {/* 내리기 마감 전의 첫 설정 또는 남은 횟수 표시 */}
            {!isDownClosed ? (
              <span className={styles.goalLimitPill}>
              {isUnset
                ? /* "첫 설정 가능" */ message("frontend.profile.goal.firstSet")
                : /* "내리기 {0}회" */ message("frontend.profile.goal.remainDown", [remainUpdateCount])}
            </span>
            ) : null}
          </div>
          {/* 목표 내리기 마감 또는 남은 기간 영역 */}
          {isDownClosed ? (
            <span className={styles.goalLimitDanger}>
            {/* "내리기 마감" */ message("frontend.profile.goal.downLocked")}
          </span>
          ) : (
            <span className={styles.goalLimitMuted}>
              {/* "내리기 {0}일 남음" */ message("frontend.profile.goal.downRemainDays", [remainDays])}
            </span>
          )}
        </div>
      );
    };

    /**
     * handle Goal Submit 사용자 동작을 처리함
     *
     * @author HanWon.Jang
     * @return 반환값이 없음
     * @throws API 요청 또는 비동기 처리 실패 시 발생
     */
    const handleGoalSubmit = async () => {

      const nextWeekGoalCnt = Number(weekGoalCnt);
      const nextMonthGoalCnt = Number(monthGoalCnt);
      const nextYearGoalCnt = Number(yearGoalCnt);

      if (nextWeekGoalCnt <= 0 || nextMonthGoalCnt <= 0 || nextYearGoalCnt <= 0) {
        void sweetWarning(
          /* "입력이 필요합니다." */ message("frontend.alert.inputRequired"),
          /* "월간과 연간 목표를 1권 이상으로 입력해주세요." */ message("frontend.profile.goal.required"),
        );
        return;
      }

      const blockMessage = GOAL_PERIODS.map(getGoalEditBlockMessage).find(Boolean);

      if (blockMessage) {
        void sweetWarning(/* "목표를 내릴 수 없어요." */ message("frontend.profile.goal.downBlockedTitle"), blockMessage);
        return;
      }

      const downGoalLabels = GOAL_PERIODS.filter(isGoalDecreased).map((period) =>
        // "주간" 또는 "월간" 또는 "연간"
        message(getGoalPeriodLabelKey(period)),
      );

      if (downGoalLabels.length > 0) {
        const confirmResult = await sweetConfirm({
          title: /* "목표를 내릴까요?" */ message("frontend.profile.goal.downConfirmTitle"),
          text: /* "{0} 목표를 내리면 목표 내리기 횟수가 사용됩니다." */ message("frontend.profile.goal.downConfirmText", [downGoalLabels.join(", ")]),
          confirmButtonText: /* "확인" */ message("frontend.common.confirm"),
          cancelButtonText: /* "취소" */ message("frontend.common.cancel"),
        });

        if (!confirmResult.isConfirmed) {
          return;
        }
      }

      try {
        setIsGoalSaving(true);

        /**
         * 독서 목표를 저장하고 화면 요약 및 목표 모달 상태를 갱신함
         *
         * @author SeungHyeon.Kang
         * @return 독서 목표 저장 완료 Promise
         * @throws 독서 목표 저장 또는 목표 모달 정리에 실패하면 발생함
         */
        const saveReadingGoal = async (): Promise<void> => {
          // 입력한 주간, 월간, 연간 목표를 서버에 저장함
          const response = await updateReadingGoalApi({
            weekGoalCnt: nextWeekGoalCnt,
            monthGoalCnt: nextMonthGoalCnt,
            yearGoalCnt: nextYearGoalCnt,
          });
          // 화면의 목표와 달성 현황을 최신 응답으로 갱신함
          setMonthlySummary(response.data as MonthlyReadingSummary);
          // 저장이 끝난 목표 설정 모달을 닫음
          await closeProfileModal("goal");
        };

        // 목표 저장 완료 후 처리 중 알림을 같은 저장 성공 알림으로 전환함
        await runBlockingOperation(saveReadingGoal, {
          success: {
            /* "목표가 저장되었습니다." */ title: message("frontend.profile.goal.savedTitle"),
            /* "주간, 월간, 연간 독서 목표를 반영했습니다." */ text: message("frontend.profile.goal.saved"),
          },
        });
      } catch (error) {
        void sweetError(
          /* "수정에 실패했습니다." */ message("frontend.alert.updateFailedTitle"),
          getApiErrorMessage(error, /* "다시 시도해주세요." */ message("frontend.common.tryAgain")),
        );
      } finally {
        setIsGoalSaving(false);
      }
    };

    /**
     * 월간/연간 독서 요약 행과 펼침 목록을 공통 구조로 렌더링함
     * 권수 비교 버튼은 별도 버튼으로 유지하고, 제목/권수 영역을 누르면 목록만 부드럽게 열리도록 분리함
     *
     * @author HanWon.Jang
     * @param period 월간 또는 연간 구분값
     * @param code 달력 아이콘 안에 표시할 월 영문 또는 연도
     * @param titleKey 제목 메시지 key
     * @param countKey 권수 메시지 key
     * @param count 현재 기간 완료 권수
     * @param diff 이전 기간 대비 증감 권수
     * @param diffAriaKey 증감 버튼 접근성 메시지 key
     * @param reports 펼침 영역에 표시할 완료 독후감 목록
     * @return 독서 요약 행 JSX
     */
    const renderReadingSummaryRow = (
      period: ReadingPeriod,
      code: string | undefined,
      titleKey: string,
      countKey: string,
      count: number,
      diff: number,
      diffAriaKey: string,
      reports: ReadingSummaryReport[] = [],
    ) => {

      const isExpanded = expandedSummary[period];
      const hasReports = reports.length > 0;
      const goalCnt =
        period === "week"
          ? monthlySummary?.weekGoalCnt
          : period === "month"
            ? monthlySummary?.monthGoalCnt
            : monthlySummary?.yearGoalCnt;
      const goalRate =
        period === "week"
          ? monthlySummary?.weekGoalRate ?? 0
          : period === "month"
            ? monthlySummary?.monthGoalRate ?? 0
            : monthlySummary?.yearGoalRate ?? 0;
      const goalSet =
        period === "week"
          ? Boolean(monthlySummary?.weekGoalSet)
          : period === "month"
            ? Boolean(monthlySummary?.monthGoalSet)
            : Boolean(monthlySummary?.yearGoalSet);
      const goalProgressColor = getGoalProgressColor(goalRate);

      return (
        /* 기간별 독서 목표와 달성 현황 영역 */
        <div>
          {/* 기간별 독서 완료 현황 영역 */}
          <div className={styles.readingSummaryRow}>
            <button
              className={hasReports ? styles.readingSummaryToggle : styles.readingSummaryToggleStatic}
              type="button"
              aria-expanded={hasReports ? isExpanded : undefined}
              disabled={!hasReports}
              onClick={() => {

                if (hasReports) {
                  handleReadingSummary(period);
                }
              }}
            >
              {/* 주간과 월간 및 연간 구분 달력 영역 */}
              <div className={styles.monthlyCalendarIcon} aria-hidden="true">
                <span className={styles.monthlyCalendarRing}/>
                <span className={styles.monthlyCalendarMonth}>{code ?? ""}</span>
              </div>
              {/* 기간별 완료 권수 영역 */}
              <div className={styles.monthlySummaryText}>
                {/* "이번 주에 읽은 책" 또는 "이번 달에 읽은 책" 또는 "올해 읽은 책" */}
                <span className={styles.monthlySummaryLabel}>{message(titleKey)}</span>
                <strong className={styles.monthlySummaryCount}>
                  {/* "{0}권" */}
                  {message(countKey, [count])}
                </strong>
              </div>
              {hasReports && (
                <span
                  className={
                    isExpanded
                      ? styles.readingSummaryChevronOpen
                      : styles.readingSummaryChevron
                  }
                  aria-hidden="true"
                >
                <img src={"/img/icons/arrow-bottom.svg"} width={"14px"} alt="arrow"/>
              </span>
              )}
            </button>
            {/* 이전 기간 대비 독서량 증감 영역 */}
            <div
              className={styles.monthlyDiffTooltipWrap}
              ref={(element) => {

                diffTooltipRefs.current[period] = element;
              }}
            >
              <button
                className={
                  diff === 0
                    ? styles.monthlyDiffNeutral
                    : diff > 0
                      ? styles.monthlyDiffUp
                      : styles.monthlyDiffDown
                }
                type="button"
                aria-label={
                  /* "지난주 대비 독서 권수 변화" 또는 "지난달 대비 독서 권수 변화" 또는 "작년 대비 독서 권수 변화" */
                  message(diffAriaKey)
                }
                aria-expanded={activeDiffTooltip === period}
                onClick={() => handleReadingDiffClick(diff, period)}
              >
                {formatReadingDiff(diff)}
              </button>
              {activeDiffTooltip === period && (
                <div className={styles.monthlyDiffTooltip} role="tooltip">
                  {getReadingDiffMessage(diff, period)}
                </div>
              )}
            </div>
          </div>
          {/* 목표 권수와 달성률 진행 상태 영역 */}
          <div className={styles.goalProgressRow}>
          <span className={styles.goalProgressTarget}>
            {goalSet ? /* "목표 {0}권" */ message("frontend.profile.goal.target", [goalCnt ?? 0]) : ""}
          </span>
            <div className={styles.goalProgressTrack}>
            <span
              className={styles.goalProgressFill}
              style={{
                width: `${Math.min(100, goalRate)}%`,
                backgroundColor: goalProgressColor,
              }}
            />
            </div>
            <span
              className={styles.goalProgressRate}
              style={goalSet ? {color: goalProgressColor} : undefined}
            >
            {goalSet
              ? /* "{0}%" */ message("frontend.profile.goal.rate", [goalRate])
              : /* "미설정" */ message("frontend.profile.goal.unset")}
          </span>
          </div>
          {/* 기간별 완료 도서 펼침 영역 */}
          {hasReports && (
            <div
              className={
                isExpanded
                  ? styles.readingSummaryPanelOpen
                  : styles.readingSummaryPanel
              }
            >
              {/* 기간별 완료 도서 목록 영역 */}
              <div className={styles.readingSummaryPanelInner}>
                {reports.map((report) => (
                  /* 기간별 완료 도서 개별 항목 영역 */
                  <button
                    className={styles.readingSummaryReport}
                    type="button"
                    key={report.reptNumb}
                    onClick={() => handleSummaryReportClick(report.reptNumb)}
                  >
                    {/* 완료 도서 표지 영역 */}
                    <img
                      className={styles.readingSummaryCover}
                      src={getBookCoverImageSource(report.bookCvim)}
                      onError={handleBookCoverImageError}
                      alt=""
                    />
                    {/* 완료 도서 제목과 저자 및 별점 영역 */}
                    <span className={styles.readingSummaryBookText}>
                    <span
                      className={styles.readingSummaryBookTitleButton}
                      role="link"
                      tabIndex={0}
                      onClick={(event) => {
                        // 목록 전체 클릭은 독후감 상세, 제목 클릭은 해당 독후감의 도서 정보로 분리함
                        event.stopPropagation();
                        navigate(`/book/info/${report.reptNumb}`);
                      }}
                      onKeyDown={(event) => {

                        if (event.key !== "Enter" && event.key !== " ") {
                          return;
                        }

                        event.preventDefault();
                        event.stopPropagation();
                        navigate(`/book/info/${report.reptNumb}`);
                      }}
                    >
                      {report.bookTitl || /* "도서 정보가 없습니다." */ message("frontend.common.noBookInfo")}
                    </span>
                    <span className={styles.readingSummaryBookMeta}>
                      <span className={styles.readingSummaryMetaLine}>
                        {report.bookAthr && (
                          <span className={styles.readingSummaryMetaText}>
                            {report.bookAthr}
                          </span>
                        )}
                        {report.bookAthr && getReadingEndDateText(report) && (
                          <span>|</span>
                        )}
                        {getReadingEndDateText(report) && (
                          <span className={styles.readingSummaryMetaText}>
                            {getReadingEndDateText(report)}
                          </span>
                        )}
                        {(report.bookAthr || getReadingEndDateText(report)) && (
                          <span>|</span>
                        )}
                        <span className={styles.readingSummaryGrade}>
                          {getReadingGradeText(report.reptGrde)}
                        </span>
                      </span>
                    </span>
                  </span>
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      );
    };

    /**
     * 프로필 수정 버튼 클릭 시 기본 동작과 상위 영역 이벤트 전파를 막고 수정 모드로 전환함
     * 배경 영역 안의 버튼이 다른 요소로 포커스되거나 클릭 이벤트가 겹치지 않도록 클릭 흐름을 고정함
     *
     * @author HanWon.Jang
     * @param event 프로필 수정 버튼 클릭 이벤트
     */
    const handleEditModeClick = (event: MouseEvent<HTMLButtonElement>) => {

      event.preventDefault();
      event.stopPropagation();
      // 편집 진입 시점의 최신 프로필 값을 input 상태에 다시 주입해 빈 값으로 열리는 경우를 막음
      setUserNick(profile?.userNick ?? "");
      setIntrCntn(profile?.intrCntn ?? "");
      setIsEditMode(true);
    };

    /**
     * 프로필 편집을 취소하고 서버 임시 이미지와 화면 입력값을 저장 전 상태로 되돌림
     *
     * @author SeungHyeon.Kang
     * @param event 편집 취소 버튼 클릭 이벤트
     * @return 임시 이미지 삭제 완료 Promise
     */
    const handleEditCancel = async (event: MouseEvent<HTMLButtonElement>): Promise<void> => {
      event.preventDefault();
      event.stopPropagation();

      try {
        const deleteRequests: Promise<unknown>[] = [];

        // 사용자가 선택한 프로필 임시 원본과 미리보기를 즉시 삭제함
        if (profileImageDraftToken) {
          deleteRequests.push(delProfileImageDraftApi("PROFILE"));
        }

        // 사용자가 선택한 배경 임시 원본과 미리보기를 즉시 삭제함
        if (backgroundImageDraftToken) {
          deleteRequests.push(delProfileImageDraftApi("BACKGROUND"));
        }

        await Promise.all(deleteRequests);
        // 기존 사용자 정보와 이미지 위치를 그대로 유지한 조회 상태로 복원함
        if (profile) {
          syncProfileState(profile);
        }
        setIsEditMode(false);
      } catch (error) {
        void sweetError(
          /* "수정에 실패했습니다." */ message("frontend.alert.updateFailedTitle"),
          getApiErrorMessage(error, /* "다시 시도해주세요." */ message("frontend.common.tryAgain")),
        );
      }
    };

    /**
     * 닉네임 필수값을 확인한 뒤 프로필 수정 API를 호출해 텍스트와 이미지 파일을 함께 저장함
     * 저장에 성공하면 서버가 반환한 최신 프로필 정보로 화면을 갱신하고 조회 모드로 되돌림
     *
     * @author HanWon.Jang
     * @param event 프로필 수정 폼 제출 이벤트
     */
    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {

      event.preventDefault();

      // 이미지 업로드가 끝나기 전에 Enter 또는 저장 버튼으로 같은 요청이 중복 제출되지 않게 차단함
      if (isSaving) {
        // 진행 중인 프로필 저장 요청만 유지하도록 제출 처리를 종료함
        return;
      }

      if (!userNick.trim()) {
        void sweetWarning(
          /* "입력이 필요합니다." */ message("frontend.alert.inputRequired"),
          /* "닉네임을 입력해주세요." */ message("frontend.profile.nickRequired"),
        );
        return;
      }

      if (
        userNick.trim().length > USER_NICK_MAX_LENGTH ||
        !USER_NICK_REGEX.test(userNick.trim())
      ) {
        void sweetWarning(
          /* "입력이 필요합니다." */ message("frontend.alert.inputRequired"),
          /* "닉네임은 공백 없이 한글, 영문, 숫자와 문자 사이의 단일 언더바 또는 하이픈을 사용해 25자 이하로 입력해주세요." */ message("frontend.profile.nickFormat"),
        );
        return;
      }

      /**
       * 현재 프로필 입력값과 선택 이미지를 사용자 수정 API에 전달함
       *
       * @author SeungHyeon.Kang
       * @return 프로필 저장과 화면 상태 반영 완료 Promise
       * @throws 프로필 저장 또는 응답 검증에 실패하면 발생함
       */
      const submitProfileChanges = async (): Promise<void> => {
        // 현재 입력값과 업로드 초안 토큰을 서버에 저장함
        const response = await updateMyProfileApi({
          userNick: userNick.trim(),
          intrCntn: intrCntn.trim(),
          profileImageDraftToken,
          backgroundImageDraftToken,
        });
        // 저장 응답을 프로필 화면과 전역 사용자 상태에 함께 반영함
        const nextProfile = response.data as UserProfile;
        syncProfileState(nextProfile);
        notifyUserProfileUpdated(nextProfile);
        // 저장된 프로필을 읽기 화면으로 전환함
        setIsEditMode(false);
      };

      try {
        setIsSaving(true);
        // 파일 업로드를 포함한 프로필 저장이 끝날 때까지 버튼 없는 모달과 화면 이동 차단을 유지함
        await runBlockingOperation(submitProfileChanges, {
          // "프로필 저장 중..."
          title: message("frontend.profile.saving"),
          success: {
            /* "프로필이 저장되었습니다." */ title: message("frontend.profile.savedTitle"),
            /* "수정한 프로필을 반영했습니다." */ text: message("frontend.profile.saved"),
          },
        });
      } catch (error) {
        void sweetError(
          /* "수정에 실패했습니다." */ message("frontend.alert.updateFailedTitle"),
          getApiErrorMessage(error, /* "다시 시도해주세요." */ message("frontend.common.tryAgain")),
        );
      } finally {
        setIsSaving(false);
      }
    };

    if (isLoading) {
      return <Loading/>;
    }

    return (
      /* 내 프로필과 독서 활동 전체 영역 */
      <main className={styles.page}>
        {/* 마이페이지 프로필과 독서 활동 전체 영역 */}
        <form className={styles.profileShell} onSubmit={handleSubmit}>
          {/* 프로필 배경 이미지 영역 */}
          <section className={styles.cover}>
            {coverDisplaySource && (
              <BackgroundImage
                source={coverDisplaySource}
                imageClassName={styles.coverImage}
                alt=""
              />
            )}
            {previewBackground && (
              <FullscreenImageButton
                key={hasRequestedImage && requestedTagtType === "BACKGROUND_IMAGE"
                  ? `notification-${requestedRouteKey}`
                  : "my-background-image"}
                className={styles.coverImageViewerButton}
                source={previewBackground}
                alt={/* "배경사진" */ message("frontend.imageViewer.backgroundAlt")}
                initiallyOpen={hasRequestedImage && requestedTagtType === "BACKGROUND_IMAGE"}
                actions={
                  !isEditMode && profile?.backgroundImageReaction
                    ? renderImageReactions(
                      profile.backgroundImageReaction,
                      styles.viewerImageReactionBar,
                    )
                    : undefined
                }
              >
                <span aria-hidden="true"/>
              </FullscreenImageButton>
            )}
            {!previewBackground && (
              <p className={styles.coverEmptyText}>
                {/* "배경사진을 선택해주세요." */ message("frontend.profile.background.empty")}
              </p>
            )}

            {/* 조회 상태의 프로필 수정 버튼을 배경사진 위 우측 하단에 고정함 */}
            {!isEditMode ? (
              <div className={styles.coverEditAction}>
                <button
                  className={styles.coverProfileEditButton}
                  type="button"
                  onClick={handleEditModeClick}
                >
                  <svg
                    className={styles.actionIcon}
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                    focusable="false"
                  >
                    <path
                      d="M4 20h4.7L19.4 9.3a2.1 2.1 0 0 0 0-3L17.7 4.6a2.1 2.1 0 0 0-3 0L4 15.3V20Zm2-2v-1.9L16.1 6l1.9 1.9L7.9 18H6Z"/>
                  </svg>
                  {/* "프로필 수정" */ message("frontend.profile.edit")}
                </button>
              </div>
            ) : null}

            {/* 편집 상태의 배경 변경과 저장 영역 */}
            {isEditMode ? (
              <div className={styles.coverActionGroup}>
                {/* 1. 취소 버튼 (배경 변경 왼쪽으로 이동) */}
                <button
                  className={styles.coverProfileEditButton}
                  type="button"
                  onClick={(event) => void handleEditCancel(event)}
                  disabled={isSaving}
                >
                  {/* "취소" */ message("frontend.common.cancel")}
                </button>

                {/* 2. 배경 변경 버튼 */}
                <label className={styles.coverImageButton}>
                  <svg
                    className={styles.actionIcon}
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                    focusable="false"
                  >
                    <path
                      d="M9 4 7.2 6H4a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-3.2L15 4H9Zm3 14a5 5 0 1 1 0-10 5 5 0 0 1 0 10Zm0-2a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z"/>
                  </svg>
                  {/* "배경 변경" */ message("frontend.profile.backgroundChange")}
                  <input
                    className={styles.hiddenInput}
                    type="file"
                    accept="image/jpeg,image/png"
                    onChange={handleBgImageChange}
                  />
                </label>

                {/* 3. 저장 버튼 */}
                <button
                  className={isSaving ? styles.coverSaveButtonSaving : styles.coverSaveButton}
                  type="submit"
                  aria-busy={isSaving}
                  aria-live="polite"
                  disabled={isSaving}
                >
                  {isSaving ? (
                    <>
                      <span className={styles.profileSaveSpinner} aria-hidden="true"/>
                      {/* "프로필 저장 중..." */}
                      {message("frontend.profile.saving")}
                    </>
                  ) : (
                    <>
                      <svg
                        className={styles.actionIcon}
                        viewBox="0 0 24 24"
                        aria-hidden="true"
                        focusable="false"
                      >
                        <path
                          d="M5 3h12.6L21 6.4V19a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Zm2 2v5h9V5H7Zm0 14h10v-6H7v6Z"/>
                      </svg>
                      {/* "저장" */ message("frontend.profile.save")}
                    </>
                  )}
                </button>
              </div>
            ) : null}
          </section>

          {/* 프로필 기본 정보 영역 */}
          <section className={styles.profileBody}>
            {/* 프로필 이미지와 사용자 소개 영역 */}
            <div className={styles.profileHeaderRow}>
              {/* 프로필 이미지와 이미지 변경 영역 */}
              <div className={styles.avatarWrap}>
                <FullscreenImageButton
                  key={hasRequestedImage && requestedTagtType === "PROFILE_IMAGE"
                    ? `notification-${requestedRouteKey}`
                    : "my-profile-image"}
                  className={styles.profileImageViewerButton}
                  source={normalizeProfileImageSource(previewImage)}
                  fallbackSource={DEFAULT_PROFILE_IMAGE}
                  alt={/* "프로필 사진" */ message("frontend.imageViewer.profileAlt")}
                  initiallyOpen={hasRequestedImage && requestedTagtType === "PROFILE_IMAGE"}
                  actions={
                    !isEditMode && profile?.profileImageReaction
                      ? renderImageReactions(
                        profile.profileImageReaction,
                        styles.viewerImageReactionBar,
                      )
                      : undefined
                  }
                >
                  <ProfileImage
                    className={styles.profileImage}
                    src={previewImage}
                    alt={profile?.userNick ?? /* "프로필 수정" */ message("frontend.profile.edit")}
                  />
                </FullscreenImageButton>
                {isEditMode && (
                  <label className={styles.avatarCameraButton}>
                    <svg
                      className={styles.cameraIcon}
                      viewBox="0 0 24 24"
                      aria-hidden="true"
                      focusable="false"
                    >
                      <path
                        d="M9 4 7.2 6H4a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-3.2L15 4H9Zm3 14a5 5 0 1 1 0-10 5 5 0 0 1 0 10Zm0-2a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z"/>
                    </svg>
                    <input
                      className={styles.hiddenInput}
                      type="file"
                      accept="image/jpeg,image/png"
                      onChange={handleProfileImageChange}
                    />
                  </label>
                )}
              </div>

              {/* 닉네임과 한줄소개 영역 */}
              <div className={styles.profileText}>
                {isEditMode ? (
                  <input
                    className={styles.profileNameInput}
                    value={userNick}
                    maxLength={USER_NICK_MAX_LENGTH}
                    aria-label={/* "닉네임" */ message("frontend.profile.nick")}
                    onChange={(event) =>
                      setUserNick(normalizeUserNick(event.currentTarget.value))
                    }
                  />
                ) : (
                  <h1 className={styles.profileName}>{profile?.userNick || "-"}</h1>
                )}

                {isEditMode ? (
                  <textarea
                    className={styles.profileIntroInput}
                    value={intrCntn}
                    maxLength={PROFILE_INTRO_MAX_LENGTH}
                    aria-label={/* "한줄 소개" */ message("frontend.profile.intro")}
                    onChange={(event) =>
                      setIntrCntn(normalizeProfileIntro(event.currentTarget.value))
                    }
                  />
                ) : (
                  <p className={styles.profileIntro}>
                    {profile?.intrCntn || /* "한줄 소개를 등록해보세요." */ message("frontend.profile.intro.empty")}
                  </p>
                )}
              </div>
            </div>
          </section>

          {/* 프로필 외 독서 활동 조회 결과 영역 */}
          {!isEditMode && monthlySummary ? (
            /* 조회가 완료된 독서 활동 페이드 인 영역 */
            <div className={styles.activityContent}>
              {/* 총 읽은 책과 팔로우 및 좋아요 통계 영역 */}
              {renderProfileStats(monthlySummary)}
              {/* 현재 읽고 있는 책 영역 */}
              {renderCurrentReports(monthlySummary.currentReadingReports)}
              {/* 목표 달성 횟수와 기간별 독서 목표 영역 */}
              <section className={styles.monthlySummary}
                       aria-label={/* "이번 달에 읽은 책" */ message("frontend.profile.monthlyReading.title")}>
                {/* 주간과 월간 및 연간 목표 달성 횟수 영역 */}
                <div className={styles.goalAchievementSummary}>
                  <p className={`${styles.goalAchievementTitle} ${styles.myPageSectionTitle}`}>
                    {/* "목표 달성 횟수" */ message("frontend.profile.goal.achievementTitle")}
                  </p>
                  {/* 전체와 주간 및 월간 및 연간 목표 달성 통계 영역 */}
                  <div className={styles.goalAchievementGrid}>
                    {/* 전체 목표 달성 횟수 영역 */}
                    <div className={styles.goalAchievementItem}>
                      <span className={styles.goalAchievementLabel}>
                        {/* "주간" */ message("frontend.profile.goal.weekLabel")}
                      </span>
                      <strong className={styles.goalAchievementCount}>
                        {/* "{0}회" */ message("frontend.profile.goal.achievementCount", [monthlySummary.weekGoalAchvCnt])}
                      </strong>
                    </div>
                    {/* 주간 목표 달성 횟수 영역 */}
                    <div className={styles.goalAchievementItem}>
                      <span className={styles.goalAchievementLabel}>
                        {/* "월간" */ message("frontend.profile.goal.monthLabel")}
                      </span>
                      <strong className={styles.goalAchievementCount}>
                        {/* "{0}회" */ message("frontend.profile.goal.achievementCount", [monthlySummary.monthGoalAchvCnt])}
                      </strong>
                    </div>
                    {/* 월간 목표 달성 횟수 영역 */}
                    <div className={styles.goalAchievementItem}>
                      <span className={styles.goalAchievementLabel}>
                        {/* "연간" */ message("frontend.profile.goal.yearLabel")}
                      </span>
                      <strong className={styles.goalAchievementCount}>
                        {/* "{0}회" */ message("frontend.profile.goal.achievementCount", [monthlySummary.yearGoalAchvCnt])}
                      </strong>
                    </div>
                    {/* 연간 목표 달성 횟수 영역 */}
                    <div className={styles.goalAchievementItem}>
                      <span className={styles.goalAchievementLabel}>
                        {/* "총" */ message("frontend.profile.goal.totalLabel")}
                      </span>
                      <strong className={styles.goalAchievementCount}>
                        {/* "{0}회" */ message("frontend.profile.goal.achievementCount", [monthlySummary.totalGoalAchvCnt])}
                      </strong>
                    </div>
                  </div>
                </div>
                {/* 주간 독서 목표와 달성 현황 영역 */}
                <div className={styles.readingSummaryDivider}/>
                {renderReadingSummaryRow(
                  "week",
                  monthlySummary.weekCode,
                  "frontend.profile.weeklyReading.title",
                  "frontend.common.bookCount",
                  monthlySummary.currentWeekCount,
                  monthlySummary.weekCountDiff,
                  "frontend.profile.weeklyReading.diffAria",
                  monthlySummary.currentWeekReports,
                )}
                {/* 월간 독서 목표와 달성 현황 영역 */}
                <div className={styles.readingSummaryDivider}/>
                {renderReadingSummaryRow(
                  "month",
                  monthlySummary.monthCode,
                  "frontend.profile.monthlyReading.title",
                  "frontend.common.bookCount",
                  monthlySummary.currentMonthCount,
                  monthlySummary.countDiff,
                  "frontend.profile.monthlyReading.diffAria",
                  monthlySummary.currentMonthReports,
                )}
                {/* 연간 독서 목표와 달성 현황 영역 */}
                <div className={styles.readingSummaryDivider}/>
                {renderReadingSummaryRow(
                  "year",
                  monthlySummary.yearCode,
                  "frontend.profile.yearlyReading.title",
                  "frontend.common.bookCount",
                  monthlySummary.currentYearCount,
                  monthlySummary.yearCountDiff,
                  "frontend.profile.yearlyReading.diffAria",
                  monthlySummary.currentYearReports,
                )}
              </section>
              {/* 독서 목표 설정과 수정 진입 영역 */}
              <button
                className={styles.goalSettingButton}
                type="button"
                onClick={handleGoalModalOpen}
              >
                {/* "목표 수정하기" 또는 "목표 설정하기" */}
                {message(
                  monthlySummary.weekGoalSet && monthlySummary.monthGoalSet && monthlySummary.yearGoalSet
                    ? "frontend.profile.goal.edit"
                    : "frontend.profile.goal.set",
                )}
                <svg width="14" height="14" viewBox="0 0 14 14" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M5.19751 11.62L9.00083 7.81668C9.44999 7.36752 9.44999 6.63252 9.00083 6.18335L5.19751 2.38"
                        stroke="#8a8a8a" strokeWidth="1.5" strokeMiterlimit="10" strokeLinecap="round"
                        strokeLinejoin="round"/>
                </svg>
              </button>
              {/* 스크롤 진입 시 조회하는 본인 전용 독서 통계 영역 */}
              <ReadingStatisticsSection/>
            </div>
          ) : null}
        </form>

        {/* 현재 프로필 또는 배경 사진의 범용 댓글 바텀시트 영역 */}
        {replyTarget ? (
          <ReplySheet
            report={{reptNumb: replyTarget.tagtNumb, userNick: profile?.userNick}}
            tagtType={replyTarget.tagtType}
            focusReplNumb={focusReplNumb}
            onClose={() => void handleImageReplyClose()}
          />
        ) : null}

        {/* 현재 읽는 책의 수정 화면으로 연결하는 모달 영역 */}
        {currentReadingReport && createPortal((
          /* 현재 읽는 책 수정 안내 모달 배경 영역 */
          <div
            className={`${styles.goalModalOverlay} ${
              closingModal === "currentReading" ? styles.goalModalOverlayClosing : ""
            }`}
            role="presentation"
            onMouseDown={(event) => {

              if (event.currentTarget === event.target) {
                void closeProfileModal("currentReading");
              }
            }}
          >
            {/* 현재 읽는 책 수정 안내 모달 본문 영역 */}
            <section
              className={`${styles.goalModal} ${
                closingModal === "currentReading" ? styles.goalModalClosing : ""
              }`}
              role="dialog"
              aria-modal="true"
              aria-labelledby="current-reading-title"
            >
              {/* 현재 읽는 책 수정 안내 제목과 닫기 영역 */}
              <div className={styles.goalModalHeader}>
                <h2 className={styles.goalModalTitle} id="current-reading-title">
                  {/* "다 읽으셨나요?" */ message("frontend.profile.currentReading.completionPrompt")}
                </h2>
                <button
                  className={modalControlStyles.roundClose}
                  type="button"
                  aria-label={/* "닫기" */ message("frontend.common.close")}
                  onClick={() => void closeProfileModal("currentReading")}
                >
                  ×
                </button>
              </div>

              {/* 수정 대상 도서 정보 영역 */}
              <div className={styles.currentReadingModalBody}>
                <div className={styles.currentReadingModalBookInfo}>
                  <img
                    className={styles.currentReadingModalCover}
                    src={getBookCoverImageSource(currentReadingReport.bookCvim)}
                    onError={handleBookCoverImageError}
                    alt=""
                  />
                  <div className={styles.currentReadingModalBookText}>
                    <p className={styles.currentReadingModalBookTitle}>
                      {currentReadingReport.bookTitl || /* "도서 정보가 없습니다." */ message("frontend.common.noBookInfo")}
                    </p>
                    {getReadingPeriodText(currentReadingReport) && (
                      <p className={styles.currentReadingModalBookMeta}>
                        {getReadingPeriodText(currentReadingReport)}
                      </p>
                    )}
                  </div>
                </div>
              </div>

              {/* 현재 읽는 책 안내 닫기와 수정 진입 영역 */}
              <div className={styles.goalModalActions}>
                <ActionButton
                  variant="secondary"
                  size="lg"
                  width="full"
                  onClick={() => void closeProfileModal("currentReading")}
                >
                  {/* "닫기" */ message("frontend.common.close")}
                </ActionButton>
                <ActionButton
                  variant="primary"
                  size="lg"
                  width="full"
                  onClick={handleReadingEditClick}
                >
                  <span>{/* "수정하기" */ message("frontend.common.update")}</span>
                </ActionButton>
              </div>
            </section>
          </div>
        ), document.body)}

        {/* 팔로잉과 팔로워 사용자 목록 모달 영역 */}
        {followListType && createPortal((
          /* 팔로우 사용자 목록 모달 배경 영역 */
          <div
            className={`${styles.goalModalOverlay} ${
              closingModal === "followList" ? styles.goalModalOverlayClosing : ""
            }`}
            role="presentation"
            onMouseDown={(event) => {

              if (event.currentTarget === event.target) {
                void closeProfileModal("followList");
              }
            }}
          >
            {/* 팔로우 사용자 목록 모달 본문 영역 */}
            <section
              className={`${styles.followModal} ${
                closingModal === "followList" ? styles.goalModalClosing : ""
              }`}
              role="dialog"
              aria-modal="true"
              aria-labelledby="follow-list-title"
            >
              {/* 팔로우 사용자 목록 제목과 닫기 영역 */}
              <div className={styles.goalModalHeader}>
                <h2 className={styles.goalModalTitle} id="follow-list-title">
                  {/* "팔로우" 또는 "팔로워" */}
                  {message(
                    followListType === "following"
                      ? "frontend.common.following"
                      : "frontend.common.followers",
                  )}
                </h2>
                <button
                  className={modalControlStyles.roundClose}
                  type="button"
                  aria-label={/* "닫기" */ message("frontend.common.close")}
                  onClick={() => void closeProfileModal("followList")}
                >
                  ×
                </button>
              </div>

              {/* 팔로우 사용자 목록과 조회 상태 영역 */}
              <div
                className={isFollowListScrolling ? styles.followModalListScrolling : styles.followModalList}
                onScroll={handleFollowListScroll}
              >
                {isFollowListLoading && (
                  /* 팔로우 사용자 목록을 불러오는 동안 모달 안에 소형 공통 회전 링을 표시함 */
                  <Loading
                    title={/* "목록 조회 중" */ message("frontend.common.loadingList")}
                    isFullScreen={false}
                    isCompact
                  />
                )}
                {!isFollowListLoading && followUsers.length === 0 && (
                  <p className={styles.followModalEmpty}>
                    {/* "팔로우한 사용자가 없습니다." 또는 "팔로워가 없습니다." */}
                    {message(
                      followListType === "following"
                        ? "frontend.profile.followingList.empty"
                        : "frontend.profile.followerList.empty",
                    )}
                  </p>
                )}
                {!isFollowListLoading && followUsers.map((user) => (
                  /* 팔로우 사용자 개별 항목 영역 */
                  <div className={styles.followModalItem} key={user.userNumb}>
                    {/* 팔로우 사용자 프로필 정보 영역 */}
                    <button
                      className={styles.followModalProfileButton}
                      type="button"
                      onClick={() => handleFollowListUserClick(user.userNumb)}
                    >
                      <ProfileImage
                        className={styles.followModalAvatar}
                        src={user.porfPath}
                        alt={user.userNick ?? /* "닉네임" */ message("frontend.profile.nick")}
                      />
                      <span className={styles.followModalText}>
                      <strong className={styles.followModalName}>
                        {user.userNick || "-"}
                      </strong>
                      <span className={styles.followModalIntro}>
                        {user.intrCntn || /* "한줄 소개를 등록해보세요." */ message("frontend.profile.intro.empty")}
                      </span>
                    </span>
                    </button>
                    {/* 팔로우 상태 확인과 변경 영역 */}
                    {user.meYsno !== "Y" && (
                      <button
                        className={userListStyles.statusButton}
                        data-follow-status={user.followStatName}
                        type="button"
                        disabled={followUpdatingUserNumb === user.userNumb}
                        onClick={() => void handleFollowStatusClick(user)}
                      >
                        {user.followStatName}
                      </button>
                    )}
                  </div>
                ))}
                <InfiniteScrollTrigger
                  hasNext={!isFollowListLoading && hasNextFollowUser}
                  isLoading={isNextFollowLoading}
                  onLoadMore={() => {
                    // 목록 하단에 도달하면 다음 팔로우 사용자 서버 페이지를 조회함
                    void loadMoreFollow();
                  }}
                />
              </div>
            </section>
          </div>
        ), document.body)}

        {/* 주간과 월간 및 연간 독서 목표 설정 모달 영역 */}
        {isGoalModalOpen && createPortal((
            /* 독서 목표 설정 모달 배경 영역 */
            <div
              className={`${styles.goalModalOverlay} ${
                closingModal === "goal" ? styles.goalModalOverlayClosing : ""
              }`}
              role="presentation"
              onMouseDown={(event) => {

                if (event.currentTarget === event.target) {
                  void closeProfileModal("goal");
                }
              }}
            >
              {/* 독서 목표 설정 모달 본문 영역 */}
              <section
                className={`${styles.goalModal} ${styles.goalSettingsModal} ${
                  closingModal === "goal" ? styles.goalModalClosing : ""
                }`}
                role="dialog"
                aria-modal="true"
                aria-labelledby="reading-goal-title"
              >
                {/* 독서 목표 설정 제목과 도움말 및 닫기 영역 */}
                <div className={styles.goalModalHeader}>
                  <h2 className={styles.goalModalTitle} id="reading-goal-title">
                    {/* "독서 목표 설정" */ message("frontend.profile.goal.modalTitle")}
                  </h2>
                  <div className={styles.goalModalHeaderActions}>
                    <button
                      className={styles.goalHelpButton}
                      type="button"
                      onClick={() => {

                        setClosingModal(null);
                        setIsGoalHelpModalOpen(true);
                      }}
                    >
                      {/* "도움말" */ message("frontend.profile.goal.helpButton")}
                    </button>
                    <button
                      className={modalControlStyles.roundClose}
                      type="button"
                      aria-label={/* "닫기" */ message("frontend.common.close")}
                      onClick={() => void closeProfileModal("goal")}
                    >
                      <img src="/img/icons/icon-close.svg" alt="" width="12" height="12"/>
                    </button>
                  </div>
                </div>
                {/* 기간별 목표 권수 입력 영역 */}
                <div className={styles.goalModalBody}>
                  {/* 주간 목표 권수 입력과 수정 제한 안내 영역 */}
                  <div className={styles.goalInputLabel}>
                    {renderGoalLimitInfo("week")}
                    <StepperButton
                      minusAriaLabel={`${/* "주간" */ message("frontend.profile.goal.weekLabel")} 감소`}
                      plusAriaLabel={`${/* "주간" */ message("frontend.profile.goal.weekLabel")} 증가`}
                      minusOnClick={() => handleGoalCountStep("week", -1)}
                      plusOnClick={() => handleGoalCountStep("week", 1)}
                      inputId="week-goal-count"
                      inputValue={weekGoalCnt}
                      inputPlaceholder={/* "목표 권수" */ message("frontend.profile.goal.placeholder")}
                      inputOnChange={(event) => setWeekGoalCnt(normalizeGoalCount(event.currentTarget.value))}
                    />
                  </div>
                  {/* 월간 목표 권수 입력과 수정 제한 안내 영역 */}
                  <div className={styles.goalInputLabel}>
                    {renderGoalLimitInfo("month")}
                    <StepperButton
                      minusAriaLabel={`${/* "월간" */ message("frontend.profile.goal.monthLabel")} 감소`}
                      plusAriaLabel={`${/* "월간" */ message("frontend.profile.goal.monthLabel")} 증가`}
                      minusOnClick={() => handleGoalCountStep("month", -1)}
                      plusOnClick={() => handleGoalCountStep("month", 1)}
                      inputId="month-goal-count"
                      inputValue={monthGoalCnt}
                      inputPlaceholder={/* "목표 권수" */ message("frontend.profile.goal.placeholder")}
                      inputOnChange={(event) => setMonthGoalCnt(normalizeGoalCount(event.currentTarget.value))}
                    />
                  </div>
                  {/* 연간 목표 권수 입력과 수정 제한 안내 영역 */}
                  <div className={styles.goalInputLabel}>
                    {renderGoalLimitInfo("year")}
                    <StepperButton
                      minusAriaLabel={`${/* "연간" */ message("frontend.profile.goal.yearLabel")} 감소`}
                      plusAriaLabel={`${/* "연간" */ message("frontend.profile.goal.yearLabel")} 증가`}
                      minusOnClick={() => handleGoalCountStep("year", -1)}
                      plusOnClick={() => handleGoalCountStep("year", 1)}
                      inputId="year-goal-count"
                      inputValue={yearGoalCnt}
                      inputPlaceholder={/* "목표 권수" */ message("frontend.profile.goal.placeholder")}
                      inputOnChange={(event) => setYearGoalCnt(normalizeGoalCount(event.currentTarget.value))}
                    />
                  </div>
                </div>
                  {/* 독서 목표 설정 취소와 저장 영역 */
                  }
                  <div className={styles.goalSettingsActions}>
                    <ActionButton
                      variant="secondary"
                      size="lg"
                      width="full"
                      onClick={() => void closeProfileModal("goal")}
                    >
                      {/* "취소" */ message("frontend.common.cancel")}
                    </ActionButton>
                    <ActionButton
                      variant="primary"
                      size="lg"
                      width="full"
                      disabled={isGoalSaving}
                      onClick={handleGoalSubmit}
                    >
                      {/* "저장하기" */ message("frontend.common.save")}
                    </ActionButton>
                  </div>
              </section>
            </div>
          ),
          document.body
        )}

        {/* 독서 목표 설정 기준 도움말 모달 영역 */}
        {
          isGoalHelpModalOpen && createPortal((
            /* 독서 목표 도움말 모달 배경 영역 */
            <div
              className={`${styles.goalModalOverlay} ${
                closingModal === "goalHelp" ? styles.goalModalOverlayClosing : ""
              }`}
              role="presentation"
              onMouseDown={(event) => {

                if (event.currentTarget === event.target) {
                  void closeProfileModal("goalHelp");
                }
              }}
            >
              {/* 독서 목표 도움말 모달 본문 영역 */}
              <section
                className={`${styles.goalHelpModal} ${
                  closingModal === "goalHelp" ? styles.goalModalClosing : ""
                }`}
                role="dialog"
                aria-modal="true"
                aria-labelledby="reading-goal-help-title"
              >
                {/* 독서 목표 도움말 제목과 닫기 영역 */}
                <div className={styles.goalModalHeader}>
                  <h2 className={styles.goalModalTitle} id="reading-goal-help-title">
                    {/* "목표 내리기" */ message("frontend.profile.goal.helpTitle")}
                  </h2>
                  <button
                    className={modalControlStyles.roundClose}
                    type="button"
                    aria-label={/* "닫기" */ message("frontend.common.close")}
                    onClick={() => void closeProfileModal("goalHelp")}
                  >
                    ×
                  </button>
                </div>
                {/* 독서 목표 기간별 수정 기준 안내 영역 */}
                <div className={styles.goalHelpBody}>
                  <p className={styles.goalHelpLead}>
                    {/* "목표를 올리는 것은 언제나 가능하고, 목표를 내릴 때만 기간별 횟수와 가능 기간이 제한됩니다." */ message("frontend.profile.goal.helpLead")}
                  </p>
                  <ul className={styles.goalHelpList}>
                    {/* 목표 내리기 정책 문구 목록 */}
                    {goalHelpItems.map(renderGoalHelpItem)}
                  </ul>
                </div>
              </section>
            </div>
          ), document.body)
        }
      </main>
    );
  }
;

export default ProfileEditPage;
