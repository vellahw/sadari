package org.our.sadari.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * fileName       : BookDto
 * author         : SeungHyeon.Kang
 * date           : 2026-07-17
 * description    : 도서 요청과 응답 데이터를 전달함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-07-17        SeungHyeon.Kang    최초 생성
 * 2026-07-28        SeungHyeon.Kang    DTO 문서화 규칙 정비
 * 2026-09-08        HanWon.Jang        도서 정보 언어 코드 추가
 * 2026-09-11        HanWon.Jang        빈 언어 코드 기본값 처리 허용
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "도서 정보 DTO")
public class BookDto {

    @Schema(description = "도서 번호", example = "1")
    private Long bookNumb;

    @Schema(description = "도서 제목", example = "용의자 X의 헌신")
    @Size(max = 500)
    private String bookTitl;

    @Schema(description = "저자명", example = "히가시노 게이고")
    @Size(max = 500)
    private String bookAthr;

    @Schema(description = "출판사", example = "재인")
    @Size(max = 500)
    private String bookPubl;

    @Schema(description = "ISBN", example = "9788990982704")
    @Size(max = 100)
    private String bookIsbn;

    @Schema(description = "도서 정보 언어 코드", example = "en", allowableValues = {"ko", "en"})
    @Pattern(regexp = "^(?:ko|en)?$")
    private String langCode;

    @Schema(description = "도서 표지 이미지 URL")
    @Size(max = 1000)
    private String bookCvim;

    @Schema(description = "도서 설명")
    @Size(max = 4000)
    private String bookDesc;

    @Schema(description = "출간일", example = "2006-08-11")
    private String publDate;

    @Schema(description = "도서 평균 평점", example = "4.5")
    private BigDecimal bookAvgGrde;

    @Schema(description = "도서 검색 API locale 값")
    private String locale;
}
