package org.our.sadari.report.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.our.sadari.global.common.constant.Constant;
import org.our.sadari.global.common.result.ResultData;
import org.our.sadari.report.dto.ReportDto;
import org.our.sadari.report.dto.ReportTranslationDto;
import org.our.sadari.report.mapper.ReportMapper;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

/**
 * fileName       : ReportTranslationServiceTest
 * author         : HanWon.Jang
 * date           : 2026-09-07
 * description    : 독후감 번역 캐시 재사용과 월간 버튼 노출 제한을 검증함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-09-07        HanWon.Jang        최초 생성
 */
@ExtendWith(MockitoExtension.class)
class ReportTranslationServiceTest {

    // 독후감 원문과 번역 캐시 데이터 접근 객체
    @Mock
    private ReportMapper reportMapper;
    // 월간 번역 사용량 저장 객체
    @Mock
    private StringRedisTemplate redisTemplate;
    // Redis 문자열 값 연산 객체
    @Mock
    private ValueOperations<String, String> valueOperations;
    // Google 번역 API 호출 객체
    @Mock
    private RestTemplate restTemplate;

    // 독후감 번역 단위 테스트 대상
    private ReportTranslationService translationService;

    /**
     * 각 테스트에서 영어 표시 언어와 동일한 번역 설정을 구성함
     *
     * @author HanWon.Jang
     */
    @BeforeEach
    void setUp() {
        // 번역 서비스에 독립된 Mock 의존성을 주입함
        translationService = new ReportTranslationService(reportMapper, redisTemplate, restTemplate);
        // 단위 테스트에서 외부 호출 여부만 판정할 수 있도록 API 설정을 주입함
        ReflectionTestUtils.setField(translationService, "translationUrl", "https://example.invalid/translate");
        ReflectionTestUtils.setField(translationService, "translationApiKey", "test-key");
        ReflectionTestUtils.setField(translationService, "monthlyCharacterLimit", 500000L);
        ReflectionTestUtils.setField(translationService, "quotaZone", "America/Los_Angeles");
        // 한국어 원문을 영어로 표시하는 요청 환경을 구성함
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    /**
     * 테스트별 요청 언어가 다른 테스트로 전파되지 않도록 초기화함
     *
     * @author HanWon.Jang
     */
    @AfterEach
    void tearDown() {
        // 스레드에 저장된 테스트 언어 환경을 제거함
        LocaleContextHolder.resetLocaleContext();
    }

    /**
     * 유효한 번역 캐시는 Redis 예약과 Google 호출 없이 반환하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void reuseTranslationCache() {
        // 공개 범위 검증을 통과할 한국어 독후감 원문을 구성함
        ReportTranslationDto source = new ReportTranslationDto();
        source.setReptNumb(17L);
        source.setSourceLangCode("ko");
        source.setSourceContent("좋은 책입니다");
        when(reportMapper.getReportTrnsSource(any(ReportTranslationDto.class))).thenReturn(source);
        // 서비스가 계산한 원문 해시와 같은 유효 캐시를 동적으로 반환함
        when(reportMapper.getReportTrnsDtl(any(ReportTranslationDto.class))).thenAnswer(invocation -> {
            ReportTranslationDto request = invocation.getArgument(0);
            ReportTranslationDto cached = new ReportTranslationDto();
            cached.setReptNumb(request.getReptNumb());
            cached.setLangCode(request.getLangCode());
            cached.setOrigHash(request.getOrigHash());
            cached.setTrnsCntn("This is a good book.");
            return cached;
        });

        // 같은 원문과 대상 언어의 번역을 요청함
        ResultData result = translationService.setReportTranslation(31L, 17L);
        // 유효 캐시가 성공 응답으로 반환됐는지 확인함
        assertEquals(200, result.getCode());
        ReportTranslationDto translation = (ReportTranslationDto) result.getData();
        assertTrue(translation.isCached());
        assertEquals("This is a good book.", translation.getTrnsCntn());
        // 캐시 적중 요청은 월간 사용량과 Google 호출을 사용하지 않는지 확인함
        verifyNoInteractions(redisTemplate, restTemplate);
    }

    /**
     * 월간 잔여량이 없는 새 번역 카드의 버튼을 숨기는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void hideAtMonthlyLimit() {
        // 월간 500,000자를 이미 모두 사용한 Redis 상태를 구성함
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(any(String.class))).thenReturn("500000");
        // 캐시가 없는 한국어 공개 독후감 카드를 구성함
        ReportDto report = new ReportDto();
        report.setLangCode("ko");
        report.setReptCntn("번역할 내용");
        report.setTrnsCacheYsno(Constant.COMM_NO);

        // 영어 표시 요청에 월간 사용량 정책을 적용함
        translationService.applyReportAvailability(List.of(report));
        // 새 번역으로 한도를 넘기는 카드의 번역 버튼이 숨겨지는지 확인함
        assertEquals(Constant.COMM_NO, report.getTrnsAvaiYsno());
    }

    /**
     * API 키가 없어도 유효한 번역 캐시는 계속 볼 수 있는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void showCacheWithoutApiKey() {
        // 신규 Google 호출을 할 수 없는 API 키 누락 상태를 구성함
        ReflectionTestUtils.setField(translationService, "translationApiKey", "");
        // 현재 원문과 대상 언어에 해당하는 유효 캐시가 있는 카드를 구성함
        ReportDto report = new ReportDto();
        report.setLangCode("ko");
        report.setReptCntn("이미 번역된 내용");
        report.setTrnsCacheYsno(Constant.COMM_YES);

        // 영어 표시 요청에 캐시 우선 노출 정책을 적용함
        translationService.applyReportAvailability(List.of(report));
        // API 키와 월간 잔여량에 관계없이 캐시 보기 버튼이 유지되는지 확인함
        assertEquals(Constant.COMM_YES, report.getTrnsAvaiYsno());
        verifyNoInteractions(redisTemplate, restTemplate);
    }
}
