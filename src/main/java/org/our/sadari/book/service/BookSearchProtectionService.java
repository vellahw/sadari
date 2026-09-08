package org.our.sadari.book.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.our.sadari.book.dto.BookSearchResponseDto;
import org.our.sadari.book.dto.PopularSearchKeywordDto;
import org.our.sadari.global.common.service.BadWordDetectionService;
import org.our.sadari.global.common.util.StringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/**
 * fileName       : BookSearchProtectionService
 * author         : HanWon.Jang
 * date           : 2026-08-16
 * description    : Redis로 도서 검색 호출 제한과 공용 캐시 및 인기 검색어를 관리함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-08-16        SeungHyeon.Kang    최초 생성 및 검색 보호 처리
 * 2026-08-28        HanWon.Jang        캐시 유형별 단기 한도 분리
 * 2026-09-08        HanWon.Jang        검색 공급자별 캐시와 실제 호출 한도 분리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookSearchProtectionService {

    // 캐시 적중을 포함한 회원별 분간 요청 제한을 검사하고 허용된 요청만 증가시키는 Lua 스크립트
    private static final String REQUEST_LIMIT_LUA = """
            local minuteCount = tonumber(redis.call('GET', KEYS[1]) or '0')
            if minuteCount >= tonumber(ARGV[1]) then
                return 0
            end
            minuteCount = redis.call('INCR', KEYS[1])
            if minuteCount == 1 then
                redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
            end
            return 1
            """;
    // 회원별 일간 및 공급자별 앱 전체 호출 한도를 함께 검사하고 실제 호출 예약만 원자 증가시키는 Lua 스크립트
    private static final String PROVIDER_LIMIT_LUA = """
            local dailyCount = tonumber(redis.call('GET', KEYS[1]) or '0')
            local providerCount = tonumber(redis.call('GET', KEYS[2]) or '0')
            if dailyCount >= tonumber(ARGV[1]) or providerCount >= tonumber(ARGV[2]) then
                return 0
            end
            dailyCount = redis.call('INCR', KEYS[1])
            if dailyCount == 1 then
                redis.call('EXPIRE', KEYS[1], tonumber(ARGV[3]))
            end
            providerCount = redis.call('INCR', KEYS[2])
            if providerCount == 1 then
                redis.call('EXPIRE', KEYS[2], tonumber(ARGV[3]))
            end
            return 1
            """;
    // 설정에 따라 회원별 최근 집계를 제한하고 일별 인기 검색어 점수를 원자 증가시키는 Lua 스크립트
    private static final String POPULAR_KEYWORD_LUA = """
            local dedupEnabled = ARGV[7] == 'true'
            if dedupEnabled then
                local previousAt = redis.call('ZSCORE', KEYS[2], ARGV[2])
                if previousAt and tonumber(previousAt) > tonumber(ARGV[4]) then
                    return 0
                end
                redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', ARGV[4])
                redis.call('ZADD', KEYS[2], ARGV[3], ARGV[2])
                redis.call('EXPIRE', KEYS[2], tonumber(ARGV[5]))
            end
            redis.call('ZINCRBY', KEYS[1], 1, ARGV[1])
            redis.call('EXPIRE', KEYS[1], tonumber(ARGV[6]))
            return 1
            """;
    // 회원별 캐시 적중 도서 검색 요청 횟수 Redis 키 접두사
    private static final String CACHE_HIT_MINUTE_LIMIT_KEY_PREFIX = "book:search:rate:minute:cache-hit:";
    // 회원별 캐시 미적중 도서 검색 요청 횟수 Redis 키 접두사
    private static final String CACHE_MISS_MINUTE_LIMIT_KEY_PREFIX = "book:search:rate:minute:cache-miss:";
    // 회원별 일간 도서 검색 요청 횟수 Redis 키 접두사
    private static final String DAILY_LIMIT_KEY_PREFIX = "book:search:rate:day:";
    // 앱 전체 공급자별 도서 검색 실제 호출 횟수 Redis 키 접두사
    private static final String PROVIDER_LIMIT_KEY_PREFIX = "book:search:provider:day:";
    // 검색어 원문을 노출하지 않는 공용 검색 결과 Redis 키 접두사
    private static final String SEARCH_CACHE_KEY_PREFIX = "book:search:cache:";
    // 최근 일자별 인기 검색어 점수 Redis 키 접두사
    private static final String POPULAR_DAY_KEY_PREFIX = "book:search:popular:day:";
    // 회원별 인기 검색어 중복 방지 Redis 키 접두사
    private static final String USER_KEYWORD_KEY_PREFIX = "book:search:popular:user:";
    // 최근 일자별 점수를 합산한 인기 검색어 Redis 키 접두사
    private static final String POPULAR_TOTAL_KEY_PREFIX = "book:search:popular:total:";
    // 회원별 제한 Lua 실행 객체
    private static final DefaultRedisScript<Long> REQUEST_LIMIT_SCRIPT =
            new DefaultRedisScript<>(REQUEST_LIMIT_LUA, Long.class);
    // 앱 전체 외부 호출 제한 Lua 실행 객체
    private static final DefaultRedisScript<Long> PROVIDER_LIMIT_SCRIPT =
            new DefaultRedisScript<>(PROVIDER_LIMIT_LUA, Long.class);
    // 인기 검색어 중복 방지와 점수 증가 Lua 실행 객체
    private static final DefaultRedisScript<Long> POPULAR_KEYWORD_SCRIPT =
            new DefaultRedisScript<>(POPULAR_KEYWORD_LUA, Long.class);
    // Lua 스크립트의 요청 허용 결과값
    private static final long REQUEST_ALLOWED = 1L;
    // 분간 제한 Redis 키 유효시간
    private static final int MINUTE_LIMIT_TTL_SECONDS = 60;
    // 일간 제한 Redis 키 유효시간
    private static final int DAILY_LIMIT_TTL_SECONDS = 86400;
    // 화면 노출 후보를 비속어 재검사 후에도 충분히 확보할 배수
    private static final int POPULAR_CANDIDATE_MULTIPLIER = 5;
    // 인기 검색어에 허용할 최소 글자 수
    private static final int POPULAR_KEYWORD_MIN_LENGTH = 2;
    // 인기 검색어에 허용할 최대 글자 수
    private static final int POPULAR_KEYWORD_MAX_LENGTH = 40;
    // 인기 검색어 날짜 경계에 사용하는 한국 표준시
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    // 검색어 안의 연속된 일반 공백과 유니코드 구분 공백을 하나로 정리하는 패턴
    private static final Pattern KEYWORD_SPACE_PATTERN = Pattern.compile("[\\p{javaWhitespace}\\p{Z}]+");
    // 사용자 식별정보가 인기 검색어에 노출되지 않도록 이메일 형태를 판정하는 패턴
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}");
    // 사용자 식별정보가 인기 검색어에 노출되지 않도록 전화번호 형태를 판정하는 패턴
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?:\\d[ -]?){10,11}");
    // 외부 주소가 인기 검색어를 광고 수단으로 사용하지 못하도록 URL 형태를 판정하는 패턴
    private static final Pattern URL_PATTERN = Pattern.compile("(?:https?://|www\\.)\\S+");

    // 회원 한 명의 60초간 최대 캐시 적중 도서 검색 요청 수
    @Value("${book.search.cache-hit-rate-limit-per-minute:300}")
    private int cacheHitRateLimitPerMinute;
    // 회원 한 명의 60초간 최대 캐시 미적중 도서 검색 요청 수
    @Value("${book.search.cache-miss-rate-limit-per-minute:60}")
    private int cacheMissRateLimitPerMinute;
    // 회원 한 명의 24시간 최대 외부 도서 검색 실제 호출 수
    @Value("${book.search.rate-limit-per-day:200}")
    private int rateLimitPerDay;
    // 공급자별 앱 전체의 24시간 최대 외부 도서 검색 실제 호출 수
    @Value("${book.search.provider-call-limit-per-day:27000}")
    private int providerCallLimitPerDay;
    // 공용 도서 검색 결과 Redis 캐시 유효시간
    @Value("${book.search.cache-ttl-seconds:600}")
    private int cacheTtlSeconds;
    // 인기 검색어를 합산하고 같은 회원의 중복을 제한할 최근 일수
    @Value("${book.search.popular-keyword-window-days:7}")
    private int popularKeywordWindowDays;
    // 운영에서는 회원별 중복을 제한하고 로컬 단독 검증에서는 반복 검색 집계를 허용할 여부
    @Value("${book.search.popular-keyword-user-dedup-enabled:true}")
    private boolean popularKeywordDedupEnabled;
    // 인기 검색어에 노출하기 위해 필요한 최소 고유 회원 수
    @Value("${book.search.popular-keyword-min-user-count:3}")
    private int popularKeywordMinUserCount;
    // 검색 화면에 전달할 인기 검색어 최대 건수
    @Value("${book.search.popular-keyword-max-size:10}")
    private int popularKeywordMaxSize;

    // 도서 검색 제한과 캐시를 공유할 Redis 문자열 연산 객체
    private final StringRedisTemplate redisTemplate;
    // 외부 도서 검색 화면 응답 캐시 직렬화 객체
    private final ObjectMapper objectMapper;
    // 검색은 허용하면서 인기 검색어 노출만 제한할 비속어 판정 서비스
    private final BadWordDetectionService badWordDetectionService;

    /**
     * 캐시 유형별 회원 검색 제한을 검사하고 요청 횟수를 반영함
     *
     * @author HanWon.Jang
     * @param userNumb 도서 검색을 요청한 로그인 회원 번호
     * @param cacheHit 공용 검색 결과 캐시 적중 여부
     * @return 분간 제한을 통과한 요청 여부
     */
    public boolean isRequestAllowed(Long userNumb, boolean cacheHit) {
        // 인증되지 않은 요청은 외부 API 쿼터를 사용할 수 없도록 차단함
        if (StringUtil.isEmpty(userNumb)) {
            // 회원 식별값이 없는 요청을 거절함
            return false;
        }

        // Redis 장애 시 외부 공급자 쿼터가 무방비로 소모되지 않도록 검색 요청을 차단함
        try {
            // 캐시 유형에 맞는 한 회원의 분간 요청 제한값을 선택함
            int rateLimit = cacheHit ? cacheHitRateLimitPerMinute : cacheMissRateLimitPerMinute;
            // 캐시 유형별 독립 카운터로 회원의 분간 요청 제한을 검사함
            Long result = redisTemplate.execute(
                    REQUEST_LIMIT_SCRIPT
                  , List.of(getMinuteLimitKey(userNumb, cacheHit))
                  , String.valueOf(rateLimit), String.valueOf(MINUTE_LIMIT_TTL_SECONDS)
            );
            // Redis가 명시적으로 허용한 요청만 검색 진행 대상으로 반환함
            return !StringUtil.isEmpty(result) && result == REQUEST_ALLOWED;
        }

        // 검색 제한 저장소 장애는 비밀값 없이 운영 로그에 기록함
        catch (RuntimeException e) {
            // Redis 제한을 확인하지 못한 원인을 예외 정보와 함께 기록함
            log.error("도서 검색 회원별 요청 제한을 확인하지 못했습니다.", e);
            // 제한을 확인하지 못한 요청을 외부 공급자 호출 전에 차단함
            return false;
        }
    }

    /**
     * 캐시 미적중 회원과 공급자별 앱 전체 도서 검색 실제 호출 한도를 함께 예약함
     *
     * @author HanWon.Jang
     * @param userNumb 외부 도서 검색 실제 호출을 요청한 로그인 회원 번호
     * @param provider 호출할 외부 도서 검색 공급자 코드
     * @return 회원별 일간 및 앱 전체 외부 호출 한도 안에서 예약된 요청 여부
     */
    public boolean reserveProviderCall(Long userNumb, String provider) {
        // 인증되지 않았거나 공급자가 없는 요청은 회원별 일간 한도와 공급자 쿼터를 사용할 수 없도록 차단함
        if (StringUtil.hasEmpty(userNumb, provider)) {
            // 회원 식별값이 없는 외부 호출 예약을 거절함
            return false;
        }

        // Redis 장애 시 회원별 일간 및 공급자별 전체 보호 한도를 우회하지 않도록 차단함
        try {
            // 회원별 일간 및 앱 전체 실제 호출 횟수를 한 번의 Redis 명령으로 예약함
            Long result = redisTemplate.execute(
                    PROVIDER_LIMIT_SCRIPT
                  , List.of(getDailyLimitKey(userNumb), getProviderLimitKey(provider))
                  , String.valueOf(rateLimitPerDay), String.valueOf(providerCallLimitPerDay)
                  , String.valueOf(DAILY_LIMIT_TTL_SECONDS)
            );
            // 두 일간 한도 안에서 함께 예약된 외부 호출만 허용함
            return !StringUtil.isEmpty(result) && result == REQUEST_ALLOWED;
        }

        // 회원별 또는 앱 전체 보호 카운터 장애는 쿼터 소모 없이 운영 로그로 남김
        catch (RuntimeException e) {
            // Redis 일간 호출 한도를 확인하지 못한 원인을 예외 정보와 함께 기록함
            log.error("도서 검색 회원별 및 공급자별 일일 호출 한도를 확인하지 못했습니다.", e);
            // 보호 한도를 확인하지 못한 외부 공급자 호출을 차단함
            return false;
        }
    }

    /**
     * 공급자와 검색어 및 시작 위치가 같은 공용 검색 결과를 Redis에서 조회함
     *
     * @author SeungHyeon.Kang
     * @param provider 외부 도서 검색 공급자 코드
     * @param query 사용자가 입력한 도서 검색어
     * @param start 화면 검색 시작 위치
     * @return 역직렬화된 화면 검색 결과 또는 캐시 누락값
     */
    public BookSearchResponseDto getCachedSearch(String provider, String query, int start) {
        // Redis 조회나 캐시 역직렬화 오류가 원문 검색어를 노출하지 않도록 공통 실패 경로로 격리함
        try {
            // 검색어 해시와 페이지로 구성한 공용 캐시 값을 조회함
            String cachedJson = redisTemplate.opsForValue().get(getCacheKey(provider, query, start));

            // 저장된 검색 결과가 없으면 외부 공급자 API 호출이 필요함을 반환함
            if (StringUtil.isEmpty(cachedJson)) {
                // 공용 검색 캐시 누락값을 반환함
                return null;
            }

            // 캐시 JSON을 화면 도서 검색 응답 DTO로 복원함
            return objectMapper.readValue(cachedJson, BookSearchResponseDto.class);
        }

        // 손상된 캐시나 Redis 장애는 외부 호출 예약 단계가 처리하도록 캐시 누락으로 전환함
        catch (RuntimeException | JsonProcessingException e) {
            // 검색어 원문 없이 공용 캐시 조회 실패 원인을 기록함
            log.error("외부 도서 검색 공용 캐시를 조회하지 못했습니다.", e);
            // 복원할 수 없는 공용 검색 캐시를 사용하지 않음
            return null;
        }
    }

    /**
     * 외부 도서 검색 결과를 사용자와 연결하지 않은 공용 Redis 캐시에 저장함
     *
     * @author SeungHyeon.Kang
     * @param provider 외부 도서 검색 공급자 코드
     * @param query 사용자가 입력한 도서 검색어
     * @param start 화면 검색 시작 위치
     * @param searchResult 화면 계약으로 변환한 도서 검색 결과
     */
    public void setCachedSearch(String provider, String query, int start, BookSearchResponseDto searchResult) {
        // 캐시 실패가 이미 완료된 외부 검색 응답을 사용자에게 반환하지 못하게 하지 않도록 격리함
        try {
            // 화면 도서 검색 응답을 Redis 저장 문자열로 직렬화함
            String cachedJson = objectMapper.writeValueAsString(searchResult);
            // 사용자 식별값 없는 공용 검색 결과를 설정된 짧은 유효시간 동안 저장함
            redisTemplate.opsForValue().set(
                    getCacheKey(provider, query, start)
                  , cachedJson
                  , Duration.ofSeconds(cacheTtlSeconds)
            );
        }

        // 공용 캐시 저장 실패는 쿼터 보호 카운터를 되돌리지 않고 운영 로그에만 기록함
        catch (RuntimeException | JsonProcessingException e) {
            // 검색어 원문 없이 공용 캐시 저장 실패 원인을 기록함
            log.error("외부 도서 검색 공용 캐시를 저장하지 못했습니다.", e);
        }
    }

    /**
     * 검색 결과가 존재하는 첫 페이지 검색어를 최근 인기 검색어 후보에 반영함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 검색을 실행한 로그인 회원 번호
     * @param query 사용자가 입력한 도서 검색어
     */
    public void setPopularKeyword(Long userNumb, String query) {
        // 인기 검색어 부가 기능 장애가 정상 도서 검색을 중단하지 않도록 전체 집계 경로를 격리함
        try {
            // 검색어 표기 차이를 합치고 비속어 우회와 개인정보형 문자열을 노출 전에 제외함
            String normalizedKeyword = normalizeKeyword(query);

            // 인증값이 없거나 화면 노출에 부적합한 검색어는 Redis에 원문을 저장하지 않음
            if (StringUtil.isEmpty(userNumb) || !isKeywordEligible(normalizedKeyword)) {
                // 검색은 유지하면서 인기 검색어 점수 반영만 종료함
                return;
            }

            int windowDays = Math.max(popularKeywordWindowDays, 1);
            long currentEpochSecond = Instant.now().getEpochSecond();
            long windowSeconds = Duration.ofDays(windowDays).getSeconds();
            long cutoffEpochSecond = currentEpochSecond - windowSeconds;
            long retentionSeconds = Duration.ofDays(windowDays + 1L).getSeconds();
            // 회원별 중복 방지에는 검색어 원문 대신 단방향 해시를 사용함
            String keywordHash = getSha256(normalizedKeyword);
            // 프로필별 중복 정책을 적용하면서 일별 검색어 점수를 원자 증가시킴
            redisTemplate.execute(
                    POPULAR_KEYWORD_SCRIPT
                  , List.of(getPopularDayKey(LocalDate.now(SEOUL_ZONE)), getUserKeywordKey(userNumb))
                  , normalizedKeyword, keywordHash, String.valueOf(currentEpochSecond)
                  , String.valueOf(cutoffEpochSecond), String.valueOf(retentionSeconds)
                  , String.valueOf(retentionSeconds), String.valueOf(popularKeywordDedupEnabled)
            );
        }

        // 비속어 사전이나 Redis 장애는 검색어 원문 없이 기록하고 도서 검색 성공 응답에는 영향을 주지 않음
        catch (RuntimeException e) {
            // 인기 검색어 부가 집계 실패 원인을 민감한 검색어 원문 없이 기록함
            log.error("도서 인기 검색어를 집계하지 못했습니다.", e);
        }
    }

    /**
     * 최근 일자별 고유 회원 점수를 합산하고 화면 노출에 안전한 인기 검색어를 조회함
     *
     * @author SeungHyeon.Kang
     * @return 순위와 정규화된 검색어를 포함한 인기 검색어 목록
     */
    public List<PopularSearchKeywordDto> getPopularKeywordList() {
        // 인기 검색어 조회 장애가 월간 인기 도서 화면 전체를 실패시키지 않도록 빈 목록으로 격리함
        try {
            int windowDays = Math.max(popularKeywordWindowDays, 1);
            int maxSize = Math.max(popularKeywordMaxSize, 1);
            int minUserCount = Math.max(popularKeywordMinUserCount, 1);
            int candidateSize = maxSize * POPULAR_CANDIDATE_MULTIPLIER;
            LocalDate currentDate = LocalDate.now(SEOUL_ZONE);
            // 최근 집계 일수에 해당하는 날짜별 Redis 키를 순서대로 구성함
            List<String> dayKeys = new ArrayList<>();

            // 현재 날짜부터 설정된 최근 기간까지 일별 점수 키를 포함함
            for (int dayOffset = 0; dayOffset < windowDays; dayOffset++) {
                // 각 날짜의 인기 검색어 점수를 합산 대상에 추가함
                dayKeys.add(getPopularDayKey(currentDate.minusDays(dayOffset)));
            }

            String totalKey = getPopularTotalKey(currentDate);
            // Redis가 최근 일별 점수를 정확히 합산한 정렬 집합을 생성함
            ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
            String rankingKey = dayKeys.get(0);

            // 집계 기간이 이틀 이상이면 첫 날짜에 나머지 날짜 점수를 합산함
            if (dayKeys.size() > 1) {
                // 최근 전체 날짜의 검색어 점수를 공통 합산 키에 저장함
                zSetOperations.unionAndStore(dayKeys.get(0), dayKeys.subList(1, dayKeys.size()), totalKey);
                // 집계 결과 키도 일별 원본과 같은 보존 범위 안에서 자동 정리함
                redisTemplate.expire(totalKey, Duration.ofDays(windowDays + 1L));
                // 화면 순위는 최근 전체 날짜가 합산된 키에서 조회함
                rankingKey = totalKey;
            }

            // 비속어 사전 변경 뒤 제외될 후보를 고려해 화면 건수보다 넓게 조회함
            Set<ZSetOperations.TypedTuple<String>> candidates =
                    zSetOperations.reverseRangeWithScores(rankingKey, 0, candidateSize - 1L);

            // 집계 결과가 없으면 인기 검색어 영역을 표시하지 않도록 빈 목록을 반환함
            if (StringUtil.isEmpty(candidates)) {
                // 화면에 노출할 인기 검색어가 없는 결과를 반환함
                return List.of();
            }

            // 노출 시점에 비속어 사전과 최소 회원 수를 다시 적용할 목록을 생성함
            List<PopularSearchKeywordDto> popularKeywordList = new ArrayList<>();

            // 점수가 높은 후보부터 현재 노출 정책을 다시 검증함
            for (ZSetOperations.TypedTuple<String> candidate : candidates) {
                String keyword = candidate.getValue();
                Double score = candidate.getScore();

                // 최소 회원 수 미만이거나 현재 비속어 사전에 걸리는 검색어는 화면에서 제외함
                if (StringUtil.hasEmpty(keyword, score) || score < minUserCount
                        || !isKeywordEligible(keyword)) {
                    // 다음 인기 검색어 후보를 검증함
                    continue;
                }

                // 안전한 후보의 현재 화면 순위와 검색어를 목록에 추가함
                popularKeywordList.add(new PopularSearchKeywordDto(popularKeywordList.size() + 1, keyword));

                // 화면 최대 건수를 채우면 불필요한 나머지 비속어 검사를 중단함
                if (popularKeywordList.size() >= maxSize) {
                    // 설정된 인기 검색어 최대 건수에서 후보 순회를 종료함
                    break;
                }
            }

            // 외부에서 집계 목록을 변경하지 못하도록 불변 인기 검색어 목록을 반환함
            return List.copyOf(popularKeywordList);
        }

        // Redis 또는 비속어 사전 장애는 검색어 원문을 남기지 않고 빈 인기 검색어 목록으로 전환함
        catch (RuntimeException e) {
            // 인기 검색어 조회 실패 원인을 민감한 검색어 원문 없이 기록함
            log.error("도서 인기 검색어를 조회하지 못했습니다.", e);
            // 월간 인기 도서 화면을 유지할 수 있도록 빈 인기 검색어 목록을 반환함
            return List.of();
        }
    }

    /**
     * 물리 삭제 회원에게 남은 인기 검색어 중복 방지 데이터를 제거함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 물리 삭제가 완료된 회원 번호
     */
    public void delUserKeywordData(Long userNumb) {
        // 삭제할 회원 식별값이 없으면 다른 회원의 인기 검색어 데이터에 영향을 주지 않고 종료함
        if (StringUtil.isEmpty(userNumb)) {
            // 삭제 대상이 없는 인기 검색어 중복 방지 정리를 종료함
            return;
        }

        // 익명 집계 점수는 유지하고 회원 번호와 연결된 중복 방지 키만 삭제함
        redisTemplate.delete(getUserKeywordKey(userNumb));
    }

    /**
     * 물리 삭제 회원에게 남은 분간 및 일간 도서 검색 제한 데이터를 제거함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 물리 삭제가 완료된 회원 번호
     */
    public void delUserLimits(Long userNumb) {
        // 삭제할 회원 식별값이 없으면 다른 Redis 키에 영향을 주지 않고 종료함
        if (StringUtil.isEmpty(userNumb)) {
            // 삭제 대상이 없는 검색 제한 정리를 종료함
            return;
        }

        // 회원과 연결된 캐시 유형별 분간 및 일간 제한 키를 함께 삭제함
        redisTemplate.delete(List.of(
                getMinuteLimitKey(userNumb, true)
              , getMinuteLimitKey(userNumb, false)
              , getDailyLimitKey(userNumb)
        ));
    }

    /**
     * 회원별 캐시 유형에 맞는 분간 도서 검색 제한 Redis 키를 생성함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 도서 검색을 요청한 회원 번호
     * @param cacheHit 공용 검색 결과 캐시 적중 여부
     * @return 회원별 분간 제한 Redis 키
     */
    private String getMinuteLimitKey(Long userNumb, boolean cacheHit) {
        // 캐시 유형에 맞는 분간 제한 접두사를 선택함
        String keyPrefix = cacheHit
                ? CACHE_HIT_MINUTE_LIMIT_KEY_PREFIX
                : CACHE_MISS_MINUTE_LIMIT_KEY_PREFIX;
        // 회원 번호와 캐시 유형별 분간 제한 접두사를 결합한 Redis 키를 반환함
        return keyPrefix + userNumb;
    }

    /**
     * 회원별 일간 도서 검색 제한 Redis 키를 생성함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 도서 검색을 요청한 회원 번호
     * @return 회원별 일간 제한 Redis 키
     */
    private String getDailyLimitKey(Long userNumb) {
        // 회원 번호와 일간 제한 접두사를 결합한 Redis 키를 반환함
        return DAILY_LIMIT_KEY_PREFIX + userNumb;
    }

    /**
     * 공급자별 앱 전체 도서 검색 실제 호출 Redis 키를 생성함
     *
     * @author HanWon.Jang
     * @param provider 외부 도서 검색 공급자 코드
     * @return 공급자별 앱 전체 일간 실제 호출 Redis 키
     */
    private String getProviderLimitKey(String provider) {
        // 공급자 코드 표기 차이로 호출 한도가 분리되지 않도록 소문자로 정규화함
        return PROVIDER_LIMIT_KEY_PREFIX + provider.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 인기 검색어 노출과 집계에 사용할 검색어를 정규화함
     *
     * @author SeungHyeon.Kang
     * @param query 사용자가 입력한 도서 검색어
     * @return 호환 문자와 공백 및 영문 대소문자를 정리한 검색어
     */
    private String normalizeKeyword(String query) {
        // 비어 있는 검색어는 정규화 함수 호출 중 예외가 발생하지 않도록 빈 문자열로 반환함
        if (StringUtil.isEmpty(query)) {
            // 인기 검색어에 저장할 수 없는 빈 문자열을 반환함
            return "";
        }

        // 호환 문자를 표준 형태로 바꾸고 연속 공백을 하나로 축약해 같은 검색 의도를 합침
        String normalizedQuery = Normalizer.normalize(query, Normalizer.Form.NFKC);
        // 영문 대소문자와 앞뒤 및 연속 공백 차이를 제거한 화면 표시 검색어를 반환함
        return KEYWORD_SPACE_PATTERN.matcher(normalizedQuery.trim().toLowerCase(Locale.ROOT)).replaceAll(" ");
    }

    /**
     * 정규화된 검색어가 인기 검색어 화면에 노출될 수 있는지 판정함
     *
     * @author SeungHyeon.Kang
     * @param keyword 노출 적격성을 검사할 정규화된 검색어
     * @return 길이와 개인정보 및 비속어 기준을 모두 통과한 여부
     */
    private boolean isKeywordEligible(String keyword) {
        // 빈 검색어는 비속어 사전을 조회하지 않고 노출 대상에서 제외함
        if (StringUtil.isEmpty(keyword)) {
            // 인기 검색어 노출 부적격으로 판정함
            return false;
        }

        int keywordLength = keyword.codePointCount(0, keyword.length());

        // 의미가 부족하거나 화면과 저장소를 과도하게 차지하는 길이는 노출하지 않음
        if (keywordLength < POPULAR_KEYWORD_MIN_LENGTH || keywordLength > POPULAR_KEYWORD_MAX_LENGTH) {
            // 허용 길이를 벗어난 검색어를 인기 검색어에서 제외함
            return false;
        }

        // 이메일과 전화번호 및 URL 형태는 비속어 여부와 관계없이 공용 화면에 노출하지 않음
        if (EMAIL_PATTERN.matcher(keyword).find() || PHONE_PATTERN.matcher(keyword).find()
                || URL_PATTERN.matcher(keyword).find()) {
            // 개인정보 또는 광고성 주소가 될 수 있는 검색어를 노출 부적격으로 판정함
            return false;
        }

        // 기존 비속어 정책과 인기 검색어의 공백 삽입 우회까지 함께 검사함
        String compactKeyword = KEYWORD_SPACE_PATTERN.matcher(keyword).replaceAll("");
        // 원문이나 공백 제거본에서 비속어가 발견되면 검색만 허용하고 인기 목록에는 노출하지 않음
        return badWordDetectionService.findBadWord(keyword).isEmpty()
                && badWordDetectionService.findBadWord(compactKeyword).isEmpty();
    }

    /**
     * 날짜별 인기 검색어 점수 Redis 키를 생성함
     *
     * @author SeungHyeon.Kang
     * @param date 인기 검색어를 집계한 한국 표준시 날짜
     * @return 날짜별 인기 검색어 점수 Redis 키
     */
    private String getPopularDayKey(LocalDate date) {
        // 날짜별 점수가 독립적으로 만료되도록 날짜 접미사를 결합한 키를 반환함
        return POPULAR_DAY_KEY_PREFIX + date;
    }

    /**
     * 회원별 인기 검색어 중복 방지 Redis 키를 생성함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 인기 검색어 집계 중복을 제한할 회원 번호
     * @return 회원별 최근 검색어 해시 Redis 키
     */
    private String getUserKeywordKey(Long userNumb) {
        // 영구 탈퇴 시 한 키로 정리할 수 있도록 회원 번호를 접미사로 사용하는 키를 반환함
        return USER_KEYWORD_KEY_PREFIX + userNumb;
    }

    /**
     * 최근 일자별 점수를 합산할 인기 검색어 Redis 키를 생성함
     *
     * @author SeungHyeon.Kang
     * @param date 합산 결과를 생성한 한국 표준시 날짜
     * @return 최근 인기 검색어 합산 Redis 키
     */
    private String getPopularTotalKey(LocalDate date) {
        // 날짜가 바뀌면 이전 합산 결과와 충돌하지 않도록 현재 날짜를 결합한 키를 반환함
        return POPULAR_TOTAL_KEY_PREFIX + date;
    }

    /**
     * 검색어 원문 대신 공급자와 시작 위치를 포함한 SHA-256 해시로 공용 캐시 키를 생성함
     *
     * @author SeungHyeon.Kang
     * @param provider 외부 도서 검색 공급자 코드
     * @param query 사용자가 입력한 도서 검색어
     * @param start 화면 검색 시작 위치
     * @return 검색어 원문을 포함하지 않는 공용 캐시 Redis 키
     */
    private String getCacheKey(String provider, String query, int start) {
        // 대소문자와 연속 공백 차이로 같은 검색이 중복 저장되지 않도록 검색어를 정규화함
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        // 공급자와 시작 위치가 다른 검색 결과가 충돌하지 않도록 해시 입력에 함께 포함함
        String cacheHash = getSha256(provider.trim().toLowerCase(Locale.ROOT) + ":" + normalizedQuery + ":" + start);
        // 검색어 원문이 없는 16진수 해시 Redis 키를 반환함
        return SEARCH_CACHE_KEY_PREFIX + cacheHash;
    }

    /**
     * Redis 식별에 사용할 문자열을 SHA-256 단방향 해시로 변환함
     *
     * @author SeungHyeon.Kang
     * @param value 원문 노출 없이 식별할 문자열
     * @return 소문자 16진수 SHA-256 해시
     */
    private String getSha256(String value) {

        // 런타임이 항상 제공하는 SHA-256 알고리즘으로 입력 원문을 단방향 변환함
        try {
            // Redis 식별 문자열을 해시할 메시지 다이제스트 객체를 생성함
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            // 원문 문자열을 UTF-8 바이트 기준의 고정 길이 해시로 변환함
            byte[] digest = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
            // Redis 키 구성에 사용할 16진수 해시를 반환함
            return HexFormat.of().formatHex(digest);
        }

        // 필수 해시 알고리즘이 없는 런타임은 원문 Redis 키로 우회하지 않고 중단함
        catch (NoSuchAlgorithmException e) {
            // SHA-256을 사용할 수 없는 실행 환경 오류를 호출부에 전달함
            throw new IllegalStateException("SHA-256 algorithm is unavailable.", e);
        }
    }
}
