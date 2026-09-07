package org.our.sadari.global.common.result;

import lombok.Getter;

/**
 * fileName       : ResultEnum
 * author         : SeungHyeon.Kang
 * date           : 2026-03-25
 * description    : 공통 처리에 사용하는 상수와 코드를 정의함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-03-25        SeungHyeon.Kang    최초 생성
 * 2026-08-13        SeungHyeon.Kang    탈퇴 후 유효 제재가 남은 계정 인증 코드 추가
 * 2026-08-16        SeungHyeon.Kang    도서 검색 요청 제한 코드 추가
 * 2026-08-20        SeungHyeon.Kang    타이머 목표시간 검증 코드 추가
 * 2026-08-20        SeungHyeon.Kang        모임 독서 변경 검증 코드 추가
 * 2026-08-22        SeungHyeon.Kang    중복 신고 검증 코드 추가
 * 2026-09-03        HanWon.Jang        응답 메시지 주석 정리
 * 2026-09-07        HanWon.Jang        독후감 번역 실패 코드 추가
 */
@Getter
public enum ResultEnum {
    // "저장되었어요."
    COMMON_SAVE_SUCCESS(2001, "common.alert.0001"),

    // "수정되었어요."
    COMMON_UPDATE_SUCCESS(2002, "common.alert.0002"),

    // "삭제되었어요."
    COMMON_DELETE_SUCCESS(2003, "common.alert.0003"),

    // "조회 결과가 없어요."
    COMMON_NO_DATA(2004, "common.alert.0004"),

    // "저장에 실패했어요.\n다시 시도해주세요."
    COMMON_SAVE_REJECTED(2005, "common.alert.0005"),

    // "수정에 실패했어요.\n다시 시도해주세요."
    COMMON_UPDATE_REJECTED(2006, "common.alert.0006"),

    // "삭제에 실패했어요.\n다시 시도해주세요."
    COMMON_DELETE_REJECTED(2007, "common.alert.0007"),

    // "검색에 실패했어요.\n다시 시도해주세요."
    COMMON_SEARCH_REJECTED(2008, "common.alert.0008"),

    // "요청값이 올바르지 않아요."
    COMMON_INVALID_REQUEST(2009, "common.alert.0009"),

    // "독후감 내용은 {0}byte 이하로 입력해주세요."
    COMMON_REPORT_CONTENT_TOO_LONG(2010, "common.alert.0010"),

    // "시작일은 종료일보다 늦을 수 없습니다."
    COMMON_REPORT_DATE_RANGE_INVALID(2011, "common.alert.0011"),

    // "다음 항목을 입력해주세요.\n{0}"
    COMMON_REPORT_REQUIRED_MISSING(2012, "common.alert.0012"),

    // "선택한 책 정보가 올바르지 않습니다. 다른 책을 선택해주세요."
    COMMON_REPORT_BOOK_INVALID(2013, "common.alert.0013"),

    // "데이터베이스에 연결할 수 없어요.\n잠시 후 다시 시도해주세요."
    COMMON_DB_CONNECTION_FAILED(2014, "common.alert.0014"),

    // "욕설이나 비속어는 사용할 수 없어요.\n감지된 단어: {0}"
    COMMON_BAD_WORD_INCLUDED(2015, "common.alert.0015"),

    // "이미 사용 중인 닉네임이에요."
    USER_NICK_DUPLICATED(2016, "user.alert.0001"),

    // "JPG 또는 PNG 형식의 10MB 이하 이미지 파일만 업로드할 수 있어요."
    COMMON_IMAGE_INVALID(2018, "common.alert.0018"),

    // "Firebase Web Push 설정이 누락되었어요.\n누락된 항목: {0}"
    PUSH_CONFIG_MISSING(2017, "push.alert.0001"),

    // "다른 탭이나 디바이스에서 먼저 수정했어요.\n최신 내용을 확인한 뒤 다시 수정해주세요."
    COMMON_EDIT_CONFLICT(2019, "common.alert.0019"),

    // "올바르지 않은 접근이에요.\n다시 시도해주세요."
    COMMON_ACCESS_REJECTED(2020, "common.alert.0020"),

    // "독서 타이머 세션을 찾을 수 없습니다."
    TIMER_SESSION_NOT_FOUND(2021, "timer.alert.0001"),

    // "변경할 수 없는 독서 타이머 상태입니다."
    TIMER_STATE_INVALID(2022, "timer.alert.0002"),

    // "읽는 중인 내 도서만 타이머에 연결할 수 있습니다."
    TIMER_BOOK_INVALID(2023, "timer.alert.0003"),

    // "타이머 알림은 1분 이상 8시간 이하로 설정할 수 있습니다."
    TIMER_TARGET_INVALID(2025, "timer.alert.0004"),

    // "검색 요청이 너무 많아요. 잠시 후 다시 시도해주세요."
    BOOK_SEARCH_RATE_LIMITED(2024, "book.alert.0001"),

    // "작성된 독후감이 있어 도서를 변경할 수 없어요."
    READING_CLUB_BOOK_CHANGE_REJECTED(2026, "readingClub.alert.0001"),

    // "동일한 대상은 다시 신고할 수 없어요."
    COMPLAINT_DUPLICATED(2027, "complaint.alert.0001"),

    // "번역 기능을 사용할 수 없어요.\n잠시 후 다시 시도해주세요."
    REPORT_TRANSLATION_FAILED(2028, "report.translation.failed"),

    // "이번 달 번역 사용량을 모두 사용했어요."
    REPORT_TRANSLATION_LIMITED(2029, "report.translation.limited"),

    // "번역할 수 없는 독후감이에요."
    REPORT_TRANSLATION_UNAVAILABLE(2030, "report.translation.unavailable"),

    // "인증에 실패했어요.\n다시 로그인 해주세요."
    AUTH_FAIL(1001, "auth.common.fail"),

    // "유효하지 않은 토큰이에요.\n다시 로그인 해주세요."
    TOKEN_INVALID(1002, "auth.token.invalid"),

    // "토큰이 만료되었어요.\n다시 로그인 해주세요."
    TOKEN_EXPIRED(1003, "auth.token.expired"),

    // "접근 권한이 없습니다."
    FORBIDDEN(1004, "auth.common.forbidden"),

    // "이용 정지가 남아 있어 이 카카오 계정으로 가입할 수 없어요."
    AUTH_WITHDRAWN_SUSPENDED(1005, "auth.withdrawn.suspended");

    // 공통 응답 결과 코드
    private final int code;
    // 다국어 응답 메시지 키
    private final String messageKey;

    // 결과 코드와 메시지 키를 Enum 항목에 연결함
    ResultEnum(int code, String messageKey) {

        this.code = code;
        this.messageKey = messageKey;
    }
}
