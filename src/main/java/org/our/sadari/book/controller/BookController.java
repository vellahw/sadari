package org.our.sadari.book.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.our.sadari.book.dto.BookCoverColorRequestDto;
import org.our.sadari.book.service.BookCoverColorService;
import org.our.sadari.book.service.BookPopularService;
import org.our.sadari.book.service.BookSearchProtectionService;
import org.our.sadari.book.service.BookSearchService;
import org.our.sadari.global.common.result.ResultData;
import org.our.sadari.report.service.ReportService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * fileName       : BookController
 * author         : SeungHyeon.Kang
 * date           : 2026-07-17
 * description    : 도서 API를 제공함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-07-17        SeungHyeon.Kang    최초 생성
 * 2026-07-30        SeungHyeon.Kang    도서 표지 기반 책장 색상 자동 선택 API 추가
 * 2026-07-31        SeungHyeon.Kang    카카오 도서 검색 API 적용
 * 2026-08-01        Hanwon.Jang        읽는 중 독후감 평균 평점 제외 정책 추가
 * 2026-08-16        SeungHyeon.Kang    인기 도서·검색어 조회 API 추가
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/book")
@Tag(name = "도서", description = "인기 도서와 인기 검색어 및 도서 검색과 표지 색상 및 ISBN 기준 공개 평점 평균 조회 API")
public class BookController {

    // BookSearch 업무 처리 서비스
    private final BookSearchService bookSearchService;
    // 도서 표지 대표색 기반 책장 색상 선택 서비스
    private final BookCoverColorService bookCoverColorService;
    // 기간별 독후감 작성자 수 기준 인기 도서 조회 서비스
    private final BookPopularService bookPopularService;
    // Redis 도서 검색 인기 검색어 조회 서비스
    private final BookSearchProtectionService bookSearchProtectionService;
    // Report 업무 처리 서비스
    private final ReportService reportService;

    /**
     * 검색어와 검색 시작 위치를 사용하여 계정 언어에 맞는 외부 도서 목록을 검색함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 도서 검색을 요청한 로그인 회원 번호
     * @param query 계정 언어에 맞는 외부 도서 API에 전달할 검색어
     * @param start 기존 화면 계약에서 사용하는 검색 결과 시작 위치
     * @return 검색된 도서 목록
     */
    @GetMapping("/search")
    @Operation(summary = "도서 검색", description = "로그인 회원의 요청 횟수를 제한하고 한국어 설정은 카카오, 영어 설정은 Google Books에서 도서를 조회한다.")
    public ResultData searchBooks(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                , @Parameter(description = "도서 검색어", example = "히가시노 게이고") @RequestParam("query") String query
                                , @Parameter(description = "공급자 페이지의 검색 시작 위치", example = "1") @RequestParam(value = "start", defaultValue = "1") int start) {
        // 로그인 회원과 검색어 및 시작 위치로 계정 언어에 맞는 도서 목록을 조회함
        return bookSearchService.searchBooks(userNumb, query, start);
    }

    /**
     * 회원 상태와 독서 상태 및 공개 여부에 관계없이 선택 기간의 인기 도서를 조회함
     *
     * @author SeungHyeon.Kang
     * @param period 주간과 월간 및 연간 중 조회할 집계 기간
     * @return 독후감 고유 작성자 수 기준 상위 10권의 도서 목록
     */
    @GetMapping("/popular")
    @Operation(summary = "기간별 인기 도서 조회", description = "현재 주와 달 또는 연도의 TM_REPORT에 남아 있는 모든 독후감의 고유 작성자 수를 도서별로 집계해 상위 10권과 도서 평균 평점을 조회한다.")
    public ResultData getPopularBookList(@Parameter(description = "인기 도서 집계 기간", example = "monthly")
                                         @RequestParam(value = "period", defaultValue = "monthly") String period) {
        // 선택 기간의 독후감 고유 작성자 수 기준 인기 도서 목록을 조회함
        return bookPopularService.getPopularBookList(period);
    }

    /**
     * 최근 설정 기간의 고유 회원 검색 수를 기준으로 안전한 인기 검색어를 조회함
     *
     * @author SeungHyeon.Kang
     * @return 비속어와 개인정보형 문자열을 제외한 인기 검색어 목록
    */
    @GetMapping("/popular-search-keywords")
    @Operation(
            summary = "도서 인기 검색어 조회"
          , description = "최근 설정 기간의 결과가 있는 첫 페이지 검색을 고유 회원 기준으로 집계하고 "
            + "비속어와 개인정보형 검색어를 제외한 상위 검색어를 조회한다."
    )
    public ResultData getPopularKeywordList() {
        // 현재 비속어 사전과 최소 회원 수 정책을 통과한 최근 인기 검색어를 조회함
        return ResultData.success(bookSearchProtectionService.getPopularKeywordList());
    }

    /**
     * ISBN 기준 도서 평균 평점 조회함
     *
     * @author SeungHyeon.Kang
     * @return 처리 결과
     */
    @GetMapping("/ratingAverage/by-isbn")
    @Operation(summary = "ISBN 공개 평점 평균 조회", description = "공개 여부와 관계없이 해당 ISBN의 완료 또는 중단 독후감 평점 평균을 조회한다.")
    public ResultData getRatingAverageByIsbn(@Parameter(description = "평점 평균을 조회할 도서 ISBN", example = "9788972756194")@RequestParam("isbn") String isbn) {
        // ISBN 기준 도서 평균 평점 조회 결과를 반환함
        return reportService.getPublicRatingAvgByIsbn(isbn);
    }

    /**
     * 신뢰된 도서 검색 표지의 대표색과 가장 가까운 활성 BOOK_COLR 코드를 조회함
     *
     * @author SeungHyeon.Kang
     * @param requestDto 대표색을 분석할 도서 표지 URL
     * @return 표지 대표색과 가장 가까운 책장 색상 코드
     */
    @PostMapping("/cover-color")
    @Operation(summary = "도서 표지 기반 책장 색상 조회", description = "신뢰된 도서 검색 표지의 대표색을 분석해 활성 BOOK_COLR 중 가장 가까운 색상 코드를 조회한다.")
    public ResultData getBookCoverColor(@Valid @RequestBody BookCoverColorRequestDto requestDto) {
        // 검증된 도서 검색 표지 URL로 자동 책장 색상을 조회함
        return bookCoverColorService.getBookCoverColor(requestDto);
    }
}
