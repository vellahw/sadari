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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.our.sadari.feed.dto.FeedDto;
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
 * 2026-09-08        HanWon.Jang        한영 본문 번역 방향 검증
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
    @ParameterizedTest
    @CsvSource({"en,ko,좋은 책입니다,This is a good book.", "ko,ko,This is a good book.,좋은 책입니다"})
    void reuseTranslationCache(String targetLanguage, String storedLanguage, String content
                              , String translatedContent) {
        // 작성 당시 앱 언어와 본문이 다른 경우에도 현재 표시 언어로 번역 요청
        LocaleContextHolder.setLocale(Locale.forLanguageTag(targetLanguage));
        // 공개 범위 검증을 통과할 한국어 독후감 원문을 구성함
        ReportTranslationDto source = new ReportTranslationDto();
        source.setReptNumb(17L);
        source.setSourceLangCode(storedLanguage);
        source.setSourceContent(content);
        when(reportMapper.getReportTrnsSource(any(ReportTranslationDto.class))).thenReturn(source);
        // 서비스가 계산한 원문 해시와 같은 유효 캐시를 동적으로 반환함
        when(reportMapper.getReportTrnsDtl(any(ReportTranslationDto.class))).thenAnswer(invocation -> {
            ReportTranslationDto request = invocation.getArgument(0);
            ReportTranslationDto cached = new ReportTranslationDto();
            cached.setReptNumb(request.getReptNumb());
            cached.setLangCode(request.getLangCode());
            cached.setOrigHash(request.getOrigHash());
            cached.setTrnsCntn(translatedContent);
            return cached;
        });

        // 같은 원문과 대상 언어의 번역을 요청함
        ResultData result = translationService.setReportTranslation(31L, 17L);
        // 유효 캐시가 성공 응답으로 반환됐는지 확인함
        assertEquals(200, result.getCode());
        ReportTranslationDto translation = (ReportTranslationDto) result.getData();
        assertTrue(translation.isCached());
        assertEquals(translatedContent, translation.getTrnsCntn());
        // 번역 캐시 조회 대상이 앱의 현재 표시 언어와 일치하는지 검증
        assertEquals(targetLanguage, translation.getLangCode());
        // 캐시 적중 요청은 월간 사용량과 Google 호출을 사용하지 않는지 확인함
        verifyNoInteractions(redisTemplate, restTemplate);
    }

    /**
     * 작성 당시 앱 언어와 무관하게 목록과 피드의 본문 기준 번역 버튼 판정 검증
     *
     * @author HanWon.Jang
     * @param targetLanguage 현재 표시 언어
     * @param storedLanguage 작성 당시 언어
     * @param content 독후감 본문
     * @param expected 번역 버튼 표시 여부
     */
    @ParameterizedTest
    @CsvSource({
        "ko,ko,This is a good book.,Y",
        "ko,en,This is a good book.,Y",
        "en,en,정말 재미있게 읽은 책입니다,Y",
        "ko,en,정말 재미있게 읽은 책입니다,N",
        "en,ko,This is a good book.,N",
        "ko,ko,'',N",
        "ko,ko,123 😀,N"
    })
    void showByContentLanguage(String targetLanguage, String storedLanguage, String content
                               , String expected) {
        // 새 번역이 가능한 월간 잔여량과 표시 언어 구성
        LocaleContextHolder.setLocale(Locale.forLanguageTag(targetLanguage));
        // 월간 사용량 저장소 대체
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // 사용량이 없는 월의 번역 허용 상태 구성
        when(valueOperations.get(any(String.class))).thenReturn("0");
        // 공개 목록의 원문과 저장된 언어 구성
        ReportDto report = new ReportDto();
        // 작성 당시 앱 언어 설정
        report.setLangCode(storedLanguage);
        // 실제 작성한 본문 설정
        report.setReptCntn(content);
        // 동일한 독후감의 피드 항목 구성
        FeedDto feed = new FeedDto();
        // 번역 대상 독후감 피드 유형 설정
        feed.setTagtType(Constant.LIKE_TARGET_REPORT);
        // 작성 당시 앱 언어 설정
        feed.setLangCode(storedLanguage);
        // 실제 작성한 본문 설정
        feed.setReptCntn(content);
        // 공개 목록의 번역 버튼 판정 실행
        translationService.applyReportAvailability(List.of(report));
        // 피드의 번역 버튼 판정 실행
        translationService.applyFeedAvailability(List.of(feed));
        // 두 화면에 같은 본문 언어 판정 적용 여부 확인
        assertEquals(expected, report.getTrnsAvaiYsno());
        // 피드의 번역 버튼 표시 여부 확인
        assertEquals(expected, feed.getTrnsAvaiYsno());
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
