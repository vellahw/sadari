# Kakao 도서 검색 API 쿼터를 Redis로 보호한 방법

## 시작한 문제

도서 검색 화면에서 사용자가 10권씩 더 보기를 누를 때마다 외부 API를 다시 호출하면 50권을 확인하는 데 최대 다섯 번의 쿼터를 사용한다. 정상 사용만으로도 호출 수가 빠르게 늘고, 같은 검색어의 반복 요청이나 자동화 요청이 섞이면 애플리케이션 전체 쿼터까지 영향을 받는다.

검색 전 화면에 보여 줄 인기 도서도 단순 독후감 수로 정렬하면 한 사용자가 여러 번 작성한 책이 과대 대표될 수 있었다. 그래서 검색 호출 보호와 인기 도서 기준을 별개의 문제로 나눴다.

## 선택한 구조

외부 검색은 서버가 한 번에 최대 50권을 받아 오고, React 화면이 이를 10권씩 나눠 표시한다. 서버는 호출 전에 Redis에서 다음 세 경계를 확인한다.

1. 같은 회원의 단시간 반복 요청
2. 회원별 하루 요청량
3. 애플리케이션 전체의 실제 외부 호출량

동일 검색어와 페이지는 10분 공용 캐시에 저장한다. 캐시 키에는 원문 검색어 대신 정규화한 검색어와 페이지의 SHA-256 해시를 사용했다. 캐시 적중 요청과 미적중 요청의 단기 한도를 분리해, 값싼 캐시 조회 때문에 실제 검색 사용성이 지나치게 낮아지지 않게 했다.

핵심 흐름은 `BookSearchService.searchBooks`에 모여 있다. 아래 코드는 캐시와 제한의 순서를 보이기 위해 응답 변환 부분을 덜어낸 축약본이다.

```java
KakaoBookJsonDto cached = bookSearchProtectionService.getCachedSearch(query, page);
boolean cacheHit = !StringUtil.isEmpty(cached);

if (!bookSearchProtectionService.isRequestAllowed(userNumb, cacheHit)) {
    return ResultData.fail(ResultEnum.BOOK_SEARCH_RATE_LIMITED);
}

if (StringUtil.isEmpty(cached)) {
    if (!bookSearchProtectionService.reserveProviderCall(userNumb)) {
        return ResultData.fail(ResultEnum.BOOK_SEARCH_RATE_LIMITED);
    }
    ResponseEntity<String> response = requestKakaoBookSearch(query.trim(), page);
    cached = objectMapper.readValue(response.getBody(), KakaoBookJsonDto.class);
    bookSearchProtectionService.setCachedSearch(query, page, cached);
}
```

실제 구현에서는 회원 한도와 전체 한도의 확인·증가를 Redis Lua 스크립트 한 번으로 처리한다. 여러 서버 인스턴스에서 동시에 요청해도 “확인 시점에는 여유가 있었지만 둘 다 증가한” 경쟁 조건을 만들지 않기 위해서다. Redis 상태를 확인할 수 없을 때는 외부 호출을 허용하지 않는 방향으로 실패시켜 쿼터를 보호한다.

## 인기 도서 기준

주간·월간·연간 인기 도서는 기간 안에 독후감을 작성한 고유 사용자 수를 기준으로 상위 10권을 먼저 고른다. 동률이면 최근 작성 시각과 도서 식별 순서로 결과를 고정한다. 별점은 인기 순위의 언어와 관계없이 읽는 중을 제외한 모든 독후감을 합산한다.

이 구분 덕분에 “많이 기록된 책”과 “평가가 좋은 책”이 서로 다른 지표라는 점을 유지할 수 있었다. 목록 조회에서 행마다 평균을 다시 계산하지 않고 먼저 집계한 결과를 결합해 반복 I/O도 피했다.

## 확인한 결과와 남은 일

50권 확인에 필요한 외부 호출은 캐시 미스 기준 최대 다섯 번에서 한 번으로 줄었다. 이는 호출 구조로 계산한 값이며 응답 시간 실측값은 아니다. 같은 검색은 캐시가 살아 있는 동안 외부 호출을 추가로 사용하지 않는다.

현재 외부 제공자는 Kakao 도서 검색이다. 영어 검색어 입력은 가능하지만 영어권 서지·줄거리 품질을 보장하는 Google Books 연동은 아직 구현 전이다. 도서 정보의 언어별 저장 구조도 원본 DDL에는 준비되어 있지만 현재 저장 조회 로직은 ISBN 중심이므로, Google Books를 붙일 때 ISBN과 언어를 함께 조회 조건으로 맞추는 작업이 남아 있다.

## 구현 근거

- [BookSearchService.java](../../src/main/java/org/our/sadari/book/service/BookSearchService.java)
- [BookSearchProtectionService.java](../../src/main/java/org/our/sadari/book/service/BookSearchProtectionService.java)
- [BookPopularService.java](../../src/main/java/org/our/sadari/book/service/BookPopularService.java)
- [도서 검색 쿼터 보호 정책](../policies/book-search-policy.md)
