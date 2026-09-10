/**
 * fileName       : useClubChatPage
 * author         : SeungHyeon.Kang
 * date           : 2026-09-04
 * description    : 활성 모임원의 채팅 조회와 전송 상태를 관리함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-09-04        SeungHyeon.Kang    최초 생성
 * 2026-09-10        HanWon.Jang        채팅 열람과 알림 읽음 동기화
 */
import {getApiErrorMessage} from "@/app/api/resultData";
import {sweetError} from "@/app/lib/sweetAlert/sweetAlert";
import {message} from "@/app/messages/message";
import {
  createClubChatApi,
  getClubChatListApi,
  getClubDtlApi,
  uptClubChatReadApi,
  type ClubChatMessage,
  type ReadingClub,
} from "@/features/ReadingClub/api/readingClubApi";
import {notifyUnreadAlimChange} from "@/features/Alim/lib/alimEvents";
import {useCallback, useEffect, useState} from "react";
import {useNavigate, useParams} from "react-router-dom";

// 새 채팅을 확인하는 간격
const CHAT_POLL_INTERVAL_MS = 3000;

/** 모임 채팅 화면의 조회와 전송 상태를 제공함. @author SeungHyeon.Kang */
export const useClubChatPage = () => {
  const navigate = useNavigate();
  const {clubNumb: clubNumbParam} = useParams<{clubNumb: string}>();
  const clubNumb = Number(clubNumbParam);
  const [club, setClub] = useState<ReadingClub | null>(null);
  const [messages, setMessages] = useState<ClubChatMessage[]>([]);
  const [content, setContent] = useState("");
  const [pendingContent, setPendingContent] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSending, setIsSending] = useState(false);

  /** 새 채팅과 변경된 안 읽은 수를 기존 목록에 병합함. @author SeungHyeon.Kang */
  const mergeMessages = useCallback((nextMessages: ClubChatMessage[]): void => {
    if (!nextMessages.length) {
      return;
    }

    setMessages((currentMessages) => {
      const messageMap = new Map<number, ClubChatMessage>();
      for (const chat of currentMessages) {
        messageMap.set(chat.chatNumb, chat);
      }

      for (const chat of nextMessages) {
        messageMap.set(chat.chatNumb, chat);
      }

      return Array.from(messageMap.values()).sort((left, right) => left.chatNumb - right.chatNumb);
    });
  }, []);

  // 모임별 화면 수명 안에서 열람 신호와 읽음 요청을 순서대로 처리
  useEffect(() => {
    if (!Number.isFinite(clubNumb) || clubNumb <= 0) {
      navigate("/reading-clubs/mine", {replace: true});
      return;
    }

    let active = true;
    let polling = false;
    let initialized = false;
    let queue = Promise.resolve();
    // 다른 탭과 재진입 화면의 종료 요청을 구분할 식별값
    const viewId = crypto.randomUUID();
    setIsLoading(true);
    setMessages([]);
    setClub(null);

    /** 늦게 끝난 읽음 요청 뒤에 화면 종료 신호를 전달하여 열람 상태 역전 방지 */
    const syncView = (viewing: boolean, chatNumb?: number): Promise<void> => {
      const operation = queue.then(async () => {
        // 화면을 나가거나 숨긴 뒤 대기 중인 읽음 요청 실행 차단
        if (viewing && (!active || document.visibilityState !== "visible")) {
          return;
        }
        const unreadCnt = await uptClubChatReadApi(clubNumb, chatNumb, viewId, viewing);
        // 이전 화면의 응답이 현재 화면의 알림 배지를 덮어쓰는 현상 방지
        if (active && document.visibilityState === "visible") {
          notifyUnreadAlimChange(unreadCnt);
        }
      });
      // 실패한 요청 때문에 이후 열람 해제와 재시도가 중단되는 현상 방지
      queue = operation.catch(() => undefined);
      return operation;
    };

    /** 표시 중인 화면만 채팅 조회 및 열람 유효 시간 갱신 */
    const loadMessages = async (): Promise<void> => {
      if (!active || polling || document.visibilityState !== "visible") {
        return;
      }
      polling = true;
      try {
        // 빈 채팅방도 메시지 도착 전에 열람 상태 등록
        if (!initialized) {
          await syncView(true).catch(() => undefined);
        }
        const [nextClub, nextMessages] = await Promise.all([
          initialized ? Promise.resolve(null) : getClubDtlApi(clubNumb),
          getClubChatListApi(clubNumb),
        ]);
        // 다른 모임으로 이동한 뒤 도착한 목록과 읽음 처리 제외
        if (!active || document.visibilityState !== "visible") {
          return;
        }
        if (nextClub) {
          setClub(nextClub);
        }
        mergeMessages(nextMessages);
        initialized = true;
        // 확인한 메시지까지 알림 읽음 처리와 배지 동기화
        await syncView(true, nextMessages.at(-1)?.chatNumb);
      } catch (error: unknown) {
        // 최초 조회 실패만 안내하고 주기 조회 실패는 다음 주기에 재시도
        if (active && !initialized) {
          void sweetError(
            message("frontend.readingClub.chat.loadErrorTitle"),
            getApiErrorMessage(error, message("frontend.common.tryAgain")),
          ).then(() => {
            if (active) {
              navigate(`/reading-clubs/${clubNumb}`, {replace: true});
            }
          });
        }
      } finally {
        polling = false;
        if (active && initialized) {
          setIsLoading(false);
        }
      }
    };

    void loadMessages();
    const pollTimer = window.setInterval(() => {
      void loadMessages();
    }, CHAT_POLL_INTERVAL_MS);

    /** 화면 표시 여부에 따라 열람 해제 또는 최신 채팅 재조회 */
    const handleVisibilityChange = (): void => {
      if (document.visibilityState === "visible") {
        void loadMessages();
      } else {
        void syncView(false).catch(() => undefined);
      }
    };
    /** 페이지 종료 시 현재 화면의 열람 상태 해제 */
    const releaseView = (): void => {
      void syncView(false).catch(() => undefined);
    };
    document.addEventListener("visibilitychange", handleVisibilityChange);
    window.addEventListener("pagehide", releaseView);

    return () => {
      active = false;
      window.clearInterval(pollTimer);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
      window.removeEventListener("pagehide", releaseView);
      releaseView();
    };
  }, [clubNumb, mergeMessages, navigate]);

  /** 입력한 채팅을 한 번만 전송함. @author SeungHyeon.Kang */
  const handleSend = async (): Promise<void> => {
    const normalizedContent = content.trim();
    if (!normalizedContent || isSending) {
      return;
    }

    setIsSending(true);
    setPendingContent(normalizedContent);
    setContent("");
    try {
      const savedMessage = await createClubChatApi(
        clubNumb,
        normalizedContent,
        crypto.randomUUID(),
      );
      setPendingContent(null);
      mergeMessages([savedMessage]);
    } catch (error) {
      setPendingContent(null);
      setContent(normalizedContent);
      void sweetError(
        message("frontend.readingClub.chat.sendErrorTitle"),
        getApiErrorMessage(error, message("frontend.common.tryAgain")),
      );
    } finally {
      setIsSending(false);
    }
  };

  return {
    club,
    content,
    isLoading,
    isSending,
    messages,
    pendingContent,
    handleSend,
    setContent,
  };
};
