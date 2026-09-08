package org.our.sadari.book.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.our.sadari.book.dto.BookJsonDto;
import org.our.sadari.book.dto.BookSearchResponseDto;
import org.our.sadari.global.common.result.ResultData;
import org.our.sadari.global.common.util.MessageUtils;
import org.our.sadari.user.dto.UserSettingDto;
import org.our.sadari.user.mapper.UserMapper;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * fileName       : BookSearchServiceTest
 * author         : HanWon.Jang
 * date           : 2026-07-31
 * description    : 계정 언어별 카카오와 Google Books 검색 및 화면 응답 변환을 검증함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-07-31        SeungHyeon.Kang    최초 생성
 * 2026-08-16        SeungHyeon.Kang    도서 검색·인기 검색어 검증 추가
 * 2026-08-28        HanWon.Jang        캐시 유형별 단기 한도 검증
 * 2026-09-08        HanWon.Jang        영어 설정 Google Books 검색 검증 추가
 */
@ExtendWith(MockitoExtension.class)
class BookSearchServiceTest {

    // 외부 도서 검색 API 통신 객체
    @Mock
    private RestTemplate restTemplate;
    // 회원별 제한과 공용 캐시 및 공급자별 쿼터 보호 서비스 대역
    @Mock
    private BookSearchProtectionService bookSearchProtectionService;
    // 로그인 회원의 저장 언어 설정 조회 Mapper 대역
    @Mock
    private UserMapper userMapper;
    // 외부 도서 검색 요청 URI 검증 객체
    @Captor
    private ArgumentCaptor<URI> uriCaptor;
    // 외부 도서 검색 인증 헤더 검증 객체
    @Captor
    private ArgumentCaptor<HttpEntity<?>> httpEntityCaptor;

    // 언어별 외부 도서 검색 서비스
    private BookSearchService bookSearchService;
    // 화면 응답 필드명 검증 객체
    private ObjectMapper objectMapper;

    /** 각 테스트에서 언어별 도서 검색 서비스와 설정값을 구성함 */
    @BeforeEach
    void setUp() {
        // 외부 JSON 응답을 변환할 객체를 생성함
        objectMapper = new ObjectMapper();
        // 도서 검색 테스트 대상을 생성함
        bookSearchService = new BookSearchService(
                restTemplate, objectMapper, bookSearchProtectionService, userMapper
        );
        // 테스트 요청이 사용할 카카오 도서 검색 주소를 설정함
        ReflectionTestUtils.setField(bookSearchService, "bookSearchUrl", "https://dapi.kakao.com/v3/search/book");
        // 테스트 요청이 사용할 가상 카카오 REST API 키를 설정함
        ReflectionTestUtils.setField(bookSearchService, "kakaoRestApiKey", "test-rest-key");
        // 테스트 요청이 사용할 Google Books 주소를 설정함
        ReflectionTestUtils.setField(bookSearchService, "googleBooksUrl", "https://www.googleapis.com/books/v1/volumes");
        // 테스트 요청이 사용할 가상 Google Books API 키를 설정함
        ReflectionTestUtils.setField(bookSearchService, "googleBooksApiKey", "test-google-key");
        // 공통 실패 응답이 사용할 테스트 메시지 소스를 생성함
        StaticMessageSource messageSource = new StaticMessageSource();
        // 검색 실패 코드의 테스트용 사용자 문구를 등록함
        messageSource.addMessage("common.alert.0008", Locale.KOREAN, "검색에 실패했어요.");
        // 검색 요청 제한 코드의 테스트용 사용자 문구를 등록함
        messageSource.addMessage("book.alert.0001", Locale.KOREAN, "검색 요청이 너무 많아요.");
        // 공통 메시지 조회 로케일을 등록한 한국어 문구와 일치시킴
        LocaleContextHolder.setLocale(Locale.KOREAN);
        // 공통 실패 응답에서 테스트 메시지를 조회할 수 있도록 설정함
        new MessageUtils().setMessageSource(messageSource);
    }

    /** 한국어 설정에서 카카오 검색 결과와 50권 페이지 계약을 유지하는지 검증함 */
    @Test
    void searchesKakaoInKorean() {
        String responseBody = """
                {
                  "meta": {"is_end": false, "pageable_count": 20, "total_count": 20},
                  "documents": [{
                    "authors": ["기시미 이치로", "고가 후미타케"],
                    "contents": "도서 소개",
                    "datetime": "2014-11-17T00:00:00.000+09:00",
                    "isbn": "8996991341 9788996991342",
                    "publisher": "인플루엔셜",
                    "thumbnail": "https://search1.kakaocdn.net/thumb/R120x174.q85/?fname=http%3A%2F%2Ft1.daumcdn.net%2Flbook%2Fimage%2F6253040",
                    "title": "미움받을 용기"
                  }]
                }
                """;
        // 한국어 사용 계정 설정을 구성함
        when(userMapper.getUserSettingDtl(7L)).thenReturn(getSetting("N"));
        // 카카오 외부 요청과 검색 보호 허용 결과를 구성함
        when(bookSearchProtectionService.isRequestAllowed(7L, false)).thenReturn(true);
        when(bookSearchProtectionService.reserveProviderCall(7L, "kakao")).thenReturn(true);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        // 카카오 두 번째 페이지에 대응하는 시작 위치로 검색함
        ResultData resultData = bookSearchService.searchBooks(7L, "미움받을 용기", 51);
        BookSearchResponseDto searchResult = assertInstanceOf(BookSearchResponseDto.class, resultData.getData());
        BookJsonDto.BookDto bookDto = assertInstanceOf(BookJsonDto.BookDto.class, searchResult.getBookList().get(0));

        // 기존 카카오 화면 계약과 한국어 도서 코드가 유지되는지 확인함
        assertEquals(200, resultData.getCode());
        assertEquals(101, searchResult.getNextStart());
        assertEquals("ko", bookDto.getLangCode());
        assertEquals("9788996991342", bookDto.getIsbn());
        assertEquals("20141117", bookDto.getPubdate());
        // 카카오 인증과 50권 두 번째 페이지 요청을 확인함
        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.GET), httpEntityCaptor.capture(), eq(String.class));
        assertEquals("KakaoAK test-rest-key", httpEntityCaptor.getValue().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        assertTrue(uriCaptor.getValue().getQuery().contains("page=2"));
        assertTrue(uriCaptor.getValue().getQuery().contains("size=50"));
        // 공급자별 화면 응답 캐시 저장을 확인함
        verify(bookSearchProtectionService).setCachedSearch(
                eq("kakao"), eq("미움받을 용기"), eq(51), any(BookSearchResponseDto.class)
        );
    }

    /** 영어 설정에서 Google Books 검색과 40권 페이지 및 ISBN13 변환을 검증함 */
    @Test
    void searchesGoogleInEnglish() {
        String responseBody = """
                {
                  "totalItems": 100,
                  "items": [{
                    "id": "google-volume-id",
                    "volumeInfo": {
                      "title": "Clean Architecture",
                      "authors": ["Robert C. Martin"],
                      "publisher": "Pearson",
                      "publishedDate": "2018-04",
                      "description": "A guide to software architecture",
                      "industryIdentifiers": [
                        {"type": "ISBN_10", "identifier": "0134494164"},
                        {"type": "ISBN_13", "identifier": "9780134494166"}
                      ],
                      "imageLinks": {
                        "smallThumbnail": "http://books.google.com/small.jpg",
                        "thumbnail": "http://books.google.com/cover.jpg"
                      },
                      "language": "en"
                    }
                  }]
                }
                """;
        // 영어 사용 계정 설정을 구성함
        when(userMapper.getUserSettingDtl(7L)).thenReturn(getSetting("Y"));
        // Google Books 외부 요청과 검색 보호 허용 결과를 구성함
        when(bookSearchProtectionService.isRequestAllowed(7L, false)).thenReturn(true);
        when(bookSearchProtectionService.reserveProviderCall(7L, "google")).thenReturn(true);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        // Google Books 두 번째 페이지에 대응하는 시작 위치로 검색함
        ResultData resultData = bookSearchService.searchBooks(7L, "Clean Architecture", 41);
        BookSearchResponseDto searchResult = assertInstanceOf(BookSearchResponseDto.class, resultData.getData());
        BookJsonDto.BookDto bookDto = searchResult.getBookList().get(0);

        // Google Books 화면 계약과 영어 도서 코드를 확인함
        assertEquals(200, resultData.getCode());
        assertEquals(81, searchResult.getNextStart());
        assertEquals("en", bookDto.getLangCode());
        assertEquals("9780134494166", bookDto.getIsbn());
        assertEquals("201804", bookDto.getPubdate());
        assertEquals("https://books.google.com/cover.jpg", bookDto.getImage());
        // Google Books의 0부터 시작하는 인덱스와 영어 제한 및 최대 40건 요청을 확인함
        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
        String requestQuery = uriCaptor.getValue().getQuery();
        assertTrue(requestQuery.contains("startIndex=40"));
        assertTrue(requestQuery.contains("maxResults=40"));
        assertTrue(requestQuery.contains("langRestrict=en"));
        assertTrue(requestQuery.contains("printType=books"));
        assertTrue(requestQuery.contains("key=test-google-key"));
        // Google 공급자 전용 캐시 저장을 확인함
        verify(bookSearchProtectionService).setCachedSearch(
                eq("google"), eq("Clean Architecture"), eq(41), any(BookSearchResponseDto.class)
        );
    }

    /** 공급자별 공용 캐시에 적중하면 실제 외부 호출 예산을 사용하지 않는지 검증함 */
    @Test
    void skipsCallOnCacheHit() {
        // 영어 사용 계정과 Google 공급자 캐시 결과를 구성함
        when(userMapper.getUserSettingDtl(7L)).thenReturn(getSetting("Y"));
        BookSearchResponseDto cachedResult = new BookSearchResponseDto(List.of(), true, null);
        when(bookSearchProtectionService.getCachedSearch("google", "book", 1)).thenReturn(cachedResult);
        when(bookSearchProtectionService.isRequestAllowed(7L, true)).thenReturn(true);

        // 공용 캐시가 있는 영어 도서 검색을 실행함
        ResultData resultData = bookSearchService.searchBooks(7L, "book", 1);

        // 외부 호출 없이 캐시 결과가 성공 응답으로 반환되는지 확인함
        assertEquals(200, resultData.getCode());
        verifyNoInteractions(restTemplate);
        verify(bookSearchProtectionService, never()).reserveProviderCall(7L, "google");
    }

    /** Google Books 인증 오류가 사용자 공통 검색 실패 코드로 변환되는지 검증함 */
    @Test
    void handlesGoogleBooksFailure() {
        // 영어 사용 계정과 Google Books 인증 오류 흐름을 구성함
        when(userMapper.getUserSettingDtl(7L)).thenReturn(getSetting("Y"));
        when(bookSearchProtectionService.isRequestAllowed(7L, false)).thenReturn(true);
        when(bookSearchProtectionService.reserveProviderCall(7L, "google")).thenReturn(true);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        // 인증 오류가 발생하는 영어 도서 검색을 실행함
        ResultData resultData = bookSearchService.searchBooks(7L, "book", 1);

        // 외부 오류 원문 대신 기존 사용자 공통 검색 실패 코드를 확인함
        assertEquals(2008, resultData.getCode());
    }

    /** 테스트에서 사용할 사용자 언어 설정을 생성함 */
    private UserSettingDto getSetting(String englishYsno) {
        // 영어 사용 여부를 포함한 설정 DTO를 생성함
        UserSettingDto setting = new UserSettingDto();
        setting.setEnglishYsno(englishYsno);
        // 계정 언어 분기에서 사용할 설정을 반환함
        return setting;
    }
}
