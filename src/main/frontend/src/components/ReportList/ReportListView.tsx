/**
 * 공개 독후감과 모임 회차 독후감 페이지가 공유하는 목록 UI를 제공함
 *
 * @author HanWon.Jang
 */
import { getApiErrorMessage } from "@/app/api/resultData";
import { sweetError } from "@/app/lib/sweetAlert/sweetAlert";
import { message } from "@/app/messages/message";
import InfiniteScrollTrigger from "@/components/InfiniteScroll/InfiniteScrollTrigger";
import CustomSelect, {
  type CustomSelectOption,
} from "@/components/Select/CustomSelect";
import UserActionMenu from "@/components/UserActionMenu/UserActionMenu";
import {
  setReportTranslationApi,
  type PublicReportSortType,
} from "@/features/Book/api/bookApi";
import type { PublicReportType } from "@/features/Book/types/book.type";
import type {
  ReportListBookSummary,
  ReportListItem,
} from "@/features/Book/types/reportList.type";
import {
  getBookCoverImageSource,
  handleBookCoverImageError,
} from "@/features/Book/utils/bookCoverImage";
import ProfileImage from "@/features/User/components/ProfileImage";
import ReplySheet from "@/features/reply/ReplySheet";
import LikeUserListButton from "@/features/Social/components/LikeUserListButton";
import { REPORT_CONTENT_PREVIEW_LENGTH } from "@/features/Book/utils/reportListView";
import { useState } from "react";
import * as styles from "./ReportListView.css";
import AnimatedReportContent from "./AnimatedReportContent";

const SORT_OPTIONS: readonly CustomSelectOption<PublicReportSortType>[] = [
  {
    value: "RELATION_DESC",
    label: /* "기본순" */ message("frontend.book.publicReports.sort.default"),
  },
  {
    value: "LATEST_DESC",
    label: /* "최신순" */ message("frontend.book.publicReports.sort.latest"),
  },
  {
    value: "GRADE_DESC",
    label: /* "별점순" */ message("frontend.home.sort.gradeDesc"),
  },
  {
    value: "LIKE_DESC",
    label: /* "추천순" */ message("frontend.book.publicReports.sort.recommended"),
  },
];

type ReportListViewProps = {
  book: ReportListBookSummary;
  reports: ReportListItem[];
  reportsCount: number;
  sort: PublicReportSortType;
  status?: string;
  statusOptions?: readonly CustomSelectOption<string>[];
  emptyMessage: string;
  commentReport: PublicReportType | null;
  focusReplNumb?: number;
  isLikePending: boolean;
  hasNext: boolean;
  isFetchingNext: boolean;
  onSortChange: (sort: PublicReportSortType) => void;
  onStatusChange?: (status: string) => void;
  onToggleReport: (reptNumb: number) => void;
  onProfileClick: (userNumb: number) => void;
  onLike: (report: PublicReportType) => void;
  onOpenReply: (report: PublicReportType) => void;
  onCloseReply: () => void;
  onLoadMore: () => void;
};

/**
 * 독후감 상태에 대응하는 공통 카드 배지 스타일을 반환함
 *
 * @author HanWon.Jang
 * @param statusTone 완료와 중단 및 독서 중 상태 구분값
 * @return 상태 배지 클래스명
 */
const getStatusClassName = (
  statusTone: ReportListItem["statusTone"],
): string => {
  // 완독 상태이면 브랜드 색상의 완료 배지 클래스를 반환함
  if (statusTone === "done") {
    // 완료 상태 배지 클래스를 반환함
    return styles.statusDone;
  }

  // 중단 상태이면 회색의 중단 배지 클래스를 반환함
  if (statusTone === "stopped") {
    // 중단 상태 배지 클래스를 반환함
    return styles.statusStopped;
  }

  // 나머지 상태에는 독서 중 배지 클래스를 반환함
  return styles.statusReading;
};

/**
 * 도서 요약과 필터 및 독후감 카드 목록을 동일한 화면 구조로 표시함
 *
 * @author HanWon.Jang
 * @param props 독후감 목록 표시 데이터와 사용자 동작 처리 함수
 * @return 공통 독후감 목록 UI
 */
const ReportListView = ({
  book,
  reports,
  reportsCount,
  sort,
  status,
  statusOptions,
  emptyMessage,
  commentReport,
  focusReplNumb,
  isLikePending,
  hasNext,
  isFetchingNext,
  onSortChange,
  onStatusChange,
  onToggleReport,
  onProfileClick,
  onLike,
  onOpenReply,
  onCloseReply,
  onLoadMore,
}: ReportListViewProps) => {
  // 독후감 번호별로 최초 조회한 번역문을 현재 화면에서 재사용함
  const [translations, setTranslations] = useState<Record<number, string>>({});
  // 카드별 원문 또는 번역문 표시 상태를 관리함
  const [translatedReports, setTranslatedReports] = useState<Record<number, boolean>>({});
  // 같은 화면에서 중복 번역 요청을 막기 위해 처리 중인 독후감 번호를 관리함
  const [pendingReportNumb, setPendingReportNumb] = useState<number>();
  const showsStatusFilter = status !== undefined
    && statusOptions !== undefined
    && onStatusChange !== undefined;

  /**
   * 카드에 표시할 원문 또는 번역문을 반환함
   *
   * @author HanWon.Jang
   * @param report 표시할 독후감 카드
   * @return 현재 전환 상태에 맞는 독후감 본문
   */
  const getVisibleContent = (report: ReportListItem): string => {
    // 번역 보기 상태이면서 번역문이 있으면 번역문을 표시함
    if (translatedReports[report.reptNumb] && translations[report.reptNumb]) {
      // 현재 화면에 저장한 번역문을 반환함
      return translations[report.reptNumb];
    }

    // 번역 전이거나 원문 보기 상태이면 서버가 조회한 원문을 반환함
    return report.reportContent;
  };

  /**
   * 번역 캐시를 조회하거나 생성한 뒤 원문과 번역문 표시 상태를 전환함
   *
   * @author HanWon.Jang
   * @param report 번역 표시 상태를 변경할 공개 독후감
   * @return 반환값이 없음
   */
  const handleTranslation = async (report: ReportListItem): Promise<void> => {
    // 현재 화면에 번역문이 있으면 외부 요청 없이 원문과 번역문만 전환함
    if (translations[report.reptNumb]) {
      // 선택한 카드의 표시 상태만 반전함
      setTranslatedReports((current) => ({
        ...current,
        [report.reptNumb]: !current[report.reptNumb],
      }));
      return;
    }

    // 같은 독후감의 처리 중 요청은 중복 전송하지 않음
    if (pendingReportNumb === report.reptNumb) {
      return;
    }

    // 번역 요청이 끝날 때까지 선택한 카드의 버튼을 비활성화함
    setPendingReportNumb(report.reptNumb);

    try {
      // 서버가 공개 범위와 월간 한도를 재검증한 번역문을 조회함
      const translation = await setReportTranslationApi(report.reptNumb);
      // 번역문을 카드 번호 기준으로 저장해 이후 전환에 재사용함
      setTranslations((current) => ({
        ...current,
        [report.reptNumb]: translation.trnsCntn,
      }));
      // 최초 번역 성공 직후 해당 카드에 번역문을 표시함
      setTranslatedReports((current) => ({ ...current, [report.reptNumb]: true }));
    }

    // 서버 실패 응답은 원시 예외 없이 공통 메시지로 안내함
    catch (error) {
      // 번역 실패 원인을 서버 메시지 또는 공통 재시도 문구로 표시함
      await sweetError(
        message("frontend.report.translation.failedTitle"),
        getApiErrorMessage(error, message("frontend.common.tryAgain")),
      );
    }

    // 성공과 실패 모두 다음 번역 요청을 허용함
    finally {
      // 처리 중인 독후감 번호를 초기화함
      setPendingReportNumb(undefined);
    }
  };

  // 공개 목록과 모임 회차 목록이 공유하는 화면 구조를 반환함
  return (
    <>
      <main className={styles.page}>
        <div className={styles.content}>
          {/* 도서 표지와 독후감 목록 요약 영역 */}
          <section className={styles.header}>
            <div className={styles.headerWrap}>
              <div className={styles.coverFrame}>
                <img
                  className={styles.coverImage}
                  src={getBookCoverImageSource(book.cover)}
                  onError={handleBookCoverImageError}
                  alt={book.title ?? message("frontend.common.bookInfo")}
                />
              </div>
              <div className={styles.headingArea}>
                <h1 className={styles.bookTitle}>{book.title ?? "-"}</h1>
                <div className={styles.authorRatingLine}>
                  <p className={styles.meta}>{book.author ?? "-"}</p>
                  {book.ratingAverage !== null
                  && book.ratingAverage !== undefined
                  && book.ratingAverage !== "" ? (
                      <>
                        <span className={styles.metaSeparator}>|</span>
                        <span className={styles.ratingSummary}>
                          <span className={styles.ratingStar}>
                            <img
                              src="/img/icons/icon-star-rate.svg"
                              alt=""
                              aria-hidden="true"
                              width="14px"
                            />
                          </span>
                          <span>{book.ratingAverage}</span>
                        </span>
                      </>
                    ) : null}
                </div>
              </div>
            </div>
          </section>

          {/* 독후감 정렬과 선택적 독서 상태 필터 영역 */}
          <section
            className={styles.filters}
            aria-label={message("frontend.book.publicReports.filterLabel")}
          >
            <CustomSelect
              value={sort}
              options={SORT_OPTIONS}
              ariaLabel={message("frontend.home.sort.label")}
              onChange={onSortChange}
            />
            {showsStatusFilter ? (
              <CustomSelect
                value={status}
                options={statusOptions}
                ariaLabel={message("frontend.report.field.status")}
                onChange={onStatusChange}
              />
            ) : null}
          </section>

          {reports.length > 0 ? (
            /* 독후감 카드 목록 영역 */
            <section className={styles.list}>
              {reports.map((report) => (
                <article className={styles.item} key={report.reptNumb}>
                  <div className={styles.itemTop}>
                    <div className={styles.statusWrap}>
                      <button
                        className={styles.profileButton}
                        type="button"
                        onClick={() => onProfileClick(report.userNumb)}
                      >
                        <ProfileImage
                          className={styles.profileImage}
                          src={report.porfPath}
                          alt=""
                        />
                        <span className={styles.writer}>{report.userNick || "-"}</span>
                      </button>
                      <span className={getStatusClassName(report.statusTone)}>
                        {report.reportStatusName}
                      </span>
                    </div>

                    {/* 신고 및 차단 더보기 메뉴 */}
                    <div className={styles.actionMenuWrap}>
                      <UserActionMenu
                        userNick={report.userNick}
                        triggerIconClassName={styles.actionMenuTriggerIcon}
                        reportTarget={{
                          targetType: "REPORT",
                          targetNumb: report.reptNumb,
                          reportNumb: report.reptNumb,
                          userNumb: report.userNumb,
                          userNick: report.userNick,
                          content: report.reptCntn,
                        }}
                      />
                    </div>
                  </div>

                  {/* 독후감 별점 영역 */}
                  <div>
                    <span
                      className={styles.reportRating}
                      aria-label={message("frontend.report.gradeValue", [report.rating])}
                    >
                      {Array.from({ length: 5 }, (_, index) => {
                        const starValue = index + 1;
                        const iconName = report.rating >= starValue
                          ? "icon-star-rate"
                          : report.rating >= starValue - 0.5
                            ? "icon-star-rate-half"
                            : "icon-star-rate-empty";

                        // 현재 별점 값에 대응하는 별 아이콘을 반환함
                        return (
                          <img
                            className={styles.reportRatingIcon}
                            key={starValue}
                            src={`/img/icons/${iconName}.svg`}
                            alt=""
                            aria-hidden="true"
                          />
                        );
                      })}
                    </span>
                  </div>

                  {report.reportContent.length > 0 ? (
                    /* 독후감 본문과 긴 내용 펼치기 영역 */
                    <>
                      <AnimatedReportContent
                        content={getVisibleContent(report)}
                        expanded={report.isExpanded
                          || getVisibleContent(report).length <= REPORT_CONTENT_PREVIEW_LENGTH}
                      />

                      {getVisibleContent(report).length > REPORT_CONTENT_PREVIEW_LENGTH ? (
                        <button
                          className={styles.expandButton}
                          type="button"
                          aria-label={message(
                            report.isExpanded
                              ? "frontend.common.collapse"
                              : "frontend.book.publicReports.expand",
                          )}
                          onClick={() => onToggleReport(report.reptNumb)}
                        >
                          <img
                            className={
                              report.isExpanded
                                ? styles.expandArrowOpen
                                : styles.expandArrow
                            }
                            src="/img/icons/arrow-bottom.svg"
                            alt=""
                            aria-hidden="true"
                          />
                        </button>
                      ) : null}
                    </>
                  ) : null}

                  <footer className={styles.itemFooter}>
                    {report.trnsAvaiYsno === "Y" ? (
                      <button
                        className={styles.translationButton}
                        type="button"
                        disabled={pendingReportNumb === report.reptNumb}
                        onClick={() => void handleTranslation(report)}
                      >
                        {pendingReportNumb === report.reptNumb
                          ? message("frontend.report.translation.loading")
                          : message(translatedReports[report.reptNumb]
                            ? "frontend.report.translation.original"
                            : "frontend.report.translation.view")}
                      </button>
                    ) : <span />}
                    <div className={styles.itemMetrics}>
                      <div className={styles.metricGroup}>
                      <button
                        className={styles.metricIconButton}
                        type="button"
                        aria-label={message("frontend.common.like")}
                        aria-pressed={report.likeYsno === "Y"}
                        disabled={isLikePending}
                        onClick={() => onLike(report)}
                      >
                        {report.likeYsno === "Y" ? (
                          <img src="/img/icons/icon-heart-fill.svg" alt="" aria-hidden="true" />
                        ) : (
                          <img src="/img/icons/icon-heart.svg" alt="" aria-hidden="true" />
                        )}
                      </button>
                      <LikeUserListButton
                        className={styles.metricCountButton}
                        tagtType="REPORT"
                        tagtNumb={report.reptNumb}
                        countLabel={report.likeCountLabel}
                      />
                      </div>
                      <button
                        className={styles.commentButton}
                        type="button"
                        aria-label={message("frontend.book.publicReports.viewComments")}
                        onClick={() => onOpenReply(report)}
                      >
                        <img src="/img/icons/icon-comment.svg" alt="" aria-hidden="true" />
                        <span>{report.commentCountLabel}</span>
                      </button>
                    </div>
                  </footer>
                </article>
              ))}
              <InfiniteScrollTrigger
                hasNext={hasNext}
                isLoading={isFetchingNext}
                onLoadMore={onLoadMore}
              />
            </section>
          ) : (
            <p className={styles.empty}>
              {reportsCount > 0
                ? message("frontend.book.publicReports.filteredEmpty")
                : emptyMessage}
            </p>
          )}
        </div>
      </main>

      {commentReport ? (
        <ReplySheet
          report={commentReport}
          focusReplNumb={focusReplNumb}
          onClose={onCloseReply}
        />
      ) : null}
    </>
  );
};

export default ReportListView;
