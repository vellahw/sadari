package org.our.sadari.readingClub.mapper;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.our.sadari.readingClub.dto.ReadingClubDto;
import org.our.sadari.report.dto.ReportDto;

/**
 * fileName       : ReadingClubMapper
 * author         : SeungHyeon.Kang
 * date           : 2026-08-05
 * description    : 독서 모임 1차 기능의 데이터베이스 접근 메서드를 정의함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-08-05        SeungHyeon.Kang    최초 생성
 * 2026-08-14        SeungHyeon.Kang,Hanwon.Jang    모임원·수정·독서 메서드 추가
 * 2026-08-20        SeungHyeon.Kang    현재 독서 수정 메서드 추가
 * 2026-08-22        HanWon.Jang        종료 결과·독후감 조회 추가
 * 2026-08-23        HanWon.Jang        이전 독서 기록·회차 결과 조회 추가
 * 2026-08-24        HanWon.Jang        가입 신청 취소·모임원 퇴장 추가
 * 2026-08-29        HanWon.Jang        진행 회차 독후감 조회 확장
 * 2026-08-31        HanWon.Jang        독서 회차 조기 마감·결과 확인 추가
 * 2026-09-04        SeungHyeon.Kang    모임 채팅 읽음 수·강제 퇴장 이력 추가
 */
@Mapper
public interface ReadingClubMapper {

    /** 활성 모임원 여부를 조회함. @param clubNumb 모임 번호 @param userNumb 사용자 번호 @return 활성 모임원 수 */
    int getActiveMemberCnt(@Param("clubNumb") Long clubNumb, @Param("userNumb") Long userNumb);

    /** 모임 채팅을 조회함. @param clubNumb 모임 번호 @param userNumb 사용자 번호 @param afterChatNumb 마지막 채팅 번호 @param maxSize 최대 조회 수 @return 채팅 목록 */
    List<ReadingClubDto.ClubChatDto> getClubChatList(@Param("clubNumb") Long clubNumb
                                                    , @Param("userNumb") Long userNumb
                                                    , @Param("afterChatNumb") Long afterChatNumb
                                                    , @Param("maxSize") int maxSize);

    /** 모임원의 마지막 읽은 채팅 번호를 갱신함. @param clubNumb 모임 번호 @param userNumb 사용자 번호 @param chatNumb 마지막 읽은 채팅 번호 @return 수정 수 */
    int uptClubChatRead(@Param("clubNumb") Long clubNumb, @Param("userNumb") Long userNumb
                       , @Param("chatNumb") Long chatNumb);

    /** 모임 채팅을 중복 없이 저장함. @param clubNumb 모임 번호 @param userNumb 사용자 번호 @param chatType 채팅 유형 @param request 채팅 요청 @return 저장 수 */
    int setClubChat(@Param("clubNumb") Long clubNumb, @Param("userNumb") Long userNumb
                   , @Param("chatType") String chatType
                   , @Param("request") ReadingClubDto.ClubChatReqDto request);

    /** 저장된 채팅을 중복 방지 키로 조회함. @param clubNumb 모임 번호 @param userNumb 사용자 번호 @param clntUuid 중복 방지 키 @return 채팅 */
    ReadingClubDto.ClubChatDto getClubChatByUuid(@Param("clubNumb") Long clubNumb
                                                , @Param("userNumb") Long userNumb
                                                , @Param("clntUuid") String clntUuid);

    /** 채팅 알림을 받을 활성 모임원 번호를 조회함. @param clubNumb 모임 번호 @param senderNumb 발신 사용자 번호 @return 수신 사용자 번호 목록 */
    List<Long> getClubChatAlimUserList(@Param("clubNumb") Long clubNumb
                                      , @Param("senderNumb") Long senderNumb);

    /** 다음 도서 추천 목록을 조회함. @param clubNumb 모임 번호 @param userNumb 사용자 번호 @return 추천 목록 */
    List<ReadingClubDto.BookRecommendationDto> getBookRecommendationList(@Param("clubNumb") Long clubNumb
                                                                        , @Param("userNumb") Long userNumb
                                                                        , @Param("cycleStdt") LocalDateTime cycleStdt);

    /** 최신 독서 회차의 투표 주기 기준일을 조회함. @param clubNumb 모임 번호 @return 투표 정책 기준 회차 */
    ReadingClubDto.BookVoteRuleDto getBookVoteRuleDtl(@Param("clubNumb") Long clubNumb);

    /** 사용자별 추천 등록을 직렬화하기 위해 활성 모임원 행을 잠금. @param clubNumb 모임 번호 @param userNumb 사용자 번호 @return 사용자 번호 */
    Long getMemberForUpdate(@Param("clubNumb") Long clubNumb, @Param("userNumb") Long userNumb);

    /** 현재 투표 주기에 등록한 내 후보 수를 조회함. @param clubNumb 모임 번호 @param userNumb 사용자 번호 @param cycleStdt 주기 시작 일시 @return 후보 수 */
    int getMyBookRecommCnt(@Param("clubNumb") Long clubNumb, @Param("userNumb") Long userNumb
                          , @Param("cycleStdt") LocalDateTime cycleStdt);

    /** 현재 투표 주기에 등록한 내 투표 수를 조회함. @param clubNumb 모임 번호 @param userNumb 사용자 번호 @param cycleStdt 주기 시작 일시 @return 투표 수 */
    int getMyBookVoteCnt(@Param("clubNumb") Long clubNumb, @Param("userNumb") Long userNumb
                        , @Param("cycleStdt") LocalDateTime cycleStdt);

    /** 다음 도서 추천을 등록함. @param clubNumb 모임 번호 @param userNumb 사용자 번호 @param request 추천 도서 @return 등록 수 */
    int setBookRecommendation(@Param("clubNumb") Long clubNumb, @Param("userNumb") Long userNumb
                             , @Param("request") ReadingClubDto.BookRecommendationDto request);

    /** 본인의 다음 도서 추천을 삭제함. @param clubNumb 모임 번호 @param recmNumb 추천 번호 @param userNumb 사용자 번호 @return 삭제 수 */
    int delBookRecommendation(@Param("clubNumb") Long clubNumb, @Param("recmNumb") Long recmNumb
                             , @Param("userNumb") Long userNumb, @Param("cycleStdt") LocalDateTime cycleStdt);

    /** 다음 도서 투표를 등록하거나 변경함. @param clubNumb 모임 번호 @param userNumb 사용자 번호 @param recmNumb 추천 번호 @return 반영 수 */
    int uptBookVote(@Param("clubNumb") Long clubNumb, @Param("userNumb") Long userNumb
                   , @Param("recmNumb") Long recmNumb, @Param("cycleStdt") LocalDateTime cycleStdt);

    /**
     * 같은 중복 방지 키로 이미 생성된 모임 독서 회차를 조회함
     *
     * @author Hanwon.Jang
     * @param clubNumb 모임 번호
     * @param idemKeyx 중복 등록 방지 키
     * @return 기존 회차 번호
     */
    Long getReadingRoundByIdempotency(@Param("clubNumb") Long clubNumb
                                     , @Param("idemKeyx") String idemKeyx);

    /**
     * 활성 계정인 모임장이 현재 활성 멤버 관계를 유지하는지 확인함
     *
     * @author Hanwon.Jang
     * @param clubNumb 모임 번호
     * @param userNumb 모임장 사용자 번호
     * @return 등록 권한 관계 수
     */
    int getActiveOwnerCnt(@Param("clubNumb") Long clubNumb, @Param("userNumb") Long userNumb);

    /**
     * 모임 독서에 자동 참여할 활성 계정의 활성 멤버 번호를 조회함
     *
     * @author Hanwon.Jang
     * @param clubNumb 모임 번호
     * @return 자동 참여 사용자 번호 목록
     */
    List<Long> getActiveMemberUserNumbList(Long clubNumb);

    /**
     * 잠긴 모임 안에서 다음 독서 회차 번호를 계산함
     *
     * @author Hanwon.Jang
     * @param clubNumb 모임 번호
     * @return 다음 회차 번호
     */
    Long getNextReadingRoundNumb(Long clubNumb);

    /**
     * 선택 도서와 목표 기간으로 모임 독서 회차를 생성함
     *
     * @author Hanwon.Jang
     * @param clubNumb 모임 번호
     * @param userNumb 등록 모임장 사용자 번호
     * @param request 독서 회차 등록 정보
     * @return 등록 건수
     */
    int setReadingRound(@Param("clubNumb") Long clubNumb, @Param("userNumb") Long userNumb
                       , @Param("request") ReadingClubDto.ReadingCreateReqDto request);

    /**
     * 자동 생성된 멤버 독후감을 모임 독서 참여 정보와 연결함
     *
     * @author Hanwon.Jang
     * @param clubNumb 모임 번호
     * @param rondNumb 회차 번호
     * @param partNumb 참여 순번
     * @param userNumb 참여 사용자 번호
     * @param reptNumb 자동 생성 독후감 번호
     * @return 등록 건수
     */
    int setReadingParticipant(@Param("clubNumb") Long clubNumb, @Param("rondNumb") Long rondNumb
                             , @Param("partNumb") long partNumb, @Param("userNumb") Long userNumb
                             , @Param("reptNumb") Long reptNumb);

    /**
     * 수정할 예정 또는 진행 중인 모임 독서 회차를 잠가 조회함
     *
     * @author SeungHyeon.Kang
     * @param clubNumb 모임 번호
     * @param rondNumb 회차 번호
     * @return 잠긴 모임 독서 회차와 도서 정보
     */
    ReadingClubDto.ReadingManageDto getReadingForUpdate(@Param("clubNumb") Long clubNumb
                                                        , @Param("rondNumb") Long rondNumb);

    /**
     * 현재 회차에 연결된 독후감 행을 잠가 작성 여부 검사와 변경을 직렬화함
     *
     * @author SeungHyeon.Kang
     * @param clubNumb 모임 번호
     * @param rondNumb 회차 번호
     * @return 잠긴 연결 독후감 번호 목록
     */
    List<Long> getReadingReportNumbListForUpdate(@Param("clubNumb") Long clubNumb
                                                 , @Param("rondNumb") Long rondNumb);

    /**
     * 자동 생성 초기값에서 변경된 연결 독후감 수를 조회함
     *
     * @author SeungHyeon.Kang
     * @param clubNumb 모임 번호
     * @param rondNumb 회차 번호
     * @return 작성 또는 상태 변경된 독후감 수
     */
    int getWrittenReadingReportCnt(@Param("clubNumb") Long clubNumb, @Param("rondNumb") Long rondNumb);

    /**
     * 현재 모임 독서 회차의 도서와 목표 기간을 수정함
     *
     * @author SeungHyeon.Kang
     * @param clubNumb 모임 번호
     * @param rondNumb 회차 번호
     * @param request 수정할 도서와 목표 기간
     * @return 수정된 회차 수
     */
    int uptReading(@Param("clubNumb") Long clubNumb, @Param("rondNumb") Long rondNumb
                  , @Param("request") ReadingClubDto.ReadingUpdateReqDto request);

    /**
     * 현재 회차에 연결된 모든 독후감의 도서와 목표 기간을 동기화함
     *
     * @author SeungHyeon.Kang
     * @param clubNumb 모임 번호
     * @param rondNumb 회차 번호
     * @param request 수정할 도서와 목표 기간
     * @return 수정된 독후감 수
     */
    int uptReadingReportList(@Param("clubNumb") Long clubNumb, @Param("rondNumb") Long rondNumb
                            , @Param("request") ReadingClubDto.ReadingUpdateReqDto request);

    /**
     * 목표 기간 안에 활성 참여자 전원이 완독한 진행 회차를 완료 상태로 변경함
     *
     * @author HanWon.Jang
     * @param clubNumb 모임 번호
     * @param rondNumb 회차 번호
     * @return 완료된 회차 수
     */
    int uptEarlyReadingRound(@Param("clubNumb") Long clubNumb, @Param("rondNumb") Long rondNumb);

    /**
     * 조기 마감 회차의 활성 참여자를 목표 달성으로 고정함
     *
     * @author HanWon.Jang
     * @param clubNumb 모임 번호
     * @param rondNumb 회차 번호
     * @return 목표 달성이 확정된 참여자 수
     */
    int uptEarlyReadingGoal(@Param("clubNumb") Long clubNumb, @Param("rondNumb") Long rondNumb);

    /**
     * 조기 마감 회차 결과를 확인할 현재 활성 모임원을 등록함
     *
     * @author HanWon.Jang
     * @param clubNumb 모임 번호
     * @param rondNumb 회차 번호
     * @return 등록된 결과 확인 대상 수
     */
    int setEarlyResultTarget(@Param("clubNumb") Long clubNumb, @Param("rondNumb") Long rondNumb);

    /**
     * 사용자가 저장한 관심분야 수를 조회함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 조회할 사용자 번호
     * @return 저장된 관심분야 수
     */
    int getUserInterestCnt(Long userNumb);

    /**
     * 요청한 코드 중 활성 관심분야 세부코드 수를 조회함
     *
     * @author SeungHyeon.Kang
     * @param categoryList 검증할 관심분야 세부코드 목록
     * @return 활성 세부코드 수
     */
    int getValidCategoryCnt(@Param("categoryList") List<String> categoryList);

    /**
     * 독서 모임 마스터를 생성하고 생성 번호를 요청 DTO에 반영함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 모임장 사용자 번호
     * @param request 생성할 모임 정보
     * @return 생성된 모임 수
     */
    int setClub(@Param("userNumb") Long userNumb
               , @Param("request") ReadingClubDto.ClubCreateReqDto request);

    /**
     * 현재 모임장이 소유한 운영 중 모임의 기본 정보와 운영 설정을 수정함
     *
     * @author Hanwon.Jang
     * @param userNumb 모임장 사용자 번호
     * @param clubNumb 수정할 모임 번호
     * @param request 수정할 모임 정보
     * @return 수정된 모임 수
     */
    int uptClub(@Param("userNumb") Long userNumb, @Param("clubNumb") Long clubNumb
              , @Param("request") ReadingClubDto.ClubCreateReqDto request);

    /**
     * 현재 모임장이 소유한 운영 중 모임을 물리 삭제함
     *
     * @author Hanwon.Jang
     * @param userNumb 모임장 사용자 번호
     * @param clubNumb 삭제할 모임 번호
     * @return 삭제된 모임 수
     */
    int delClub(@Param("userNumb") Long userNumb, @Param("clubNumb") Long clubNumb);

    /**
     * 모임 삭제 전 선택지를 참조하는 투표용지 정리
     * @author HanWon.Jang
     * @param clubNumb 삭제할 모임 번호
     * @return 삭제된 투표용지 수
     */
    int delClubBallots(@Param("clubNumb") Long clubNumb);

    /**
     * 모임 삭제 전 결선부터 차수별 투표 정리
     * @author HanWon.Jang
     * @param clubNumb 삭제할 모임 번호
     * @param voteRoun 삭제할 투표 차수
     * @return 삭제된 투표 수
     */
    int delClubVotes(@Param("clubNumb") Long clubNumb, @Param("voteRoun") int voteRoun);

    /**
     * 모임 삭제 전 도서 선정을 참조하는 선거 정리
     * @author HanWon.Jang
     * @param clubNumb 삭제할 모임 번호
     * @return 삭제된 선거 수
     */
    int delClubElections(@Param("clubNumb") Long clubNumb);

    /**
     * 모임 삭제 전 회차를 참조하는 도서 선정 정리
     * @author HanWon.Jang
     * @param clubNumb 삭제할 모임 번호
     * @return 삭제된 도서 선정 수
     */
    int delClubSelections(@Param("clubNumb") Long clubNumb);

    /**
     * 모임 카테고리 한 건을 등록함
     *
     * @author SeungHyeon.Kang
     * @param clubNumb 모임 번호
     * @param intrCode 관심분야 세부코드
     * @param sortOrdr 모임 내 노출 순서
     * @return 등록된 카테고리 수
     */
    int setClubCategory(@Param("clubNumb") Long clubNumb
                       , @Param("intrCode") String intrCode
                       , @Param("sortOrdr") int sortOrdr);

    /**
     * 모임 수정 전에 기존 카테고리 관계를 삭제함
     *
     * @author Hanwon.Jang
     * @param clubNumb 수정할 모임 번호
     * @return 삭제된 카테고리 수
     */
    int delClubCategory(Long clubNumb);

    /**
     * 모임 개설자를 활성 모임장 회원으로 등록함
     *
     * @author SeungHyeon.Kang
     * @param clubNumb 모임 번호
     * @param userNumb 모임장 사용자 번호
     * @return 등록된 회원 수
     */
    int setOwnerMember(@Param("clubNumb") Long clubNumb, @Param("userNumb") Long userNumb);

    /**
     * 승인형 모임의 현재 가입 질문 한 행을 등록함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 질문 등록 사용자 번호
     * @param question 등록할 질문 값
     * @return 등록된 질문 행 수
     */
    int setClubQuestion(@Param("userNumb") Long userNumb
                       , @Param("question") ReadingClubDto.QuestionDto question);

    /**
     * 승인형 모임의 현재 가입 질문을 수정함
     *
     * @author Hanwon.Jang
     * @param userNumb 질문 수정 사용자 번호
     * @param question 수정할 질문 값
     * @return 수정된 질문 행 수
     */
    int uptClubQuestion(@Param("userNumb") Long userNumb
                       , @Param("question") ReadingClubDto.QuestionDto question);

    /**
     * 로그인 사용자가 활성 회원인 모임 목록을 조회함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @return 내 모임 목록
     */
    List<ReadingClubDto.ClubViewDto> getMyClubList(Long userNumb);

    /**
     * 공개 모임을 관심분야 일치 순서로 조회함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @param keyword 모임명과 소개 검색어
     * @return 공개 모임 목록
     */
    List<ReadingClubDto.ClubViewDto> getFindClubList(@Param("userNumb") Long userNumb
                                                   , @Param("keyword") String keyword);

    /**
     * 모임 상세 상태를 로그인 사용자 관점으로 조회함
     *
     * @author SeungHyeon.Kang
     * @param clubNumb 모임 번호
     * @param userNumb 로그인 사용자 번호
     * @return 모임 상세 상태
     */
    ReadingClubDto.ClubViewDto getClubDtl(@Param("clubNumb") Long clubNumb
                                        , @Param("userNumb") Long userNumb);

    /**
     * 정원과 권한 변경 전에 모임 마스터 행을 잠가 조회함
     *
     * @author SeungHyeon.Kang
     * @param clubNumb 모임 번호
     * @return 잠긴 모임 정보
     */
    ReadingClubDto.ClubViewDto getClubForUpdate(Long clubNumb);

    /**
     * 모임의 카테고리 목록을 노출 순서대로 조회함
     *
     * @author SeungHyeon.Kang
     * @param clubNumb 모임 번호
     * @return 모임 카테고리 목록
     */
    List<ReadingClubDto.CategoryDto> getClubCategoryList(Long clubNumb);

    /**
     * 모임당 한 행인 현재 가입 질문을 조회함
     *
     * @author SeungHyeon.Kang
     * @param clubNumb 모임 번호
     * @return 현재 가입 질문
     */
    ReadingClubDto.QuestionDto getClubQuestion(Long clubNumb);

    /**
     * 모임과 사용자의 현재 회원 또는 초대 관계를 조회함
     *
     * @author SeungHyeon.Kang
     * @param clubNumb 모임 번호
     * @param userNumb 사용자 번호
     * @return 현재 관계
     */
    ReadingClubDto.MemberDto getClubMember(@Param("clubNumb") Long clubNumb
                                          , @Param("userNumb") Long userNumb);

    /**
     * 활성 계정인 활성 모임원 목록과 프로필 이미지 경로를 가입 순서로 조회함
     *
     * @author SeungHyeon.Kang
     * @param clubNumb 조회할 모임 번호
     * @return 모임원 프로필 목록
     */
    List<ReadingClubDto.MemberProfileDto> getClubMemberList(Long clubNumb);

    /** 모임장이 관리할 퇴장 모임원 목록을 조회함. @author HanWon.Jang @param clubNumb 모임 번호 @return 퇴장 내역 목록 */
    List<ReadingClubDto.MemberExitHistoryDto> getMemberExitList(Long clubNumb);

    /** 퇴장 모임원의 제한 내역을 삭제함. @author HanWon.Jang @param ownerNumb 모임장 번호 @param clubNumb 모임 번호 @param targetUserNumb 대상 사용자 번호 @return 삭제 건수 */
    int delMemberRestriction(@Param("ownerNumb") Long ownerNumb, @Param("clubNumb") Long clubNumb
                            , @Param("targetUserNumb") Long targetUserNumb);

    /**
     * 활성 계정인 모임장이 지정한 활성 일반 멤버를 퇴장 상태로 변경하고 재가입을 차단함
     *
     * @author HanWon.Jang
     * @param ownerNumb 모임장 사용자 번호
     * @param clubNumb 모임 번호
     * @param targetUserNumb 퇴장 대상 사용자 번호
     * @return 변경된 모임원 수
     */
    int uptMemberExit(@Param("ownerNumb") Long ownerNumb, @Param("clubNumb") Long clubNumb
                     , @Param("targetUserNumb") Long targetUserNumb);

    /** 잠긴 모임에서 다음 강제 퇴장 이력 번호를 조회함. @param clubNumb 모임 번호 @return 다음 이력 번호 */
    Long getNextKickNumb(Long clubNumb);

    /** 강제 퇴장 이력을 저장함. @return 저장 수 */
    int setMemberKick(@Param("clubNumb") Long clubNumb, @Param("kickNumb") Long kickNumb
                      , @Param("targetUserNumb") Long targetUserNumb
                      , @Param("procUser") Long procUser, @Param("kickRson") String kickRson);

    /** 최근 미해제 강제 퇴장 이력에 제한 해제 일시를 저장함. @return 변경 수 */
    int uptMemberKickRelease(@Param("clubNumb") Long clubNumb
                             , @Param("targetUserNumb") Long targetUserNumb);

    /**
     * 최신 또는 지정한 완료 독서 회차의 도서와 목표 집계 결과를 조회함
     *
     * @author HanWon.Jang
     * @param clubNumb 조회할 모임 번호
     * @param userNumb 로그인 사용자 번호
     * @param rondNumb 조회할 회차 번호이며 최신 회차 조회이면 Null
     * @return 최신 또는 지정한 완료 독서 목표 결과
     */
    ReadingClubDto.ReadingGoalResultDto getReadingGoalResult(@Param("clubNumb") Long clubNumb
                                                           , @Param("userNumb") Long userNumb
                                                           , @Param("rondNumb") Long rondNumb);

    /**
     * 활성 모임원이 팝업에서 닫은 회차 결과의 확인 일시를 저장함
     *
     * @author HanWon.Jang
     * @param clubNumb 모임 번호
     * @param rondNumb 회차 번호
     * @param userNumb 확인한 사용자 번호
     * @return 확인 처리된 결과 수
     */
    int uptReadingResultConfirm(@Param("clubNumb") Long clubNumb, @Param("rondNumb") Long rondNumb
                               , @Param("userNumb") Long userNumb);

    /**
     * 종료된 회차에서 공개 가능한 목표 달성자 프로필을 조회함
     *
     * @author HanWon.Jang
     * @param clubNumb 조회할 모임 번호
     * @param rondNumb 조회할 회차 번호
     * @return 목표 달성자 프로필 목록
     */
    List<ReadingClubDto.MemberProfileDto> getReadingGoalAchievementMemberList(
            @Param("clubNumb") Long clubNumb, @Param("rondNumb") Long rondNumb);

    /**
     * 활성 계정과 활성 모임원 관계를 모두 유지하는 조회자인지 확인함
     *
     * @author HanWon.Jang
     * @param clubNumb 조회할 모임 번호
     * @param userNumb 조회를 요청한 사용자 번호
     * @return 활성 모임원 접근 관계 수
     */
    int getActiveMemberAccessCnt(@Param("clubNumb") Long clubNumb
                               , @Param("userNumb") Long userNumb);

    /**
     * 모임의 모든 완료 회차를 최신 순서로 조회함
     *
     * @author HanWon.Jang
     * @param clubNumb 조회할 모임 번호
     * @param pageOffset 목록 조회 시작 위치
     * @param pageLimit 다음 페이지 판정용 조회 건수
     * @return 가입 시점과 관계없이 조회된 이전 독서 기록
     */
    List<ReadingClubDto.ReadingHistoryDto> getReadingHistoryList(
            @Param("clubNumb") Long clubNumb, @Param("pageOffset") int pageOffset
          , @Param("pageLimit") int pageLimit);

    /**
     * 진행 또는 완료된 모임 독서 회차의 도서와 완료 독후감 평균 별점을 조회함
     *
     * @author HanWon.Jang
     * @param clubNumb 조회할 모임 번호
     * @param rondNumb 조회할 회차 번호
     * @return 진행 또는 완료 회차 독후감 페이지 요약
     */
    ReadingClubDto.ReadingRoundReportPageDto getReadingRoundReportSummary(
            @Param("clubNumb") Long clubNumb, @Param("rondNumb") Long rondNumb);

    /**
     * 진행 또는 완료된 모임 독서 회차에서 현재 활성 모임원이 작성한 완료 독후감을 조회함
     *
     * @author HanWon.Jang
     * @param userNumb 조회를 요청한 사용자 번호
     * @param clubNumb 조회할 모임 번호
     * @param rondNumb 조회할 회차 번호
     * @param sortType 정렬 코드
     * @param pageOffset 목록 조회 시작 위치
     * @param pageLimit 다음 페이지 판정용 조회 건수
     * @return 공개 여부와 무관한 완료 독후감 목록
     */
    List<ReportDto> getReadingRoundReportList(@Param("userNumb") Long userNumb
                                             , @Param("clubNumb") Long clubNumb
                                             , @Param("rondNumb") Long rondNumb
                                             , @Param("sortType") String sortType
                                             , @Param("pageOffset") int pageOffset
                                             , @Param("pageLimit") int pageLimit);

    /**
     * 목표 종료일이 지난 회차 참여자의 달성 여부를 독후감 상태로 확정함
     *
     * @author HanWon.Jang
     * @return 달성 여부가 확정된 참여자 수
     */
    int uptExpiredReadingParticipantGoal();

    /**
     * 목표 기간이 끝난 회차 결과를 확인할 현재 활성 모임원을 등록함
     *
     * @author HanWon.Jang
     * @return 등록된 결과 확인 대상 수
     */
    int setExpiredResultTarget();

    /**
     * 참여자 목표 확정이 끝난 만료 회차를 완료 상태로 변경함
     *
     * @author HanWon.Jang
     * @return 완료 상태로 변경된 회차 수
     */
    int uptExpiredReadingRound();

    /**
     * 모임의 만료된 초대 예약석을 물리 삭제함
     *
     * @author SeungHyeon.Kang
     * @param clubNumb 모임 번호
     * @return 삭제된 초대 수
     */
    int delExpiredInvitation(Long clubNumb);

    /**
     * 사용자가 받은 만료 초대를 물리 삭제함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 사용자 번호
     * @return 삭제된 초대 수
     */
    int delUserExpiredInvitation(Long userNumb);

    /**
     * 활성 회원과 유효한 초대 예약석을 합산함
     *
     * @author SeungHyeon.Kang
     * @param clubNumb 모임 번호
     * @return 현재 점유 좌석 수
     */
    int getOccupiedSeatCnt(Long clubNumb);

    /**
     * 공개 범위 변경을 제한하는 예정 또는 진행 중 회차 수를 조회함
     *
     * @author Hanwon.Jang
     * @param clubNumb 조회할 모임 번호
     * @return 예정 또는 진행 중 회차 수
     */
    int getOngoingRoundCnt(Long clubNumb);

    /**
     * 가입 방식 변경을 제한하는 처리 대기 신청 수를 조회함
     *
     * @author Hanwon.Jang
     * @param clubNumb 조회할 모임 번호
     * @return 처리 대기 가입 신청 수
     */
    int getPendingApplicationCnt(Long clubNumb);

    /**
     * 공개 즉시 가입 사용자를 활성 일반 회원으로 등록함
     *
     * @author SeungHyeon.Kang
     * @param clubNumb 모임 번호
     * @param userNumb 가입 사용자 번호
     * @return 등록된 회원 수
     */
    int setActiveMember(@Param("clubNumb") Long clubNumb, @Param("userNumb") Long userNumb);

    /**
     * 모임장의 맞팔 초대를 예약석 회원 행으로 등록함
     *
     * @author SeungHyeon.Kang
     * @param clubNumb 모임 번호
     * @param userNumb 초대 대상 사용자 번호
     * @param senderNumb 초대 모임장 사용자 번호
     * @return 등록된 초대 수
     */
    int setInvitation(@Param("clubNumb") Long clubNumb
                    , @Param("userNumb") Long userNumb
                    , @Param("senderNumb") Long senderNumb);

    /**
     * 유효한 맞팔 관계인지 조회함
     *
     * @author SeungHyeon.Kang
     * @param ownerNumb 모임장 사용자 번호
     * @param userNumb 초대 대상 사용자 번호
     * @return 상호 팔로우 관계 수
     */
    int getMutualFollowCnt(@Param("ownerNumb") Long ownerNumb, @Param("userNumb") Long userNumb);

    /**
     * 모임장의 맞팔 초대 후보를 조회함
     *
     * @author SeungHyeon.Kang
     * @param clubNumb 모임 번호
     * @param ownerNumb 모임장 사용자 번호
     * @return 아직 모임 관계가 없는 맞팔 후보 목록
     */
    List<ReadingClubDto.InviteCandidateDto> getInviteCandidateList(@Param("clubNumb") Long clubNumb
                                                                  , @Param("ownerNumb") Long ownerNumb);

    /**
     * 모임장이 발송한 유효한 초대 중 활성 회원에게 보낸 목록을 조회함
     *
     * @author Hanwon.Jang
     * @param clubNumb 모임 번호
     * @param ownerNumb 모임장 사용자 번호
     * @return 활성 회원에게 발송한 유효한 초대 목록
     */
    List<ReadingClubDto.SentInvitationDto> getSentInvitationList(@Param("clubNumb") Long clubNumb
                                                                , @Param("ownerNumb") Long ownerNumb);

    /**
     * 로그인 사용자에게 도착한 유효한 초대 목록을 조회함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @return 수신 초대 목록
     */
    List<ReadingClubDto.InvitationDto> getInvitationList(Long userNumb);

    /**
     * 초대 예약석을 활성 회원으로 전환함
     *
     * @author SeungHyeon.Kang
     * @param clubNumb 모임 번호
     * @param userNumb 초대 수락 사용자 번호
     * @return 전환된 회원 수
     */
    int uptInvitationAccepted(@Param("clubNumb") Long clubNumb, @Param("userNumb") Long userNumb);

    /**
     * 거절·취소하는 유효 초대 예약석을 물리 삭제함
     *
     * @author SeungHyeon.Kang
     * @param clubNumb 모임 번호
     * @param userNumb 초대 대상 사용자 번호
     * @return 삭제된 초대 수
     */
    int delInvitation(@Param("clubNumb") Long clubNumb, @Param("userNumb") Long userNumb);

    /**
     * 모임장이 활성 회원에게 발송한 유효한 초대를 취소함
     *
     * @author Hanwon.Jang
     * @param clubNumb 모임 번호
     * @param userNumb 초대 대상 사용자 번호
     * @param ownerNumb 모임장 사용자 번호
     * @return 삭제한 초대 수
     */
    int delOwnerInvitation(@Param("clubNumb") Long clubNumb
                         , @Param("userNumb") Long userNumb
                         , @Param("ownerNumb") Long ownerNumb);

    /**
     * 승인형 모임의 처리 중이거나 재신청 제한 중인 신청을 조회함
     *
     * @author SeungHyeon.Kang
     * @param clubNumb 모임 번호
     * @param userNumb 신청 사용자 번호
     * @return 가입 신청 제한 기준이 되는 신청
     */
    ReadingClubDto.ApplicationDto getBlockedApplication(@Param("clubNumb") Long clubNumb
                                                       , @Param("userNumb") Long userNumb);

    /**
     * 질문 사본과 답변을 포함한 승인 가입 신청을 생성함
     *
     * @author SeungHyeon.Kang
     * @param application 생성할 신청 데이터
     * @return 생성된 신청 수
     */
    int setJoinApplication(ReadingClubDto.ApplicationDto application);

    /**
     * 활성 가입 신청자가 자신의 처리 대기 신청과 답변을 삭제함
     *
     * @author HanWon.Jang
     * @param clubNumb 모임 번호
     * @param userNumb 가입 신청 사용자 번호
     * @return 삭제된 신청 수
     */
    int delOwnApplication(@Param("clubNumb") Long clubNumb
                        , @Param("userNumb") Long userNumb);

    /**
     * 모임장이 심사할 처리 중 가입 신청을 조회함
     *
     * @author SeungHyeon.Kang
     * @param clubNumb 모임 번호
     * @return 처리 중 가입 신청 목록
     */
    List<ReadingClubDto.ApplicationDto> getApplicationList(Long clubNumb);

    /**
     * 가입 신청을 승인 또는 거절하고 답변 본문을 즉시 삭제함
     *
     * @author SeungHyeon.Kang
     * @param clubNumb 모임 번호
     * @param applNumb 모임별 신청 번호
     * @param ownerNumb 처리 모임장 사용자 번호
     * @param joinStat 승인 또는 거절 상태
     * @return 처리된 신청 수
     */
    int uptJoinApplication(@Param("clubNumb") Long clubNumb
                         , @Param("applNumb") Long applNumb
                         , @Param("ownerNumb") Long ownerNumb
                         , @Param("joinStat") String joinStat);

    /**
     * 신청 번호로 현재 처리 중 가입 신청을 잠가 조회함
     *
     * @author SeungHyeon.Kang
     * @param clubNumb 모임 번호
     * @param applNumb 모임별 신청 번호
     * @return 잠긴 가입 신청
     */
    ReadingClubDto.ApplicationDto getApplicationForUpdate(@Param("clubNumb") Long clubNumb
                                                         , @Param("applNumb") Long applNumb);
}
