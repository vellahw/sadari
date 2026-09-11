package org.our.sadari.report.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.our.sadari.book.mapper.BookMapper;
import org.our.sadari.global.common.constant.Constant;
import org.our.sadari.global.common.dto.PageDto;
import org.our.sadari.global.common.exception.CustomException;
import org.our.sadari.global.common.code.util.CodeUtil;
import org.our.sadari.global.common.result.ResultData;
import org.our.sadari.global.common.service.BadWordDetectionService;
import org.our.sadari.global.common.util.LocaleUtil;
import org.our.sadari.global.common.util.DateUtil;
import org.our.sadari.global.common.util.MessageUtils;
import org.our.sadari.global.common.util.StringUtil;
import org.our.sadari.global.common.util.XssUtil;
import org.our.sadari.global.common.result.ResultEnum;
import org.our.sadari.myPage.dto.MonthlyReadingSummaryDto;
import org.our.sadari.myPage.dto.ReadingGoalDto;
import org.our.sadari.myPage.dto.ReadingSummaryQueryDto;
import org.our.sadari.report.dto.ReportAlimDto;
import org.our.sadari.report.dto.ReportDto;
import org.our.sadari.report.mapper.ReportMapper;
import org.our.sadari.social.dto.SocialDto;
import org.our.sadari.social.mapper.SocialMapper;
import org.our.sadari.user.dto.UserSettingDto;
import org.our.sadari.user.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * fileName       : ReportServiceImpl
 * author         : SeungHyeon.Kang
 * date           : 2026-07-17
 * description    : 독후감과 독서 목표 업무 로직을 구현함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-07-17        SeungHyeon.Kang    최초 생성
 * 2026-07-30        SeungHyeon.Kang    독후감 별점 0.5점 단위 검증 추가
 * 2026-08-01        SeungHyeon.Kang    최근 독후감·공개 정책 추가
 * 2026-08-04        SeungHyeon.Kang    독서 요약 공개 범위 조건 추가
 * 2026-08-14        SeungHyeon.Kang    공개 독후감 팔로우 작성자 우선 조회 반영
 * 2026-08-15        SeungHyeon.Kang    공개 독후감 조회·정렬 추가
 * 2026-08-21        SeungHyeon.Kang    독후감별 좋아요·댓글 알림 설정 추가
 * 2026-09-07        SeungHyeon.Kang    독후감 작성 언어와 공개 번역 가능 여부 반영
 * 2026-09-11        HanWon.Jang        진행 중인 모임 독후감 삭제 시 중도하차 처리 추가
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    // 홈과 공개 목록이 한 번에 조회할 화면 항목 수
    private static final int PAGE_SIZE = 12;

    // Report 데이터 접근 객체
    private final ReportMapper reportMapper;
    // Social 데이터 접근 객체
    private final SocialMapper socialMapper;
    // Book 데이터 접근 객체
    private final BookMapper bookMapper;
    // User 설정 데이터 접근 객체
    private final UserMapper userMapper;
    // 공통코드 캐시 조회 객체
    private final CodeUtil codeUtil;
    // BadWordDetection 업무 처리 서비스
    private final BadWordDetectionService badWordDetectionService;
    // 공개 독후감 번역 가능 여부 처리 서비스
    private final ReportTranslationService reportTranslationService;
    private static final DateTimeFormatter GOAL_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    // 목표 주간 FIELDS 설정값
    private static final WeekFields GOAL_WEEK_FIELDS = WeekFields.ISO;
    // 주간 목표 최대 UPDATE 건수 설정값
    private static final int WEEK_GOAL_MAX_UPDATE_COUNT = 1;
    // 월간 목표 최대 UPDATE 건수 설정값
    private static final int MONTH_GOAL_MAX_UPDATE_COUNT = 3; // 월간 목표는 한 달 단위로 조정하므로 목표 내리기를 최대 3회까지 허용함
    // 연간 목표 최대 UPDATE 건수 설정값
    private static final int YEAR_GOAL_MAX_UPDATE_COUNT = 5; // 연간 목표는 장기 목표이므로 목표 내리기를 최대 5회까지 허용함
    // 주간 목표 LOCK REMAINING 일수 설정값
    private static final int WEEK_GOAL_LOCK_REMAINING_DAYS = 3; // 주간 목표는 해당 주가 3일 남은 시점부터 목표 내리기를 잠금
    // 월간 목표 LOCK REMAINING 일수 설정값
    private static final int MONTH_GOAL_LOCK_REMAINING_DAYS = 7; // 월간 목표는 해당 월이 7일 남은 시점부터 목표 내리기를 잠금
    // 독후감 FIELD 상태 키 설정값
    private static final String REPORT_FIELD_STATUS_KEY = "common.report.field.status";
    // 독후감 FIELD 시작 날짜 키 설정값
    private static final String REPORT_FIELD_START_DATE_KEY = "common.report.field.startDate";
    // 독후감 FIELD 종료 날짜 키 설정값
    private static final String REPORT_FIELD_END_DATE_KEY = "common.report.field.endDate";
    // 독후감 FIELD 평점 키 설정값
    private static final String REPORT_FIELD_GRADE_KEY = "common.report.field.grade";
    // 독후감 FIELD COLOR 키 설정값
    private static final String REPORT_FIELD_COLOR_KEY = "common.report.field.color";
    // 독후감 FIELD 내용 키 설정값
    private static final String REPORT_FIELD_CONTENT_KEY = "common.report.field.content";
    // 독후감 별점 최대값
    private static final BigDecimal REPORT_GRADE_MAX = BigDecimal.valueOf(5);
    // 독후감 별점 허용 간격
    private static final BigDecimal REPORT_GRADE_STEP = new BigDecimal("0.5");

    /**
     * 로그인 사용자의 독후감 목록을 검색어와 정렬 조건에 맞춰 조회함
     * 검색어는 HTML entity를 일반 텍스트로 보정한 뒤 Mapper에 전달함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @param bookKeyword 책 제목 또는 작가명 검색어
     * @param sortType 목록 정렬 유형
     * @return 독후감 목록 조회 결과
     */
    @Override
    public ResultData getBookList(Long userNumb, String bookKeyword, String sortType) {
        // 독후감 또는 독서 목표 처리 데이터를 담을 객체를 생성함
        ReportDto reportDto = new ReportDto();
        // UserNumb 업무 값을 reportDto DTO에 설정함
        reportDto.setUserNumb(userNumb);
        // BookKeyword 업무 값을 reportDto DTO에 설정함
        reportDto.setBookKeyword(StringUtil.normalizePlainText(bookKeyword));
        // SortType 업무 값을 reportDto DTO에 설정함
        reportDto.setSortType(normalizeListSortType(sortType));
        // ReptStat 업무 값을 reportDto DTO에 설정함
        reportDto.setReptStat(Constant.REPORT_STAT_READ);

        // ReportList 데이터를 DB에서 조회함
        List<ReportDto> list = reportMapper.getReportList(reportDto);
        // 로그인 사용자의 독후감 목록을 검색어와 정렬 조건에 맞춰 조회 결과를 성공 응답으로 반환함
        return ResultData.success(list);
    }

    /**
     * 로그인 사용자의 독후감을 페이지 단위로 조회함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @param bookKeyword 책 제목 또는 작가명 검색어
     * @param sortType 목록 정렬 유형
     * @param page 조회할 페이지 번호
     * @return 현재 페이지 독후감과 다음 페이지 여부
     */
    @Override
    public ResultData getBookPage(Long userNumb, String bookKeyword, String sortType, int page) {
        // 인증 사용자 번호가 없으면 다른 사용자의 독후감 목록을 조회하지 않음
        if (StringUtil.isEmpty(userNumb)) {
            // "인증에 실패했어요.\n다시 로그인 해주세요."
            return ResultData.fail(ResultEnum.AUTH_FAIL);
        }

        // 요청 페이지를 첫 페이지 이상으로 보정함
        int normalizedPage = Math.max(page, 1);
        // 페이지 조회 조건을 담을 객체를 생성함
        ReportDto reportDto = new ReportDto();
        // 로그인 사용자 번호를 조회 조건으로 설정함
        reportDto.setUserNumb(userNumb);
        // 검색어를 화면 표시 제어문자 없이 조회 조건으로 설정함
        reportDto.setBookKeyword(StringUtil.normalizePlainText(bookKeyword));
        // 허용된 정렬 유형을 조회 조건으로 설정함
        reportDto.setSortType(normalizeListSortType(sortType));
        // 읽는 중 여부 표시 기준 상태를 설정함
        reportDto.setReptStat(Constant.REPORT_STAT_READ);
        // 현재 페이지의 시작 위치를 조회 조건으로 설정함
        reportDto.setPageOffset((normalizedPage - 1) * PAGE_SIZE);
        // 다음 페이지 존재 여부를 판정할 한 건을 추가해 조회함
        reportDto.setPageLimit(PAGE_SIZE + 1);
        // 페이지 조건으로 제한한 독후감 목록을 조회함
        List<ReportDto> searchedList = reportMapper.getReportList(reportDto);
        // Mapper가 빈 값을 반환해도 페이지 응답을 유지하도록 빈 목록으로 보정함
        List<ReportDto> safeList = StringUtil.isEmpty(searchedList) ? List.of() : searchedList;
        // 제한 건수보다 한 건 더 조회되었는지 다음 페이지 여부로 판정함
        boolean hasNext = safeList.size() > PAGE_SIZE;
        // 화면에는 현재 페이지 크기만 전달함
        List<ReportDto> visibleList = hasNext ? safeList.subList(0, PAGE_SIZE) : safeList;
        // 현재 페이지 독후감과 다음 페이지 여부를 반환함
        return ResultData.success(new PageDto<>(visibleList, normalizedPage, hasNext));
    }

    /**
     * 로그인 사용자가 동일 ISBN으로 가장 최근에 작성한 독후감을 조회함
     * 조회 결과가 없으면 새 독후감 작성 흐름을 유지할 수 있도록 성공 응답에 빈 데이터를 담음
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @param bookIsbn 조회할 도서 ISBN
     * @return 동일 ISBN의 최근 독후감 조회 결과
     */
    @Override
    public ResultData getReportByIsbnDtl(Long userNumb, String bookIsbn) {
        // ISBN이 없으면 기존 독후감과 선택한 도서를 안전하게 비교할 수 없음
        if (StringUtil.isEmpty(bookIsbn)) {
            // "조회 결과가 없어요."
            return ResultData.fail(ResultEnum.COMMON_NO_DATA);
        }

        // 동일 ISBN의 기존 독후감 조회 조건을 담을 객체를 생성함
        ReportDto reportDto = new ReportDto();
        // 로그인 사용자 본인의 독후감만 조회하도록 사용자 번호를 설정함
        reportDto.setUserNumb(userNumb);
        // 외부 도서 검색 결과의 ISBN을 일반 텍스트로 보정해 조회 조건에 설정함
        reportDto.setBookIsbn(StringUtil.normalizePlainText(bookIsbn));
        // 가장 최근에 작성한 동일 ISBN 독후감 조회 결과를 성공 응답으로 반환함
        return ResultData.success(reportMapper.getReportByIsbnDtl(reportDto));
    }

    /**
     * 본인 또는 다른 사용자 화면에 표시할 주간, 월간, 연간 독서량 요약과 목표 달성 정보를 조회함
     * 현재 기간과 직전 기간을 같은 기준으로 비교해 증감값과 펼침 목록을 함께 구성함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 조회할 사용자 번호
     * @param pubcYsno 다른 사용자 조회에 적용할 독후감 공개 여부
     * @return 독서량 요약, 목표 달성률, 목표 달성 횟수, 기간별 독후감 목록
     */
    @Override
    public ResultData getMonthlyReadingSummary(Long userNumb, String pubcYsno) {
        // 독서량과 목표 기간 계산의 기준 날짜를 조회함
        LocalDate today = LocalDate.now();
        // 통합 집계 SQL에 전달할 기간과 공통코드 조건을 생성함
        ReadingSummaryQueryDto queryReq = getReadingSummaryQueryReq(userNumb, pubcYsno, today);
        // 기간별 독서량과 목표 및 누적 달성 횟수를 한 번에 조회함
        ReadingSummaryQueryDto queryResult = reportMapper.getReadingSummary(queryReq);

        // 집계 SQL이 결과를 반환하지 않은 예외 상황에서도 빈 요약을 구성할 수 있도록 보정함
        if (StringUtil.isEmpty(queryResult)) {
            // 집계 기본값을 담을 객체를 생성함
            queryResult = new ReadingSummaryQueryDto();
        }

        // 통합 집계 결과를 화면 응답 형식으로 변환함
        MonthlyReadingSummaryDto summary = getReadingSummaryResponse(queryResult, today);
        // 다른 사용자 화면에서는 소유자의 독서 목표 공개 설정을 적용함
        applyReadingGoalPrivacy(summary, userNumb, pubcYsno);
        // 현재 읽는 책과 올해 완료한 책을 한 번에 조회함
        List<ReportDto> reportList = reportMapper.getReadingSummaryList(queryReq);
        // 한 번 조회한 목록을 현재 주와 월 및 연도 화면 목록으로 분류함
        applyReadingSummary(summary, reportList, today);

        // 마이페이지의 기간별 독서량과 목표 달성 요약을 반환함
        return ResultData.success(summary);
    }

    /** 다른 사용자에게 비공개인 독서 목표와 달성 정보를 응답에서 제거함 */
    private void applyReadingGoalPrivacy(MonthlyReadingSummaryDto summary, Long userNumb, String pubcYsno) {
        if (StringUtil.isEmpty(pubcYsno)) {
            summary.setGoalPublicYsno(Constant.COMM_YES);
            return;
        }

        UserSettingDto setting = userMapper.getUserSettingDtl(userNumb);
        boolean isPublic = !StringUtil.isEmpty(setting)
                && Constant.COMM_YES.equals(setting.getReadingGoalYsno());
        summary.setGoalPublicYsno(isPublic ? Constant.COMM_YES : Constant.COMM_NO);
        if (isPublic) {
            return;
        }

        summary.setWeekGoalCnt(null);
        summary.setMonthGoalCnt(null);
        summary.setYearGoalCnt(null);
        summary.setPreviousWeekGoalCnt(null);
        summary.setPreviousMonthGoalCnt(null);
        summary.setPreviousYearGoalCnt(null);
        summary.setWeekGoalRate(0);
        summary.setMonthGoalRate(0);
        summary.setYearGoalRate(0);
        summary.setWeekGoalSet(false);
        summary.setMonthGoalSet(false);
        summary.setYearGoalSet(false);
        summary.setWeekGoalAchvCnt(0);
        summary.setMonthGoalAchvCnt(0);
        summary.setYearGoalAchvCnt(0);
        summary.setTotalGoalAchvCnt(0);
    }

    /**
     * 독서 요약 통합 조회에 사용할 기간과 공통코드 조건을 생성함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 조회할 사용자 번호
     * @param pubcYsno 다른 사용자 조회에 적용할 독후감 공개 여부
     * @param today 기간 계산 기준일
     * @return 독서 요약 통합 조회 조건
     */
    private ReadingSummaryQueryDto getReadingSummaryQueryReq(Long userNumb, String pubcYsno, LocalDate today) {

        // 현재 주 시작일을 계산함
        LocalDate currentWeekStart = today.with(GOAL_WEEK_FIELDS.dayOfWeek(), 1);
        // 현재 월 시작일을 계산함
        LocalDate currentMonthStart = today.withDayOfMonth(1);
        // 현재 연도 시작일을 계산함
        LocalDate currentYearStart = today.withDayOfYear(1);
        // 통합 조회 조건을 담을 객체를 생성함
        ReadingSummaryQueryDto req = new ReadingSummaryQueryDto();
        // 조회할 사용자 번호를 설정함
        req.setUserNumb(userNumb);
        // 집계에 사용할 완료 독서 상태를 설정함
        req.setDoneStat(Constant.REPORT_STAT_DONE);
        // 현재 읽는 책 조회에 사용할 독서 상태를 설정함
        req.setReadStat(Constant.REPORT_STAT_READ);
        // 다른 사용자 화면에서 공개 독후감만 집계하도록 공개 여부를 설정함
        req.setPubcYsno(pubcYsno);
        // 주간 목표 유형을 설정함
        req.setWeekGoalType(Constant.GOAL_TYPE_WEEK);
        // 월간 목표 유형을 설정함
        req.setMonthGoalType(Constant.GOAL_TYPE_MONTH);
        // 연간 목표 유형을 설정함
        req.setYearGoalType(Constant.GOAL_TYPE_YEAR);
        // 현재 주 시작일을 설정함
        req.setCurrentWeekStart(currentWeekStart.toString());
        // 다음 주 시작일을 설정함
        req.setNextWeekStart(currentWeekStart.plusWeeks(1).toString());
        // 이전 주 시작일을 설정함
        req.setPreviousWeekStart(currentWeekStart.minusWeeks(1).toString());
        // 현재 월 시작일을 설정함
        req.setCurrentMonthStart(currentMonthStart.toString());
        // 다음 월 시작일을 설정함
        req.setNextMonthStart(currentMonthStart.plusMonths(1).toString());
        // 이전 월 시작일을 설정함
        req.setPreviousMonthStart(currentMonthStart.minusMonths(1).toString());
        // 현재 연도 시작일을 설정함
        req.setCurrentYearStart(currentYearStart.toString());
        // 이전 연도 시작일을 설정함
        req.setPreviousYearStart(currentYearStart.minusYears(1).toString());
        // 다음 연도 시작일을 설정함
        req.setNextYearStart(currentYearStart.plusYears(1).toString());
        // 현재 주 목표 기준값을 설정함
        req.setCurrentWeekGoalDate(getGoalDate(currentWeekStart, Constant.GOAL_TYPE_WEEK));
        // 이전 주 목표 기준값을 설정함
        req.setPreviousWeekGoalDate(getGoalDate(currentWeekStart.minusWeeks(1), Constant.GOAL_TYPE_WEEK));
        // 현재 월 목표 기준값을 설정함
        req.setCurrentMonthGoalDate(getGoalDate(currentMonthStart, Constant.GOAL_TYPE_MONTH));
        // 이전 월 목표 기준값을 설정함
        req.setPreviousMonthGoalDate(getGoalDate(currentMonthStart.minusMonths(1), Constant.GOAL_TYPE_MONTH));
        // 현재 연도 목표 기준값을 설정함
        req.setCurrentYearGoalDate(getGoalDate(currentYearStart, Constant.GOAL_TYPE_YEAR));
        // 이전 연도 목표 기준값을 설정함
        req.setPreviousYearGoalDate(getGoalDate(currentYearStart.minusYears(1), Constant.GOAL_TYPE_YEAR));

        // 기간과 목표 기준값이 설정된 통합 조회 조건을 반환함
        return req;
    }

    /**
     * 통합 집계 결과를 마이페이지 독서 요약 응답으로 변환함
     *
     * @author SeungHyeon.Kang
     * @param result 통합 집계 결과
     * @param today 기간 계산 기준일
     * @return 마이페이지 독서 요약 응답
     */
    private MonthlyReadingSummaryDto getReadingSummaryResponse(ReadingSummaryQueryDto result, LocalDate today) {

        // 화면 응답에 사용할 독서 요약 객체를 생성함
        MonthlyReadingSummaryDto summary = new MonthlyReadingSummaryDto();
        // 주간 표시 코드를 설정함
        summary.setWeekCode(Constant.GOAL_TYPE_WEEK);
        // 현재 주 완료 권수를 설정함
        summary.setCurrentWeekCount(result.getCurrentWeekCount());
        // 이전 주 완료 권수를 설정함
        summary.setPreviousWeekCount(result.getPreviousWeekCount());
        // 주간 완료 권수 차이를 설정함
        summary.setWeekCountDiff(result.getCurrentWeekCount() - result.getPreviousWeekCount());
        // 현재 월 표시 코드를 설정함
        summary.setMonthCode(today.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase(Locale.ENGLISH));
        // 현재 월 완료 권수를 설정함
        summary.setCurrentMonthCount(result.getCurrentMonthCount());
        // 이전 월 완료 권수를 설정함
        summary.setPreviousMonthCount(result.getPreviousMonthCount());
        // 월간 완료 권수 차이를 설정함
        summary.setCountDiff(result.getCurrentMonthCount() - result.getPreviousMonthCount());
        // 현재 연도 표시 코드를 설정함
        summary.setYearCode(String.valueOf(today.getYear()));
        // 현재 연도 완료 권수를 설정함
        summary.setCurrentYearCount(result.getCurrentYearCount());
        // 이전 연도 완료 권수를 설정함
        summary.setPreviousYearCount(result.getPreviousYearCount());
        // 연간 완료 권수 차이를 설정함
        summary.setYearCountDiff(result.getCurrentYearCount() - result.getPreviousYearCount());
        // 현재 주 목표 권수를 설정함
        summary.setWeekGoalCnt(result.getWeekGoalCnt());
        // 이전 주 목표 권수를 설정함
        summary.setPreviousWeekGoalCnt(result.getPreviousWeekGoalCnt());
        // 현재 월 목표 권수를 설정함
        summary.setMonthGoalCnt(result.getMonthGoalCnt());
        // 이전 월 목표 권수를 설정함
        summary.setPreviousMonthGoalCnt(result.getPreviousMonthGoalCnt());
        // 현재 연도 목표 권수를 설정함
        summary.setYearGoalCnt(result.getYearGoalCnt());
        // 이전 연도 목표 권수를 설정함
        summary.setPreviousYearGoalCnt(result.getPreviousYearGoalCnt());
        // 주간 목표 설정 여부를 설정함
        summary.setWeekGoalSet(!StringUtil.isEmpty(result.getWeekGoalCnt()));
        // 월간 목표 설정 여부를 설정함
        summary.setMonthGoalSet(!StringUtil.isEmpty(result.getMonthGoalCnt()));
        // 연간 목표 설정 여부를 설정함
        summary.setYearGoalSet(!StringUtil.isEmpty(result.getYearGoalCnt()));
        // 주간 목표 달성률을 설정함
        summary.setWeekGoalRate(getGoalRate(result.getCurrentWeekCount(), result.getWeekGoalCnt()));
        // 월간 목표 달성률을 설정함
        summary.setMonthGoalRate(getGoalRate(result.getCurrentMonthCount(), result.getMonthGoalCnt()));
        // 연간 목표 달성률을 설정함
        summary.setYearGoalRate(getGoalRate(result.getCurrentYearCount(), result.getYearGoalCnt()));
        // 주간 목표 수정 가능 횟수를 설정함
        summary.setWeekGoalRemainUpdateCnt(getGoalRemainUpdateCount(
                result.getWeekGoalCnt(), result.getWeekGoalUpdtCnt(), Constant.GOAL_TYPE_WEEK));
        // 월간 목표 수정 가능 횟수를 설정함
        summary.setMonthGoalRemainUpdateCnt(getGoalRemainUpdateCount(
                result.getMonthGoalCnt(), result.getMonthGoalUpdtCnt(), Constant.GOAL_TYPE_MONTH));
        // 연간 목표 수정 가능 횟수를 설정함
        summary.setYearGoalRemainUpdateCnt(getGoalRemainUpdateCount(
                result.getYearGoalCnt(), result.getYearGoalUpdtCnt(), Constant.GOAL_TYPE_YEAR));
        // 주간 목표 수정 가능 잔여 일수를 설정함
        summary.setWeekGoalEditableRemainDays(getGoalEditableRemainDays(today, Constant.GOAL_TYPE_WEEK));
        // 월간 목표 수정 가능 잔여 일수를 설정함
        summary.setMonthGoalEditableRemainDays(getGoalEditableRemainDays(today, Constant.GOAL_TYPE_MONTH));
        // 연간 목표 수정 가능 잔여 일수를 설정함
        summary.setYearGoalEditableRemainDays(getGoalEditableRemainDays(today, Constant.GOAL_TYPE_YEAR));
        // 주간 목표 수정 잠금 여부를 설정함
        summary.setWeekGoalUpdateLocked(isGoalUpdateLocked(today, Constant.GOAL_TYPE_WEEK));
        // 월간 목표 수정 잠금 여부를 설정함
        summary.setMonthGoalUpdateLocked(isGoalUpdateLocked(today, Constant.GOAL_TYPE_MONTH));
        // 연간 목표 수정 잠금 여부를 설정함
        summary.setYearGoalUpdateLocked(isGoalUpdateLocked(today, Constant.GOAL_TYPE_YEAR));
        // 주간 목표 달성 횟수를 설정함
        summary.setWeekGoalAchvCnt(result.getWeekGoalAchvCnt());
        // 월간 목표 달성 횟수를 설정함
        summary.setMonthGoalAchvCnt(result.getMonthGoalAchvCnt());
        // 연간 목표 달성 횟수를 설정함
        summary.setYearGoalAchvCnt(result.getYearGoalAchvCnt());
        // 전체 목표 달성 횟수를 설정함
        summary.setTotalGoalAchvCnt(
                result.getWeekGoalAchvCnt() + result.getMonthGoalAchvCnt() + result.getYearGoalAchvCnt());

        // 화면 응답 형식으로 변환한 독서 요약을 반환함
        return summary;
    }

    /**
     * 한 번 조회한 독후감 목록을 현재 읽는 책과 주간 및 월간 및 연간 목록으로 분류함
     *
     * @author SeungHyeon.Kang
     * @param summary 목록을 설정할 독서 요약 응답
     * @param reportList 현재 읽는 책과 올해 완료한 책 목록
     * @param today 기간 계산 기준일
     */
    private void applyReadingSummary(MonthlyReadingSummaryDto summary, List<ReportDto> reportList
                                          , LocalDate today) {

        // 현재 읽는 책을 담을 목록을 생성함
        List<ReportDto> currentReadingReports = new ArrayList<>();
        // 현재 주에 완료한 책을 담을 목록을 생성함
        List<ReportDto> currentWeekReports = new ArrayList<>();
        // 현재 월에 완료한 책을 담을 목록을 생성함
        List<ReportDto> currentMonthReports = new ArrayList<>();
        // 현재 연도에 완료한 책을 담을 목록을 생성함
        List<ReportDto> currentYearReports = new ArrayList<>();
        // 현재 주 시작일을 계산함
        LocalDate currentWeekStart = today.with(GOAL_WEEK_FIELDS.dayOfWeek(), 1);
        // 현재 월 시작일을 계산함
        LocalDate currentMonthStart = today.withDayOfMonth(1);
        // 현재 연도 시작일을 계산함
        LocalDate currentYearStart = today.withDayOfYear(1);

        // 조회 결과가 있는 경우에만 각 화면 기간으로 목록을 분류함
        if (!StringUtil.isEmpty(reportList)) {
            // 현재 읽는 책과 완료한 책을 상태 및 종료일 기준으로 순차 분류함
            for (ReportDto report : reportList) {
                // 읽는 중인 독후감은 완료 기간과 관계없이 현재 읽는 책 목록에 포함함
                if (Constant.REPORT_STAT_READ.equals(report.getReptStat())) {
                    // 현재 읽는 책 목록에 독후감을 추가함
                    currentReadingReports.add(report);

                    continue;
                }

                // 완료 상태가 아니거나 종료일이 없는 데이터는 기간별 완료 목록에서 제외함
                if (!Constant.REPORT_STAT_DONE.equals(report.getReptStat())
                        || StringUtil.isEmpty(report.getReptEndt())) {

                    continue;
                }

                // 현재 연도에 완료한 독후감을 연간 목록에 추가함
                if (isReportInPeriod(report.getReptEndt(), currentYearStart, currentYearStart.plusYears(1))) {
                    // 현재 연간 완료 목록에 독후감을 추가함
                    currentYearReports.add(report);
                }

                // 현재 월에 완료한 독후감을 월간 목록에 추가함
                if (isReportInPeriod(report.getReptEndt(), currentMonthStart, currentMonthStart.plusMonths(1))) {
                    // 현재 월간 완료 목록에 독후감을 추가함
                    currentMonthReports.add(report);
                }

                // 현재 주에 완료한 독후감을 주간 목록에 추가함
                if (isReportInPeriod(report.getReptEndt(), currentWeekStart, currentWeekStart.plusWeeks(1))) {
                    // 현재 주간 완료 목록에 독후감을 추가함
                    currentWeekReports.add(report);
                }
            }
        }

        // 현재 읽는 책 목록을 응답에 설정함
        summary.setCurrentReadingReports(currentReadingReports);
        // 현재 주 완료 목록을 응답에 설정함
        summary.setCurrentWeekReports(currentWeekReports);
        // 현재 월 완료 목록을 응답에 설정함
        summary.setCurrentMonthReports(currentMonthReports);
        // 현재 연도 완료 목록을 응답에 설정함
        summary.setCurrentYearReports(currentYearReports);
    }

    /**
     * 독서 종료일이 시작일 이상이고 종료 경계일 미만인지 확인함
     *
     * @author SeungHyeon.Kang
     * @param reptEndt 독서 종료일
     * @param periodStart 기간 시작일
     * @param periodEndExclusive 기간 종료 경계일
     * @return 지정한 기간에 포함되는지 여부
     */
    private boolean isReportInPeriod(String reptEndt, LocalDate periodStart, LocalDate periodEndExclusive) {

        // YYYY-MM-DD 형식의 문자열 순서가 날짜 순서와 같으므로 불필요한 날짜 변환 없이 범위를 비교함
        return reptEndt.compareTo(periodStart.toString()) >= 0
                && reptEndt.compareTo(periodEndExclusive.toString()) < 0;
    }

    /**
     * 목표 기준일과 목표 유형을 DB 조회용 GOAL_DATE 값으로 변환해 현재 목표를 조회함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @param targetDate 목표 기준일
     * @param goalType 주간, 월간, 연간 목표 유형
     * @return 저장된 목표 정보, 없으면 null
     */
    private ReadingGoalDto getReadingGoalDtl(Long userNumb, LocalDate targetDate, String goalType) {
        // 독서 목표 조회 또는 저장 조건을 담을 객체를 생성함
        ReadingGoalDto req = new ReadingGoalDto();
        // UserNumb 업무 값을 req DTO에 설정함
        req.setUserNumb(userNumb);
        // GoalDate 업무 값을 req DTO에 설정함
        req.setGoalDate(getGoalDate(targetDate, goalType));
        // GoalType 업무 값을 req DTO에 설정함
        req.setGoalType(goalType);
        // 목표 기준일과 목표 유형을 DB 조회용 GOAL_DATE 값으로 변환해 현재 목표를 조회 결과를 반환함
        return reportMapper.getReadingGoalDtl(req);
    }

    /**
     * 완료 독후감 수와 목표 권수를 비교해 화면 표시용 달성률을 계산함
     * 100%를 넘는 경우에도 진행 막대는 최대값으로 표시해야 하므로 100으로 제한함
     *
     * @author SeungHyeon.Kang
     * @param doneCount 완료 독후감 수
     * @param goalCount 목표 권수
     * @return 0부터 100까지의 달성률
     */
    private int getGoalRate(int doneCount, Integer goalCount) {
        // 목표 권수가 없거나 0 이하이면 달성률 계산이 불가능하므로 0%로 처리함
        if (StringUtil.isEmpty(goalCount) || goalCount <= 0) {
            // 완료 독후감 수와 목표 권수를 비교해 화면 표시용 달성률을 계산 결과를 반환함
            return 0;
        }

        // 완료 독후감 수와 목표 권수를 비교해 화면 표시용 달성률을 계산 결과를 반환함
        return Math.min(100, (int) Math.round((doneCount * 100.0) / goalCount));
    }

    /**
     * 목표 유형에 따라 TM_GOALXM.GOAL_DATE에 저장할 기준값을 만듦
     * 주간은 ISO week 기준 YYYYWW, 월간은 YYYYMM, 연간은 YYYY00 형식을 사용함
     *
     * @author SeungHyeon.Kang
     * @param targetDate 목표 기준일
     * @param goalType 목표 유형
     * @return 목표 기준값
     */
    private String getGoalDate(LocalDate targetDate, String goalType) {
        // 주간 목표는 ISO 주차 기준값을 사용해야 하므로 별도 변환 로직으로 분기함
        if (Constant.GOAL_TYPE_WEEK.equals(goalType)) {
            // 목표 유형에 따라 TM_GOALXM.GOAL_DATE에 저장할 기준값을 만든다 결과를 반환함
            return getGoalWeekDate(targetDate);
        }

        // 연간 목표는 월 정보가 필요 없으므로 YYYY00 형식으로 저장함
        if (Constant.GOAL_TYPE_YEAR.equals(goalType)) {
            // 목표 유형에 따라 TM_GOALXM.GOAL_DATE에 저장할 기준값을 만든다 결과를 반환함
            return targetDate.getYear() + "00";
        }

        // 목표 유형에 따라 TM_GOALXM.GOAL_DATE에 저장할 기준값을 만든다 결과를 반환함
        return YearMonth.from(targetDate).format(GOAL_MONTH_FORMATTER);
    }

    /**
     * ISO 주차 기준으로 주간 목표의 GOAL_DATE 값을 생성함
     * 연말과 연초가 겹치는 주차를 올바르게 처리하기 위해 week-based-year를 사용함
     *
     * @author SeungHyeon.Kang
     * @param targetDate 목표 기준일
     * @return YYYYWW 형식의 주간 목표 기준값
     */
    private String getGoalWeekDate(LocalDate targetDate) {
        // 지정한 키에 대응하는 값을 조회함
        int weekYear = targetDate.get(GOAL_WEEK_FIELDS.weekBasedYear());
        // 지정한 키에 대응하는 값을 조회함
        int weekNumber = targetDate.get(GOAL_WEEK_FIELDS.weekOfWeekBasedYear());
        // ISO 주차 기준으로 주간 목표의 GOAL_DATE 값을 생성 결과를 반환함
        return String.format("%04d%02d", weekYear, weekNumber);
    }

    /**
     * 주간, 월간, 연간 독서 목표를 한 번에 저장함
     * 목표를 올리는 것은 항상 허용하고, 목표를 낮추는 경우에만 기간과 횟수 제한을 적용함
     *
     * @author SeungHyeon.Kang
     * @param readingGoalDto 저장할 주간, 월간, 연간 목표 권수
     * @return 저장 후 갱신된 마이페이지 독서 요약 정보
     */
    private boolean isValidReadingGoal(ReadingGoalDto readingGoalDto) {
        // 주간, 월간, 연간 독서 목표를 한 번에 저장 결과를 반환함
        return !(StringUtil.isEmpty(readingGoalDto) || StringUtil.isEmpty(readingGoalDto.getWeekGoalCnt())
                // 필수 값이 비어 있는지 공통 기준으로 확인함
                || StringUtil.isEmpty(readingGoalDto.getMonthGoalCnt()) || StringUtil.isEmpty(readingGoalDto.getYearGoalCnt())
                // getWeekGoalCnt 조회로 후속 처리에 필요한 데이터를 가져옴
                || readingGoalDto.getWeekGoalCnt() <= 0 || readingGoalDto.getMonthGoalCnt() <= 0
                // getYearGoalCnt 조회로 후속 처리에 필요한 데이터를 가져옴
                || readingGoalDto.getYearGoalCnt() <= 0);
    }
    /**
     * 로그인 사용자의 독서 목표 권수를 저장함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 처리할 사용자 번호
     * @param readingGoalDto 저장할 독서 목표 기간과 권수
     * @return 업무 처리 성공 또는 실패 응답
     */
    @Override
    @Transactional
    public ResultData setReadingGoal(Long userNumb, ReadingGoalDto readingGoalDto) {
        // 주간, 월간, 연간 목표 중 하나라도 유효하지 않으면 저장 요청 전체를 거절함
        if (!isValidReadingGoal(readingGoalDto)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 목표 기간 계산의 기준이 되는 오늘 날짜를 조회함
        LocalDate today = LocalDate.now();
        // getWeekGoalCnt 조회로 후속 처리에 필요한 데이터를 가져옴
        ResultEnum weekResult = setReadingGoalByType(userNumb, today, Constant.GOAL_TYPE_WEEK, readingGoalDto.getWeekGoalCnt());
        // 주간 목표 저장 중 제한 규칙에 걸리면 이후 월간, 연간 저장을 진행하지 않음
        if (!StringUtil.isEmpty(weekResult)) {
            // 주간 독서 목표 저장 결과 코드에 연결된 사용자 메시지
            return ResultData.fail(weekResult);
        }

        // getMonthGoalCnt 조회로 후속 처리에 필요한 데이터를 가져옴
        ResultEnum monthResult = setReadingGoalByType(userNumb, today, Constant.GOAL_TYPE_MONTH, readingGoalDto.getMonthGoalCnt());
        // 월간 목표 저장 중 제한 규칙에 걸리면 이후 연간 저장을 진행하지 않음
        if (!StringUtil.isEmpty(monthResult)) {
            // 월간 독서 목표 저장 결과 코드에 연결된 사용자 메시지
            return ResultData.fail(monthResult);
        }

        // getYearGoalCnt 조회로 후속 처리에 필요한 데이터를 가져옴
        ResultEnum yearResult = setReadingGoalByType(userNumb, today, Constant.GOAL_TYPE_YEAR, readingGoalDto.getYearGoalCnt());
        // 연간 목표 저장 중 제한 규칙에 걸리면 실패 결과를 그대로 반환함
        if (!StringUtil.isEmpty(yearResult)) {
            // 연간 독서 목표 저장 결과 코드에 연결된 사용자 메시지
            return ResultData.fail(yearResult);
        }

        // 로그인 사용자의 독서 목표 권수를 저장 결과를 반환함
        return getMonthlyReadingSummary(userNumb, null);
    }

    /**
     * 직전 기간(지난주, 지난달, 작년)에 설정되어 있던 독서 목표 데이터를 조회하여 현재 기간의 목표로 일괄 복사함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인한 회원 번호
     * @return 복사 처리 결과 데이터 (복사된 목표가 없으면 실패 결과, 성공 시 최신 독서 요약 정보 반환)
     */
    @Override
    @Transactional
    public ResultData copyPreviousReadingGoal(Long userNumb) {
        // 1. 현재 시점 및 직전 주간, 월간, 연간의 시작 날짜 기준점을 계산함
        LocalDate today = LocalDate.now();                                                      // 실시간 현재 일자 획득
        // 목표 기간 계산에 사용할 기준 요일로 날짜를 조정함
        LocalDate currentWeekStart = today.with(GOAL_WEEK_FIELDS.dayOfWeek(), 1);               // ISO 기준 이번 주 월요일 날짜
        // 지난 주 목표 조회에 사용할 시작일을 계산함
        LocalDate previousWeekStart = currentWeekStart.minusWeeks(1);                           // ISO 기준 지난 주 월요일 날짜
        // 기준 월에서 필요한 일자로 날짜를 조정함
        LocalDate currentMonthStart = today.withDayOfMonth(1);                                  // 당월 1일 날짜
        // 지난 달 목표 조회에 사용할 시작일을 계산함
        LocalDate previousMonthStart = currentMonthStart.minusMonths(1);                        // 전월 1일 날짜
        // 연간 목표 조회 기준일을 연도의 첫날로 조정함
        LocalDate currentYearStart = today.withDayOfYear(1);                                    // 금년 1월 1일 날짜
        // 지난해 목표 조회에 사용할 시작일을 계산함
        LocalDate previousYearStart = currentYearStart.minusYears(1);                           // 전년 1월 1일 날짜

        // 2. 주간, 월간, 연간 목표 순으로 직전 기간의 목표를 복사 처리하고 성공한 총 건수를 누적함
        int copiedCount = 0;
        // copyPrevReadingGoal 호출로 이전 목표값을 새 목표에 반영함
        copiedCount += copyPrevReadingGoal(userNumb, today, currentWeekStart, previousWeekStart, Constant.GOAL_TYPE_WEEK);
        // copyPrevReadingGoal 호출로 이전 목표값을 새 목표에 반영함
        copiedCount += copyPrevReadingGoal(userNumb, today, currentMonthStart, previousMonthStart, Constant.GOAL_TYPE_MONTH);
        // copyPrevReadingGoal 호출로 이전 목표값을 새 목표에 반영함
        copiedCount += copyPrevReadingGoal(userNumb, today, currentYearStart, previousYearStart, Constant.GOAL_TYPE_YEAR);

        // 3. 복사된 목표가 단 1건도 없는 경우(이미 목표가 존재하거나 이전 목표 데이터가 없는 경우) 요청 실패로 응답함
        if (copiedCount == 0) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 직전 기간(지난주, 지난달, 작년)에 설정되어 있던 독서 목표 데이터를 조회하여 현재 기간의 목표로 일괄 복사한 결과를 반환함
        return getMonthlyReadingSummary(userNumb, null);
    }

    /**
     * 목표 타입별로 직전 기간의 목표 데이터를 검증하고 현재 기간의 목표로 단건 복사함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인한 회원 번호
     * @param today 현재 날짜
     * @param currentDate 현재 기간의 시작일
     * @param previousDate 직전 기간의 시작일
     * @param goalType 목표 타입 (WEEK / MONTH / YEAR)
     * @return 목표 복사 성공 여부 (성공: 1, 실패/스킵: 0)
     */
    private int copyPrevReadingGoal(Long userNumb, LocalDate today, LocalDate currentDate
                                            , LocalDate previousDate, String goalType) {
        // 1. 이미 현재 기간에 설정된 목표가 존재하는 경우 덮어쓰지 않고 즉시 스킵함
        ReadingGoalDto currentGoal = getReadingGoalDtl(userNumb, currentDate, goalType);
        // currentGoal 값이 비어 있을 때 후속 참조를 차단하기 위한 분기임
        if (!StringUtil.isEmpty(currentGoal)) {
            // 목표 타입별로 직전 기간의 목표 데이터를 검증하고 현재 기간의 목표로 단건 복사한 결과를 반환함
            return 0;
        }

        // 2. 직전 기간의 목표 데이터를 조회하여 값이 없거나 0 이하의 유효하지 않은 권수인 경우 스킵함
        ReadingGoalDto previousGoal = getReadingGoalDtl(userNumb, previousDate, goalType);
        // previousGoal 값이 비어 있을 때 후속 참조를 차단하기 위한 분기임
        if (StringUtil.isEmpty(previousGoal) || StringUtil.isEmpty(previousGoal.getGoalCnt())
                || previousGoal.getGoalCnt() <= 0) {
            // 목표 타입별로 직전 기간의 목표 데이터를 검증하고 현재 기간의 목표로 단건 복사한 결과를 반환함
            return 0;
        }

        // 3. 직전 기간의 목표 권수를 기반으로 현재 기간의 목표를 새로 등록함
        ResultEnum result = setReadingGoalByType(userNumb, today, goalType, previousGoal.getGoalCnt());

        // 목표 타입별로 직전 기간의 목표 데이터를 검증하고 현재 기간의 목표로 단건 복사한 결과를 반환함
        return StringUtil.isEmpty(result) ? 1 : 0;
    }

    /**
     * 목표 유형 하나에 대해 현재 목표와 신규 목표를 비교한 뒤 저장함
     * 같은 값이면 DB 갱신을 생략하고, 낮추는 값이면 별도 제한 검증을 수행함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @param today 현재 날짜
     * @param goalType 목표 유형
     * @param goalCnt 새 목표 권수
     * @return 저장을 막아야 하는 결과 코드, 정상 저장 가능하면 null
     */
    private ResultEnum setReadingGoalByType(Long userNumb, LocalDate today, String goalType
                                          , Integer goalCnt) {
        // getReadingGoalDtl 조회로 후속 처리에 필요한 데이터를 가져옴
        ReadingGoalDto currentGoal = getReadingGoalDtl(userNumb, today, goalType);

        // 현재 목표와 새 목표가 같으면 수정 횟수를 증가시키지 않기 위해 DB 갱신을 생략함
        if (!StringUtil.isEmpty(currentGoal) && goalCnt.equals(currentGoal.getGoalCnt())) {
            // 조회하거나 생성할 값이 없음을 반환함
            return null;
        }

        // validateReadingGoalDown 검증으로 잘못된 요청이 업무 로직에 진입하지 않도록 차단함
        ResultEnum validateResult = validateReadingGoalDown(currentGoal, today, goalType, goalCnt);
        // 목표 내리기 검증에서 실패 코드가 나오면 해당 코드를 Controller까지 전달함
        if (!StringUtil.isEmpty(validateResult)) {
            // 목표 유형 하나에 대해 현재 목표와 신규 목표를 비교한 뒤 저장 결과를 반환함
            return validateResult;
        }

        // 독서 목표 조회 또는 저장 조건을 담을 객체를 생성함
        ReadingGoalDto req = new ReadingGoalDto();
        // UserNumb 업무 값을 req DTO에 설정함
        req.setUserNumb(userNumb);
        // GoalDate 업무 값을 req DTO에 설정함
        req.setGoalDate(getGoalDate(today, goalType));
        // GoalType 업무 값을 req DTO에 설정함
        req.setGoalType(goalType);
        // GoalCnt 업무 값을 req DTO에 설정함
        req.setGoalCnt(goalCnt);
        // ReadingGoal 업무 값을 reportMapper DTO에 설정함
        reportMapper.setReadingGoal(req);
        // 조회하거나 생성할 값이 없음을 반환함
        return null;
    }

    /**
     * 목표 권수를 낮추는 요청인지 판단하고 낮추기 제한을 검증함
     * 신규 설정 또는 목표 올리기는 제한하지 않고, 낮추기만 횟수와 마감 기간을 적용함
     *
     * @author SeungHyeon.Kang
     * @param currentGoal 현재 저장된 목표
     * @param today 현재 날짜
     * @param goalType 목표 유형
     * @param goalCnt 새 목표 권수
     * @return 제한 위반 결과 코드, 통과하면 null
     */
    private ResultEnum validateReadingGoalDown(ReadingGoalDto currentGoal, LocalDate today, String goalType
                                             , Integer goalCnt) {
        // 신규 목표 설정이거나 목표를 올리는 요청이면 내리기 제한을 적용하지 않음
        if (StringUtil.isEmpty(currentGoal) || currentGoal.getGoalCnt() <= goalCnt) {
            // 조회하거나 생성할 값이 없음을 반환함
            return null;
        }

        // 목표 내리기 허용 횟수를 모두 사용한 경우 더 이상 목표를 낮출 수 없음
        if (getGoalUpdateLimit(goalType) <= getGoalUpdateCount(currentGoal)) {
            // 목표 권수를 낮추는 요청인지 판단하고 낮추기 제한을 검증 결과를 반환함
            return ResultEnum.COMMON_INVALID_REQUEST;
        }

        // 목표 내리기 가능 기간이 마감된 경우 목표를 낮출 수 없음
        if (isGoalUpdateLocked(today, goalType)) {
            // 목표 권수를 낮추는 요청인지 판단하고 낮추기 제한을 검증 결과를 반환함
            return ResultEnum.COMMON_INVALID_REQUEST;
        }

        // 조회하거나 생성할 값이 없음을 반환함
        return null;
    }

    /**
     * 목표 유형별 목표 내리기 가능 횟수를 반환함
     * 주간 1회, 월간 3회, 연간 5회 제한을 적용함
     *
     * @author SeungHyeon.Kang
     * @param goalType 목표 유형
     * @return 목표 내리기 허용 횟수
     */
    private int getGoalUpdateLimit(String goalType) {
        // 주간 목표는 ISO 주차 기준값을 사용해야 하므로 별도 변환 로직으로 분기함
        if (Constant.GOAL_TYPE_WEEK.equals(goalType)) {
            // 목표 유형별 목표 내리기 가능 횟수를 반환함
            return WEEK_GOAL_MAX_UPDATE_COUNT;
        }

        // 월간 목표는 주간보다 넓은 기간을 다루므로 3회까지 목표 내리기를 허용함
        if (Constant.GOAL_TYPE_MONTH.equals(goalType)) {
            // 목표 유형별 목표 내리기 가능 횟수를 반환함
            return MONTH_GOAL_MAX_UPDATE_COUNT;
        }

        // 목표 유형별 목표 내리기 가능 횟수를 반환함
        return YEAR_GOAL_MAX_UPDATE_COUNT;
    }

    /**
     * 목표 권수와 사용한 수정 횟수를 기준으로 목표 내리기 잔여 횟수를 계산함
     *
     * @author SeungHyeon.Kang
     * @param goalCnt 현재 목표 권수
     * @param goalUpdtCnt 사용한 목표 수정 횟수
     * @param goalType 목표 유형
     * @return 목표 내리기 잔여 횟수
     */
    private int getGoalRemainUpdateCount(Integer goalCnt, Integer goalUpdtCnt, String goalType) {

        // 저장된 목표가 아직 없으면 유형별 전체 내리기 횟수를 잔여 횟수로 표시함
        if (StringUtil.isEmpty(goalCnt)) {
            // 목표 유형에 허용된 전체 수정 횟수를 반환함
            return getGoalUpdateLimit(goalType);
        }

        // 사용 횟수가 없으면 0으로 보정해 남은 수정 횟수를 계산함
        int usedUpdateCount = StringUtil.isEmpty(goalUpdtCnt) ? 0 : goalUpdtCnt;

        // 목표 유형별 제한에서 사용한 횟수를 뺀 잔여 수정 횟수를 반환함
        return Math.max(0, getGoalUpdateLimit(goalType) - usedUpdateCount);
    }

    /**
     * 목표 내리기 사용 횟수가 null이면 0으로 보정함
     *
     * @author SeungHyeon.Kang
     * @param currentGoal 현재 목표
     * @return 목표 내리기 사용 횟수
     */
    private int getGoalUpdateCount(ReadingGoalDto currentGoal) {
        // 목표 내리기 사용 횟수가 null이면 0으로 보정 결과를 반환함
        return StringUtil.isEmpty(currentGoal.getUpdtCntt()) ? 0 : currentGoal.getUpdtCntt();
    }

    /**
     * 목표 내리기 가능 기간이 지났는지 판단함
     * 주간은 해당 주 종료 3일 전부터, 월간은 월 종료 7일 전부터, 연간은 12월 1일부터 내리기를 막음
     *
     * @author SeungHyeon.Kang
     * @param today 현재 날짜
     * @param goalType 목표 유형
     * @return 목표 내리기 마감 여부
     */
    private boolean isGoalUpdateLocked(LocalDate today, String goalType) {
        // 주간 목표는 ISO 주차 기준값을 사용해야 하므로 별도 변환 로직으로 분기함
        if (Constant.GOAL_TYPE_WEEK.equals(goalType)) {
            // 목표 기간 계산에 사용할 기준 요일로 날짜를 조정함
            LocalDate weekLastDay = today.with(GOAL_WEEK_FIELDS.dayOfWeek(), 7);
            // 목표 내리기 가능 기간이 지났는지 판단 결과를 반환함
            return ChronoUnit.DAYS.between(today, weekLastDay) <= WEEK_GOAL_LOCK_REMAINING_DAYS;
        }

        // 월간 목표는 월 종료 7일 전부터 내리기를 막기 위해 월 마지막 날을 기준으로 계산함
        if (Constant.GOAL_TYPE_MONTH.equals(goalType)) {
            // 기준 월에서 필요한 일자로 날짜를 조정함
            LocalDate monthLastDay = today.withDayOfMonth(today.lengthOfMonth());
            // 목표 내리기 가능 기간이 지났는지 판단 결과를 반환함
            return ChronoUnit.DAYS.between(today, monthLastDay) <= MONTH_GOAL_LOCK_REMAINING_DAYS;
        }

        // 목표 내리기 가능 기간이 지났는지 판단 결과를 반환함
        return today.getMonthValue() == 12;
    }

    /**
     * 목표 내리기가 가능한 잔여 일수를 계산함
     * 기간이 이미 마감되었으면 음수가 내려가지 않도록 0으로 보정함
     *
     * @author SeungHyeon.Kang
     * @param today 현재 날짜
     * @param goalType 목표 유형
     * @return 목표 내리기 가능 잔여 일수
     */
    private int getGoalEditableRemainDays(LocalDate today, String goalType) {
        // 주간 목표는 ISO 주차 기준값을 사용해야 하므로 별도 변환 로직으로 분기함
        if (Constant.GOAL_TYPE_WEEK.equals(goalType)) {
            // 목표 기간 계산에 사용할 기준 요일로 날짜를 조정함
            LocalDate weekLastDay = today.with(GOAL_WEEK_FIELDS.dayOfWeek(), 7);
            // 목표 내리기가 가능한 잔여 일수를 계산 결과를 반환함
            return Math.max(0, (int) ChronoUnit.DAYS.between(today, weekLastDay) - WEEK_GOAL_LOCK_REMAINING_DAYS);
        }

        // 월간 목표의 내리기 가능 잔여 일수는 월 마지막 날에서 잠금 기준일을 뺀 값으로 계산함
        if (Constant.GOAL_TYPE_MONTH.equals(goalType)) {
            // 기준 월에서 필요한 일자로 날짜를 조정함
            LocalDate monthLastDay = today.withDayOfMonth(today.lengthOfMonth());
            // 목표 내리기가 가능한 잔여 일수를 계산 결과를 반환함
            return Math.max(0, (int) ChronoUnit.DAYS.between(today, monthLastDay) - MONTH_GOAL_LOCK_REMAINING_DAYS);
        }

        // 필요한 값으로 불변 객체를 생성함
        LocalDate yearLockDate = LocalDate.of(today.getYear(), 12, 1);
        // 목표 내리기가 가능한 잔여 일수를 계산 결과를 반환함
        return Math.max(0, (int) ChronoUnit.DAYS.between(today, yearLockDate));
    }

    /**
     * 로그인 사용자가 작성한 독후감 상세 정보와 연결된 도서 정보를 조회함
     * 사용자 번호를 조건에 포함해 다른 사용자의 비공개 독후감을 조회하지 못하도록 함
     *
     * @author SeungHyeon.Kang
     * @return 독후감 상세 조회 결과
     */
    private String normalizeListSortType(String sortType) {
        // 허용된 정렬값만 Mapper에 전달해 동적 정렬 조건이 임의로 확장되지 않게 함
        if (Constant.SORT_START_DATE_DESC.equals(sortType) || Constant.SORT_GRADE_DESC.equals(sortType)) {
            // 로그인 사용자가 작성한 독후감 상세 정보와 연결된 도서 정보를 조회 결과를 반환함
            return sortType;
        }

        // 로그인 사용자가 작성한 독후감 상세 정보와 연결된 도서 정보를 조회 결과를 반환함
        return Constant.SORT_END_DATE_DESC;
    }
    /**
     * 독후감 번호로 독후감과 도서 상세 정보를 조회함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 처리할 사용자 번호
     * @param reptNumb 조회하거나 수정할 독후감 번호
     * @return 업무 처리 성공 또는 실패 응답
     */
    @Override
    public ResultData getDetail(Long userNumb, Long reptNumb) {
        // 대상 독후감 번호가 없으면 상세, 수정, 삭제 대상을 특정할 수 없으므로 실패 처리함
        if (StringUtil.isEmpty(reptNumb)) {
            // "조회 결과가 없어요."
            return ResultData.fail(ResultEnum.COMMON_NO_DATA);
        }

        // 독후감 또는 독서 목표 처리 데이터를 담을 객체를 생성함
        ReportDto reportDto = new ReportDto();
        // UserNumb 업무 값을 reportDto DTO에 설정함
        reportDto.setUserNumb(userNumb);
        // ReptNumb 업무 값을 reportDto DTO에 설정함
        reportDto.setReptNumb(reptNumb);
        // Locale 업무 값을 reportDto DTO에 설정함
        reportDto.setLocale(LocaleUtil.getLocale());
        // ReptStat 업무 값을 reportDto DTO에 설정함
        reportDto.setReptStat(Constant.REPORT_STAT_DONE);

        // ReportDtl 데이터를 DB에서 조회함
        ReportDto detail = reportMapper.getReportDtl(reportDto);

        // 조회 결과가 없으면 존재하지 않거나 접근할 수 없는 독후감으로 판단함
        if (StringUtil.isEmpty(detail)) {
            // "조회 결과가 없어요."
            return ResultData.fail(ResultEnum.COMMON_NO_DATA);
        }

        // 독후감 번호로 독후감과 도서 상세 정보를 조회 결과를 성공 응답으로 반환함
        return ResultData.success(detail);
    }

    /**
     * ISBN 기준 활성 사용자의 공개 독후감을 요청한 정렬 기준으로 조회함
     * 로그인 사용자의 좋아요와 작성자 팔로우 여부를 함께 표시하기 위해 사용자 번호를 Mapper에 전달함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @param bookIsbn 조회할 도서 ISBN
     * @param sortType 공개 독후감 정렬 코드
     * @param reptStat 공개 독후감 상태 필터
     * @param page 조회할 페이지 번호
     * @return 공개 독후감 목록 조회 결과
     */
    @Override
    public ResultData getPublicReportsByIsbn(Long userNumb, String bookIsbn, String sortType
                                            , String reptStat, int page) {
        // ISBN이 없으면 도서를 특정할 수 없으므로 공개 독후감 또는 평균 별점을 조회하지 않음
        if (StringUtil.isEmpty(bookIsbn)) {
            // "조회 결과가 없어요."
            return ResultData.fail(ResultEnum.COMMON_NO_DATA);
        }

        // 독후감 또는 독서 목표 처리 데이터를 담을 객체를 생성함
        ReportDto reportDto = new ReportDto();
        // UserNumb 업무 값을 reportDto DTO에 설정함
        reportDto.setUserNumb(userNumb);
        // BookIsbn 업무 값을 reportDto DTO에 설정함
        reportDto.setBookIsbn(StringUtil.normalizePlainText(bookIsbn));
        // 외부 정렬 코드를 허용 목록과 비교할 수 있는 문자열로 정규화함
        String normalizedSortType = StringUtil.normalizePlainText(sortType);
        // 허용된 정렬 코드가 아니면 친구와 팔로잉 우선 기본순으로 보정함
        if (!Constant.SORT_RELATION_DESC.equals(normalizedSortType)
                && !Constant.SORT_LATEST_DESC.equals(normalizedSortType)
                && !Constant.SORT_GRADE_DESC.equals(normalizedSortType)
                && !Constant.SORT_LIKE_DESC.equals(normalizedSortType)) {
            normalizedSortType = Constant.SORT_RELATION_DESC;
        }

        // 검증된 정렬 코드를 Mapper 조건으로 설정함
        reportDto.setSortType(normalizedSortType);
        // 완료와 중단 상태만 서버 필터로 허용하고 전체 요청은 조건을 비움
        String normalizedReptStat = StringUtil.normalizePlainText(reptStat);

        // 완료 또는 중단 상태이면 페이지 원본 조회 단계에 적용함
        if (Constant.REPORT_STAT_DONE.equals(normalizedReptStat)
                || Constant.REPORT_STAT_STOP.equals(normalizedReptStat)) {
            // 검증된 독서 상태를 공개 목록 조회 조건으로 설정함
            reportDto.setReptStat(normalizedReptStat);
        }

        // 요청 페이지를 첫 페이지 이상으로 보정함
        int normalizedPage = Math.max(page, 1);
        // 현재 페이지의 시작 위치를 조회 조건으로 설정함
        reportDto.setPageOffset((normalizedPage - 1) * PAGE_SIZE);
        // 다음 페이지 존재 여부를 판정할 한 건을 추가해 조회함
        reportDto.setPageLimit(PAGE_SIZE + 1);
        // ISBN과 상태 및 정렬 조건으로 범위를 제한한 공개 독후감을 조회함
        List<ReportDto> searchedList = reportMapper.getPublicReportList(reportDto);
        // Mapper가 빈 값을 반환해도 페이지 응답을 유지하도록 빈 목록으로 보정함
        List<ReportDto> safeList = StringUtil.isEmpty(searchedList) ? List.of() : searchedList;
        // 제한 건수보다 한 건 더 조회되었는지 다음 페이지 여부로 판정함
        boolean hasNext = safeList.size() > PAGE_SIZE;
        // 화면에는 현재 페이지 크기만 전달함
        List<ReportDto> visibleList = hasNext ? safeList.subList(0, PAGE_SIZE) : safeList;
        // 현재 표시 언어와 월간 잔여량 및 캐시 상태로 카드별 번역 버튼 노출 여부를 설정함
        reportTranslationService.applyReportAvailability(visibleList);
        // ISBN 기준 공개 독후감 페이지와 다음 페이지 여부를 반환함
        return ResultData.success(new PageDto<>(visibleList, normalizedPage, hasNext));
    }

    /** 알림이 지정한 공개 독후감 한 건과 도서 정보를 조회함 */
    @Override
    public ResultData getPublicReportTarget(Long userNumb, Long reptNumb) {
        if (StringUtil.hasEmpty(userNumb, reptNumb) || reptNumb <= 0) {
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        ReportDto request = new ReportDto();
        request.setUserNumb(userNumb);
        request.setReptNumb(reptNumb);
        ReportDto target = reportMapper.getPublicReportTarget(request);

        // 현재 공개 상태인 대상이 없으면 만료되거나 접근할 수 없는 알림으로 처리함
        if (StringUtil.isEmpty(target)) {
            // "조회된 데이터가 없어요."
            return ResultData.fail(ResultEnum.COMMON_NO_DATA);
        }

        // 알림 직접 진입 카드에도 목록과 같은 번역 버튼 노출 정책을 적용함
        reportTranslationService.applyReportAvailability(List.of(target));
        // 공개 독후감 한 건을 성공 응답으로 반환함
        return ResultData.success(target);
    }

    /**
     * ISBN 기준 도서 평균 별점을 조회함
     * 평균 별점은 공개 여부와 무관하게 읽는 중 상태를 제외한 독후감을 기준으로 계산함
     *
     * @author SeungHyeon.Kang
     * @param bookIsbn 조회할 도서 ISBN
     * @return 평균 별점 조회 결과
     */
    @Override
    public ResultData getPublicRatingAvgByIsbn(String bookIsbn) {
        // ISBN이 없으면 도서를 특정할 수 없으므로 공개 독후감 또는 평균 별점을 조회하지 않음
        if (StringUtil.isEmpty(bookIsbn)) {
            // "조회 결과가 없어요."
            return ResultData.fail(ResultEnum.COMMON_NO_DATA);
        }

        // ISBN 기준 도서 평균 별점을 조회 결과를 성공 응답으로 반환함
        return ResultData.success(reportMapper.getPublicRatingAvgByIsbn(StringUtil.normalizePlainText(bookIsbn)));
    }

    /**
     * 독후감과 필요한 도서 정보를 등록함
     * 도서가 이미 존재하면 기존 도서 번호를 재사용하고, 없으면 도서를 먼저 등록한 뒤 독후감을 저장함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @param reportDto 등록할 독후감 및 도서 정보
     * @return 등록된 독후감 번호
     */
    @Override
    @Transactional
    public ResultData setReport(Long userNumb, ReportDto reportDto) {
        // 등록 요청의 도서 필수값이 누락되면 도서와 독후감 저장을 모두 중단함
        if (hasInvalidBookFields(reportDto)) {
            // "선택한 책 정보가 올바르지 않습니다. 다른 책을 선택해주세요."
            return ResultData.fail(ResultEnum.COMMON_REPORT_BOOK_INVALID);
        }

        // UserNumb 업무 값을 reportDto DTO에 설정함
        reportDto.setUserNumb(userNumb);
        // setDefaultReportColor 호출로 업무 처리에 필요한 값을 설정함
        setDefaultReportColor(reportDto);
        // 신규 독후감에 사용자 공개 및 알림 기본값을 적용함
        applyNewReportDefaults(reportDto);
        // 독후감 입력값에서 허용하지 않는 스크립트 내용을 제거함
        sanitizeReport(reportDto, true);
        // 읽는 중 독후감은 공개 목록과 평점 집계에 들어가지 않도록 저장값을 제한함
        applyReadingStatusPolicy(reportDto);

        // validateReport 검증으로 잘못된 요청이 업무 로직에 진입하지 않도록 차단함
        ReportValidationResult validationResult = validateReport(reportDto, true);
        // 업무 검증 실패가 있으면 DB 변경 전에 사용자에게 전달할 실패 결과를 반환함
        if (!StringUtil.isEmpty(validationResult)) {
            // 독후감과 필요한 도서 정보를 등록 과정에서 확인된 사용자 메시지
            return ResultData.fail(validationResult.resultEnum(), validationResult.args());
        }

        // ISBN 기준 등록된 도서가 없을 때만 도서 마스터를 신규 생성함
        if (bookMapper.dupBook(reportDto) == 0) {
            // Book 업무 값을 bookMapper DTO에 설정함
            bookMapper.setBook(reportDto);
        }

        // 앞선 조건에 해당하지 않는 대체 업무 흐름으로 전환함
        else {
            // BookNumb 업무 값을 reportDto DTO에 설정함
            reportDto.setBookNumb(bookMapper.getBookNumbByIsbn(reportDto));
        }

        // Report 업무 값을 reportMapper DTO에 설정함
        reportMapper.setReport(reportDto);
        // 독후감 등록 후 PK가 채워지지 않으면 저장 실패로 판단함
        if (StringUtil.isEmpty(reportDto.getReptNumb())) {
            // "저장에 실패했어요.\n다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_SAVE_REJECTED);
        }

        // 독후감과 필요한 도서 정보를 등록 결과를 성공 응답으로 반환함
        return ResultData.success(reportDto.getReptNumb());
    }

    /**
     * 기존 독후감 정보를 수정함
     * URL의 독후감 번호를 DTO에 주입해 클라이언트가 본문 번호를 조작해도 수정 대상이 바뀌지 않도록 함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @param reptNumb 수정할 독후감 번호
     * @param reportDto 수정할 독후감 정보
     * @return 수정된 독후감 번호
     */
    @Override
    @Transactional
    public ResultData uptReport(Long userNumb, Long reptNumb, ReportDto reportDto) {
        // 대상 독후감 번호가 없으면 상세, 수정, 삭제 대상을 특정할 수 없으므로 실패 처리함
        if (StringUtil.isEmpty(reptNumb) || StringUtil.isEmpty(reportDto)
                || StringUtil.isEmpty(reportDto.getEditVersion())) {
            // "조회 결과가 없어요."
            return ResultData.fail(ResultEnum.COMMON_NO_DATA);
        }

        // UserNumb 업무 값을 reportDto DTO에 설정함
        reportDto.setUserNumb(userNumb);
        // ReptNumb 업무 값을 reportDto DTO에 설정함
        reportDto.setReptNumb(reptNumb);
        // setDefaultReportColor 호출로 업무 처리에 필요한 값을 설정함
        setDefaultReportColor(reportDto);
        // setDefaultPublicFlag 호출로 업무 처리에 필요한 값을 설정함
        setDefaultPublicFlag(reportDto);
        // 수정 시점의 사용자 표시 언어를 독후감 원문 언어로 설정함
        setReportLanguage(reportDto, userMapper.getUserSettingDtl(userNumb));
        // 독후감 입력값에서 허용하지 않는 스크립트 내용을 제거함
        sanitizeReport(reportDto, false);
        // 읽는 중으로 되돌린 독후감은 기존 공개 여부와 평점을 제거함
        applyReadingStatusPolicy(reportDto);

        // validateReport 검증으로 잘못된 요청이 업무 로직에 진입하지 않도록 차단함
        ReportValidationResult validationResult = validateReport(reportDto, true);
        // 업무 검증 실패가 있으면 DB 변경 전에 사용자에게 전달할 실패 결과를 반환함
        if (!StringUtil.isEmpty(validationResult)) {
            // 기존 독후감 정보를 수정 과정에서 확인된 사용자 메시지
            return ResultData.fail(validationResult.resultEnum(), validationResult.args());
        }

        // 요청값이 업무에서 허용한 범위와 상태를 만족하는지 구분함
        if (reportMapper.uptReport(reportDto) == 0) {
            // 다른 탭이나 기기에서 먼저 변경한 원본을 덮어쓰지 않도록 충돌 결과를 반환함
            return ResultData.fail(ResultEnum.COMMON_EDIT_CONFLICT);
        }

        // 기존 독후감 정보를 수정 결과를 성공 응답으로 반환함
        return ResultData.success(reportDto.getReptNumb());
    }

    /**
     * 로그인 사용자가 작성한 독후감의 좋아요 또는 댓글 알림 사용 여부를 변경함
     * 독후감 공개 여부와 관계없이 작성자 본인은 설정할 수 있으며, 유형별 전용 컬럼만 수정함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @param reptNumb 수정할 독후감 번호
     * @param alimType 변경할 알림 유형
     * @param reportAlimDto 변경할 알림 사용 여부
     * @return 변경된 알림 사용 여부
     */
    @Override
    @Transactional
    public ResultData uptReportAlim(Long userNumb, Long reptNumb, String alimType
                                  , ReportAlimDto reportAlimDto) {
        // 인증 사용자, 변경 대상, 설정 유형과 사용 여부가 없으면 수정 대상을 확정할 수 없으므로 요청을 거부함
        if (StringUtil.hasEmpty(userNumb, reptNumb, alimType, reportAlimDto)
                || StringUtil.isEmpty(reportAlimDto.getUseYsno())) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // URL에서 받은 알림 유형을 대소문자와 앞뒤 공백 차이 없이 비교하도록 정규화함
        String normalizedAlimType = alimType.trim().toLowerCase(Locale.ROOT);
        // 본문에서 받은 사용 여부를 공통 Y 또는 N 값과 비교할 수 있도록 정규화함
        String normalizedUseYsno = reportAlimDto.getUseYsno().trim().toUpperCase(Locale.ROOT);

        // 허용하지 않은 알림 유형이나 Y 또는 N 이외의 값은 DB 변경 전에 거부함
        if ((!Constant.REPORT_ALIM_LIKE.equals(normalizedAlimType)
                && !Constant.REPORT_ALIM_REPLY.equals(normalizedAlimType))
                || (!Constant.COMM_YES.equals(normalizedUseYsno)
                && !Constant.COMM_NO.equals(normalizedUseYsno))) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 인증 정보에서 확인한 사용자 번호를 소유자 변경 조건으로 설정함
        reportAlimDto.setUserNumb(userNumb);
        // URL에서 확정한 독후감 번호를 변경 조건으로 설정함
        reportAlimDto.setReptNumb(reptNumb);
        // 정규화한 알림 유형을 응답과 업무 분기에 사용할 값으로 설정함
        reportAlimDto.setAlimType(normalizedAlimType);
        // 정규화한 알림 사용 여부를 DB 변경값으로 설정함
        reportAlimDto.setUseYsno(normalizedUseYsno);

        // 요청 유형에 해당하는 알림 설정 컬럼 하나의 변경 건수를 저장함
        int updateCnt;

        // 좋아요 유형이면 독후감 좋아요 알림 컬럼만 변경함
        if (Constant.REPORT_ALIM_LIKE.equals(normalizedAlimType)) {
            // 독후감 좋아요 알림 사용 여부를 소유자 조건으로 변경함
            updateCnt = reportMapper.uptLikeAlim(reportAlimDto);
        }

        // 댓글 유형이면 독후감 댓글 알림 컬럼만 변경함
        else {
            // 독후감 댓글 알림 사용 여부를 소유자 조건으로 변경함
            updateCnt = reportMapper.uptReplyAlim(reportAlimDto);
        }

        // 작성자 소유 독후감이 없어 반영된 행이 없으면 다른 사용자 데이터 접근 요청으로 거부함
        if (updateCnt == 0) {
            // "접근할 수 없는 요청이에요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 화면에서 즉시 현재 메뉴 문구를 갱신할 수 있도록 변경된 설정 정보를 반환함
        return ResultData.success(reportAlimDto);
    }

    /**
     * 마이페이지의 현재 읽고 있는 책 목록에서 독서 상태와 별점 및 공개 여부를 빠르게 수정함
     * 전체 독후감 수정 화면으로 이동하지 않아도 완료 여부와 공개 범위를 즉시 반영할 수 있도록 별도 수정 범위를 사용함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @param reptNumb 수정할 독후감 번호
     * @param reportDto 수정할 독서 상태와 별점 정보
     * @return 수정 처리 결과
     */
    @Override
    @Transactional
    public ResultData uptReptStatusGrade(Long userNumb, Long reptNumb, ReportDto reportDto) {
        // 대상 독후감 번호가 없으면 수정 대상을 특정할 수 없으므로 실패 처리함
        if (StringUtil.isEmpty(reptNumb)) {
            // "조회 결과가 없어요."
            return ResultData.fail(ResultEnum.COMMON_NO_DATA);
        }

        // UserNumb 업무 값을 reportDto DTO에 설정함
        reportDto.setUserNumb(userNumb);
        // ReptNumb 업무 값을 reportDto DTO에 설정함
        reportDto.setReptNumb(reptNumb);
        // ReptGrde 업무 값을 reportDto DTO에 설정함
        reportDto.setReptGrde(StringUtil.normalizePlainText(reportDto.getReptGrde()));
        // ReptStat 업무 값을 reportDto DTO에 설정함
        reportDto.setReptStat(StringUtil.normalizePlainText(reportDto.getReptStat()));
        // PubcYsno 업무 값을 reportDto DTO에 설정함
        reportDto.setPubcYsno(StringUtil.normalizePlainText(reportDto.getPubcYsno()));
        // 빠른 수정 요청에 공개 여부가 없으면 기존 클라이언트도 안전하게 비공개로 처리함
        setDefaultPublicFlag(reportDto);
        // 읽는 중 상태를 빠른 수정 API로 전달해도 공개와 평점 정책을 우회하지 못하게 함
        applyReadingStatusPolicy(reportDto);
        // ReptEndt 업무 값을 reportDto DTO에 설정함
        reportDto.setReptEndt(LocalDate.now().toString()); // 빠른 완료/중단 처리에서는 사용자가 저장한 시점을 실제 독서 종료일로 기록함

        // validateReport 검증으로 잘못된 요청이 업무 로직에 진입하지 않도록 차단함
        ReportValidationResult validationResult = validateReport(reportDto, false);
        // 업무 검증 실패가 있으면 DB 변경 전에 사용자에게 전달할 실패 결과를 반환함
        if (!StringUtil.isEmpty(validationResult)) {
            // 마이페이지의 현재 읽고 있는 책 목록에서 독서 상태와 별점만 빠르게 수정 과정에서 확인된 사용자 메시지
            return ResultData.fail(validationResult.resultEnum(), validationResult.args());
        }

        // 사용자 번호를 WHERE 조건에 함께 사용해 다른 사용자의 독후감은 수정되지 않도록 막음
        int result = reportMapper.uptReptStatusGrade(reportDto);

        // 요청값이 업무에서 허용한 범위와 상태를 만족하는지 구분함
        if (result == 0) {
            // "수정에 실패했어요.\n다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_UPDATE_REJECTED);
        }

        // 마이페이지의 현재 읽고 있는 책 목록에서 독서 상태와 별점만 빠르게 수정 결과를 성공 응답으로 반환함
        return ResultData.success(reportDto.getReptNumb());
    }

    /**
     * 로그인 사용자의 독후감을 삭제함
     * 사용자 번호와 독후감 번호를 함께 조건으로 사용해 본인 독후감만 삭제되도록 함
     *
     * @author HanWon.Jang
     * @param userNumb 로그인 사용자 번호
     * @param reptNumb 삭제할 독후감 번호
     * @return 삭제 처리 결과
     * @throws CustomException 독후감 삭제가 거절된 경우 발생
     */
    @Override
    @Transactional
    public ResultData delReport(Long userNumb, Long reptNumb) {
        // 대상 독후감 번호가 없으면 상세, 수정, 삭제 대상을 특정할 수 없으므로 실패 처리함
        if (StringUtil.isEmpty(reptNumb)) {
            // "조회 결과가 없어요."
            return ResultData.fail(ResultEnum.COMMON_NO_DATA);
        }

        // 독후감 또는 독서 목표 처리 데이터를 담을 객체를 생성함
        ReportDto reportDto = new ReportDto();
        // UserNumb 업무 값을 reportDto DTO에 설정함
        reportDto.setUserNumb(userNumb);
        // ReptNumb 업무 값을 reportDto DTO에 설정함
        reportDto.setReptNumb(reptNumb);

        // 진행 중인 모임 회차의 연결 독후감이면 참여 기록을 중도하차로 확정함
        reportMapper.uptClubReadingDropout(reportDto);
        // 댓글을 제거하기 전에 댓글과 답글을 대상으로 등록된 공용 좋아요를 정리함
        reportMapper.delReportReplyLikes(reportDto);
        // 자기 참조 외래키에 막히지 않도록 최상위 댓글보다 대댓글을 먼저 정리함
        reportMapper.delReportChildReplies(reportDto);
        // 대댓글이 제거된 뒤 독후감을 참조하는 나머지 댓글을 정리함
        reportMapper.delReportReplies(reportDto);
        // 독후감 원본을 제거하기 전에 독후감 대상 공용 좋아요를 정리함
        reportMapper.delReportLikes(reportDto);

        // 삭제 반영 건수가 없으면 본인 독후감이 아니거나 이미 삭제된 데이터로 판단함
        if (reportMapper.delReport(reportDto) == 0) {
            // "삭제에 실패했어요.\n다시 시도해주세요."
            throw new CustomException(ResultEnum.COMMON_DELETE_REJECTED, HttpStatus.BAD_REQUEST);
        }


        // 로그인 사용자의 독후감을 삭제 결과를 성공 응답으로 반환함
        return ResultData.success();
    }
    /**
     * 독후감 등록에 필요한 도서 필수값이 모두 존재하는지 확인함
     * 도서 검색 API 응답을 조작해 들어오는 경우에도 백엔드에서 한 번 더 검증함
     *
     * @author SeungHyeon.Kang
     * @param reportDto 검증할 독후감 및 도서 정보
     * @return 도서 필수값 누락 여부
     */
    private boolean hasInvalidBookFields(ReportDto reportDto) {
        // 독후감 등록에 필요한 도서 필수값이 모두 존재하는지 확인 결과를 반환함
        return StringUtil.isEmpty(reportDto) || StringUtil.hasEmpty(
                // getBookTitl 조회로 후속 처리에 필요한 데이터를 가져옴
                reportDto.getBookTitl(),
                // getBookAthr 조회로 후속 처리에 필요한 데이터를 가져옴
                reportDto.getBookAthr(),
                // getBookPubl 조회로 후속 처리에 필요한 데이터를 가져옴
                reportDto.getBookPubl(),
                // getBookIsbn 조회로 후속 처리에 필요한 데이터를 가져옴
                reportDto.getBookIsbn(),
                // getBookCvim 조회로 후속 처리에 필요한 데이터를 가져옴
                reportDto.getBookCvim(),
                // getBookDesc 조회로 후속 처리에 필요한 데이터를 가져옴
                reportDto.getBookDesc()
        );
    }

    /**
     * 독후감 등록과 수정에 공통으로 적용되는 업무 검증을 수행함
     * 필수값, 공통코드, 날짜 범위, 본문 byte 길이, 공개 여부 코드를 순서대로 확인함
     *
     * @author SeungHyeon.Kang
     * @param reportDto 검증할 독후감 정보
     * @param isFullScan 독후감 내용을 모두 유효성 검사 할 것인지를 판단
     * @return 검증 실패 결과, 통과하면 null
     */
    private ReportValidationResult validateReport(ReportDto reportDto, boolean isFullScan) {

        List<String> missingFields = new ArrayList<>();
        // 두 값이 동일한지 안전하게 비교함
        boolean isReadingStatus = Constant.REPORT_STAT_READ.equals(reportDto.getReptStat());
        // 필수 값이 비어 있는지 공통 기준으로 확인함
        boolean hasReportContent = !StringUtil.isEmpty(reportDto.getReptCntn());

        // 독서 상태는 필수값이며 READ_STAT 공통코드에 등록된 값만 저장함
        if (StringUtil.isEmpty(reportDto.getReptStat()) || !codeUtil.existsCode(Constant.CODE_READ_STAT, reportDto.getReptStat())) {
            // 처리한 값을 결과 컬렉션에 추가함
            missingFields.add(MessageUtils.getMessage(REPORT_FIELD_STATUS_KEY));
        }

        // 종료일은 상태와 관계없이 기간 계산에 필요하므로 필수값으로 검증함
        if (StringUtil.isEmpty(reportDto.getReptEndt())) {
            // 처리한 값을 결과 컬렉션에 추가함
            missingFields.add(MessageUtils.getMessage(REPORT_FIELD_END_DATE_KEY));
        }

        // 도서 평점의 저장값이 없으면 저장값을 0점으로 보정해 저장값을 숫자로 유지함
        if (StringUtil.isEmpty(reportDto.getReptGrde())) {
            // ReptGrde 업무 값을 reportDto DTO에 설정함
            reportDto.setReptGrde("0");
        }

        // 전체 편집과 빠른 수정 모두 허용 범위와 0.5점 간격을 벗어난 평점을 저장하지 않음
        if (!isValidReportGrade(reportDto.getReptGrde())) {
            // 처리한 값을 결과 컬렉션에 추가함
            missingFields.add(MessageUtils.getMessage(REPORT_FIELD_GRADE_KEY));
        }

        //등록 수정화면에서 행해지는 등록 및 수정은 모든 값을 입력받아야함
        if(isFullScan) {
            // 시작일은 상태와 관계없이 기간 계산에 필요하므로 필수값으로 검증함
            if (StringUtil.isEmpty(reportDto.getReptStdt())) {
                // 처리한 값을 결과 컬렉션에 추가함
                missingFields.add(MessageUtils.getMessage(REPORT_FIELD_START_DATE_KEY));
            }

            // 책장 색상은 필수값이며 BOOK_COLR 공통코드에 등록된 값만 저장함
            if (StringUtil.isEmpty(reportDto.getReptColr()) || !codeUtil.existsCode(Constant.CODE_BOOK_COLR, reportDto.getReptColr())) {
                // 처리한 값을 결과 컬렉션에 추가함
                missingFields.add(MessageUtils.getMessage(REPORT_FIELD_COLOR_KEY));
            }

            // 읽고 있어요 상태는 사용자가 아직 기록을 남기지 않을 수 있으므로 본문 필수 검증에서 제외함
            // 완료/중단 상태는 실제 독후감 기록 저장 단계이므로 기존처럼 본문을 필수값으로 유지함
            if (!isReadingStatus && !hasReportContent) {
                // 처리한 값을 결과 컬렉션에 추가함
                missingFields.add(MessageUtils.getMessage(REPORT_FIELD_CONTENT_KEY));
            }

            // 전체 입력에서 누락된 항목이 있으면 날짜와 본문 상세 검증보다 필수값 안내를 우선함
            if (!missingFields.isEmpty()) {
                // 새로 생성한 ReportValidationResult 객체를 반환함
                return new ReportValidationResult(ResultEnum.COMMON_REPORT_REQUIRED_MISSING, formatMissingFields(missingFields));
            }

            // 시작일이 종료일보다 늦은 데이터는 프론트 조작 여부와 관계없이 저장하지 않음
            if (!DateUtil.validateReportDateRange(reportDto.getReptStdt(), reportDto.getReptEndt())) {
                // 새로 생성한 ReportValidationResult 객체를 반환함
                return new ReportValidationResult(ResultEnum.COMMON_REPORT_DATE_RANGE_INVALID);
            }

            // 데이터베이스 저장 한도를 넘는 본문은 DB 오류가 나기 전에 업무 검증으로 차단함
            if (hasReportContent && XssUtil.utf8ByteLength(reportDto.getReptCntn()) > Constant.REPORT_CONTENT_MAX_BYTES) {
                // 새로 생성한 ReportValidationResult 객체를 반환함
                return new ReportValidationResult(ResultEnum.COMMON_REPORT_CONTENT_TOO_LONG, Constant.REPORT_CONTENT_MAX_BYTES);
            }

            // 비속어 필터링
            if (hasReportContent) {
                // findBadWord 업무 로직을 badWordDetectionService에 위임함
                Optional<String> badWord = badWordDetectionService.findBadWord(reportDto.getReptCntn());
                // 요청값이 업무에서 허용한 범위와 상태를 만족하는지 구분함
                if (badWord.isPresent()) {
                    // 새로 생성한 ReportValidationResult 객체를 반환함
                    return new ReportValidationResult(ResultEnum.COMMON_BAD_WORD_INCLUDED, badWord.get());
                }
            }

        }

        // 전체 편집과 빠른 수정에 필요한 필수값이 하나라도 없으면 DB 변경 전에 안내함
        if (!missingFields.isEmpty()) {
            // 새로 생성한 ReportValidationResult 객체를 반환함
            return new ReportValidationResult(ResultEnum.COMMON_REPORT_REQUIRED_MISSING, formatMissingFields(missingFields));
        }

        // 공개 여부는 Y 또는 N만 허용해 공개 독후감 조회 조건을 안정적으로 유지함
        if (!Constant.COMM_YES.equals(reportDto.getPubcYsno()) && !Constant.COMM_NO.equals(reportDto.getPubcYsno())) {
            // 새로 생성한 ReportValidationResult 객체를 반환함
            return new ReportValidationResult(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 조회하거나 생성할 값이 없음을 반환함
        return null;
    }

    /**
     * 누락된 필수 항목 목록을 사용자에게 보여줄 수 있는 줄바꿈 문장으로 변환함
     *
     * @author SeungHyeon.Kang
     * @param missingFields 누락된 필드 표시명 목록
     * @return 필수값 누락 메시지 인자
     */
    private String formatMissingFields(List<String> missingFields) {
        // 누락된 필수 항목 목록을 사용자에게 보여줄 수 있는 줄바꿈 문장으로 변환 결과를 반환함
        return "- " + String.join("\n- ", missingFields);
    }

    /**
     * 별점 값이 숫자이며 0점부터 5점 범위 안의 0.5점 단위인지 확인함
     * 0점은 읽고있어요 상태에서 별점을 선택하지 않은 값을 저장하기 위한 내부 보정값으로 허용함
     *
     * @author SeungHyeon.Kang
     * @param reptGrde 검증할 별점 문자열
     * @return 유효한 별점 여부
     */
    private boolean isValidReportGrade(String reptGrde) {
        // 별점이 비어 있으면 호출한 검증 흐름에서 상태별 필수 여부를 먼저 판단하도록 false를 반환함
        if (StringUtil.isEmpty(reptGrde)) {
            // 비어 있는 별점의 검증 실패 여부를 반환함
            return false;
        }

        // 사용자 입력 문자열을 정밀한 소수로 변환해 부동소수점 오차 없이 0.5점 간격을 확인함
        try {
            // 독후감 별점 문자열을 소수점 비교가 가능한 값으로 변환함
            BigDecimal grade = new BigDecimal(reptGrde);
            boolean isWithinRange = grade.compareTo(BigDecimal.ZERO) >= 0
                    && grade.compareTo(REPORT_GRADE_MAX) <= 0;
            boolean isHalfPointStep = grade.remainder(REPORT_GRADE_STEP).compareTo(BigDecimal.ZERO) == 0;

            // 허용 범위와 0.5점 간격을 모두 만족하는 별점 여부를 반환함
            return isWithinRange && isHalfPointStep;
        }

        // 숫자로 변환할 수 없는 외부 입력은 유효하지 않은 별점으로 처리함
        catch (NumberFormatException e) {
            // 숫자가 아닌 별점의 검증 실패 여부를 반환함
            return false;
        }
    }

    private void setDefaultReportColor(ReportDto reportDto) {
        // 책장 색상은 필수값이며 공통코드에 등록된 색상 코드만 허용함
        if (StringUtil.isEmpty(reportDto.getReptColr()) || reportDto.getReptColr().isBlank()) {
            // ReptColr 업무 값을 reportDto DTO에 설정함
            reportDto.setReptColr(codeUtil.getFirstCode(Constant.CODE_BOOK_COLR));
        }
    }

    /**
     * 공개 여부 값이 비어 있으면 비공개로 기본 설정함
     * 사용자가 명시적으로 공개를 선택하지 않은 독후감이 외부에 노출되지 않도록 함
     *
     * @author SeungHyeon.Kang
     * @param reportDto 공개 여부 기본값을 반영할 독후감 DTO
     */
    private void setDefaultPublicFlag(ReportDto reportDto) {
        // 공개 여부는 Y 또는 N만 허용해 공개 독후감 조회 조건을 안정적으로 유지함
        if (StringUtil.isEmpty(reportDto.getPubcYsno()) || reportDto.getPubcYsno().isBlank()) {
            // PubcYsno 업무 값을 reportDto DTO에 설정함
            reportDto.setPubcYsno(Constant.COMM_NO);
        }
    }

    /**
     * 사용자 설정의 표시 언어를 독후감 원문 언어 코드로 변환함
     *
     * @author SeungHyeon.Kang
     * @param reportDto 원문 언어를 설정할 독후감 DTO
     * @param setting 현재 사용자 설정
     */
    private void setReportLanguage(ReportDto reportDto, UserSettingDto setting) {
        // 영어 사용 설정이 명시된 경우에만 영어 원문으로 분류하고 나머지는 한국어로 보정함
        String languageCode = !StringUtil.isEmpty(setting)
                && Constant.COMM_YES.equals(setting.getEnglishYsno()) ? "en" : "ko";
        // 번역 방향 판정에 사용할 원문 언어 코드를 서버 설정 기준으로 저장함
        reportDto.setLangCode(languageCode);
    }

    /** 신규 독후감의 공개 및 반응 알림 기본값을 사용자 설정에서 적용함 */
    private void applyNewReportDefaults(ReportDto reportDto) {
        UserSettingDto setting = userMapper.getUserSettingDtl(reportDto.getUserNumb());
        // 등록 시점의 사용자 표시 언어를 독후감 원문 언어로 설정함
        setReportLanguage(reportDto, setting);

        if (StringUtil.isEmpty(reportDto.getPubcYsno()) || reportDto.getPubcYsno().isBlank()) {
            reportDto.setPubcYsno(StringUtil.isEmpty(setting)
                    ? Constant.COMM_NO
                    : setting.getReportPublicDefaultYsno());
        }

        reportDto.setLikeAlimYsno(StringUtil.isEmpty(setting)
                ? Constant.COMM_YES
                : setting.getReportLikeDefaultYsno());
        reportDto.setReplyAlimYsno(StringUtil.isEmpty(setting)
                ? Constant.COMM_YES
                : setting.getReportReplyDefaultYsno());
    }

    /**
     * 읽고 있는 독후감의 평점과 공개 여부를 집계 및 공개 대상이 아닌 값으로 강제함
     * 화면에서 숨긴 입력값을 조작하더라도 서버 저장 정책이 동일하게 유지되도록 함
     *
     * @author SeungHyeon.Kang
     * @param reportDto 상태별 저장 정책을 적용할 독후감 DTO
     */
    private void applyReadingStatusPolicy(ReportDto reportDto) {

        // 읽는 중이 아닌 완료와 중단 독후감은 사용자가 선택한 평점과 공개 여부를 유지함
        if (!Constant.REPORT_STAT_READ.equals(reportDto.getReptStat())) {
            return;
        }

        // 선택 불가한 평점은 집계에 사용되지 않는 내부값 0으로 저장함
        reportDto.setReptGrde("0");
        // 읽는 중 독후감은 다른 사용자에게 노출되지 않도록 비공개로 저장함
        reportDto.setPubcYsno(Constant.COMM_NO);
    }

    /**
     * 독후감 입력값의 HTML entity와 불필요한 텍스트 표현을 일반 문자열로 정규화함
     * 등록 시에는 도서 정보도 함께 정규화하고, 수정 시에는 독후감 필드만 정규화함
     *
     * @author SeungHyeon.Kang
     * @param reportDto 정규화할 독후감 DTO
     * @param includeBookFields 도서 필드 정규화 포함 여부
     */
    private void sanitizeReport(ReportDto reportDto, boolean includeBookFields) {
        // ReptStat 업무 값을 reportDto DTO에 설정함
        reportDto.setReptStat(StringUtil.normalizePlainText(reportDto.getReptStat()));
        // ReptStdt 업무 값을 reportDto DTO에 설정함
        reportDto.setReptStdt(StringUtil.normalizePlainText(reportDto.getReptStdt()));
        // ReptEndt 업무 값을 reportDto DTO에 설정함
        reportDto.setReptEndt(StringUtil.normalizePlainText(reportDto.getReptEndt()));
        // ReptGrde 업무 값을 reportDto DTO에 설정함
        reportDto.setReptGrde(StringUtil.normalizePlainText(reportDto.getReptGrde()));
        // ReptColr 업무 값을 reportDto DTO에 설정함
        reportDto.setReptColr(StringUtil.normalizePlainText(reportDto.getReptColr()));
        // PubcYsno 업무 값을 reportDto DTO에 설정함
        reportDto.setPubcYsno(StringUtil.normalizePlainText(reportDto.getPubcYsno()));
        // ReptCntn 업무 값을 reportDto DTO에 설정함
        reportDto.setReptCntn(StringUtil.normalizePlainText(reportDto.getReptCntn()));

        // 등록 요청일 때만 도서 필드를 함께 정규화하고, 수정 요청에서는 독후감 필드만 정규화함
        if (includeBookFields) {
            // BookTitl 업무 값을 reportDto DTO에 설정함
            reportDto.setBookTitl(StringUtil.normalizePlainText(reportDto.getBookTitl()));
            // BookAthr 업무 값을 reportDto DTO에 설정함
            reportDto.setBookAthr(StringUtil.normalizePlainText(reportDto.getBookAthr()));
            // BookPubl 업무 값을 reportDto DTO에 설정함
            reportDto.setBookPubl(StringUtil.normalizePlainText(reportDto.getBookPubl()));
            // BookIsbn 업무 값을 reportDto DTO에 설정함
            reportDto.setBookIsbn(StringUtil.normalizePlainText(reportDto.getBookIsbn()));
            // BookCvim 업무 값을 reportDto DTO에 설정함
            reportDto.setBookCvim(StringUtil.normalizePlainText(reportDto.getBookCvim()));
            // BookDesc 업무 값을 reportDto DTO에 설정함
            reportDto.setBookDesc(StringUtil.normalizePlainText(reportDto.getBookDesc()));
        }
    }

    /**
     * 독후감 검증 실패 결과와 메시지 인자를 함께 전달하기 위한 내부 record임
     *
     * @author SeungHyeon.Kang
     * @param resultEnum 실패 결과 코드
     * @param args 메시지 치환 인자
     */
    private record ReportValidationResult(ResultEnum resultEnum, Object... args) {

    }
}
