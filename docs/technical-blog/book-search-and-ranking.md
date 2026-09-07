# Kakao 도서 검색 API 쿼터를 Redis로 보호한 방법

## 문제

도서 검색 화면은 한 번에 10권을 보여 주지만 외부 API는 한 번에 최대 50권을 반환합니다. 화면 단위에 맞춰 외부 API까지 10권씩 호출하면 사용자가 50권을 보는 동안 같은 검색어로 다섯 번 요청하게 됩니다. 캐시, 회원별 제한, 서비스 전체 제한을 각각 따로 검사하면 동시 요청이 제한을 함께 통과하는 문제도 생깁니다.

Sadari는 서버가 50권을 먼저 받아 공용 캐시에 보관하고, 화면이 그 결과를 10권씩 나눠 사용합니다. 캐시가 없는 요청만 외부 호출량에 포함하며 Redis Lua 스크립트로 검사와 증가를 한 번에 처리합니다.

## 검색 흐름

`BookSearchService.searchBooks`는 입력 검증부터 외부 응답 변환까지 검색의 순서를 결정합니다.

```java
public ResultData searchBooks(Long userNumb, String query, int start) {
    // 인증값과 검색어 및 50권 페이지 경계가 올바르지 않으면 외부 요청 전에 차단함
    if (StringUtil.hasEmpty(userNumb, query) || start < MIN_START || start > MAX_START
            || (start - MIN_START) % DISPLAY_COUNT != 0) {
        // "요청값이 올바르지 않아요."
        return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
    }

    try {
        // 기존 시작 위치를 카카오 API의 1부터 시작하는 50권 페이지 번호로 변환함
        int page = ((start - MIN_START) / DISPLAY_COUNT) + 1;
        // 동일 검색어와 페이지의 짧은 공용 캐시를 먼저 조회함
        KakaoBookJsonDto kakaoBookJsonDto =
                bookSearchProtectionService.getCachedSearch(query, page);
        // 조회 결과로 캐시 적중과 미적중 요청의 독립된 단기 제한을 선택함
        boolean cacheHit = !StringUtil.isEmpty(kakaoBookJsonDto);

        // 캐시 유형별 회원 단기 요청 한도를 넘은 검색을 차단함
        if (!bookSearchProtectionService.isRequestAllowed(userNumb, cacheHit)) {
            // "검색 요청이 너무 많아요. 잠시 후 다시 시도해주세요."
            return ResultData.fail(ResultEnum.BOOK_SEARCH_RATE_LIMITED);
        }

        // 공용 캐시에 검색 결과가 없을 때만 카카오 일일 쿼터를 예약하고 외부 API를 호출함
        if (StringUtil.isEmpty(kakaoBookJsonDto)) {
            if (!bookSearchProtectionService.reserveProviderCall(userNumb)) {
                return ResultData.fail(ResultEnum.BOOK_SEARCH_RATE_LIMITED);
            }

            // 사용자 검색어로 카카오 도서 검색 API에서 최대 50권을 호출함
            ResponseEntity<String> response = requestKakaoBookSearch(query.trim(), page);

            // 본문이 없는 외부 응답은 정상 검색 결과로 해석하지 않음
            if (StringUtil.isEmpty(response.getBody())) {
                return ResultData.fail(ResultEnum.COMMON_SEARCH_REJECTED);
            }

            // 카카오 원문 응답을 외부 API 전용 DTO로 역직렬화함
            kakaoBookJsonDto = objectMapper.readValue(response.getBody(), KakaoBookJsonDto.class);
            // 같은 검색어의 반복 호출이 카카오 쿼터를 다시 소모하지 않도록 공용 캐시에 저장함
            bookSearchProtectionService.setCachedSearch(query, page, kakaoBookJsonDto);
        }

        // 외부 필드명이 화면 응답 필드명을 바꾸지 않도록 명시적인 화면 DTO로 변환함
        List<BookJsonDto.BookDto> bookList = getBookList(kakaoBookJsonDto.getDocuments());

        // 결과가 있는 첫 페이지 검색만 인기 검색어 후보로 반영해 추가 페이지와 빈 검색을 제외함
        if (start == MIN_START && !StringUtil.isEmpty(bookList)) {
            bookSearchProtectionService.setPopularKeyword(userNumb, query);
        }

        // 카카오 메타정보가 없으면 추가 호출로 쿼터를 소모하지 않도록 마지막 페이지로 처리함
        boolean isEnd = StringUtil.isEmpty(kakaoBookJsonDto.getMeta())
                || kakaoBookJsonDto.getMeta().isEnd() || start == MAX_START;
        Integer nextStart = isEnd ? null : start + DISPLAY_COUNT;
        return ResultData.success(new BookSearchResponseDto(bookList, isEnd, nextStart));
    }
    // 외부 API의 오류 본문과 인증값은 응답이나 로그로 전달하지 않음
    catch (RestClientResponseException e) {
        log.error("카카오 도서 검색 API가 오류 응답을 반환했습니다. 상태 코드={}",
                e.getStatusCode().value());
        return ResultData.fail(ResultEnum.COMMON_SEARCH_REJECTED);
    }
}
```

코드는 다음 순서로 읽을 수 있습니다.

1. `start`는 화면의 임의 오프셋이 아니라 50권 경계만 허용합니다. 잘못된 값은 외부 호출보다 먼저 차단합니다.
2. `page`로 변환한 뒤 캐시를 먼저 봅니다. 그래야 캐시 적중 여부에 따라 서로 다른 요청 제한을 적용할 수 있습니다.
3. `isRequestAllowed`는 캐시 요청까지 포함한 회원의 짧은 시간 반복을 막습니다.
4. `reserveProviderCall`은 캐시 미적중일 때만 실행됩니다. 따라서 캐시 조회가 외부 제공자의 일일 쿼터를 차감하지 않습니다.
5. 응답 본문이 비어 있으면 빈 검색 결과가 아니라 제공자 장애로 처리합니다.
6. 외부 DTO를 화면 DTO로 변환해 카카오 응답 필드의 변경이 프론트엔드 계약으로 바로 번지지 않게 합니다.
7. 인기 검색어는 첫 페이지의 성공 검색만 기록합니다. 더 보기 요청을 별도 검색으로 중복 집계하지 않기 위해서입니다.

## Redis에서 검사와 증가를 묶기

짧은 시간 요청 제한은 `BookSearchProtectionService.isRequestAllowed`가 담당합니다.

```java
public boolean isRequestAllowed(Long userNumb, boolean cacheHit) {
    // 인증되지 않은 요청은 외부 API 쿼터를 사용할 수 없도록 차단함
    if (StringUtil.isEmpty(userNumb)) {
        return false;
    }

    // Redis 장애 시 카카오 쿼터가 무방비로 소모되지 않도록 검색 요청을 차단함
    try {
        // 캐시 유형에 맞는 한 회원의 분간 요청 제한값을 선택함
        int rateLimit = cacheHit
                ? cacheHitRateLimitPerMinute
                : cacheMissRateLimitPerMinute;
        // 캐시 유형별 독립 카운터로 회원의 분간 요청 제한을 검사함
        Long result = redisTemplate.execute(
                REQUEST_LIMIT_SCRIPT,
                List.of(getMinuteLimitKey(userNumb, cacheHit)),
                String.valueOf(rateLimit),
                String.valueOf(MINUTE_LIMIT_TTL_SECONDS)
        );
        // Redis가 명시적으로 허용한 요청만 검색 진행 대상으로 반환함
        return !StringUtil.isEmpty(result) && result == REQUEST_ALLOWED;
    }
    catch (RuntimeException e) {
        log.error("도서 검색 회원별 요청 제한을 확인하지 못했습니다.", e);
        // 제한을 확인하지 못한 요청을 카카오 호출 전에 차단함
        return false;
    }
}
```

`cacheHit`에 따라 제한값뿐 아니라 카운터 키도 나뉩니다. 캐시 적중은 외부 비용이 없으므로 더 넉넉하게 허용할 수 있고, 미적중 요청은 더 엄격히 제어할 수 있습니다. Redis가 응답하지 않을 때 `true`를 반환하지 않는 것도 중요합니다. 이 기능의 보호 대상은 단순 응답 속도가 아니라 외부 서비스 전체 쿼터이므로, 한도를 확인할 수 없다면 호출하지 않는 실패 폐쇄 방식을 택했습니다.

실제 외부 호출을 예약할 때는 회원 한도와 서비스 전체 한도를 함께 넘겨줍니다.

```java
public boolean reserveProviderCall(Long userNumb) {
    if (StringUtil.isEmpty(userNumb)) {
        return false;
    }

    try {
        // 회원별 일간 및 앱 전체 실제 호출 횟수를 한 번의 Redis 명령으로 예약함
        Long result = redisTemplate.execute(
                PROVIDER_LIMIT_SCRIPT,
                List.of(getDailyLimitKey(userNumb), PROVIDER_LIMIT_KEY),
                String.valueOf(rateLimitPerDay),
                String.valueOf(providerCallLimitPerDay),
                String.valueOf(DAILY_LIMIT_TTL_SECONDS)
        );
        // 두 일간 한도 안에서 함께 예약된 외부 호출만 허용함
        return !StringUtil.isEmpty(result) && result == REQUEST_ALLOWED;
    }
    catch (RuntimeException e) {
        log.error("도서 검색 회원별 및 카카오 일일 호출 한도를 확인하지 못했습니다.", e);
        return false;
    }
}
```

애플리케이션에서 `GET`으로 두 값을 읽고 Java에서 비교한 다음 `INCR`을 두 번 호출하면, 동시에 들어온 요청들이 모두 이전 값을 읽고 제한을 통과할 수 있습니다. Lua 스크립트에 두 키를 함께 전달하면 Redis 안에서 검사와 증가가 원자적으로 실행됩니다.

## 인기 도서 기간 계산

인기 도서는 서버의 실행 환경이 아니라 서울 날짜를 기준으로 주·월·연 경계를 만듭니다.

```java
String normalizedPeriod = period.trim().toLowerCase(Locale.ROOT);
LocalDate currentDate = LocalDate.now(SEOUL_ZONE);

switch (normalizedPeriod) {
    case PERIOD_WEEKLY:
        // 주간 집계는 현재 날짜가 속한 주의 월요일부터 시작함
        periodStartDate = currentDate.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        nextPeriodStartDate = periodStartDate.plusWeeks(1);
        break;
    case PERIOD_MONTHLY:
        // 월간 집계는 현재 날짜가 속한 달의 1일부터 시작함
        periodStartDate = currentDate.withDayOfMonth(1);
        nextPeriodStartDate = periodStartDate.plusMonths(1);
        break;
    case PERIOD_YEARLY:
        // 연간 집계는 현재 날짜가 속한 연도의 1월 1일부터 시작함
        periodStartDate = currentDate.withDayOfYear(1);
        nextPeriodStartDate = periodStartDate.plusYears(1);
        break;
    default:
        throw new CustomException(ResultEnum.COMMON_INVALID_REQUEST, HttpStatus.BAD_REQUEST);
}
```

종료일을 그 기간의 마지막 시각으로 만들지 않고 다음 기간의 시작으로 계산합니다. 조회 조건을 `시작 이상, 다음 시작 미만`으로 만들면 밀리초나 데이터베이스 정밀도 차이 때문에 마지막 날의 기록이 빠지는 문제를 피할 수 있습니다. 조회된 목록은 독후감을 작성한 고유 사용자 수를 우선 기준으로 정렬하고, 평점은 언어와 관계없이 완료된 독서 기록 전체를 합산합니다.

## 한계와 다음 작업

현재 검색 제공자는 Kakao뿐입니다. 영어 검색어는 전달할 수 있지만 영어권 서지 정보와 영문 줄거리 품질을 보장하는 Google Books 연동은 아직 구현하지 않았습니다.

도서 저장 구조에는 언어별 도서를 구분할 준비가 되어 있지만 현재 `BookMapper.dupBook`과 `BookMapper.getBookNumbByIsbn` 호출은 ISBN만으로 기존 도서를 찾습니다. 같은 ISBN의 한국어·영어 메타데이터를 별도 도서로 저장하려면 Google Books를 붙이기 전에 이 조회 계약을 ISBN과 언어의 조합으로 맞춰야 합니다. 이 부분은 완료된 기능처럼 서술하지 않았습니다.

## 관련 소스

- [BookSearchService.java](../../src/main/java/org/our/sadari/book/service/BookSearchService.java)
- [BookSearchProtectionService.java](../../src/main/java/org/our/sadari/book/service/BookSearchProtectionService.java)
- [BookPopularService.java](../../src/main/java/org/our/sadari/book/service/BookPopularService.java)
- [도서 검색 쿼터 보호 정책](../policies/book-search-policy.md)
