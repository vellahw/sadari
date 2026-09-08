package org.our.sadari.book.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.our.sadari.book.dto.BookJsonDto;
import org.our.sadari.book.dto.BookSearchResponseDto;
import org.our.sadari.book.dto.GoogleBooksJsonDto;
import org.our.sadari.book.dto.KakaoBookJsonDto;
import org.our.sadari.book.util.BookCoverUrlUtil;
import org.our.sadari.global.common.constant.Constant;
import org.our.sadari.global.common.result.ResultData;
import org.our.sadari.global.common.result.ResultEnum;
import org.our.sadari.global.common.util.StringUtil;
import org.our.sadari.user.dto.UserSettingDto;
import org.our.sadari.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * fileName       : BookSearchService
 * author         : HanWon.Jang
 * date           : 2026-07-06
 * description    : 사용자 언어 설정에 맞는 외부 도서 검색 API를 호출하고 화면 응답으로 변환함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-07-06        SeungHyeon.Kang    최초 생성
 * 2026-07-31        SeungHyeon.Kang    종료된 네이버 API를 카카오 도서 검색 API로 교체
 * 2026-08-16        SeungHyeon.Kang    도서 검색·인기 검색어 처리 추가
 * 2026-08-28        HanWon.Jang        캐시 적중 한도 분리
 * 2026-09-08        HanWon.Jang        영어 설정의 Google Books 검색 분기 추가
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookSearchService {

    // 카카오 도서 검색 API의 요청당 최대 조회 건수
    private static final int KAKAO_DISPLAY_COUNT = 50;
    // Google Books API의 요청당 최대 조회 건수
    private static final int GOOGLE_DISPLAY_COUNT = 40;
    // 최소 시작 설정값
    private static final int MIN_START = 1;
    // 공급자별 최대 50페이지를 허용할 페이지 수
    private static final int MAX_PAGE_COUNT = 50;
    // 카카오 REST API 인증 스킴
    private static final String KAKAO_AUTH_SCHEME = "KakaoAK ";
    // 한국어 설정에서 사용할 카카오 공급자 코드
    private static final String PROVIDER_KAKAO = "kakao";
    // 영어 설정에서 사용할 Google Books 공급자 코드
    private static final String PROVIDER_GOOGLE = "google";
    // 한국어 도서 정보 언어 코드
    private static final String LANGUAGE_KOREAN = "ko";
    // 영어 도서 정보 언어 코드
    private static final String LANGUAGE_ENGLISH = "en";
    // Google Books ISBN13 식별자 유형
    private static final String GOOGLE_ISBN_13 = "ISBN_13";
    // Google Books ISBN10 식별자 유형
    private static final String GOOGLE_ISBN_10 = "ISBN_10";

    // 카카오 도서 검색 URL 설정값
    @Value("${book.search.url}")
    private String bookSearchUrl;

    // 카카오 도서 검색 API 인증에 사용하는 REST API 키
    @Value("${kakao.key.restApi}")
    private String kakaoRestApiKey;

    // Google Books 도서 검색 URL 설정값
    @Value("${google.books.url}")
    private String googleBooksUrl;

    // Google Books API 인증에 사용하는 서버 API 키
    @Value("${google.books.api-key:}")
    private String googleBooksApiKey;

    // 외부 HTTP API 통신 객체
    private final RestTemplate restTemplate;
    // 외부 JSON 응답 변환 객체
    private final ObjectMapper objectMapper;
    // 회원별 검색 제한과 공용 검색 캐시 및 공급자별 쿼터 보호 서비스
    private final BookSearchProtectionService bookSearchProtectionService;
    // 로그인 회원의 저장 언어 설정 조회 Mapper
    private final UserMapper userMapper;

    /**
     * 검색어와 사용자 언어 설정에 맞는 외부 도서 목록을 검색함
     *
     * @author HanWon.Jang
     * @param userNumb 도서 검색을 요청한 로그인 회원 번호
     * @param query 외부 도서 API에 전달할 검색어
     * @param start 화면에서 사용하는 검색 결과 시작 위치
     * @return 사용자 화면 형식으로 변환된 도서 검색 결과
     */
    public ResultData searchBooks(Long userNumb, String query, int start) {
        // 인증값과 검색어가 올바르지 않으면 설정 조회와 외부 요청 전에 차단함
        if (StringUtil.hasEmpty(userNumb, query)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 계정 설정 조회와 외부 API 통신 및 응답 변환 실패를 공통 검색 실패 응답으로 격리함
        try {
            // 요청 헤더가 아닌 계정에 저장된 언어 설정을 조회함
            UserSettingDto userSetting = userMapper.getUserSettingDtl(userNumb);
            // 영어 사용 설정이 명시된 경우에만 Google Books 공급자를 선택함
            String provider = isEnglishSetting(userSetting) ? PROVIDER_GOOGLE : PROVIDER_KAKAO;
            // 공급자별 공식 최대 조회 건수를 현재 페이지 크기로 선택함
            int displayCount = getDisplayCount(provider);

            // 공급자 페이지 경계를 벗어난 시작 위치를 외부 요청 전에 차단함
            if (!isValidStart(start, displayCount)) {
                // "요청값이 올바르지 않아요."
                return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
            }

            // 공급자와 검색어 및 시작 위치가 같은 짧은 공용 캐시를 먼저 조회함
            BookSearchResponseDto searchResponse = bookSearchProtectionService
                    .getCachedSearch(provider, query, start);
            // 조회 결과로 캐시 적중과 미적중 요청의 독립된 단기 제한을 선택함
            boolean cacheHit = !StringUtil.isEmpty(searchResponse);

            // 캐시 유형별 회원 단기 요청 한도를 넘은 검색을 차단함
            if (!bookSearchProtectionService.isRequestAllowed(userNumb, cacheHit)) {
                // "검색 요청이 너무 많아요. 잠시 후 다시 시도해주세요."
                return ResultData.fail(ResultEnum.BOOK_SEARCH_RATE_LIMITED);
            }

            // 공용 캐시에 검색 결과가 없을 때만 공급자별 일일 쿼터를 예약하고 외부 API를 호출함
            if (StringUtil.isEmpty(searchResponse)) {
                // 회원별 일간 또는 공급자별 앱 전체 실제 호출이 보호 한도를 넘으면 요청 전에 차단함
                if (!bookSearchProtectionService.reserveProviderCall(userNumb, provider)) {
                    // "검색 요청이 너무 많아요. 잠시 후 다시 시도해주세요."
                    return ResultData.fail(ResultEnum.BOOK_SEARCH_RATE_LIMITED);
                }

                // 선택된 공급자의 원문 응답을 기존 화면 페이지 계약으로 변환함
                searchResponse = PROVIDER_GOOGLE.equals(provider)
                        ? searchGoogleBooks(query.trim(), start)
                        : searchKakaoBooks(query.trim(), start);
                // 같은 공급자 검색의 반복 호출이 외부 쿼터를 다시 소모하지 않도록 공용 캐시에 저장함
                bookSearchProtectionService.setCachedSearch(provider, query, start, searchResponse);
            }

            // 결과가 있는 첫 페이지 검색만 인기 검색어 후보로 반영해 추가 페이지와 빈 검색을 제외함
            if (start == MIN_START && !StringUtil.isEmpty(searchResponse.getBookList())) {
                // 검색 성공 응답과 독립된 Redis 인기 검색어 집계를 시도함
                bookSearchProtectionService.setPopularKeyword(userNumb, query);
            }

            // 공급자와 무관한 동일 화면 응답 계약을 반환함
            return ResultData.success(searchResponse);
        }

        // 인증, 호출량 또는 요청 오류는 비밀값과 원문 응답을 제외한 상태 코드만 기록함
        catch (RestClientResponseException e) {
            // 운영에서 외부 API 거절 원인을 구분할 수 있도록 HTTP 상태만 기록함
            log.error("외부 도서 검색 API가 오류 응답을 반환했습니다. 상태 코드={}", e.getStatusCode().value());
            // "검색에 실패했어요.\n다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_SEARCH_REJECTED);
        }

        // 외부 통신 또는 JSON 계약 오류는 사용자와 로그에 요청 주소 및 응답 원문을 노출하지 않음
        catch (RestClientException | JsonProcessingException e) {
            // API 키가 포함될 수 있는 예외 메시지 대신 실패 유형만 기록함
            log.error("외부 도서 검색 API 연동에 실패했습니다. 오류 유형={}", e.getClass().getSimpleName());
            // "검색에 실패했어요.\n다시 시도해주세요."
            return ResultData.fail(ResultEnum.COMMON_SEARCH_REJECTED);
        }
    }

    /**
     * 카카오 도서 검색 API 응답을 화면 페이지 계약으로 변환함
     *
     * @author HanWon.Jang
     * @param query 카카오 도서 API에 전달할 검색어
     * @param start 화면 검색 시작 위치
     * @return 카카오 도서 검색 화면 응답
     * @throws JsonProcessingException 카카오 응답 JSON 변환에 실패한 경우 발생
     */
    private BookSearchResponseDto searchKakaoBooks(String query, int start) throws JsonProcessingException {
        // 화면 시작 위치를 카카오 API의 1부터 시작하는 페이지 번호로 변환함
        int page = ((start - MIN_START) / KAKAO_DISPLAY_COUNT) + 1;
        // 사용자 검색어로 카카오 도서 검색 API에서 최대 50권을 호출함
        ResponseEntity<String> response = requestKakaoBookSearch(query, page);
        // 본문이 없는 외부 응답은 정상 검색 결과로 해석하지 않음
        if (StringUtil.isEmpty(response.getBody())) {
            // 검색 실패 공통 처리로 전환할 응답 계약 오류를 발생시킴
            throw new JsonProcessingException("Kakao Books response body is empty") { };
        }

        // 카카오 원문 응답을 외부 API 전용 DTO로 역직렬화함
        KakaoBookJsonDto kakaoResponse = objectMapper.readValue(response.getBody(), KakaoBookJsonDto.class);
        // 외부 필드명이 화면 응답 필드명을 바꾸지 않도록 화면 DTO 목록으로 변환함
        List<BookJsonDto.BookDto> bookList = getKakaoBookList(kakaoResponse.getDocuments());
        // 카카오 메타정보가 없거나 최대 페이지이면 추가 호출을 만들지 않음
        boolean end = StringUtil.isEmpty(kakaoResponse.getMeta()) || kakaoResponse.getMeta().isEnd()
                || getPage(start, KAKAO_DISPLAY_COUNT) == MAX_PAGE_COUNT;
        // 마지막 페이지가 아닐 때만 다음 카카오 시작 위치를 계산함
        Integer nextStart = end ? null : start + KAKAO_DISPLAY_COUNT;
        // 카카오 결과를 공급자와 무관한 화면 페이지 계약으로 반환함
        return new BookSearchResponseDto(bookList, end, nextStart);
    }

    /**
     * Google Books API 응답을 화면 페이지 계약으로 변환함
     *
     * @author HanWon.Jang
     * @param query Google Books API에 전달할 검색어
     * @param start 화면 검색 시작 위치
     * @return 영어 도서 검색 화면 응답
     * @throws JsonProcessingException Google Books 응답 JSON 변환에 실패한 경우 발생
     */
    private BookSearchResponseDto searchGoogleBooks(String query, int start) throws JsonProcessingException {
        // API 키가 없는 환경에서는 인증 없는 요청을 보내지 않고 안전하게 실패함
        if (StringUtil.isEmpty(googleBooksApiKey)) {
            // 설정 누락을 외부 응답 계약 오류와 같은 검색 실패 경로로 전달함
            throw new JsonProcessingException("Google Books API key is not configured") { };
        }

        // 사용자 검색어와 0부터 시작하는 시작 인덱스로 Google Books를 호출함
        ResponseEntity<String> response = requestGoogleBookSearch(query, start - MIN_START);
        // 본문이 없는 외부 응답은 정상 검색 결과로 해석하지 않음
        if (StringUtil.isEmpty(response.getBody())) {
            // 검색 실패 공통 처리로 전환할 응답 계약 오류를 발생시킴
            throw new JsonProcessingException("Google Books response body is empty") { };
        }

        // Google Books 원문 응답을 외부 API 전용 DTO로 역직렬화함
        GoogleBooksJsonDto googleResponse = objectMapper.readValue(response.getBody(), GoogleBooksJsonDto.class);
        // ISBN이 있어 저장 가능한 영어 도서만 기존 화면 DTO 목록으로 변환함
        List<BookJsonDto.BookDto> bookList = getGoogleBookList(googleResponse.getItems());
        // 전체 결과의 끝에 도달했거나 최대 페이지이면 추가 호출을 만들지 않음
        boolean end = start - MIN_START + GOOGLE_DISPLAY_COUNT >= googleResponse.getTotalItems()
                || getPage(start, GOOGLE_DISPLAY_COUNT) == MAX_PAGE_COUNT
                || StringUtil.isEmpty(googleResponse.getItems());
        // 마지막 페이지가 아닐 때만 다음 Google Books 시작 위치를 계산함
        Integer nextStart = end ? null : start + GOOGLE_DISPLAY_COUNT;
        // Google Books 결과를 공급자와 무관한 화면 페이지 계약으로 반환함
        return new BookSearchResponseDto(bookList, end, nextStart);
    }

    /**
     * 검색어와 페이지 번호로 카카오 도서 검색 API를 호출함
     *
     * @author SeungHyeon.Kang
     * @param query 카카오 도서 API에 전달할 검색어
     * @param page 카카오 도서 검색 페이지 번호
     * @return 카카오 도서 검색 API의 HTTP 응답
     */
    private ResponseEntity<String> requestKakaoBookSearch(String query, int page) {
        // 카카오 인증 헤더를 담을 객체를 생성함
        HttpHeaders headers = new HttpHeaders();
        // 서버 전용 REST API 키를 카카오 인증 형식으로 설정함
        headers.set(HttpHeaders.AUTHORIZATION, KAKAO_AUTH_SCHEME + kakaoRestApiKey);
        // 검색어와 페이지 및 표시 건수를 안전하게 인코딩한 카카오 요청 URI를 생성함
        URI uri = UriComponentsBuilder
                .fromUriString(bookSearchUrl)
                .queryParam("query", query)
                .queryParam("size", KAKAO_DISPLAY_COUNT)
                .queryParam("page", page)
                .queryParam("sort", "accuracy")
                .build()
                .encode()
                .toUri();
        // 인증 헤더를 포함한 카카오 도서 검색 응답을 반환함
        return restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    /**
     * 검색어와 시작 인덱스로 Google Books API의 영어 도서를 호출함
     *
     * @author HanWon.Jang
     * @param query Google Books API에 전달할 검색어
     * @param startIndex 0부터 시작하는 Google Books 검색 결과 위치
     * @return Google Books API의 HTTP 응답
     */
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

    /**
     * 카카오 도서 원문 목록을 사용자 화면 응답 목록으로 변환함
     *
     * @author HanWon.Jang
     * @param kakaoBookList 카카오 도서 검색 API 원문 목록
     * @return 한국어 코드와 표지 대체 주소를 포함한 화면 도서 목록
     */
    private List<BookJsonDto.BookDto> getKakaoBookList(List<KakaoBookJsonDto.BookDto> kakaoBookList) {
        // 카카오 응답에 documents가 없으면 빈 검색 결과로 처리함
        if (StringUtil.isEmpty(kakaoBookList)) {
            // 화면에 전달할 빈 도서 목록을 반환함
            return List.of();
        }

        // 카카오 원문과 화면 계약을 분리하여 저장할 결과 목록을 생성함
        List<BookJsonDto.BookDto> bookList = new ArrayList<>();
        // 개별 카카오 도서를 화면 필드명과 값 형식으로 변환함
        for (KakaoBookJsonDto.BookDto kakaoBook : kakaoBookList) {
            // ISBN이 있는 카카오 도서만 저장 가능한 화면 결과 목록에 추가함
            BookJsonDto.BookDto bookDto = getKakaoBookDto(kakaoBook);
            if (!StringUtil.isEmpty(bookDto.getIsbn())) {
                bookList.add(bookDto);
            }
        }
        // 화면 계약으로 변환한 도서 목록을 반환함
        return bookList;
    }

    /**
     * 카카오 도서 원문 항목을 기존 사용자 화면 필드로 변환함
     *
     * @author HanWon.Jang
     * @param kakaoBook 카카오 도서 검색 API 원문 항목
     * @return 한국어 언어 코드와 표지 대체 주소를 포함한 화면 도서 정보
     */
    private BookJsonDto.BookDto getKakaoBookDto(KakaoBookJsonDto.BookDto kakaoBook) {
        // 화면 도서 필드를 명시적으로 설정할 응답 객체를 생성함
        BookJsonDto.BookDto bookDto = new BookJsonDto.BookDto();
        // 카카오 도서 제목을 기존 화면 필드에 설정함
        bookDto.setTitle(getSafeText(kakaoBook.getTitle()));
        // 카카오 저자 배열을 기존 구분 문자열로 변환하여 설정함
        bookDto.setAuthor(getAuthor(kakaoBook.getAuthors()));
        // 카카오 출판사를 기존 화면 필드에 설정함
        bookDto.setPublisher(getSafeText(kakaoBook.getPublisher()));
        // ISBN10과 ISBN13 중 기존 데이터와 호환되는 값을 설정함
        bookDto.setIsbn(getKakaoIsbn(kakaoBook.getIsbn()));
        // 같은 ISBN의 영어 도서 정보와 구분할 한국어 코드를 설정함
        bookDto.setLangCode(LANGUAGE_KOREAN);
        // 검증된 Daum 원본 표지를 기존 image 응답 필드에 설정함
        bookDto.setImage(BookCoverUrlUtil.getOriginalCoverUrl(kakaoBook.getThumbnail()));
        // 원본 표지 실패 시 사용할 공식 카카오 썸네일을 별도 필드에 설정함
        bookDto.setThumbnailImage(getSafeText(kakaoBook.getThumbnail()));
        // 카카오 도서 소개를 기존 화면 필드에 설정함
        bookDto.setDescription(getSafeText(kakaoBook.getContents()));
        // 카카오 출간일시를 기존 yyyyMMdd 형식으로 변환하여 설정함
        bookDto.setPubdate(getPublishedDate(kakaoBook.getDatetime()));
        // 외부 필드명과 분리된 사용자 화면 도서 정보를 반환함
        return bookDto;
    }

    /**
     * Google Books 원문 목록을 사용자 화면 응답 목록으로 변환함
     *
     * @author HanWon.Jang
     * @param googleBookList Google Books 원문 도서 목록
     * @return ISBN과 영어 언어 코드를 포함한 화면 도서 목록
     */
    private List<BookJsonDto.BookDto> getGoogleBookList(List<GoogleBooksJsonDto.VolumeDto> googleBookList) {
        // Google Books 응답에 items가 없으면 빈 검색 결과로 처리함
        if (StringUtil.isEmpty(googleBookList)) {
            // 화면에 전달할 빈 도서 목록을 반환함
            return List.of();
        }

        // Google Books 원문과 화면 계약을 분리하여 저장할 결과 목록을 생성함
        List<BookJsonDto.BookDto> bookList = new ArrayList<>();
        // 개별 Google Books 도서를 화면 필드명과 값 형식으로 변환함
        for (GoogleBooksJsonDto.VolumeDto googleBook : googleBookList) {
            // 상세 정보가 없는 항목은 화면과 저장에 필요한 값을 만들 수 없어 제외함
            if (StringUtil.isEmpty(googleBook) || StringUtil.isEmpty(googleBook.getVolumeInfo())) {
                continue;
            }

            // ISBN이 있는 Google Books 도서만 저장 가능한 화면 결과 목록에 추가함
            BookJsonDto.BookDto bookDto = getGoogleBookDto(googleBook.getVolumeInfo());
            if (!StringUtil.isEmpty(bookDto.getIsbn())) {
                bookList.add(bookDto);
            }
        }
        // 화면 계약으로 변환한 영어 도서 목록을 반환함
        return bookList;
    }

    /**
     * Google Books 도서 원문 항목을 기존 사용자 화면 필드로 변환함
     *
     * @author HanWon.Jang
     * @param volumeInfo Google Books 도서 상세 원문
     * @return 영어 언어 코드와 ISBN을 포함한 화면 도서 정보
     */
    private BookJsonDto.BookDto getGoogleBookDto(GoogleBooksJsonDto.VolumeInfoDto volumeInfo) {
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

    /**
     * 계정 설정이 영어 사용 상태인지 확인함
     *
     * @author HanWon.Jang
     * @param userSetting 로그인 회원의 저장 설정
     * @return 영어 사용 설정 여부
     */
    private boolean isEnglishSetting(UserSettingDto userSetting) {
        // 설정 행이 없거나 영어 사용이 명시되지 않은 기존 회원은 한국어 검색을 유지함
        return !StringUtil.isEmpty(userSetting) && Constant.COMM_YES.equals(userSetting.getEnglishYsno());
    }

    /** 공급자별 페이지 최대 조회 건수를 반환함 */
    private int getDisplayCount(String provider) {
        // Google Books는 공식 최대 40건을 사용하고 카카오는 기존 50건을 유지함
        return PROVIDER_GOOGLE.equals(provider) ? GOOGLE_DISPLAY_COUNT : KAKAO_DISPLAY_COUNT;
    }

    /** 공급자 페이지 크기에 맞는 검색 시작 위치인지 확인함 */
    private boolean isValidStart(int start, int displayCount) {
        // 1부터 시작하는 공급자별 페이지 경계와 최대 페이지 수를 함께 확인함
        return start >= MIN_START && (start - MIN_START) % displayCount == 0
                && getPage(start, displayCount) <= MAX_PAGE_COUNT;
    }

    /** 화면 시작 위치를 공급자 페이지 번호로 변환함 */
    private int getPage(int start, int displayCount) {
        // 첫 시작 위치를 1페이지로 맞춘 공급자 페이지 번호를 반환함
        return ((start - MIN_START) / displayCount) + 1;
    }

    /** 외부 저자 배열을 기존 화면의 구분 문자열로 변환함 */
    private String getAuthor(List<String> authors) {
        // 저자가 없는 도서도 화면에서 안전하게 렌더링할 수 있도록 빈 문자열을 적용함
        if (StringUtil.isEmpty(authors)) {
            // 비어 있는 저자명을 반환함
            return StringUtil.EMPTY;
        }
        // 기존 화면과 저장 로직이 사용하는 저자 구분 형식으로 반환함
        return String.join("^", authors);
    }

    /** 카카오 ISBN 문자열에서 ISBN13을 우선 선택함 */
    private String getKakaoIsbn(String isbn) {
        // ISBN이 없는 도서는 저장할 수 없도록 빈 문자열을 적용함
        if (StringUtil.isEmpty(isbn)) {
            // 비어 있는 ISBN을 반환함
            return StringUtil.EMPTY;
        }
        // ISBN10과 ISBN13을 개별 후보로 분리함
        String[] isbnValues = isbn.trim().split("\\s+");
        // 카카오 응답에서 마지막에 제공되는 ISBN13을 우선 반환함
        return isbnValues[isbnValues.length - 1];
    }

    /** Google Books 산업 식별자에서 ISBN13을 우선 선택함 */
    private String getGoogleIsbn(List<GoogleBooksJsonDto.IndustryIdentifierDto> identifiers) {
        // 산업 식별자가 없는 도서는 저장할 수 없도록 빈 문자열을 적용함
        if (StringUtil.isEmpty(identifiers)) {
            // 비어 있는 ISBN을 반환함
            return StringUtil.EMPTY;
        }
        // ISBN13이 존재하면 한국어 도서와 같은 대표 식별값으로 우선 사용함
        for (GoogleBooksJsonDto.IndustryIdentifierDto identifier : identifiers) {
            if (!StringUtil.isEmpty(identifier) && GOOGLE_ISBN_13.equals(identifier.getType())
                    && !StringUtil.isEmpty(identifier.getIdentifier())) {
                return identifier.getIdentifier();
            }
        }
        // ISBN13이 없는 도서는 ISBN10을 저장 식별값으로 사용함
        for (GoogleBooksJsonDto.IndustryIdentifierDto identifier : identifiers) {
            if (!StringUtil.isEmpty(identifier) && GOOGLE_ISBN_10.equals(identifier.getType())
                    && !StringUtil.isEmpty(identifier.getIdentifier())) {
                return identifier.getIdentifier();
            }
        }
        // 지원하는 ISBN 식별자가 없는 항목을 제외할 수 있도록 빈 문자열을 반환함
        return StringUtil.EMPTY;
    }

    /** Google Books 기본 표지 주소를 조회함 */
    private String getGoogleBookImage(GoogleBooksJsonDto.ImageLinksDto imageLinks) {
        // 표지 정보가 없으면 화면의 기본 표지가 사용되도록 빈 문자열을 반환함
        if (StringUtil.isEmpty(imageLinks)) {
            return StringUtil.EMPTY;
        }
        // 기본 표지가 없으면 소형 표지를 대표 표지로 사용함
        String image = StringUtil.isEmpty(imageLinks.getThumbnail())
                ? imageLinks.getSmallThumbnail() : imageLinks.getThumbnail();
        // HTTPS 화면에서 혼합 콘텐츠가 발생하지 않도록 주소 스킴을 정규화함
        return normalizeHttpsUrl(image);
    }

    /** Google Books 소형 대체 표지 주소를 조회함 */
    private String getGoogleSmallThumbnail(GoogleBooksJsonDto.ImageLinksDto imageLinks) {
        // 표지 정보가 없으면 대체 표지 주소도 빈 문자열로 반환함
        return StringUtil.isEmpty(imageLinks)
                ? StringUtil.EMPTY : normalizeHttpsUrl(imageLinks.getSmallThumbnail());
    }

    /** 외부 표지 주소를 HTTPS로 정규화함 */
    private String normalizeHttpsUrl(String imageUrl) {
        // 표지 주소가 없으면 화면 기본 이미지가 사용되도록 빈 문자열을 반환함
        if (StringUtil.isEmpty(imageUrl)) {
            return StringUtil.EMPTY;
        }
        // Google Books가 HTTP 주소를 반환해도 HTTPS 화면에서 불러올 수 있도록 변환함
        return imageUrl.startsWith("http://") ? "https://" + imageUrl.substring(7) : imageUrl;
    }

    /** 외부 출간일을 화면의 숫자 날짜 형식으로 변환함 */
    private String getPublishedDate(String publishedDate) {
        // 출간일이 없는 도서도 화면에서 안전하게 렌더링할 수 있도록 빈 문자열을 적용함
        if (StringUtil.isEmpty(publishedDate)) {
            // 비어 있는 출간일을 반환함
            return StringUtil.EMPTY;
        }
        // 카카오 일시와 Google Books 부분 날짜에서 날짜 부분만 분리함
        int dateEndIndex = Math.min(publishedDate.length(), 10);
        // 기존 화면 날짜 변환과 호환되도록 숫자만 남긴 값을 반환함
        return publishedDate.substring(0, dateEndIndex).replaceAll("[^0-9]", StringUtil.EMPTY);
    }

    /** 외부 API 문자열의 null을 기존 화면 계약의 빈 문자열로 보정함 */
    private String getSafeText(String text) {
        // null 외부 문자열은 화면에서 직접 참조할 수 있도록 빈 문자열로 반환함
        return StringUtil.isEmpty(text) ? StringUtil.EMPTY : text;
    }
}
