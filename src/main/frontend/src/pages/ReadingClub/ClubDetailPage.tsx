import {message} from "@/app/messages/message";
import {formatDashedDateToDot} from "@/app/utils/dateUtil";
import {ActionButton} from "@/components/Button/ActionButton";
import LinkButton from "@/components/Button/LinkButton/LinkButton";
import CustomSelect, {type CustomSelectOption} from "@/components/Select/CustomSelect";
import Skeleton from "@/components/Skeleton/Skeleton";
import SearchBookButton from "@/features/Book/Set/components/searchBookButton/SearchBookButton";
import {
  getBookCoverImageSource,
  handleBookCoverImageError,
} from "@/features/Book/utils/bookCoverImage";
import OwnerElectionOverlay from "@/features/ReadingClub/components/OwnerElectionOverlay";
import ReadingGoalResultOverlay from "@/features/ReadingClub/components/ReadingGoalResultOverlay";
import * as resultStyles from "@/features/ReadingClub/components/ReadingGoalResultOverlay.css";
import ProfileImage from "@/features/User/components/ProfileImage";
import {getGoalProgressColor} from "@/features/User/utils/goalProgress";
import {useClubDetailPage} from "@/features/ReadingClub/hooks/useClubDetailPage";
import {getReadingDeadline} from "@/features/ReadingClub/utils/readingClubDeadline";
import clsx from "clsx";
import type {ChangeEvent} from "react";
import {createPortal} from "react-dom";
import {Link, useNavigate} from "react-router-dom";
import * as styles from "./ClubDetailPage.css";

/**
 * fileName       : ClubDetailPage
 * author         : Hanwon.Jang
 * date           : 2026-09-01
 * description    : 모임 상세보기 페이지
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-09-01        Hanwon.Jang    상단 주석 추가 및 리팩토링
 */

// 모임 상세에 한 번에 표시할 프로필 이미지 수를 제한
const MEMBER_PROFILE_VISIBLE_LIMIT = 10;

// 모임 액션 모드 (보기 | 수정 | 삭제)
type ClubDetailAction = "" | "UPDATE" | "DELETE";

const ClubDetailPage = () => {
  const navigate = useNavigate();

  const {
    answers,
    canJoin,
    club,
    isCancellingApplication,
    isDeleting,
    isJoinModalOpen,
    isLeaving,
    isCompletingReading,
    isClosingResult,
    isVotingOwner,
    members,
    ownerElection,
    readingGoalResult,
    handleApplicationCancel,
    handleAnswerChange,
    handleClubAction,
    handleClubLeave,
    handleJoinClub,
    handleJoinAction,
    handleJoinModalClose,
    handleOwnerVote,
    handleReadingHistory,
    handleReadingComplete,
    handleResultClose,
    handleReportWrite,
  } = useClubDetailPage();

  // 상세 데이터가 준비되기 전에는 스켈레톤 로딩을 표시
  if (!club) {
    return (
      <main
        className={styles.page}
        aria-busy="true"
        aria-label={message("frontend.readingClub.common.loading")}
      >
        <Skeleton width="100%" height={156} borderRadius={20}/>
        <Skeleton width="100%" height={289} borderRadius={22}/>
        <Skeleton width="100%" height={80} borderRadius={18}/>
        <Skeleton width="100%" height={108} borderRadius={12}/>
      </main>
    );
  }

  // 모임 공개/비공개 텍스트 표시
  const visibility = club.clubVisb === "PUBLIC"
    ? /* "공개" */ message("frontend.common.public")
    : /* "비공개" */ message("frontend.common.private");

  // 모임원인지에 대한 상태
  const isActiveMember = club.membStat === "ACTIVE";

  // 모임 정보를 볼 수 있는 상태인지
  const canViewOverview = isActiveMember
    || (club.clubVisb === "PUBLIC" && club.clubStat === "ACTIVE");

  // 프로필 이미지는 최대 10명까지만 표시
  const memberProfiles = members.slice(0, MEMBER_PROFILE_VISIBLE_LIMIT);
  // 10명을 초과한 모임원 수만 추가 인원 문구로 표시
  const additionalMemberCount = Math.max(members.length - MEMBER_PROFILE_VISIBLE_LIMIT, 0);
  // 추가 인원이 있는 경우 표시 프로필을 겹쳐 배치
  const hasAdditionalMembers = additionalMemberCount > 0;

  // 예정 또는 진행 중인 회차 번호가 있으면 현재 독서 정보를 표시
  const hasCurrentReading = Number.isFinite(club.currentRondNumb);

  // 현재 독서 회차
  const currentReportCount = Math.max(0, club.currentReportCnt ?? 0);

  // 현재 진행중인 독서가 있는지에 대한 상태
  const hasCurrentReports = club.currentRondStat === "READING" && currentReportCount > 0;

  // 첫 회차가 아직 없으면 다음 독서 순번을 1로 표시
  const readingOrder = club.readingOrdr ?? 1;

  // API 일시값에서 화면과 날짜 계산에 사용할 로컬 날짜 부분만 분리
  const goalStartDate = club.currentGoalStdt?.slice(0, 10);
  const goalEndDate = club.currentGoalEndt?.slice(0, 10);
  const formattedGoalStartDate = formatDashedDateToDot(goalStartDate);
  const formattedGoalEndDate = formatDashedDateToDot(goalEndDate);

  // 같은 연도의 종료일은 연도를 생략
  const readingPeriod = goalStartDate?.slice(0, 4) === goalEndDate?.slice(0, 4)
    ? `${formattedGoalStartDate} ~ ${formattedGoalEndDate.slice(5)}`
    : `${formattedGoalStartDate} ~ ${formattedGoalEndDate}`;

  // 목록과 상세 화면이 같은 날짜 경계와 문구를 사용하도록 공통 표시값을 조회
  const readingDeadline = getReadingDeadline(club.currentGoalEndt);

  // 현재 회차에는 모임장이 반드시 참여하므로 빈 집계도 한 명으로 표시
  const goalMemberCount = Math.max(1, club.currentGoalMembCnt ?? 0);
  const goalAchievementCount = Math.min(
    goalMemberCount,
    Math.max(0, club.currentGoalAchvCnt ?? 0),
  );

  // 달성률 계산
  const goalAchievementRate = goalMemberCount > 0
    ? (goalAchievementCount / goalMemberCount) * 100
    : 0;
  // 달성률 그래프 컬러 계산
  const goalProgressColor = getGoalProgressColor(goalAchievementRate);

  // 조기모임이 가능한 상태
  const canEarlyClose = club.membRole === "OWNER"
    && club.currentRondStat === "READING"
    && Boolean(readingDeadline)
    && readingDeadline?.state !== "ENDED"
    && goalAchievementCount === goalMemberCount;

  // 현재 유저의 독서 상태 라벨 (다 읽었어요 | 중단했어요 | 읽고 있어요)
  const currentReportStatusLabel = club.currentReportStat === "DONE"
    ? message("frontend.report.status.done")
    : club.currentReportStat === "STOP"
      ? message("frontend.report.status.stopped")
      : club.currentReportStat === "READ"
        ? message("frontend.report.status.reading")
        : message("frontend.readingClub.detail.readingParticipationUnavailable");


  const clubActionOptions: readonly CustomSelectOption<ClubDetailAction>[] = [
    {
      value: "UPDATE",
      label: /* "수정하기" */ message("frontend.common.update"),
      disabled: isDeleting,
    },
    {
      value: "DELETE",
      label: /* "삭제하기" */ message("frontend.common.delete"),
      className: styles.dangerOption,
      disabled: isDeleting,
    },
  ];

  // 다음 도서 투표 이동 클릭 함수
  const handleNextBookVote = () => {

    navigate(`/reading-clubs/vote/book/${club.clubNumb}`);
  };

  // 현재 활성 모임의 채팅 화면으로 이동함
  const handleClubChat = (): void => {
    navigate(`/reading-clubs/chat/${club.clubNumb}`);
  };

  /**
   * 현재 진행 회차의 모임원 완료 독후감 목록으로 이동
   *
   * @author HanWon.Jang
   */
  const handleCurrentReports = (): void => {
    // 현재 회차가 유효할 때만 모임원 독후감 목록으로 이동
    if (!Number.isFinite(club.currentRondNumb)) {
      return;
    }

    navigate(`/reading-clubs/history/${club.clubNumb}/${club.currentRondNumb}/reports`, {
      state: {
        title: club.currentBookTitl,
        author: club.currentBookAthr,
        cover: club.currentBookCvim,
      },
    });
  };

  /**
   * 승인제 모임의 가입 질문과 답변 입력란을 표시함
   *
   * @author HanWon.Jang
   * @param question 표시할 가입 질문
   * @param index 질문 순서
   * @return 가입 질문과 답변 입력 영역
   */
  const renderJoinQuestion = (question: string, index: number) => {
    const answer = answers[index] ?? "";
    const inputId = `club-join-answer-${index}`;
    // "답변을 작성해주세요"
    const answerPlaceholder = message("frontend.readingClub.detail.answerPlaceholder");
    // "{0}/200"
    const answerCount = message("frontend.readingClub.detail.answerCount", [answer.length]);

    /**
     * 현재 가입 질문의 답변을 변경함
     *
     * @author HanWon.Jang
     * @param event 답변 입력 변경 이벤트
     * @return 반환값이 없음
     */
    const handleChange = (event: ChangeEvent<HTMLTextAreaElement>): void => {
      // 변경한 질문 순서와 답변을 가입 신청 상태에 반영함
      handleAnswerChange(index, event.currentTarget.value);
    };

    // 가입 질문과 최대 200자의 답변 입력란을 반환함
    return (
      <div className={styles.joinQuestionItem} key={`${question}-${index}`}>
        <label className={styles.joinQuestionLabel} htmlFor={inputId}>
          {/* "{0}. {1}" */}
          {message("frontend.readingClub.detail.joinQuestion", [index + 1, question])}
        </label>
        {/* 가입 질문 답변 입력과 글자 수 영역 */}
        <div className={styles.joinAnswerField}>
          <textarea
            className={styles.joinAnswerInput}
            id={inputId}
            value={answer}
            maxLength={200}
            placeholder={answerPlaceholder}
            autoFocus={index === 0}
            onChange={handleChange}
          />
          <span className={styles.joinAnswerCount}>{answerCount}</span>
        </div>
      </div>
    );
  };

  return (
    <>
      <main className={styles.page}>
        <header className={styles.clubSummary}>
          {club.membRole === "OWNER" ? (
            <CustomSelect<ClubDetailAction>
              className={styles.moreSelect}
              triggerClassName={styles.moreButton}
              optionListClassName={styles.moreOptionList}
              optionClassName={styles.moreOption}
              value=""
              options={clubActionOptions}
              ariaLabel={message("frontend.readingClub.detail.more")}
              triggerContent={<img className={styles.moreIcon} src="/img/icons/icon-more.svg" alt=""/>}
              showArrow={false}
              onChange={handleClubAction}
            />
          ) : null}
          <div className={styles.chips}>
            {club.categoryList?.map((category) => (
              <span className={styles.chip} key={category.intrCode}>{category.intrName}</span>
            ))}
          </div>
          <div className={styles.summaryText}>
            <h1 className={styles.detailTitle}>{club.clubName}</h1>
            <p className={styles.meta}>
              {message("frontend.readingClub.detail.summaryMeta", [
                visibility,
                club.memberCnt,
                club.ownrNick ?? "-",
              ])}
            </p>
          </div>
          <p className={styles.description}>{club.clubCntn || "-"}</p>
        </header>

        {canViewOverview ? (
          <>
            <section className={styles.section}>
              <h2 className={styles.sectionTitle}>{message("frontend.readingClub.detail.currentReading")}</h2>

              <div>
                <div className={styles.currentReadingCard}>
                  <div className={styles.readingCardHeader}>
                    <strong className={styles.readingOrder}>
                      {/* "{0}번째 독서" */}
                      {message("frontend.readingClub.detail.readingOrder", [readingOrder])}
                    </strong>
                    {hasCurrentReading && readingDeadline ? (
                      <span
                        className={styles.dDay}
                        data-ended={readingDeadline.state === "ENDED"}
                      >
                      {readingDeadline.label}
                    </span>
                    ) : null}
                  </div>
                  {hasCurrentReading ? (
                    <div className={styles.currentReadingContent}>
                      <div className={styles.readingBook}>
                        <img
                          className={styles.currentBookImage}
                          src={getBookCoverImageSource(club.currentBookCvim)}
                          onError={handleBookCoverImageError}
                          alt={club.currentBookTitl ?? ""}
                        />
                        <div className={styles.currentBookInformation}>
                          <div className={styles.currentBookSummary}>
                            <div className={styles.currentBookIdentity}>
                              <strong className={styles.currentBookTitle}>{club.currentBookTitl}</strong>
                              {club.currentBookAthr ? (
                                <span className={styles.currentBookAuthor}>{club.currentBookAthr}</span>
                              ) : null}
                            </div>
                            {goalStartDate && goalEndDate ? (
                              <span className={styles.currentReadingPeriod}>{readingPeriod}</span>
                            ) : null}
                          </div>
                          {isActiveMember ? (
                            <div className={styles.myReadingStatus}>
                            <span className={styles.myReadingStatusLabel}>
                              {message("frontend.readingClub.detail.myReadingStatus")}
                            </span>
                              <span
                                className={clsx(
                                  styles.myReadingStatusValue,
                                  !club.currentReportStat && styles.readingStatusUnavailable,
                                )}
                              >
                              <span className={styles.readingStatusDot} aria-hidden="true"/>
                                {currentReportStatusLabel}
                            </span>
                            </div>
                          ) : null}
                        </div>
                      </div>
                      <div className={styles.goalStatus}>
                        <div className={styles.goalProgressTrack}>
                        <span
                          className={styles.goalProgressFill}
                          style={{
                            width: `${goalAchievementRate}%`,
                            backgroundColor: goalProgressColor,
                          }}
                        />
                        </div>
                        <span className={styles.goalAchievementText}>
                        {/* "{0}/{1}명 목표 달성" */}
                          {message("frontend.readingClub.detail.goalAchievement", [
                            goalAchievementCount,
                            goalMemberCount,
                          ])}
                      </span>
                      </div>
                    </div>
                  ) : (
                    <div className={styles.readingEmpty}>
                      {/* 현재 독서 등록을 시작하는 책 검색은 모임장에게만 제공함 */}
                      {club.membRole === "OWNER" ? (
                        <SearchBookButton to={`/reading-clubs/books/search/${club.clubNumb}`}/>
                      ) : null}
                      <p>{message("frontend.readingClub.detail.currentReadingEmpty")}</p>
                    </div>
                  )}
                </div>

                {canEarlyClose ? (
                  <button
                    type={"button"}
                    className={styles.earlyCloseButton}
                    disabled={isCompletingReading}
                    onClick={() => void handleReadingComplete()}
                  >
                    {/* "독서 조기 마감" */}
                    <strong
                      className={styles.earlyCloseButtonTitle}>{message("frontend.readingClub.detail.earlyCloseButton")}</strong>
                    <small
                      className={styles.navigationDescription}>{message("frontend.readingClub.detail.earlyCloseButtonDescription")}</small>
                  </button>
                ) : null}

                {/* 독서 관리하기 */}
                {club.membRole === "OWNER" && hasCurrentReading ? (
                  <LinkButton
                    link={`/reading-clubs/update/book/${club.clubNumb}/${club.currentRondNumb}`}
                    className={styles.managementReadingBtn}
                    replace
                  >
                    {message("frontend.readingClub.management.reading")}
                    <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path
                        d="M6.68262 14.9401L11.5726 10.0501C12.1501 9.47257 12.1501 8.52757 11.5726 7.95007L6.68262 3.06006"
                        stroke="#878787" strokeWidth="1.5" strokeMiterlimit="10" strokeLinecap="round"
                        strokeLinejoin="round"/>
                    </svg>
                  </LinkButton>
                ) : null}

              </div>
            </section>

            <section className={styles.section}>
              <div>
                {/* 함께 읽는 멤버 */}
                <div className={styles.memberHeader}>
                  <h2 className={styles.sectionTitle}>
                    {message("frontend.readingClub.detail.members", [club.memberCnt])}
                  </h2>

                  {/* 모임 채팅 버튼 */}
                  {isActiveMember ? (
                    <button className={styles.chatButton} type="button" onClick={handleClubChat}>
                      <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path
                          d="M10.3933 8.26667V10.98C10.3933 11.22 10.3666 11.4467 10.3066 11.6533C10.0599 12.6333 9.24659 13.2467 8.12659 13.2467H6.31325L4.29992 14.5867C3.99992 14.7933 3.59992 14.5733 3.59992 14.2133V13.2467C2.91992 13.2467 2.35325 13.02 1.95992 12.6267C1.55992 12.2267 1.33325 11.66 1.33325 10.98V8.26667C1.33325 7 2.11992 6.12667 3.33325 6.01333C3.41992 6.00667 3.50659 6 3.59992 6H8.12659C9.48659 6 10.3933 6.90667 10.3933 8.26667Z"
                          fill="#293038"/>
                        <path
                          d="M11.8333 10.4002C12.6799 10.4002 13.3933 10.1202 13.8866 9.62016C14.3866 9.12683 14.6666 8.4135 14.6666 7.56683V4.16683C14.6666 2.60016 13.3999 1.3335 11.8333 1.3335H6.16659C4.59992 1.3335 3.33325 2.60016 3.33325 4.16683V4.66683C3.33325 4.8535 3.47992 5.00016 3.66659 5.00016H8.12659C9.93325 5.00016 11.3933 6.46016 11.3933 8.26683V10.0668C11.3933 10.2535 11.5399 10.4002 11.7266 10.4002H11.8333Z"
                          fill="#293038"/>
                      </svg>
                      {message("frontend.readingClub.detail.clubChat")}
                    </button>
                  ) : null}
                </div>
                <div className={styles.memberSummary}
                     aria-label={message("frontend.readingClub.detail.members", [club.memberCnt])}>
                  {/* 참여한 모임원 프로필 이미지 목록 영역 */}
                  <ul
                    className={clsx(
                      styles.memberProfiles,
                      hasAdditionalMembers && styles.memberProfilesOverlapped,
                    )}
                  >
                    {memberProfiles.map((member) => (
                      <li className={styles.memberProfileItem} key={member.userNumb}>
                        {/* 모임원 프로필 이미지 영역 */}
                        <Link
                          className={styles.memberProfileLink}
                          to={member.mineYsno === "Y" ? "/mypage/profile" : `/social/profile/${member.userNumb}`}
                          aria-label={message("frontend.profile.view", [member.userNick ?? "-"])}
                        >
                          <ProfileImage
                            className={styles.memberProfileImage}
                            src={member.porfPath}
                            alt=""
                            title={member.userNick}
                          />
                        </Link>
                      </li>
                    ))}
                  </ul>
                  {hasAdditionalMembers ? (
                    <span className={styles.memberCountText}>
                    +{message("frontend.readingClub.common.memberCount", [additionalMemberCount])}
                  </span>
                  ) : null}
                </div>

                {/* 멤버 관리는 활성 모임장에게만 제공함 */}
                {club.membRole === "OWNER" ? (
                  <LinkButton
                    link={`/reading-clubs/manage/members/${club.clubNumb}`}
                    className={styles.managementMembersBtn}
                  >
                    {message("frontend.readingClub.management.members")}
                    <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path
                        d="M6.68262 14.9401L11.5726 10.0501C12.1501 9.47257 12.1501 8.52757 11.5726 7.95007L6.68262 3.06006"
                        stroke="#878787" strokeWidth="1.5" strokeMiterlimit="10" strokeLinecap="round"
                        strokeLinejoin="round"/>
                    </svg>
                  </LinkButton>
                ) : null}
              </div>
            </section>

            <nav className={styles.clubNavigation} aria-label={message("frontend.readingClub.detail.clubMenu")}>
              {isActiveMember ? (
                <button
                  className={clsx(resultStyles.navigationButton, styles.showReviewsButton)}
                  type="button"
                  onClick={handleCurrentReports}
                >
                  {hasCurrentReports ? (
                      /* "모임원 독후감 {0}편 보기" */
                      message("frontend.readingClub.result.viewReports", [currentReportCount])
                    ) :
                    /* "모임원 독후감 보기" */
                    message("frontend.readingClub.result.viewReportsDefault")
                  }
                  <img src="/img/icons/icon-chevron-right.svg" alt="" aria-hidden="true"/>
                </button>
              ) : null}

              {isActiveMember ? (
                <button
                  className={styles.navigationRow}
                  type="button"
                  onClick={handleNextBookVote}
                >
                  {/* 다음 도서 투표 */}
                  <span>
                    <strong>{message("frontend.readingClub.detail.nextVote")}</strong>
                    <small className={styles.navigationDescription}>
                      {message("frontend.readingClub.detail.nextVoteDescription")}
                    </small>
                  </span>
                  <img
                    src="/img/icons/icon-chevron-right.svg"
                    alt="chevron"
                    aria-hidden="true"
                  />
                </button>
              ) : null}

              {/* 이전 독서 기록 */}
              <button
                className={styles.readingHistoryButton}
                type="button"
                onClick={handleReadingHistory}
              >
                <strong>{message("frontend.readingClub.detail.previousReading")}</strong>
                <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path
                    d="M6.68262 14.9401L11.5726 10.0501C12.1501 9.47257 12.1501 8.52757 11.5726 7.95007L6.68262 3.06006"
                    stroke="#878787" strokeWidth="1.5" strokeMiterlimit="10" strokeLinecap="round"
                    strokeLinejoin="round"/>
                </svg>
              </button>

              {isActiveMember && club.membRole === "MEMBER" ? (
                /* 모임 자진 탈퇴 */
                <button
                  className={styles.clubLeaveButton}
                  type="button"
                  onClick={handleClubLeave}
                  disabled={isLeaving}
                >
                  <strong>{message("frontend.readingClub.detail.leaveButton")}</strong>
                  <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path
                      d="M6.68262 14.9401L11.5726 10.0501C12.1501 9.47257 12.1501 8.52757 11.5726 7.95007L6.68262 3.06006"
                      stroke="#D84A5F" strokeWidth="1.5" strokeMiterlimit="10" strokeLinecap="round"
                      strokeLinejoin="round"/>
                  </svg>
                </button>
              ) : null}
            </nav>
          </>
        ) : null}
      </main>

      {/* 가입 신청 대기 중인 상태일 때의 버튼 */}
      {club.joinStat === "PENDING" ? (
        <div className={styles.JoinButtonArea}>
          <ActionButton
            type="button"
            size="lg"
            variant="secondary"
            width="full"
            disabled={isCancellingApplication}
            onClick={() => void handleApplicationCancel()}
          >
            {message("frontend.readingClub.detail.cancelApplicationButton")}
          </ActionButton>
          <small className={styles.JoinButtonDescription}>{message("frontend.readingClub.detail.pendingDescription")}</small>
        </div>
      ) : null}

      {/* 모임 미가입 회원일 때 보여지는 버튼 */}
      {canJoin ? (
        <div className={styles.JoinButtonArea}>
          <ActionButton
            size="lg"
            width="full"
            onClick={handleJoinAction}
          >
            {club.joinType === "OPEN"
              ? message("frontend.readingClub.detail.joinNow")
              : message("frontend.readingClub.detail.apply")}
          </ActionButton>

          {club.joinType === "OPEN" ? (
            <small className={styles.JoinButtonDescription}>
              {message("frontend.readingClub.detail.joinNowDescription")}
            </small>
          ) : null}
        </div>
      ) : null}

      {/* 승인제 모임 가입 신청 팝업 영역 */}
      {canJoin && club.joinType === "APPROVAL" && isJoinModalOpen ? createPortal(
        <div className={styles.joinModalOverlay} role="presentation">
          {/* 가입 질문 답변 모달 본문 영역 */}
          <section
            className={styles.joinModal}
            role="dialog"
            aria-modal="true"
            aria-labelledby="club-join-modal-title"
            aria-describedby="club-join-modal-description"
          >
            {/* 가입 신청 모달 헤더 영역 */}
            <header className={styles.joinModalHeader}>
              <h2 className={styles.joinModalTitle} id="club-join-modal-title">
                {/* "가입 신청하기" */}
                {message("frontend.readingClub.detail.apply")}
              </h2>
              {/* "닫기" */}
              <button
                className={styles.joinModalClose}
                type="button"
                aria-label={message("frontend.common.close")}
                onClick={handleJoinModalClose}
              >
                <img src="/img/icons/icon-close.svg" alt="" aria-hidden="true" />
              </button>
            </header>

            {/* 가입 안내와 질문 답변 스크롤 영역 */}
            <div className={styles.joinModalContent}>
              {/* 가입 신청 안내 영역 */}
              <div className={styles.joinModalIntro}>
                <strong className={styles.joinModalHeading}>
                  {/* "아래 질문에 답변해 주세요" */}
                  {message("frontend.readingClub.detail.answerHeading")}
                </strong>
                <p className={styles.joinModalDescription} id="club-join-modal-description">
                  {/* "승인제 모임은 모임장이 답변을 확인한 후 가입이 승인돼요." */}
                  {message("frontend.readingClub.detail.answerDescription")}
                </p>
              </div>

              {/* 가입 질문과 답변 입력 목록 영역 */}
              <div className={styles.joinQuestionList}>
                {club.questionList?.map(renderJoinQuestion)}
              </div>

              {/* 가입 답변 보관 안내 영역 */}
              <aside className={styles.joinRetentionNotice}>
                <strong className={styles.joinRetentionTitle}>
                  {/* "답변 보관 안내" */}
                  {message("frontend.readingClub.detail.answerRetentionTitle")}
                </strong>
                <p className={styles.joinRetentionDescription}>
                  {/* "답변은 모임장의 승인 또는 거절 시 즉시 삭제돼요." */}
                  {message("frontend.readingClub.detail.answerRetentionDescription")}
                </p>
              </aside>
            </div>

            {/* 가입 신청 제출 버튼 영역 */}
            <div className={styles.joinModalActions}>
              <ActionButton size="lg" width="full" onClick={handleJoinClub}>
                {/* "가입 신청하기" */}
                {message("frontend.readingClub.detail.apply")}
              </ActionButton>
            </div>
          </section>
        </div>,
        document.body,
      ) : null}

      {isActiveMember && hasCurrentReading ? (
        <div className={styles.ActionButtonArea}>
          <ActionButton size="lg" width="full" onClick={handleReportWrite}>
            {club.currentReportStat === 'DONE'
              ? /* "내 독후감 보기" */ message("frontend.readingClub.detail.viewReport")

              : /* "내 독후감 쓰기" */ message("frontend.readingClub.detail.writeReport")
            }
          </ActionButton>
        </div>
      ) : null}


      {/* 목표 결과 오버레이 팝업 */}
      {isActiveMember && readingGoalResult ? createPortal(
        <ReadingGoalResultOverlay
          key={readingGoalResult.rondNumb}
          closing={isClosingResult}
          result={readingGoalResult}
          onClose={handleResultClose}
        />,
        document.body,
      ) : null}

      {/* 모임장 승계 투표 오버레이 팝업 */}
      {isActiveMember && !readingGoalResult
      && club.clubStat === "OWNER_ELECTION" && ownerElection ? createPortal(
        <OwnerElectionOverlay
          election={ownerElection}
          submitting={isVotingOwner}
          onVote={handleOwnerVote}
        />,
        document.body,
      ) : null}
    </>
  );
};

export default ClubDetailPage;
