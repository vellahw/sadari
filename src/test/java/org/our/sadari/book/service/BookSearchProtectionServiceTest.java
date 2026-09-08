package org.our.sadari.book.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.our.sadari.book.dto.BookSearchResponseDto;
import org.our.sadari.book.dto.PopularSearchKeywordDto;
import org.our.sadari.global.common.service.BadWordDetectionService;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * fileName       : BookSearchProtectionServiceTest
 * author         : HanWon.Jang
 * date           : 2026-08-16
 * description    : Redis 도서 검색 제한과 검색어 비노출 공용 캐시를 검증함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-08-16        SeungHyeon.Kang    최초 생성 및 검색 보호 검증
 * 2026-08-28        HanWon.Jang        캐시 유형별 단기 한도 검증
 */
@ExtendWith(MockitoExtension.class)
class BookSearchProtectionServiceTest {

    // 도서 검색 제한과 캐시를 저장할 Redis 문자열 연산 대역
    @Mock
    private StringRedisTemplate redisTemplate;
    // 공용 검색 결과 Redis 값 연산 대역
    @Mock
    private ValueOperations<String, String> valueOperations;
    // 인기 검색어 점수와 회원별 중복 시각을 저장할 Redis 정렬 집합 연산 대역
    @Mock
    private ZSetOperations<String, String> zSetOperations;
    // 인기 검색어 노출 여부를 판정할 비속어 검사 대역
    @Mock
    private BadWordDetectionService badWordDetectionService;
    // 검색어 원문 비노출 캐시 키 검증 객체
    @Captor
    private ArgumentCaptor<String> cacheKeyCaptor;

    // Redis 도서 검색 보호 검증 대상 서비스
    private BookSearchProtectionService bookSearchProtectionService;

    /**
     * 각 테스트에서 도서 검색 보호 서비스와 운영 기본 제한값을 구성함
     *
     * @author SeungHyeon.Kang
     */
    @BeforeEach
    void setUp() {
        // 실제 JSON 계약으로 캐시 직렬화를 확인할 객체를 생성함
        ObjectMapper objectMapper = new ObjectMapper();
        // Redis 대역과 JSON 객체로 도서 검색 보호 검증 대상을 생성함
        bookSearchProtectionService = new BookSearchProtectionService(redisTemplate, objectMapper, badWordDetectionService);
        // 회원별 60초 캐시 적중 요청 제한 기본값을 설정함
        ReflectionTestUtils.setField(bookSearchProtectionService, "cacheHitRateLimitPerMinute", 300);
        // 회원별 60초 캐시 미적중 요청 제한 기본값을 설정함
        ReflectionTestUtils.setField(bookSearchProtectionService, "cacheMissRateLimitPerMinute", 60);
        // 회원별 24시간 요청 제한 기본값을 설정함
        ReflectionTestUtils.setField(bookSearchProtectionService, "rateLimitPerDay", 200);
        // 앱 전체 카카오 실제 호출 보호 기본값을 설정함
        ReflectionTestUtils.setField(bookSearchProtectionService, "providerCallLimitPerDay", 27000);
        // 공용 검색 결과 캐시 기본 유효시간을 설정함
        ReflectionTestUtils.setField(bookSearchProtectionService, "cacheTtlSeconds", 600);
        // 인기 검색어의 최근 집계 기간 기본값을 설정함
        ReflectionTestUtils.setField(bookSearchProtectionService, "popularKeywordWindowDays", 7);
        // 운영 기본값과 같이 회원별 같은 검색어의 반복 집계를 제한함
        ReflectionTestUtils.setField(bookSearchProtectionService, "popularKeywordDedupEnabled", true);
        // 인기 검색어 화면 노출에 필요한 최소 고유 회원 수를 설정함
        ReflectionTestUtils.setField(bookSearchProtectionService, "popularKeywordMinUserCount", 3);
        // 인기 검색어 화면 최대 노출 건수를 설정함
        ReflectionTestUtils.setField(bookSearchProtectionService, "popularKeywordMaxSize", 10);
    }

    /**
     * 캐시 적중 회원별 단기 제한을 독립된 Redis Lua 실행으로 확인하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void allowsCacheHitRequest() {
        // Redis Lua Script가 회원의 요청을 허용하도록 설정함
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any()
              , eq(List.of("book:search:rate:minute:cache-hit:7"))
              , eq("300"), eq("60")
        )).thenReturn(1L);

        // 로그인 회원의 현재 도서 검색 요청 허용 여부를 확인함
        boolean allowed = bookSearchProtectionService.isRequestAllowed(7L, true);

        // 캐시 적중 단기 제한 안의 요청이 허용되는지 확인함
        assertTrue(allowed);
    }

    /**
     * 캐시 미적중 회원별 단기 제한을 독립된 Redis Lua 실행으로 확인하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void allowsCacheMissRequest() {
        // Redis Lua Script가 회원의 캐시 미적중 요청을 허용하도록 설정함
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any()
              , eq(List.of("book:search:rate:minute:cache-miss:7"))
              , eq("60"), eq("60")
        )).thenReturn(1L);

        // 로그인 회원의 현재 캐시 미적중 도서 검색 요청 허용 여부를 확인함
        boolean allowed = bookSearchProtectionService.isRequestAllowed(7L, false);

        // 캐시 미적중 단기 제한 안의 요청이 허용되는지 확인함
        assertTrue(allowed);
    }

    /**
     * Redis 장애 시 카카오 쿼터 보호를 우선해 회원 검색을 차단하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void blocksOnRedisFailure() {
        // 회원 제한 Lua 실행 단계에서 Redis 장애가 발생하도록 설정함
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any()
              , eq(List.of("book:search:rate:minute:cache-miss:7"))
              , eq("60"), eq("60")
        )).thenThrow(new IllegalStateException("Redis unavailable"));

        // Redis 장애 중 로그인 회원의 검색 허용 여부를 확인함
        boolean allowed = bookSearchProtectionService.isRequestAllowed(7L, false);

        // 제한을 확인할 수 없는 요청이 차단되는지 확인함
        assertFalse(allowed);
    }

    /**
     * 캐시 미적중 회원과 앱 전체의 일간 카카오 호출 한도를 함께 예약하는지 검증함
     *
     * @author HanWon.Jang
     */
    @Test
    void reservesDailyProviderCall() {
        // Redis Lua Script가 회원별 및 앱 전체 실제 호출을 함께 허용하도록 설정함
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any()
              , eq(List.of("book:search:rate:day:7", "book:search:provider:day:kakao"))
              , eq("200"), eq("27000"), eq("86400")
        )).thenReturn(1L);

        // 캐시 미적중 회원의 카카오 실제 호출 한도를 예약함
        boolean allowed = bookSearchProtectionService.reserveProviderCall(7L, "kakao");

        // 회원별 및 앱 전체 일간 한도가 함께 예약되는지 확인함
        assertTrue(allowed);
    }

    /**
     * 공용 검색 결과 캐시 키가 검색어 원문을 포함하지 않고 설정 TTL로 저장되는지 검증함
     *
     * @author SeungHyeon.Kang
     */
    @Test
    void cachesWithoutPlainQuery() {
        // Redis 값 저장 연산을 공용 검색 캐시에 사용할 수 있도록 설정함
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // 마지막 페이지인 빈 화면 검색 응답을 생성함
        BookSearchResponseDto searchResult = new BookSearchResponseDto(List.of(), true, null);

        // 검색어와 첫 페이지의 카카오 응답을 공용 캐시에 저장함
        bookSearchProtectionService.setCachedSearch("google", "민감한 검색어", 1, searchResult);

        // 공용 캐시 키와 JSON 및 TTL이 Redis에 저장되었는지 확인함
        verify(valueOperations).set(cacheKeyCaptor.capture(), anyString(), eq(Duration.ofSeconds(600)));
        // Redis 키가 도서 검색 공용 캐시 영역을 사용하는지 확인함
        assertTrue(cacheKeyCaptor.getValue().startsWith("book:search:cache:"));
        // Redis 키에 사용자가 입력한 검색어 원문이 포함되지 않았는지 확인함
        assertFalse(cacheKeyCaptor.getValue().contains("민감한 검색어"));
    }

    /**
     * 물리 삭제 회원의 고정 이름 분간 및 일간 제한 키를 함께 삭제하는지 검증함
     *
     * @author SeungHyeon.Kang
     */
    @Test
    void deletesMemberRateLimits() {
        // 물리 삭제된 회원의 도서 검색 제한 데이터를 정리함
        bookSearchProtectionService.delUserLimits(7L);

        // 캐시 유형별 분간 및 일간 제한 키가 한 번의 Redis 삭제로 제거되는지 확인함
        verify(redisTemplate).delete(List.of(
                "book:search:rate:minute:cache-hit:7"
              , "book:search:rate:minute:cache-miss:7"
              , "book:search:rate:day:7"
        ));
    }

    /**
     * 공백을 삽입한 비속어 검색은 허용하되 인기 검색어 점수에는 반영하지 않는지 검증함
     *
     * @author SeungHyeon.Kang
     */
    @Test
    void skipsSpacedBadWord() {
        // 공백을 보존한 기존 비속어 검사는 검색어를 허용하도록 설정함
        when(badWordDetectionService.findBadWord("시 발")).thenReturn(Optional.empty());
        // 인기 검색어 전용 공백 제거 검사에서는 비속어를 탐지하도록 설정함
        when(badWordDetectionService.findBadWord("시발")).thenReturn(Optional.of("시발"));

        // 비속어 검색 자체를 차단하지 않고 인기 검색어 반영 여부만 확인함
        bookSearchProtectionService.setPopularKeyword(7L, "시 발");

        // 노출 부적격 검색어가 Redis 인기 점수나 회원별 중복 정보에 저장되지 않는지 확인함
        verifyNoInteractions(redisTemplate);
    }

    /**
     * 안전한 검색어를 회원별 최근 중복 방지와 함께 일별 인기 점수에 반영하는지 검증함
     *
     * @author SeungHyeon.Kang
     */
    @Test
    void ranksEligibleKeyword() {
        // 정규화된 검색어와 공백 제거본이 비속어 사전을 통과하도록 설정함
        when(badWordDetectionService.findBadWord("데미안")).thenReturn(Optional.empty());
        // Redis Lua Script가 회원별 첫 검색어 점수를 반영하도록 설정함
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any()
              , org.mockito.ArgumentMatchers.<String>anyList()
              , eq("데미안"), anyString(), anyString(), anyString(), anyString(), anyString(), eq("true")
        )).thenReturn(1L);

        // 결과가 존재하는 첫 페이지에서 호출할 인기 검색어 집계를 실행함
        bookSearchProtectionService.setPopularKeyword(7L, "  데미안  ");

        String currentDate = LocalDate.now(ZoneId.of("Asia/Seoul")).toString();
        // 정규화 검색어와 날짜별 점수 및 회원별 해시 키가 하나의 Lua 실행에 전달되는지 확인함
        verify(redisTemplate).execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any()
              , eq(List.of("book:search:popular:day:" + currentDate, "book:search:popular:user:7"))
              , eq("데미안"), anyString(), anyString(), anyString(), anyString(), anyString(), eq("true")
        );
    }

    /**
     * 로컬 중복 제한 해제 설정에서 같은 회원의 반복 검색을 매번 Redis 집계에 전달하는지 검증함
     *
     * @author SeungHyeon.Kang
     */
    @Test
    void countsRepeatedKeyword() {
        // 로컬 단독 화면 검증과 같이 회원별 같은 검색어의 중복 제한을 해제함
        ReflectionTestUtils.setField(bookSearchProtectionService, "popularKeywordDedupEnabled", false);
        // 반복 검색할 정규화 검색어가 비속어 사전을 통과하도록 설정함
        when(badWordDetectionService.findBadWord("데미안")).thenReturn(Optional.empty());

        // 같은 회원의 첫 번째 로컬 검색을 인기 점수 집계에 전달함
        bookSearchProtectionService.setPopularKeyword(7L, "데미안");
        // 같은 회원의 두 번째 로컬 검색도 중복으로 제외하지 않고 집계에 전달함
        bookSearchProtectionService.setPopularKeyword(7L, "데미안");

        // 로컬 중복 제한 해제값을 포함한 Redis 집계가 검색 횟수만큼 실행되는지 확인함
        verify(redisTemplate, times(2)).execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any()
              , org.mockito.ArgumentMatchers.<String>anyList()
              , eq("데미안"), anyString(), anyString(), anyString(), anyString(), anyString(), eq("false")
        );
    }

    /**
     * 집계 뒤 비속어 사전이 변경되어도 조회 시점에 부적격 검색어를 다시 제외하는지 검증함
     *
     * @author SeungHyeon.Kang
     */
    @Test
    void filtersKeywordOnRead() {
        // 최근 일별 검색어 점수를 합산하고 조회할 Redis 정렬 집합 연산을 설정함
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        // 정상 검색어와 공백 우회 비속어가 함께 높은 점수 후보로 반환되도록 구성함
        Set<ZSetOperations.TypedTuple<String>> candidates = new LinkedHashSet<>();
        // 최소 고유 회원 수를 통과한 정상 검색어 후보를 추가함
        candidates.add(new DefaultTypedTuple<>("데미안", 5D));
        // 조회 시점의 비속어 재검사를 확인할 공백 우회 후보를 추가함
        candidates.add(new DefaultTypedTuple<>("시 발", 4D));
        // 화면 건수보다 넓게 조회한 최근 합산 후보를 반환하도록 설정함
        when(zSetOperations.reverseRangeWithScores(anyString(), eq(0L), eq(49L))).thenReturn(candidates);
        // 정상 검색어는 기존 사전과 공백 제거 검사 모두 통과하도록 설정함
        when(badWordDetectionService.findBadWord("데미안")).thenReturn(Optional.empty());
        // 공백 우회 검색어의 기존 검사는 허용하도록 설정함
        when(badWordDetectionService.findBadWord("시 발")).thenReturn(Optional.empty());
        // 공백 제거본에서는 비속어가 탐지되도록 설정함
        when(badWordDetectionService.findBadWord("시발")).thenReturn(Optional.of("시발"));

        // 현재 노출 정책을 다시 적용한 인기 검색어 목록을 조회함
        List<PopularSearchKeywordDto> popularKeywordList = bookSearchProtectionService.getPopularKeywordList();

        // 비속어 후보를 제외한 정상 검색어 한 건만 반환되는지 확인함
        assertEquals(1, popularKeywordList.size());
        // 정상 검색어의 재계산된 첫 번째 순위를 확인함
        assertEquals(1, popularKeywordList.get(0).getRank());
        // 안전한 정규화 검색어가 화면 응답에 유지되는지 확인함
        assertEquals("데미안", popularKeywordList.get(0).getKeyword());
        // 집계 기간이 이틀 이상이면 첫 날짜와 나머지 날짜가 합산되는지 확인함
        verify(zSetOperations).unionAndStore(anyString(), org.mockito.ArgumentMatchers.<String>anyList(), anyString());
    }

    /**
     * 물리 삭제 회원의 인기 검색어 중복 방지 키만 제거하는지 검증함
     *
     * @author SeungHyeon.Kang
     */
    @Test
    void deletesKeywordDedupe() {
        // 물리 삭제된 회원과 연결된 인기 검색어 중복 방지 정보를 정리함
        bookSearchProtectionService.delUserKeywordData(7L);

        // 익명 일별 점수와 분리된 회원별 중복 방지 키가 삭제되는지 확인함
        verify(redisTemplate).delete("book:search:popular:user:7");
        // 다른 Redis 키를 함께 삭제하는 호출이 발생하지 않는지 확인함
        verify(redisTemplate, never()).delete("book:search:popular:day:7");
    }
}
