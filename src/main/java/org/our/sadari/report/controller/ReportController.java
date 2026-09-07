package org.our.sadari.report.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.our.sadari.global.common.constant.Constant;
import org.our.sadari.global.common.result.ResultData;
import org.our.sadari.global.common.result.ResultEnum;
import org.our.sadari.report.dto.ReportAlimDto;
import org.our.sadari.report.dto.ReportDto;
import org.our.sadari.report.service.ReportService;
import org.our.sadari.report.service.ReportTranslationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * fileName       : ReportController
 * author         : SeungHyeon.Kang
 * date           : 2026-07-17
 * description    : 독후감과 독서 목표 API를 제공함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-07-17        SeungHyeon.Kang    최초 생성
 * 2026-08-01        SeungHyeon.Kang,SeungHyeon.Kang    최근 독후감·공개 API 추가
 * 2026-08-11        SeungHyeon.Kang    다중 탭 독후감 수정 충돌 409 응답 추가
 * 2026-08-14        SeungHyeon.Kang    공개 독후감 팔로우 작성자 우선 조회 API 반영
 * 2026-08-15        SeungHyeon.Kang    공개 독후감 조회·정렬 API
 * 2026-08-21        SeungHyeon.Kang    독후감별 알림 설정 API 추가
 * 2026-09-07        HanWon.Jang        공개 독후감 번역 API 추가
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/book")
@Tag(name = "독후감", description = "독후감 목록, 상세, 등록, 수정, 삭제, 공개 독후감, 좋아요, 알림 설정 API")
public class ReportController {

    // Report 업무 처리 서비스
    private final ReportService reportService;
    // 독후감 번역 업무 처리 서비스
    private final ReportTranslationService reportTranslationService;

    /**
     * 로그인 사용자의 독후감 목록을 검색어와 정렬 조건에 따라 조회함
     * bookKeyword는 책 제목과 작가명 검색에 사용하고, sortType이 없으면 종료일 내림차순을 기본값으로 사용함
     *
     * @author SeungHyeon.Kang
     * @param userNumb Spring Security에서 주입한 로그인 사용자 번호
     * @param bookKeyword 책 제목 또는 작가명 검색어
     * @param sortType 목록 정렬 유형
     * @param page 조회할 페이지 번호
     * @return 독후감 목록 조회 결과
     */
    @GetMapping("/getBookList")
    @Operation(summary = "내 독후감 목록 조회", description = "로그인 사용자의 독후감을 책 제목 또는 작가명 검색어와 정렬 조건으로 조회한다.")
    public ResultData getBookList(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                , @Parameter(description = "책 제목 또는 작가명 검색어", example = "용의자") @RequestParam(value = "bookKeyword", required = false) String bookKeyword
                                , @Parameter(description = "정렬 유형", example = Constant.SORT_END_DATE_DESC) @RequestParam(value = "sortType", defaultValue = Constant.SORT_END_DATE_DESC) String sortType
                                , @Parameter(description = "조회할 페이지 번호", example = "1") @RequestParam(value = "page", defaultValue = "1") int page) {
        // 로그인 사용자의 독후감 목록을 검색어와 정렬 조건에 따라 조회 결과를 반환함
        return reportService.getBookPage(userNumb, bookKeyword, sortType, page);
    }

    /**
     * 로그인 사용자가 작성한 독후감 상세 정보와 연결된 도서 정보를 함께 조회함
     * 화면에서는 같은 URL 안에서 독후감 영역과 도서 정보 영역을 전환해 사용함
     *
     * @author SeungHyeon.Kang
     * @param userNumb Spring Security에서 주입한 로그인 사용자 번호
     * @param bookNumb 상세 조회할 독후감 번호
     * @return 독후감 상세 조회 결과
     */
    @GetMapping("/getBookdetail/{bookNumb}")
    @Operation(summary = "내 독후감 상세 조회", description = "로그인 사용자가 작성한 독후감과 연결된 도서 정보를 함께 조회한다.")
    public ResultData getDetail(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                              , @Parameter(description = "독후감 번호", example = "1") @PathVariable("bookNumb") Long bookNumb) {
        // 로그인 사용자가 작성한 독후감 상세 정보와 연결된 도서 정보를 함께 조회 결과를 반환함
        return reportService.getDetail(userNumb, bookNumb);
    }

    /**
     * 로그인 사용자가 동일 ISBN으로 가장 최근에 작성한 독후감을 조회함
     * 도서 검색 화면에서 기존 독후감 수정과 추가 작성 중 하나를 선택할 수 있도록 독후감 번호를 제공함
     *
     * @author SeungHyeon.Kang
     * @param userNumb Spring Security에서 주입한 로그인 사용자 번호
     * @param isbn 최근 독후감을 조회할 도서 ISBN
     * @return 동일 ISBN의 최근 독후감 조회 결과
     */
    @GetMapping("/reports/by-isbn")
    @Operation(summary = "ISBN 기준 내 최근 독후감 조회", description = "로그인 사용자가 동일 ISBN으로 가장 최근에 작성한 독후감을 조회한다.")
    public ResultData getReportByIsbnDtl(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                       , @Parameter(description = "최근 독후감을 조회할 도서 ISBN", example = "9788972756194") @RequestParam("isbn") String isbn) {
        // 로그인 사용자가 동일 ISBN으로 가장 최근에 작성한 독후감 조회 결과를 반환함
        return reportService.getReportByIsbnDtl(userNumb, isbn);
    }

    /**
     * ISBN을 기준으로 다른 사용자가 공개한 독후감 목록을 조회함
     * 좋아요 여부와 좋아요 수 표시를 위해 로그인 사용자 번호를 함께 전달함
     *
     * @author SeungHyeon.Kang
     * @param userNumb Spring Security에서 주입한 로그인 사용자 번호
     * @param isbn 공개 독후감을 조회할 도서 ISBN
     * @param sortType 공개 독후감 정렬 코드
     * @param reptStat 공개 독후감 상태 필터
     * @param page 조회할 페이지 번호
     * @return 공개 독후감 목록 조회 결과
     */
    @GetMapping("/publicReports/by-isbn")
    @Operation(summary = "ISBN 공개 독후감 목록 조회", description = "해당 ISBN 도서의 공개 독후감을 관계순과 최신순 및 별점순 및 추천순으로 조회한다.")
    public ResultData getPublicReportsByIsbn(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                           , @Parameter(description = "공개 독후감을 조회할 도서 ISBN", example = "9788972756194") @RequestParam("isbn") String isbn
                                           , @Parameter(description = "공개 독후감 정렬 코드", example = "RELATION_DESC")
                                             @RequestParam(value = "sortType", defaultValue = Constant.SORT_RELATION_DESC) String sortType
                                           , @Parameter(description = "독서 상태 필터", example = "DONE")
                                             @RequestParam(value = "reptStat", defaultValue = "ALL") String reptStat
                                           , @Parameter(description = "조회할 페이지 번호", example = "1")
                                             @RequestParam(value = "page", defaultValue = "1") int page) {
        // ISBN을 기준으로 다른 사용자가 공개한 독후감 목록을 조회 결과를 반환함
        return reportService.getPublicReportsByIsbn(userNumb, isbn, sortType, reptStat, page);
    }

    /** 알림이 지정한 공개 독후감 한 건을 직접 조회함 */
    @GetMapping("/publicReports/{reptNumb}")
    @Operation(summary = "공개 독후감 직접 조회", description = "알림에서 지정한 공개 독후감과 연결 도서 정보를 조회한다.")
    public ResultData getPublicReportTarget(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                          , @PathVariable Long reptNumb) {
        return reportService.getPublicReportTarget(userNumb, reptNumb);
    }

    /**
     * 공개 독후감을 현재 사용자 표시 언어로 번역하고 결과를 재사용 캐시에 저장함
     *
     * @author HanWon.Jang
     * @param userNumb Spring Security에서 주입한 로그인 사용자 번호
     * @param reptNumb 번역할 공개 독후감 번호
     * @return 번역문과 캐시 사용 여부
     */
    @PostMapping("/translations/{reptNumb}")
    @Operation(summary = "공개 독후감 번역", description = "공개 독후감을 현재 표시 언어로 번역하고 같은 원문 번역을 재사용한다.")
    public ResultData setReportTranslation(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userNumb,
            @Parameter(description = "번역할 공개 독후감 번호", example = "1") @PathVariable Long reptNumb) {
        // 공개 범위와 월간 사용량을 서버에서 다시 확인한 번역 결과를 반환함
        return reportTranslationService.setReportTranslation(userNumb, reptNumb);
    }

    /**
     * 새 독후감과 필요 시 신규 도서 정보를 함께 등록함
     * DTO 검증은 Controller에서 1차 수행하고, 업무 규칙 검증은 Service에서 한 번 더 수행함
     *
     * @author SeungHyeon.Kang
     * @param userNumb Spring Security에서 주입한 로그인 사용자 번호
     * @param requestDto 등록할 독후감과 도서 정보
     * @return 등록된 독후감 번호
     */
    @PostMapping("/setReport")
    @Operation(summary = "독후감 등록", description = "도서 정보가 없으면 도서를 먼저 저장한 뒤 로그인 사용자의 독후감을 등록한다.")
    public ResultData createReport(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                 , @Valid @RequestBody ReportDto requestDto) {
        // 새 독후감과 필요 시 신규 도서 정보를 함께 등록 결과를 반환함
        return reportService.setReport(userNumb, requestDto);
    }

    /**
     * 기존 독후감 정보를 수정함
     * URL의 reptNumb를 기준으로 수정 대상을 확정하고, 본문 DTO에는 변경할 독후감 값을 담음
     *
     * @author SeungHyeon.Kang
     * @param userNumb Spring Security에서 주입한 로그인 사용자 번호
     * @param reptNumb 수정할 독후감 번호
     * @param request 수정할 독후감 정보
     * @param response 수정 충돌 HTTP 상태를 기록할 응답 객체
     * @return 수정된 독후감 번호
     */
    @PutMapping("/uptReport/{reptNumb}")
    @Operation(summary = "독후감 수정", description = "기존 독후감의 도서, 기간, 상태, 별점, 공개 여부, 본문을 수정한다.")
    public ResultData uptReport(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                              , @Parameter(description = "수정할 독후감 번호", example = "1") @PathVariable("reptNumb") Long reptNumb
                              , @Valid @RequestBody ReportDto request
                              , @Parameter(hidden = true) HttpServletResponse response) {
        // 원본 버전을 포함한 독후감 수정 결과를 조회함
        ResultData result = reportService.uptReport(userNumb, reptNumb, request);
        // 다른 탭이나 기기의 선행 수정이 확인되면 표준 충돌 상태로 응답함
        if (result.getCode() == ResultEnum.COMMON_EDIT_CONFLICT.getCode()) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
        }
        // 기존 독후감 정보를 수정 결과를 반환함
        return result;
    }

    /**
     * 로그인 사용자가 작성한 독후감의 좋아요 또는 댓글 알림 사용 여부를 변경함
     * 공개 여부와 관계없이 상세 화면에서 설정할 수 있도록 소유자 조건만 적용함
     *
     * @author SeungHyeon.Kang
     * @param userNumb Spring Security에서 주입한 로그인 사용자 번호
     * @param reptNumb 수정할 독후감 번호
     * @param alimType 변경할 알림 유형
     * @param request 변경할 알림 사용 여부
     * @return 변경된 알림 사용 여부
     */
    @PutMapping("/{reptNumb}/notification-settings/{alimType}")
    @Operation(summary = "독후감별 알림 설정 변경", description = "독후감 공개 여부와 관계없이 작성자가 좋아요 또는 댓글 알림 사용 여부를 변경한다.")
    public ResultData uptReportAlim(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                  , @Parameter(description = "수정할 독후감 번호", example = "1") @PathVariable Long reptNumb
                                  , @Parameter(description = "변경할 알림 유형", example = "like") @PathVariable String alimType
                                  , @Valid @RequestBody ReportAlimDto request) {
        // 로그인 사용자가 작성한 독후감의 유형별 알림 설정 변경 결과를 반환함
        return reportService.uptReportAlim(userNumb, reptNumb, alimType, request);
    }

    /**
     * 마이페이지의 현재 읽고 있는 책 목록에서 독서 상태와 별점 및 공개 여부를 빠르게 수정함
     * 본문과 시작일 등 전체 독후감 수정 화면에서 다루는 값은 변경하지 않도록 별도 API로 분리함
     *
     * @author SeungHyeon.Kang
     * @param userNumb Spring Security에서 주입한 로그인 사용자 번호
     * @param reptNumb 수정할 독후감 번호
     * @param request 수정할 독서 상태와 별점 및 공개 여부 정보
     * @return 수정 처리 결과
     */
    @PutMapping("/uptReport/status-grade/{reptNumb}")
    @Operation(summary = "독서 상태와 별점 및 공개 여부 빠른 수정", description = "마이페이지 팝업에서 독서 상태와 별점 및 공개 여부를 빠르게 수정한다.")
    public ResultData uptReptStatusGrade(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                                       , @Parameter(description = "수정할 독후감 번호", example = "1") @PathVariable("reptNumb") Long reptNumb
                                       , @RequestBody ReportDto request) {
        // 마이페이지의 현재 읽고 있는 책 목록에서 독서 상태와 별점 및 공개 여부를 빠르게 수정 결과를 반환함
        return reportService.uptReptStatusGrade(userNumb, reptNumb, request);
    }

    /**
     * 로그인 사용자가 작성한 독후감을 삭제함
     * Service에서 사용자 번호와 독후감 번호를 함께 조건으로 사용해 본인 데이터만 삭제되도록 함
     *
     * @author SeungHyeon.Kang
     * @param userNumb Spring Security에서 주입한 로그인 사용자 번호
     * @param reptNumb 삭제할 독후감 번호
     * @return 삭제 처리 결과
     */
    @DeleteMapping("/delReport/{reptNumb}")
    @Operation(summary = "독후감 삭제", description = "로그인 사용자가 작성한 독후감을 삭제한다.")
    public ResultData delReport(@Parameter(hidden = true) @AuthenticationPrincipal Long userNumb
                              , @Parameter(description = "삭제할 독후감 번호", example = "1") @PathVariable("reptNumb") Long reptNumb) {
        // 로그인 사용자가 작성한 독후감을 삭제 결과를 반환함
        return reportService.delReport(userNumb, reptNumb);
    }
}
