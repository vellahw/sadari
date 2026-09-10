package org.our.sadari.readingClub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.our.sadari.global.common.result.ResultData;
import org.our.sadari.readingClub.dto.ReadingClubDto;
import org.our.sadari.readingClub.service.ReadingClubService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * fileName       : ReadingClubController
 * author         : SeungHyeon.Kang
 * date           : 2026-08-05
 * description    : 독서 모임 생성, 탐색, 가입, 초대와 승인 API를 제공함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-08-05        SeungHyeon.Kang    최초 생성
 * 2026-08-14        Hanwon.Jang        모임원·수정·독서 API 추가
 * 2026-08-20        Hanwon.Jang        현재 독서 수정 API 추가
 * 2026-08-22        HanWon.Jang        종료 결과·독후감 조회 API
 * 2026-08-23        HanWon.Jang        이전 독서 기록·회차 결과 조회 API
 * 2026-08-24        HanWon.Jang        가입 신청 취소·모임원 퇴장 API 추가
 * 2026-08-29        HanWon.Jang        진행 회차 독후감 조회 API 확장
 * 2026-08-31        HanWon.Jang        독서 조기 마감·결과 확인 API 추가
 * 2026-09-01        HanWon.Jang        공개 모임 요약·자진 탈퇴 API 확장
 * 2026-09-04        SeungHyeon.Kang    모임 채팅 읽음 수·강제 퇴장 API 추가
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reading-clubs")
@Tag(name = "독서 모임", description = "독서 모임 1차 기능 API")
public class ReadingClubController {

    // 독서 모임 생성과 참여 업무 서비스
    private final ReadingClubService readingClubService;

    /** 다음 도서 추천 목록을 조회함. @author HanWon.Jang @param userNumb 사용자 번호 @param clubNumb 모임 번호 @return 추천 목록 */
    @GetMapping("/{clubNumb}/book-recommendations")
    @Operation(summary = "다음 도서 추천 조회")
    public ResultData getBookRecommendationList(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                                , @PathVariable Long clubNumb) {
        // 활성 모임원에게 공개할 다음 도서 추천 목록을 반환함
        return readingClubService.getBookRecommendationList(userNumb, clubNumb);
    }

    /** 다음 도서를 추천함. @author HanWon.Jang @param userNumb 사용자 번호 @param clubNumb 모임 번호 @param request 추천 도서 @return 등록 결과 */
    @PostMapping("/{clubNumb}/book-recommendations")
    @Operation(summary = "다음 도서 추천 등록")
    public ResultData setBookRecommendation(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                            , @PathVariable Long clubNumb
                                            , @Valid @RequestBody ReadingClubDto.BookRecommendationDto request) {
        // 활성 모임원의 중복되지 않은 도서 추천 결과를 반환함
        return readingClubService.setBookRecommendation(userNumb, clubNumb, request);
    }

    /** 본인의 다음 도서 추천을 삭제함. @author HanWon.Jang @param userNumb 사용자 번호 @param clubNumb 모임 번호 @param recmNumb 추천 번호 @return 삭제 결과 */
    @DeleteMapping("/{clubNumb}/book-recommendations/{recmNumb}")
    @Operation(summary = "다음 도서 추천 삭제")
    public ResultData delBookRecommendation(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                            , @PathVariable Long clubNumb, @PathVariable Long recmNumb) {
        // 서버에서 소유권을 검증한 추천 삭제 결과를 반환함
        return readingClubService.delBookRecommendation(userNumb, clubNumb, recmNumb);
    }

    /** 다음 도서 투표
     *  @author HanWon.Jang
     *  @param userNumb 사용자 번호
     *  @param clubNumb 모임 번호
     *  @param request 투표 대상
     *  @return 투표 결과
     *  */
    @PutMapping("/{clubNumb}/book-vote")
    @Operation(summary = "다음 도서 투표")
    public ResultData uptBookVote(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                 , @PathVariable Long clubNumb
                                 , @Valid @RequestBody ReadingClubDto.BookVoteReqDto request) {
        // 한 사용자당 하나로 갱신되는 투표 결과를 반환함
        return readingClubService.uptBookVote(userNumb, clubNumb, request);
    }

    /**
     * 모임 독서 회차와 활성 멤버별 읽는 중 독후감을 하나의 요청으로 등록함
     *
     * @author Hanwon.Jang
     * @param userNumb 등록을 요청한 모임장 사용자 번호
     * @param clubNumb 모임 번호
     * @param request 선택 도서와 목표 독서 기간
     * @return 생성된 회차 번호
     */
    @PostMapping("/{clubNumb}/setBook")
    @Operation(summary = "모임 독서 등록")
    public ResultData setReading(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                , @PathVariable Long clubNumb
                                , @Valid @RequestBody ReadingClubDto.ReadingCreateReqDto request) {
        // 회차와 활성 멤버별 독후감을 같은 트랜잭션으로 생성한 결과를 반환함
        return readingClubService.setReading(userNumb, clubNumb, request);
    }

    /**
     * 현재 모임 독서의 도서와 목표 기간을 수정
     *
     * @author Hanwon.Jang
     * @param userNumb 수정을 요청한 모임장 사용자 번호
     * @param clubNumb 모임 번호
     * @param rondNumb 수정할 회차 번호
     * @param request 수정할 도서와 목표 기간
     * @return 수정된 회차 번호
     */
    @PutMapping("/{clubNumb}/{rondNumb}/updateClub")
    @Operation(summary = "현재 모임 독서 수정")
    public ResultData uptReading(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                , @PathVariable Long clubNumb, @PathVariable Long rondNumb
                                , @Valid @RequestBody ReadingClubDto.ReadingUpdateReqDto request) {
        // 모임장 권한과 연결 독후감 작성 여부를 적용한 수정 결과를 반환함
        return readingClubService.uptReading(userNumb, clubNumb, rondNumb, request);
    }

    /**
     * 활성 모임장이 전원 완독한 진행 회차를 목표 기간 안에 조기 마감함
     *
     * @author HanWon.Jang
     * @param userNumb 마감을 요청한 모임장 사용자 번호
     * @param clubNumb 모임 번호
     * @param rondNumb 마감할 회차 번호
     * @return 완료된 회차 번호
     */
    @PutMapping("/{clubNumb}/readings/{rondNumb}/completion")
    @Operation(summary = "모임 독서 회차 조기 마감")
    public ResultData uptReadingCompletion(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
          , @PathVariable Long clubNumb, @PathVariable Long rondNumb) {
        // 서버에서 모임장 권한과 전원 완독 상태를 다시 검증한 조기 마감 결과를 반환함
        return readingClubService.uptReadingCompletion(userNumb, clubNumb, rondNumb);
    }

    /**
     * 로그인 사용자가 활성 회원으로 참여 중인 독서 모임 목록을 조회함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @return 내 모임 목록 조회 결과
     */
    @GetMapping("/mine")
    @Operation(summary = "내 모임 조회")
    public ResultData getMyClubList(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb) {
        // 로그인 사용자의 활성 모임 목록을 반환함
        return readingClubService.getMyClubList(userNumb);
    }

    /**
     * 로그인 사용자의 관심분야와 검색어를 반영한 공개 모임 목록을 조회함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @param keyword 모임명과 소개 검색어
     * @return 공개 모임 목록 조회 결과
     */
    @GetMapping
    @Operation(summary = "공개 모임 찾기")
    public ResultData getFindClubList(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                    , @RequestParam(required = false) String keyword) {
        // 관심분야 일치와 검색어를 적용한 공개 모임을 반환함
        return readingClubService.getFindClubList(userNumb, keyword);
    }

    /**
     * 로그인 사용자의 참여 관계를 포함한 독서 모임 상세 정보를 조회함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @param clubNumb 모임 번호
     * @return 모임 상세 조회 결과
     */
    @GetMapping("/{clubNumb}")
    @Operation(summary = "모임 상세 조회")
    public ResultData getClubDtl(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                , @PathVariable Long clubNumb) {
        // 로그인 사용자 관점의 모임 상세를 반환함
        return readingClubService.getClubDtl(userNumb, clubNumb);
    }

    /**
     * 활성 모임원과 공개 중인 활성 모임 조회자에게 활성 모임원 프로필 목록을 제공함
     *
     * @author Hanwon.Jang
     * @param userNumb 조회를 요청한 사용자 번호
     * @param clubNumb 조회할 모임 번호
     * @return 모임원 프로필 목록 조회 결과
     */
    @GetMapping("/{clubNumb}/members")
    @Operation(summary = "모임원 프로필 목록 조회")
    public ResultData getClubMemberList(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                       , @PathVariable Long clubNumb) {
        // 같은 모임의 활성 모임원 프로필 목록을 반환함
        return readingClubService.getClubMemberList(userNumb, clubNumb);
    }

    /**
     * 현재 모임장이 다른 활성 일반 멤버를 퇴장시키고 재가입을 차단함
     *
     * @author HanWon.Jang
     * @param userNumb 모임장 사용자 번호
     * @param clubNumb 모임 번호
     * @param targetUserNumb 퇴장 대상 사용자 번호
     * @return 모임원 퇴장 결과
     */
    @DeleteMapping("/{clubNumb}/members/{targetUserNumb}")
    @Operation(summary = "모임원 강제 퇴장")
    public ResultData delMember(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                               , @PathVariable Long clubNumb, @PathVariable Long targetUserNumb
                               , @Valid @RequestBody ReadingClubDto.MemberKickReqDto request) {
        // 모임장 권한과 활성 멤버 상태를 검증한 퇴장 결과를 반환함
        return readingClubService.delMember(userNumb, clubNumb, targetUserNumb, request);
    }

    /** 활성 모임원의 채팅 목록을 조회함. @author SeungHyeon.Kang */
    @GetMapping("/{clubNumb}/chats")
    @Operation(summary = "모임 채팅 조회")
    public ResultData getClubChatList(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                     , @PathVariable Long clubNumb
                                     , @RequestParam(required = false) Long afterChatNumb) {
        // 활성 회원 권한을 검증한 채팅 목록을 반환함
        return readingClubService.getClubChatList(userNumb, clubNumb, afterChatNumb);
    }

    /**
     * 활성 모임원의 마지막 읽은 채팅 번호를 갱신함.
     * @author SeungHyeon.Kang
     * */
    @PatchMapping("/{clubNumb}/chats/read")
    @Operation(summary = "모임 채팅 읽음 처리")
    public ResultData uptClubChatRead(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                     , @PathVariable Long clubNumb
                                     , @Valid @RequestBody ReadingClubDto.ClubChatReadReqDto request) {
        // 활성 회원 권한과 채팅 소속을 검증한 읽음 처리 결과를 반환함
        return readingClubService.uptClubChatRead(userNumb, clubNumb, request);
    }

    /**
     * 활성 모임원이 채팅을 전송함.
     * @author SeungHyeon.Kang
     */
    @PostMapping("/{clubNumb}/chats")
    @Operation(summary = "모임 채팅 전송")
    public ResultData setClubChat(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                 , @PathVariable Long clubNumb
                                 , @Valid @RequestBody ReadingClubDto.ClubChatReqDto request) {
        // 중복 방지 키를 적용하여 저장한 채팅을 반환함
        return readingClubService.setClubChat(userNumb, clubNumb, request);
    }

    /** 모임장에게 퇴장 내역과 재가입 제한 상태를 제공함. @author HanWon.Jang @param userNumb 모임장 번호 @param clubNumb 모임 번호 @return 퇴장 내역 */
    @GetMapping("/{clubNumb}/members/exits")
    @Operation(summary = "모임원 퇴장 내역 조회")
    public ResultData getMemberExitList(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                       , @PathVariable Long clubNumb) {
        // 모임장 권한을 검증한 퇴장 내역을 반환함
        return readingClubService.getMemberExitList(userNumb, clubNumb);
    }

    /** 퇴장 회원의 제한 내역을 삭제함. @author HanWon.Jang @param userNumb 모임장 번호 @param clubNumb 모임 번호 @param targetUserNumb 대상 사용자 번호 @return 내역 삭제 결과 */
    @DeleteMapping("/{clubNumb}/members/{targetUserNumb}/restriction")
    @Operation(summary = "퇴장 모임원 재가입 제한 해제")
    public ResultData delMemberRestriction(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                          , @PathVariable Long clubNumb, @PathVariable Long targetUserNumb) {
        // 모임장 권한과 퇴장 및 제한 상태를 검증한 내역 삭제 결과를 반환함
        return readingClubService.delMemberRestriction(userNumb, clubNumb, targetUserNumb);
    }

    /**
     * 종료된 최신 독서 회차의 목표 결과 조회
     *
     * @author HanWon.Jang
     * @param userNumb 조회를 요청한 사용자 번호
     * @param clubNumb 조회할 모임 번호
     * @return 종료된 최신 독서 목표 결과
     */
    @GetMapping("/{clubNumb}/reading-result")
    @Operation(summary = "종료된 모임 독서 목표 결과 조회")
    public ResultData getReadingGoalResult(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                          , @PathVariable Long clubNumb) {
        // 현재 활성 모임원에게 공개할 수 있는 종료 회차 결과를 반환함
        return readingClubService.getReadingGoalResult(userNumb, clubNumb);
    }

    /**
     * 이전 독서 기록 페이지의 상세페이지 (독서 회차 목표 결과)
     *
     * @author HanWon.Jang
     * @param userNumb 조회를 요청한 사용자 번호
     * @param clubNumb 조회할 모임 번호
     * @param rondNumb 조회할 완료 회차 번호
     * @return 지정한 완료 독서 회차의 목표 결과
     */
    @GetMapping("/{clubNumb}/readings/{rondNumb}/result")
    @Operation(summary = "지정한 완료 모임 독서 목표 결과 조회")
    public ResultData getReadingGoalResult(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                         , @PathVariable Long clubNumb, @PathVariable Long rondNumb) {
        // 현재 활성 모임원에게 선택한 완료 회차의 목표 결과를 반환함
        return readingClubService.getReadingGoalResult(userNumb, clubNumb, rondNumb);
    }

    /**
     * 활성 모임원이 팝업에서 직접 닫은 독서 회차 결과를 확인 처리함
     *
     * @author HanWon.Jang
     * @param userNumb 확인한 사용자 번호
     * @param clubNumb 모임 번호
     * @param rondNumb 확인한 완료 회차 번호
     * @return 결과 확인 처리 결과
     */
    @PatchMapping("/{clubNumb}/readings/{rondNumb}/result-confirmation")
    @Operation(summary = "모임 독서 목표 결과 확인", description = "활성 모임원이 팝업의 닫기 명령으로 확인한 회차 결과를 저장한다.")
    public ResultData uptReadingResultConfirm(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
          , @PathVariable Long clubNumb, @PathVariable Long rondNumb) {
        // 활성 계정과 모임원 관계를 검증한 회차 결과 확인 처리 결과를 반환함
        return readingClubService.uptReadingResultConfirm(userNumb, clubNumb, rondNumb);
    }

    /**
     * 이전 독서 기록 리스트 조회
     *
     * @author HanWon.Jang
     * @param userNumb 조회를 요청한 사용자 번호
     * @param clubNumb 조회할 모임 번호
     * @param page 조회할 페이지 번호
     * @return 종료 회차 도서와 목표 달성 집계 페이지
     */
    @GetMapping("/{clubNumb}/readings")
    @Operation(summary = "이전 모임 독서 기록 목록 조회")
    public ResultData getReadingHistoryList(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
          , @PathVariable Long clubNumb, @RequestParam(defaultValue = "1") int page) {
        // 접근 권한에 따라 결과 상세 이동 가능 여부를 포함한 종료 회차 목록을 반환함
        return readingClubService.getReadingHistoryList(userNumb, clubNumb, page);
    }

    /**
     * 활성 모임원에게 진행 또는 완료된 대상 회차의 완료 독후감을 공개 여부와 무관하게 제공함
     *
     * @author HanWon.Jang
     * @param userNumb 조회를 요청한 사용자 번호
     * @param clubNumb 조회할 모임 번호
     * @param rondNumb 조회할 회차 번호
     * @param sortType 독후감 정렬 코드
     * @param page 조회할 페이지 번호
     * @return 회차 도서 정보와 완료 독후감 페이지
     */
    @GetMapping("/{clubNumb}/readings/{rondNumb}/reports")
    @Operation(summary = "진행 또는 완료 모임 독서 회차의 완료 독후감 목록 조회")
    public ResultData getReadingRoundReportList(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
          , @PathVariable Long clubNumb
          , @PathVariable Long rondNumb
          , @RequestParam(defaultValue = "LATEST_DESC") String sortType
          , @RequestParam(defaultValue = "1") int page) {
        // 현재 활성 모임원에게만 대상 회차의 완료 독후감 목록을 반환함
        return readingClubService.getReadingRoundReportList(
                userNumb, clubNumb, rondNumb, sortType, page);
    }

    /**
     * 모임 정보와 카테고리 및 가입 질문을 저장하고 개설자를 모임장으로 등록함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 모임장 사용자 번호
     * @param request 모임 생성 입력값
     * @return 생성된 모임 상세 조회 결과
     */
    @PostMapping
    @Operation(summary = "새 모임 생성")
    public ResultData setClub(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                             , @Valid @RequestBody ReadingClubDto.ClubCreateReqDto request) {
        // 모임과 개설자 회원 관계를 한 트랜잭션으로 생성한 결과를 반환함
        return readingClubService.setClub(userNumb, request);
    }

    /**
     * 현재 모임장이 모임 기본 정보와 운영 설정을 수정함
     *
     * @author Hanwon.Jang
     * @param userNumb 로그인 사용자 번호
     * @param clubNumb 수정할 모임 번호
     * @param request 수정할 모임 정보
     * @return 수정된 모임 상세 조회 결과
     */
    @PutMapping("/{clubNumb}")
    @Operation(summary = "모임 수정")
    public ResultData uptClub(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                            , @PathVariable Long clubNumb
                            , @Valid @RequestBody ReadingClubDto.ClubCreateReqDto request) {
        // 현재 모임장 권한과 운영 제약을 적용한 수정 결과를 반환함
        return readingClubService.uptClub(userNumb, clubNumb, request);
    }

    /**
     * 현재 모임장이 모임과 종속 데이터를 복구 불가능하게 삭제함
     *
     * @author Hanwon.Jang
     * @param userNumb 로그인 사용자 번호
     * @param clubNumb 삭제할 모임 번호
     * @return 모임 물리 삭제 결과
     */
    @DeleteMapping("/{clubNumb}")
    @Operation(summary = "모임 삭제")
    public ResultData delClub(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                            , @PathVariable Long clubNumb) {
        // 현재 모임장만 실행할 수 있는 물리 삭제 결과를 반환함
        return readingClubService.delClub(userNumb, clubNumb);
    }

    /**
     * 공개 모임의 가입 방식에 따라 활성 회원을 등록하거나 승인 신청을 저장함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 가입 사용자 번호
     * @param clubNumb 모임 번호
     * @param request 가입 질문 답변
     * @return 가입 또는 신청 처리 결과
     */
    @PostMapping("/{clubNumb}/memberships")
    @Operation(summary = "공개 모임 가입")
    public ResultData setJoin(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                             , @PathVariable Long clubNumb
                             , @Valid @RequestBody ReadingClubDto.JoinReqDto request) {
        // 모임 정책에 맞춰 즉시 가입 또는 승인 신청 결과를 반환함
        return readingClubService.setJoin(userNumb, clubNumb, request);
    }

    /**
     * 활성 일반 모임원이 본인의 모임 활동 연결을 정리하고 자진 탈퇴함
     *
     * @author HanWon.Jang
     * @param userNumb 탈퇴를 요청한 사용자 번호
     * @param clubNumb 탈퇴할 모임 번호
     * @return 모임 자진 탈퇴 결과
     */
    @DeleteMapping("/{clubNumb}/memberships")
    @Operation(summary = "모임 자진 탈퇴", description = "활성 일반 모임원의 모임 활동 연결을 삭제하고 개인 독후감 원본은 보존한다.")
    public ResultData delMembership(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                   , @PathVariable Long clubNumb) {
        // 서버에서 계정과 일반 모임원 자격을 검증한 자진 탈퇴 결과를 반환함
        return readingClubService.delMembership(userNumb, clubNumb);
    }

    /**
     * 가입 신청자가 승인 전 자신의 처리 대기 신청을 취소함
     *
     * @author HanWon.Jang
     * @param userNumb 가입 신청 사용자 번호
     * @param clubNumb 모임 번호
     * @return 가입 신청 취소 결과
     */
    @DeleteMapping("/{clubNumb}/applications")
    @Operation(summary = "내 가입 신청 취소")
    public ResultData delApplication(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                    , @PathVariable Long clubNumb) {
        // 로그인 사용자의 처리 대기 신청만 취소한 결과를 반환함
        return readingClubService.delApplication(userNumb, clubNumb);
    }

    /**
     * 모임 관계가 없는 모임장의 맞팔로우 사용자를 초대 후보로 조회함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 모임장 사용자 번호
     * @param clubNumb 모임 번호
     * @return 맞팔로우 초대 후보 목록 조회 결과
     */
    @GetMapping("/{clubNumb}/invitation-candidates")
    @Operation(summary = "맞팔 초대 후보 조회")
    public ResultData getInviteCandidateList(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                             , @PathVariable Long clubNumb) {
        // 아직 모임 관계가 없는 활성 맞팔 후보를 반환함
        return readingClubService.getInviteCandidateList(userNumb, clubNumb);
    }

    /**
     * 모임장이 활성 회원에게 발송한 유효한 초대 목록을 조회함
     *
     * @author Hanwon.Jang
     * @param userNumb 모임장 사용자 번호
     * @param clubNumb 모임 번호
     * @return 보낸 초대 목록 조회 결과
     */
    @GetMapping("/{clubNumb}/invitations/sent")
    @Operation(summary = "보낸 모임 초대 조회")
    public ResultData getSentInvitationList(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                            , @PathVariable Long clubNumb) {
        // 활성 회원에게 발송한 만료 전 초대 목록을 반환함
        return readingClubService.getSentInvitationList(userNumb, clubNumb);
    }

    /**
     * 선택한 맞팔로우 사용자에게 모임 초대를 발송하고 정원 내 좌석을 예약함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 모임장 사용자 번호
     * @param clubNumb 모임 번호
     * @param request 초대 대상 목록
     * @return 초대 저장 결과
     */
    @PostMapping("/{clubNumb}/invitations")
    @Operation(summary = "맞팔 모임 초대")
    public ResultData setInvitation(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                   , @PathVariable Long clubNumb
                                   , @Valid @RequestBody ReadingClubDto.InviteReqDto request) {
        // 선택한 모든 맞팔 대상의 좌석 예약 결과를 반환함
        return readingClubService.setInvitation(userNumb, clubNumb, request);
    }

    /**
     * 로그인 사용자에게 도착한 만료 전 모임 초대 목록을 조회함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @return 받은 초대 목록 조회 결과
     */
    @GetMapping("/invitations/received")
    @Operation(summary = "받은 모임 초대 조회")
    public ResultData getInvitationList(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb) {
        // 로그인 사용자의 유효한 받은 초대를 반환함
        return readingClubService.getInvitationList(userNumb);
    }

    /**
     * 로그인 사용자의 유효한 초대 예약석을 활성 모임원 관계로 전환함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 초대 대상 사용자 번호
     * @param clubNumb 모임 번호
     * @return 초대 수락 처리 결과
     */
    @PutMapping("/{clubNumb}/invitations/received")
    @Operation(summary = "받은 모임 초대 수락")
    public ResultData uptInvitationAccepted(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                            , @PathVariable Long clubNumb) {
        // 예약석을 활성 회원으로 전환한 결과를 반환함
        return readingClubService.uptInvitationAccepted(userNumb, clubNumb);
    }

    /**
     * 로그인 사용자의 초대 예약석을 이력 없이 삭제하여 초대를 거절함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 초대 대상 사용자 번호
     * @param clubNumb 모임 번호
     * @return 초대 거절 처리 결과
     */
    @DeleteMapping("/{clubNumb}/invitations/received")
    @Operation(summary = "받은 모임 초대 거절")
    public ResultData delInvitation(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                   , @PathVariable Long clubNumb) {
        // 본인의 초대 예약석을 이력 없이 삭제한 결과를 반환함
        return readingClubService.delInvitation(userNumb, clubNumb);
    }

    /**
     * 모임장이 발송한 특정 사용자의 초대 예약석을 이력 없이 삭제함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 모임장 사용자 번호
     * @param clubNumb 모임 번호
     * @param targetUserNumb 초대 대상 사용자 번호
     * @return 초대 취소 처리 결과
     */
    @DeleteMapping("/{clubNumb}/invitations/{targetUserNumb}")
    @Operation(summary = "발송한 모임 초대 취소")
    public ResultData delOwnerInvitation(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                        , @PathVariable Long clubNumb, @PathVariable Long targetUserNumb) {
        // 모임장이 지정한 초대 예약석을 삭제한 결과를 반환함
        return readingClubService.delOwnerInvitation(userNumb, clubNumb, targetUserNumb);
    }

    /**
     * 모임장이 심사할 처리 중 가입 신청의 질문과 답변을 조회함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 모임장 사용자 번호
     * @param clubNumb 모임 번호
     * @return 처리 중 가입 신청 목록 조회 결과
     */
    @GetMapping("/{clubNumb}/applications")
    @Operation(summary = "가입 신청 조회")
    public ResultData getApplicationList(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                        , @PathVariable Long clubNumb) {
        // 모임장이 심사할 질문과 답변 목록을 반환함
        return readingClubService.getApplicationList(userNumb, clubNumb);
    }

    /**
     * 모임장이 가입 신청을 승인 또는 거절하고 신청 답변을 즉시 삭제함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 모임장 사용자 번호
     * @param clubNumb 모임 번호
     * @param applNumb 모임별 신청 번호
     * @param request 승인 또는 거절 상태
     * @return 가입 신청 처리 결과
     */
    @PutMapping("/{clubNumb}/applications/{applNumb}")
    @Operation(summary = "가입 신청 승인 또는 거절")
    public ResultData uptApplication(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                    , @PathVariable Long clubNumb, @PathVariable Long applNumb
                                    , @Valid @RequestBody ReadingClubDto.ApplicationDecisionReqDto request) {
        // 승인 시 좌석을 검사하고 처리된 답변을 즉시 삭제한 결과를 반환함
        return readingClubService.uptApplication(userNumb, clubNumb, applNumb, request);
    }
}
