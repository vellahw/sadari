package org.our.sadari.book.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

/**
 * fileName       : GoogleBooksJsonDto
 * author         : HanWon.Jang
 * date           : 2026-09-08
 * description    : Google Books 도서 검색 원문 응답을 전달함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-09-08        HanWon.Jang        최초 생성
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleBooksJsonDto {

    // 검색 조건에 해당하는 전체 도서 수
    private int totalItems;

    // 현재 검색 페이지의 도서 목록
    private List<VolumeDto> items;

    /**
     * Google Books 개별 도서 항목을 전달함
     *
     * @author HanWon.Jang
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VolumeDto {

        // Google Books 내부 도서 식별값
        private String id;

        // 제목과 저자 및 ISBN을 포함한 도서 상세 정보
        private VolumeInfoDto volumeInfo;
    }

    /**
     * Google Books 도서 상세 정보를 전달함
     *
     * @author HanWon.Jang
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VolumeInfoDto {

        // 도서 제목
        private String title;

        // 도서 저자 목록
        private List<String> authors;

        // 도서 출판사
        private String publisher;

        // 연도 또는 연월 및 연월일 형식의 출간일
        private String publishedDate;

        // 도서 설명
        private String description;

        // ISBN10과 ISBN13을 포함한 산업 식별자 목록
        private List<IndustryIdentifierDto> industryIdentifiers;

        // 도서 표지 이미지 주소 묶음
        private ImageLinksDto imageLinks;

        // 도서 정보 언어 코드
        private String language;
    }

    /**
     * Google Books 산업 식별자를 전달함
     *
     * @author HanWon.Jang
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IndustryIdentifierDto {

        // ISBN10 또는 ISBN13 식별자 유형
        private String type;

        // 유형에 대응하는 도서 식별값
        private String identifier;
    }

    /**
     * Google Books 도서 표지 이미지 주소를 전달함
     *
     * @author HanWon.Jang
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageLinksDto {

        // 소형 화면 대체용 도서 표지 주소
        private String smallThumbnail;

        // 기본 도서 표지 주소
        private String thumbnail;
    }
}
