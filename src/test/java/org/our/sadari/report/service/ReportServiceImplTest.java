package org.our.sadari.report.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.our.sadari.book.mapper.BookMapper;
import org.our.sadari.global.common.code.util.CodeUtil;
import org.our.sadari.global.common.constant.Constant;
import org.our.sadari.global.common.result.ResultData;
import org.our.sadari.global.common.result.ResultEnum;
import org.our.sadari.global.common.service.BadWordDetectionService;
import org.our.sadari.myPage.dto.ReadingSummaryQueryDto;
import org.our.sadari.report.dto.ReportAlimDto;
import org.our.sadari.report.dto.ReportDto;
import org.our.sadari.report.mapper.ReportMapper;
import org.our.sadari.social.mapper.SocialMapper;
import org.our.sadari.user.mapper.UserMapper;

/**
 * fileName       : ReportServiceImplTest
 * author         : SeungHyeon.Kang
 * date           : 2026-08-04
 * description    : 독후감 서비스의 조회와 등록 및 삭제 정책을 검증함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-08-04        SeungHyeon.Kang       최초 생성
 * 2026-08-14        SeungHyeon.Kang    독후감 참조 데이터 삭제 순서 검증 추가
 * 2026-08-15        SeungHyeon.Kang    공개 독후감 정렬 코드 검증 추가
 * 2026-08-20        SeungHyeon.Kang    책장 색상 기본값 검증 추가
 * 2026-08-21        SeungHyeon.Kang    독후감별 알림 설정 변경 검증 추가
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    // Report 데이터 접근 객체
    @Mock
    private ReportMapper reportMapper;

    // Social 데이터 접근 객체
    @Mock
    private SocialMapper socialMapper;

    // Book 데이터 접근 객체
    @Mock
    private BookMapper bookMapper;

    // User 설정 데이터 접근 객체
    @Mock
    private UserMapper userMapper;

    // 공통코드 캐시 조회 객체
    @Mock
    private CodeUtil codeUtil;

    // 비속어 검사 서비스
    @Mock
    private BadWordDetectionService badWordDetectionService;

    // 공개 독후감 번역 가능 여부 처리 서비스
    @Mock
    private ReportTranslationService reportTranslationService;

    // 독서 요약 서비스 단위 테스트 대상
    private ReportServiceImpl reportService;

    /**
     * 각 테스트가 독립된 Mock 의존성을 사용하는 독후감 서비스 구현체를 구성함
     *
     * @author SeungHyeon.Kang
     */
    @BeforeEach
    void setUp() {
        // 독서 요약 서비스 단위 테스트 대상을 생성함
        reportService = new ReportServiceImpl(
                reportMapper, socialMapper, bookMapper, userMapper, codeUtil, badWordDetectionService,
                reportTranslationService);
        // 독서 요약 집계 SQL이 빈 기본 집계 결과를 반환하도록 설정함
        lenient().when(reportMapper.getReadingSummary(any(ReadingSummaryQueryDto.class)))
                .thenReturn(new ReadingSummaryQueryDto());
        // 독서 요약 목록 SQL이 빈 목록을 반환하도록 설정함
        lenient().when(reportMapper.getReadingSummaryList(any(ReadingSummaryQueryDto.class)))
                .thenReturn(List.of());
    }

    /**
     * 비공개 여부와 무관한 좋아요 알림 설정 요청이 소유자 조건으로 변경되는지 검증함
     *
     * @author SeungHyeon.Kang
     */
    @Test
    void uptReportAlimLike() {
        // 좋아요 알림을 끄는 요청을 생성함
        ReportAlimDto request = new ReportAlimDto();
        // 변경할 알림 사용 여부를 끔으로 설정함
        request.setUseYsno(Constant.COMM_NO);
        // 소유자 독후감의 좋아요 알림 한 건이 변경되는 조건을 구성함
        when(reportMapper.uptLikeAlim(request)).thenReturn(1);

        // 로그인 사용자의 독후감 좋아요 알림 끄기를 요청함
        ResultData result = reportService.uptReportAlim(31L, 157L, "like", request);

        // 설정 변경 성공 응답을 확인함
        assertEquals(200, result.getCode());
        // 인증 사용자 번호가 소유자 조건으로 설정되는지 확인함
        assertEquals(31L, request.getUserNumb());
        // URL 독후감 번호가 변경 조건으로 설정되는지 확인함
        assertEquals(157L, request.getReptNumb());
        // 좋아요 알림 전용 Mapper만 호출되는지 확인함
        verify(reportMapper).uptLikeAlim(request);
    }

    /**
     * 다른 사용자의 독후감 알림 설정 변경 요청을 접근 거부하는지 검증함
     *
     * @author SeungHyeon.Kang
     */
    @Test
    void uptReportAlimRejectOwner() {
        // 댓글 알림을 켜는 요청을 생성함
        ReportAlimDto request = new ReportAlimDto();
        // 변경할 알림 사용 여부를 켬으로 설정함
        request.setUseYsno(Constant.COMM_YES);
        // 소유자 조건에 맞는 독후감이 없어 변경되지 않는 조건을 구성함
        when(reportMapper.uptReplyAlim(request)).thenReturn(0);

        // 로그인 사용자의 소유가 아닌 독후감 댓글 알림 변경을 요청함
        ResultData result = reportService.uptReportAlim(31L, 157L, "reply", request);

        // 접근 거부 결과 코드를 확인함
        assertEquals(ResultEnum.COMMON_ACCESS_REJECTED.getCode(), result.getCode());
    }

    /**
     * 공개 독후감의 추천순 요청을 Mapper 정렬 조건으로 전달하는지 검증함
     *
     * @author SeungHyeon.Kang
     */
    @Test
    void getPublicUsesLikeSort() {
        // 공개 독후감 SQL이 빈 목록을 반환하도록 설정함
        when(reportMapper.getPublicReportList(any(ReportDto.class))).thenReturn(List.of());

        // 좋아요가 많은 순으로 공개 독후감을 조회함
        reportService.getPublicReportsByIsbn(31L, "9788972756194", Constant.SORT_LIKE_DESC, "ALL", 1);

        // Mapper에 전달된 정렬 코드를 확인할 인자 Capture를 생성함
        ArgumentCaptor<ReportDto> reportCaptor = ArgumentCaptor.forClass(ReportDto.class);
        // 공개 독후감 SQL 조회 인자를 Capture함
        verify(reportMapper).getPublicReportList(reportCaptor.capture());
        // 요청한 추천순 코드가 변경 없이 전달됐는지 검증함
        assertEquals(Constant.SORT_LIKE_DESC, reportCaptor.getValue().getSortType());
    }

    /**
     * 허용되지 않은 공개 독후감 정렬 코드를 관계 우선 기본순으로 보정하는지 검증함
     *
     * @author SeungHyeon.Kang
     */
    @Test
    void getPublicDefaultsSort() {
        // 공개 독후감 SQL이 빈 목록을 반환하도록 설정함
        when(reportMapper.getPublicReportList(any(ReportDto.class))).thenReturn(List.of());

        // 허용 목록에 없는 정렬 코드로 공개 독후감을 조회함
        reportService.getPublicReportsByIsbn(31L, "9788972756194", "UNKNOWN_DESC", "ALL", 1);

        // Mapper에 전달된 정렬 코드를 확인할 인자 Capture를 생성함
        ArgumentCaptor<ReportDto> reportCaptor = ArgumentCaptor.forClass(ReportDto.class);
        // 공개 독후감 SQL 조회 인자를 Capture함
        verify(reportMapper).getPublicReportList(reportCaptor.capture());
        // 허용되지 않은 정렬이 관계 우선 기본순으로 보정됐는지 검증함
        assertEquals(Constant.SORT_RELATION_DESC, reportCaptor.getValue().getSortType());
    }

    /**
     * 다른 사용자 소셜 요약이 집계와 목록 SQL 모두에 공개 독후감 조건을 전달하는지 검증함
     *
     * @author SeungHyeon.Kang
     */
    @Test
    void getSocialSummaryPublic() {
        // 소셜 프로필과 같은 공개 범위로 독서 요약을 조회함
        reportService.getMonthlyReadingSummary(31L, Constant.COMM_YES);

        // 집계 SQL에 전달된 공개 범위 조건을 확인할 인자 Capture를 생성함
        ArgumentCaptor<ReadingSummaryQueryDto> summaryCaptor = ArgumentCaptor.forClass(ReadingSummaryQueryDto.class);
        // 목록 SQL에 전달된 공개 범위 조건을 확인할 인자 Capture를 생성함
        ArgumentCaptor<ReadingSummaryQueryDto> reportListCaptor = ArgumentCaptor.forClass(ReadingSummaryQueryDto.class);
        // 독서량과 목표 달성 집계 SQL의 조회 조건을 Capture함
        verify(reportMapper).getReadingSummary(summaryCaptor.capture());
        // 현재 읽는 책과 완료 독후감 목록 SQL의 조회 조건을 Capture함
        verify(reportMapper).getReadingSummaryList(reportListCaptor.capture());

        // 집계 SQL이 공개 독후감만 계산하도록 공개 여부가 전달되었는지 확인함
        assertEquals(Constant.COMM_YES, summaryCaptor.getValue().getPubcYsno());
        // 목록 SQL이 공개 독후감만 반환하도록 공개 여부가 전달되었는지 확인함
        assertEquals(Constant.COMM_YES, reportListCaptor.getValue().getPubcYsno());
    }

    /**
     * 본인 마이페이지 요약은 공개 여부와 관계없이 기존 전체 독후감 범위를 유지하는지 검증함
     *
     * @author SeungHyeon.Kang
     */
    @Test
    void getMySummaryKeepsAll() {
        // 마이페이지와 같은 전체 범위로 독서 요약을 조회함
        reportService.getMonthlyReadingSummary(31L, null);

        // 집계 SQL에 전달된 전체 범위 조건을 확인할 인자 Capture를 생성함
        ArgumentCaptor<ReadingSummaryQueryDto> summaryCaptor = ArgumentCaptor.forClass(ReadingSummaryQueryDto.class);
        // 목록 SQL에 전달된 전체 범위 조건을 확인할 인자 Capture를 생성함
        ArgumentCaptor<ReadingSummaryQueryDto> reportListCaptor = ArgumentCaptor.forClass(ReadingSummaryQueryDto.class);
        // 독서량과 목표 달성 집계 SQL의 조회 조건을 Capture함
        verify(reportMapper).getReadingSummary(summaryCaptor.capture());
        // 현재 읽는 책과 완료 독후감 목록 SQL의 조회 조건을 Capture함
        verify(reportMapper).getReadingSummaryList(reportListCaptor.capture());

        // 집계 SQL의 공개 여부 조건이 비어 있어 본인 전체 독후감을 유지하는지 확인함
        assertNull(summaryCaptor.getValue().getPubcYsno());
        // 목록 SQL의 공개 여부 조건이 비어 있어 본인 전체 독후감을 유지하는지 확인함
        assertNull(reportListCaptor.getValue().getPubcYsno());
    }

    /**
     * 빈 책장 색상이 Controller DTO 검증을 통과해 Service 기본값 보정까지 전달되는지 검증함
     *
     * @author SeungHyeon.Kang
     */
    @Test
    void blankColorUsesServiceRule() {
        // 독후감 등록의 Controller 필수 입력값을 가진 요청 DTO를 생성함
        ReportDto reportDto = new ReportDto();
        // 읽는 중 독후감 상태를 요청 DTO에 설정함
        reportDto.setReptStat(Constant.REPORT_STAT_READ);
        // 목표 독서 시작일을 요청 DTO에 설정함
        reportDto.setReptStdt("2026-08-20");
        // 목표 독서 종료일을 요청 DTO에 설정함
        reportDto.setReptEndt("2026-08-31");
        // Service 기본값 보정 대상인 빈 책장 색상을 요청 DTO에 설정함
        reportDto.setReptColr("");

        // Jakarta Bean Validation 실행 자원을 테스트 범위에서 생성하고 종료함
        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            // Controller와 같은 Bean Validation 규칙을 실행할 검증기를 조회함
            Validator validator = validatorFactory.getValidator();
            // 타이머 등록 요청과 같은 빈 색상 DTO의 제약 위반 목록을 조회함
            Set<ConstraintViolation<ReportDto>> violations = validator.validate(reportDto);
            // 책장 색상 필드에 선행 제약 위반이 남아 있는지 판정함
            boolean hasColorViolation = violations.stream()
                    .map(violation -> violation.getPropertyPath().toString())
                    .anyMatch("reptColr"::equals);

            // 빈 색상은 Service의 공통코드 기본값 보정 전에 거부되지 않아야 함
            assertFalse(hasColorViolation);
        }
    }

    /**
     * 독후감 삭제 시 외래키 참조 데이터가 부모 독후감보다 먼저 삭제되는지 검증함
     *
     * @author SeungHyeon.Kang
     */
    @Test
    void delReportRefsFirst() {
        // 부모 독후감 삭제가 성공하는 조건을 설정함
        when(reportMapper.delReport(any(ReportDto.class))).thenReturn(1);

        // 댓글이 연결된 독후감 삭제를 요청함
        ResultData result = reportService.delReport(7L, 31L);

        // 독후감 삭제 성공 응답이 반환되는지 확인함
        assertEquals(200, result.getCode());
        // 외래키와 공용 대상 데이터를 정리하는 호출 순서를 검증할 객체를 생성함
        InOrder deleteOrder = inOrder(reportMapper);
        // 댓글 대상 좋아요가 댓글보다 먼저 삭제되는지 확인함
        deleteOrder.verify(reportMapper).delReportReplyLikes(any(ReportDto.class));
        // 대댓글이 최상위 댓글보다 먼저 삭제되는지 확인함
        deleteOrder.verify(reportMapper).delReportChildReplies(any(ReportDto.class));
        // 대댓글 정리 뒤 나머지 댓글이 삭제되는지 확인함
        deleteOrder.verify(reportMapper).delReportReplies(any(ReportDto.class));
        // 독후감 대상 좋아요가 부모 독후감보다 먼저 삭제되는지 확인함
        deleteOrder.verify(reportMapper).delReportLikes(any(ReportDto.class));
        // 모든 참조 데이터가 정리된 뒤 부모 독후감이 삭제되는지 확인함
        deleteOrder.verify(reportMapper).delReport(any(ReportDto.class));
    }
}
