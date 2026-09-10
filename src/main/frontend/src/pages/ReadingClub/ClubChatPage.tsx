/**
 * fileName       : ClubChatPage
 * author         : SeungHyeon.Kang
 * date           : 2026-09-04
 * description    : 활성 모임원이 대화하는 모임 채팅 화면을 구성함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-09-04        SeungHyeon.Kang    최초 생성
 */
import {message} from "@/app/messages/message";
import {formatDateValue} from "@/app/utils/dateUtil";
import {ActionButton} from "@/components/Button/ActionButton";
import {useHeaderTitle} from "@/components/Layout/Header/useHeaderTitle";
import Loading from "@/components/Loading/Loading";
import Skeleton from "@/components/Skeleton/Skeleton";
import {
  getBookCoverImageSource,
  handleBookCoverImageError,
} from "@/features/Book/utils/bookCoverImage";
import {useClubChatPage} from "@/features/ReadingClub/hooks/useClubChatPage";
import {getReadingDeadline} from "@/features/ReadingClub/utils/readingClubDeadline";
import ProfileImage from "@/features/User/components/ProfileImage";
import {useBodyScrollLock} from "@/app/utils/modalUtil";
import type {ChangeEvent, FormEvent} from "react";
import {Fragment, useCallback, useEffect, useRef} from "react";
import {Link} from "react-router-dom";
import * as styles from "./ClubChatPage.css";

const CLUB_CHAT_INPUT_FOCUSED_ATTRIBUTE = "data-club-chat-input-focused";
const SOFT_KEYBOARD_MIN_HEIGHT = 120;

/**
 * 모임 채팅 입력 포커스 상태를 문서 루트에 표시함
 *
 * @author SeungHyeon.Kang
 * @param focused 채팅 입력 포커스 여부
 * @return 반환값이 없음
 */
const setChatFocusState = (focused: boolean): void => {
  document.documentElement.toggleAttribute(CLUB_CHAT_INPUT_FOCUSED_ATTRIBUTE, focused);
};

/** 채팅 입력 포커스 상태를 해제함. @author SeungHyeon.Kang */
const clearChatInputFocusState = (): void => {
  setChatFocusState(false);
};

/**
 * 채팅 화면 진입과 해제 시 이전 포커스 상태 정리
 *
 * @author SeungHyeon.Kang
 * @return 화면 해제 시 사용할 포커스 상태 정리 함수
 */
const registerChatFocusCleanup = (): (() => void) => {
  // 새 화면 진입과 개발 갱신 후 남을 수 있는 이전 숨김 상태 제거
  clearChatInputFocusState();

  // 채팅 화면 해제 시 사용할 동일 정리 함수 반환
  return clearChatInputFocusState;
};

const ClubChatPage = () => {
  const {
    club,
    content,
    isLoading,
    isSending,
    messages,
    pendingContent,
    handleSend,
    setContent,
  } = useClubChatPage();
  const messagePanelRef = useRef<HTMLElement | null>(null);
  const inputFocusedRef = useRef(false);
  const inputViewportRef = useRef<{height: number; width: number} | null>(null);

  // 조회된 모임명을 공통 헤더 제목으로 표시함
  useHeaderTitle(club?.clubName);

  // 채팅 화면에서는 메시지 목록만 움직이도록 배경 문서 스크롤을 잠금
  useBodyScrollLock(true);

  /**
   * 새 채팅이 표시되면 바깥 문서를 움직이지 않고 메시지 목록만 끝으로 이동함
   *
   * @author SeungHyeon.Kang
   * @return 반환값이 없음
   */
  const scrollMessageList = useCallback((): void => {
    const messagePanel = messagePanelRef.current;

    // 채팅 목록이 렌더링된 경우에만 내부 스크롤 위치를 최신 메시지로 이동함
    if (messagePanel) {
      // 바깥 페이지의 스크롤 위치를 유지하면서 채팅 목록 끝을 표시함
      messagePanel.scrollTop = messagePanel.scrollHeight;
    }
  }, []);

  /**
   * 채팅 입력 직전의 표시 영역을 소프트 키보드 판별 기준으로 저장
   *
   * @author SeungHyeon.Kang
   * @return 반환값 없음
   */
  const handleChatInputFocus = (): void => {
    const visualViewport = window.visualViewport;

    // 입력 포커스 중 표시 영역 변경만 키보드 후보로 처리하기 위한 상태 기록
    inputFocusedRef.current = true;
    // 키보드가 열리기 전 표시 영역과 이후 영역의 차이 계산을 위한 기준 저장
    inputViewportRef.current = visualViewport
      ? {height: visualViewport.height, width: visualViewport.width}
      : null;
    // PC 입력 포커스만으로 남아 있던 네비게이션 숨김 상태 제거
    clearChatInputFocusState();
  };

  /**
   * 채팅 입력 종료 후 키보드 판별 기준과 네비게이션 상태 정리
   *
   * @author SeungHyeon.Kang
   * @return 반환값 없음
   */
  const handleChatInputBlur = (): void => {
    // 포커스 해제 후 표시 영역 변경을 키보드 열림으로 오인하지 않기 위한 상태 해제
    inputFocusedRef.current = false;
    // 다음 입력 시점에 새 기준을 사용하기 위한 이전 표시 영역 제거
    inputViewportRef.current = null;
    // 키보드 종료와 함께 공통 네비게이션 표시 복원
    clearChatInputFocusState();
  };

  /**
   * 입력 전후 표시 영역 높이 차이로 소프트 키보드 열림 상태 반영
   *
   * @author SeungHyeon.Kang
   * @return 반환값 없음
   */
  const handleViewportResize = useCallback((): void => {
    const visualViewport = window.visualViewport;
    const inputViewport = inputViewportRef.current;

    // 키보드 애니메이션 중 최신 메시지 위치 유지
    scrollMessageList();

    // 입력 포커스와 비교 기준이 모두 있어야 소프트 키보드 상태 판별
    if (!visualViewport || !inputFocusedRef.current || !inputViewport) {
      // PC 창 조정과 포커스 해제 상태에서 네비게이션 표시 유지
      clearChatInputFocusState();
      // 키보드 판별 대상이 아닌 표시 영역 변경 처리 종료
      return;
    }

    // 화면 회전과 가로 창 크기 변경을 키보드 열림으로 오인하지 않기 위한 기준 갱신
    if (Math.abs(visualViewport.width - inputViewport.width) > 1) {
      // 변경된 화면 방향을 다음 높이 비교의 기준으로 저장
      inputViewportRef.current = {height: visualViewport.height, width: visualViewport.width};
      // 화면 방향 변경 중 공통 네비게이션 표시 유지
      clearChatInputFocusState();
      // 새 화면 방향 기준 저장 후 현재 변경 처리 종료
      return;
    }

    const keyboardHeight = inputViewport.height - visualViewport.height;

    // 브라우저 도구막대 변화를 제외한 소프트 키보드 높이 감소만 숨김 상태에 반영
    setChatFocusState(keyboardHeight >= SOFT_KEYBOARD_MIN_HEIGHT);
  }, [scrollMessageList]);

  // 새 채팅이 표시되면 최신 메시지가 보이도록 목록 끝으로 이동함
  useEffect(scrollMessageList, [messages, pendingContent, scrollMessageList]);

  /**
   * 모바일 키보드가 채팅 표시 영역을 바꾸는 동안 최신 메시지 위치를 유지함
   *
   * @author SeungHyeon.Kang
   * @return 표시 영역 변경 감지 해제 함수 또는 미지원 환경의 빈 값
   */
  const watchViewportResize = (): (() => void) | undefined => {
    const visualViewport = window.visualViewport;

    // 실제 표시 영역 API가 없는 브라우저는 CSS 대체 높이만 사용함
    if (!visualViewport) {
      // 등록할 표시 영역 이벤트가 없음을 반환함
      return undefined;
    }

    // 키보드 애니메이션 중 표시 영역 높이와 최신 메시지 위치를 함께 갱신함
    visualViewport.addEventListener("resize", handleViewportResize);

    /**
     * 채팅 화면이 닫히면 실제 표시 영역 변경 감지를 해제함
     *
     * @author SeungHyeon.Kang
     * @return 반환값이 없음
     */
    const stopViewportWatch = (): void => {
      // 다른 화면에서 채팅 스크롤이 실행되지 않도록 이벤트를 해제함
      visualViewport.removeEventListener("resize", handleViewportResize);
    };

    // 채팅 화면 해제 시 사용할 표시 영역 이벤트 정리 함수를 반환함
    return stopViewportWatch;
  };

  // 모바일 키보드가 열리고 닫힐 때 채팅 목록의 끝 위치를 유지함
  useEffect(watchViewportResize, [handleViewportResize]);

  // 다른 화면으로 이동한 뒤 네비게이션 숨김 상태가 남지 않도록 정리함
  useEffect(registerChatFocusCleanup, []);

  /** 채팅 입력값을 상태에 반영함. @author SeungHyeon.Kang */
  const handleContentChange = (event: ChangeEvent<HTMLTextAreaElement>): void => {
    setContent(event.currentTarget.value);
  };

  /** 폼 제출 시 현재 채팅을 전송함. @author SeungHyeon.Kang */
  const handleSubmit = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault();
    void handleSend();
  };

  if (isLoading || !club) {
    return (
      <main className={styles.page} aria-busy="true">
        <Skeleton width="100%" height={68} borderRadius={18}/>
        <Skeleton width="100%" height={360} borderRadius={18}/>
      </main>
    );
  }

  const readingDeadline = getReadingDeadline(club.currentGoalEndt);
  const hasMessages = messages.length > 0 || pendingContent !== null;
  const todayValue = formatDateValue(new Date());
  const todayMessageIndex = messages.findIndex((chat) => chat.regiDate.slice(0, 10) === todayValue);

  return (
    <main className={styles.page}>
      {/* 현재 모임에서 읽는 책 요약 영역 */}
      {club.currentBookTitl ? (
        <Link className={styles.currentBookCard} to={`/reading-clubs/${club.clubNumb}`}>
          <img
            className={styles.currentBookImage}
            src={getBookCoverImageSource(club.currentBookCvim)}
            onError={handleBookCoverImageError}
            alt={club.currentBookTitl}
          />
          <span className={styles.currentBookInformation}>
            <span className={styles.currentBookLabel}>
              {/* "현재 읽는 책" */}
              {message("frontend.readingClub.chat.currentBook")}
            </span>
            <strong className={styles.currentBookTitle}>
              {club.currentBookTitl}
              {readingDeadline ? ` · ${readingDeadline.label}` : null}
            </strong>
          </span>
          <span className={styles.currentBookArrow} aria-hidden="true">›</span>
        </Link>
      ) : null}

      {/* 모임 채팅 메시지 목록 영역 */}
      <section
        ref={messagePanelRef}
        className={styles.messagePanel}
        aria-label={message("frontend.readingClub.chat.messageList")}
      >
        {hasMessages ? (
          <ol className={styles.messageList} aria-live="polite">
            {messages.map((chat, index) => (
              <Fragment key={chat.chatNumb}>
                {index === todayMessageIndex ? (
                  <li className={styles.dateDivider}>
                    {/* "오늘" */}
                    {message("frontend.readingClub.common.deadline.today")}
                  </li>
                ) : null}
                <li className={chat.mineYsno === "Y" ? styles.myMessageRow : styles.messageRow}>
                  {chat.mineYsno === "N" ? (
                    <ProfileImage
                      className={styles.avatar}
                      src={chat.porfPath}
                      alt={chat.userNick ?? message("frontend.readingClub.chat.anonymous")}
                    />
                  ) : null}
                  <div className={chat.mineYsno === "Y" ? styles.myMessageContent : styles.messageContent}>
                    {chat.mineYsno === "N" ? (
                      <span className={styles.sender}>
                        {chat.userNick ?? message("frontend.readingClub.chat.anonymous")}
                      </span>
                    ) : null}
                    <div className={chat.mineYsno === "Y" ? styles.myMessageLine : styles.messageLine}>
                      <div className={chat.mineYsno === "Y" ? styles.myBubble : styles.bubble}>
                        {chat.chatCntn}
                      </div>
                      <span className={chat.mineYsno === "Y" ? styles.myMessageMeta : styles.messageMeta}>
                        {chat.unreadCnt > 0 ? (
                          <>
                            {/* "{0}명 안 읽음" */}
                            <span
                              className={styles.unreadCount}
                              aria-label={message("frontend.readingClub.chat.unreadCount", [chat.unreadCnt])}
                            >
                              {chat.unreadCnt}
                            </span>
                          </>
                        ) : null}
                        <time className={styles.time} dateTime={chat.regiDate}>
                          {chat.regiDate.slice(11, 16)}
                        </time>
                      </span>
                    </div>
                  </div>
                </li>
              </Fragment>
            ))}
            {pendingContent ? (
              <>
                {todayMessageIndex === -1 ? (
                  <li className={styles.dateDivider}>
                    {/* "오늘" */}
                    {message("frontend.readingClub.common.deadline.today")}
                  </li>
                ) : null}
                <li className={styles.myMessageRow}>
                  <div className={styles.myMessageContent}>
                    <div className={styles.myMessageLine}>
                      <div className={styles.myBubble}>{pendingContent}</div>
                      {/* "전송 중" */}
                      <Loading title={message("frontend.readingClub.chat.sending")} isCompact isInline />
                    </div>
                  </div>
                </li>
              </>
            ) : null}
          </ol>
        ) : (
          <p className={styles.empty}>
            {/* "아직 채팅 메시지가 없어요." */}
            {message("frontend.readingClub.chat.empty")}
          </p>
        )}
      </section>

      {/* 새 채팅 메시지 입력과 전송 영역 */}
      <form className={styles.composer} onSubmit={handleSubmit}>
        <label className={styles.srOnly} htmlFor="club-chat-content">
          {/* "메시지를 입력해 주세요." */}
          {message("frontend.readingClub.chat.placeholder")}
        </label>
        <textarea
          className={styles.input}
          id="club-chat-content"
          value={content}
          maxLength={2000}
          rows={1}
          placeholder={message("frontend.readingClub.chat.placeholder")}
          disabled={isSending}
          onChange={handleContentChange}
          onFocus={handleChatInputFocus}
          onBlur={handleChatInputBlur}
        />
        {/* "보내기" */}
        <ActionButton
          className={styles.sendButton}
          type="submit"
          aria-label={message("frontend.readingClub.chat.send")}
          disabled={isSending || !content.trim()}
          icon={<span className={styles.sendIcon}>↑</span>}
        />
      </form>
    </main>
  );
};

export default ClubChatPage;
