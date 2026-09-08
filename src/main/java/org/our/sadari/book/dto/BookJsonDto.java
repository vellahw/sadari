package org.our.sadari.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * fileName       : BookJsonDto
 * author         : SeungHyeon.Kang
 * date           : 2026-07-17
 * description    : 도서 검색 결과를 사용자 화면 계약으로 전달함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-07-17        SeungHyeon.Kang    최초 생성
 * 2026-07-28        SeungHyeon.Kang    DTO 문서화 규칙 정비
 * 2026-07-31        SeungHyeon.Kang    카카오 도서 검색 응답 형식 적용
 * 2026-08-02        SeungHyeon.Kang    외부 응답 DTO와 화면 응답 DTO 분리
 * 2026-09-08        HanWon.Jang        검색 결과 언어 코드 추가
 */
@Schema(description = "도서 검색 화면 응답 DTO", hidden = true)
public class BookJsonDto {

    /**
     * 도서 검색 화면에 전달할 개별 도서 정보를 정의함
     *
     * @author SeungHyeon.Kang
     */
    // 외부 도서 검색 API에서 조회된 개별 도서 정보
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "외부 도서 검색 결과 항목 DTO", hidden = true)
    public static class BookDto {

        // 도서 제목
        private String title;

        // 도서를 집필한 저자명
        private String author;

        // 도서를 발행한 출판사명
        private String publisher;

        // 도서를 식별하는 ISBN 값
        private String isbn;

        // 도서 정보와 ISBN 중복 저장을 구분할 언어 코드
        private String langCode;

        // 카카오 썸네일에서 추출한 도서 표지 원본 이미지 URL
        private String image;

        // 원본 표지 로드에 실패할 때 사용할 공급자 썸네일 URL
        private String thumbnailImage;

        // 도서의 주요 내용을 요약한 설명
        private String description;

        // yyyyMMdd 형식의 도서 출간일
        private String pubdate;

    }
}
