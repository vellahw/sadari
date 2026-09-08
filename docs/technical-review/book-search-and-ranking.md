# 언어 설정에 따라 Kakao와 Google Books를 전환하고 Redis로 쿼터 보호하기

## 문제

한국어 화면에서 사용하던 Kakao 도서 검색은 국내 도서 정보에 적합하지만, 영어권 도서를 영어 제목과 영문 줄거리로 찾는 용도에는 한계가 있습니다. 그렇다고 검색 화면을 공급자마다 따로 만들면 화면 계약, 페이지 처리, 캐시와 저장 로직이 모두 두 벌로 늘어납니다.

Sadari는 로그인 계정의 언어 설정이 한국어이면 Kakao, 영어이면 Google Books를 사용합니다. 외부 응답은 서버에서 하나의 화면 DTO로 변환하므로 React 화면은 어느 공급자가 응답했는지 몰라도 같은 검색 목록을 그릴 수 있습니다. 검색 결과에는 도서 정보 언어를 함께 넣고, 저장 단계에서는 ISBN과 언어의 조합으로 기존 도서를 찾습니다.

Google Books의 Volumes API는 `q` 검색어, 0부터 시작하는 `startIndex`, 최대 40건의 `maxResults`, 결과 언어를 제한하는 `langRestrict`를 제공합니다. 구현은 이 공식 계약을 기준으로 했습니다. 자세한 값은 [Google Books API 사용 문서](https://developers.google.com/books/docs/v1/using)와 [Volumes 목록 참조](https://developers.google.com/books/docs/v1/reference/volumes/list)에서 확인할 수 있습니다.

## 전체 흐름

검색 한 번은 다음 순서로 처리됩니다.

1. 인증된 회원 번호와 검색어를 확인합니다.
2. 회원 설정에서 영어 사용 여부를 읽습니다.
3. 한국어는 Kakao 50건, 영어는 Google Books 40건의 페이지 경계를 적용합니다.
4. 공급자·검색어·시작 위치가 같은 Redis 캐시를 먼저 찾습니다.
5. 캐시 유형별 회원 단기 한도와 실제 외부 호출 한도를 확인합니다.
6. 외부 응답을 공통 화면 DTO로 변환하고 캐시에 저장합니다.
7. 도서를 저장할 때 ISBN과 언어가 모두 같은 기존 행만 재사용합니다.

## 1. 요청 헤더가 아니라 계정 설정으로 공급자 선택

브라우저의 `Accept-Language`만 보고 공급자를 선택하면 다른 기기나 새 탭에서 계정 설정과 검색 결과가 어긋날 수 있습니다. 검색 서비스는 인증된 회원 번호로 저장 설정을 직접 조회합니다.

```java
// 요청 헤더가 아닌 계정에 저장된 언어 설정을 조회함
UserSettingDto userSetting = userMapper.getUserSettingDtl(userNumb);
// 영어 사용 설정이 명시된 경우에만 Google Books 공급자를 선택함
String provider = isEnglishSetting(userSetting) ? PROVIDER_GOOGLE : PROVIDER_KAKAO;
// 공급자별 공식 최대 조회 건수를 현재 페이지 크기로 선택함
int displayCount = getDisplayCount(provider);
```

첫 줄은 로그인 회원의 설정 행을 읽습니다. 두 번째 줄은 영어 사용값이 명시적으로 `Y`일 때만 Google을 선택합니다. 설정 행이 없거나 값이 비어 있는 기존 회원은 Kakao를 유지하므로 배포 직후의 호환성도 보존됩니다. 세 번째 줄은 선택한 공급자의 페이지 크기를 정합니다.

판정 함수도 같은 원칙을 코드로 고정합니다.

```java
private boolean isEnglishSetting(UserSettingDto userSetting) {
    // 설정 행이 없거나 영어 사용이 명시되지 않은 기존 회원은 한국어 검색을 유지함
    return !StringUtil.isEmpty(userSetting)
            && Constant.COMM_YES.equals(userSetting.getEnglishYsno());
}
```

`userSetting`이 없을 때 바로 `false`가 되므로 null 참조가 발생하지 않습니다. 값 비교는 문자열 리터럴 대신 공통 상수를 사용합니다.

## 2. 서로 다른 페이지 규칙을 하나의 `nextStart`로 감추기

Kakao는 한 요청에 최대 50건, Google Books는 최대 40건을 반환합니다. 화면이 임의의 시작 위치를 보내도록 두면 같은 페이지를 중복 호출하거나 결과를 건너뛸 수 있으므로 공급자별 경계만 허용합니다.

```java
// 공급자 페이지 경계를 벗어난 시작 위치를 외부 요청 전에 차단함
if (!isValidStart(start, displayCount)) {
    // "요청값이 올바르지 않아요."
    return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
}
```

```java
private boolean isValidStart(int start, int displayCount) {
    // 1부터 시작하는 공급자별 페이지 경계와 최대 페이지 수를 함께 확인함
    return start >= MIN_START && (start - MIN_START) % displayCount == 0
            && getPage(start, displayCount) <= MAX_PAGE_COUNT;
}
```

영어 검색의 시작 위치는 `1, 41, 81...`, 한국어 검색은 `1, 51, 101...`만 유효합니다. React는 숫자를 직접 더하지 않고 서버가 반환한 `nextStart`를 사용하므로 공급자가 바뀌어도 화면 코드를 나누지 않습니다.

Google Books는 0부터 시작하는 `startIndex`를 사용하므로 화면의 1부터 시작하는 값을 요청 직전에 변환합니다.

```java
// 사용자 검색어와 0부터 시작하는 시작 인덱스로 Google Books를 호출함
ResponseEntity<String> response = requestGoogleBookSearch(query, start - MIN_START);
```

첫 화면의 `start=1`은 `startIndex=0`, 두 번째 영어 페이지의 `start=41`은 `startIndex=40`이 됩니다.

## 3. Google Books 요청 구성

외부 주소와 API 키는 환경 설정에서 주입합니다. 키는 프론트엔드로 보내지 않고 서버 요청에만 사용합니다.

```java
// Google Books 도서 검색 URL 설정값
@Value("${google.books.url}")
private String googleBooksUrl;

// Google Books API 인증에 사용하는 서버 API 키
@Value("${google.books.api-key:}")
private String googleBooksApiKey;
```

실제 요청은 다음과 같이 만듭니다.

```java
private ResponseEntity<String> requestGoogleBookSearch(String query, int startIndex) {
    // 검색어와 영어 제한 및 인증 키를 안전하게 인코딩한 Google Books 요청 URI를 생성함
    URI uri = UriComponentsBuilder
            .fromUriString(googleBooksUrl)
            .queryParam("q", query)
            .queryParam("startIndex", startIndex)
            .queryParam("maxResults", GOOGLE_DISPLAY_COUNT)
            .queryParam("langRestrict", LANGUAGE_ENGLISH)
            .queryParam("printType", "books")
            .queryParam("orderBy", "relevance")
            .queryParam("key", googleBooksApiKey)
            .build()
            .encode()
            .toUri();
    // Google Books 공개 도서 검색 응답을 반환함
    return restTemplate.exchange(uri, HttpMethod.GET, HttpEntity.EMPTY, String.class);
}
```

각 줄의 역할은 다음과 같습니다.

- `q`에는 사용자가 입력한 검색어를 전달합니다.
- `startIndex`는 Google의 0 기반 페이지 시작 위치입니다.
- `maxResults`는 공식 최대값인 40으로 고정합니다.
- `langRestrict=en`은 영어 도서 결과를 요청합니다.
- `printType=books`는 잡지 결과를 제외합니다.
- `orderBy=relevance`는 검색어 관련도 순서를 사용합니다.
- `key`는 서버 설정에서 읽은 API 키입니다.
- `encode()`는 공백과 특수문자가 있는 검색어를 URI에 맞게 인코딩합니다.

API 키가 비어 있으면 인증 없는 호출을 반복하지 않고 기존 검색 실패 응답으로 종료합니다. 예외 로그에는 상태 코드만 남기며 키나 외부 응답 본문은 기록하지 않습니다.

## 4. Google 응답을 기존 화면 DTO로 변환

Google의 응답 필드명을 React가 직접 사용하게 하면 외부 계약 변경이 화면 전체로 번집니다. 전용 원문 DTO로 받은 뒤 기존 `title`, `author`, `isbn`, `image`, `description`, `pubdate` 계약으로 바꿉니다.

```java
private BookJsonDto.BookDto getGoogleBookDto(
        GoogleBooksJsonDto.VolumeInfoDto volumeInfo) {
    // 화면 도서 필드를 명시적으로 설정할 응답 객체를 생성함
    BookJsonDto.BookDto bookDto = new BookJsonDto.BookDto();
    // Google Books 도서 제목을 기존 화면 필드에 설정함
    bookDto.setTitle(getSafeText(volumeInfo.getTitle()));
    // Google Books 저자 배열을 기존 구분 문자열로 변환하여 설정함
    bookDto.setAuthor(getAuthor(volumeInfo.getAuthors()));
    // Google Books 출판사를 기존 화면 필드에 설정함
    bookDto.setPublisher(getSafeText(volumeInfo.getPublisher()));
    // ISBN13을 우선하고 없으면 ISBN10을 설정함
    bookDto.setIsbn(getGoogleIsbn(volumeInfo.getIndustryIdentifiers()));
    // 같은 ISBN의 한국어 도서 정보와 구분할 영어 코드를 설정함
    bookDto.setLangCode(LANGUAGE_ENGLISH);
    // Google Books 기본 표지를 HTTPS 주소로 정규화하여 설정함
    bookDto.setImage(getGoogleBookImage(volumeInfo.getImageLinks()));
    // 기본 표지 실패 시 사용할 Google Books 소형 표지를 별도 필드에 설정함
    bookDto.setThumbnailImage(getGoogleSmallThumbnail(volumeInfo.getImageLinks()));
    // Google Books 영어 도서 설명을 기존 화면 필드에 설정함
    bookDto.setDescription(getSafeText(volumeInfo.getDescription()));
    // 부분 날짜를 포함한 Google Books 출간일을 숫자 형식으로 변환함
    bookDto.setPubdate(getPublishedDate(volumeInfo.getPublishedDate()));
    // 외부 필드명과 분리된 사용자 화면 도서 정보를 반환함
    return bookDto;
}
```

저자는 Google 배열을 기존 캐럿 구분 문자열로 바꿉니다. ISBN은 두 형식이 함께 오면 ISBN13을 먼저 선택합니다. `langCode=en`은 같은 ISBN의 한국어 서지 정보와 영어 서지 정보를 저장 단계에서 구분하는 값입니다.

Google 표지 주소가 `http`로 내려오면 HTTPS 화면에서 혼합 콘텐츠로 차단될 수 있어 스킴을 변환합니다.

```java
private String normalizeHttpsUrl(String imageUrl) {
    // 표지 주소가 없으면 화면 기본 이미지가 사용되도록 빈 문자열을 반환함
    if (StringUtil.isEmpty(imageUrl)) {
        return StringUtil.EMPTY;
    }
    // Google Books가 HTTP 주소를 반환해도 HTTPS 화면에서 불러올 수 있도록 변환함
    return imageUrl.startsWith("http://")
            ? "https://" + imageUrl.substring(7)
            : imageUrl;
}
```

Google Books의 출간일은 `YYYY`, `YYYY-MM`, `YYYY-MM-DD`처럼 정밀도가 다를 수 있습니다. 존재하지 않는 월·일을 임의로 만들지 않고 숫자만 남겨 기존 화면 포매터가 받은 정밀도만 표시하게 합니다.

## 5. 공급자별로 캐시와 호출 한도 분리

같은 `clean architecture`라는 검색어라도 Kakao 결과와 Google 결과는 다릅니다. 검색어와 페이지만 캐시 키에 넣으면 사용자가 언어를 바꾼 뒤 이전 공급자의 결과를 받을 수 있습니다. 캐시 조회와 저장에 공급자 코드를 함께 전달합니다.

```java
// 공급자와 검색어 및 시작 위치가 같은 짧은 공용 캐시를 먼저 조회함
BookSearchResponseDto searchResponse = bookSearchProtectionService
        .getCachedSearch(provider, query, start);
// 조회 결과로 캐시 적중과 미적중 요청의 독립된 단기 제한을 선택함
boolean cacheHit = !StringUtil.isEmpty(searchResponse);
```

키에는 검색어 원문을 넣지 않습니다.

```java
private String getCacheKey(String provider, String query, int start) {
    // 대소문자와 연속 공백 차이로 같은 검색이 중복 저장되지 않도록 검색어를 정규화함
    String normalizedQuery = query.trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("\\s+", " ");
    // 공급자와 시작 위치가 다른 검색 결과가 충돌하지 않도록 해시 입력에 함께 포함함
    String cacheHash = getSha256(
            provider.trim().toLowerCase(Locale.ROOT)
                    + ":" + normalizedQuery + ":" + start);
    // 검색어 원문이 없는 16진수 해시 Redis 키를 반환함
    return SEARCH_CACHE_KEY_PREFIX + cacheHash;
}
```

첫 줄은 철자 대소문자와 연속 공백 차이를 합칩니다. 다음 줄은 공급자, 정규화 검색어, 시작 위치를 하나의 SHA-256 입력으로 만듭니다. Redis에는 해시만 남기므로 캐시 키에서 검색어 원문이 드러나지 않습니다.

실제 호출 한도도 공급자별 앱 카운터를 사용합니다.

```java
public boolean reserveProviderCall(Long userNumb, String provider) {
    // 인증되지 않았거나 공급자가 없는 요청은 회원별 일간 한도와 공급자 쿼터를 사용할 수 없도록 차단함
    if (StringUtil.hasEmpty(userNumb, provider)) {
        return false;
    }

    try {
        // 회원별 일간 및 앱 전체 실제 호출 횟수를 한 번의 Redis 명령으로 예약함
        Long result = redisTemplate.execute(
                PROVIDER_LIMIT_SCRIPT,
                List.of(getDailyLimitKey(userNumb), getProviderLimitKey(provider)),
                String.valueOf(rateLimitPerDay),
                String.valueOf(providerCallLimitPerDay),
                String.valueOf(DAILY_LIMIT_TTL_SECONDS)
        );
        // 두 일간 한도 안에서 함께 예약된 외부 호출만 허용함
        return !StringUtil.isEmpty(result) && result == REQUEST_ALLOWED;
    }
    catch (RuntimeException e) {
        log.error("도서 검색 회원별 및 공급자별 일일 호출 한도를 확인하지 못했습니다.", e);
        // 보호 한도를 확인하지 못한 외부 공급자 호출을 차단함
        return false;
    }
}
```

회원별 실제 호출 한도는 두 공급자를 합산하고, 앱 전체 한도는 공급자 코드별로 나뉩니다. Redis Lua 스크립트가 검사와 증가를 한 번에 처리하므로 동시 요청이 같은 잔여 한도를 보고 모두 통과하는 경쟁 조건을 막습니다. Redis 장애 시에는 외부 호출을 허용하지 않는 실패 폐쇄 방식을 유지합니다.

## 6. ISBN과 언어를 함께 사용하는 도서 식별

영문판과 한국어판 검색 결과가 같은 ISBN을 가질 수 있어도 제목, 출판사, 표지와 줄거리는 서로 다를 수 있습니다. 내부 도서 번호는 그대로 단일 기본키로 사용하고, 도서 중복 확인과 기존 번호 조회에 ISBN과 언어를 함께 전달합니다.

```java
// ISBN과 언어 기준으로 이미 등록된 도서가 있는지 확인함
int dupBook(BookDto bookDto);

// ISBN과 언어 기준으로 기존 도서 번호를 조회함
Long getBookNumbByIsbn(BookDto bookDto);
```

검색 결과가 독후감이나 독서 모임 저장 요청으로 이동할 때도 언어 코드를 보존합니다.

```typescript
const data = {
  bookTitl: stripHtmlTags(selectedBook.title),
  bookAthr: normalizeBookAuthor(selectedBook.author),
  bookPubl: stripHtmlTags(selectedBook.publisher),
  bookIsbn: sanitizeText(selectedBook.isbn),
  langCode: selectedBook.langCode,
  bookCvim: sanitizeText(selectedBook.image),
  bookDesc: stripHtmlTags(selectedBook.description),
  publDate: stripHtmlTags(selectedBook.pubdate),
};
```

결과적으로 같은 ISBN이라도 `ko`와 `en`은 각각의 도서 정보를 유지합니다. 독후감 평점 조회는 기존 정책대로 ISBN 전체를 기준으로 하므로 서지 정보 언어가 달라도 평점은 합산됩니다.

## 7. 회귀 테스트

단위 테스트는 두 공급자의 분기와 외부 계약을 각각 검증합니다.

```java
// 영어 사용 계정 설정을 구성함
when(userMapper.getUserSettingDtl(7L)).thenReturn(getSetting("Y"));
// Google Books 외부 요청과 검색 보호 허용 결과를 구성함
when(bookSearchProtectionService.isRequestAllowed(7L, false)).thenReturn(true);
when(bookSearchProtectionService.reserveProviderCall(7L, "google")).thenReturn(true);

// Google Books 두 번째 페이지에 대응하는 시작 위치로 검색함
ResultData resultData = bookSearchService.searchBooks(
        7L, "Clean Architecture", 41);
```

```java
// Google Books 화면 계약과 영어 도서 코드를 확인함
assertEquals(81, searchResult.getNextStart());
assertEquals("en", bookDto.getLangCode());
assertEquals("9780134494166", bookDto.getIsbn());
assertEquals("201804", bookDto.getPubdate());
assertEquals("https://books.google.com/cover.jpg", bookDto.getImage());
```

또한 요청 URI에 `startIndex=40`, `maxResults=40`, `langRestrict=en`, `printType=books`가 들어가는지 확인합니다. 한국어 테스트는 기존 Kakao 인증 헤더, 50건 페이지, ISBN13과 날짜 변환을 계속 검증합니다. 공급자별 캐시 테스트는 캐시 적중 시 외부 API와 실제 호출 한도를 사용하지 않는지 확인합니다.

## 현재 상태와 남은 운영 확인

Google Books 호출, 영어 결과 매핑, 공급자별 캐시·쿼터 분리, ISBN+언어 저장 조회는 구현되었습니다. 운영 환경에서는 Google Books API가 활성화된 프로젝트의 서버 키를 `GOOGLE_BOOKS_API_KEY`에 등록해야 영어 검색이 성공합니다.

자동 테스트는 외부 API를 모의 응답으로 검증하므로 Google의 실제 검색 품질과 운영 키 제한 설정까지 보장하지는 않습니다. 배포 후에는 영어 계정으로 대표 검색어, 부분 출간일, 표지가 없는 도서, ISBN이 없는 결과의 제외 동작을 한 번 더 확인해야 합니다.

## 관련 소스

- [BookSearchService.java](../../src/main/java/org/our/sadari/book/service/BookSearchService.java)
- [GoogleBooksJsonDto.java](../../src/main/java/org/our/sadari/book/dto/GoogleBooksJsonDto.java)
- [BookSearchProtectionService.java](../../src/main/java/org/our/sadari/book/service/BookSearchProtectionService.java)
- [BookMapper.java](../../src/main/java/org/our/sadari/book/mapper/BookMapper.java)
- [도서 검색 쿼터 보호 정책](../policies/book-search-policy.md)
