package org.our.sadari.readingClub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.our.sadari.alim.service.AlimService;
import org.our.sadari.book.mapper.BookMapper;
import org.our.sadari.global.common.code.util.CodeUtil;
import org.our.sadari.global.common.constant.Constant;
import org.our.sadari.global.common.dto.PageDto;
import org.our.sadari.global.common.exception.CustomException;
import org.our.sadari.global.common.result.ResultData;
import org.our.sadari.global.common.result.ResultEnum;
import org.our.sadari.global.common.service.BadWordDetectionService;
import org.our.sadari.global.common.util.MessageUtils;
import org.our.sadari.readingClub.dto.ReadingClubDto;
import org.our.sadari.readingClub.mapper.ReadingClubMapper;
import org.our.sadari.readingClub.mapper.ReadingClubMembershipMapper;
import org.our.sadari.report.mapper.ReportMapper;
import org.our.sadari.report.dto.ReportDto;
import org.our.sadari.social.service.UserBlockService;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * fileName       : ReadingClubServiceImplTest
 * author         : HanWon.Jang
 * date           : 2026-08-13
 * description    : 독서 모임 서비스의 목록 관계 데이터와 접근 정책을 검증함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-08-13        SeungHyeon.Kang    최초 생성
 * 2026-08-14        Hanwon.Jang        모임 접근·초대·독서 검증 추가
 * 2026-08-20        SeungHyeon.Kang,Hanwon.Jang    독서 수정·초대 알림 검증
 * 2026-08-21        SeungHyeon.Kang    초대 알림 상황 통합 검증
 * 2026-08-22        HanWon.Jang        종료 결과·독후감 조회 검증
 * 2026-08-23        HanWon.Jang        이전 독서 기록·회차 결과 조회 검증
 * 2026-08-24        HanWon.Jang        가입 처리 알림·신청 취소·모임원 퇴장 검증
 * 2026-08-27        HanWon.Jang        가입 승인 알림 상황 검증
 * 2026-08-29        HanWon.Jang        진행 회차 독후감 조회 검증
 * 2026-08-31        HanWon.Jang        독서 조기 마감·결과 확인 검증
 * 2026-09-01        HanWon.Jang        공개 모임 조회·자진 탈퇴 검증
 * 2026-09-04        SeungHyeon.Kang    가입 차단·채팅 읽음 수·퇴장 이력 검증
 * 2026-09-11        HanWon.Jang        도서 언어·알림 기본값 등록 검증
 */
@ExtendWith(MockitoExtension.class)
class ReadingClubServiceImplTest {

    // 독서 모임 데이터 접근 객체
    @Mock
    private ReadingClubMapper readingClubMapper;

    // 모임 자진 탈퇴 데이터 정리 객체
    @Mock
    private ReadingClubMembershipMapper readingClubMembershipMapper;

    // 사용자 입력 비속어 검사 서비스
    @Mock
    private BadWordDetectionService badWordDetectionService;

    // 사용자 알림과 푸시 발송 서비스
    @Mock
    private AlimService alimService;

    // 도서 마스터 데이터 접근 객체
    @Mock
    private BookMapper bookMapper;

    // 독후감 데이터 접근 객체
    @Mock
    private ReportMapper reportMapper;

    // 사용자 간 양방향 차단 관계 조회 서비스
    @Mock
    private UserBlockService userBlockService;

    // 공통코드 조회 도구
    @Mock
    private CodeUtil codeUtil;
    // 채팅 화면 열람 여부 조회 서비스
    @Mock
    private ClubChatViewService clubChatViewService;

    // 독서 모임 서비스 단위 테스트 대상
    private ReadingClubServiceImpl readingClubService;

    /**
     * 각 테스트가 독립된 Mock 의존성을 사용하는 독서 모임 서비스 구현체를 구성함
     *
     * @author SeungHyeon.Kang
     */
    @BeforeEach
    void setUp() {
        // 실패 응답에서 사용할 서버 공통 메시지 소스를 생성함
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        // 서버 공통 메시지 프로퍼티를 테스트 조회 기준으로 설정함
        messageSource.setBasename("messages");
        // 한글 메시지 원문이 손상되지 않도록 인코딩을 설정함
        messageSource.setDefaultEncoding("UTF-8");
        // ResultData 실패 응답이 공통 메시지 소스를 사용하도록 초기화함
        new MessageUtils().setMessageSource(messageSource);

        // 독서 모임 서비스 단위 테스트 대상을 생성함
        readingClubService = new ReadingClubServiceImpl(
                readingClubMapper, readingClubMembershipMapper, badWordDetectionService
              , alimService, bookMapper, reportMapper
              , userBlockService, codeUtil, clubChatViewService);
    }

    /**
     * 모임 독서를 등록하면 모든 활성 멤버에게 같은 기간의 읽는 중 독후감과 참여 관계를 생성하는지 검증함
     *
     * @author Hanwon.Jang
     */
    @Test
    void setReadingCreatesReports() {

        // 활성 모임장과 선택 도서 및 목표 기간 요청을 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setClubStat("ACTIVE");
        club.setOwnrNumb(20L);
        ReadingClubDto.ReadingCreateReqDto request = createReadingRequest();

        // 모임장 권한과 활성 멤버 및 도서 연결이 모두 유효하도록 조회 결과를 설정함
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.getActiveOwnerCnt(10L, 20L)).thenReturn(1);
        when(readingClubMapper.getReadingRoundByIdempotency(10L, "reading-request-1")).thenReturn(null);
        when(readingClubMapper.getOngoingRoundCnt(10L)).thenReturn(0);
        when(readingClubMapper.getActiveMemberUserNumbList(10L)).thenReturn(List.of(20L, 30L));
        when(codeUtil.getFirstCode(Constant.CODE_BOOK_COLR)).thenReturn("GREEN");
        when(bookMapper.dupBook(request)).thenReturn(1);
        when(bookMapper.getBookNumbByIsbn(request)).thenReturn(99L);
        when(readingClubMapper.getNextReadingRoundNumb(10L)).thenReturn(1L);
        when(readingClubMapper.setReadingRound(10L, 20L, request)).thenReturn(1);
        when(readingClubMapper.setReadingParticipant(any(), any(), any(Long.class), any(), any())).thenReturn(1);
        doAnswer(invocation -> {
            // 호출 순서에 따라 생성 키를 부여해 참여 연결 검증이 가능하게 함
            ReportDto report = invocation.getArgument(0);
            report.setReptNumb(report.getUserNumb() + 100L);
            return 1;
        }).when(reportMapper).setReport(any(ReportDto.class));

        // 모임 독서 등록을 실행함
        ResultData result = readingClubService.setReading(20L, 10L, request);

        // 회차 생성 성공과 멤버별 독후감의 상태 및 동일 기간을 검증함
        assertEquals(200, result.getCode());
        assertEquals(Map.of("rondNumb", 1L), result.getData());
        ArgumentCaptor<ReportDto> reportCaptor = ArgumentCaptor.forClass(ReportDto.class);
        verify(reportMapper, times(2)).setReport(reportCaptor.capture());
        assertEquals(List.of(20L, 30L), reportCaptor.getAllValues().stream().map(ReportDto::getUserNumb).toList());
        assertEquals(List.of("READ", "READ"), reportCaptor.getAllValues().stream().map(ReportDto::getReptStat).toList());
        assertEquals(List.of("ko", "ko"), reportCaptor.getAllValues().stream().map(ReportDto::getLangCode).toList());
        assertEquals(List.of("Y", "Y"), reportCaptor.getAllValues().stream().map(ReportDto::getLikeAlimYsno).toList());
        assertEquals(List.of("Y", "Y"), reportCaptor.getAllValues().stream().map(ReportDto::getReplyAlimYsno).toList());
        assertEquals(List.of("2026-08-14", "2026-08-14"), reportCaptor.getAllValues().stream().map(ReportDto::getReptStdt).toList());
        assertEquals(List.of("2026-08-31", "2026-08-31"), reportCaptor.getAllValues().stream().map(ReportDto::getReptEndt).toList());
        verify(readingClubMapper).setReadingParticipant(10L, 1L, 1L, 20L, 120L);
        verify(readingClubMapper).setReadingParticipant(10L, 1L, 2L, 30L, 130L);
    }

    /**
     * 빈 도서 언어 코드가 서비스 기본값 보정 전에 Controller 검증에서 거절되지 않는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void blankLangUsesDefault() {

        ReadingClubDto.ReadingCreateReqDto request = createReadingRequest();
        request.setLangCode("");

        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = validatorFactory.getValidator();
            Set<ConstraintViolation<ReadingClubDto.ReadingCreateReqDto>> violations = validator.validate(request);
            boolean hasLanguageViolation = violations.stream()
                    .map(violation -> violation.getPropertyPath().toString())
                    .anyMatch("langCode"::equals);

            assertFalse(hasLanguageViolation);
        }
    }

    /**
     * 내 모임 목록에 모임별 대표 카테고리 정보를 결합하는지 검증함
     *
     * @author SeungHyeon.Kang
     */
    @Test
    void getMyClubListCategories() {
        // 현재 도서 표지가 포함된 내 모임 조회 결과를 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        // 관계 데이터 조회에 사용할 모임 번호를 설정함
        club.setClubNumb(10L);
        // 목록 API가 유지해야 하는 현재 도서 표지를 설정함
        club.setCurrentBookCvim("https://example.com/book.jpg");
        // 대표 카테고리 조회 결과를 구성함
        ReadingClubDto.CategoryDto category = new ReadingClubDto.CategoryDto();
        // 화면에 표시할 카테고리 코드를 설정함
        category.setIntrCode("NOVEL");
        // 화면에 표시할 카테고리명을 설정함
        category.setIntrName("소설");

        // 내 모임과 모임별 카테고리 조회 결과를 반환하도록 구성함
        when(readingClubMapper.getMyClubList(20L)).thenReturn(List.of(club));
        // 대표 카테고리 관계를 목록 후처리에 제공함
        when(readingClubMapper.getClubCategoryList(10L)).thenReturn(List.of(category));

        // 로그인 사용자의 내 모임 목록을 조회함
        ResultData result = readingClubService.getMyClubList(20L);

        // 카테고리와 도서 표지를 유지한 성공 응답을 검증함
        assertEquals(200, result.getCode());
        assertEquals(List.of(club), result.getData());
        assertEquals("소설", club.getCategoryList().get(0).getIntrName());
        assertEquals("https://example.com/book.jpg", club.getCurrentBookCvim());
        verify(readingClubMapper).getClubCategoryList(10L);
    }

    /**
     * 활성 계정인 모임장 관계가 아니면 모임 독서와 멤버 독후감을 생성하지 않는지 검증함
     *
     * @author Hanwon.Jang
     */
    @Test
    void setReadingRejectsInactive() {

        // 운영 중인 모임과 형식이 유효한 등록 요청을 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setClubStat("ACTIVE");
        ReadingClubDto.ReadingCreateReqDto request = createReadingRequest();
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.getActiveOwnerCnt(10L, 20L)).thenReturn(0);

        // 비활성 계정의 모임 독서 등록을 요청함
        ResultData result = readingClubService.setReading(20L, 10L, request);

        // 접근 거부 응답과 저장 Mapper 미호출을 검증함
        assertEquals(ResultEnum.COMMON_ACCESS_REJECTED.getCode(), result.getCode());
        verify(readingClubMapper, never()).setReadingRound(any(), any(), any());
        verify(reportMapper, never()).setReport(any());
    }

    /**
     * 기존 도서를 유지한 기간 수정은 연결 독후감 작성 여부와 관계없이 함께 반영하는지 검증함
     *
     * @author Hanwon.Jang
     */
    @Test
    void uptReadingSameBook() {

        // 활성 모임장과 현재 회차 및 같은 ISBN의 수정 요청을 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setClubStat("ACTIVE");
        ReadingClubDto.ReadingManageDto reading = new ReadingClubDto.ReadingManageDto();
        reading.setBookNumb(99L);
        reading.setBookIsbn("9781234567890");
        ReadingClubDto.ReadingUpdateReqDto request = createReadingUpdate("9781234567890");

        // 모임장 접근과 현재 회차 잠금 및 수정 성공 결과를 설정함
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.getActiveOwnerCnt(10L, 20L)).thenReturn(1);
        when(readingClubMapper.getReadingForUpdate(10L, 1L)).thenReturn(reading);
        when(readingClubMapper.getReadingReportNumbListForUpdate(10L, 1L)).thenReturn(List.of(120L));
        when(readingClubMapper.uptReading(10L, 1L, request)).thenReturn(1);

        // 같은 도서의 목표 기간 수정을 실행함
        ResultData result = readingClubService.uptReading(20L, 10L, 1L, request);

        // 성공 응답과 기존 도서 번호 및 연결 독후감 기간 동기화를 검증함
        assertEquals(200, result.getCode());
        assertEquals(Map.of("rondNumb", 1L), result.getData());
        assertEquals(99L, request.getBookNumb());
        verify(readingClubMapper, never()).getWrittenReadingReportCnt(10L, 1L);
        verify(readingClubMapper).uptReadingReportList(10L, 1L, request);
        verify(bookMapper, never()).dupBook(any());
    }

    /**
     * 작성된 연결 독후감이 하나라도 있으면 다른 도서로 변경하지 않는지 검증함
     *
     * @author Hanwon.Jang
     */
    @Test
    void rejectBookChangeReport() {

        // 활성 모임장과 현재 도서 및 다른 ISBN의 수정 요청을 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setClubStat("ACTIVE");
        ReadingClubDto.ReadingManageDto reading = new ReadingClubDto.ReadingManageDto();
        reading.setBookNumb(99L);
        reading.setBookIsbn("9781234567890");
        ReadingClubDto.ReadingUpdateReqDto request = createReadingUpdate("9780987654321");

        // 연결 독후감 잠금 뒤 작성된 독후감 한 건이 조회되도록 설정함
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.getActiveOwnerCnt(10L, 20L)).thenReturn(1);
        when(readingClubMapper.getReadingForUpdate(10L, 1L)).thenReturn(reading);
        when(readingClubMapper.getReadingReportNumbListForUpdate(10L, 1L)).thenReturn(List.of(120L));
        when(readingClubMapper.getWrittenReadingReportCnt(10L, 1L)).thenReturn(1);

        // 작성된 독후감이 있는 회차의 도서 변경을 요청함
        ResultData result = readingClubService.uptReading(20L, 10L, 1L, request);

        // 정책 전용 실패 응답과 모든 변경 Mapper 미호출을 검증함
        assertEquals(ResultEnum.READING_CLUB_BOOK_CHANGE_REJECTED.getCode(), result.getCode());
        verify(readingClubMapper, never()).uptReading(any(), any(), any());
        verify(readingClubMapper, never()).uptReadingReportList(any(), any(), any());
        verify(bookMapper, never()).dupBook(any());
    }

    /**
     * 작성된 독후감이 없으면 다른 도서와 기간을 회차 및 연결 독후감에 반영하는지 검증함
     *
     * @author Hanwon.Jang
     */
    @Test
    void changeBookBeforeReport() {

        // 활성 모임장과 현재 도서 및 다른 ISBN의 수정 요청을 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setClubStat("ACTIVE");
        ReadingClubDto.ReadingManageDto reading = new ReadingClubDto.ReadingManageDto();
        reading.setBookNumb(99L);
        reading.setBookIsbn("9781234567890");
        ReadingClubDto.ReadingUpdateReqDto request = createReadingUpdate("9780987654321");

        // 작성된 독후감이 없고 변경 도서가 도서 마스터에 존재하도록 설정함
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.getActiveOwnerCnt(10L, 20L)).thenReturn(1);
        when(readingClubMapper.getReadingForUpdate(10L, 1L)).thenReturn(reading);
        when(readingClubMapper.getReadingReportNumbListForUpdate(10L, 1L)).thenReturn(List.of(120L));
        when(readingClubMapper.getWrittenReadingReportCnt(10L, 1L)).thenReturn(0);
        when(bookMapper.dupBook(request)).thenReturn(1);
        when(bookMapper.getBookNumbByIsbn(request)).thenReturn(100L);
        when(readingClubMapper.uptReading(10L, 1L, request)).thenReturn(1);

        // 현재 회차의 도서와 목표 기간 수정을 실행함
        ResultData result = readingClubService.uptReading(20L, 10L, 1L, request);

        // 새 도서 번호와 회차 및 연결 독후감 동기화 성공을 검증함
        assertEquals(200, result.getCode());
        assertEquals(100L, request.getBookNumb());
        verify(readingClubMapper).uptReading(10L, 1L, request);
        verify(readingClubMapper).uptReadingReportList(10L, 1L, request);
    }

    /**
     * 비활성 계정의 모임장은 현재 독서 수정에 접근할 수 없는지 검증함
     *
     * @author Hanwon.Jang
     */
    @Test
    void uptReadingRejectsInactive() {

        // 운영 중인 모임과 유효한 수정 요청을 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setClubStat("ACTIVE");
        ReadingClubDto.ReadingUpdateReqDto request = createReadingUpdate("9781234567890");
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.getActiveOwnerCnt(10L, 20L)).thenReturn(0);

        // 비활성 계정의 모임 독서 수정을 요청함
        ResultData result = readingClubService.uptReading(20L, 10L, 1L, request);

        // 접근 거부 응답과 회차 조회 및 수정 미실행을 검증함
        assertEquals(ResultEnum.COMMON_ACCESS_REJECTED.getCode(), result.getCode());
        verify(readingClubMapper, never()).getReadingForUpdate(any(), any());
        verify(readingClubMapper, never()).uptReading(any(), any(), any());
    }

    /**
     * 활성 모임장이 전원 완독 회차를 조기 마감하면 목표 결과를 확정하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void completeReadingEarly() {
        // 운영 중인 모임과 잠금 가능한 현재 회차를 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setClubStat("ACTIVE");
        ReadingClubDto.ReadingManageDto reading = new ReadingClubDto.ReadingManageDto();
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.getActiveOwnerCnt(10L, 20L)).thenReturn(1);
        when(readingClubMapper.getReadingForUpdate(10L, 1L)).thenReturn(reading);
        when(readingClubMapper.getReadingReportNumbListForUpdate(10L, 1L)).thenReturn(List.of(120L, 121L));
        when(readingClubMapper.uptEarlyReadingRound(10L, 1L)).thenReturn(1);
        when(readingClubMapper.uptEarlyReadingGoal(10L, 1L)).thenReturn(2);
        when(readingClubMapper.setEarlyResultTarget(10L, 1L)).thenReturn(2);

        // 활성 모임장이 현재 독서의 조기 마감을 요청함
        ResultData result = readingClubService.uptReadingCompletion(20L, 10L, 1L);

        // 완료 회차 번호와 회차 및 참여자 목표 확정 순서를 검증함
        assertEquals(200, result.getCode());
        assertEquals(Map.of("rondNumb", 1L), result.getData());
        InOrder completionOrder = org.mockito.Mockito.inOrder(readingClubMapper);
        completionOrder.verify(readingClubMapper).uptEarlyReadingRound(10L, 1L);
        completionOrder.verify(readingClubMapper).uptEarlyReadingGoal(10L, 1L);
        completionOrder.verify(readingClubMapper).setEarlyResultTarget(10L, 1L);
    }

    /**
     * 활성 모임원이 닫은 결과 팝업을 확인 처리하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void confirmReadingResult() {
        // 요청 사용자가 활성 계정인 현재 활성 모임원이 되도록 구성함
        when(readingClubMapper.getActiveMemberAccessCnt(10L, 20L)).thenReturn(1);
        when(readingClubMapper.uptReadingResultConfirm(10L, 1L, 20L)).thenReturn(1);

        // 사용자가 팝업에서 첫 번째 회차 결과를 직접 닫음
        ResultData result = readingClubService.uptReadingResultConfirm(20L, 10L, 1L);

        // 확인 일시 저장과 성공 응답을 검증함
        assertEquals(200, result.getCode());
        verify(readingClubMapper).uptReadingResultConfirm(10L, 1L, 20L);
    }

    /**
     * 활성 계정인 현재 모임원이 아니면 결과 확인을 거절하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void rejectResultConfirmAccess() {
        // 요청 사용자가 활성 계정인 현재 모임원이 아니도록 구성함
        when(readingClubMapper.getActiveMemberAccessCnt(10L, 20L)).thenReturn(0);

        // 접근 권한이 없는 사용자가 첫 번째 회차 결과 확인을 요청함
        ResultData result = readingClubService.uptReadingResultConfirm(20L, 10L, 1L);

        // 접근 거부와 확인 상태 미변경을 검증함
        assertEquals(ResultEnum.COMMON_ACCESS_REJECTED.getCode(), result.getCode());
        verify(readingClubMapper, never()).uptReadingResultConfirm(any(), any(), any());
    }

    /**
     * 활성 모임장이 아닌 사용자의 조기 마감 요청을 거절하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void rejectEarlyCloseNonOwner() {
        // 운영 중인 모임이지만 요청 사용자가 활성 모임장이 아닌 상태를 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setClubStat("ACTIVE");
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.getActiveOwnerCnt(10L, 20L)).thenReturn(0);

        // 활성 모임장이 아닌 사용자가 현재 독서의 조기 마감을 요청함
        ResultData result = readingClubService.uptReadingCompletion(20L, 10L, 1L);

        // 접근 거부와 회차 상태 변경 미실행을 검증함
        assertEquals(ResultEnum.COMMON_ACCESS_REJECTED.getCode(), result.getCode());
        verify(readingClubMapper, never()).getReadingForUpdate(any(), any());
        verify(readingClubMapper, never()).uptEarlyReadingRound(any(), any());
    }

    /**
     * 모임 독서 등록 테스트에 사용할 선택 도서와 목표 기간을 구성함
     *
     * @author Hanwon.Jang
     * @return 유효한 모임 독서 등록 요청
     */
    private ReadingClubDto.ReadingCreateReqDto createReadingRequest() {

        // 외부 도서 검색 결과와 목표 기간 및 중복 방지 키를 설정함
        ReadingClubDto.ReadingCreateReqDto request = new ReadingClubDto.ReadingCreateReqDto();
        request.setBookTitl("테스트 도서");
        request.setBookAthr("테스트 저자");
        request.setBookPubl("테스트 출판사");
        request.setBookIsbn("9781234567890");
        request.setBookCvim("https://example.com/book.jpg");
        request.setBookDesc("테스트 도서 소개");
        request.setPublDate("2026-08-01");
        request.setGoalStdt("2026-08-14");
        request.setGoalEndt("2026-08-31");
        request.setIdemKeyx("reading-request-1");
        // 테스트용 등록 요청을 반환함
        return request;
    }

    /**
     * 현재 독서 수정 테스트에 사용할 도서와 목표 기간을 구성함
     *
     * @author Hanwon.Jang
     * @param bookIsbn 수정 요청에 사용할 ISBN
     * @return 유효한 현재 독서 수정 요청
     */
    private ReadingClubDto.ReadingUpdateReqDto createReadingUpdate(String bookIsbn) {

        // 외부 도서 검색 결과와 변경할 목표 기간을 설정함
        ReadingClubDto.ReadingUpdateReqDto request = new ReadingClubDto.ReadingUpdateReqDto();
        request.setBookTitl("수정 테스트 도서");
        request.setBookAthr("테스트 저자");
        request.setBookPubl("테스트 출판사");
        request.setBookIsbn(bookIsbn);
        request.setBookCvim("https://example.com/updated-book.jpg");
        request.setBookDesc("수정 테스트 도서 소개");
        request.setPublDate("2026-08-01");
        request.setGoalStdt("2026-08-20");
        request.setGoalEndt("2026-09-10");
        // 테스트용 수정 요청을 반환함
        return request;
    }

    /**
     * 모임장이 활성 맞팔 회원을 초대하면 예약석 저장과 초대 알림 발송을 함께 수행하는지 검증함
     *
     * @author Hanwon.Jang
     */
    @Test
    void setInviteSendsAlim() {
        // 초대 권한과 알림 문구를 제공할 운영 중인 모임 정보를 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        // 현재 사용자를 모임장으로 설정함
        club.setOwnrNumb(20L);
        // 알림 사용자명 치환값으로 사용할 모임장 닉네임을 설정함
        club.setOwnrNick("모임장");
        // 알림 모임명 치환값으로 사용할 모임명을 설정함
        club.setClubName("함께 읽는 모임");
        // 초대가 가능한 운영 상태를 설정함
        club.setClubStat("ACTIVE");
        // 초대 예약석을 확보할 수 있는 정원을 설정함
        club.setMaxxMemb(10);

        // 한 명의 맞팔 회원을 선택한 초대 요청을 구성함
        ReadingClubDto.InviteReqDto request = new ReadingClubDto.InviteReqDto();
        // 초대 대상 사용자 번호를 설정함
        request.setUserNumbList(List.of(30L));

        // 모임 잠금과 좌석 및 맞팔 검증을 모두 통과하도록 조회 결과를 구성함
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.getOccupiedSeatCnt(10L)).thenReturn(1);
        when(readingClubMapper.getMutualFollowCnt(20L, 30L)).thenReturn(1);
        when(readingClubMapper.getClubMember(10L, 30L)).thenReturn(null);
        // INVITE_CLUB 템플릿 기반 알림 저장이 성공하도록 결과를 구성함
        when(alimService.sendAlim(
                30L
              , Constant.ALIM_SITU_FOLLOW_CLUB
              , Constant.ALIM_TEMP_CODE_INVITE_CLUB
              , Constant.ALIM_TARGET_READING_CLUB
              , 10L
              , null
              , Map.of("userName", "모임장", "clubName", "함께 읽는 모임")
        )).thenReturn(ResultData.success());

        // 모임장으로 활성 맞팔 회원을 초대함
        ResultData result = readingClubService.setInvitation(20L, 10L, request);

        // 초대 성공과 예약석 저장 및 템플릿 기반 알림 발송을 검증함
        assertEquals(200, result.getCode());
        // 팔로우 요청과 모임 초대가 FOLLOW 상황 코드로 통합됐는지 확인함
        assertEquals("FOLLOW", Constant.ALIM_SITU_FOLLOW_CLUB);
        verify(readingClubMapper).setInvitation(10L, 30L, 20L);
        verify(alimService).sendAlim(
                30L
              , Constant.ALIM_SITU_FOLLOW_CLUB
              , Constant.ALIM_TEMP_CODE_INVITE_CLUB
              , Constant.ALIM_TARGET_READING_CLUB
              , 10L
              , null
              , Map.of("userName", "모임장", "clubName", "함께 읽는 모임")
        );
    }

    /**
     * 초대 알림 저장이 실패하면 초대 처리도 성공으로 확정하지 않는지 검증함
     *
     * @author SeungHyeon.Kang
     */
    @Test
    void setInviteFailsWithAlim() {
        // 초대 권한과 알림 문구를 제공할 운영 중인 모임 정보를 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        // 현재 사용자를 모임장으로 설정함
        club.setOwnrNumb(20L);
        // 알림 사용자명 치환값으로 사용할 모임장 닉네임을 설정함
        club.setOwnrNick("모임장");
        // 알림 모임명 치환값으로 사용할 모임명을 설정함
        club.setClubName("함께 읽는 모임");
        // 초대가 가능한 운영 상태를 설정함
        club.setClubStat("ACTIVE");
        // 초대 예약석을 확보할 수 있는 정원을 설정함
        club.setMaxxMemb(10);

        // 한 명의 맞팔 회원을 선택한 초대 요청을 구성함
        ReadingClubDto.InviteReqDto request = new ReadingClubDto.InviteReqDto();
        // 초대 대상 사용자 번호를 설정함
        request.setUserNumbList(List.of(30L));

        // 모임 잠금과 좌석 및 맞팔 검증을 모두 통과하도록 조회 결과를 구성함
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.getOccupiedSeatCnt(10L)).thenReturn(1);
        when(readingClubMapper.getMutualFollowCnt(20L, 30L)).thenReturn(1);
        when(readingClubMapper.getClubMember(10L, 30L)).thenReturn(null);
        // 사용할 수 있는 초대 알림 템플릿이 없는 실패 결과를 구성함
        when(alimService.sendAlim(
                30L
              , Constant.ALIM_SITU_FOLLOW_CLUB
              , Constant.ALIM_TEMP_CODE_INVITE_CLUB
              , Constant.ALIM_TARGET_READING_CLUB
              , 10L
              , null
              , Map.of("userName", "모임장", "clubName", "함께 읽는 모임")
        )).thenReturn(ResultData.fail(ResultEnum.COMMON_NO_DATA));

        // 초대 알림을 저장할 수 없으면 트랜잭션을 롤백할 예외가 발생하는지 검증함
        CustomException exception = assertThrows(
                CustomException.class
              , () -> readingClubService.setInvitation(20L, 10L, request)
        );

        // 사용자에게는 초대 저장 실패로 응답하도록 공통 결과 코드를 검증함
        assertEquals(ResultEnum.COMMON_SAVE_REJECTED, exception.getResultEnum());
        // 알림 저장 전에 초대 예약석 저장을 시도했는지 검증함
        verify(readingClubMapper).setInvitation(10L, 30L, 20L);
    }

    /**
     * 모임장이 운영 제약을 지키는 입력으로 모임 정보와 관계 데이터를 수정하는지 검증함
     *
     * @author Hanwon.Jang
     */
    @Test
    void uptOwnedActiveClub() {
        // 수정 권한과 기존 운영 설정을 가진 모임 정보를 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        // 현재 사용자를 모임장으로 설정함
        club.setOwnrNumb(20L);
        // 수정 가능한 운영 상태를 설정함
        club.setClubStat("ACTIVE");
        // 공개 범위 변경 검사가 필요하지 않은 기존 값을 설정함
        club.setClubVisb("PUBLIC");
        // 가입 방식 변경 검사가 필요하지 않은 기존 값을 설정함
        club.setJoinType("OPEN");

        // 수정할 모임 기본 정보와 운영 설정을 구성함
        ReadingClubDto.ClubCreateReqDto request = new ReadingClubDto.ClubCreateReqDto();
        // 수정할 모임명을 설정함
        request.setClubName("수정한 모임");
        // 수정할 모임 소개를 설정함
        request.setClubCntn("수정한 소개");
        // 유지할 공개 범위를 설정함
        request.setClubVisb("PUBLIC");
        // 유지할 가입 방식을 설정함
        request.setJoinType("OPEN");
        // 현재 점유 좌석보다 큰 정원을 설정함
        request.setMaxxMemb(10);
        // 새로 저장할 카테고리 목록을 설정함
        request.setCategoryList(List.of("NOVEL"));
        // 즉시 가입 방식에는 질문이 없도록 설정함
        request.setQuestionList(List.of());

        // 수정 완료 뒤 반환할 상세 정보를 구성함
        ReadingClubDto.ClubViewDto updatedClub = new ReadingClubDto.ClubViewDto();
        // 상세 조회 대상 모임 번호를 설정함
        updatedClub.setClubNumb(10L);
        // 상세 조회에서 질문을 결합하지 않는 가입 방식을 설정함
        updatedClub.setJoinType("OPEN");

        // 권한과 입력 및 좌석 검증부터 수정 후 상세 조회까지 성공하도록 구성함
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.getValidCategoryCnt(List.of("NOVEL"))).thenReturn(1);
        when(readingClubMapper.getOccupiedSeatCnt(10L)).thenReturn(2);
        when(readingClubMapper.uptClub(20L, 10L, request)).thenReturn(1);
        when(readingClubMapper.getClubDtl(10L, 20L)).thenReturn(updatedClub);
        when(readingClubMapper.getClubCategoryList(10L)).thenReturn(List.of());

        // 모임장으로 모임 정보를 수정함
        ResultData result = readingClubService.uptClub(20L, 10L, request);

        // 수정 성공과 카테고리 관계 갱신을 검증함
        assertEquals(200, result.getCode());
        verify(readingClubMapper).delClubCategory(10L);
        verify(readingClubMapper).setClubCategory(10L, "NOVEL", 1);
    }

    /**
     * 모임장이 현재 점유 좌석보다 작은 정원으로 모임을 수정하지 못하는지 검증함
     *
     * @author Hanwon.Jang
     */
    @Test
    void rejectCapacityBelowSeats() {
        // 수정 권한을 가진 운영 중 모임 정보를 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        // 현재 사용자를 모임장으로 설정함
        club.setOwnrNumb(20L);
        // 수정 가능한 운영 상태를 설정함
        club.setClubStat("ACTIVE");
        // 기존 공개 범위를 설정함
        club.setClubVisb("PUBLIC");
        // 기존 가입 방식을 설정함
        club.setJoinType("OPEN");

        // 현재 좌석보다 작은 정원을 가진 수정 요청을 구성함
        ReadingClubDto.ClubCreateReqDto request = new ReadingClubDto.ClubCreateReqDto();
        // 유효한 모임명을 설정함
        request.setClubName("수정한 모임");
        // 유효한 모임 소개를 설정함
        request.setClubCntn("수정한 소개");
        // 유지할 공개 범위를 설정함
        request.setClubVisb("PUBLIC");
        // 유지할 가입 방식을 설정함
        request.setJoinType("OPEN");
        // 점유 좌석보다 작은 정원을 설정함
        request.setMaxxMemb(2);
        // 유효한 카테고리 목록을 설정함
        request.setCategoryList(List.of("NOVEL"));
        // 질문이 필요 없는 가입 방식의 빈 질문 목록을 설정함
        request.setQuestionList(List.of());

        // 권한과 입력은 통과하지만 현재 세 좌석이 점유된 상태를 구성함
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.getValidCategoryCnt(List.of("NOVEL"))).thenReturn(1);
        when(readingClubMapper.getOccupiedSeatCnt(10L)).thenReturn(3);

        // 현재 점유 좌석보다 작은 정원으로 수정을 요청함
        ResultData result = readingClubService.uptClub(20L, 10L, request);

        // 수정 거절과 모임 마스터 미수정을 검증함
        assertEquals(ResultEnum.COMMON_UPDATE_REJECTED.getCode(), result.getCode());
        verify(readingClubMapper, never()).uptClub(20L, 10L, request);
    }

    /**
     * 계정 상태 SQL 가드를 통과하지 못한 모임장의 수정 후속 저장을 차단하는지 검증함
     *
     * @author Hanwon.Jang
     */
    @Test
    void rejectRestrictedOwnerUpt() {
        // 소유 관계는 남아 있지만 계정 상태 SQL 가드가 필요한 모임을 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        // 요청 사용자를 기존 모임장으로 설정함
        club.setOwnrNumb(20L);
        // 소유 모임 자체는 운영 상태로 설정함
        club.setClubStat("ACTIVE");
        // 공개 범위 변경 검사가 필요하지 않은 기존 값을 설정함
        club.setClubVisb("PUBLIC");
        // 가입 방식 변경 검사가 필요하지 않은 기존 값을 설정함
        club.setJoinType("OPEN");

        // 기본 유효성 검사를 통과할 수정 요청을 구성함
        ReadingClubDto.ClubCreateReqDto request = new ReadingClubDto.ClubCreateReqDto();
        // 유효한 모임명을 설정함
        request.setClubName("수정한 모임");
        // 유효한 소개를 설정함
        request.setClubCntn("수정한 소개");
        // 유지할 공개 범위를 설정함
        request.setClubVisb("PUBLIC");
        // 유지할 가입 방식을 설정함
        request.setJoinType("OPEN");
        // 현재 좌석보다 큰 정원을 설정함
        request.setMaxxMemb(10);
        // 유효한 카테고리를 설정함
        request.setCategoryList(List.of("NOVEL"));
        // 즉시 가입에는 질문을 사용하지 않음
        request.setQuestionList(List.of());

        // 입력 검증은 통과하지만 활성 계정 조건이 포함된 수정 SQL은 실패하도록 구성함
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.getValidCategoryCnt(List.of("NOVEL"))).thenReturn(1);
        when(readingClubMapper.getOccupiedSeatCnt(10L)).thenReturn(2);
        when(readingClubMapper.uptClub(20L, 10L, request)).thenReturn(0);

        // 계정 제한 상태를 SQL 가드로 모사해 모임 수정을 요청함
        ResultData result = readingClubService.uptClub(20L, 10L, request);

        // 수정 거절 뒤 만료 초대와 관계 데이터를 변경하지 않았는지 검증함
        assertEquals(ResultEnum.COMMON_UPDATE_REJECTED.getCode(), result.getCode());
        verify(readingClubMapper, never()).delExpiredInvitation(10L);
        verify(readingClubMapper, never()).delClubCategory(10L);
    }

    /**
     * 모임장이 운영 중인 자신의 모임을 물리 삭제하는지 검증함
     *
     * @author Hanwon.Jang
     */
    @Test
    void delOwnedActiveClub() {
        // 삭제 권한을 가진 운영 중 모임 정보를 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        // 현재 사용자를 모임장으로 설정함
        club.setOwnrNumb(20L);
        // 삭제 가능한 운영 상태를 설정함
        club.setClubStat("ACTIVE");

        // 모임 잠금 조회와 소유권 조건이 포함된 삭제가 성공하도록 구성함
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.delClub(20L, 10L)).thenReturn(1);

        // 모임장으로 모임 물리 삭제를 요청함
        ResultData result = readingClubService.delClub(20L, 10L);

        // 삭제 성공과 모임 마스터 삭제 호출을 검증함
        assertEquals(200, result.getCode());
        // 외래키 자식과 결선 투표를 부모보다 먼저 삭제하는 순서 검증
        InOrder deletionOrder = org.mockito.Mockito.inOrder(readingClubMapper);
        deletionOrder.verify(readingClubMapper).getClubForUpdate(10L);
        deletionOrder.verify(readingClubMapper).delClubBallots(10L);
        deletionOrder.verify(readingClubMapper).delClubVotes(10L, 2);
        deletionOrder.verify(readingClubMapper).delClubVotes(10L, 1);
        deletionOrder.verify(readingClubMapper).delClubElections(10L);
        deletionOrder.verify(readingClubMapper).delClubSelections(10L);
        deletionOrder.verify(readingClubMapper).delClub(20L, 10L);
    }

    /**
     * 계정 상태 SQL 가드를 통과하지 못한 모임장의 물리 삭제를 차단하는지 검증함
     *
     * @author Hanwon.Jang
     */
    @Test
    void rejectRestrictedOwnerDel() {
        // 소유 관계는 남아 있지만 계정 상태 SQL 가드가 필요한 모임을 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        // 요청 사용자를 기존 모임장으로 설정함
        club.setOwnrNumb(20L);
        // 소유 모임 자체는 운영 상태로 설정함
        club.setClubStat("ACTIVE");

        // 활성 계정 조건이 포함된 삭제 SQL이 대상을 찾지 못하도록 구성함
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.delClub(20L, 10L)).thenReturn(0);

        // 마지막 삭제 거절 시 트랜잭션 롤백을 유발하는 업무 예외 검증
        CustomException exception = assertThrows(CustomException.class, () -> readingClubService.delClub(20L, 10L));
        assertEquals(ResultEnum.COMMON_DELETE_REJECTED, exception.getResultEnum());
    }

    /**
     * 공개형 모임의 즉시 가입이 완료되면 모임장에게 신규 멤버 알림을 발송하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void setOpenJoinAlertsOwner() {
        // 즉시 가입이 가능한 공개형 모임과 알림 수신 모임장 정보를 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setClubStat("ACTIVE");
        club.setClubVisb("PUBLIC");
        club.setJoinType("OPEN");
        club.setOwnrNumb(30L);
        club.setClubName("책벌레 모임");
        club.setMaxxMemb(10);

        // 좌석이 남은 모임의 멤버 등록과 모임장 알림이 모두 성공하도록 구성함
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.getOccupiedSeatCnt(10L)).thenReturn(1);
        when(readingClubMapper.setActiveMember(10L, 20L)).thenReturn(1);
        when(alimService.sendAlim(30L, Constant.ALIM_SITU_FOLLOW_CLUB
                , Constant.ALIM_TEMP_CODE_CLUB_MEMBER_JOINED, Constant.ALIM_TARGET_READING_CLUB, 10L, null
                , Map.of("clubName", "책벌레 모임"))).thenReturn(ResultData.success());

        // 공개형 모임에 즉시 가입함
        readingClubService.setJoin(20L, 10L, new ReadingClubDto.JoinReqDto());

        // 모임장에게 모임명만 포함한 신규 멤버 가입 알림이 발송되는지 검증함
        verify(alimService).sendAlim(
                30L
              , Constant.ALIM_SITU_FOLLOW_CLUB
              , Constant.ALIM_TEMP_CODE_CLUB_MEMBER_JOINED
              , Constant.ALIM_TARGET_READING_CLUB
              , 10L
              , null
              , Map.of("clubName", "책벌레 모임")
        );
    }

    /**
     * 관리자가 모집을 중지한 활성 모임에 신규 가입을 허용하지 않는지 검증함
     *
     * @author SeungHyeon.Kang
     */
    @Test
    void setJoinRejectsRecruitStop() {
        // 운영 상태는 활성이나 관리자 모집 중지가 적용된 공개 모임을 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        // 기존 모임원 활동을 유지하는 정상 운영 상태를 설정함
        club.setClubStat("ACTIVE");
        // 공개 목록에서 접근 가능한 공개 범위를 설정함
        club.setClubVisb("PUBLIC");
        // 즉시 가입 방식에서도 모집 중지 정책이 우선하도록 설정함
        club.setJoinType("OPEN");
        // 관리자 모집 중지 여부를 명시적으로 설정함
        club.setRcrtYsno("N");
        // 잠금 조회에서 모집 중지 모임을 반환하도록 설정함
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);

        // 모집 중지 모임에 신규 가입을 요청함
        ResultData result = readingClubService.setJoin(20L, 10L, new ReadingClubDto.JoinReqDto());

        // 관리자 모집 중지가 신규 가입보다 우선해 접근 거절되는지 검증함
        assertEquals(ResultEnum.COMMON_ACCESS_REJECTED.getCode(), result.getCode());
        // 모집 중지 상태에서는 좌석 계산을 시작하지 않는지 검증함
        verify(readingClubMapper, never()).getOccupiedSeatCnt(10L);
        // 모집 중지 상태에서는 활성 회원 관계를 만들지 않는지 검증함
        verify(readingClubMapper, never()).setActiveMember(10L, 20L);
    }

    /**
     * 재가입 차단이 없는 자진 탈퇴 회원이 공개 모임에 다시 가입하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void setOpenJoinAllowsLeaver() {
        // 즉시 재가입이 가능한 공개형 모임을 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setClubStat("ACTIVE");
        club.setClubVisb("PUBLIC");
        club.setJoinType("OPEN");
        club.setOwnrNumb(30L);
        club.setClubName("책벌레 모임");
        club.setMaxxMemb(10);

        // 재가입 차단 없이 자진 탈퇴한 기존 회원 관계를 구성함
        ReadingClubDto.MemberDto member = new ReadingClubDto.MemberDto();
        member.setMembStat("EXITED");
        member.setBlocYsno("N");

        // 기존 회원 행 갱신과 모임장 알림이 성공하도록 구성함
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.getClubMember(10L, 20L)).thenReturn(member);
        when(readingClubMapper.getOccupiedSeatCnt(10L)).thenReturn(1);
        when(readingClubMapper.setActiveMember(10L, 20L)).thenReturn(2);
        when(alimService.sendAlim(30L, Constant.ALIM_SITU_FOLLOW_CLUB
                , Constant.ALIM_TEMP_CODE_CLUB_MEMBER_JOINED, Constant.ALIM_TARGET_READING_CLUB, 10L, null
                , Map.of("clubName", "책벌레 모임"))).thenReturn(ResultData.success());

        // 자진 탈퇴 회원이 공개형 모임에 즉시 재가입함
        readingClubService.setJoin(20L, 10L, new ReadingClubDto.JoinReqDto());

        // 기존 탈퇴 회원 행이 활성 회원 관계로 갱신되는지 검증함
        verify(readingClubMapper).setActiveMember(10L, 20L);
    }

    /**
     * 공개형 모임의 신규 멤버 알림 저장 실패가 즉시 가입을 롤백하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void setOpenJoinAlimRollsBack() {
        // 즉시 가입이 가능한 공개형 모임과 알림 수신 모임장 정보를 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setClubStat("ACTIVE");
        club.setClubVisb("PUBLIC");
        club.setJoinType("OPEN");
        club.setOwnrNumb(30L);
        club.setClubName("책벌레 모임");
        club.setMaxxMemb(10);

        // 멤버 등록 후 알림 템플릿 조회가 실패하도록 구성함
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.getOccupiedSeatCnt(10L)).thenReturn(1);
        when(readingClubMapper.setActiveMember(10L, 20L)).thenReturn(1);
        when(alimService.sendAlim(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(ResultData.fail(ResultEnum.COMMON_NO_DATA));

        // 알림 없이 멤버 관계만 저장되는 상태를 허용하지 않는지 검증함
        assertThrows(CustomException.class
                , () -> readingClubService.setJoin(20L, 10L, new ReadingClubDto.JoinReqDto()));
    }

    /**
     * 승인형 모임 가입 신청이 저장되면 모임장에게 신청 알림을 발송하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void setJoinAlertsOwner() {
        // 승인형 공개 모임과 모임장 및 알림 문구에 사용할 모임명을 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setClubStat("ACTIVE");
        club.setClubVisb("PUBLIC");
        club.setJoinType("APPROVAL");
        club.setOwnrNumb(30L);
        club.setClubName("책벌레 모임");

        // 현재 가입 질문과 신청자의 답변을 같은 순서로 구성함
        ReadingClubDto.QuestionDto question = new ReadingClubDto.QuestionDto();
        question.setQuesFirs("가입 이유를 알려주세요.");
        ReadingClubDto.JoinReqDto request = new ReadingClubDto.JoinReqDto();
        request.setAnswerList(List.of("함께 읽고 싶어요."));

        // 가입 신청 저장과 모임장 알림 발송이 모두 성공하도록 구성함
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.getClubQuestion(10L)).thenReturn(question);
        when(badWordDetectionService.findBadWord("함께 읽고 싶어요.")).thenReturn(Optional.empty());
        when(readingClubMapper.setJoinApplication(any())).thenReturn(1);
        when(alimService.sendAlim(30L, Constant.ALIM_SITU_FOLLOW_CLUB
                , Constant.ALIM_TEMP_CODE_CLUB_JOIN_REQUESTED, Constant.ALIM_TARGET_READING_CLUB, 10L, null
                , Map.of("clubName", "책벌레 모임"))).thenReturn(ResultData.success());

        // 승인형 모임 가입을 신청함
        readingClubService.setJoin(20L, 10L, request);

        // 모임장에게 신청자 식별정보 없이 모임명만 치환하는 알림이 발송되는지 검증함
        verify(alimService).sendAlim(
                30L
              , Constant.ALIM_SITU_FOLLOW_CLUB
              , Constant.ALIM_TEMP_CODE_CLUB_JOIN_REQUESTED
              , Constant.ALIM_TARGET_READING_CLUB
              , 10L
              , null
              , Map.of("clubName", "책벌레 모임")
        );
    }

    /**
     * 가입 거절 후 재신청 제한 기간에는 새 승인 가입 신청을 저장하지 않는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void setJoinBlocksRejected() {
        // 승인형 공개 모임을 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setClubStat("ACTIVE");
        club.setClubVisb("PUBLIC");
        club.setJoinType("APPROVAL");

        // 최근 거절된 가입 신청을 재신청 제한 기준으로 구성함
        ReadingClubDto.ApplicationDto application = new ReadingClubDto.ApplicationDto();
        application.setJoinStat("REJECTED");

        // 모임 조회와 재신청 제한 조회가 최근 거절 신청을 반환하도록 구성함
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.getBlockedApplication(10L, 20L)).thenReturn(application);

        // 재신청 제한 기간에 승인형 모임 가입을 다시 요청함
        ResultData result = readingClubService.setJoin(20L, 10L, new ReadingClubDto.JoinReqDto());

        // 저장 거절 응답과 가입 신청 미등록을 검증함
        assertEquals(ResultEnum.COMMON_SAVE_REJECTED.getCode(), result.getCode());
        verify(readingClubMapper, never()).setJoinApplication(any());
    }

    /**
     * 신규 가입 신청 알림 저장이 실패하면 신청 저장도 롤백 예외로 종료하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void setJoinAlimFailRollsBack() {
        // 승인형 공개 모임과 모임장 정보를 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setClubStat("ACTIVE");
        club.setClubVisb("PUBLIC");
        club.setJoinType("APPROVAL");
        club.setOwnrNumb(30L);
        club.setClubName("책벌레 모임");

        // 현재 가입 질문과 신청자의 답변을 같은 순서로 구성함
        ReadingClubDto.QuestionDto question = new ReadingClubDto.QuestionDto();
        question.setQuesFirs("가입 이유를 알려주세요.");
        ReadingClubDto.JoinReqDto request = new ReadingClubDto.JoinReqDto();
        request.setAnswerList(List.of("함께 읽고 싶어요."));

        // 신청 저장 후 알림 템플릿 조회 실패가 반환되도록 구성함
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.getClubQuestion(10L)).thenReturn(question);
        when(badWordDetectionService.findBadWord("함께 읽고 싶어요.")).thenReturn(Optional.empty());
        when(readingClubMapper.setJoinApplication(any())).thenReturn(1);
        when(alimService.sendAlim(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(ResultData.fail(ResultEnum.COMMON_NO_DATA));

        // 알림 없이 가입 신청만 저장되는 상태를 허용하지 않는지 검증함
        assertThrows(CustomException.class, () -> readingClubService.setJoin(20L, 10L, request));
    }

    /**
     * 활성 모임원이 활성 모임원 목록을 조회하는지 검증함
     *
     * @author Hanwon.Jang
     */
    @Test
    void getActiveClubMembers() {
        // 조회 요청 사용자의 활성 모임원 관계를 구성함
        ReadingClubDto.MemberDto requester = new ReadingClubDto.MemberDto();
        requester.setMembStat("ACTIVE");

        // 모임원 목록에 반환할 프로필 정보를 구성함
        ReadingClubDto.MemberProfileDto profile = new ReadingClubDto.MemberProfileDto();
        profile.setUserNumb(20L);
        profile.setUserNick("모임원");
        profile.setMembRole("MEMBER");

        // 조회 요청 사용자의 모임원 관계와 활성 모임원 목록을 반환함
        when(readingClubMapper.getClubMember(10L, 20L)).thenReturn(requester);
        when(readingClubMapper.getActiveMemberAccessCnt(10L, 20L)).thenReturn(1);
        when(readingClubMapper.getClubMemberList(10L)).thenReturn(List.of(profile));

        // 활성 모임원으로 모임원 프로필 목록을 조회함
        ResultData result = readingClubService.getClubMemberList(20L, 10L);

        // 성공 코드와 조회한 프로필 목록을 검증함
        assertEquals(200, result.getCode());
        assertEquals(List.of(profile), result.getData());
        verify(readingClubMapper).getClubMemberList(10L);
    }

    /**
     * 활성 모임원 관계가 없는 사용자의 프로필 목록 조회를 거절하는지 검증함
     *
     * @author SeungHyeon.Kang
     */
    @Test
    void rejectClubMemberList() {
        // 조회 요청 사용자의 모임원 관계가 없도록 구성함
        when(readingClubMapper.getClubMember(10L, 20L)).thenReturn(null);

        // 모임 외부 사용자로 모임원 프로필 목록을 조회함
        ResultData result = readingClubService.getClubMemberList(20L, 10L);

        // 접근 거절 코드와 목록 SQL 미호출을 검증함
        assertEquals(ResultEnum.COMMON_ACCESS_REJECTED.getCode(), result.getCode());
        verify(readingClubMapper, never()).getClubMemberList(10L);
    }

    /**
     * 공개 중인 활성 모임은 비회원에게도 활성 모임원 목록을 제공하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void getPublicClubMembers() {
        // 비회원이 조회할 공개 중인 활성 모임을 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setClubVisb("PUBLIC");
        club.setClubStat("ACTIVE");
        // 공개 가능한 활성 모임원 프로필을 구성함
        ReadingClubDto.MemberProfileDto profile = new ReadingClubDto.MemberProfileDto();
        profile.setUserNumb(30L);
        // 활성 모임원 관계가 없는 비회원과 공개 모임 조회 결과를 설정함
        when(readingClubMapper.getActiveMemberAccessCnt(10L, 20L)).thenReturn(0);
        when(readingClubMapper.getClubDtl(10L, 20L)).thenReturn(club);
        when(readingClubMapper.getClubMemberList(10L)).thenReturn(List.of(profile));

        // 비회원으로 공개 모임의 모임원 프로필 목록을 조회함
        ResultData result = readingClubService.getClubMemberList(20L, 10L);

        // 공개 가능한 활성 모임원 목록이 반환되는지 검증함
        assertEquals(200, result.getCode());
        assertEquals(List.of(profile), result.getData());
    }

    /**
     * 활성 신청자로 조회되지 않는 처리 대기 신청의 승인을 차단하는지 검증함
     *
     * @author Hanwon.Jang
     */
    @Test
    void rejectHiddenApplicant() {
        // 가입 신청 처리 권한을 가진 모임장 정보를 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        // 현재 사용자를 모임장으로 설정함
        club.setOwnrNumb(20L);

        // 승인 요청 상태를 구성함
        ReadingClubDto.ApplicationDecisionReqDto request = new ReadingClubDto.ApplicationDecisionReqDto();
        // 처리 상태를 승인으로 설정함
        request.setJoinStat("APPROVED");

        // 모임장 권한은 유효하지만 제한 계정의 신청은 활성 신청 조회에서 제외되도록 구성함
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        // 계정 제한으로 숨겨진 신청은 잠금 조회 결과에도 포함하지 않음
        when(readingClubMapper.getApplicationForUpdate(10L, 30L)).thenReturn(null);

        // 숨겨진 신청을 승인하려는 요청을 처리함
        ResultData result = readingClubService.uptApplication(20L, 10L, 30L, request);

        // 수정 거절 코드와 신청 상태 미변경을 검증함
        assertEquals(ResultEnum.COMMON_UPDATE_REJECTED.getCode(), result.getCode());
        verify(readingClubMapper, never()).uptJoinApplication(10L, 30L, 20L, "APPROVED");
    }

    /**
     * 가입 신청 승인과 거절 결과 알림이 가입 처리 상황 코드를 사용하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void uptAppUsesRejectedSitu() {
        // 가입 신청 처리 권한과 알림 문구에 사용할 모임 정보를 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setOwnrNumb(20L);
        club.setClubName("책벌레 모임");

        // 처리 대기 중인 활성 신청자 정보를 구성함
        ReadingClubDto.ApplicationDto application = new ReadingClubDto.ApplicationDto();
        application.setUserNumb(30L);

        // 거절 처리 요청을 구성해 좌석 변경 없이 공통 처리 결과 알림 경로를 검증함
        ReadingClubDto.ApplicationDecisionReqDto request = new ReadingClubDto.ApplicationDecisionReqDto();
        request.setJoinStat("REJECTED");

        // 신청 거절 저장과 신청자 알림 발송이 모두 성공하도록 구성함
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.getApplicationForUpdate(10L, 40L)).thenReturn(application);
        when(readingClubMapper.uptJoinApplication(10L, 40L, 20L, "REJECTED")).thenReturn(1);
        when(alimService.sendAlim(30L, Constant.ALIM_SITU_REJECTED
                , Constant.ALIM_TEMP_CODE_CLUB_JOIN_REJECTED, Constant.ALIM_TARGET_READING_CLUB, 10L, null
                , Map.of("clubName", "책벌레 모임"))).thenReturn(ResultData.success());

        // 모임장이 가입 신청을 거절함
        ResultData result = readingClubService.uptApplication(20L, 10L, 40L, request);

        // 가입 처리 상황 코드와 거절 템플릿 조합으로 알림이 발송되는지 검증함
        assertEquals(200, result.getCode());
        assertEquals("REJECTED", Constant.ALIM_SITU_REJECTED);
        verify(alimService).sendAlim(
                30L
              , Constant.ALIM_SITU_REJECTED
              , Constant.ALIM_TEMP_CODE_CLUB_JOIN_REJECTED
              , Constant.ALIM_TARGET_READING_CLUB
              , 10L
              , null
              , Map.of("clubName", "책벌레 모임")
        );
    }

    /**
     * 가입 신청 승인 알림이 모임 알림 상황과 승인 템플릿을 함께 사용하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void uptAppUsesApprovedSitu() {
        // 가입 승인 권한과 알림 문구에 사용할 모임 정보를 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        // 현재 사용자를 모임장으로 설정함
        club.setOwnrNumb(20L);
        // 승인할 좌석이 남도록 모임 정원을 설정함
        club.setMaxxMemb(10);
        // 승인 알림에 사용할 모임명을 설정함
        club.setClubName("책벌레 모임");

        // 처리 대기 중인 활성 신청자 정보를 구성함
        ReadingClubDto.ApplicationDto application = new ReadingClubDto.ApplicationDto();
        // 승인 후 멤버와 알림 수신자로 사용할 신청자 번호를 설정함
        application.setUserNumb(30L);

        // 승인 처리 요청을 구성함
        ReadingClubDto.ApplicationDecisionReqDto request = new ReadingClubDto.ApplicationDecisionReqDto();
        // 처리 상태를 승인으로 설정함
        request.setJoinStat("APPROVED");

        // 신청 승인과 신청자 알림 발송이 모두 성공하도록 구성함
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.getApplicationForUpdate(10L, 40L)).thenReturn(application);
        when(readingClubMapper.getOccupiedSeatCnt(10L)).thenReturn(1);
        when(readingClubMapper.uptJoinApplication(10L, 40L, 20L, "APPROVED")).thenReturn(1);
        when(alimService.sendAlim(30L, Constant.ALIM_SITU_FOLLOW_CLUB
                , Constant.ALIM_TEMP_CODE_CLUB_JOIN_APPROVED, Constant.ALIM_TARGET_READING_CLUB, 10L, null
                , Map.of("clubName", "책벌레 모임"))).thenReturn(ResultData.success());

        // 모임장이 가입 신청을 승인함
        ResultData result = readingClubService.uptApplication(20L, 10L, 40L, request);

        // 모임 알림 상황과 승인 템플릿 조합으로 승인과 알림이 완료되는지 검증함
        assertEquals(200, result.getCode());
        verify(readingClubMapper).setActiveMember(10L, 30L);
        verify(alimService).sendAlim(
                30L
              , Constant.ALIM_SITU_FOLLOW_CLUB
              , Constant.ALIM_TEMP_CODE_CLUB_JOIN_APPROVED
              , Constant.ALIM_TARGET_READING_CLUB
              , 10L
              , null
              , Map.of("clubName", "책벌레 모임")
        );
    }

    /**
     * 가입 신청자가 자신의 처리 대기 신청과 답변을 삭제하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void delAppCancelsPending() {
        // 활성 신청자의 처리 대기 신청 삭제가 성공하도록 구성함
        when(readingClubMapper.delOwnApplication(10L, 20L)).thenReturn(1);

        // 가입 승인 전 본인의 신청을 취소함
        ResultData result = readingClubService.delApplication(20L, 10L);

        // 신청 행 삭제 성공 응답을 검증함
        assertEquals(200, result.getCode());
        verify(readingClubMapper).delOwnApplication(10L, 20L);
    }

    /**
     * 이미 처리됐거나 본인 소유가 아닌 신청은 취소하지 않는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void delAppRejectsMissing() {
        // 처리 대기 중인 본인 신청이 조회되지 않도록 삭제 결과를 구성함
        when(readingClubMapper.delOwnApplication(10L, 20L)).thenReturn(0);

        // 존재하지 않는 처리 대기 신청의 취소를 요청함
        ResultData result = readingClubService.delApplication(20L, 10L);

        // 삭제 거절 응답을 검증함
        assertEquals(ResultEnum.COMMON_DELETE_REJECTED.getCode(), result.getCode());
    }

    /**
     * 모임장이 활성 일반 멤버를 퇴장시키고 알림을 발송하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void delMemberExitsTarget() {
        // 운영 중인 모임의 현재 모임장 정보를 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setOwnrNumb(20L);
        club.setClubStat("ACTIVE");
        club.setClubName("함께 읽는 모임");
        ReadingClubDto.MemberKickReqDto request = new ReadingClubDto.MemberKickReqDto();
        request.setKickRson("반복적인 운영 규칙 위반");

        // 권한과 활성 멤버 변경 및 알림 저장이 모두 성공하도록 구성함
        when(badWordDetectionService.findBadWord("반복적인 운영 규칙 위반")).thenReturn(Optional.empty());
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.uptMemberExit(20L, 10L, 30L)).thenReturn(1);
        when(readingClubMapper.getNextKickNumb(10L)).thenReturn(1L);
        when(readingClubMapper.setMemberKick(10L, 1L, 30L, 20L, "반복적인 운영 규칙 위반")).thenReturn(1);
        when(alimService.sendAlim(
                30L
              , Constant.ALIM_SITU_REJECTED
              , Constant.ALIM_TEMP_CODE_CLUB_MEMBER_EXITED
              , Constant.ALIM_TARGET_READING_CLUB
              , 10L
              , null
              , Map.of("clubName", "함께 읽는 모임")
        )).thenReturn(ResultData.success());

        // 모임장이 선택한 일반 멤버의 퇴장을 요청함
        ResultData result = readingClubService.delMember(20L, 10L, 30L, request);

        // 퇴장 상태 변경과 알림이 함께 처리됐는지 검증함
        assertEquals(200, result.getCode());
        verify(readingClubMapper).uptMemberExit(20L, 10L, 30L);
        verify(readingClubMapper).setMemberKick(10L, 1L, 30L, 20L, "반복적인 운영 규칙 위반");
        verify(alimService).sendAlim(
                30L
              , Constant.ALIM_SITU_REJECTED
              , Constant.ALIM_TEMP_CODE_CLUB_MEMBER_EXITED
              , Constant.ALIM_TARGET_READING_CLUB
              , 10L
              , null
              , Map.of("clubName", "함께 읽는 모임")
        );
    }

    /**
     * 모임장이 자신을 퇴장시키려는 요청을 거절하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void delMemberRejectsOwner() {
        // 운영 중인 모임의 현재 모임장 정보를 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setOwnrNumb(20L);
        club.setClubStat("ACTIVE");
        ReadingClubDto.MemberKickReqDto request = new ReadingClubDto.MemberKickReqDto();
        request.setKickRson("운영 규칙 위반");

        // 퇴장 대상이 모임장 본인이 되도록 구성함
        when(badWordDetectionService.findBadWord("운영 규칙 위반")).thenReturn(Optional.empty());
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);

        // 모임장이 자신을 퇴장시키는 요청을 처리함
        ResultData result = readingClubService.delMember(20L, 10L, 20L, request);

        // 접근 거절과 관계 상태 미변경을 검증함
        assertEquals(ResultEnum.COMMON_ACCESS_REJECTED.getCode(), result.getCode());
        verify(readingClubMapper, never()).uptMemberExit(20L, 10L, 20L);
    }

    /** 활성 모임원이 채팅을 저장하면 다른 활성 모임원에게 댓글 아이콘 알림을 보내는지 검증함. */
    @Test
    void setChatSendsReplyAlim() {
        ReadingClubDto.ClubChatReqDto request = new ReadingClubDto.ClubChatReqDto();
        request.setChatCntn(" 이번 주 책 어땠나요? ");
        request.setClntUuid("6e6e0ec8-dfd8-4ba2-8278-f26e6fd6a008");
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setClubStat("ACTIVE");
        club.setClubName("함께 읽는 모임");
        ReadingClubDto.ClubChatDto savedChat = new ReadingClubDto.ClubChatDto();
        savedChat.setChatNumb(1L);
        savedChat.setChatCntn("이번 주 책 어땠나요?");
        savedChat.setUserNick("독서가");

        when(badWordDetectionService.findBadWord("이번 주 책 어땠나요?")).thenReturn(Optional.empty());
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.getActiveMemberCnt(10L, 20L)).thenReturn(1);
        when(readingClubMapper.setClubChat(10L, 20L, "TEXT", request)).thenReturn(1);
        when(readingClubMapper.getClubChatByUuid(10L, 20L, request.getClntUuid())).thenReturn(savedChat);
        when(readingClubMapper.getClubChatAlimUserList(10L, 20L)).thenReturn(List.of(30L));
        when(alimService.sendAlim(
                30L, Constant.ALIM_SITU_REPLY, Constant.ALIM_TEMP_CODE_CLUB_CHAT_MESSAGE
              , Constant.ALIM_TARGET_READING_CLUB, 10L, 1L
              , Map.of("clubName", "함께 읽는 모임", "userName", "독서가"
                     , "chatContent", "이번 주 책 어땠나요?")
        )).thenReturn(ResultData.success());

        ResultData result = readingClubService.setClubChat(20L, 10L, request);

        assertEquals(200, result.getCode());
        assertEquals(savedChat, result.getData());
        verify(alimService).sendAlim(
                30L, Constant.ALIM_SITU_REPLY, Constant.ALIM_TEMP_CODE_CLUB_CHAT_MESSAGE
              , Constant.ALIM_TARGET_READING_CLUB, 10L, 1L
              , Map.of("clubName", "함께 읽는 모임", "userName", "독서가"
                     , "chatContent", "이번 주 책 어땠나요?")
        );
    }

    /** 열람 중인 수신자 제외 및 다른 수신자 알림 유지 검증 @author HanWon.Jang */
    @Test
    void setChatSkipsViewers() {
        // 활성 모임에서 한 회원만 현재 채팅 화면을 열람하는 상황 구성
        ReadingClubDto.ClubChatReqDto request = new ReadingClubDto.ClubChatReqDto();
        request.setChatCntn("함께 읽어요");
        request.setClntUuid("6e6e0ec8-dfd8-4ba2-8278-f26e6fd6a008");
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setClubStat("ACTIVE");
        club.setClubName("테스트 모임");
        ReadingClubDto.ClubChatDto chat = new ReadingClubDto.ClubChatDto();
        chat.setChatNumb(42L);
        chat.setChatCntn(request.getChatCntn());
        chat.setUserNick("테스트 작성자");
        when(badWordDetectionService.findBadWord(request.getChatCntn())).thenReturn(Optional.empty());
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.getActiveMemberCnt(10L, 20L)).thenReturn(1);
        when(readingClubMapper.setClubChat(10L, 20L, "TEXT", request)).thenReturn(1);
        when(readingClubMapper.getClubChatByUuid(10L, 20L, request.getClntUuid())).thenReturn(chat);
        when(readingClubMapper.getClubChatAlimUserList(10L, 20L)).thenReturn(List.of(30L, 40L));
        when(clubChatViewService.isViewing(10L, 30L)).thenReturn(true);
        when(alimService.sendAlim(org.mockito.ArgumentMatchers.eq(40L), any(), any(), any(), any(), any(), any()))
                .thenReturn(ResultData.success());

        // 열람 중인 회원에게 저장·푸시 요청 없이 다른 회원에게만 원본 번호 전달 검증
        assertEquals(200, readingClubService.setClubChat(20L, 10L, request).getCode());
        verify(alimService, never()).sendAlim(org.mockito.ArgumentMatchers.eq(30L), any(), any(), any(), any(), any(), any());
        verify(alimService).sendAlim(40L, Constant.ALIM_SITU_REPLY, Constant.ALIM_TEMP_CODE_CLUB_CHAT_MESSAGE,
                Constant.ALIM_TARGET_READING_CLUB, 10L, 42L,
                Map.of("clubName", "테스트 모임", "userName", "테스트 작성자", "chatContent", "함께 읽어요"));
    }

    /** 빈 채팅방의 열람 등록과 화면 종료 시 읽음 위치 보존 검증 @author HanWon.Jang */
    @Test
    void uptEmptyChatView() {
        // 원본 메시지 없이 열람 신호만 전달하는 활성 모임원 구성
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setClubStat("ACTIVE");
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.getActiveMemberCnt(10L, 20L)).thenReturn(1);
        when(alimService.getUnreadAlimCnt(20L)).thenReturn(ResultData.success(Map.of("unreadCnt", 2)));
        ReadingClubDto.ClubChatReadReqDto request = new ReadingClubDto.ClubChatReadReqDto();
        request.setViewId("6e6e0ec8-dfd8-4ba2-8278-f26e6fd6a008");
        request.setViewing(true);

        // 화면 등록 뒤 종료 신호는 읽음 위치 변경 없이 해당 화면만 해제
        assertEquals(200, readingClubService.uptClubChatRead(20L, 10L, request).getCode());
        request.setViewing(false);
        assertEquals(200, readingClubService.uptClubChatRead(20L, 10L, request).getCode());
        verify(clubChatViewService).uptChatView(10L, 20L, request.getViewId(), true);
        verify(clubChatViewService).uptChatView(10L, 20L, request.getViewId(), false);
        verify(readingClubMapper, never()).uptClubChatRead(any(), any(), any());
        verify(readingClubMapper, never()).uptClubChatAlimRead(any(), any());
    }

    /** 식별값 없는 열람 신호와 비어 있는 요청 거부 검증 @author HanWon.Jang */
    @Test
    void uptViewRejectsInvalid() {
        // 잘못된 요청은 잠금이나 열람 저장소에 접근하기 전에 거부
        ReadingClubDto.ClubChatReadReqDto request = new ReadingClubDto.ClubChatReadReqDto();
        assertEquals(ResultEnum.COMMON_INVALID_REQUEST.getCode(),
                readingClubService.uptClubChatRead(20L, 10L, request).getCode());
        request.setViewing(true);
        assertEquals(ResultEnum.COMMON_INVALID_REQUEST.getCode(),
                readingClubService.uptClubChatRead(20L, 10L, request).getCode());
        assertEquals(ResultEnum.COMMON_INVALID_REQUEST.getCode(),
                readingClubService.uptClubChatRead(20L, 10L, null).getCode());
        org.mockito.Mockito.verifyNoInteractions(readingClubMapper, clubChatViewService, alimService);
    }

    /** 비활성 계정 또는 비회원은 모임 채팅 목록을 볼 수 없는지 검증함. */
    @Test
    void getChatRejectsInactive() {
        when(readingClubMapper.getActiveMemberCnt(10L, 20L)).thenReturn(0);

        ResultData result = readingClubService.getClubChatList(20L, 10L, null);

        assertEquals(ResultEnum.COMMON_ACCESS_REJECTED.getCode(), result.getCode());
        verify(readingClubMapper, never()).getClubChatList(10L, 20L, null, 100);
    }

    /**
     * 활성 모임원의 마지막 읽은 채팅 번호를 갱신하는지 검증함
     *
     * @author SeungHyeon.Kang
     */
    @Test
    void uptChatReadUpdatesCursor() {
        // 실제 채팅 번호를 담은 읽음 처리 요청을 구성함
        // 채팅 저장과 같은 모임 잠금 및 활성 상태 구성
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setClubStat("ACTIVE");
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        ReadingClubDto.ClubChatReadReqDto request = new ReadingClubDto.ClubChatReadReqDto();
        // 마지막으로 화면에 표시한 채팅 번호를 설정함
        request.setChatNumb(15L);
        // 활성 계정의 활성 모임원 관계를 반환하도록 구성함
        when(readingClubMapper.getActiveMemberCnt(10L, 20L)).thenReturn(1);
        // 요청한 채팅이 같은 모임에 있어 읽음 위치가 갱신되도록 구성함
        when(readingClubMapper.uptClubChatRead(10L, 20L, 15L)).thenReturn(1);

        when(alimService.getUnreadAlimCnt(20L)).thenReturn(ResultData.success(Map.of("unreadCnt", 2)));

        // 활성 모임원의 채팅 읽음 처리를 실행함
        ResultData result = readingClubService.uptClubChatRead(20L, 10L, request);

        // 성공 응답과 마지막 읽은 채팅 번호 갱신을 검증함
        assertEquals(200, result.getCode());
        // 요청한 모임과 사용자 및 채팅 번호로 읽음 위치를 갱신했는지 검증함
        verify(readingClubMapper).uptClubChatRead(10L, 20L, 15L);
        // 읽음 위치 갱신 뒤 알림을 정리하고 최신 배지 응답 반환 검증
        InOrder order = org.mockito.Mockito.inOrder(readingClubMapper, alimService);
        order.verify(readingClubMapper).uptClubChatRead(10L, 20L, 15L);
        order.verify(readingClubMapper).uptClubChatAlimRead(10L, 20L);
        order.verify(alimService).getUnreadAlimCnt(20L);
        assertEquals(Map.of("unreadCnt", 2), result.getData());
    }

    /** 같은 모임에 없는 채팅 번호는 읽음 위치로 저장하지 않는지 검증함. */
    @Test
    void uptReadRejectsUnknownChat() {
        // 채팅 저장과 같은 모임 잠금 및 활성 상태 구성
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setClubStat("ACTIVE");
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        ReadingClubDto.ClubChatReadReqDto request = new ReadingClubDto.ClubChatReadReqDto();
        request.setChatNumb(15L);
        when(readingClubMapper.getActiveMemberCnt(10L, 20L)).thenReturn(1);
        when(readingClubMapper.uptClubChatRead(10L, 20L, 15L)).thenReturn(0);

        ResultData result = readingClubService.uptClubChatRead(20L, 10L, request);

        assertEquals(ResultEnum.COMMON_INVALID_REQUEST.getCode(), result.getCode());
        // 권한 또는 원본 검증 실패 시 알림과 열람 상태 미변경 검증
        verify(readingClubMapper, never()).uptClubChatAlimRead(10L, 20L);
        org.mockito.Mockito.verifyNoInteractions(clubChatViewService);
    }

    /**
     * 비활성 모임원은 채팅 읽음 위치를 변경할 수 없는지 검증함
     *
     * @author SeungHyeon.Kang
     */
    @Test
    void uptReadRejectsInactive() {
        // 접근이 거절될 채팅 읽음 처리 요청을 구성함
        // 채팅 저장과 같은 모임 잠금 및 활성 상태 구성
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setClubStat("ACTIVE");
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        ReadingClubDto.ClubChatReadReqDto request = new ReadingClubDto.ClubChatReadReqDto();
        // 유효한 채팅 번호를 설정해 회원 권한만 검증하도록 구성함
        request.setChatNumb(15L);
        // 활성 모임원 관계가 없는 조회 결과를 반환하도록 구성함
        when(readingClubMapper.getActiveMemberCnt(10L, 20L)).thenReturn(0);

        // 비활성 모임원의 채팅 읽음 처리를 실행함
        ResultData result = readingClubService.uptClubChatRead(20L, 10L, request);

        // 접근 거절 응답과 읽음 상태 미변경을 검증함
        assertEquals(ResultEnum.COMMON_ACCESS_REJECTED.getCode(), result.getCode());
        // 권한이 없는 회원의 읽음 위치를 갱신하지 않았는지 검증함
        verify(readingClubMapper, never()).uptClubChatRead(10L, 20L, 15L);
        // 권한 또는 원본 검증 실패 시 알림과 열람 상태 미변경 검증
        verify(readingClubMapper, never()).uptClubChatAlimRead(10L, 20L);
        org.mockito.Mockito.verifyNoInteractions(clubChatViewService);
    }

    /**
     * 활성 일반 모임원의 활동 연결을 삭제하고 자진 탈퇴 상태를 남기는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void delMembershipClearsLinks() {
        // 활성 계정의 일반 모임원 관계가 자진 탈퇴 상태로 변경되도록 구성함
        when(readingClubMembershipMapper.uptMemberLeave(10L, 20L)).thenReturn(1);

        // 활성 일반 모임원이 현재 모임의 자진 탈퇴를 요청함
        ResultData result = readingClubService.delMembership(20L, 10L);

        // 회원 상태와 모든 모임 활동 연결이 정리됐는지 검증함
        assertEquals(200, result.getCode());
        // 탈퇴 사실과 처리 일시를 남기는 회원 상태 변경을 검증함
        verify(readingClubMembershipMapper).uptMemberLeave(10L, 20L);
        // 다음 도서 투표 삭제를 검증함
        verify(readingClubMembershipMapper).delMemberBookVotes(10L, 20L);
        // 다음 도서 추천 삭제를 검증함
        verify(readingClubMembershipMapper).delMemberBookRecs(10L, 20L);
        // 모임장 투표와 유권자 자격 삭제를 검증함
        verify(readingClubMembershipMapper).delMemberElectionVotes(10L, 20L);
        // 개인 독후감이 아닌 모임 회차 연결 삭제를 검증함
        verify(readingClubMembershipMapper).delMemberRoundLinks(10L, 20L);
        // 목표 결과 확인 기록 삭제를 검증함
        verify(readingClubMembershipMapper).delMemberResultHistory(10L, 20L);
        // 가입 신청 기록 삭제를 검증함
        verify(readingClubMembershipMapper).delMemberApplications(10L, 20L);
    }

    /**
     * 모임장 또는 비활성 관계의 자진 탈퇴 요청이 활동 기록을 삭제하지 않는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void delMembershipRejectsOwner() {
        // 모임장 또는 비활성 관계가 SQL 권한 검증을 통과하지 못하도록 구성함
        when(readingClubMembershipMapper.uptMemberLeave(10L, 20L)).thenReturn(0);

        // 자진 탈퇴 권한이 없는 사용자의 요청을 처리함
        ResultData result = readingClubService.delMembership(20L, 10L);

        // 접근 거절과 활동 연결 보존을 검증함
        assertEquals(ResultEnum.COMMON_ACCESS_REJECTED.getCode(), result.getCode());
        // 권한 검증 실패 뒤 도서 투표를 삭제하지 않는지 검증함
        verify(readingClubMembershipMapper, never()).delMemberBookVotes(10L, 20L);
        // 권한 검증 실패 뒤 회차 참여 연결을 삭제하지 않는지 검증함
        verify(readingClubMembershipMapper, never()).delMemberRoundLinks(10L, 20L);
    }

    /**
     * 재가입 차단된 퇴장 멤버의 공개 모임 상세 접근도 거절하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void getClubDtlRejectsBlocked() {
        // 강제 퇴장으로 재가입이 차단된 기존 모임 관계를 구성함
        ReadingClubDto.MemberDto relation = new ReadingClubDto.MemberDto();
        relation.setMembStat("EXITED");
        relation.setBlocYsno("Y");
        when(readingClubMapper.getClubMember(10L, 30L)).thenReturn(relation);

        // 퇴장 대상이 공개 모임 상세 조회를 요청함
        ResultData result = readingClubService.getClubDtl(30L, 10L);

        // 접근 거절과 상세 SQL 미호출을 검증함
        assertEquals(ResultEnum.COMMON_ACCESS_REJECTED.getCode(), result.getCode());
        verify(readingClubMapper, never()).getClubDtl(10L, 30L);
    }

    /**
     * 모임장이 활성 회원에게 보낸 유효한 초대 목록을 조회하는지 검증함
     *
     * @author Hanwon.Jang
     */
    @Test
    void getVisibleSentInvites() {
        // 보낸 초대 조회 권한을 가진 모임장 정보를 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        // 현재 사용자를 모임장으로 설정함
        club.setOwnrNumb(20L);
        // 활성 회원에게 보낸 유효한 초대 정보를 구성함
        ReadingClubDto.SentInvitationDto invitation = new ReadingClubDto.SentInvitationDto();
        // 초대 대상 사용자 번호를 설정함
        invitation.setUserNumb(30L);

        // 모임장 권한과 계정 상태 필터가 적용된 보낸 초대 목록을 반환함
        when(readingClubMapper.getClubDtl(10L, 20L)).thenReturn(club);
        when(readingClubMapper.getSentInvitationList(10L, 20L)).thenReturn(List.of(invitation));

        // 모임장으로 보낸 초대 목록을 조회함
        ResultData result = readingClubService.getSentInvitationList(20L, 10L);

        // 성공 응답과 조회 목록을 검증함
        assertEquals(200, result.getCode());
        assertEquals(List.of(invitation), result.getData());
        verify(readingClubMapper).getSentInvitationList(10L, 20L);
    }

    /**
     * 모임장이 활성 회원에게 보낸 초대만 취소하는지 검증함
     *
     * @author Hanwon.Jang
     */
    @Test
    void cancelVisibleInvitation() {
        // 초대 취소 권한을 가진 모임장 정보를 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        // 현재 사용자를 모임장으로 설정함
        club.setOwnrNumb(20L);

        // 모임장 권한과 활성 대상 초대 삭제 결과를 반환함
        when(readingClubMapper.getClubForUpdate(10L)).thenReturn(club);
        when(readingClubMapper.delOwnerInvitation(10L, 30L, 20L)).thenReturn(1);

        // 활성 회원에게 보낸 초대를 취소함
        ResultData result = readingClubService.delOwnerInvitation(20L, 10L, 30L);

        // 성공 응답과 모임장 전용 삭제 호출을 검증함
        assertEquals(200, result.getCode());
        verify(readingClubMapper).delOwnerInvitation(10L, 30L, 20L);
        verify(readingClubMapper, never()).delInvitation(10L, 30L);
    }

    /**
     * 종료된 최신 회차의 목표 결과와 공개 가능한 달성자 목록을 활성 모임원에게 반환하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void getLatestGoalResult() {
        // 조회 사용자를 활성 모임원으로 구성함
        ReadingClubDto.MemberDto requester = new ReadingClubDto.MemberDto();
        requester.setMembStat("ACTIVE");

        // 종료된 최신 회차의 집계 결과를 구성함
        ReadingClubDto.ReadingGoalResultDto readingResult = new ReadingClubDto.ReadingGoalResultDto();
        readingResult.setClubNumb(10L);
        readingResult.setRondNumb(3L);
        readingResult.setPartCnt(2);
        readingResult.setGoalAchvCnt(1);

        // 계정 정책상 공개 가능한 목표 달성자를 구성함
        ReadingClubDto.MemberProfileDto achievementMember = new ReadingClubDto.MemberProfileDto();
        achievementMember.setUserNumb(20L);
        achievementMember.setUserNick("모임원");

        when(readingClubMapper.getClubMember(10L, 20L)).thenReturn(requester);
        when(readingClubMapper.getReadingGoalResult(10L, 20L, null)).thenReturn(readingResult);
        when(readingClubMapper.getReadingGoalAchievementMemberList(10L, 3L))
                .thenReturn(List.of(achievementMember));

        // 종료된 독서 목표 결과를 조회함
        ResultData result = readingClubService.getReadingGoalResult(20L, 10L);

        // 집계 결과와 달성자 목록을 함께 반환하는지 검증함
        assertEquals(200, result.getCode());
        assertEquals(readingResult, result.getData());
        assertEquals(List.of(achievementMember), readingResult.getAchievementMemberList());
    }

    /**
     * 활성 모임원이 선택한 완료 회차의 목표 결과를 조회하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void getSelectedGoalResult() {
        // 조회 사용자를 활성 모임원으로 구성함
        ReadingClubDto.MemberDto requester = new ReadingClubDto.MemberDto();
        requester.setMembStat("ACTIVE");
        ReadingClubDto.ReadingGoalResultDto readingResult = new ReadingClubDto.ReadingGoalResultDto();
        readingResult.setClubNumb(10L);
        readingResult.setRondNumb(1L);

        // 가입 이전에 종료된 첫 번째 회차의 결과를 반환하도록 설정함
        when(readingClubMapper.getClubMember(10L, 20L)).thenReturn(requester);
        when(readingClubMapper.getReadingGoalResult(10L, 20L, 1L)).thenReturn(readingResult);
        when(readingClubMapper.getReadingGoalAchievementMemberList(10L, 1L)).thenReturn(List.of());

        // 이전 독서 기록에서 선택한 회차의 결과를 조회함
        ResultData result = readingClubService.getReadingGoalResult(20L, 10L, 1L);

        // 선택 회차 번호가 유지된 목표 결과를 반환하는지 검증함
        assertEquals(200, result.getCode());
        assertEquals(readingResult, result.getData());
        verify(readingClubMapper).getReadingGoalResult(10L, 20L, 1L);
    }

    /**
     * 활성 모임원이 아닌 사용자의 종료 독서 목표 결과 조회를 거절하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void rejectGoalResultNonMember() {
        // 조회 사용자가 모임원이 아니도록 구성함
        when(readingClubMapper.getClubMember(10L, 20L)).thenReturn(null);

        // 모임 외부 사용자로 종료 결과를 조회함
        ResultData result = readingClubService.getReadingGoalResult(20L, 10L);

        // 접근 거절과 결과 집계 SQL 미호출을 검증함
        assertEquals(ResultEnum.COMMON_ACCESS_REJECTED.getCode(), result.getCode());
        verify(readingClubMapper, never()).getReadingGoalResult(10L, 20L, null);
        verify(readingClubMapper, never()).getReadingGoalAchievementMemberList(any(), any());
    }

    /**
     * 현재 활성 모임원에게 가입 시점 조건 없이 이전 독서 기록을 반환하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void getCompletedRoundHistory() {
        // 가입 시점과 관계없이 반환할 종료 회차 기록을 구성함
        ReadingClubDto.ReadingHistoryDto history = new ReadingClubDto.ReadingHistoryDto();
        history.setClubNumb(10L);
        history.setRondNumb(1L);
        history.setPartCnt(8);
        history.setGoalAchvCnt(5);

        // 현재 활성 모임원 권한과 첫 페이지 조회 결과를 설정함
        when(readingClubMapper.getActiveMemberAccessCnt(10L, 20L)).thenReturn(1);
        when(readingClubMapper.getReadingHistoryList(10L, 0, 13)).thenReturn(List.of(history));

        // 현재 활성 모임원으로 이전 독서 기록을 조회함
        ResultData result = readingClubService.getReadingHistoryList(20L, 10L, 1);

        // 가입일 검증 없이 조회한 종료 회차 페이지를 반환하는지 검증함
        assertEquals(200, result.getCode());
        @SuppressWarnings("unchecked")
        PageDto<ReadingClubDto.ReadingHistoryDto> historyPage =
                (PageDto<ReadingClubDto.ReadingHistoryDto>) result.getData();
        assertEquals(List.of(history), historyPage.list());
        assertEquals(true, historyPage.list().get(0).getResultAccessible());
        assertEquals(1, historyPage.page());
        assertEquals(false, historyPage.hasNext());
    }

    /**
     * 비활성 계정 또는 비활성 모임원은 이전 독서 기록에 접근할 수 없는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void rejectHistoryInactive() {
        // 계정과 모임 관계를 모두 충족하는 접근 행이 없도록 설정함
        when(readingClubMapper.getActiveMemberAccessCnt(10L, 20L)).thenReturn(0);

        // 비활성 접근 관계로 이전 독서 기록을 조회함
        ResultData result = readingClubService.getReadingHistoryList(20L, 10L, 1);

        // 접근 거절과 회차 목록 조회 미실행을 검증함
        assertEquals(ResultEnum.COMMON_ACCESS_REJECTED.getCode(), result.getCode());
        verify(readingClubMapper, never()).getReadingHistoryList(any(), anyInt(), anyInt());
    }

    /**
     * 공개 중인 활성 모임의 비회원에게 결과 상세 권한 없이 이전 독서 기록을 제공하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void getPublicClubHistory() {
        // 비회원에게 공개할 활성 모임과 종료 회차 기록을 구성함
        ReadingClubDto.ClubViewDto club = new ReadingClubDto.ClubViewDto();
        club.setClubVisb("PUBLIC");
        club.setClubStat("ACTIVE");
        ReadingClubDto.ReadingHistoryDto history = new ReadingClubDto.ReadingHistoryDto();
        history.setRondNumb(1L);
        // 활성 모임원 관계가 없는 비회원의 공개 모임과 종료 회차 조회 결과를 설정함
        when(readingClubMapper.getActiveMemberAccessCnt(10L, 20L)).thenReturn(0);
        when(readingClubMapper.getClubDtl(10L, 20L)).thenReturn(club);
        when(readingClubMapper.getReadingHistoryList(10L, 0, 13)).thenReturn(List.of(history));

        // 비회원으로 공개 모임의 이전 독서 기록을 조회함
        ResultData result = readingClubService.getReadingHistoryList(20L, 10L, 1);

        // 목록은 제공하되 회차 목표 결과 상세 접근은 허용하지 않는지 검증함
        assertEquals(200, result.getCode());
        @SuppressWarnings("unchecked")
        PageDto<ReadingClubDto.ReadingHistoryDto> historyPage =
                (PageDto<ReadingClubDto.ReadingHistoryDto>) result.getData();
        assertEquals(false, historyPage.list().get(0).getResultAccessible());
    }

    /**
     * 활성 모임원이 진행 또는 완료 회차를 조회하면 DONE 독후감 페이지를 반환하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void getCurrentOrDoneReports() {
        // 진행 또는 완료 회차의 도서 요약과 공개 여부가 다른 DONE 독후감을 구성함
        ReadingClubDto.ReadingRoundReportPageDto summary =
                new ReadingClubDto.ReadingRoundReportPageDto();
        summary.setClubNumb(10L);
        summary.setRondNumb(1L);
        ReportDto privateReport = new ReportDto();
        privateReport.setReptNumb(101L);
        privateReport.setReptStat(Constant.REPORT_STAT_DONE);
        privateReport.setPubcYsno(Constant.COMM_NO);

        // 조회자 접근과 대상 회차 및 독후감 조회 결과가 모두 유효하도록 설정함
        when(readingClubMapper.getActiveMemberAccessCnt(10L, 20L)).thenReturn(1);
        when(readingClubMapper.getReadingRoundReportSummary(10L, 1L)).thenReturn(summary);
        when(readingClubMapper.getReadingRoundReportList(
                20L, 10L, 1L, Constant.SORT_LATEST_DESC, 0, 13))
                .thenReturn(List.of(privateReport));

        // 활성 모임원으로 진행 또는 완료 회차 독후감 목록을 조회함
        ResultData result = readingClubService.getReadingRoundReportList(
                20L, 10L, 1L, Constant.SORT_LATEST_DESC, 1);

        // 비공개 DONE 독후감도 회차 페이지에 포함되는지 검증함
        assertEquals(200, result.getCode());
        assertEquals(summary, result.getData());
        PageDto<ReportDto> reportPage = summary.getReportPage();
        assertEquals(List.of(privateReport), reportPage.list());
        assertEquals(1, reportPage.page());
        assertEquals(false, reportPage.hasNext());
    }

    /**
     * 비활성 계정 또는 비활성 모임원은 비공개 글이 포함된 회차 독후감 목록에 접근할 수 없는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void rejectRoundReportInactive() {
        // 계정과 모임원 관계를 모두 충족한 접근 행이 없도록 설정함
        when(readingClubMapper.getActiveMemberAccessCnt(10L, 20L)).thenReturn(0);

        // 비활성 접근 관계로 완료 회차 독후감 목록을 조회함
        ResultData result = readingClubService.getReadingRoundReportList(
                20L, 10L, 1L, Constant.SORT_LATEST_DESC, 1);

        // 접근 거절과 회차 및 독후감 조회 미실행을 검증함
        assertEquals(ResultEnum.COMMON_ACCESS_REJECTED.getCode(), result.getCode());
        verify(readingClubMapper, never()).getReadingRoundReportSummary(any(), any());
    }

    /**
     * 종료 스케줄러 처리가 참여자 결과를 먼저 확정한 뒤 회차를 종료하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void completeExpiredRoundOrder() {
        // 종료된 회차 확정 작업을 실행함
        readingClubService.completeExpiredRound();

        // 참여자 목표 결과 확정 후 회차 종료 순서가 유지되는지 검증함
        InOrder completionOrder = org.mockito.Mockito.inOrder(readingClubMapper);
        completionOrder.verify(readingClubMapper).uptExpiredReadingParticipantGoal();
        completionOrder.verify(readingClubMapper).setExpiredResultTarget();
        completionOrder.verify(readingClubMapper).uptExpiredReadingRound();
    }
}
