package org.our.sadari.readingClub.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.our.sadari.alim.service.AlimService;
import org.our.sadari.book.dto.BookDto;
import org.our.sadari.book.mapper.BookMapper;
import org.our.sadari.global.common.code.util.CodeUtil;
import org.our.sadari.global.common.constant.Constant;
import org.our.sadari.global.common.dto.PageDto;
import org.our.sadari.global.common.exception.CustomException;
import org.our.sadari.global.common.result.ResultData;
import org.our.sadari.global.common.result.ResultEnum;
import org.our.sadari.global.common.service.BadWordDetectionService;
import org.our.sadari.global.common.util.StringUtil;
import org.our.sadari.readingClub.dto.ReadingClubDto;
import org.our.sadari.readingClub.mapper.ReadingClubMapper;
import org.our.sadari.readingClub.mapper.ReadingClubMembershipMapper;
import org.our.sadari.report.dto.ReportDto;
import org.our.sadari.report.mapper.ReportMapper;
import org.our.sadari.social.service.UserBlockService;
import org.our.sadari.user.dto.UserSettingDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * fileName       : ReadingClubServiceImpl
 * author         : HanWon.Jang
 * date           : 2026-08-05
 * description    : 독서 모임 생성, 탐색, 가입, 초대와 승인 업무를 처리함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-08-05        SeungHyeon.Kang    최초 생성
 * 2026-08-14        SeungHyeon.Kang,Hanwon.Jang    모임원·초대·독서 처리 추가
 * 2026-08-20        SeungHyeon.Kang,Hanwon.Jang    독서 수정·초대 알림 처리
 * 2026-08-21        SeungHyeon.Kang    초대 알림 상황 통합
 * 2026-08-22        HanWon.Jang        종료 결과·독후감 조회 처리
 * 2026-08-23        HanWon.Jang        이전 독서 기록·회차 결과 조회 처리
 * 2026-08-24        HanWon.Jang        가입 알림·신청 취소·모임원 퇴장 처리
 * 2026-08-26        HanWon.Jang        다음 도서 투표 정책 처리
 * 2026-08-27        HanWon.Jang        가입 승인 알림 상황 수정
 * 2026-08-29        HanWon.Jang        진행 회차 독후감 조회 확장
 * 2026-08-31        HanWon.Jang        독서 조기 마감·결과 확인 처리
 * 2026-09-01        HanWon.Jang        공개 모임 조회·자진 탈퇴 처리
 * 2026-09-03        HanWon.Jang        사용자 차단 관계의 신규 참여 제한 추가
 * 2026-09-04        SeungHyeon.Kang    모임 채팅 읽음 수·강제 퇴장 이력 처리 추가
 * 2026-09-10        HanWon.Jang        채팅 열람과 알림 읽음 동기화
 * 2026-09-11        HanWon.Jang        모임 독서 등록 기본값 처리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadingClubServiceImpl implements ReadingClubService {

    // 공통 업무 성공 응답 코드
    private static final int RESULT_SUCCESS_CODE = 200;
    // 공개 모임 공개 범위 코드
    private static final String CLUB_PUBLIC = "PUBLIC";
    // 비공개 모임 공개 범위 코드
    private static final String CLUB_PRIVATE = "PRIVATE";
    // 승인 없이 즉시 가입하는 가입 방식 코드
    private static final String JOIN_OPEN = "OPEN";
    // 모임장 승인이 필요한 가입 방식 코드
    private static final String JOIN_APPROVAL = "APPROVAL";
    // 맞팔로우 초대만 허용하는 가입 방식 코드
    private static final String JOIN_INVITE = "INVITE";
    // 운영 중인 모임 상태 코드
    private static final String CLUB_ACTIVE = "ACTIVE";
    // 현재 모임에 참여 중인 활성 모임원 상태 코드
    private static final String MEMBER_ACTIVE = "ACTIVE";
    // 모임을 탈퇴하거나 퇴장한 모임원 상태 코드
    private static final String MEMBER_EXITED = "EXITED";
    // 사용자가 작성한 일반 채팅 유형 코드
    private static final String CHAT_TYPE_TEXT = "TEXT";
    // 모임 채팅 한 번의 최대 조회 건수
    private static final int CHAT_LIST_SIZE = 100;
    // 알림에 포함할 채팅 본문 최대 문자 수
    private static final int CHAT_ALIM_PREVIEW_SIZE = 80;
    // 모임 회차 독후감 목록이 한 번에 조회할 화면 항목 수
    private static final int REPORT_PAGE_SIZE = 12;
    // 이전 독서 기록 목록이 한 번에 조회할 화면 항목 수
    private static final int READING_HISTORY_PAGE_SIZE = 12;
    // 승인된 가입 신청 상태 코드
    private static final String APPLICATION_APPROVED = "APPROVED";
    // 거절된 가입 신청 상태 코드
    private static final String APPLICATION_REJECTED = "REJECTED";

    // 독서 모임 데이터베이스 접근 Mapper
    private final ReadingClubMapper readingClubMapper;
    // 모임 자진 탈퇴 데이터 정리 Mapper
    private final ReadingClubMembershipMapper readingClubMembershipMapper;
    // 사용자 입력 비속어 검사 서비스
    private final BadWordDetectionService badWordDetectionService;
    // 사용자 알림과 푸시 발송 서비스
    private final AlimService alimService;
    // 도서 마스터 데이터 접근 Mapper
    private final BookMapper bookMapper;
    // 멤버별 독후감 데이터 접근 Mapper
    private final ReportMapper reportMapper;
    // 모임 참여 당사자 사이의 양방향 사용자 차단 검증 서비스
    private final UserBlockService userBlockService;
    // 독후감 기본 책갈피 색상 공통코드 조회 도구
    private final CodeUtil codeUtil;
    // 활성 채팅 화면에 대한 알림 생략 판단 서비스
    private final ClubChatViewService clubChatViewService;

    /**
     * 다음 도서 추천 목록 조회
     *
     * @author HanWon.Jang
     * @param userNumb 유저 번호
     * @param clubNumb 모임 번호
     * @return
     */
    @Override
    public ResultData getBookRecommendationList(Long userNumb, Long clubNumb) {
        // 필수 식별값과 활성 모임원 권한을 함께 검증함
        if (StringUtil.hasEmpty(userNumb, clubNumb)
                || readingClubMapper.getActiveMemberCnt(clubNumb, userNumb) == 0) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }
        // 최신 회차 종료일을 기준으로 현재 투표 주기와 마감일을 계산함
        ReadingClubDto.BookVoteRuleDto voteRule = getBookVoteRule(clubNumb);
        // 화면에 후보 목록과 사용자별 등록 상태를 함께 전달할 응답을 생성함
        ReadingClubDto.BookVotePageDto pageDto = new ReadingClubDto.BookVotePageDto();
        // 현재 투표 주기에 등록된 후보만 화면 목록에 설정함
        pageDto.setCandidateList(readingClubMapper.getBookRecommendationList(clubNumb, userNumb
                                                                            , voteRule.getCycleStdt()));
        // 진행 중인 마감 주기가 있을 때만 마감일을 화면에 설정함
        pageDto.setVoteDeadline(voteRule.getVoteDeadline());
        // 진행 중인 마감 주기의 남은 일수를 화면에 설정함
        pageDto.setDDay(voteRule.getDDay());
        // 마감일 전 또는 새 상시 주기일 때만 후보 등록을 허용함
        pageDto.setCanRecommend(voteRule.isCanRecommend());
        // 현재 주기의 내 후보 등록 여부를 설정함
        pageDto.setHasRecommended(readingClubMapper.getMyBookRecommCnt(clubNumb, userNumb
                                                                      , voteRule.getCycleStdt()) > 0);
        // 현재 주기의 변경 불가능한 투표 완료 여부를 설정함
        pageDto.setHasVoted(readingClubMapper.getMyBookVoteCnt(clubNumb, userNumb, voteRule.getCycleStdt()) > 0);
        // 서버가 계산한 투표 화면 정책 정보를 반환함
        return ResultData.success(pageDto);
    }

    /**
     * 다음 도서 투표 후보 등록
     * @author HanWon.Jang
     * @param userNumb
     * @param clubNumb
     * @param request
     * @return
     */
    @Override
    @Transactional
    public ResultData setBookRecommendation(Long userNumb, Long clubNumb
                                            , ReadingClubDto.BookRecommendationDto request) {
        // 도서 식별 정보와 활성 모임원 권한이 있어야 추천을 등록함
        if (StringUtil.hasEmpty(userNumb, clubNumb, request, request.getBookIsbn(), request.getBookTitl())
                || readingClubMapper.getActiveMemberCnt(clubNumb, userNumb) == 0) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 같은 사용자의 동시 후보 등록을 직렬화하고 활성 모임원 자격을 다시 검증함
        if (StringUtil.isEmpty(readingClubMapper.getMemberForUpdate(clubNumb, userNumb))) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 최신 회차에 따른 후보 등록 가능 시점과 현재 주기를 계산함
        ReadingClubDto.BookVoteRuleDto voteRule = getBookVoteRule(clubNumb);
        // 마감일이거나 현재 주기에 이미 후보를 등록했다면 추가 등록을 차단함
        if (!voteRule.isCanRecommend()
                || readingClubMapper.getMyBookRecommCnt(clubNumb, userNumb, voteRule.getCycleStdt()) > 0) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 도서 마스터는 ISBN 기준으로 재사용하고 없을 때만 생성함
        // 언어 코드가 없는 이전 화면 요청은 기존 한국어 도서 정보로 보정함
        setDefaultBookLanguage(request);

        Long bookNumb = bookMapper.getBookNumbByIsbn(request);

        if (StringUtil.isEmpty(bookNumb)) {
            bookMapper.setBook(request);
            bookNumb = request.getBookNumb();
        }
        request.setBookNumb(bookNumb);
        // DB 고유 제약으로 같은 모임의 동일 도서 중복 추천을 차단함
        readingClubMapper.setBookRecommendation(clubNumb, userNumb, request);
        // 생성된 추천 번호를 반환함
        return ResultData.success(request.getRecmNumb());
    }

    /**
     * 도서 투표 추천 후보 삭제
     *
     * @author HanWon.Jang
     * @param userNumb
     * @param clubNumb
     * @param recmNumb
     * @return
     */
    @Override
    @Transactional
    public ResultData delBookRecommendation(Long userNumb, Long clubNumb, Long recmNumb) {
        // 활성 모임원만 본인 추천 삭제를 요청할 수 있음
        if (StringUtil.hasEmpty(userNumb, clubNumb, recmNumb)
                || readingClubMapper.getActiveMemberCnt(clubNumb, userNumb) == 0) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }
        // 같은 사용자의 후보 변경 요청을 직렬화함
        readingClubMapper.getMemberForUpdate(clubNumb, userNumb);
        // 최신 회차에 따른 후보 수정 가능 시점과 현재 주기를 계산함
        ReadingClubDto.BookVoteRuleDto voteRule = getBookVoteRule(clubNumb);
        // 마감일에는 후보 삭제를 허용하지 않음
        if (!voteRule.isCanRecommend()) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 현재 주기의 추천자 소유권 조건으로 삭제하고 다른 후보 삭제를 차단함
        if (readingClubMapper.delBookRecommendation(clubNumb, recmNumb, userNumb
                                                    , voteRule.getCycleStdt()) == 0) {
            // "조회 결과가 없어요."
            return ResultData.fail(ResultEnum.COMMON_NO_DATA);
        }
        // 본인 추천과 연결 투표 삭제 완료 응답을 반환함
        return ResultData.success();
    }

    /**
     * 다음 도서 투표
     *
     * @author HanWon.Jang
     * @param userNumb
     * @param clubNumb
     * @param request
     * @return
     */
    @Override
    @Transactional
    public ResultData uptBookVote(Long userNumb, Long clubNumb, ReadingClubDto.BookVoteReqDto request) {
        // 활성 모임원과 유효한 추천 번호만 투표 처리에 사용함
        if (StringUtil.hasEmpty(userNumb, clubNumb, request, request.getRecmNumb())
                || readingClubMapper.getActiveMemberCnt(clubNumb, userNumb) == 0) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 같은 사용자의 동시 투표 요청을 직렬화함
        readingClubMapper.getMemberForUpdate(clubNumb, userNumb);
        // 현재 투표 주기에 이미 투표했다면 값 변경을 차단함
        ReadingClubDto.BookVoteRuleDto voteRule = getBookVoteRule(clubNumb);
        if (readingClubMapper.getMyBookVoteCnt(clubNumb, userNumb, voteRule.getCycleStdt()) > 0
                || readingClubMapper.uptBookVote(clubNumb, userNumb, request.getRecmNumb()
                                                , voteRule.getCycleStdt()) == 0) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 현재 주기에 최초 등록된 변경 불가능한 투표 결과를 반환함
        return ResultData.success();
    }

    /**
     * 최신 독서 회차 종료일에서 다음 도서 투표 주기와 마감 상태를 계산함
     *
     * @author HanWon.Jang
     * @param clubNumb 모임 번호
     * @return 현재 투표 주기와 화면 정책 정보
     */
    private ReadingClubDto.BookVoteRuleDto getBookVoteRule(Long clubNumb) {
        // 최신 독서 회차가 없으면 마감 없는 최초 투표 주기를 사용함
        ReadingClubDto.BookVoteRuleDto voteRule = readingClubMapper.getBookVoteRuleDtl(clubNumb);
        if (StringUtil.isEmpty(voteRule) || StringUtil.isEmpty(voteRule.getGoalEndt())) {
            // 마감 없는 최초 주기의 정책 정보를 생성함
            voteRule = new ReadingClubDto.BookVoteRuleDto();
            // 독서 회차가 없으면 후보를 상시 등록할 수 있음
            voteRule.setCanRecommend(true);
            // 마감 없는 최초 투표 주기를 반환함
            return voteRule;
        }

        // 회차 목표 종료일 이틀 뒤를 해당 회차의 다음 도서 투표 마감일로 계산함
        LocalDate deadline = voteRule.getGoalEndt().toLocalDate().plusDays(2);
        // 서버 현지 날짜를 투표 일자 경계의 기준으로 사용함
        LocalDate today = LocalDate.now();
        // 마감일 다음날부터는 이전 후보를 제외한 새 상시 투표 주기를 시작함
        if (today.isAfter(deadline)) {
            // 새 주기 시작 시각을 마감일 다음날 자정으로 설정함
            voteRule.setCycleStdt(deadline.plusDays(1).atStartOfDay());
            // 새 주기에는 후보를 다시 등록할 수 있음
            voteRule.setCanRecommend(true);
            // 새 회차가 시작되기 전에는 이전 마감일을 화면에 표시하지 않음
            voteRule.setVoteDeadline(null);
            // 새 상시 투표 주기 정보를 반환함
            return voteRule;
        }

        // 진행 중인 회차가 시작된 시각부터 현재 후보와 투표를 구분함
        voteRule.setCycleStdt(voteRule.getRoundRegiDate());
        // 마감일 하루 전까지만 후보 등록과 삭제를 허용함
        voteRule.setCanRecommend(today.isBefore(deadline));
        // 화면에 표시할 마감일을 ISO 날짜 형식으로 설정함
        voteRule.setVoteDeadline(deadline.toString());
        // 오늘을 포함한 마감일까지의 날짜 차이를 디데이로 설정함
        voteRule.setDDay((int) ChronoUnit.DAYS.between(today, deadline));
        // 진행 중인 마감 투표 주기 정보를 반환함
        return voteRule;
    }

    /**
     * 모임 독서 회차와 활성 멤버별 읽는 중 독후감을 하나의 요청으로 등록
     *
     * @author Hanwon.Jang
     * @param userNumb 등록을 요청한 모임장 사용자 번호
     * @param clubNumb 모임 번호
     * @param request 선택 도서와 목표 독서 기간
     * @return 생성된 회차 번호
     */
    @Override
    @Transactional
    public ResultData setReading(Long userNumb, Long clubNumb, ReadingClubDto.ReadingCreateReqDto request) {

        // 모임과 등록 요청의 필수 참조값이 없으면 저장을 시작하지 않음
        if (StringUtil.hasEmpty(userNumb, clubNumb, request) || !isValidReadingRequest(request)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 같은 모임의 회차 번호 계산과 동시 등록을 직렬화하기 위해 모임 행을 잠금
        ReadingClubDto.ClubViewDto club = readingClubMapper.getClubForUpdate(clubNumb);

        // 활성 계정인 현재 모임장만 독서를 등록할 수 있음
        if (StringUtil.isEmpty(club) || !CLUB_ACTIVE.equals(club.getClubStat())
                || readingClubMapper.getActiveOwnerCnt(clubNumb, userNumb) == 0) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 네트워크 재시도로 같은 요청 키가 전달되면 기존 회차를 다시 반환함
        Long existingRondNumb = readingClubMapper.getReadingRoundByIdempotency(clubNumb, request.getIdemKeyx());
        if (!StringUtil.isEmpty(existingRondNumb)) {
            // 이미 생성된 회차 번호를 성공 결과로 반환함
            return ResultData.success(Map.of("rondNumb", existingRondNumb));
        }

        // 예정 또는 진행 중인 독서가 있으면 중복 회차 생성을 차단함
        if (readingClubMapper.getOngoingRoundCnt(clubNumb) > 0) {
            // "저장할 수 없어요. 입력 내용을 확인해주세요."
            return ResultData.fail(ResultEnum.COMMON_SAVE_REJECTED);
        }

        // 계정과 멤버 관계가 모두 활성인 사용자만 이번 회차에 자동 참여시킴
        List<Long> memberUserNumbList = readingClubMapper.getActiveMemberUserNumbList(clubNumb);

        // 모임장이 포함된 활성 멤버 목록이 없으면 불완전한 회차를 만들지 않음
        if (StringUtil.isEmpty(memberUserNumbList) || memberUserNumbList.isEmpty()
                || !memberUserNumbList.contains(userNumb)) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 자동 생성 독후감에 사용할 활성 책갈피 색상의 첫 값을 조회함
        String reportColor = codeUtil.getFirstCode(Constant.CODE_BOOK_COLR);
        if (StringUtil.isEmpty(reportColor)) {
            // 설정 누락은 부분 저장 없이 서버 오류로 롤백함
            throw new CustomException(ResultEnum.COMMON_SAVE_REJECTED, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 언어 코드가 없는 이전 화면 요청은 기존 한국어 도서 정보로 보정함
        setDefaultBookLanguage(request);

        // ISBN과 언어 기준으로 등록된 도서가 없을 때만 도서 마스터를 생성함
        if (bookMapper.dupBook(request) == 0) {
            // 신규 도서 마스터를 저장함
            int savedBookCnt = bookMapper.setBook(request);
            if (savedBookCnt != 1 || StringUtil.isEmpty(request.getBookNumb())) {
                // 도서 마스터 생성 실패는 전체 등록을 롤백함
                throw new CustomException(ResultEnum.COMMON_SAVE_REJECTED, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            // 기존 ISBN의 도서 번호를 이번 회차에 연결함
            request.setBookNumb(bookMapper.getBookNumbByIsbn(request));
        }

        // 기존 도서 조회 결과가 없으면 외래키가 없는 회차 생성을 차단함
        if (StringUtil.isEmpty(request.getBookNumb())) {
            // 도서 연결 실패는 전체 등록을 롤백함
            throw new CustomException(ResultEnum.COMMON_SAVE_REJECTED, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 잠긴 모임 범위에서 다음 회차 번호를 계산함
        request.setRondNumb(readingClubMapper.getNextReadingRoundNumb(clubNumb));
        if (StringUtil.isEmpty(request.getRondNumb())
                || readingClubMapper.setReadingRound(clubNumb, userNumb, request) != 1) {
            // 회차 생성 실패는 도서와 멤버 독후감까지 모두 롤백함
            throw new CustomException(ResultEnum.COMMON_SAVE_REJECTED, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        long partNumb = 1L;
        // 활성 멤버마다 같은 도서와 목표 기간의 읽는 중 독후감을 만듦
        for (Long memberUserNumb : memberUserNumbList) {
            // 현재 멤버의 자동 생성 독후감 값을 구성함
            ReportDto report = toReadingReport(memberUserNumb, request, reportColor);
            if (reportMapper.setReport(report) != 1 || StringUtil.isEmpty(report.getReptNumb())) {
                // 멤버 한 명의 독후감 생성 실패도 전체 등록을 롤백함
                throw new CustomException(ResultEnum.COMMON_SAVE_REJECTED, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // 생성된 독후감을 이번 모임 독서 참여 정보와 연결함
            int savedParticipantCnt = readingClubMapper.setReadingParticipant(
                    clubNumb, request.getRondNumb(), partNumb, memberUserNumb, report.getReptNumb());
            if (savedParticipantCnt != 1) {
                // 참여 연결 실패는 회차와 모든 멤버 독후감을 롤백함
                throw new CustomException(ResultEnum.COMMON_SAVE_REJECTED, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            partNumb++;
        }

        // 생성된 회차 번호를 등록 완료 결과로 반환함
        return ResultData.success(Map.of("rondNumb", request.getRondNumb()));
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
    @Override
    @Transactional
    public ResultData uptReading(Long userNumb, Long clubNumb, Long rondNumb
                                , ReadingClubDto.ReadingUpdateReqDto request) {

        // 모임과 회차 및 수정 요청의 필수 참조값이 없으면 변경을 시작하지 않음
        if (StringUtil.hasEmpty(userNumb, clubNumb, rondNumb, request)
                || !isValidReadingUpdate(request)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 계정 상태와 모임장 관계를 같은 잠금 범위에서 검증하기 위해 모임 행을 먼저 잠금
        ReadingClubDto.ClubViewDto club = readingClubMapper.getClubForUpdate(clubNumb);
        if (StringUtil.isEmpty(club) || !CLUB_ACTIVE.equals(club.getClubStat())
                || readingClubMapper.getActiveOwnerCnt(clubNumb, userNumb) == 0) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 완료되지 않은 수정 대상 회차를 잠가 동시 독후감 작성과 도서 변경을 직렬화함
        ReadingClubDto.ReadingManageDto reading = readingClubMapper.getReadingForUpdate(clubNumb, rondNumb);
        if (StringUtil.isEmpty(reading)) {
            // "조회 결과가 없어요."
            return ResultData.fail(ResultEnum.COMMON_NO_DATA);
        }

        // 작성 여부 검사와 연결 정보 변경이 끝날 때까지 현재 회차의 독후감 행을 잠금
        readingClubMapper.getReadingReportNumbListForUpdate(clubNumb, rondNumb);

        // ISBN이 달라진 경우에만 연결 독후감 작성 여부에 따른 도서 변경 정책을 적용함
        boolean bookChanged = !request.getBookIsbn().equals(reading.getBookIsbn());
        if (bookChanged && readingClubMapper.getWrittenReadingReportCnt(clubNumb, rondNumb) > 0) {
            // "작성된 독후감이 있어 도서를 변경할 수 없어요."
            return ResultData.fail(ResultEnum.READING_CLUB_BOOK_CHANGE_REJECTED);
        }

        // 언어 코드가 없는 이전 화면 요청은 기존 한국어 도서 정보로 보정함
        setDefaultBookLanguage(request);
        // 기존 도서를 유지하면 도서 마스터를 다시 조회하거나 생성하지 않음
        if (!bookChanged) {
            request.setBookNumb(reading.getBookNumb());
        } else if (bookMapper.dupBook(request) == 0) {
            // 변경할 도서가 공용 도서 마스터에 없으면 새 도서를 먼저 등록함
            int savedBookCnt = bookMapper.setBook(request);
            if (savedBookCnt != 1 || StringUtil.isEmpty(request.getBookNumb())) {
                // 도서 등록 실패는 회차와 독후감이 일부 변경되지 않도록 전체 작업을 롤백함
                throw new CustomException(ResultEnum.COMMON_UPDATE_REJECTED, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            // 이미 존재하는 ISBN의 도서 번호를 수정 대상 회차에 연결함
            request.setBookNumb(bookMapper.getBookNumbByIsbn(request));
        }

        // 유효한 도서 번호가 없으면 회차와 독후감의 기존 연결을 유지함
        if (StringUtil.isEmpty(request.getBookNumb())) {
            // "수정에 실패했어요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_UPDATE_REJECTED);
        }

        // 회차와 모든 연결 독후감의 도서 및 목표 기간을 같은 트랜잭션으로 변경함
        if (readingClubMapper.uptReading(clubNumb, rondNumb, request) != 1) {
            // 회차 수정 실패는 연결 독후감까지 변경되지 않도록 전체 작업을 롤백함
            throw new CustomException(ResultEnum.COMMON_UPDATE_REJECTED, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        readingClubMapper.uptReadingReportList(clubNumb, rondNumb, request);

        // 수정 완료한 현재 회차 번호를 반환함
        return ResultData.success(Map.of("rondNumb", rondNumb));
    }

    /**
     * 모임 회차 조기 마감
     *
     * @author HanWon.Jang
     * @param userNumb 마감을 요청한 모임장 사용자 번호
     * @param clubNumb 모임 번호
     * @param rondNumb 마감할 회차 번호
     * @return 완료된 회차 번호
     */
    @Override
    @Transactional
    public ResultData uptReadingCompletion(Long userNumb, Long clubNumb, Long rondNumb) {
        // 권한과 회차를 특정할 식별값이 없으면 상태 변경을 시작하지 않음
        if (StringUtil.hasEmpty(userNumb, clubNumb, rondNumb)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 계정 상태와 현재 모임장 관계를 같은 잠금 범위에서 검증함
        ReadingClubDto.ClubViewDto club = readingClubMapper.getClubForUpdate(clubNumb);
        if (StringUtil.isEmpty(club) || !CLUB_ACTIVE.equals(club.getClubStat())
                || readingClubMapper.getActiveOwnerCnt(clubNumb, userNumb) == 0) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 완료되지 않은 대상 회차와 연결 독후감을 잠가 완독 상태 변경과 조기 마감을 직렬화함
        if (StringUtil.isEmpty(readingClubMapper.getReadingForUpdate(clubNumb, rondNumb))) {
            // "조회 결과가 없어요."
            return ResultData.fail(ResultEnum.COMMON_NO_DATA);
        }
        readingClubMapper.getReadingReportNumbListForUpdate(clubNumb, rondNumb);

        // SQL에서 목표 기간과 활성 참여자 전원 완독을 다시 확인한 회차만 완료 처리함
        if (readingClubMapper.uptEarlyReadingRound(clubNumb, rondNumb) != 1) {
            // "수정에 실패했어요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_UPDATE_REJECTED);
        }

        // 회차 완료와 참여자 목표 확정 중 하나라도 실패하면 전체 상태를 원복함
        if (readingClubMapper.uptEarlyReadingGoal(clubNumb, rondNumb) < 1) {
            throw new CustomException(ResultEnum.COMMON_UPDATE_REJECTED, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 마감 시점의 모든 활성 모임원에게 같은 회차 결과를 미확인 상태로 등록함
        if (readingClubMapper.setEarlyResultTarget(clubNumb, rondNumb) < 1) {
            throw new CustomException(ResultEnum.COMMON_UPDATE_REJECTED, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 화면이 완료 회차 결과를 즉시 조회할 수 있도록 마감한 회차 번호를 반환함
        return ResultData.success(Map.of("rondNumb", rondNumb));
    }

    /**
     * 내 참여 모임 리스트 조회
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @return 내 모임 목록 조회 결과
     */
    @Override
    public ResultData getMyClubList(Long userNumb) {
        // 인증 사용자가 없으면 모임 관계를 조회하지 않음
        if (StringUtil.isEmpty(userNumb)) {
            // "인증에 실패했어요. 다시 로그인 해주세요."
            return ResultData.fail(ResultEnum.AUTH_FAIL);
        }

        // 로그인 사용자가 활성 회원으로 참여 중인 모임을 조회함
        List<ReadingClubDto.ClubViewDto> clubs = readingClubMapper.getMyClubList(userNumb);
        // 조회한 각 모임에 카테고리 표시 정보를 결합함
        List<ReadingClubDto.ClubViewDto> result = fillClubRelations(clubs, false);
        // 카테고리 표시 정보가 포함된 내 모임 목록을 반환함
        return ResultData.success(result);
    }

    /**
     * 모임 찾기 목록의 리스트 조회
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @param keyword 모임명과 소개 검색어
     * @return 공개 모임 목록 조회 결과
     */
    @Override
    public ResultData getFindClubList(Long userNumb, String keyword) {
        // 모임 찾기는 관심분야를 하나 이상 선택한 사용자만 이용함
        if (StringUtil.isEmpty(userNumb) || readingClubMapper.getUserInterestCnt(userNumb) == 0) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 검색어 양끝 공백과 길이를 보정함
        String normalizedKeyword = StringUtil.normalizePlainText(keyword, 100);
        // 관심분야 우선순위와 검색어를 적용한 공개 모임을 조회함
        List<ReadingClubDto.ClubViewDto> clubs = readingClubMapper.getFindClubList(userNumb, normalizedKeyword);
        // 조회한 각 공개 모임에 카테고리 표시 정보를 결합함
        List<ReadingClubDto.ClubViewDto> result = fillClubRelations(clubs, false);
        // 카테고리 표시 정보가 포함된 공개 모임 목록을 반환함
        return ResultData.success(result);
    }

    /**
     * 로그인 사용자의 참여 관계를 포함한 독서 모임 상세 정보를 조회함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @param clubNumb 모임 번호
     * @return 모임 상세 조회 결과
     */
    @Override
    public ResultData getClubDtl(Long userNumb, Long clubNumb) {
        // 상세 조회에 필요한 두 식별값을 검증함
        if (StringUtil.hasEmpty(userNumb, clubNumb)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 강제 퇴장 후 재가입이 차단된 관계인지 먼저 확인함
        ReadingClubDto.MemberDto relation = readingClubMapper.getClubMember(clubNumb, userNumb);
        // 모임장이 차단을 해제하기 전까지 공개 범위와 관계없이 모임 상세 접근을 거절함
        if (!StringUtil.isEmpty(relation) && Constant.COMM_YES.equals(relation.getBlocYsno())) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 로그인 사용자 관점의 모임 상세를 조회함
        ReadingClubDto.ClubViewDto club = readingClubMapper.getClubDtl(clubNumb, userNumb);
        // 존재하지 않는 모임은 데이터 없음으로 반환함
        if (StringUtil.isEmpty(club)) {
            // "조회 결과가 없어요."
            return ResultData.fail(ResultEnum.COMMON_NO_DATA);
        }

        // 비공개 모임은 활성 회원 또는 유효 초대 대상만 상세를 볼 수 있음
        if (CLUB_PRIVATE.equals(club.getClubVisb()) && StringUtil.isEmpty(club.getMembStat())) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 카테고리와 승인형 질문을 결합함
        fillClubRelation(club, true);
        // 완성된 상세 정보를 반환함
        return ResultData.success(club);
    }

    /**
     * 모임 참여 멤버 리스트 조회
     *
     * @author SeungHyeon.Kang
     * @param userNumb 조회를 요청한 사용자 번호
     * @param clubNumb 조회할 모임 번호
     * @return 모임원 프로필 목록 조회 결과
     */
    @Override
    public ResultData getClubMemberList(Long userNumb, Long clubNumb) {

        // 모임원 관계 조회에 필요한 식별값을 검증함
        if (StringUtil.hasEmpty(userNumb, clubNumb)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 활성 모임원 또는 공개 중인 활성 모임을 조회하는 사용자만 목록을 볼 수 있음
        if (!canViewClubOverview(userNumb, clubNumb,
                readingClubMapper.getActiveMemberAccessCnt(clubNumb, userNumb) > 0)) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 활성 계정인 활성 모임원과 프로필 이미지 경로를 조회함
        List<ReadingClubDto.MemberProfileDto> members = readingClubMapper.getClubMemberList(clubNumb);
        // 접근 가능한 모임원 프로필 목록을 반환함
        return ResultData.success(members);
    }

    /**
     * 모임 채팅 목록 조회
     *
     * @author SeungHyeon.Kang
     * @param userNumb
     * @param clubNumb
     * @param afterChatNumb
     * @return
     */
    @Override
    public ResultData getClubChatList(Long userNumb, Long clubNumb, Long afterChatNumb) {
        // 채팅 식별값과 마지막 채팅 번호 범위를 검증함
        if (StringUtil.hasEmpty(userNumb, clubNumb)
                || (!StringUtil.isEmpty(afterChatNumb) && afterChatNumb <= 0)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 활성 계정의 활성 모임원에게만 채팅 열람을 허용함
        if (readingClubMapper.getActiveMemberCnt(clubNumb, userNumb) == 0) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 최초에는 최신 채팅을, 이후에는 마지막 번호 다음 채팅을 시간순으로 반환함
        return ResultData.success(readingClubMapper.getClubChatList(
                clubNumb, userNumb, afterChatNumb, CHAT_LIST_SIZE));
    }

    /**
     * 활성 모임원의 마지막 읽은 채팅 번호를 갱신함.
     *
     * @author SeungHyeon.Kang
     * @param userNumb
     * @param clubNumb
     * @param request
     * @return
     */
    @Override
    @Transactional
    public ResultData uptClubChatRead(Long userNumb, Long clubNumb
                                    , ReadingClubDto.ClubChatReadReqDto request) {
        // 읽음 상태를 변경할 모임과 채팅 식별값을 검증함
        if (StringUtil.hasEmpty(userNumb, clubNumb, request)
                || (!StringUtil.isEmpty(request.getChatNumb()) && request.getChatNumb() <= 0)
                || (StringUtil.isEmpty(request.getChatNumb()) && StringUtil.isEmpty(request.getViewing()))
                || (!StringUtil.isEmpty(request.getViewing()) && StringUtil.isEmpty(request.getViewId()))) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 채팅 생성과 읽음 처리를 직렬화하여 늦게 저장된 알림의 읽음 누락 방지
        ReadingClubDto.ClubViewDto club = readingClubMapper.getClubForUpdate(clubNumb);
        if (StringUtil.isEmpty(club) || !CLUB_ACTIVE.equals(club.getClubStat())
                || readingClubMapper.getActiveMemberCnt(clubNumb, userNumb) == 0) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 빈 채팅방의 열람 신호와 실제 채팅 읽음 위치 갱신 분리
        if (!StringUtil.isEmpty(request.getChatNumb())) {
            // 같은 모임에 존재하는 채팅까지만 읽음 위치 이동
            if (readingClubMapper.uptClubChatRead(clubNumb, userNumb, request.getChatNumb()) == 0) {
                // "요청값이 올바르지 않아요."
                return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
            }
            // 확인한 위치까지의 해당 모임 채팅 알림만 함께 읽음 처리
            readingClubMapper.uptClubChatAlimRead(clubNumb, userNumb);
        }
        // 알림 배지에 즉시 반영할 현재 안 읽은 알림 수 조회
        ResultData result = alimService.getUnreadAlimCnt(userNumb);
        // 권한과 읽음 위치 검증을 통과한 화면의 열람 상태만 갱신
        if (!StringUtil.isEmpty(request.getViewing())) {
            clubChatViewService.uptChatView(clubNumb, userNumb, request.getViewId(), request.getViewing());
        }
        return result;
    }

    /**
     * 활성 모임원이 채팅을 전송함.
     *
     * @author SeungHyeon.Kang
     * @param userNumb
     * @param clubNumb
     * @param request
     * @return
     */
    @Override
    @Transactional
    public ResultData setClubChat(Long userNumb, Long clubNumb, ReadingClubDto.ClubChatReqDto request) {
        // 필수 식별값과 요청 본문을 검증함
        if (StringUtil.hasEmpty(userNumb, clubNumb, request, request.getChatCntn(), request.getClntUuid())) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 앞뒤 공백을 제거한 채팅 본문을 저장값으로 확정함
        String normalizedContent = StringUtil.normalizePlainText(request.getChatCntn());
        if (StringUtil.isEmpty(normalizedContent) || normalizedContent.length() > 2000) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 채팅의 비속어를 확인해 다른 모임원에게 노출되기 전에 차단함
        var badWord = badWordDetectionService.findBadWord(normalizedContent);
        if (badWord.isPresent()) {
            // "욕설이나 비속어는 사용할 수 없어요. 감지된 단어: {0}"
            return ResultData.fail(ResultEnum.COMMON_BAD_WORD_INCLUDED, badWord.get());
        }

        // 모임 상태와 회원 권한을 같은 트랜잭션에서 확정하도록 모임 행을 잠금
        ReadingClubDto.ClubViewDto club = readingClubMapper.getClubForUpdate(clubNumb);
        if (StringUtil.isEmpty(club) || !CLUB_ACTIVE.equals(club.getClubStat())
                || readingClubMapper.getActiveMemberCnt(clubNumb, userNumb) == 0) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 정규화된 본문을 중복 방지 키와 함께 저장함
        request.setChatCntn(normalizedContent);
        int insertCnt = readingClubMapper.setClubChat(clubNumb, userNumb, CHAT_TYPE_TEXT, request);
        ReadingClubDto.ClubChatDto chat = readingClubMapper.getClubChatByUuid(
                clubNumb, userNumb, request.getClntUuid());
        if (StringUtil.isEmpty(chat)) {
            // "등록에 실패했어요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_SAVE_REJECTED);
        }

        // 최초 저장 요청만 다른 활성 모임원에게 댓글 아이콘 유형의 채팅 알림을 발송함
        if (insertCnt > 0) {
            String preview = StringUtil.cutString(chat.getChatCntn(), "…", CHAT_ALIM_PREVIEW_SIZE);
            for (Long receiverNumb : readingClubMapper.getClubChatAlimUserList(clubNumb, userNumb)) {
                // 현재 해당 채팅방을 보고 있는 모임원의 알림센터 저장과 푸시 생략
                if (clubChatViewService.isViewing(clubNumb, receiverNumb)) {
                    continue;
                }
                ResultData alimResult = alimService.sendAlim(
                        receiverNumb
                      , Constant.ALIM_SITU_REPLY
                      , Constant.ALIM_TEMP_CODE_CLUB_CHAT_MESSAGE
                      , Constant.ALIM_TARGET_READING_CLUB
                      , clubNumb
                      , chat.getChatNumb()
                      , Map.of("clubName", club.getClubName()
                             , "userName", chat.getUserNick()
                             , "chatContent", preview)
                );
                // 알림 저장 실패 시 채팅만 남지 않도록 전체 트랜잭션을 롤백함
                if (StringUtil.isEmpty(alimResult) || alimResult.getCode() != RESULT_SUCCESS_CODE) {
                    throw new CustomException(ResultEnum.COMMON_SAVE_REJECTED
                                              , HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        }

        // 저장 또는 같은 중복 방지 키로 조회된 채팅을 반환함
        return ResultData.success(chat);
    }

    /**
     * 종료된 최신 독서 회차의 목표 결과 조회
     *
     * @author HanWon.Jang
     * @param userNumb 조회를 요청한 사용자 번호
     * @param clubNumb 조회할 모임 번호
     * @return 종료된 최신 독서 목표 결과
     */
    @Override
    public ResultData getReadingGoalResult(Long userNumb, Long clubNumb) {
        // 모임 상세에서는 가장 최근에 종료된 독서 회차의 결과를 조회함
        return getGoalResultInternal(userNumb, clubNumb, null);
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
    @Override
    public ResultData getReadingGoalResult(Long userNumb, Long clubNumb, Long rondNumb) {
        // 회차 결과 조회에 필요한 모든 식별값이 있는지 확인함
        if (StringUtil.hasEmpty(userNumb, clubNumb, rondNumb)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 목록에서 선택한 완료 회차의 결과를 조회함
        return getGoalResultInternal(userNumb, clubNumb, rondNumb);
    }

    /**
     * 독서 회차 결과 팝업 닫기 확인 처리
     *
     * @author HanWon.Jang
     * @param userNumb 확인한 사용자 번호
     * @param clubNumb 모임 번호
     * @param rondNumb 확인한 완료 회차 번호
     * @return 결과 확인 처리 결과
     */
    @Override
    @Transactional
    public ResultData uptReadingResultConfirm(Long userNumb, Long clubNumb, Long rondNumb) {
        // 확인 대상과 사용자를 특정할 식별값이 없으면 저장을 시작하지 않음
        if (StringUtil.hasEmpty(userNumb, clubNumb, rondNumb)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 활성 계정과 현재 활성 모임원 관계가 아니면 결과 확인 상태를 변경하지 않음
        if (readingClubMapper.getActiveMemberAccessCnt(clubNumb, userNumb) == 0) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 이미 확인된 중복 요청도 성공하도록 현재 사용자의 미확인 결과만 갱신함
        readingClubMapper.uptReadingResultConfirm(clubNumb, rondNumb, userNumb);
        // 사용자가 직접 닫은 결과의 확인 처리가 끝난 성공 응답을 반환함
        return ResultData.success();
    }

    /**
     * 활성 모임원에게 최신 또는 지정 완료 회차의 목표 결과를 제공
     *
     * @author HanWon.Jang
     * @param userNumb 조회를 요청한 사용자 번호
     * @param clubNumb 조회할 모임 번호
     * @param rondNumb 조회할 회차 번호이며 최신 회차 조회이면 Null
     * @return 완료 독서 회차의 목표 결과
     */
    private ResultData getGoalResultInternal(Long userNumb, Long clubNumb, Long rondNumb) {
        // 종료 결과 조회에 필요한 두 식별값이 없으면 권한과 결과를 조회하지 않음
        if (StringUtil.hasEmpty(userNumb, clubNumb)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 요청 사용자의 현재 모임원 관계를 조회함
        ReadingClubDto.MemberDto member = readingClubMapper.getClubMember(clubNumb, userNumb);
        // 현재 활성 모임원만 종료 회차 결과를 볼 수 있음
        if (StringUtil.isEmpty(member) || !MEMBER_ACTIVE.equals(member.getMembStat())) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 공개 가능한 활성 참여자 기준으로 최신 또는 지정 종료 회차 요약을 조회함
        ReadingClubDto.ReadingGoalResultDto result =
                readingClubMapper.getReadingGoalResult(clubNumb, userNumb, rondNumb);
        // 종료 회차가 없으면 화면에서 결과 팝업을 생략할 수 있도록 빈 성공 응답을 반환함
        if (StringUtil.isEmpty(result)) {
            // 종료 결과가 없는 성공 응답을 반환함
            return ResultData.success();
        }

        // 비활성화와 탈퇴 정책을 적용한 목표 달성자 프로필만 결과에 결합함
        result.setAchievementMemberList(
                readingClubMapper.getReadingGoalAchievementMemberList(clubNumb, result.getRondNumb()));
        // 종료 회차 요약과 공개 가능한 달성자 목록을 반환함
        return ResultData.success(result);
    }

    /**
     * 이전 독서 기록 리스트 조회
     *
     * @author HanWon.Jang
     * @param userNumb 조회를 요청한 사용자 번호
     * @param clubNumb 조회할 모임 번호
     * @param page 조회할 페이지 번호
     * @return 가입 시점과 관계없이 조회된 이전 독서 기록 페이지
     */
    @Override
    public ResultData getReadingHistoryList(Long userNumb, Long clubNumb, int page) {
        // 현재 계정과 모임 관계가 모두 활성인 사용자만 과거 회차 집계에 접근할 수 있음
        if (StringUtil.hasEmpty(userNumb, clubNumb)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 현재 활성 모임원만 회차별 목표 결과 상세로 이동할 수 있음
        boolean resultAccessible = readingClubMapper.getActiveMemberAccessCnt(clubNumb, userNumb) > 0;
        // 활성 모임원 또는 공개 중인 활성 모임을 조회하는 사용자만 목록을 볼 수 있음
        if (!canViewClubOverview(userNumb, clubNumb, resultAccessible)) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 요청 페이지를 첫 페이지 이상으로 보정함
        int normalizedPage = Math.max(page, 1);
        // 다음 페이지 존재 여부를 판정할 한 건을 추가해 종료 회차를 조회함
        List<ReadingClubDto.ReadingHistoryDto> searchedList =
                readingClubMapper.getReadingHistoryList(
                        clubNumb, (normalizedPage - 1) * READING_HISTORY_PAGE_SIZE
                      , READING_HISTORY_PAGE_SIZE + 1);
        // Mapper가 빈 값을 반환해도 페이지 응답을 유지하도록 빈 목록으로 보정함
        List<ReadingClubDto.ReadingHistoryDto> safeList =
                StringUtil.isEmpty(searchedList) ? List.of() : searchedList;
        // 제한 건수보다 한 건 더 조회되었는지 다음 페이지 여부로 판정함
        boolean hasNext = safeList.size() > READING_HISTORY_PAGE_SIZE;
        // 화면에는 현재 페이지 크기만 전달함
        List<ReadingClubDto.ReadingHistoryDto> visibleList = hasNext
                ? safeList.subList(0, READING_HISTORY_PAGE_SIZE)
                : safeList;
        // 각 회차에 현재 사용자의 목표 결과 상세 조회 권한을 표시함
        visibleList.forEach(history -> history.setResultAccessible(resultAccessible));
        // 가입 시점과 관계없이 조회한 종료 회차 페이지를 반환함
        return ResultData.success(new PageDto<>(visibleList, normalizedPage, hasNext));
    }

    /**
     * 활성 모임원 또는 공개 중인 활성 모임 조회자의 요약 정보 접근을 확인함
     *
     * @author HanWon.Jang
     * @param userNumb 조회를 요청한 사용자 번호
     * @param clubNumb 조회할 모임 번호
     * @param activeMember 활성 계정과 활성 모임원 관계 충족 여부
     * @return 독서 현황, 모임원과 이전 독서 기록 조회 가능 여부
     */
    private boolean canViewClubOverview(Long userNumb, Long clubNumb, boolean activeMember) {
        // 강제 퇴장 뒤 재가입이 차단된 사용자는 공개 모임 정보도 조회할 수 없음
        ReadingClubDto.MemberDto relation = readingClubMapper.getClubMember(clubNumb, userNumb);
        // 차단 관계가 있으면 공개 범위와 관계없이 조회를 거절함
        if (!StringUtil.isEmpty(relation) && Constant.COMM_YES.equals(relation.getBlocYsno())) {
            // 차단된 사용자의 요약 정보 접근 불가를 반환함
            return false;
        }
        // 현재 활성 모임원은 모임 공개 상태와 관계없이 조회할 수 있음
        if (activeMember) {
            // 활성 모임원의 요약 정보 접근 가능을 반환함
            return true;
        }
        // 비회원에게 공개할 모임의 공개 범위와 운영 상태를 조회함
        ReadingClubDto.ClubViewDto club = readingClubMapper.getClubDtl(clubNumb, userNumb);
        // 공개 중인 활성 모임에만 비회원 조회를 허용함
        return !StringUtil.isEmpty(club) && CLUB_PUBLIC.equals(club.getClubVisb())
                && CLUB_ACTIVE.equals(club.getClubStat());
    }

    /**
     * 활성 모임원에게 완료된 대상 회차의 완료 독후감을 공개 여부와 무관하게 제공함
     * 비활성 계정과 비활성 모임 관계의 작성자는 목록에서 제외함
     *
     * @author HanWon.Jang
     * @param userNumb 조회를 요청한 사용자 번호
     * @param clubNumb 조회할 모임 번호
     * @param rondNumb 조회할 회차 번호
     * @param sortType 독후감 정렬 코드
     * @param page 조회할 페이지 번호
     * @return 진행 또는 완료 회차의 도서 정보와 완료 독후감 페이지
     */
    @Override
    public ResultData getReadingRoundReportList(Long userNumb, Long clubNumb, Long rondNumb
                                               , String sortType, int page) {
        // 접근 관계와 대상 회차를 특정할 식별값이 없으면 조회하지 않음
        if (StringUtil.hasEmpty(userNumb, clubNumb, rondNumb)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 활성 계정이면서 현재 활성 모임원인 사용자만 비공개 독후감이 포함된 목록에 접근할 수 있음
        if (readingClubMapper.getActiveMemberAccessCnt(clubNumb, userNumb) < 1) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 요청한 진행 또는 완료 회차의 도서와 평균 별점 정보를 조회함
        ReadingClubDto.ReadingRoundReportPageDto result =
                readingClubMapper.getReadingRoundReportSummary(clubNumb, rondNumb);
        // 조회 가능한 진행 또는 완료 회차가 없으면 임의 회차의 독후감을 노출하지 않음
        if (StringUtil.isEmpty(result)) {
            // "조회 결과가 없어요."
            return ResultData.fail(ResultEnum.COMMON_NO_DATA);
        }

        // 화면에서 지원하는 정렬 코드만 허용하고 나머지는 최신순으로 보정함
        String normalizedSortType = StringUtil.normalizePlainText(sortType);
        if (!Constant.SORT_RELATION_DESC.equals(normalizedSortType)
                && !Constant.SORT_LATEST_DESC.equals(normalizedSortType)
                && !Constant.SORT_GRADE_DESC.equals(normalizedSortType)
                && !Constant.SORT_LIKE_DESC.equals(normalizedSortType)) {
            normalizedSortType = Constant.SORT_LATEST_DESC;
        }

        // 요청 페이지를 첫 페이지 이상으로 보정함
        int normalizedPage = Math.max(page, 1);
        // 다음 페이지 존재 여부를 판정할 한 건을 추가해 대상 회차의 완료 독후감을 조회함
        List<ReportDto> searchedList = readingClubMapper.getReadingRoundReportList(
                userNumb, clubNumb, rondNumb, normalizedSortType
              , (normalizedPage - 1) * REPORT_PAGE_SIZE, REPORT_PAGE_SIZE + 1);
        // Mapper가 빈 값을 반환해도 페이지 응답을 유지하도록 빈 목록으로 보정함
        List<ReportDto> safeList = StringUtil.isEmpty(searchedList) ? List.of() : searchedList;
        // 제한 건수보다 한 건 더 조회되었는지 다음 페이지 여부로 판정함
        boolean hasNext = safeList.size() > REPORT_PAGE_SIZE;
        // 화면에는 현재 페이지 크기만 전달함
        List<ReportDto> visibleList = hasNext
                ? safeList.subList(0, REPORT_PAGE_SIZE)
                : safeList;
        // 회차 도서 요약에 완료 독후감 페이지를 결합함
        result.setReportPage(new PageDto<>(visibleList, normalizedPage, hasNext));
        // 공개 여부와 무관한 완료 독후감 페이지를 현재 활성 모임원에게 반환함
        return ResultData.success(result);
    }

    /**
     * {@inheritDoc}
     *
     * @author HanWon.Jang
     * @return 반환값이 없음
     */
    @Override
    @Transactional
    public void completeExpiredRound() {
        // 회차 상태를 변경하기 전에 마감 시점의 참여자별 목표 달성 여부를 먼저 고정함
        readingClubMapper.uptExpiredReadingParticipantGoal();
        // 정상 종료 시점의 모든 활성 모임원에게 회차별 미확인 결과를 등록함
        readingClubMapper.setExpiredResultTarget();
        // 참여자 결과가 모두 고정된 만료 회차를 완료 상태로 변경함
        readingClubMapper.uptExpiredReadingRound();
    }

    /**
     * {@inheritDoc}
     *
     * @author SeungHyeon.Kang
     * @param userNumb 모임장 사용자 번호
     * @param request 모임 생성 입력값
     * @return 생성된 모임 상세 조회 결과
     */
    @Override
    @Transactional
    public ResultData setClub(Long userNumb, ReadingClubDto.ClubCreateReqDto request) {
        // 인증 사용자와 생성 요청을 검증함
        if (StringUtil.hasEmpty(userNumb, request) || !isValidClubRequest(request)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 사용자 입력 문구의 비속어를 검사함
        if (hasBadWord(request)) {
            // "욕설이나 비속어는 사용할 수 없어요. 감지된 단어: 모임 정보"
            return ResultData.fail(ResultEnum.COMMON_BAD_WORD_INCLUDED, "모임 정보");
        }

        // 모임 마스터를 생성함
        readingClubMapper.setClub(userNumb, request);
        // 선택 순서를 모임 내 정렬 순서로 저장함
        for (int index = 0; index < request.getCategoryList().size(); index++) {
            // 카테고리 한 건을 저장함
            readingClubMapper.setClubCategory(request.getClubNumb(), request.getCategoryList().get(index), index + 1);
        }

        // 개설자를 활성 모임장 회원으로 등록함
        readingClubMapper.setOwnerMember(request.getClubNumb(), userNumb);
        // 승인형 모임은 생성 시점의 가입 질문을 함께 저장함
        if (JOIN_APPROVAL.equals(request.getJoinType())) {
            // 모임당 질문 한 행을 저장함
            readingClubMapper.setClubQuestion(userNumb, toQuestion(request.getClubNumb(), request.getQuestionList()));
        }

        // 생성된 모임 상세를 반환함
        return getClubDtl(userNumb, request.getClubNumb());
    }

    /**
     * {@inheritDoc}
     *
     * @author Hanwon.Jang
     * @param userNumb 모임장 사용자 번호
     * @param clubNumb 수정할 모임 번호
     * @param request 수정할 모임 정보
     * @return 수정된 모임 상세 조회 결과
     */
    @Override
    @Transactional
    public ResultData uptClub(Long userNumb, Long clubNumb, ReadingClubDto.ClubCreateReqDto request) {
        // 모임 수정에 필요한 사용자와 대상 및 요청 본문을 먼저 검증함
        if (StringUtil.hasEmpty(userNumb, clubNumb, request)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 권한과 운영 제약을 같은 트랜잭션에서 판단하도록 모임 마스터 행을 잠금
        ReadingClubDto.ClubViewDto club = readingClubMapper.getClubForUpdate(clubNumb);
        // 현재 운영 중인 모임의 모임장만 모임 정보를 수정할 수 있음
        if (!isOwner(club, userNumb) || !CLUB_ACTIVE.equals(club.getClubStat())) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 수정 입력을 정규화하고 허용된 공개 범위와 가입 방식 조합인지 검증함
        if (!isValidClubRequest(request)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 모임명과 소개 및 승인 질문의 비속어를 저장 전에 검사함
        if (hasBadWord(request)) {
            // "욕설이나 비속어는 사용할 수 없어요. 감지된 단어: 모임 정보"
            return ResultData.fail(ResultEnum.COMMON_BAD_WORD_INCLUDED, "모임 정보");
        }

        // 현재 활성 회원과 유효한 예약 초대보다 작은 정원으로 줄일 수 없음
        if (readingClubMapper.getOccupiedSeatCnt(clubNumb) > request.getMaxxMemb()) {
            // "수정에 실패했어요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_UPDATE_REJECTED);
        }

        // 예정 또는 진행 중 회차가 있으면 기존 콘텐츠 공개 범위가 달라지지 않게 함
        if (!club.getClubVisb().equals(request.getClubVisb())
                && readingClubMapper.getOngoingRoundCnt(clubNumb) > 0) {
            // "수정에 실패했어요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_UPDATE_REJECTED);
        }

        // 처리 대기 신청이 있으면 신청 당시 가입 정책이 달라지지 않게 함
        if (!club.getJoinType().equals(request.getJoinType())
                && readingClubMapper.getPendingApplicationCnt(clubNumb) > 0) {
            // "수정에 실패했어요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_UPDATE_REJECTED);
        }

        // 권한 조건을 SQL에도 적용해 검증 이후의 소유권 또는 운영 상태 변경을 방어함
        if (readingClubMapper.uptClub(userNumb, clubNumb, request) == 0) {
            // "수정에 실패했어요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_UPDATE_REJECTED);
        }

        // 활성 계정의 수정 권한이 확정된 뒤 만료된 초대 예약석을 정리함
        readingClubMapper.delExpiredInvitation(clubNumb);

        // 기존 카테고리를 지운 뒤 요청 순서대로 유효한 관계를 다시 구성함
        readingClubMapper.delClubCategory(clubNumb);
        // 선택한 모든 카테고리를 순서값과 함께 저장함
        for (int index = 0; index < request.getCategoryList().size(); index++) {
            // 카테고리 한 건을 새 노출 순서로 저장함
            readingClubMapper.setClubCategory(clubNumb, request.getCategoryList().get(index), index + 1);
        }

        // 승인형 가입 방식은 이후 신청에 사용할 현재 질문을 등록하거나 수정함
        if (JOIN_APPROVAL.equals(request.getJoinType())) {
            // 요청 질문 목록을 고정 컬럼 DTO로 변환함
            ReadingClubDto.QuestionDto question = toQuestion(clubNumb, request.getQuestionList());
            // 기존 질문 행이 없으면 수정 대신 신규 질문 행을 등록함
            if (readingClubMapper.uptClubQuestion(userNumb, question) == 0) {
                // 승인형으로 새로 전환된 모임의 질문 행을 등록함
                readingClubMapper.setClubQuestion(userNumb, question);
            }
        }

        // 수정된 카테고리와 질문을 포함한 모임 상세를 반환함
        return getClubDtl(userNumb, clubNumb);
    }

    /**
     * {@inheritDoc}
     *
     * @author HanWon.Jang
     * @param userNumb 가입 신청 사용자 번호
     * @param clubNumb 모임 번호
     * @return 가입 신청 취소 결과
     */
    @Override
    @Transactional
    public ResultData delApplication(Long userNumb, Long clubNumb) {
        // 필수 식별값이 없거나 본인의 처리 대기 신청이 아니면 삭제하지 않음
        if (StringUtil.hasEmpty(userNumb, clubNumb)
                || readingClubMapper.delOwnApplication(clubNumb, userNumb) == 0) {
            // "삭제에 실패했어요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_DELETE_REJECTED);
        }

        // 신청 행을 물리 삭제하여 저장된 가입 답변도 즉시 제거함
        return ResultData.success();
    }

    /**
     * {@inheritDoc}
     *
     * @author HanWon.Jang
     * @param userNumb 모임장 사용자 번호
     * @param clubNumb 모임 번호
     * @param targetUserNumb 퇴장 대상 사용자 번호
     * @return 모임원 퇴장 결과
     */
    @Override
    @Transactional
    public ResultData delMember(Long userNumb, Long clubNumb, Long targetUserNumb
                                , ReadingClubDto.MemberKickReqDto request) {
        // 퇴장 처리에 필요한 식별값을 검증함
        if (StringUtil.hasEmpty(userNumb, clubNumb, targetUserNumb, request, request.getKickRson())) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 강제 퇴장 사유의 공백과 길이 및 비속어를 검증함
        String kickReason = StringUtil.normalizePlainText(request.getKickRson());
        if (StringUtil.isEmpty(kickReason) || kickReason.length() > 1000) {
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }
        var badWord = badWordDetectionService.findBadWord(kickReason);
        if (badWord.isPresent()) {
            return ResultData.fail(ResultEnum.COMMON_BAD_WORD_INCLUDED, badWord.get());
        }

        // 권한과 대상 상태 검증 및 동시 퇴장 요청을 직렬화하도록 모임 마스터 행을 잠금
        ReadingClubDto.ClubViewDto club = readingClubMapper.getClubForUpdate(clubNumb);
        // 운영 중인 모임의 모임장만 자신이 아닌 일반 멤버를 퇴장시킬 수 있음
        if (!isOwner(club, userNumb) || !CLUB_ACTIVE.equals(club.getClubStat())
                || userNumb.equals(targetUserNumb)) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // SQL의 활성 계정·소유권·일반 멤버 조건을 모두 통과한 대상만 퇴장 처리함
        if (readingClubMapper.uptMemberExit(userNumb, clubNumb, targetUserNumb) == 0) {
            // "수정에 실패했어요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_UPDATE_REJECTED);
        }

        // 잠긴 모임 안에서 순번을 계산하여 강제 퇴장 사유와 처리자를 보존 이력으로 저장함
        Long kickNumb = readingClubMapper.getNextKickNumb(clubNumb);
        if (StringUtil.isEmpty(kickNumb)
                || readingClubMapper.setMemberKick(clubNumb, kickNumb, targetUserNumb
                                                   , userNumb, kickReason) != 1) {
            throw new CustomException(ResultEnum.COMMON_SAVE_REJECTED, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 퇴장 대상에게 모임명이 포함된 알림을 저장하고 푸시를 예약함
        ResultData alimResult = alimService.sendAlim(
                targetUserNumb
              , Constant.ALIM_SITU_REJECTED
              , Constant.ALIM_TEMP_CODE_CLUB_MEMBER_EXITED
              , Constant.ALIM_TARGET_READING_CLUB
              , clubNumb
              , null
              , Map.of("clubName", club.getClubName())
        );

        // 알림 저장에 실패하면 퇴장 관계만 확정되지 않도록 전체 트랜잭션을 롤백함
        if (StringUtil.isEmpty(alimResult) || alimResult.getCode() != RESULT_SUCCESS_CODE) {
            // "수정에 실패했어요. 다시 시도해주세요."
            throw new CustomException(ResultEnum.COMMON_UPDATE_REJECTED, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 접근과 재가입 차단 및 알림 처리가 모두 완료된 성공 응답을 반환함
        return ResultData.success();
    }

    /** {@inheritDoc} @author HanWon.Jang */
    @Override
    public ResultData getMemberExitList(Long userNumb, Long clubNumb) {
        // 조회 식별값이 없으면 권한 조회를 수행하지 않음
        if (StringUtil.hasEmpty(userNumb, clubNumb)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 현재 활성 모임장만 퇴장 내역을 관리할 수 있음
        if (readingClubMapper.getActiveOwnerCnt(clubNumb, userNumb) == 0) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 권한이 확인된 모임의 퇴장 내역을 반환함
        return ResultData.success(readingClubMapper.getMemberExitList(clubNumb));
    }

    /** {@inheritDoc} @author HanWon.Jang */
    @Override
    @Transactional
    public ResultData delMemberRestriction(Long userNumb, Long clubNumb, Long targetUserNumb) {
        // 제한 내역 삭제에 필요한 식별값을 검증함
        if (StringUtil.hasEmpty(userNumb, clubNumb, targetUserNumb)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 모임장 소유권과 퇴장 및 제한 상태가 모두 일치할 때만 관계를 삭제함
        if (readingClubMapper.delMemberRestriction(userNumb, clubNumb, targetUserNumb) == 0) {
            // "삭제에 실패했어요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_DELETE_REJECTED);
        }

        // 삭제한 제한 관계에 대응하는 최근 강제 퇴장 이력에는 해제 일시를 남김
        readingClubMapper.uptMemberKickRelease(clubNumb, targetUserNumb);

        // 퇴장 관계와 재가입 제한을 함께 제거한 성공 결과를 반환함
        return ResultData.success();
    }

    /**
     * {@inheritDoc}
     *
     * @author Hanwon.Jang
     * @param userNumb 모임장 사용자 번호
     * @param clubNumb 삭제할 모임 번호
     * @return 모임 물리 삭제 결과
     */
    @Override
    @Transactional
    public ResultData delClub(Long userNumb, Long clubNumb) {
        // 모임 삭제에 필요한 사용자 번호와 대상 모임 번호를 검증함
        if (StringUtil.hasEmpty(userNumb, clubNumb)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 삭제 권한과 운영 상태를 한 트랜잭션에서 확정하도록 모임 마스터 행을 잠금
        ReadingClubDto.ClubViewDto club = readingClubMapper.getClubForUpdate(clubNumb);
        // 현재 운영 중인 모임의 모임장만 복구 불가능한 삭제를 실행할 수 있음
        if (!isOwner(club, userNumb) || !CLUB_ACTIVE.equals(club.getClubStat())) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 선택지 삭제를 막는 투표용지를 먼저 정리
        readingClubMapper.delClubBallots(clubNumb);
        // 본선 투표를 참조하는 결선 투표부터 정리
        readingClubMapper.delClubVotes(clubNumb, 2);
        // 결선 참조가 제거된 본선 투표와 선택지 및 유권자 정리
        readingClubMapper.delClubVotes(clubNumb, 1);
        // 도서 선정을 참조하는 모임장 선거 정리
        readingClubMapper.delClubElections(clubNumb);
        // 회차를 참조하는 도서 선정 정리
        readingClubMapper.delClubSelections(clubNumb);

        // 소유권과 활성 계정을 다시 검증하며 나머지 종속 데이터와 모임 삭제
        if (readingClubMapper.delClub(userNumb, clubNumb) == 0) {
            // 마지막 삭제가 거절되면 앞선 종속 데이터 삭제도 롤백
            throw new CustomException(ResultEnum.COMMON_DELETE_REJECTED, HttpStatus.BAD_REQUEST);
        }

        // 개인 독후감과 공용 도서를 제외한 모임 및 종속 데이터 삭제 성공을 반환함
        return ResultData.success();
    }

    /**
     * {@inheritDoc}
     *
     * @author SeungHyeon.Kang
     * @param userNumb 가입 사용자 번호
     * @param clubNumb 모임 번호
     * @param request 가입 질문 답변
     * @return 가입 또는 신청 처리 결과
     */
    @Override
    @Transactional
    public ResultData setJoin(Long userNumb, Long clubNumb, ReadingClubDto.JoinReqDto request) {
        // 가입 대상과 사용자 식별값을 검증함
        if (StringUtil.hasEmpty(userNumb, clubNumb, request)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 정원과 중복 관계를 같은 트랜잭션에서 판단하도록 모임 행을 잠금
        ReadingClubDto.ClubViewDto club = readingClubMapper.getClubForUpdate(clubNumb);
        // 공개 운영 중이며 관리자가 모집을 허용한 모임만 직접 가입할 수 있음
        if (StringUtil.isEmpty(club) || !CLUB_ACTIVE.equals(club.getClubStat())
                || !CLUB_PUBLIC.equals(club.getClubVisb()) || Constant.COMM_NO.equals(club.getRcrtYsno())) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 가입 사용자와 모임장이 차단 관계이면 즉시 가입과 승인 신청을 모두 허용하지 않음
        if (userBlockService.isBlocked(userNumb, club.getOwnrNumb())) {
            // 차단 방향을 노출하지 않는 공통 접근 거절 응답을 반환함
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 차단되지 않은 자진 탈퇴 관계만 새 가입 절차에서 다시 사용할 수 있음
        ReadingClubDto.MemberDto member = readingClubMapper.getClubMember(clubNumb, userNumb);
        // 현재 회원이나 초대 관계 또는 재가입 차단 관계이면 중복 가입을 막음
        if (!canJoinAgain(member)) {
            // "저장에 실패했어요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_SAVE_REJECTED);
        }

        // 즉시 가입은 현재 좌석을 검사하고 활성 회원을 바로 생성함
        if (JOIN_OPEN.equals(club.getJoinType())) {
            // 만료 초대를 지워 좌석 집계를 최신화함
            readingClubMapper.delExpiredInvitation(clubNumb);
            // 정원이 가득 찬 모임에는 새 회원을 추가하지 않음
            if (readingClubMapper.getOccupiedSeatCnt(clubNumb) >= club.getMaxxMemb()) {
                // "저장에 실패했어요. 다시 시도해주세요."
                return ResultData.fail(ResultEnum.COMMON_SAVE_REJECTED);
            }

            // 신규 등록 또는 자진 탈퇴 관계 재활성화가 반영되지 않으면 가입 완료 상태를 만들지 않음
            if (readingClubMapper.setActiveMember(clubNumb, userNumb) < 1) {
                // "저장에 실패했어요. 다시 시도해주세요."
                throw new CustomException(ResultEnum.COMMON_SAVE_REJECTED, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // 모임장에게 모임명과 모임 대상 정보가 포함된 신규 멤버 가입 알림을 저장하고 푸시를 예약함
            ResultData alimResult = alimService.sendAlim(
                    club.getOwnrNumb()
                  , Constant.ALIM_SITU_FOLLOW_CLUB
                  , Constant.ALIM_TEMP_CODE_CLUB_MEMBER_JOINED
                  , Constant.ALIM_TARGET_READING_CLUB
                  , clubNumb
                  , null
                  , Map.of("clubName", club.getClubName())
            );
            // 알림 저장에 실패하면 멤버 관계만 확정되지 않도록 즉시 가입 전체를 롤백함
            if (StringUtil.isEmpty(alimResult) || alimResult.getCode() != RESULT_SUCCESS_CODE) {
                // "저장에 실패했어요. 다시 시도해주세요."
                throw new CustomException(ResultEnum.COMMON_SAVE_REJECTED, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // 가입 완료 후 상세를 반환함
            return getClubDtl(userNumb, clubNumb);
        }

        // 승인 가입 이외 방식은 공개 페이지 직접 가입을 허용하지 않음
        if (!JOIN_APPROVAL.equals(club.getJoinType())) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 처리 중 신청 또는 거절 후 7일의 재신청 제한 기간에는 새 신청을 허용하지 않음
        if (!StringUtil.isEmpty(readingClubMapper.getBlockedApplication(clubNumb, userNumb))) {
            // "저장에 실패했어요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_SAVE_REJECTED);
        }

        // 현재 질문과 답변 개수 및 내용을 검증함
        ReadingClubDto.QuestionDto question = readingClubMapper.getClubQuestion(clubNumb);
        List<String> questions = toQuestionList(question);
        List<String> answers = normalizeTextList(request.getAnswerList(), 2000);
        // 모든 질문에 순서대로 장문 답변을 입력해야 함
        if (questions.isEmpty() || questions.size() != answers.size() || hasEmptyText(answers)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 답변 내 비속어 포함 여부를 검사함
        for (String answer : answers) {
            // 한 답변이라도 비속어가 있으면 전체 신청을 거절함
            if (badWordDetectionService.findBadWord(answer).isPresent()) {
                // "욕설이나 비속어는 사용할 수 없어요. 감지된 단어: 가입 답변"
                return ResultData.fail(ResultEnum.COMMON_BAD_WORD_INCLUDED, "가입 답변");
            }
        }

        // 신청 당시 질문과 답변을 한 행에 복사함
        if (readingClubMapper.setJoinApplication(toApplication(clubNumb, userNumb, questions, answers)) != 1) {
            // "저장에 실패했어요. 다시 시도해주세요."
            throw new CustomException(ResultEnum.COMMON_SAVE_REJECTED, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 모임장에게 모임명과 모임 대상 정보가 포함된 신규 가입 신청 알림을 저장하고 푸시를 예약함
        ResultData alimResult = alimService.sendAlim(
                club.getOwnrNumb()
              , Constant.ALIM_SITU_FOLLOW_CLUB
              , Constant.ALIM_TEMP_CODE_CLUB_JOIN_REQUESTED
              , Constant.ALIM_TARGET_READING_CLUB
              , clubNumb
              , null
              , Map.of("clubName", club.getClubName())
        );
        // 템플릿 누락 등으로 알림 저장에 실패하면 가입 신청만 확정되지 않도록 전체 트랜잭션을 롤백함
        if (StringUtil.isEmpty(alimResult) || alimResult.getCode() != RESULT_SUCCESS_CODE) {
            // "저장에 실패했어요. 다시 시도해주세요."
            throw new CustomException(ResultEnum.COMMON_SAVE_REJECTED, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 신청 완료 상세를 반환함
        return getClubDtl(userNumb, clubNumb);
    }

    /**
     * {@inheritDoc}
     *
     * @author HanWon.Jang
     * @param userNumb 탈퇴를 요청한 사용자 번호
     * @param clubNumb 탈퇴할 모임 번호
     * @return 모임 자진 탈퇴 결과
     */
    @Override
    @Transactional
    public ResultData delMembership(Long userNumb, Long clubNumb) {
        // 자진 탈퇴에 필요한 식별값을 검증함
        if (StringUtil.hasEmpty(userNumb, clubNumb)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 활성 계정의 활성 일반 모임원만 탈퇴 상태로 변경하고 모임장은 거절함
        if (readingClubMembershipMapper.uptMemberLeave(clubNumb, userNumb) == 0) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 탈퇴 회원이 다음 도서에 남긴 투표를 삭제함
        readingClubMembershipMapper.delMemberBookVotes(clubNumb, userNumb);
        // 탈퇴 회원의 다음 도서 추천과 해당 추천에 종속된 투표를 삭제함
        readingClubMembershipMapper.delMemberBookRecs(clubNumb, userNumb);
        // 탈퇴 회원의 모임장 투표와 유권자 자격을 삭제함
        readingClubMembershipMapper.delMemberElectionVotes(clubNumb, userNumb);
        // 개인 독후감 원본은 유지하고 모임 회차 참여 연결만 삭제함
        readingClubMembershipMapper.delMemberRoundLinks(clubNumb, userNumb);
        // 탈퇴 회원에게 생성된 목표 결과 확인 기록을 삭제함
        readingClubMembershipMapper.delMemberResultHistory(clubNumb, userNumb);
        // 이전 가입 질문과 답변을 포함한 신청 기록을 삭제함
        readingClubMembershipMapper.delMemberApplications(clubNumb, userNumb);

        // 모임 활동 연결 정리가 완료된 자진 탈퇴 성공 결과를 반환함
        return ResultData.success();
    }

    /**
     * {@inheritDoc}
     *
     * @author HanWon.Jang
     * @param userNumb 모임장 사용자 번호
     * @param clubNumb 모임 번호
     * @return 맞팔로우 초대 후보 목록 조회 결과
     */
    @Override
    public ResultData getInviteCandidateList(Long userNumb, Long clubNumb) {
        // 모임장 권한을 검증함
        ReadingClubDto.ClubViewDto club = readingClubMapper.getClubDtl(clubNumb, userNumb);
        // 현재 모임장만 맞팔 초대 후보를 볼 수 있음
        if (!isOwner(club, userNumb)) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 아직 관계가 없는 활성 맞팔로우 사용자를 조회함
        List<ReadingClubDto.InviteCandidateDto> candidates = readingClubMapper.getInviteCandidateList(clubNumb
                                                                                                      , userNumb);
        // 모임장이 선택할 수 있는 맞팔로우 초대 후보 목록을 반환함
        return ResultData.success(candidates);
    }

    /**
     * {@inheritDoc}
     *
     * @author Hanwon.Jang
     * @param userNumb 모임장 사용자 번호
     * @param clubNumb 모임 번호
     * @return 보낸 초대 목록 조회 결과
     */
    @Override
    public ResultData getSentInvitationList(Long userNumb, Long clubNumb) {
        // 현재 모임장만 활성 회원에게 발송한 초대 목록을 조회할 수 있음
        if (!isOwner(readingClubMapper.getClubDtl(clubNumb, userNumb), userNumb)) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 비활성화 또는 삭제 대기 회원을 제외한 유효한 보낸 초대 목록을 반환함
        return ResultData.success(readingClubMapper.getSentInvitationList(clubNumb, userNumb));
    }

    /**
     * {@inheritDoc}
     *
     * @author SeungHyeon.Kang
     * @param userNumb 모임장 사용자 번호
     * @param clubNumb 모임 번호
     * @param request 초대 대상 목록
     * @return 초대 저장 결과
     */
    @Override
    @Transactional
    public ResultData setInvitation(Long userNumb, Long clubNumb, ReadingClubDto.InviteReqDto request) {
        // 인증 사용자와 모임 및 요청 본문이 있어야 초대 대상을 확인할 수 있음
        if (StringUtil.hasEmpty(userNumb, clubNumb, request)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 요청 본문에서 초대 대상 사용자 번호 목록을 가져옴
        List<Long> targetUserNumbList = request.getUserNumbList();
        // 초대 대상 목록이 없으면 중복 검사와 좌석 계산을 진행하지 않음
        if (StringUtil.isEmpty(targetUserNumbList)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 초대 대상 사용자 번호의 중복을 검사할 집합을 생성함
        Set<Long> targetUserNumbSet = new HashSet<>(targetUserNumbList);
        // 같은 사용자가 중복 선택되면 전체 초대를 저장하지 않음
        if (targetUserNumbSet.size() != targetUserNumbList.size()) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 좌석 예약을 직렬화하도록 모임 행을 잠금
        ReadingClubDto.ClubViewDto club = readingClubMapper.getClubForUpdate(clubNumb);
        // 현재 운영 중이며 관리자가 모집을 허용한 모임의 모임장만 초대할 수 있음
        if (!isOwner(club, userNumb) || !CLUB_ACTIVE.equals(club.getClubStat())
                || Constant.COMM_NO.equals(club.getRcrtYsno())) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 만료된 예약석을 먼저 제거함
        readingClubMapper.delExpiredInvitation(clubNumb);
        // 선택한 전체 대상의 좌석을 한 번에 확보할 수 있어야 함
        if (readingClubMapper.getOccupiedSeatCnt(clubNumb) + targetUserNumbList.size() > club.getMaxxMemb()) {
            // "저장에 실패했어요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_SAVE_REJECTED);
        }

        // 모든 대상의 맞팔·기존 관계를 먼저 검증함
        for (Long targetUserNumb : targetUserNumbList) {
            // 맞팔이 아니거나 이미 모임 관계가 있으면 일괄 초대를 중단함
            if (StringUtil.isEmpty(targetUserNumb)
                    || userBlockService.isBlocked(userNumb, targetUserNumb)
                    || readingClubMapper.getMutualFollowCnt(userNumb, targetUserNumb) == 0
                    || !StringUtil.isEmpty(readingClubMapper.getClubMember(clubNumb, targetUserNumb))) {
                // "저장에 실패했어요. 다시 시도해주세요."
                return ResultData.fail(ResultEnum.COMMON_SAVE_REJECTED);
            }
        }

        // 검증된 대상마다 3일 유효한 예약석을 생성함
        for (Long targetUserNumb : targetUserNumbList) {
            // 초대 대상 한 명의 예약석을 등록함
            readingClubMapper.setInvitation(clubNumb, targetUserNumb, userNumb);
            // 초대받은 활성 회원의 알림센터에 모임장과 모임명이 포함된 초대 알림을 저장하고 푸시를 예약함
            ResultData alimResult = alimService.sendAlim(
                    targetUserNumb
                  , Constant.ALIM_SITU_FOLLOW_CLUB
                  , Constant.ALIM_TEMP_CODE_INVITE_CLUB
                  , Constant.ALIM_TARGET_READING_CLUB
                  , clubNumb
                  , null
                  , Map.of("userName", club.getOwnrNick(), "clubName", club.getClubName())
            );
            // 템플릿 누락 등으로 알림 저장에 실패하면 초대만 남지 않도록 전체 트랜잭션을 롤백함
            if (StringUtil.isEmpty(alimResult) || alimResult.getCode() != RESULT_SUCCESS_CODE) {
                // "저장에 실패했어요. 다시 시도해주세요."
                throw new CustomException(ResultEnum.COMMON_SAVE_REJECTED, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        // 초대 저장 성공 응답을 반환함
        return ResultData.success();
    }

    /**
     * {@inheritDoc}
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @return 받은 초대 목록 조회 결과
     */
    @Override
    @Transactional
    public ResultData getInvitationList(Long userNumb) {
        // 인증 사용자를 검증함
        if (StringUtil.isEmpty(userNumb)) {
            // "인증에 실패했어요. 다시 로그인 해주세요."
            return ResultData.fail(ResultEnum.AUTH_FAIL);
        }

        // 사용자에게 도착한 만료 초대를 먼저 삭제함
        readingClubMapper.delUserExpiredInvitation(userNumb);
        // 만료되지 않은 받은 모임 초대를 조회함
        List<ReadingClubDto.InvitationDto> invitations = readingClubMapper.getInvitationList(userNumb);
        // 로그인 사용자의 유효한 받은 초대 목록을 반환함
        return ResultData.success(invitations);
    }

    /**
     * {@inheritDoc}
     *
     * @author SeungHyeon.Kang
     * @param userNumb 초대 대상 사용자 번호
     * @param clubNumb 모임 번호
     * @return 초대 수락 처리 결과
     */
    @Override
    @Transactional
    public ResultData uptInvitationAccepted(Long userNumb, Long clubNumb) {
        // 대상 모임 행을 잠가 삭제와 경합하지 않게 함
        ReadingClubDto.ClubViewDto club = readingClubMapper.getClubForUpdate(clubNumb);
        // 수락 시점에 모임장과 초대 대상이 차단 관계이면 만료 전 초대도 활성화하지 않음
        if (!StringUtil.isEmpty(club) && userBlockService.isBlocked(userNumb, club.getOwnrNumb())) {
            // 차단 관계의 신규 모임 참여를 공통 수정 거절 응답으로 반환함
            return ResultData.fail(ResultEnum.COMMON_UPDATE_REJECTED);
        }

        // 유효한 운영 모임이고 모집이 허용된 경우에만 예약석을 활성 회원으로 전환함
        if (StringUtil.isEmpty(userNumb) || StringUtil.isEmpty(club)
                || Constant.COMM_NO.equals(club.getRcrtYsno())
                || readingClubMapper.uptInvitationAccepted(clubNumb, userNumb) == 0) {
            // "수정에 실패했어요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_UPDATE_REJECTED);
        }

        // 초대 수락 후 모임 상세를 반환함
        return getClubDtl(userNumb, clubNumb);
    }

    /**
     * {@inheritDoc}
     *
     * @author SeungHyeon.Kang
     * @param userNumb 초대 대상 사용자 번호
     * @param clubNumb 모임 번호
     * @return 초대 거절 처리 결과
     */
    @Override
    @Transactional
    public ResultData delInvitation(Long userNumb, Long clubNumb) {
        // 본인에게 도착한 초대 예약석만 삭제함
        if (StringUtil.hasEmpty(userNumb, clubNumb)
                || readingClubMapper.delInvitation(clubNumb, userNumb) == 0) {
            // "삭제에 실패했어요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_DELETE_REJECTED);
        }

        // 이력을 남기지 않은 초대 거절 성공 응답을 반환함
        return ResultData.success();
    }

    /**
     * {@inheritDoc}
     *
     * @author SeungHyeon.Kang
     * @param userNumb 모임장 사용자 번호
     * @param clubNumb 모임 번호
     * @param targetUserNumb 초대 대상 사용자 번호
     * @return 초대 취소 처리 결과
     */
    @Override
    @Transactional
    public ResultData delOwnerInvitation(Long userNumb, Long clubNumb, Long targetUserNumb) {
        // 현재 모임장만 특정 대상의 예약석을 취소할 수 있음
        if (!isOwner(readingClubMapper.getClubForUpdate(clubNumb), userNumb)
                || readingClubMapper.delOwnerInvitation(clubNumb, targetUserNumb, userNumb) == 0) {
            // "삭제에 실패했어요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_DELETE_REJECTED);
        }

        // 초대 취소 성공 응답을 반환함
        return ResultData.success();
    }

    /**
     * {@inheritDoc}
     *
     * @author SeungHyeon.Kang
     * @param userNumb 모임장 사용자 번호
     * @param clubNumb 모임 번호
     * @return 처리 중 가입 신청 목록 조회 결과
     */
    @Override
    public ResultData getApplicationList(Long userNumb, Long clubNumb) {
        // 현재 모임장만 신청 답변을 조회할 수 있음
        if (!isOwner(readingClubMapper.getClubDtl(clubNumb, userNumb), userNumb)) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 질문과 답변 컬럼을 화면용 목록으로 변환함
        List<ReadingClubDto.ApplicationDto> applications = readingClubMapper.getApplicationList(clubNumb);
        // 각 신청의 질문과 답변을 목록에 채움
        for (ReadingClubDto.ApplicationDto application : applications) {
            // 질문 목록을 설정함
            application.setQuestionList(toQuestionList(application));
            // 답변 목록을 설정함
            application.setAnswerList(toAnswerList(application));
        }

        // 심사 대기 신청 목록을 반환함
        return ResultData.success(applications);
    }

    /**
     * {@inheritDoc}
     *
     * @author SeungHyeon.Kang
     * @param userNumb 모임장 사용자 번호
     * @param clubNumb 모임 번호
     * @param applNumb 모임별 신청 번호
     * @param request 승인 또는 거절 상태
     * @return 가입 신청 처리 결과
     */
    @Override
    @Transactional
    public ResultData uptApplication(Long userNumb, Long clubNumb, Long applNumb
                                    , ReadingClubDto.ApplicationDecisionReqDto request) {
        // 승인 또는 거절 값만 허용함
        if (StringUtil.hasEmpty(userNumb, clubNumb, applNumb, request)
                || !(APPLICATION_APPROVED.equals(request.getJoinStat())
                || APPLICATION_REJECTED.equals(request.getJoinStat()))) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 정원과 신청 상태를 같은 트랜잭션에서 처리하도록 모임을 잠금
        ReadingClubDto.ClubViewDto club = readingClubMapper.getClubForUpdate(clubNumb);
        // 현재 모임장만 신청을 처리할 수 있음
        if (!isOwner(club, userNumb)) {
            // "올바르지 않은 접근이에요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 처리 중 신청을 잠가 중복 처리를 막음
        ReadingClubDto.ApplicationDto application = readingClubMapper.getApplicationForUpdate(clubNumb, applNumb);
        // 이미 처리됐거나 삭제된 신청은 갱신하지 않음
        if (StringUtil.isEmpty(application)) {
            // "수정에 실패했어요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_UPDATE_REJECTED);
        }

        // 처리 시점에 모임장과 신청자가 차단 관계이면 승인과 거절 모두 별도 알림 없이 거부함
        if (userBlockService.isBlocked(userNumb, application.getUserNumb())) {
            // 차단 방향을 노출하지 않는 공통 수정 거절 응답을 반환함
            return ResultData.fail(ResultEnum.COMMON_UPDATE_REJECTED);
        }

        // 승인 시점에만 모집 가능 여부와 실제 좌석을 경쟁함
        if (APPLICATION_APPROVED.equals(request.getJoinStat())) {
            // 관리자 모집 중지 이후에는 기존 대기 신청도 새 회원으로 승인하지 않음
            if (Constant.COMM_NO.equals(club.getRcrtYsno())) {
                // "수정에 실패했어요. 다시 시도해주세요."
                return ResultData.fail(ResultEnum.COMMON_UPDATE_REJECTED);
            }

            // 만료 초대를 제거하고 현재 좌석을 다시 계산함
            readingClubMapper.delExpiredInvitation(clubNumb);
            // 활성 회원과 예약석이 정원에 도달했으면 신청을 대기 상태로 유지함
            if (readingClubMapper.getOccupiedSeatCnt(clubNumb) >= club.getMaxxMemb()) {
                // "수정에 실패했어요. 다시 시도해주세요."
                return ResultData.fail(ResultEnum.COMMON_UPDATE_REJECTED);
            }

            // 현재 회원이나 초대 관계 또는 재가입 차단 관계이면 중복 등록하지 않음
            ReadingClubDto.MemberDto member = readingClubMapper.getClubMember(clubNumb, application.getUserNumb());
            // 차단되지 않은 자진 탈퇴 관계만 승인 가입으로 다시 활성화할 수 있음
            if (!canJoinAgain(member)) {
                // "수정에 실패했어요. 다시 시도해주세요."
                return ResultData.fail(ResultEnum.COMMON_UPDATE_REJECTED);
            }

            // 승인 대상자를 활성 일반 회원으로 등록함
            readingClubMapper.setActiveMember(clubNumb, application.getUserNumb());
        }

        // 처리 상태와 처리자를 기록하면서 답변 본문을 즉시 삭제함
        if (readingClubMapper.uptJoinApplication(
                clubNumb, applNumb, userNumb, request.getJoinStat()) == 0) {
            // "수정에 실패했어요. 다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_UPDATE_REJECTED);
        }

        // 처리 결과에 맞는 가입 신청 알림 템플릿을 선택함
        String tempCode = APPLICATION_APPROVED.equals(request.getJoinStat())
                ? Constant.ALIM_TEMP_CODE_CLUB_JOIN_APPROVED
                : Constant.ALIM_TEMP_CODE_CLUB_JOIN_REJECTED;
        // 승인 템플릿은 모임 알림 상황을 사용하고 거절 템플릿은 거절 상황을 사용함
        String alimSitu = APPLICATION_APPROVED.equals(request.getJoinStat())
                ? Constant.ALIM_SITU_FOLLOW_CLUB
                : Constant.ALIM_SITU_REJECTED;
        // 신청자에게 모임명과 모임 대상 정보가 포함된 처리 결과 알림을 저장하고 푸시를 예약함
        ResultData alimResult = alimService.sendAlim(
                application.getUserNumb()
              , alimSitu
              , tempCode
              , Constant.ALIM_TARGET_READING_CLUB
              , clubNumb
              , null
              , Map.of("clubName", club.getClubName())
        );
        // 템플릿 누락 등으로 알림 저장에 실패하면 신청 처리만 확정되지 않도록 전체 트랜잭션을 롤백함
        if (StringUtil.isEmpty(alimResult) || alimResult.getCode() != RESULT_SUCCESS_CODE) {
            // "수정에 실패했어요. 다시 시도해주세요."
            throw new CustomException(ResultEnum.COMMON_UPDATE_REJECTED, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 승인 또는 거절 성공 응답을 반환함
        return ResultData.success();
    }

    private boolean isValidClubRequest(ReadingClubDto.ClubCreateReqDto request) {
        // 사용자 입력 문구를 저장 전 정규화함
        request.setClubName(StringUtil.normalizePlainText(request.getClubName(), 100));
        // 모임 소개를 저장 허용 길이로 정규화함
        request.setClubCntn(StringUtil.normalizePlainText(request.getClubCntn(), 2000));
        // 카테고리와 질문 목록을 정규화함
        List<String> categories = normalizeTextList(request.getCategoryList(), 20);
        // 정규화한 카테고리의 중복을 검사할 집합을 생성함
        Set<String> categorySet = new HashSet<>(categories);
        // 정규화한 카테고리 목록을 요청에 반영함
        request.setCategoryList(categories);
        // 질문 목록을 정규화해 요청에 반영함
        request.setQuestionList(normalizeTextList(request.getQuestionList(), 500));

        // 공통 필수값과 허용 범위를 검사함
        if (StringUtil.hasEmpty(request.getClubName(), request.getClubCntn(), request.getClubVisb()
                , request.getJoinType(), request.getMaxxMemb()) || request.getMaxxMemb() < 2
                || request.getMaxxMemb() > 100 || categories.isEmpty() || categories.size() > 3
                || categorySet.size() != categories.size()
                || readingClubMapper.getValidCategoryCnt(categories) != categories.size()) {
            // 생성 조건 미충족을 반환함
            return false;
        }

        // 공개 범위와 가입 방식 조합을 검사함
        boolean validVisibility = CLUB_PUBLIC.equals(request.getClubVisb())
                || CLUB_PRIVATE.equals(request.getClubVisb());
        // 비공개는 초대형만, 공개는 세 방식 모두 허용함
        boolean validJoinType = CLUB_PRIVATE.equals(request.getClubVisb())
                ? JOIN_INVITE.equals(request.getJoinType())
                : JOIN_OPEN.equals(request.getJoinType()) || JOIN_APPROVAL.equals(request.getJoinType())
                  || JOIN_INVITE.equals(request.getJoinType());
        // 승인형은 질문 1~5개가 필수이고 다른 방식은 질문을 저장하지 않음
        boolean validQuestions = JOIN_APPROVAL.equals(request.getJoinType())
                ? !request.getQuestionList().isEmpty() && request.getQuestionList().size() <= 5
                  && !hasEmptyText(request.getQuestionList())
                : request.getQuestionList().isEmpty();
        // 조합 검증 결과를 반환함
        return validVisibility && validJoinType && validQuestions;
    }

    private boolean hasBadWord(ReadingClubDto.ClubCreateReqDto request) {
        // 모임명과 소개에서 비속어를 검사함
        if (badWordDetectionService.findBadWord(request.getClubName()).isPresent()
                || badWordDetectionService.findBadWord(request.getClubCntn()).isPresent()) {
            // 비속어가 있음을 반환함
            return true;
        }

        // 가입 질문을 순차 검사함
        for (String question : request.getQuestionList()) {
            // 비속어가 포함된 질문이 있으면 검사를 종료함
            if (badWordDetectionService.findBadWord(question).isPresent()) {
                // 비속어가 있음을 반환함
                return true;
            }
        }

        // 모든 입력이 검사 조건을 통과했음을 반환함
        return false;
    }

    private List<ReadingClubDto.ClubViewDto> fillClubRelations(List<ReadingClubDto.ClubViewDto> clubs
                                                              , boolean includeQuestions) {
        // 각 모임에 별도 관계 데이터를 결합함
        for (ReadingClubDto.ClubViewDto club : clubs) {
            // 카테고리와 선택적 질문을 결합함
            fillClubRelation(club, includeQuestions);
        }

        // 관계 데이터가 채워진 원래 목록을 반환함
        return clubs;
    }

    private void fillClubRelation(ReadingClubDto.ClubViewDto club, boolean includeQuestions) {
        // 모임 카테고리 목록을 노출 순서대로 설정함
        club.setCategoryList(readingClubMapper.getClubCategoryList(club.getClubNumb()));
        // 상세 화면이고 승인형인 경우에만 현재 질문을 설정함
        if (includeQuestions && JOIN_APPROVAL.equals(club.getJoinType())) {
            // 현재 질문 컬럼을 화면용 목록으로 변환함
            club.setQuestionList(toQuestionList(readingClubMapper.getClubQuestion(club.getClubNumb())));
        }
    }

    private boolean isOwner(ReadingClubDto.ClubViewDto club, Long userNumb) {
        // 모임 정보와 사용자 번호가 모두 있고 현재 모임장 번호가 같은지 반환함
        return !StringUtil.isEmpty(club) && !StringUtil.isEmpty(userNumb)
                && userNumb.equals(club.getOwnrNumb());
    }

    /**
     * 기존 관계가 없거나 재가입 차단 없이 자진 탈퇴한 관계인지 확인함
     *
     * @author HanWon.Jang
     * @param member 현재 모임원 관계
     * @return 새 가입 절차를 시작할 수 있으면 true
     */
    private boolean canJoinAgain(ReadingClubDto.MemberDto member) {
        // 기존 관계가 없으면 최초 가입을 허용함
        if (StringUtil.isEmpty(member)) {
            // 최초 가입 가능 상태를 반환함
            return true;
        }

        // 자진 탈퇴 상태이면서 재가입 차단이 없을 때만 다시 가입할 수 있음
        return MEMBER_EXITED.equals(member.getMembStat())
                && Constant.COMM_NO.equals(member.getBlocYsno());
    }

    private ReadingClubDto.QuestionDto toQuestion(Long clubNumb, List<String> questions) {
        // 가입 질문을 고정 컬럼에 담을 DTO를 생성함
        ReadingClubDto.QuestionDto result = new ReadingClubDto.QuestionDto();
        // 질문이 귀속될 모임 번호를 설정함
        result.setClubNumb(clubNumb);
        // 최대 다섯 개 질문을 고정 컬럼에 순서대로 설정함
        setQuestionFields(result, questions);
        // 저장용 질문 DTO를 반환함
        return result;
    }

    private ReadingClubDto.ApplicationDto toApplication(Long clubNumb, Long userNumb
                                                       , List<String> questions, List<String> answers) {
        // 가입 질문 사본과 답변을 담을 신청 DTO를 생성함
        ReadingClubDto.ApplicationDto result = new ReadingClubDto.ApplicationDto();
        // 신청 대상 모임 번호를 설정함
        result.setClubNumb(clubNumb);
        // 신청 사용자 번호를 설정함
        result.setUserNumb(userNumb);
        // 질문 사본을 고정 컬럼에 설정함
        setApplicationQuestions(result, questions);
        // 답변을 고정 컬럼에 설정함
        setApplicationAnswers(result, answers);
        // 저장용 신청 DTO를 반환함
        return result;
    }

    private void setQuestionFields(ReadingClubDto.QuestionDto target, List<String> values) {
        // 첫 번째 질문은 승인형 모임에 필수로 설정함
        target.setQuesFirs(getListValue(values, 0));
        // 선택적인 두 번째 질문을 설정함
        target.setQuesSeco(getListValue(values, 1));
        // 선택적인 세 번째 질문을 설정함
        target.setQuesThir(getListValue(values, 2));
        // 선택적인 네 번째 질문을 설정함
        target.setQuesFour(getListValue(values, 3));
        // 선택적인 다섯 번째 질문을 설정함
        target.setQuesFift(getListValue(values, 4));
    }

    private void setApplicationQuestions(ReadingClubDto.ApplicationDto target, List<String> values) {
        // 질문 사본을 순서별 컬럼에 설정함
        target.setQuesFirs(getListValue(values, 0));
        // 두 번째 질문 사본을 설정함
        target.setQuesSeco(getListValue(values, 1));
        // 세 번째 질문 사본을 설정함
        target.setQuesThir(getListValue(values, 2));
        // 네 번째 질문 사본을 설정함
        target.setQuesFour(getListValue(values, 3));
        // 다섯 번째 질문 사본을 설정함
        target.setQuesFift(getListValue(values, 4));
    }

    private void setApplicationAnswers(ReadingClubDto.ApplicationDto target, List<String> values) {
        // 답변을 질문과 같은 순서의 컬럼에 설정함
        target.setAnsrFirs(getListValue(values, 0));
        // 두 번째 가입 답변을 설정함
        target.setAnsrSeco(getListValue(values, 1));
        // 세 번째 가입 답변을 설정함
        target.setAnsrThir(getListValue(values, 2));
        // 네 번째 가입 답변을 설정함
        target.setAnsrFour(getListValue(values, 3));
        // 다섯 번째 가입 답변을 설정함
        target.setAnsrFift(getListValue(values, 4));
    }

    private List<String> toQuestionList(ReadingClubDto.QuestionDto question) {
        // 질문 행이 없으면 빈 목록을 반환함
        if (StringUtil.isEmpty(question)) {
            // 질문 없음 목록을 반환함
            return List.of();
        }

        // 질문 컬럼을 Null 제외 목록으로 반환함
        return compactList(question.getQuesFirs(), question.getQuesSeco(), question.getQuesThir()
                         , question.getQuesFour(), question.getQuesFift());
    }

    private List<String> toQuestionList(ReadingClubDto.ApplicationDto application) {
        // 신청 당시 질문 컬럼을 Null 제외 목록으로 반환함
        return compactList(application.getQuesFirs(), application.getQuesSeco(), application.getQuesThir()
                         , application.getQuesFour(), application.getQuesFift());
    }

    private List<String> toAnswerList(ReadingClubDto.ApplicationDto application) {
        // 처리 전 답변 컬럼을 Null 제외 목록으로 반환함
        return compactList(application.getAnsrFirs(), application.getAnsrSeco(), application.getAnsrThir()
                         , application.getAnsrFour(), application.getAnsrFift());
    }

    private List<String> compactList(String... values) {
        // Null이 아닌 고정 컬럼 값을 담을 목록을 생성함
        List<String> result = new ArrayList<>();
        // 고정 컬럼 값을 순서대로 순회함
        for (String value : values) {
            // 값이 있는 컬럼만 화면용 목록에 추가함
            if (!StringUtil.isEmpty(value)) {
                // 원래 순서를 유지해 목록에 추가함
                result.add(value);
            }
        }

        // Null이 제거된 순서 목록을 반환함
        return result;
    }

    /**
     * 모임 독서 등록 요청의 도서 필드와 목표 기간을 정규화하고 검증함
     *
     * @author Hanwon.Jang
     * @param request 검증할 모임 독서 등록 요청
     * @return 저장 가능한 요청이면 true
     */
    private boolean isValidReadingRequest(ReadingClubDto.ReadingCreateReqDto request) {

        // 외부 도서 검색 결과도 서버 저장 규격에 맞춰 길이와 공백을 정규화함
        request.setBookTitl(StringUtil.normalizePlainText(request.getBookTitl(), 500));
        request.setBookAthr(StringUtil.normalizePlainText(request.getBookAthr(), 500));
        request.setBookPubl(StringUtil.normalizePlainText(request.getBookPubl(), 500));
        request.setBookIsbn(StringUtil.normalizePlainText(request.getBookIsbn(), 100));
        request.setBookCvim(StringUtil.normalizePlainText(request.getBookCvim(), 1000));
        request.setBookDesc(StringUtil.normalizePlainText(request.getBookDesc(), 4000));
        request.setPublDate(StringUtil.normalizePlainText(request.getPublDate(), 10));
        request.setGoalStdt(StringUtil.normalizePlainText(request.getGoalStdt(), 10));
        request.setGoalEndt(StringUtil.normalizePlainText(request.getGoalEndt(), 10));
        request.setIdemKeyx(StringUtil.normalizePlainText(request.getIdemKeyx(), 64));

        // 독후감 등록과 동일한 도서 필수값과 회차 키 및 기간을 요구함
        if (StringUtil.hasEmpty(request.getBookTitl(), request.getBookAthr(), request.getBookPubl()
                              , request.getBookIsbn(), request.getBookCvim(), request.getBookDesc()
                              , request.getGoalStdt(), request.getGoalEndt(), request.getIdemKeyx())) {
            // 필수값 누락을 검증 실패로 반환함
            return false;
        }

        try {
            // ISO 날짜만 허용하고 시작일이 종료일보다 늦지 않은지 확인함
            LocalDate startDate = LocalDate.parse(request.getGoalStdt());
            LocalDate endDate = LocalDate.parse(request.getGoalEndt());
            // 날짜 범위 검증 결과를 반환함
            return !startDate.isAfter(endDate);
        } catch (DateTimeParseException exception) {
            // 형식이 맞지 않는 날짜는 저장 요청에서 제외함
            return false;
        }
    }

    /**
     * 모임 독서 수정 요청의 도서 필드와 목표 기간을 정규화하고 검증함
     *
     * @author Hanwon.Jang
     * @param request 검증할 모임 독서 수정 요청
     * @return 수정 가능한 요청이면 true
     */
    private boolean isValidReadingUpdate(ReadingClubDto.ReadingUpdateReqDto request) {

        // 외부 도서 검색 결과와 기간을 서버 저장 규격에 맞춰 정규화함
        request.setBookTitl(StringUtil.normalizePlainText(request.getBookTitl(), 500));
        request.setBookAthr(StringUtil.normalizePlainText(request.getBookAthr(), 500));
        request.setBookPubl(StringUtil.normalizePlainText(request.getBookPubl(), 500));
        request.setBookIsbn(StringUtil.normalizePlainText(request.getBookIsbn(), 100));
        request.setBookCvim(StringUtil.normalizePlainText(request.getBookCvim(), 1000));
        request.setBookDesc(StringUtil.normalizePlainText(request.getBookDesc(), 4000));
        request.setPublDate(StringUtil.normalizePlainText(request.getPublDate(), 10));
        request.setGoalStdt(StringUtil.normalizePlainText(request.getGoalStdt(), 10));
        request.setGoalEndt(StringUtil.normalizePlainText(request.getGoalEndt(), 10));

        // 도서 연결과 목표 기간 수정에 필요한 필수값을 모두 요구함
        if (StringUtil.hasEmpty(request.getBookTitl(), request.getBookAthr(), request.getBookPubl()
                              , request.getBookIsbn(), request.getBookCvim(), request.getBookDesc()
                              , request.getGoalStdt(), request.getGoalEndt())) {
            // 필수값 누락을 검증 실패로 반환함
            return false;
        }

        try {
            // ISO 날짜만 허용하고 시작일이 종료일보다 늦지 않은지 확인함
            LocalDate startDate = LocalDate.parse(request.getGoalStdt());
            LocalDate endDate = LocalDate.parse(request.getGoalEndt());
            // 날짜 범위 검증 결과를 반환함
            return !startDate.isAfter(endDate);
        } catch (DateTimeParseException exception) {
            // 형식이 맞지 않는 날짜는 수정 요청에서 제외함
            return false;
        }
    }

    /**
     * 모임 독서 회차의 도서와 목표 기간으로 멤버별 읽는 중 독후감을 구성함
     *
     * @author Hanwon.Jang
     * @param userNumb 자동 생성 대상 사용자 번호
     * @param request 모임 독서 회차 정보
     * @param reportColor 기본 책갈피 색상 코드
     * @return 자동 생성할 읽는 중 독후감
     */
    private ReportDto toReadingReport(Long userNumb, ReadingClubDto.ReadingCreateReqDto request
                                     , String reportColor) {

        // 자동 생성 독후감 DTO를 구성함
        ReportDto report = new ReportDto();
        report.setUserNumb(userNumb);
        report.setBookNumb(request.getBookNumb());
        report.setLangCode(request.getLangCode());
        report.setReptStat(Constant.REPORT_STAT_READ);
        report.setReptStdt(request.getGoalStdt());
        report.setReptEndt(request.getGoalEndt());
        report.setReptGrde("0");
        report.setReptColr(reportColor);
        report.setPubcYsno(Constant.COMM_NO);
        report.setLikeAlimYsno(Constant.COMM_YES);
        report.setReplyAlimYsno(Constant.COMM_YES);
        report.setReptCntn("");
        // 동일한 목표 기간의 읽는 중 독후감을 반환함
        return report;
    }

    private List<String> normalizeTextList(List<String> values, int maxLength) {
        // Null 목록은 빈 목록으로 정규화함
        if (StringUtil.isEmpty(values)) {
            // 공통 빈 목록을 반환함
            return List.of();
        }

        // 정규화된 문구를 담을 새 목록을 생성함
        List<String> result = new ArrayList<>();
        // 각 문구를 같은 최대 길이로 정규화함
        for (String value : values) {
            // 정규화한 문구를 순서대로 추가함
            result.add(StringUtil.normalizePlainText(value, maxLength));
        }

        // 정규화된 새 목록을 반환함
        return result;
    }

    private boolean hasEmptyText(List<String> values) {
        // 모든 문구의 빈 값 여부를 검사함
        for (String value : values) {
            // 하나라도 비어 있으면 유효하지 않음
            if (StringUtil.isEmpty(value)) {
                // 빈 문구가 있음을 반환함
                return true;
            }
        }

        // 빈 문구가 없음을 반환함
        return false;
    }

    private String getListValue(List<String> values, int index) {
        // 요청한 순서가 목록 안에 있을 때만 값을 반환함
        if (!StringUtil.isEmpty(values) && index < values.size()) {
            // 해당 순서의 문구를 반환함
            return values.get(index);
        }

        // 선택 항목이 없는 고정 컬럼에는 Null을 반환함
        return null;
    }

    /**
     * 이전 화면 요청의 도서 정보 언어를 한국어로 보정함
     *
     * @author SeungHyeon.Kang
     * @param bookDto 도서 정보 언어를 확인할 요청 DTO
     */
    private void setDefaultBookLanguage(BookDto bookDto) {
        // 언어별 검색 결과는 전달값을 유지하고 기존 요청만 한국어 도서 정보로 처리함
        if (StringUtil.isEmpty(bookDto.getLangCode())) {
            bookDto.setLangCode("ko");
        }
    }
}
