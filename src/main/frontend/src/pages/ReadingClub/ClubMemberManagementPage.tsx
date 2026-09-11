/**
 * fileName       : ClubMemberManagementPage
 * author         : HanWon.Jang
 * date           : 2026-08-14
 * description    : 모임장의 가입 신청 확인과 멤버 및 초대 관리 화면을 구성함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-08-14        Hanwon.Jang        최초 생성
 * 2026-08-24        HanWon.Jang        모임원 퇴장 버튼과 사유 모달 추가
 */
import { message } from "@/app/messages/message";
import { useBodyScrollLock } from "@/app/utils/modalUtil";
import { ActionButton } from "@/components/Button/ActionButton";
import Skeleton from "@/components/Skeleton/Skeleton";
import type {
  ClubApplication,
  ClubMemberProfile,
  InviteCandidate,
  SentClubInvitation,
} from "@/features/ReadingClub/api/readingClubApi";
import { useClubMemberManage } from "@/features/ReadingClub/hooks/useClubMemberManage";
import ProfileImage from "@/features/User/components/ProfileImage";
import type { ChangeEvent } from "react";
import { createPortal } from "react-dom";
import { Link, useParams } from "react-router-dom";
import * as styles from "./ClubMemberManagementPage.css";

type CandidateRowProps = {
  candidate: InviteCandidate;
  disabled: boolean;
  onInvite: (userNumb: number) => void;
};

/**
 * 맞팔 초대 후보의 프로필과 초대 실행 버튼을 표시함
 *
 * @author Hanwon.Jang
 * @param candidate 표시할 초대 후보
 * @param disabled 초대 처리 중 여부
 * @param onInvite 회원 초대 함수
 * @return 초대 후보 선택 항목
 */
const CandidateRow = ({ candidate, disabled, onInvite }: CandidateRowProps) => {
  /**
   * 현재 후보에게 모임 초대를 발송함
   *
   * @author Hanwon.Jang
   * @return 반환값이 없음
   */
  const handleInvite = (): void => {
    // 후보 사용자 번호를 사용하여 초대 발송을 요청함
    onInvite(candidate.userNumb);
  };

  // 피그마 카드 구조에 맞춘 프로필 정보와 초대 버튼을 반환함
  return (
    <article className={styles.invitationCard}>
      {/* 초대 후보 프로필 영역 */}
      <ProfileImage
        className={styles.avatar}
        src={candidate.porfPath}
        alt={candidate.userNick ?? ""}
      />
      <span className={styles.candidateInfo}>
        <strong className={styles.profileName}>{candidate.userNick ?? "-"}</strong>
        {candidate.intrText ? (
          <small className={styles.invitationInterest}>{candidate.intrText}</small>
        ) : null}
      </span>
      <ActionButton
        className={styles.inviteActionButton}
        variant="secondary"
        size="sm"
        disabled={disabled}
        onClick={handleInvite}
      >
        {message("frontend.readingClub.memberManage.inviteAction")}
      </ActionButton>
    </article>
  );
};

/**
 * 모임장의 가입 신청 확인과 활성 멤버 관리 화면을 표시함
 *
 * @author Hanwon.Jang
 * @return 멤버와 가입 신청 관리 화면
 */
const ClubMemberManagementPage = () => {
  const {
    applications,
    candidates,
    club,
    isInviteOpen,
    isLoading,
    isSubmitting,
    members,
    sentInvitations,
    selectedApplication,
    selectedMember,
    exitReason,
    handleAnswerClose,
    handleAnswerOpen,
    handleApplicationDecision,
    handleInviteCancel,
    handleInviteClose,
    handleInviteOpen,
    handleInviteSubmit,
    handleExitOpen,
    handleExitClose,
    handleMemberExit,
    setExitReason,
  } = useClubMemberManage();
  const { clubNumb } = useParams<{ clubNumb: string }>();

  // 답변 또는 초대 모달이 열려 있는 동안 배경 화면의 스크롤을 잠금
  useBodyScrollLock(Boolean(selectedApplication) || Boolean(selectedMember) || isInviteOpen);

  /**
   * 강제 퇴장 사유 입력값을 반영함
   *
   * @author SeungHyeon.Kang
   * @param event 사유 입력 변경 이벤트
   * @return 반환값이 없음
   */
  const handleExitReasonChange = (event: ChangeEvent<HTMLTextAreaElement>): void => {
    setExitReason(event.currentTarget.value);
  };

  /**
   * 선택한 가입 신청을 승인함
   *
   * @author Hanwon.Jang
   * @return 반환값이 없음
   */
  const handleApprove = (): void => {
    // 현재 답변을 확인한 가입 신청을 승인 상태로 처리함
    handleApplicationDecision("APPROVED");
  };

  /**
   * 선택한 가입 신청을 거절함
   *
   * @author Hanwon.Jang
   * @return 반환값이 없음
   */
  const handleReject = (): void => {
    // 현재 답변을 확인한 가입 신청을 거절 상태로 처리함
    handleApplicationDecision("REJECTED");
  };

  /**
   * 가입 신청 한 건의 프로필과 답변 확인 버튼을 표시함
   *
   * @author Hanwon.Jang
   * @param application 표시할 가입 신청
   * @return 가입 신청 카드
   */
  const renderApplication = (application: ClubApplication) => {
    /**
     * 현재 가입 신청의 답변 확인 모달을 엶
     *
     * @author Hanwon.Jang
     * @return 반환값이 없음
     */
    const handleOpen = (): void => {
      // 현재 카드의 가입 신청을 답변 확인 대상으로 설정함
      handleAnswerOpen(application);
    };

    // 신청자 프로필과 답변 확인 동작을 포함한 카드를 반환함
    return (
      <article className={styles.profileCard} key={application.applNumb}>
        {/* 가입 신청자 프로필 영역 */}
        <Link
          className={styles.profileSummary}
          to={`/social/profile/${application.userNumb}`}
          aria-label={message("frontend.profile.view", [application.userNick ?? "-"])}
        >
          <ProfileImage
            className={styles.avatar}
            src={application.porfPath}
            alt={application.userNick ?? ""}
          />
          <strong className={styles.profileName}>{application.userNick ?? "-"}</strong>
        </Link>

        {/* 가입 신청 답변 확인 영역 */}
        <button className={styles.answerButton} type="button" onClick={handleOpen}>
          {/* "답변 보기" */}
          {message("frontend.readingClub.memberManage.viewAnswer")}
          <svg width="12" height="12" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M4.45508 9.96005L7.71506 6.70005C8.10006 6.31505 8.10006 5.68505 7.71506 5.30005L4.45508 2.04004" stroke="#2F8F64" strokeWidth="1.5" strokeMiterlimit="10" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
        </button>
      </article>
    );
  };

  /**
   * 활성 모임원 한 명의 프로필과 관리 진입 버튼을 표시함
   *
   * @author Hanwon.Jang
   * @param member 표시할 모임원
   * @return 모임원 카드
   */
  const renderMember = (member: ClubMemberProfile) => {
    /**
     * 현재 카드의 일반 멤버를 퇴장 대상으로 선택함
     *
     * @author HanWon.Jang
     * @return 반환값이 없음
     */
    const handleOpenExit = (): void => {
      // 현재 카드의 사용자 번호와 닉네임을 퇴장 모달에 전달함
      handleExitOpen(member);
    };

    // 모임장과 일반 멤버를 구분한 프로필 카드를 반환함
    return (
      <article className={styles.profileCard} key={member.userNumb}>
        {/* 활성 모임원 프로필 영역 */}
        <Link
          className={styles.profileSummary}
          to={member.mineYsno === "Y" ? "/mypage/profile" : `/social/profile/${member.userNumb}`}
          aria-label={message("frontend.profile.view", [member.userNick ?? "-"])}
        >
          <ProfileImage
            className={styles.avatar}
            src={member.porfPath}
            alt={member.userNick ?? ""}
          />
          <strong className={styles.profileName}>{member.userNick ?? "-"}</strong>
        </Link>

        {/* 일반 멤버 퇴장 관리 진입 영역 */}
        {member.membRole !== "OWNER" ? (
          <ActionButton
            className={styles.exitButton}
            variant="danger"
            size="sm"
            disabled={isSubmitting}
            onClick={handleOpenExit}
          >
            {message("frontend.readingClub.memberManage.exitAction")}
          </ActionButton>
        ) : null}
      </article>
    );
  };

  /**
   * 신청 답변의 질문과 답변 한 쌍을 표시함
   *
   * @author Hanwon.Jang
   * @param question 표시할 가입 질문
   * @param index 질문 순서
   * @return 질문과 답변 항목
   */
  const renderAnswer = (question: string, index: number) => {
    // 같은 순서의 질문과 신청 답변을 한 항목으로 반환함
    return (
      <div className={styles.answerItem} key={`${question}-${index}`}>
        <strong className={styles.questionText}>{message("frontend.readingClub.detail.question", [index+1, question])}</strong>
        <p className={styles.answerText}>
          {message("frontend.readingClub.detail.answer", [selectedApplication?.answerList[index] ?? "-"])}
        </p>
      </div>
    );
  };

  /**
   * 맞팔 초대 후보 한 명의 선택 항목을 표시함
   *
   * @author Hanwon.Jang
   * @param candidate 표시할 초대 후보
   * @return 초대 후보 선택 항목
   */
  const renderCandidate = (candidate: InviteCandidate) => {
    // 현재 처리 상태를 전달한 피그마 초대 후보 카드를 반환함
    return (
      <CandidateRow
        key={candidate.userNumb}
        candidate={candidate}
        disabled={isSubmitting}
        onInvite={handleInviteSubmit}
      />
    );
  };

  /**
   * 모임장이 보낸 유효한 초대 한 건과 취소 버튼을 표시함
   *
   * @author Hanwon.Jang
   * @param invitation 표시할 보낸 초대
   * @return 보낸 초대 카드
   */
  const renderSentInvitation = (invitation: SentClubInvitation) => {
    /**
     * 현재 회원에게 보낸 초대를 취소함
     *
     * @author Hanwon.Jang
     * @return 반환값이 없음
     */
    const handleCancel = (): void => {
      // 현재 카드의 초대 대상 사용자 번호로 취소를 요청함
      handleInviteCancel(invitation.userNumb);
    };

    // 프로필과 관심분야 및 취소 기능을 포함한 보낸 초대 카드를 반환함
    return (
      <article className={styles.invitationCard} key={invitation.userNumb}>
        <ProfileImage
          className={styles.avatar}
          src={invitation.porfPath}
          alt={invitation.userNick ?? ""}
        />
        <span className={styles.candidateInfo}>
          <strong className={styles.profileName}>{invitation.userNick ?? "-"}</strong>
          {invitation.intrText ? (
            <small className={styles.invitationInterest}>{invitation.intrText}</small>
          ) : null}
        </span>
        <ActionButton
          className={styles.cancelInviteButton}
          variant="secondary"
          size="sm"
          disabled={isSubmitting}
          onClick={handleCancel}
        >
          {message("frontend.readingClub.memberManage.cancelInvite")}
        </ActionButton>
      </article>
    );
  };

  // 최초 조회 중에는 실제 카드 크기를 유지하는 스켈레톤 화면을 반환함
  if (isLoading) {
    return (
      <main className={styles.page} aria-label={message("frontend.readingClub.memberManage.loading")}>
        {/* 가입 신청 로딩 영역 */}
        <section className={styles.section}>
          <Skeleton width={92} height={20} borderRadius={6} />
          <div className={styles.cardList}>
            <Skeleton width="100%" height={74} borderRadius={22} />
            <Skeleton width="100%" height={74} borderRadius={22} />
          </div>
        </section>
        {/* 보낸 초대 목록 로딩 영역 */}
        <section className={styles.section}>
          <Skeleton width={92} height={20} borderRadius={6} />
          <div className={styles.cardList}>
            <Skeleton width="100%" height={74} borderRadius={22} />
          </div>
        </section>
        {/* 멤버 목록 로딩 영역 */}
        <section className={styles.section}>
          <Skeleton width={84} height={20} borderRadius={6} />
          <div className={styles.cardList}>
            <Skeleton width="100%" height={74} borderRadius={22} />
            <Skeleton width="100%" height={74} borderRadius={22} />
          </div>
        </section>
      </main>
    );
  }

  // 가입 신청과 멤버 목록 및 관리 진입 메뉴를 포함한 화면을 반환함
  return (
    <>
      <main className={styles.page}>
        {/* 가입 신청 목록 영역 */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>
            {/* "가입 신청 ({0})" */}
            {message("frontend.readingClub.memberManage.applications", [applications.length])}
          </h2>
          {applications.length ? (
            <div className={styles.cardList}>{applications.map(renderApplication)}</div>
          ) : (
            <p className={styles.emptyText}>
              {/* "대기 중인 가입 신청이 없어요." */}
              {message("frontend.readingClub.detail.noApplications")}
            </p>
          )}
        </section>

        {/* 모임장이 활성 회원에게 보낸 유효한 초대 목록 영역 */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>
            {message("frontend.readingClub.memberManage.sentInvitations", [sentInvitations.length])}
          </h2>
          {sentInvitations.length ? (
            <div className={styles.cardList}>{sentInvitations.map(renderSentInvitation)}</div>
          ) : (
            <p className={styles.emptyText}>
              {message("frontend.readingClub.memberManage.noSentInvitations")}
            </p>
          )}
        </section>

        {/* 활성 멤버 목록 영역 */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>
            {/* "멤버 ({0}/{1})" */}
            {message("frontend.readingClub.memberManage.members", [members.length, club?.maxxMemb ?? 0])}
          </h2>
          {members.length ? (
            <div className={styles.cardList}>{members.map(renderMember)}</div>
          ) : (
            <p className={styles.emptyText}>
              {/* "표시할 활성 멤버가 없어요." */}
              {message("frontend.readingClub.memberManage.noMembers")}
            </p>
          )}
        </section>

        {/* 회원 초대와 퇴장 내역 관리 메뉴 영역 */}
        <nav className={styles.managementMenu} aria-label={message("frontend.readingClub.memberManage.menuLabel")}>
          <button className={styles.menuButton} type="button" onClick={handleInviteOpen}>
            <span className={styles.menuText}>
              <strong className={styles.menuTitle}>
                {/* "회원 초대하기" */}
                {message("frontend.readingClub.memberManage.invite")}
              </strong>
              <small className={styles.menuDescription}>
                {/* "맞팔로우 회원만 초대할 수 있어요" */}
                {message("frontend.readingClub.memberManage.inviteDescription")}
              </small>
            </span>
            <img className={styles.menuChevron} src="/img/icons/icon-chevron-right.svg" alt="" />
          </button>
          <Link className={styles.restrictionButton} to={`/reading-clubs/manage/member-restrictions/${clubNumb}`}>
            <strong className={styles.restrictionTitle}>
              {/* "퇴장 내역 및 제한" */}
              {message("frontend.readingClub.memberManage.restrictions")}
            </strong>
            <img className={styles.menuChevron} src="/img/icons/icon-chevron-right.svg" alt="" />
          </Link>
        </nav>
      </main>

      {/* 가입 신청 답변 확인 모달 영역 */}
      {selectedApplication ? createPortal(
        <div className={styles.overlay} role="presentation">
          {/* 가입 신청 답변 모달 본문 영역 */}
          <section className={styles.modal} role="dialog" aria-modal="true" aria-labelledby="application-answer-title">
            {/* 가입 신청 답변 모달 헤더 영역 */}
            <div className={styles.modalHeader}>
              <h2 className={styles.modalTitle} id="application-answer-title">
                {message("frontend.readingClub.memberManage.answerTitle", [selectedApplication.userNick ?? "-"])}
              </h2>
              <button
                className={styles.closeButton}
                type="button"
                aria-label={message("frontend.common.close")}
                onClick={handleAnswerClose}
              >
                <img className={styles.closeIcon} src="/img/icons/icon-close.svg" alt="" />
              </button>
            </div>
            {/* 가입 질문과 답변 목록 영역 */}
            <div className={styles.answerList}>{selectedApplication.questionList.map(renderAnswer)}</div>
            {/* 가입 신청 승인과 거절 버튼 영역 */}
            <div className={styles.modalActions}>
              <ActionButton width="half" disabled={isSubmitting} onClick={handleApprove}>
                {/* "승인" */}
                {message("frontend.readingClub.detail.approve")}
              </ActionButton>
              <ActionButton variant="danger" width="half" disabled={isSubmitting} onClick={handleReject}>
                {/* "거절" */}
                {message("frontend.readingClub.detail.reject")}
              </ActionButton>
            </div>
          </section>
        </div>,
        document.body,
      ) : null}

      {/* 일반 모임원 강제 퇴장 사유 입력 모달 */}
      {selectedMember ? createPortal(
        <div className={styles.overlay} role="presentation">
          <section className={styles.modal} role="dialog" aria-modal="true" aria-labelledby="member-exit-title">
            <div className={styles.modalHeader}>
              <h2 className={styles.modalTitle} id="member-exit-title">
                {message("frontend.readingClub.memberManage.exitTitle", [selectedMember.userNick ?? "-"])}
              </h2>
              <button
                className={styles.closeButton}
                type="button"
                aria-label={message("frontend.common.close")}
                disabled={isSubmitting}
                onClick={handleExitClose}
              >
                <img className={styles.closeIcon} src="/img/icons/icon-close.svg" alt="" />
              </button>
            </div>
            <p className={styles.exitDescription}>
              {message("frontend.readingClub.memberManage.exitDescription")}
            </p>
            <label className={styles.exitReasonLabel} htmlFor="member-exit-reason">
              {message("frontend.readingClub.memberManage.exitReason")}
            </label>
            <textarea
              className={styles.exitReasonInput}
              id="member-exit-reason"
              value={exitReason}
              maxLength={1000}
              placeholder={message("frontend.readingClub.memberManage.exitReasonPlaceholder")}
              autoFocus
              disabled={isSubmitting}
              onChange={handleExitReasonChange}
            />
            <div className={styles.modalActions}>
              <ActionButton width="half" variant="secondary" disabled={isSubmitting} onClick={handleExitClose}>
                {message("frontend.common.cancel")}
              </ActionButton>
              <ActionButton
                width="half"
                variant="danger"
                disabled={isSubmitting || !exitReason.trim()}
                onClick={handleMemberExit}
              >
                {message("frontend.readingClub.memberManage.exitConfirm")}
              </ActionButton>
            </div>
          </section>
        </div>,
        document.body,
      ) : null}

      {/* 맞팔 회원 초대 선택 모달 영역 */}
      {isInviteOpen ? createPortal(
        <div className={styles.inviteOverlay} role="presentation">
          {/* 맞팔 회원 초대 모달 본문 영역 */}
          <section className={styles.inviteModal} role="dialog" aria-modal="true" aria-labelledby="invite-member-title">
            {/* 맞팔 회원 초대 모달 헤더 영역 */}
            <div className={styles.modalHeader}>
              <h2 className={styles.inviteModalTitle} id="invite-member-title">
                {/* "회원 초대하기" */}
                {message("frontend.readingClub.memberManage.invite")}
              </h2>
              <button
                className={styles.inviteCloseButton}
                type="button"
                aria-label={message("frontend.common.close")}
                onClick={handleInviteClose}
              >
                <img className={styles.closeIcon} src="/img/icons/icon-close.svg" alt="" />
              </button>
            </div>
            {/* 맞팔 초대 후보와 보낸 초대 목록 영역 */}
            {candidates.length || sentInvitations.length ? (
              <div className={styles.candidateList}>
                {candidates.map(renderCandidate)}
                {sentInvitations.map(renderSentInvitation)}
              </div>
            ) : (
              <p className={styles.emptyText}>
                {/* "초대할 수 있는 맞팔로워가 없어요." */}
                {message("frontend.readingClub.detail.noCandidates")}
              </p>
            )}
          </section>
        </div>,
        document.body,
      ) : null}
    </>
  );
};

export default ClubMemberManagementPage;
