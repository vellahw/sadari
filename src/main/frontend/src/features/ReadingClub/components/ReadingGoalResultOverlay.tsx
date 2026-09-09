import { message } from "@/app/messages/message";
import { formatDashedDateToDot } from "@/app/utils/dateUtil";
import {
  getBookCoverImageSource,
  handleBookCoverImageError,
} from "@/features/Book/utils/bookCoverImage";
import type { ClubReadingGoalResult } from "@/features/ReadingClub/api/readingClubApi";
import ProfileImage from "@/features/User/components/ProfileImage";
import { getGoalProgressColor } from "@/features/User/utils/goalProgress";
import type { CSSProperties } from "react";
import { useNavigate } from "react-router-dom";
import * as styles from "./ReadingGoalResultOverlay.css";
import { ActionButton } from "@/components/Button/ActionButton.tsx";

/**
 * fileName       : ReadingGoalResultOverlay
 * author         : Hanwon.Jang
 * date           : 2026-08-27
 * description    : 모임 독서 목표 결과 오버레이
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-08-27        Hanwon.Jang    주석 추가
 */

const ACHIEVEMENT_PROFILE_VISIBLE_LIMIT = 7;

type ReadingGoalResultOverlayProps = {
  closing?: boolean;
  result: ClubReadingGoalResult;
  onClose?: () => Promise<void>;
  variant?: "overlay" | "page";
};

const ReadingGoalResultOverlay = ({
  closing = false,
  result,
  onClose,
  variant = "overlay",
}: ReadingGoalResultOverlayProps) => {

  // 대상 회차의 모임원 독후감 목록으로 이동할 라우터 함수를 조회
  const navigate = useNavigate();

  // 참여자가 없는 비정상 집계에서도 진행률 계산이 유효한 숫자를 유지
  const achievementRate = result.partCnt > 0
    ? Math.min(100, Math.max(0, (result.goalAchvCnt / result.partCnt) * 100))
    : 0;

  const roundedAchievementRate = Math.round(achievementRate);

  const goalProgressColor = getGoalProgressColor(roundedAchievementRate);
  const goalStartDate = formatDashedDateToDot(result.goalStdt);
  const goalEndDate = formatDashedDateToDot(result.goalEndt);

  // 같은 연도의 종료일은 연도를 생략
  const readingPeriod = result.goalStdt.slice(0, 4) === result.goalEndt.slice(0, 4)
    ? `${goalStartDate} ~ ${goalEndDate.slice(5)}`
    : `${goalStartDate} ~ ${goalEndDate}`;

  // 상세 페이지와 같은 배경 및 결과 표지에 사용할 안전한 도서 이미지 경로를 조회
  const bookCoverSource = getBookCoverImageSource(result.bookCvim);

  // 도서 표지를 팝업 surface의 불투명 블러 배경 이미지로 전달
  const surfaceStyle = {
    "--book-bg-image": `url("${bookCoverSource}")`,
  } as CSSProperties;

  const hasAdditionalAchievementMembers = result.achievementMemberList.length
    > ACHIEVEMENT_PROFILE_VISIBLE_LIMIT;

  const visibleAchievementMembers = result.achievementMemberList.slice(
    0,
    hasAdditionalAchievementMembers
      ? ACHIEVEMENT_PROFILE_VISIBLE_LIMIT - 1
      : ACHIEVEMENT_PROFILE_VISIBLE_LIMIT,
  );

  const additionalAchievementMemberCount = Math.max(
    result.achievementMemberList.length - visibleAchievementMembers.length,
    0,
  );

  // "{0}번째 독서 목표 결과"
  const resultTitle = message("frontend.readingClub.result.roundTitle", [result.readingOrdr]);

  /**
   * 종료 독서 목표 결과 팝업을 닫고 현재 모임 상세 화면을 표시하는 팝업
   *
   * @author HanWon.Jang
   * @return
   */
  const closeReadingGoalResult = async (): Promise<void> => {
    // 상세 팝업의 확인 저장 중이거나 닫기 처리기가 없으면 화면 상태를 변경하지 않음
    if (closing || !onClose) {
      return;
    }

    // 서버 확인이 성공한 뒤 부모 화면이 결과 팝업을 제거하도록 요청함
    await onClose();
  };

  /**
   * 다른 모임원이 쓴 독후감 목록 페이지로 이동하는 함수
   *
   * @author HanWon.Jang
   * @return
   */
  const openReadingRoundReports = ()=> {
    // 목록의 첫 렌더링부터 도서 요약을 표시할 수 있도록 팝업의 도서 정보를 함께 전달함
    navigate(`/reading-clubs/history/${result.clubNumb}/${result.rondNumb}/reports`, {
      state: {
        title: result.bookTitl,
        author: result.bookAthr,
        cover: result.bookCvim,
      },
    });
  }

  // 팝업에서는 배경을 차단하고 페이지에서는 같은 결과 본문만 표시함
  return (
    <>
      {variant === "overlay" ? (
        <>
          {/* 모임 상세와 공통 헤더 및 내비게이션을 어둡게 표시하는 팝업 배경 영역 */}
          <div className={styles.backgroundOverlay} aria-hidden="true" />
        </>
      ) : null}

      <section
        className={variant === "overlay" ? styles.overlay : styles.page}
        role={variant === "overlay" ? "dialog" : undefined}
        aria-modal={variant === "overlay" ? true : undefined}
        aria-label={resultTitle}
      >
        {/* 종료 독서 목표 결과 팝업 본문 영역 */}
        <div
          className={variant === "overlay" ? styles.surface : styles.pageSurface}
          style={surfaceStyle}
        >
          {/* 종료 독서 회차 제목과 팝업 닫기 영역 */}
          {variant === "overlay" ? (
            <header className={styles.header}>
                <button
                  className={styles.closeButton}
                  type="button"
                  aria-label={/* "닫기" */ message("frontend.common.close")}
                  title={/* "닫기" */ message("frontend.common.close")}
                  disabled={closing}
                  onClick={closeReadingGoalResult}
                >
                  <img
                    className={styles.closeIcon}
                    src="/img/icons/icon-close.svg"
                    alt=""
                    aria-hidden="true"
                  />
                </button>
            </header>
              ) : null}

          {/* 종료 회차 도서와 전체 달성률 영역 */}
          <article className={styles.readingCard}>
            <h2 className={styles.title}>{resultTitle}</h2>

            <div className={styles.bookSummary}>
              <img
                className={styles.bookCover}
                src={bookCoverSource}
                alt={result.bookTitl}
                onError={handleBookCoverImageError}
              />
              <div className={styles.bookIdentity}>
                <strong className={styles.bookTitle}>{result.bookTitl}</strong>
                {result.bookAthr ? <span className={styles.bookAuthor}>{result.bookAthr}</span> : null}
                <span className={styles.readingPeriod}>{readingPeriod}</span>
              </div>
            </div>

            <div className={styles.progressArea}>
              <div className={styles.progressRow}>
                <div className={styles.progressTrack}>
                  <span
                    className={styles.progressFill}
                    style={{ width: `${achievementRate}%` }}
                  />
                </div>
                <strong
                  className={styles.progressRate}
                  style={{ color: goalProgressColor }}
                >
                  {roundedAchievementRate}%
                </strong>
              </div>
              <span className={styles.progressDescription}>
                {/* "{0}/{1}명 목표 달성" */}
                {message("frontend.readingClub.detail.goalAchievement", [
                  result.goalAchvCnt,
                  result.partCnt,
                ])}
              </span>
            </div>
          </article>

          {/* 목표 달성자 안내와 프로필 영역 */}
          <article className={styles.achievementCard}>
            <div className={styles.achievementTitleRow}>
              <img
                className={styles.verifiedIcon}
                src="/img/icons/icon-verified.svg"
                alt=""
              />
              <strong className={styles.achievementTitle}>
                {result.myGoalAchieved
                  ? /* "목표를 달성했어요" */ message("frontend.readingClub.result.achieved")
                  : /* "{0}명이 목표를 달성했어요" */ message(
                    "frontend.readingClub.result.achievedMembers",
                    [result.goalAchvCnt],
                  )}
              </strong>
            </div>

            {visibleAchievementMembers.length > 0 ? (
              <div className={styles.achievementProfiles}>
                {visibleAchievementMembers.map((member) => (
                  <ProfileImage
                    key={member.userNumb}
                    className={styles.achievementProfile}
                    src={member.porfPath}
                    alt={member.userNick ?? ""}
                    title={member.userNick}
                  />
                ))}
                {additionalAchievementMemberCount > 0 ? (
                  <span className={styles.additionalAchievementCount}>
                    +{additionalAchievementMemberCount}
                  </span>
                ) : null}
              </div>
            ) : (
              <p className={styles.noAchievement}>
                {/* "이번 회차에는 목표 달성자가 없어요" */}
                {message("frontend.readingClub.result.noAchievement")}
              </p>
            )}
          </article>

          {/* 참여와 달성 및 독후감 수를 요약하는 영역 */}
          <article className={styles.summaryCard}>
            <strong className={styles.summaryTitle}>
              {/* "회차 요약" */}
              {message("frontend.readingClub.result.summary")}
            </strong>
            <dl className={styles.summaryList}>
              <div className={styles.summaryItem}>
                <dt>
                  {/* "참여" */}
                  {message("frontend.readingClub.result.participation")}
                </dt>
                <dd>
                  {/* "{0}명" */}
                  {message("frontend.readingClub.result.memberUnit", [result.partCnt])}
                </dd>
              </div>
              <div className={styles.summaryItem}>
                <dt>
                  {/* "달성" */}
                  {message("frontend.readingClub.result.achievement")}
                </dt>
                <dd>
                  {/* "{0}명" */}
                  {message("frontend.readingClub.result.memberUnit", [result.goalAchvCnt])}
                </dd>
              </div>
              <div className={styles.summaryItem}>
                <dt>
                  {/* "독후감" */}
                  {message("frontend.readingClub.result.report")}
                </dt>
                <dd>
                  {/* "{0}편" */}
                  {message("frontend.readingClub.result.reportUnit", [result.reportCnt])}
                </dd>
              </div>
            </dl>
          </article>

          {/* 모임원 독후감 이동 안내 영역 */}
          <nav className={styles.resultNavigation}>
            {result.reportCnt !== 0 ? (
              <button
                className={styles.navigationButton}
                type="button"
                onClick={openReadingRoundReports}
              >
                <strong>
                  {/* "모임원 독후감 {0}편 보기" */}
                  {message("frontend.readingClub.result.viewReports", [result.reportCnt])}
                </strong>
                <img src="/img/icons/icon-chevron-right.svg" alt="arrow"/>
              </button>
            ) : null }
          </nav>

          {variant === "overlay" ? (
            <>
              {/* 팝업 닫기 영역 */}
              <ActionButton
                onClick={closeReadingGoalResult}
                aria-label={/* "닫기" */ message("frontend.common.close")}
                disabled={closing}
              >
                {/* "닫기" */}
                {message("frontend.common.close")}
              </ActionButton>
            </>
          ) : null}
        </div>
      </section>
    </>
  );
};

export default ReadingGoalResultOverlay;
