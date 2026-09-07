import { getApiErrorMessage } from "@/app/api/resultData";
import { sweetError } from "@/app/lib/sweetAlert/sweetAlert";
import { message } from "@/app/messages/message";
import { runBlockingOperation } from "@/app/navigation/blockingOperation";
import { useBodyScrollLock } from "@/app/utils/modalUtil";
import CustomSelect, { type CustomSelectOption } from "@/components/Select/CustomSelect";
import {
  getBookCoverImageSource,
  handleBookCoverImageError,
} from "@/features/Book/utils/bookCoverImage";
import { getSocialReadingStatsApi } from "@/features/Social/api/socialApi";
import {
  getReadingStatsApi,
  uptReadingStatsSettingApi,
  type ReadingBookTime,
  type ReadingHeatmap,
  type ReadingRatingCount,
  type ReadingStatistics,
  type ReadingStatusCount,
  type ReadingTimeDaily,
} from "@/features/User/api/userApi";
import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type MouseEvent as ReactMouseEvent,
  type ReactNode,
} from "react";
import { createPortal } from "react-dom";
import { useNavigate } from "react-router-dom";
import * as styles from "./ReadingStatisticsSection.css";

const HEATMAP_COLORS = ["#f1f4f2", "#d9eee0", "#b9dfc7", "#78b991", "#34704d"] as const;
const MONTH_LABELS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"] as const;
const STATUS_COLORS: Record<ReadingStatusCount["reptStat"], string> = {
  READ: "#F7D98B",
  DONE: "#9EDFC2",
  STOP: "#F4A7AD",
};
const STATUS_DISPLAY_ORDER: Record<ReadingStatusCount["reptStat"], number> = {
  DONE: 0,
  READ: 1,
  STOP: 2,
};
const SCROLLBAR_HIDE_DELAY_MS = 650;
const VISIBILITY_OPTIONS = ["Y", "N"] as const;

type ReadingStatisticsSectionProps = {
  targetUserNumb?: number;
};

type ReadingHeatmapChartProps = {
  heatmap: ReadingHeatmap;
  onYearChange: (readYearValue: string) => void;
  titleClassName?: string;
};

type HeatmapMonthMarker = {
  monthKey: string;
  label: string;
  column: number;
};

type HorizontalScrollAreaProps = {
  children: ReactNode;
  hint: string;
};

type ReadingComparisonRow = {
  key: "readTime" | "readDays" | "doneBooks";
  label: string;
  currentValue: string;
  previousValue: string;
  difference: number;
  differenceLabel: string;
};

/**
 * 조회 가능한 독서 연도를 홈 화면 SelectBox가 사용하는 문자열 옵션으로 변환함
 *
 * @author SeungHyeon.Kang
 * @param availableYears 서버가 제공한 조회 가능 연도 목록
 * @return 연도 선택 SelectBox 옵션 목록
 */
const getYearSelectOptions = (availableYears: number[]): Array<CustomSelectOption<string>> => {
  const yearOptions: Array<CustomSelectOption<string>> = [];

  // 서버가 제공한 최근 연도 순서를 유지하며 SelectBox 옵션을 구성함
  for (const readYear of availableYears) {
    // 연도 값을 화면 라벨과 선택값에 같은 문자열로 추가함
    yearOptions.push({ value: String(readYear), label: String(readYear) });
  }

  // 홈 화면 SelectBox에 전달할 연도 옵션 목록을 반환함
  return yearOptions;
};

/**
 * 독서 상태를 완독, 읽는 중, 중단 순서로 정렬함
 *
 * @author SeungHyeon.Kang
 * @param leftStatus 앞쪽 정렬 후보 독서 상태
 * @param rightStatus 뒤쪽 정렬 후보 독서 상태
 * @return 두 상태의 표시 순서 차이
 */
const compareStatusDisplayOrder = (leftStatus: ReadingStatusCount, rightStatus: ReadingStatusCount): number => {
  // 완독이 읽는 중보다 먼저 표시되는 고정 순서 차이를 반환함
  return STATUS_DISPLAY_ORDER[leftStatus.reptStat] - STATUS_DISPLAY_ORDER[rightStatus.reptStat];
};

/**
 * 실제 가로 오버플로가 있는 동안 안내를 표시하고 최초 스크롤 뒤 천천히 숨김
 *
 * @author SeungHyeon.Kang
 * @param children 가로로 스크롤할 잔디
 * @param hint 최초 스크롤 전 표시할 안내 문구
 * @return 가로 스크롤 감지 안내가 포함된 그래프 영역
 */
function HorizontalScrollArea({ children, hint }: HorizontalScrollAreaProps) {

  // 실제 콘텐츠 폭과 표시 폭을 비교할 스크롤 요소 참조를 생성함
  const scrollRef = useRef<HTMLDivElement | null>(null);
  // 한 번 스크롤한 뒤 안내가 다시 나타나지 않도록 감지 상태 참조를 생성함
  const isDismissedRef = useRef(false);
  // 최초 렌더링에서 가장 최근 데이터가 있는 우측 끝으로 한 번만 이동할 상태 참조를 생성함
  const isPositionedRef = useRef(false);
  // 초기 우측 이동과 사용자의 실제 스크롤 조작을 구분할 상태 참조를 생성함
  const hasUserIntentRef = useRef(false);
  // 사용자가 가로 스크롤을 멈춘 뒤 표시를 숨길 타이머 참조를 생성함
  const scrollbarTimerRef = useRef<number | null>(null);
  // 가로 오버플로가 있을 때만 안내를 노출할 화면 상태를 생성함
  const [isHintVisible, setIsHintVisible] = useState(false);
  // 첫 스크롤 뒤 안내 문구의 지연 페이드아웃을 적용할 화면 상태를 생성함
  const [isHintDismissed, setIsHintDismissed] = useState(false);
  // 초기에는 숨긴 스크롤바를 실제 사용자 스크롤 뒤 표시할 화면 상태를 생성함
  const [isScrollbarVisible, setIsScrollbarVisible] = useState(false);

  /**
   * 현재 화면 너비에서 콘텐츠가 가로로 넘치는지 측정해 안내 표시 여부를 결정함
   *
   * @author SeungHyeon.Kang
   * @return 반환값이 없음
   */
  const measureOverflow = useCallback((): void => {
    // 스크롤 요소가 아직 준비되지 않으면 폭 측정을 다음 렌더링으로 미룸
    if (!scrollRef.current) {
      return;
    }

    // 한 픽셀 이하의 렌더링 오차를 제외한 실제 가로 오버플로 폭을 계산함
    const overflowWidth = scrollRef.current.scrollWidth - scrollRef.current.clientWidth;

    // 최초 표시에서는 가장 최근 날짜가 보이도록 스크롤을 우측 끝에 배치함
    if (!isPositionedRef.current && overflowWidth > 1) {
      // 초기 위치 설정으로 발생한 스크롤은 사용자 조작으로 처리하지 않음
      scrollRef.current.scrollLeft = overflowWidth;
      // 화면 크기 변경으로 초기 위치가 다시 덮어쓰이지 않도록 완료 상태를 기록함
      isPositionedRef.current = true;
    }

    // 사용자가 아직 스크롤하지 않은 실제 오버플로 영역에만 안내를 표시함
    if (!isDismissedRef.current) {
      // 스크롤 가능한 폭이 있을 때만 반투명 안내를 화면에 반영함
      setIsHintVisible(overflowWidth > 1);
    }
  }, []);

  /**
   * 최초 렌더링과 화면 너비 변경 시 가로 오버플로를 다시 측정함
   *
   * @author SeungHyeon.Kang
   * @return 화면 크기 감지 해제 함수
   */
  const prepareOverflowMeasure = useCallback((): (() => void) => {
    // 잔디가 렌더링된 직후 실제 가로 오버플로를 측정함
    measureOverflow();
    // 반응형 화면 너비가 바뀌면 스크롤 필요 여부를 다시 계산함
    window.addEventListener("resize", measureOverflow);

    /**
     * 그래프 영역이 해제될 때 화면 크기 감지를 함께 정리함
     *
     * @author SeungHyeon.Kang
     * @return 반환값이 없음
     */
    const removeResizeListener = (): void => {
      // 더 이상 사용하지 않는 화면 크기 감지 함수를 해제함
      window.removeEventListener("resize", measureOverflow);
    };

    // Effect 정리 단계에서 실행할 화면 크기 감지 해제 함수를 반환함
    return removeResizeListener;
  }, [measureOverflow]);

  // 그래프 표시 폭과 콘텐츠 폭을 최초 및 화면 크기 변경 시 비교함
  useEffect(prepareOverflowMeasure, [prepareOverflowMeasure]);

  /**
   * 대기 중인 스크롤바 숨김 타이머를 해제함
   *
   * @author SeungHyeon.Kang
   * @return 반환값이 없음
   */
  const clearScrollbarTimer = useCallback((): void => {
    // 활성 타이머가 있을 때만 해제해 다음 스크롤의 숨김 시점을 다시 계산함
    if (scrollbarTimerRef.current !== null) {
      window.clearTimeout(scrollbarTimerRef.current);
      // 해제된 타이머가 다시 참조되지 않도록 빈 상태를 기록함
      scrollbarTimerRef.current = null;
    }
  }, []);

  /**
   * 가로 스크롤바를 숨김 상태로 전환함
   *
   * @author SeungHyeon.Kang
   * @return 반환값이 없음
   */
  const hideScrollbar = useCallback((): void => {
    // 스크롤 조작이 멈춘 뒤 스크롤바가 천천히 투명해지도록 표시 상태를 해제함
    setIsScrollbarVisible(false);
    // 완료된 타이머를 빈 상태로 기록함
    scrollbarTimerRef.current = null;
  }, []);

  /**
   * 잔디 영역이 해제될 때 남아 있는 스크롤바 타이머를 정리함
   *
   * @author SeungHyeon.Kang
   * @return 스크롤바 숨김 타이머 정리 함수
   */
  const prepareScrollCleanup = useCallback((): (() => void) => {
    // Effect 정리 단계에서 같은 타이머 해제 함수를 실행하도록 반환함
    return clearScrollbarTimer;
  }, [clearScrollbarTimer]);

  // 화면 이탈 후 스크롤바 상태를 변경하지 않도록 숨김 타이머를 정리함
  useEffect(prepareScrollCleanup, [prepareScrollCleanup]);

  /**
   * 포인터와 휠 및 키보드 입력을 사용자의 실제 스크롤 의도로 기록함
   *
   * @author SeungHyeon.Kang
   * @return 반환값이 없음
   */
  const handleScrollIntent = (): void => {
    // 초기 우측 위치 설정과 구분할 수 있도록 사용자 조작 상태를 기록함
    hasUserIntentRef.current = true;
  };

  /**
   * 사용자가 가로 스크롤을 시작하면 안내를 지연 페이드아웃하고 스크롤바를 표시함
   *
   * @author SeungHyeon.Kang
   * @return 반환값이 없음
   */
  const handleHorizontalScroll = (): void => {
    // 초기 우측 위치 설정은 사용자 조작으로 처리하지 않음
    if (!hasUserIntentRef.current) {
      return;
    }

    // 최초 사용자 스크롤에서만 안내 문구의 페이드아웃을 시작함
    if (!isDismissedRef.current) {
      // 현재 잔디에서 안내 애니메이션이 반복되지 않도록 감지 상태를 기록함
      isDismissedRef.current = true;
      // 사용자가 안내를 읽을 수 있도록 첫 이동 뒤 지연 페이드아웃 상태를 적용함
      setIsHintDismissed(true);
    }

    // 연속 스크롤 중에는 이전 숨김 예약을 취소해 표시를 유지함
    clearScrollbarTimer();
    // 실제 스크롤이 진행되는 동안 현재 위치를 확인할 스크롤바를 표시함
    setIsScrollbarVisible(true);
    // 마지막 스크롤 입력 이후 스크롤바가 천천히 사라지기 시작할 시점을 예약함
    scrollbarTimerRef.current = window.setTimeout(hideScrollbar, SCROLLBAR_HIDE_DELAY_MS);
  };

  // 안내가 사라지는 동안에도 같은 요소를 유지해 지연 페이드아웃을 적용함
  let scrollHintClassName = styles.scrollHint;

  // 최초 사용자 스크롤이 감지되면 안내에 종료 애니메이션 스타일을 추가함
  if (isHintDismissed) {
    scrollHintClassName += ` ${styles.scrollHintDismissed}`;
  }

  // 사용자 조작 전에는 브라우저 기본 스크롤바 색상을 투명하게 유지함
  let horizontalScrollClassName = styles.horizontalScroll;

  // 실제 사용자 스크롤이 감지된 뒤에는 현재 위치를 확인할 스크롤바 스타일을 추가함
  if (isScrollbarVisible) {
    horizontalScrollClassName += ` ${styles.horizontalScrollActive}`;
  }

  // 실제 오버플로 안내와 가로 스크롤 콘텐츠를 함께 반환함
  return (
    /* 잔디 또는 월별 그래프의 가로 스크롤 안내와 콘텐츠 영역 */
    <div className={styles.scrollArea}>
      {/* 최초 가로 스크롤 전 반투명 스크롤 안내 영역 */}
      {isHintVisible && (
        <span className={scrollHintClassName}>{hint}</span>
      )}
      {/* 좌우 이동이 가능한 잔디 영역 */}
      <div
        className={horizontalScrollClassName}
        ref={scrollRef}
        tabIndex={0}
        aria-label={hint}
        onPointerDown={handleScrollIntent}
        onTouchStart={handleScrollIntent}
        onWheel={handleScrollIntent}
        onKeyDown={handleScrollIntent}
        onScroll={handleHorizontalScroll}
      >
        {children}
      </div>
    </div>
  );
}

/**
 * 초 단위 독서 시간을 그래프 도움말에 표시할 시간과 분 문자열로 변환함
 *
 * @author SeungHyeon.Kang
 * @param readSecs 타이머로 확정된 독서 시간 초
 * @return 시간과 분 단위 독서 시간 문구
 */
const formatReadingTime = (readSecs: number): string => {
  const safeSeconds = Math.max(0, readSecs);
  const totalMinutes = Math.floor(safeSeconds / 60);
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;

  // 한 시간 이상은 시간과 분을 함께 표시함
  if (hours > 0) {
    // "{0}시간 {1}분"
    return message("frontend.profile.readingStats.hoursMinutes", [hours, minutes]);
  }

  // "{0}분"
  return message("frontend.profile.readingStats.minutes", [totalMinutes]);
};

/**
 * 전년도와 달라진 통계 값을 증가와 감소 또는 동일 문구로 변환함
 *
 * @author SeungHyeon.Kang
 * @param difference 현재 연도 값에서 이전 연도 값을 뺀 차이
 * @param formattedAbsolute 차이의 절대값에 단위를 포함한 문구
 * @return 증감 방향이 포함된 비교 문구
 */
const getDifferenceLabel = (difference: number, formattedAbsolute: string): string => {
  // 차이가 없으면 별도의 양수 또는 음수 기호 없이 동일 상태를 반환함
  if (difference === 0) {
    // "변화 없음"
    return message("frontend.profile.readingStats.comparisonSame");
  }

  // 양수 차이는 현재 연도에 증가한 값으로 표시함
  if (difference > 0) {
    // "+{0}"
    return message("frontend.profile.readingStats.comparisonIncrease", [formattedAbsolute]);
  }

  // "-{0}"
  return message("frontend.profile.readingStats.comparisonDecrease", [formattedAbsolute]);
};

/**
 * 현재 연도와 이전 연도의 독서 기록을 화면 비교표의 세 행으로 변환함
 *
 * @author SeungHyeon.Kang
 * @param statistics 서버에서 조회한 독서 통계
 * @return 독서 시간과 독서일 및 완독 권수 비교 행 목록
 */
const getComparisonRows = (statistics: ReadingStatistics): ReadingComparisonRow[] => {
  const comparison = statistics.yearComparison;
  const readTimeDifference = comparison.currentReadSecs - comparison.previousReadSecs;
  const readDaysDifference = comparison.currentReadDays - comparison.previousReadDays;
  const doneBooksDifference = comparison.currentDoneBooks - comparison.previousDoneBooks;

  // 현재 및 이전 연도 값과 증감값을 같은 단위로 표시하는 비교 행을 반환함
  return [
    {
      key: "readTime",
      label: message("frontend.profile.readingStats.comparisonReadTime"),
      currentValue: formatReadingTime(comparison.currentReadSecs),
      previousValue: formatReadingTime(comparison.previousReadSecs),
      difference: readTimeDifference,
      differenceLabel: getDifferenceLabel(readTimeDifference, formatReadingTime(Math.abs(readTimeDifference))),
    },
    {
      key: "readDays",
      label: message("frontend.profile.readingStats.comparisonReadDays"),
      currentValue: message("frontend.profile.readingStats.days", [comparison.currentReadDays]),
      previousValue: message("frontend.profile.readingStats.days", [comparison.previousReadDays]),
      difference: readDaysDifference,
      differenceLabel: getDifferenceLabel(
        readDaysDifference,
        message("frontend.profile.readingStats.days", [Math.abs(readDaysDifference)]),
      ),
    },
    {
      key: "doneBooks",
      label: message("frontend.profile.readingStats.comparisonDoneBooks"),
      currentValue: /* "{0}권" */ message("frontend.common.bookCount", [comparison.currentDoneBooks]),
      previousValue: /* "{0}권" */ message("frontend.common.bookCount", [comparison.previousDoneBooks]),
      difference: doneBooksDifference,
      differenceLabel: getDifferenceLabel(
        doneBooksDifference,
        /* "{0}권" */ message("frontend.common.bookCount", [Math.abs(doneBooksDifference)]),
      ),
    },
  ];
};

/**
 * 별점 분포 세로 막대의 상대 높이를 계산할 최대 독후감 수를 조회함
 *
 * @author SeungHyeon.Kang
 * @param ratingList 정수 별점별 독후감 수 목록
 * @return 별점 구간 중 가장 많은 독후감 수
 */
const getRatingMaxCount = (ratingList: ReadingRatingCount[]): number => {
  let ratingMaxCount = 0;

  // 여섯 별점 구간을 순회하며 현재까지의 최대 독후감 수를 갱신함
  for (const rating of ratingList) {
    // 음수 비정상 값은 0권으로 취급하고 기존 최대값과 비교함
    ratingMaxCount = Math.max(ratingMaxCount, Math.max(0, rating.reptCnt));
  }

  // 막대 높이 비율의 분모로 사용할 최대 독후감 수를 반환함
  return ratingMaxCount;
};

/**
 * 별점 세로 막대그래프의 권수 축에 표시할 상단과 중간 및 0권 눈금을 구성함
 *
 * @author SeungHyeon.Kang
 * @param ratingList 정수 별점별 독후감 수 목록
 * @return 큰 값부터 0까지 배치할 권수 축 눈금 목록
 */
const getRatingAxisTicks = (ratingList: ReadingRatingCount[]): number[] => {
  // 모든 별점이 0권이어도 축 높이를 유지하도록 상단 기준을 최소 1권으로 계산함
  const ratingAxisMax = Math.max(1, getRatingMaxCount(ratingList));
  // 정수 권수 눈금이 상단과 중복되지 않도록 중간 눈금을 계산함
  const middleTick = Math.ceil(ratingAxisMax / 2);

  // 상단 기준이 1권이면 중복되는 중간 눈금을 제외한 두 눈금을 반환함
  if (middleTick === ratingAxisMax) {
    return [ratingAxisMax, 0];
  }

  // 권수 축의 상단과 중간 및 기준선 눈금을 반환함
  return [ratingAxisMax, middleTick, 0];
};

/**
 * 하루 독서 시간을 잔디의 다섯 단계 색상 수준으로 구분함
 *
 * @author SeungHyeon.Kang
 * @param readSecs 해당 날짜에 확정된 독서 시간 초
 * @return 0부터 4까지의 잔디 색상 수준
 */
const getHeatmapLevel = (readSecs: number): number => {
  // 10분 미만은 기록이 없는 날을 포함해 첫 번째 잔디 색상을 사용함
  if (readSecs < 600) {
    return 0;
  }

  // 30분 미만은 두 번째 잔디 색상을 사용함
  if (readSecs < 1800) {
    return 1;
  }

  // 한 시간 미만은 세 번째 잔디 색상을 사용함
  if (readSecs < 3600) {
    return 2;
  }

  // 네 시간 미만은 네 번째 잔디 색상을 사용함
  if (readSecs < 14400) {
    return 3;
  }

  // 네 시간 이상은 가장 진한 다섯 번째 잔디 색상을 사용함
  return 4;
};

/**
 * 잔디의 첫 날짜가 속한 요일 앞에 채울 빈 칸 수를 계산함
 *
 * @author SeungHyeon.Kang
 * @param firstDate 선택 기간 잔디의 첫 날짜
 * @return 일요일 시작 열을 맞추기 위한 빈 칸 수
 */
const getHeatmapOffset = (firstDate?: string): number => {
  // 첫 날짜가 없으면 별도 정렬 칸을 만들지 않음
  if (!firstDate) {
    return 0;
  }

  // 날짜 문자열을 로컬 자정으로 해석해 일요일부터 토요일까지의 행 위치를 계산함
  return new Date(`${firstDate}T00:00:00`).getDay();
};

/**
 * 날짜별 잔디 목록에서 각 월의 첫 표시 열과 영문 세 글자 월 라벨을 계산함
 *
 * @author SeungHyeon.Kang
 * @param dailyList 날짜별 독서 시간 목록
 * @param heatmapOffset 첫 주의 요일 앞 빈 칸 수
 * @return 잔디 열에 맞춘 월 라벨 목록
 */
const getHeatmapMonths = (dailyList: ReadingTimeDaily[], heatmapOffset: number): HeatmapMonthMarker[] => {
  const monthMarkers: HeatmapMonthMarker[] = [];
  let previousMonth = "";

  // 선택 기간에 포함된 월이 처음 나타나는 날짜마다 월 라벨을 만듦
  for (let index = 0; index < dailyList.length; index += 1) {
    const monthKey = dailyList[index].readDate.slice(0, 7);

    // 같은 월의 두 번째 날짜부터는 기존 월 라벨을 재사용함
    if (monthKey === previousMonth) {
      continue;
    }

    previousMonth = monthKey;
    const monthIndex = Math.max(0, Math.min(11, Number(monthKey.slice(5)) - 1));
    // GitHub 잔디처럼 월 시작 위치에 영문 세 글자 라벨을 추가함
    monthMarkers.push({
      monthKey,
      label: MONTH_LABELS[monthIndex],
      column: Math.floor((heatmapOffset + index) / 7) + 1,
    });
  }

  // 날짜 열과 대응하는 월 라벨 목록을 반환함
  return monthMarkers;
};

/**
 * 마이페이지와 타이머 화면이 함께 사용하는 연도별 독서 시간 잔디를 표시함
 *
 * @author SeungHyeon.Kang
 * @param heatmap 조회 가능한 연도와 날짜별 독서 시간
 * @param onYearChange 잔디 조회 연도 변경 함수
 * @param titleClassName 화면별 독서 잔디 제목 스타일
 * @return 연도 선택과 월별 독서 시간 잔디 및 강도 범례
 */
export function ReadingHeatmapChart({ heatmap, onYearChange, titleClassName }: ReadingHeatmapChartProps) {
  const heatmapOffset = getHeatmapOffset(heatmap.heatmapList[0]?.readDate);
  const heatmapMonths = getHeatmapMonths(heatmap.heatmapList, heatmapOffset);
  const heatmapColumns = Math.ceil((heatmapOffset + heatmap.heatmapList.length) / 7);
  // 홈 화면과 같은 SelectBox에 전달할 연도 옵션을 구성함
  const yearOptions = getYearSelectOptions(heatmap.availableYears);
  // "좌우로 스크롤해 확인할 수 있어요"
  const scrollHint = message("frontend.profile.readingStats.scrollHint");

  /**
   * 잔디 시작 요일을 맞추기 위한 빈 셀을 렌더링함
   *
   * @author SeungHyeon.Kang
   * @param unusedValue 배열이 제공하는 사용하지 않는 빈 값
   * @param index 빈 셀의 안정적인 순번
   * @return 잔디 시작 위치를 맞추는 빈 셀
   */
  const renderHeatmapSpacer = (unusedValue: undefined, index: number) => {
    // 시작 요일 정렬을 위한 빈 잔디 셀을 반환함
    return <span className={styles.heatmapSpacer} key={`spacer-${index}`} aria-hidden="true" />;
  };

  /**
   * 날짜별 독서 시간을 강도 색상과 도움말을 가진 잔디 한 칸으로 렌더링함
   *
   * @author SeungHyeon.Kang
   * @param daily 날짜별 확정 독서 시간
   * @return 독서 시간 잔디 한 칸
   */
  const renderHeatmapDay = (daily: ReadingTimeDaily) => {
    // "{0} · {1}"
    const dayLabel = message("frontend.profile.readingStats.dayTooltip", [daily.readDate, formatReadingTime(daily.readSecs)]);

    // 날짜별 독서 강도를 색으로 구분한 잔디 셀을 반환함
    return (
      /* 날짜별 독서 시간 잔디 개별 항목 영역 */
      <span
        className={styles.heatmapCell}
        key={daily.readDate}
        role="img"
        tabIndex={0}
        aria-label={dayLabel}
        title={dayLabel}
        style={{ backgroundColor: HEATMAP_COLORS[getHeatmapLevel(daily.readSecs)] }}
      />
    );
  };

  /**
   * 잔디 열 위에 해당 월의 영문 세 글자 라벨을 렌더링함
   *
   * @author SeungHyeon.Kang
   * @param marker 월 식별값과 잔디 열 위치
   * @return 잔디 월 라벨
   */
  const renderHeatmapMonth = (marker: HeatmapMonthMarker) => {
    // GitHub 잔디처럼 월이 시작되는 열에 영문 세 글자 라벨을 반환함
    return (
      <span className={styles.heatmapMonth} key={marker.monthKey} style={{ gridColumnStart: marker.column }}>
        {marker.label}
      </span>
    );
  };

  // 두 화면에서 같은 모양과 조작을 제공하는 독서 잔디를 반환함
  return (
    /* 선택 연도의 독서 시간 잔디 영역 */
    <section className={styles.chartBlock}>
      {/* 독서 잔디 제목과 연도 선택 영역 */}
      <div className={styles.chartHeader}>
        <h3 className={titleClassName ?? styles.chartHeaderTitle}>
          {/* "독서 잔디" */}
          {message("frontend.profile.readingStats.heatmapTitle")}
        </h3>
        <CustomSelect
          value={String(heatmap.selectedYear)}
          options={yearOptions}
          ariaLabel={message("frontend.profile.readingStats.yearTitle")}
          className={styles.yearSelect}
          triggerClassName={styles.yearSelectTrigger}
          optionListClassName={styles.yearOptionList}
          optionClassName={styles.yearSelectOption}
          onChange={onYearChange}
        />
      </div>
      {/* 독서 시간 잔디 가로 스크롤 영역 */}
      <HorizontalScrollArea key={heatmap.selectedYear} hint={scrollHint}>
        {/* 영문 월 라벨과 날짜별 독서 시간 잔디 목록 영역 */}
        <div className={styles.heatmapCalendar}>
          <div
            className={styles.heatmapMonths}
            style={{ gridTemplateColumns: `repeat(${heatmapColumns}, 10px)` }}
          >
            {heatmapMonths.map(renderHeatmapMonth)}
          </div>
          <div className={styles.heatmapGrid}>
            {new Array<undefined>(heatmapOffset).fill(undefined).map(renderHeatmapSpacer)}
            {heatmap.heatmapList.map(renderHeatmapDay)}
          </div>
        </div>
      </HorizontalScrollArea>
      {/* 독서 타이머 기록 기준 안내와 독서 시간 강도 범례 영역 */}
      <div className={styles.heatmapLegendRow}>
        <p className={styles.heatmapHelp}>
          {/* "독서 타이머 기록을 기준으로 합니다." */}
          {message("frontend.profile.readingStats.heatmapHelp")}
        </p>
        <div className={styles.heatmapLegend} aria-hidden="true">
          <span>{/* "적음" */ message("frontend.profile.readingStats.less")}</span>
          <span className={styles.legendCell} style={{ backgroundColor: HEATMAP_COLORS[0] }} />
          <span className={styles.legendCell} style={{ backgroundColor: HEATMAP_COLORS[1] }} />
          <span className={styles.legendCell} style={{ backgroundColor: HEATMAP_COLORS[2] }} />
          <span className={styles.legendCell} style={{ backgroundColor: HEATMAP_COLORS[3] }} />
          <span className={styles.legendCell} style={{ backgroundColor: HEATMAP_COLORS[4] }} />
          <span>{/* "많음" */ message("frontend.profile.readingStats.more")}</span>
        </div>
      </div>
    </section>
  );
}

/**
 * 독서 상태 코드에 대응하는 화면 표시명을 조회함
 *
 * @author SeungHyeon.Kang
 * @param reptStat 읽는 중, 완독, 중단 상태 코드
 * @return 독서 상태 표시명
 */
const getStatusName = (reptStat: ReadingStatusCount["reptStat"]): string => {
  // 읽는 중 상태의 화면 문구를 반환함
  if (reptStat === "READ") {
    // "읽는 중"
    return message("frontend.common.reading");
  }

  // 완독 상태의 화면 문구를 반환함
  if (reptStat === "DONE") {
    // "완독"
    return message("frontend.profile.readingStats.statusDone");
  }

  // "중단"
  return message("frontend.profile.readingStats.statusStop");
};

/**
 * 독서 상태별 건수를 목표 진행 막대 색상의 연속 구간 배경으로 변환함
 *
 * @author SeungHyeon.Kang
 * @param statusList 읽는 중, 완독, 중단 상태별 독후감 수
 * @return 상태 비율을 반영한 원형 그래프 배경
 */
const getDonutBackground = (statusList: ReadingStatusCount[]): string => {
  let totalCount = 0;

  // 세 상태의 전체 독후감 수를 합산함
  for (const status of statusList) {
    totalCount += Math.max(0, status.reptCnt);
  }

  // 독후감이 없으면 빈 통계 상태를 회색 원으로 표시함
  if (totalCount <= 0) {
    return `conic-gradient(${HEATMAP_COLORS[0]} 0deg 360deg)`;
  }

  const segments: string[] = [];
  let startDegree = 0;

  // 상태별 비율을 이전 구간에 이어지는 각도 범위로 변환함
  for (const status of statusList) {
    const endDegree = startDegree + (Math.max(0, status.reptCnt) / totalCount) * 360;
    // 목표 달성 진행 막대의 초록, 노랑, 분홍 색상을 상태 비율 구간에 재사용함
    segments.push(`${STATUS_COLORS[status.reptStat]} ${startDegree}deg ${endDegree}deg`);
    startDegree = endDegree;
  }

  // 계산된 상태별 색상 구간을 도넛 배경으로 반환함
  return `conic-gradient(${segments.join(", ")})`;
};

/**
 * 스크롤 진입 시 본인 또는 프로필 주인의 독서 통계를 지연 조회해 표시함
 *
 * @author SeungHyeon.Kang
 * @param targetUserNumb 다른 사용자 공개 프로필에서 조회할 회원 번호
 * @return 연도별 독서 시간 잔디와 상태 비율 영역
 */
function ReadingStatisticsSection({ targetUserNumb }: ReadingStatisticsSectionProps) {

  // 본인 통계의 상위 책에서 연결 독후감 상세로 이동할 라우터 함수를 생성함
  const navigate = useNavigate();
  // 통계 영역의 스크롤 진입 여부를 관찰할 요소 참조를 생성함
  const sectionRef = useRef<HTMLElement | null>(null);
  // 화면 이탈과 재시도에서 이전 조회를 취소할 요청 참조를 생성함
  const abortControllerRef = useRef<AbortController | null>(null);
  // 동일 화면에서 자동 지연 조회가 한 번만 실행되도록 상태 참조를 생성함
  const requestedRef = useRef(false);
  // 서버가 반환한 본인 또는 공개 독서 통계 상태를 생성함
  const [statistics, setStatistics] = useState<ReadingStatistics | null>(null);
  // 독서 통계 조회 진행 상태를 생성함
  const [isLoading, setIsLoading] = useState(false);
  // 독서 통계 조회 실패 상태를 생성함
  const [isError, setIsError] = useState(false);
  // 비공개 또는 제한 계정의 통계 비표시 상태를 생성함
  const [isUnavailable, setIsUnavailable] = useState(false);
  // 본인 독서 통계 설정 모달 표시 상태를 생성함
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  // 통계 설정 모달에서 선택한 공개 여부 상태를 생성함
  const [selectedPublic, setSelectedPublic] = useState<"Y" | "N">("N");
  // 독서 통계 설정 저장 진행 상태를 생성함
  const [isSaving, setIsSaving] = useState(false);
  const isOwner = targetUserNumb === undefined;

  // 독서 통계 설정 모달이 열린 동안 배경 화면 스크롤을 잠금
  useBodyScrollLock(isSettingsOpen);

  /**
   * 본인 또는 공개 독서 통계를 조회하고 로딩 및 실패 상태를 화면에 반영함
   *
   * @author SeungHyeon.Kang
   * @param readYear 조회할 연도, 없으면 현재 연도
   * @return 독서 통계 조회 완료 Promise
   */
  const loadStatistics = useCallback(async (readYear?: number): Promise<void> => {
    // 동일 화면에서 이전 재시도 요청이 남아 있으면 최신 요청만 유지함
    abortControllerRef.current?.abort();
    // 화면 이탈 시 독서 통계 조회를 취소할 요청 제어 객체를 생성함
    const abortController = new AbortController();
    // 현재 독서 통계 요청의 취소 제어 객체를 저장함
    abortControllerRef.current = abortController;
    // 지연 조회가 시작됐음을 화면 상태에 반영함
    setIsLoading(true);
    // 이전 독서 통계 실패 상태를 초기화함
    setIsError(false);

    // 독서 통계 조회 성공과 실패 및 종료 상태를 각각 처리함
    try {
      let response: ReadingStatistics | null;

      // 대상 번호가 있으면 프로필 주인이 공개한 통계만 조회함
      if (targetUserNumb !== undefined) {
        // 다른 사용자가 공개한 독서 통계를 조회함
        response = await getSocialReadingStatsApi(targetUserNumb, readYear, abortController.signal);

      } else {
        // 현재 화면의 본인 전용 독서 통계를 조회함
        response = await getReadingStatsApi(readYear, abortController.signal);
      }

      // 화면이 유지되는 동안 조회한 독서 통계 또는 비공개 상태를 표시함
      if (!abortController.signal.aborted) {
        // 공개 가능한 통계가 없는 프로필은 빈 카드를 남기지 않도록 상태를 설정함
        setIsUnavailable(response === null);
        // 검증된 독서 통계 응답 데이터를 화면 상태에 설정함
        setStatistics(response);
      }

    } catch {
      // 화면 이탈로 취소된 요청은 실패 화면으로 전환하지 않음
      if (!abortController.signal.aborted) {
        // 독서 통계를 다시 조회할 수 있도록 실패 상태를 설정함
        setIsError(true);
      }

    } finally {
      // 다른 재시도 요청이 시작된 경우 이전 요청이 로딩 상태를 덮어쓰지 않음
      if (abortControllerRef.current === abortController && !abortController.signal.aborted) {
        // 독서 통계 조회 완료 상태를 화면에 설정함
        setIsLoading(false);
      }

    }
  }, [targetUserNumb]);

  /**
   * 통계 영역에 가까워졌을 때만 최초 조회를 시작하도록 관찰자를 연결함
   *
   * @author SeungHyeon.Kang
   * @return 통계 영역 관찰 해제 함수
   */
  const observeStatistics = useCallback((): (() => void) | undefined => {
    // 이미 조회했거나 관찰할 요소가 없으면 추가 요청을 만들지 않음
    if (requestedRef.current || !sectionRef.current) {
      return undefined;
    }

    // IntersectionObserver를 지원하지 않는 환경은 통계를 즉시 조회함
    if (!("IntersectionObserver" in window)) {
      // 동일 화면에서 다시 자동 조회되지 않도록 최초 요청 상태를 기록함
      requestedRef.current = true;
      // 독서 통계 조회를 시작함
      void loadStatistics();
      return undefined;
    }

    /**
     * 통계 카드가 화면 아래 240px 범위에 들어오면 조회를 시작함
     *
     * @author SeungHyeon.Kang
     * @param entries 관찰 중인 통계 카드의 교차 상태 목록
     * @return 반환값이 없음
     */
    const handleIntersection = (entries: IntersectionObserverEntry[]): void => {
      // 관찰 항목 중 하나라도 화면 근처에 도달했는지 확인함
      for (const entry of entries) {
        // 화면 근처에 도달하지 않은 관찰 결과는 조회를 시작하지 않음
        if (!entry.isIntersecting) {
          continue;
        }

        // 동일 화면에서 통계 쿼리가 한 번만 자동 실행되도록 요청 상태를 기록함
        requestedRef.current = true;
        // 지연된 독서 통계 조회를 시작함
        void loadStatistics();
        // 최초 지연 조회 뒤에는 스크롤 관찰을 종료함
        observer.disconnect();
        break;
      }
    };

    // 화면 진입 직전 통계를 준비하도록 아래쪽 관찰 여백을 둔 관찰자를 생성함
    const observer = new IntersectionObserver(handleIntersection, { rootMargin: "240px 0px" });
    // 통계 카드의 스크롤 위치 관찰을 시작함
    observer.observe(sectionRef.current);

    /**
     * 컴포넌트가 해제될 때 통계 카드 스크롤 관찰을 종료함
     *
     * @author SeungHyeon.Kang
     * @return 반환값이 없음
     */
    const disconnectObserver = (): void => {
      // 더 이상 사용하지 않는 통계 카드 관찰자를 해제함
      observer.disconnect();
    };

    // Effect 정리 단계에서 실행할 관찰 해제 함수를 반환함
    return disconnectObserver;
  }, [loadStatistics]);

  /**
   * 통계 컴포넌트가 해제될 때 진행 중인 API 요청을 취소함
   *
   * @author SeungHyeon.Kang
   * @return 독서 통계 요청 취소 함수
   */
  const prepareRequestCleanup = useCallback((): (() => void) => {
    /**
     * 화면을 벗어난 뒤 독서 통계 응답이 상태를 변경하지 않도록 요청을 취소함
     *
     * @author SeungHyeon.Kang
     * @return 반환값이 없음
     */
    const abortStatisticsRequest = (): void => {
      // 진행 중인 독서 통계 요청을 화면 이탈과 함께 취소함
      abortControllerRef.current?.abort();
    };

    // Effect 정리 단계에서 실행할 요청 취소 함수를 반환함
    return abortStatisticsRequest;
  }, []);

  // 스크롤이 통계 영역에 접근할 때 최초 조회를 시작함
  useEffect(observeStatistics, [observeStatistics]);
  // 화면 이탈 시 진행 중인 독서 통계 요청을 정리함
  useEffect(prepareRequestCleanup, [prepareRequestCleanup]);

  /**
   * 독서 통계 조회 실패 후 사용자의 재시도 요청을 처리함
   *
   * @author SeungHyeon.Kang
   * @return 반환값이 없음
   */
  const handleRetry = (): void => {
    // 실패한 독서 통계 조회를 다시 시작함
    void loadStatistics(statistics?.selectedYear);
  };

  /**
   * 현재 저장값을 복사해 독서 통계 설정 모달을 엶
   *
   * @author SeungHyeon.Kang
   * @return 반환값이 없음
   */
  const handleSettingsOpen = (): void => {
    // 아직 통계를 조회하지 못한 상태에서는 빈 설정 모달을 열지 않음
    if (!statistics) {
      return;
    }

    // 저장된 공개 여부를 모달 선택값으로 설정함
    setSelectedPublic(statistics.publicYsno);
    // 독서 통계 설정 모달을 표시함
    setIsSettingsOpen(true);
  };

  /**
   * 저장 중이 아닐 때 독서 통계 설정 모달을 닫음
   *
   * @author SeungHyeon.Kang
   * @return 반환값이 없음
   */
  const handleSettingsClose = (): void => {
    // 저장 요청 중에는 중복 조작과 화면 이탈을 막기 위해 모달을 유지함
    if (isSaving) {
      return;
    }

    // 독서 통계 설정 모달을 닫음
    setIsSettingsOpen(false);
  };

  /**
   * 모달 바깥 영역을 직접 누른 경우에만 독서 통계 설정 모달을 닫음
   *
   * @author SeungHyeon.Kang
   * @param event 모달 배경 마우스 이벤트
   * @return 반환값이 없음
   */
  const handleOverlayMouseDown = (event: ReactMouseEvent<HTMLDivElement>): void => {
    // 모달 본문 클릭이 배경 닫기로 전파되지 않도록 동일 대상 클릭만 처리함
    if (event.currentTarget === event.target) {
      // 저장 중 여부를 확인하는 공통 닫기 처리를 실행함
      handleSettingsClose();
    }
  };

  /**
   * 공개 여부 선택 버튼의 Y 또는 N 값을 모달 상태에 반영함
   *
   * @author SeungHyeon.Kang
   * @param event 선택한 공개 여부 버튼 이벤트
   * @return 반환값이 없음
   */
  const handleVisibilityClick = (event: ReactMouseEvent<HTMLButtonElement>): void => {
    const publicYsno = event.currentTarget.value;

    // 공통 공개 여부 코드만 선택 상태에 반영함
    if (publicYsno === "Y" || publicYsno === "N") {
      // 선택한 통계 공개 여부를 설정함
      setSelectedPublic(publicYsno);
    }
  };

  /**
   * 잔디에서 조회할 SelectBox 연도를 서버 조회에 반영함
   *
   * @author SeungHyeon.Kang
   * @param readYearValue 선택한 연도 문자열
   * @return 반환값이 없음
   */
  const handleYearChange = (readYearValue: string): void => {
    const readYear = Number(readYearValue);

    // 서버가 제공한 조회 가능 연도 중 현재 선택과 다른 연도만 다시 조회함
    if (!statistics || statistics.selectedYear === readYear || !statistics.availableYears.includes(readYear)) {
      return;
    }

    // 선택한 연도의 독서 시간 잔디를 조회함
    void loadStatistics(readYear);
  };

  /**
   * 선택한 공개 여부를 범용 회원 설정에 저장함
   *
   * @author SeungHyeon.Kang
   * @return 설정 저장 완료 Promise
   */
  const handleSettingsSave = async (): Promise<void> => {
    // 중복 저장을 막도록 독서 통계 설정 요청 시작 상태를 표시함
    setIsSaving(true);

    // 저장 성공과 실패 및 완료 상태를 각각 처리함
    try {
      /**
       * 독서 통계 공개 설정 저장과 현재 화면 상태 반영을 함께 실행함
       *
       * @author SeungHyeon.Kang
       * @return 독서 통계 공개 설정 저장 완료 Promise
       * @throws 독서 통계 공개 설정 저장 또는 응답 검증에 실패하면 발생함
       */
      const saveReadingStatisticsSetting = async (): Promise<void> => {
        // 선택한 독서 통계 공개 설정을 저장함
        const response = await uptReadingStatsSettingApi({
          publicYsno: selectedPublic,
        });
        // 갱신된 공개 상태를 현재 연도별 통계 화면에 반영함
        if (statistics) {
          // 그래프 데이터는 유지하고 저장된 공개 여부만 변경함
          setStatistics({ ...statistics, publicYsno: response });
        }
        // 저장이 완료된 독서 통계 설정 모달을 닫음
        setIsSettingsOpen(false);
      };

      // 설정 반영 후 처리 중 알림을 같은 저장 성공 알림으로 전환함
      await runBlockingOperation(saveReadingStatisticsSetting, {
        success: {
          // "통계 공개 여부가 저장되었습니다."
          title: message("frontend.profile.readingStats.savedTitle"),
          // "선택한 공개 여부를 반영했습니다."
          text: message("frontend.profile.readingStats.saved"),
        },
      });

    } catch (error) {
      // "통계 설정을 저장하지 못했습니다."
      await sweetError(
        message("frontend.profile.readingStats.saveFailed"),
        // 서버가 제공한 안전한 업무 실패 문구 또는 기본 저장 실패 문구를 표시함
        getApiErrorMessage(error, message("frontend.profile.readingStats.saveFailed")),
      );

    } finally {
      // 독서 통계 설정 저장 완료 상태를 반영함
      setIsSaving(false);
    }
  };

  /**
   * 독서 상태별 색상과 건수를 도넛 그래프 범례 한 줄로 렌더링함
   *
   * @author SeungHyeon.Kang
   * @param status 독서 상태 코드와 해당 독후감 수
   * @return 독서 상태 비율 범례 한 항목
   */
  const renderStatusLegend = (status: ReadingStatusCount) => {
    // 독서 상태 색상과 이름 및 권수를 함께 표시하는 범례를 반환함
    return (
      /* 독서 상태 비율 개별 범례 영역 */
      <div className={styles.statusLegendItem} key={status.reptStat}>
        <span className={styles.statusDot} style={{ backgroundColor: STATUS_COLORS[status.reptStat] }} aria-hidden="true" />
        <span className={styles.statusName}>{getStatusName(status.reptStat)}</span>
        <strong className={styles.statusCount}>
          {/* "{0}권" */}
          {message("frontend.common.bookCount", [status.reptCnt])}
        </strong>
      </div>
    );
  };

  /**
   * 현재 연도에 타이머로 오래 읽은 책을 순위와 누적 시간으로 렌더링함
   *
   * @author SeungHyeon.Kang
   * @param bookTime 책 정보와 현재 연도 누적 독서 시간
   * @param index 서버에서 정렬된 책 순번
   * @return 올해 오래 읽은 책 한 항목
   */
  const renderTopBook = (bookTime: ReadingBookTime, index: number) => {
    // "제목 없는 책"
    const bookTitle = bookTime.bookTitl?.trim() || message("frontend.profile.readingStats.unknownBook");
    // "저자 정보 없음"
    const bookAuthor = bookTime.bookAthr?.trim() || message("frontend.common.unknownAuthor");
    // 공개 프로필 응답처럼 독후감 번호가 없는 경우에는 링크를 만들지 않도록 안전한 번호를 계산함
    const reportNumber = bookTime.reptNumb ?? 0;
    // 본인 화면에서 양수 독후감 번호가 확인된 항목만 링크 버튼으로 제공함
    const isReportLink = isOwner && reportNumber > 0;

    /**
     * 올해 상위 책에 연결된 본인 독후감 상세 화면으로 이동함
     *
     * @author SeungHyeon.Kang
     * @return 반환값이 없음
     */
    const handleTopBookClick = (): void => {
      // 서버가 제공한 양수 독후감 번호만 상세 경로에 포함함
      if (!isReportLink) {
        return;
      }

      // 선택한 올해 상위 책의 본인 독후감 상세 화면으로 이동함
      navigate(`/report/detail/${reportNumber}`);
    };

    // 클릭 가능 여부와 관계없이 같은 순위 및 책 정보를 유지할 공통 콘텐츠를 구성함
    const topBookContent = (
      <>
        <strong className={styles.topBookRank}>
          {/* "{0}위" */}
          {message("frontend.profile.readingStats.ranking", [index + 1])}
        </strong>
        <img
          className={styles.topBookCover}
          src={getBookCoverImageSource(bookTime.bookCvim)}
          alt={bookTitle}
          onError={handleBookCoverImageError}
        />
        <div className={styles.topBookInfo}>
          <strong className={styles.topBookTitle}>{bookTitle}</strong>
          <span className={styles.topBookAuthor}>{bookAuthor}</span>
        </div>
        <strong className={styles.topBookTime}>{formatReadingTime(bookTime.readSecs)}</strong>
      </>
    );

    // 본인 화면은 독후감 상세로 이동할 수 있는 버튼으로 순위 항목을 반환함
    if (isReportLink) {
      // 키보드와 포인터로 선택 가능한 올해 상위 책 버튼을 반환함
      return (
        <li key={bookTime.bookNumb}>
          <button
            className={`${styles.topBookItem} ${styles.topBookButton}`}
            type="button"
            onClick={handleTopBookClick}
          >
            {topBookContent}
          </button>
        </li>
      );
    }

    // 공개 프로필에서는 다른 회원의 소유자 전용 독후감으로 이동하지 않는 통계 항목을 반환함
    return (
      /* 올해 타이머 독서 시간이 긴 책 개별 항목 영역 */
      <li className={styles.topBookItem} key={bookTime.bookNumb}>
        {topBookContent}
      </li>
    );
  };

  /**
   * 권수 축 한 눈금을 권 단위 문구로 렌더링함
   *
   * @author SeungHyeon.Kang
   * @param ratingTick 권수 축에 표시할 독후감 수
   * @return 권 단위가 포함된 권수 축 눈금 한 항목
   */
  const renderRatingTick = (ratingTick: number) => {
    // 별도 축 제목 없이 단위를 확인할 수 있는 권수 눈금을 반환함
    return (
      <span className={styles.ratingTick} key={ratingTick}>
        {/* "{0}권" */}
        {message("frontend.profile.readingStats.ratingTick", [ratingTick])}
      </span>
    );
  };

  /**
   * 한 별점의 독후감 수를 권수 축에 비례하는 세로 막대로 렌더링함
   *
   * @author SeungHyeon.Kang
   * @param rating 별점과 해당 독후감 수
   * @return 별점 분포 세로 막대 한 항목
   */
  const renderRating = (rating: ReadingRatingCount) => {
    // 서버 집계값이 소수로 전달되더라도 화면에는 버림 처리한 정수 별점만 표시함
    const ratingGrade = Math.floor(rating.reptGrde);
    // 음수인 비정상 집계값이 차트 높이와 화면 권수에 반영되지 않도록 보정함
    const ratingCount = Math.max(0, rating.reptCnt);
    // 권수 축의 상단 기준에 대한 현재 별점의 세로 막대 높이를 계산함
    const ratingHeight = (ratingCount / ratingAxisMax) * 100;

    // 별점과 권수 및 세로 막대를 한 열로 반환함
    return (
      /* 별점별 독후감 수 세로 막대 개별 항목 영역 */
      <div
        className={styles.ratingBar}
        key={ratingGrade}
        role="listitem"
        aria-label={/* "별점 {0}, {1}권" */ message("frontend.profile.readingStats.ratingBarAria", [ratingGrade, ratingCount])}
      >
        <div className={styles.ratingTrack} aria-hidden="true">
          <span className={styles.ratingFill} style={{ height: `${ratingHeight}%` }}>
            {/* 0권은 숨기고 양수 권수만 실제 막대 바로 위에 표시함 */}
            {ratingCount > 0 && (
              <strong className={styles.ratingCount}>
                {ratingCount}
              </strong>
            )}
          </span>
        </div>
        <span className={styles.ratingGrade} aria-hidden="true">
          <svg className={styles.ratingStar} viewBox="0 0 24 24">
            <path
              d="m12 3.5 2.55 5.17 5.7.83-4.12 4.02.97 5.68L12 16.52 6.9 19.2l.97-5.68L3.75 9.5l5.7-.83L12 3.5Z"
              fill="currentColor"
              stroke="currentColor"
              strokeWidth="1.4"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
          {ratingGrade}
        </span>
      </div>
    );
  };

  /**
   * 현재 연도와 이전 연도의 한 통계 지표 및 증감값을 렌더링함
   *
   * @author SeungHyeon.Kang
   * @param comparisonRow 비교할 지표의 표시값과 증감값
   * @return 연도 비교표 한 행
   */
  const renderComparisonRow = (comparisonRow: ReadingComparisonRow) => {
    // 증감 방향에 맞춰 초록 증가와 분홍 감소 및 회색 동일 배지 스타일을 결정함
    const differenceTone = comparisonRow.difference > 0
      ? styles.comparisonIncrease
      : comparisonRow.difference < 0
        ? styles.comparisonDecrease
        : styles.comparisonSame;

    // 지표명과 두 연도의 값 및 증감 배지를 같은 행으로 반환함
    return (
      /* 현재 연도와 이전 연도 독서 지표 비교 개별 행 */
      <div className={styles.comparisonRow} key={comparisonRow.key}>
        <strong className={styles.comparisonLabel}>{comparisonRow.label}</strong>
        <span className={styles.comparisonValue}>{comparisonRow.currentValue}</span>
        <span className={styles.comparisonValue}>{comparisonRow.previousValue}</span>
        <span className={`${styles.comparisonDifference} ${differenceTone}`}>
          {comparisonRow.differenceLabel}
        </span>
      </div>
    );
  };

  /**
   * 통계 설정 모달의 공개 또는 비공개 선택 버튼을 렌더링함
   *
   * @author SeungHyeon.Kang
   * @param publicYsno 공개 여부 코드
   * @return 공개 여부 선택 버튼
   */
  const renderVisibilityOption = (publicYsno: "Y" | "N") => {
    // 공개 여부 코드에 대응하는 화면 문구 키를 결정함
    const labelKey = publicYsno === "Y"
      ? /* "공개" */ "frontend.common.public"
      : /* "비공개" */ "frontend.common.private";
    // 선택 여부가 시각적으로 구분되는 공개 범위 버튼을 반환함
    return (
      <button
        className={styles.optionButton}
        key={publicYsno}
        type="button"
        value={publicYsno}
        aria-pressed={selectedPublic === publicYsno}
        onClick={handleVisibilityClick}
      >
        {/* "공개" 또는 "비공개" */}
        {message(labelKey)}
      </button>
    );
  };

  // 비공개 프로필에는 통계 카드와 안내 문구를 노출하지 않고 지연 조회 기준점만 유지함
  if (isUnavailable) {
    // 공개할 통계가 없는 프로필의 비표시 관찰 영역을 반환함
    return <section className={styles.observerOnly} ref={sectionRef} aria-hidden="true" />;
  }

  // 완독이 읽는 중보다 먼저 보이도록 서버 상태 목록의 화면 표시 순서를 정렬함
  const orderedStatusList = statistics ? [...statistics.statusList].sort(compareStatusDisplayOrder) : [];
  // 서버 응답이 세 권을 초과하더라도 화면에는 올해 상위 세 권까지만 표시함
  const topBookList = statistics?.topBookList.slice(0, 3) ?? [];
  let totalStatusCount = 0;

  // 도넛 중앙에 표시할 전체 독후감 수를 계산함
  for (const status of orderedStatusList) {
    totalStatusCount += Math.max(0, status.reptCnt);
  }

  // 통계가 준비된 경우에만 현재 및 이전 연도의 비교 행을 구성함
  const comparisonRows = statistics ? getComparisonRows(statistics) : [];
  // 통계가 준비되기 전에도 세로 막대그래프의 기본 권수 축을 유지함
  const ratingAxisTicks = getRatingAxisTicks(statistics?.ratingList ?? []);
  // 첫 번째 권수 눈금을 모든 세로 막대 높이의 상단 기준으로 사용함
  const ratingAxisMax = ratingAxisTicks[0] ?? 1;
  // 서버의 5점부터 0점 순서를 가로축의 0점부터 5점 순서로 바꿀 복사본을 생성함
  const ratingChartList = statistics ? [...statistics.ratingList] : [];
  // 원본 서버 상태는 유지하고 복사한 별점 목록만 오름차순 표시 순서로 뒤집음
  ratingChartList.reverse();
  // 본인 화면과 공개 프로필에 맞는 독서 통계 제목 문구 키를 결정함
  const sectionTitleKey = isOwner
    ? "frontend.profile.readingStats.title"
    : "frontend.profile.readingStats.publicTitle";

  // 스크롤 지연 조회 상태와 연도별 잔디 및 상태 비율과 본인 설정 모달을 포함한 영역을 반환함
  return (
    <>
      {/* 본인 또는 공개 프로필의 독서 시간과 상태 통계 전체 영역 */}
      <section
        className={styles.section}
        ref={sectionRef}
        aria-label={message(sectionTitleKey)}
      >
        {/* 독서 통계 제목 영역 */}
        <header className={styles.header}>
          <h2 className={styles.title}>
            {/* "나의 독서 통계" 또는 "독서 통계" */}
            {message(sectionTitleKey)}
          </h2>
        </header>

        {/* 스크롤 지연 조회 전과 조회 중의 통계 준비 상태 영역 */}
        {!statistics && !isError && (
          <div className={styles.loading} role="status" aria-live="polite">
            <span className={styles.loadingLine} />
            <span className={styles.loadingLine} />
            <span className={styles.loadingLine} />
            <span className={styles.loadingLine} />
            <span className={styles.loadingLine}>
              {/* "독서 통계를 불러오는 중입니다." */}
              <span className={styles.description}>{isLoading ? message("frontend.profile.readingStats.loading") : ""}</span>
            </span>
          </div>
        )}

        {/* 독서 통계 조회 실패와 재시도 영역 */}
        {isError && !statistics && (
          <div className={styles.error} role="alert">
            <span>
              {/* "독서 통계를 불러오지 못했습니다." */}
              {message("frontend.profile.readingStats.loadFailed")}
            </span>
            <button className={styles.retryButton} type="button" onClick={handleRetry}>
              {/* "다시 시도" */}
              {message("frontend.common.retry")}
            </button>
          </div>
        )}

        {/* 조회가 완료된 연도별 잔디와 독서 상태 비율 영역 */}
        {statistics && (
          <div className={styles.content}>
            {/* 선택 연도의 독서 시간 잔디 영역 */}
            <ReadingHeatmapChart heatmap={statistics} onYearChange={handleYearChange} />

            {/* 현재 및 최장 연속 독서 기록 영역 */}
            <section className={styles.chartBlock}>
              <h3 className={styles.chartTitle}>
                {/* "연속 독서 기록" */}
                {message("frontend.profile.readingStats.streakTitle")}
              </h3>
              <div className={styles.streakGrid}>
                {/* 오늘 또는 어제까지 이어진 현재 연속 독서일 영역 */}
                <div className={styles.streakCard}>
                  <span className={styles.streakLabel}>
                    {/* "현재 연속" */}
                    {message("frontend.profile.readingStats.currentStreak")}
                  </span>
                  <strong className={styles.streakValue}>
                    {/* "{0}일" */}
                    {message("frontend.profile.readingStats.days", [statistics.streak.currentStreakDays])}
                  </strong>
                </div>
                {/* 전체 타이머 기록 중 최장 연속 독서일 영역 */}
                <div className={styles.streakCard}>
                  <span className={styles.streakLabel}>
                    {/* "최장 연속" */}
                    {message("frontend.profile.readingStats.longestStreak")}
                  </span>
                  <strong className={styles.streakValue}>
                    {/* "{0}일" */}
                    {message("frontend.profile.readingStats.days", [statistics.streak.longestStreakDays])}
                  </strong>
                </div>
              </div>
            </section>

            {/* 읽는 중과 완독 및 중단 독서 상태 비율 영역 */}
            <section className={styles.chartBlock}>
              <h3 className={styles.chartTitle}>
                {/* "독서 상태 비율" */}
                {message("frontend.profile.readingStats.statusTitle")}
              </h3>
              <div className={styles.statusLayout}>
                {/* 독서 상태 비율 도넛 그래프 영역 */}
                <div className={styles.donut} style={{ background: getDonutBackground(orderedStatusList) }}>
                  {/* 독서 상태 전체 권수 영역 */}
                  <div className={styles.donutCenter}>
                    <strong className={styles.donutTotal}>{totalStatusCount}</strong>
                    <span className={styles.donutLabel}>
                      {/* "전체 권수" */}
                      {message("frontend.profile.readingStats.totalBooks")}
                    </span>
                  </div>
                </div>
                {/* 독서 상태별 색상과 권수 범례 영역 */}
                <div className={styles.statusLegend}>
                  {orderedStatusList.map(renderStatusLegend)}
                </div>
              </div>
            </section>

            {/* 현재 연도에 타이머로 오래 읽은 책 순위 영역 */}
            <section className={styles.chartBlock}>
              <div className={styles.chartHeader}>
                <h3 className={styles.chartHeaderTitle}>
                  {/* "올해 가장 오래 읽은 책" */}
                  {message("frontend.profile.readingStats.topBooksTitle")}
                </h3>
                <span className={styles.chartBasis}>
                  {/* "타이머 기록 기준" */}
                  {message("frontend.profile.readingStats.topBooksBasis")}
                </span>
              </div>
              {/* 올해 책별 타이머 기록의 존재 여부에 따른 순위 또는 빈 상태 영역 */}
              {topBookList.length > 0 ? (
                <ol className={styles.topBookList}>{topBookList.map(renderTopBook)}</ol>
              ) : (
                <p className={styles.emptyState}>
                  {/* "올해 타이머로 기록한 책이 없습니다." */}
                  {message("frontend.profile.readingStats.topBooksEmpty")}
                </p>
              )}
            </section>

            {/* 전체 독후감의 소수점을 버림한 1점 단위 별점 분포 영역 */}
            <section className={styles.chartBlock}>
              <h3 className={styles.chartTitle}>
                {/* "별점 분포" */}
                {message("frontend.profile.readingStats.ratingTitle")}
              </h3>
              {/* 권 단위 세로축과 별점 눈금을 사용하는 별점별 세로 막대그래프 영역 */}
              <div className={styles.ratingChart}>
                <div className={styles.ratingTickList}>{ratingAxisTicks.map(renderRatingTick)}</div>
                <div className={styles.ratingPlotGrid} aria-hidden="true" />
                <div className={styles.ratingBars} role="list">
                  {ratingChartList.map(renderRating)}
                </div>
              </div>
            </section>

            {/* 현재 연도와 이전 연도의 같은 기간 독서 기록 비교 영역 */}
            <section className={styles.chartBlock}>
              <h3 className={styles.chartTitle}>
                {/* "작년과 비교" */}
                {message("frontend.profile.readingStats.comparisonTitle")}
              </h3>
              {/* 지표명과 현재 연도 및 이전 연도와 증감 열 제목 영역 */}
              <div className={styles.comparisonTable}>
                <div className={styles.comparisonHeader} aria-hidden="true">
                  <span />
                  <strong>{statistics.yearComparison.currentYear}</strong>
                  <strong>{statistics.yearComparison.previousYear}</strong>
                  <strong>{message("frontend.profile.readingStats.comparisonChange")}</strong>
                </div>
                {comparisonRows.map(renderComparisonRow)}
              </div>
            </section>

          </div>
        )}
      </section>

      {/* 통계 카드 테두리 바깥 우측 하단의 본인 설정 진입 영역 */}
      {isOwner && statistics && (
        <button className={styles.settingButton} type="button" onClick={handleSettingsOpen}>
          {/* "공개 여부" */}
          {message("frontend.common.visibility")}
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
            <path d="M5.19751 11.62L9.00083 7.81668C9.44999 7.36752 9.44999 6.63252 9.00083 6.18335L5.19751 2.38" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </button>
      )}

      {/* 공개 여부 모달 영역 */}
      {isSettingsOpen && createPortal(
        /* 독서 통계 설정 모달 배경 영역 */
        <div className={styles.modalOverlay} role="presentation" onMouseDown={handleOverlayMouseDown}>
          {/* 독서 통계 설정 모달 본문 영역 */}
          <section className={styles.modal} role="dialog" aria-modal="true" aria-labelledby="reading-statistics-settings-title">
            {/* 독서 통계 설정 제목과 닫기 영역 */}
            <header className={styles.modalHeader}>
              <h2 className={styles.modalTitle} id="reading-statistics-settings-title">
                {/* "공개 여부" */}
                {message("frontend.common.visibility")}
              </h2>
              <button
                className={styles.modalClose}
                type="button"
                disabled={isSaving}
                aria-label={/* "닫기" */ message("frontend.common.close")}
                onClick={handleSettingsClose}
              >
                ×
              </button>
            </header>

            {/* 공개 여부 선택 영역 */}
            <div className={styles.modalBody}>
              {/* 독서 통계 공개 여부 선택과 안내 영역 */}
              <fieldset
                className={styles.settingFieldset}
                aria-label={/* "공개 여부" */ message("frontend.common.visibility")}
                aria-describedby="reading-statistics-visibility-help"
              >
                <p className={styles.settingHelp} id="reading-statistics-visibility-help">
                  {/* "공개로 설정하면 다른 사용자가 내 독서 통계를 볼 수 있습니다." */}
                  {message("frontend.profile.readingStats.publicHelp")}
                </p>
                <div className={styles.optionGrid}>{VISIBILITY_OPTIONS.map(renderVisibilityOption)}</div>
              </fieldset>
            </div>

            {/* 독서 통계 설정 저장 버튼 영역 */}
            <footer className={styles.modalFooter}>
              <button className={styles.cancelButton} type="button" disabled={isSaving} onClick={handleSettingsClose}>
                {/* "취소" */}
                {message("frontend.common.cancel")}
              </button>
              <button className={styles.saveButton} type="button" disabled={isSaving} onClick={handleSettingsSave}>
                {/* "저장하기" */}
                {message("frontend.common.save")}
              </button>
            </footer>
          </section>
        </div>,
        document.body,
      )}
    </>
  );
}

export default ReadingStatisticsSection;
