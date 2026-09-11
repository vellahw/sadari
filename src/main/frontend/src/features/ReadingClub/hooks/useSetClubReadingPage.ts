/**
 * fileName       : useSetClubReadingPage
 * author         : Hanwon.Jang
 * date           : 2026-08-14
 * description    : 모임 독서 등록 및 수정 화면의 도서와 목표 기간 상태를 관리함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-08-14        Hanwon.Jang        최초 생성
 * 2026-08-20        Hanwon.Jang        현재 독서 수정 흐름 추가
 */

import { getApiErrorMessage } from "@/app/api/resultData";
import { sweetError, sweetWarning } from "@/app/lib/sweetAlert/sweetAlert";
import { message } from "@/app/messages/message";
import { runBlockingOperation } from "@/app/navigation/blockingOperation";
import { useCompletedFormGuard } from "@/app/navigation/useCompletedFormGuard";
import { normalizeBookAuthor, stripHtmlTags } from "@/app/utils/htmlUtil";
import type { BookSearchResultType } from "@/features/Book/types/book.type";
import { getBookCoverImageSource } from "@/features/Book/utils/bookCoverImage";
import {
  createClubReadingApi,
  getClubDtlApi,
  updateClubReadingApi,
  type ClubReadingUpdateParams,
  type ReadingClub,
} from "@/features/ReadingClub/api/readingClubApi";
import type { CSSProperties, FormEvent } from "react";
import { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";

/**
 * 모임 상세 응답의 현재 도서를 검색 결과 화면 계약으로 변환함
 *
 * @author Hanwon.Jang
 * @param club 현재 독서 도서 정보를 포함한 모임 상세
 * @return 검색 결과 계약으로 변환한 현재 도서 또는 필수 정보가 없을 때 null
 */
function toBookSearchResult(club: ReadingClub): BookSearchResultType | null {

  // 수정 화면을 구성할 필수 도서 정보가 없으면 잘못된 상세 응답으로 처리함
  if (!club.currentBookTitl || !club.currentBookAthr || !club.currentBookPubl
      || !club.currentBookIsbn || !club.currentBookCvim) {
    // 필수 정보가 없는 현재 도서는 수정 화면에 전달하지 않음
    return null;
  }

  // 기존 도서를 공용 도서 요약 컴포넌트와 검색 흐름에서 사용할 형태로 반환함
  return {
    title: club.currentBookTitl,
    author: club.currentBookAthr,
    publisher: club.currentBookPubl,
    isbn: club.currentBookIsbn,
    image: club.currentBookCvim,
    description: club.currentBookDesc ?? "",
    pubdate: club.currentPublDate ?? "",
  };
}

/**
 * 저장 API에 전달할 도서와 목표 기간을 정규화함
 *
 * @author Hanwon.Jang
 * @param selectedBook 선택한 도서 검색 결과
 * @param startDate 목표 독서 시작일
 * @param endDate 목표 독서 종료일
 * @return 모임 독서 등록 및 수정 API 요청값
 */
function toReadingParams(
  selectedBook: BookSearchResultType,
  startDate: string,
  endDate: string,
): ClubReadingUpdateParams {

  // 외부 검색 결과의 HTML 표기를 제거하여 서버 저장 계약으로 변환함
  return {
    bookTitl: stripHtmlTags(selectedBook.title),
    bookAthr: normalizeBookAuthor(selectedBook.author),
    bookPubl: stripHtmlTags(selectedBook.publisher),
    bookIsbn: stripHtmlTags(selectedBook.isbn),
    langCode: selectedBook.langCode || "ko",
    bookCvim: selectedBook.image || "/img/common/no-image.png",
    bookDesc: stripHtmlTags(selectedBook.description)
      || message("frontend.readingClub.reading.noBookDescription"),
    publDate: stripHtmlTags(selectedBook.pubdate),
    goalStdt: startDate,
    goalEndt: endDate,
  };
}

/**
 * 모임 독서 등록 및 수정 화면의 상태와 이벤트 처리 함수를 제공함
 *
 * @author Hanwon.Jang
 * @return 모임 독서 등록 및 수정 화면 상태와 이벤트 처리 함수
 */
export function useSetClubReadingPage() {

  const location = useLocation();
  const navigate = useNavigate();
  const finishForm = useCompletedFormGuard();
  const {
    clubNumb: clubNumbParam,
    rondNumb: rondNumbParam,
  } = useParams<{ clubNumb: string; rondNumb?: string }>();
  const clubNumb = Number(clubNumbParam);
  const rondNumb = Number(rondNumbParam);
  const locationBook = (
    location.state as { book?: BookSearchResultType } | null
  )?.book;
  const isEditMode = rondNumbParam !== undefined;
  const hasValidClubNumb = Number.isSafeInteger(clubNumb) && clubNumb > 0;
  const hasValidRondNumb = Number.isSafeInteger(rondNumb) && rondNumb > 0;
  const [selectedBook, setSelectedBook] = useState<BookSearchResultType | undefined>(
    isEditMode ? undefined : locationBook,
  );
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isPending, setIsPending] = useState(false);
  const [bookChangeAllowed, setBookChangeAllowed] = useState(!isEditMode);
  const idempotencyKeyRef = useRef(
    globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(36).slice(2)}`,
  );
  const pageStyle = selectedBook
    ? ({
        "--book-bg-image": `url("${getBookCoverImageSource(selectedBook.image)}")`,
        "--book-bg-fade-height": "680px",
      } as CSSProperties)
    : undefined;

  useEffect(() => {

    // 잘못된 등록 또는 수정 URL은 API를 호출하지 않고 내 모임 목록으로 이동함
    if (!hasValidClubNumb || (isEditMode && !hasValidRondNumb)) {
      navigate("/reading-clubs/mine", { replace: true });
      // 잘못된 경로의 화면 초기화를 종료함
      return;
    }

    let isMounted = true;
    // 서버의 최신 모임장 권한과 현재 회차 및 도서 변경 가능 여부를 기준으로 화면을 초기화함
    void getClubDtlApi(clubNumb)
      .then((club) => {
        // 등록과 수정 화면 모두 서버가 반환한 최신 모임장 권한으로 접근을 제한함
        if (club.membRole !== "OWNER") {
          throw new Error(message("frontend.readingClub.reading.invalidManagement"));
        }
        if (!isMounted) {
          // 화면을 벗어난 뒤 도착한 상세 응답은 상태에 반영하지 않음
          return;
        }

        // 등록 화면은 권한 확인을 마친 뒤 검색에서 선택한 도서를 표시함
        if (!isEditMode) {
          setSelectedBook(locationBook);
          // 수정 화면 전용 현재 회차 초기화를 실행하지 않음
          return;
        }

        const currentBook = toBookSearchResult(club);
        if (club.currentRondNumb !== rondNumb || !currentBook) {
          throw new Error(message("frontend.readingClub.reading.invalidManagement"));
        }

        const canChangeBook = club.currentBookChangeAllowed === true;
        setBookChangeAllowed(canChangeBook);
        setSelectedBook(canChangeBook && locationBook ? locationBook : currentBook);
        setStartDate(club.currentGoalStdt?.slice(0, 10) ?? "");
        setEndDate(club.currentGoalEndt?.slice(0, 10) ?? "");
      })
      .catch((error: unknown) => {
        if (!isMounted) {
          // 화면을 벗어난 뒤 발생한 오류는 사용자에게 표시하지 않음
          return;
        }
        // "독서 관리 정보를 불러오지 못했어요."
        void sweetError(
          message("frontend.readingClub.reading.loadFailedTitle"),
          getApiErrorMessage(error, message("frontend.common.tryAgain")),
        ).then(() => navigate(`/reading-clubs/${clubNumb}`, { replace: true }));
      })
      .finally(() => {
        if (isMounted) {
          setIsLoading(false);
        }
      });

    // 화면을 벗어난 뒤 비동기 상세 응답이 상태를 변경하지 않도록 정리 함수를 반환함
    return () => {
      isMounted = false;
    };
  }, [clubNumb, hasValidClubNumb, hasValidRondNumb, isEditMode, locationBook, navigate, rondNumb]);

  /**
   * 목표 독서 시작일과 종료일을 화면 상태에 반영함
   *
   * @author Hanwon.Jang
   * @param nextStartDate 선택한 목표 독서 시작일
   * @param nextEndDate 선택한 목표 독서 종료일
   * @return 반환값이 없음
   */
  function handleRangeChange(nextStartDate: string, nextEndDate: string): void {

    setStartDate(nextStartDate);
    setEndDate(nextEndDate);
  }

  /**
   * 모임 도서 검색 결과에서 읽을 책을 다시 선택하도록 이동함
   *
   * @author Hanwon.Jang
   * @return 반환값이 없음
   */
  function handleBookChange(): void {

    if (!hasValidClubNumb) {
      navigate("/reading-clubs/mine", { replace: true });
      // 올바르지 않은 모임 번호의 검색 이동을 종료함
      return;
    }
    if (isEditMode && !bookChangeAllowed) {
      // "작성된 독후감이 있어 도서는 변경할 수 없어요. 독서 기간은 변경할 수 있어요."
      void sweetWarning(message("frontend.readingClub.reading.bookChangeLocked"));
      // 잠긴 회차의 도서 검색 이동을 종료함
      return;
    }
    navigate(`/reading-clubs/books/search/${clubNumb}`, {
      state: {
        keepSearchResult: true,
        clubReadingEditRondNumb: isEditMode ? rondNumb : undefined,
      },
    });
  }

  /**
   * 입력 중인 모임 독서 등록 또는 수정을 취소하고 모임 상세로 돌아감
   *
   * @author Hanwon.Jang
   * @return 반환값이 없음
   */
  function handleCancel(): void {

    navigate(hasValidClubNumb ? `/reading-clubs/${clubNumb}` : "/reading-clubs/mine");
  }

  /**
   * 선택 도서와 목표 기간을 서버에 전달해 모임 독서를 등록하거나 수정함
   *
   * @author Hanwon.Jang
   * @param event 모임 독서 등록 및 수정 폼 제출 이벤트
   * @return 저장 처리가 끝나면 완료되는 Promise
   */
  async function handleFormSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {

    event.preventDefault();
    if (!hasValidClubNumb || !selectedBook || (isEditMode && !hasValidRondNumb)) {
      // "책 정보가 없어요."
      await sweetWarning(
        message("frontend.readingClub.reading.bookMissingTitle"),
        message("frontend.readingClub.reading.bookMissingDescription"),
      );
      handleBookChange();
      // 도서 정보가 없는 폼 제출을 종료함
      return;
    }
    if (!startDate || !endDate) {
      // "목표 독서 기간을 선택해주세요."
      await sweetWarning(message("frontend.readingClub.reading.periodRequired"));
      // 목표 기간이 없는 폼 제출을 종료함
      return;
    }

    setIsPending(true);

    try {
      const params = toReadingParams(selectedBook, startDate, endDate);

      /**
       * 현재 화면 모드에 맞는 모임 독서 저장 API를 호출함
       *
       * @author SeungHyeon.Kang
       * @return 모임 독서 저장 완료 Promise
       * @throws 모임 독서 등록 또는 수정에 실패하면 발생함
       */
      const saveClubReading = async (): Promise<void> => {
        // 수정 화면이면 현재 회차의 도서 또는 기간을 변경함
        if (isEditMode) {
          await updateClubReadingApi(clubNumb, rondNumb, params);
          // 수정 요청이 끝났으므로 등록 API를 호출하지 않음
          return;
        }

        // 등록 화면이면 중복 방지 키와 함께 새 모임 독서를 생성함
        await createClubReadingApi(clubNumb, {
          ...params,
          idemKeyx: idempotencyKeyRef.current,
        });
      };

      // 저장 완료 후 처리 중 알림을 닫지 않고 성공 알림으로 전환함
      await runBlockingOperation(saveClubReading, {
        success: {
          // 수정은 "모임 독서를 수정했어요.", 등록은 "모임 독서가 등록됐어요."
          title: message(
            isEditMode
              ? "frontend.readingClub.reading.updatedTitle"
              : "frontend.readingClub.reading.savedTitle",
          ),
        },
      });
      // 완료된 독서 등록 또는 수정 폼이 뒤로가기로 다시 열리지 않도록 상세 화면으로 교체함
      finishForm(`/reading-clubs/${clubNumb}`);
    } catch (error) {
      // 수정은 "모임 독서를 수정하지 못했어요.", 등록은 "모임 독서를 등록하지 못했어요."
      await sweetError(
        message(
          isEditMode
            ? "frontend.readingClub.reading.updateFailedTitle"
            : "frontend.readingClub.reading.saveFailedTitle",
        ),
        getApiErrorMessage(error, message("frontend.common.tryAgain")),
      );
    } finally {
      setIsPending(false);
    }
  }

  // 모임 독서 등록 및 수정 화면에서 사용할 상태와 이벤트 처리 함수를 반환함
  return {
    selectedBook,
    startDate,
    endDate,
    isEditMode,
    isLoading,
    isPending,
    bookChangeAllowed,
    pageStyle,
    handleRangeChange,
    handleBookChange,
    handleCancel,
    handleFormSubmit,
  };
}
