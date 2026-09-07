package org.our.sadari.report.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * fileName       : ReportTranslationDto
 * author         : HanWon.Jang
 * date           : 2026-09-07
 * description    : 독후감 원문과 번역 캐시 및 번역 응답 데이터를 전달함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-09-07        HanWon.Jang        최초 생성
 */
@Data
@Schema(description = "독후감 번역 응답 DTO")
public class ReportTranslationDto {

    @JsonIgnore
    @Schema(hidden = true)
    private Long userNumb;

    @Schema(description = "독후감 번호", example = "1")
    private Long reptNumb;

    @JsonIgnore
    @Schema(hidden = true)
    private String sourceLangCode;

    @Schema(description = "번역 대상 언어 코드", example = "en", allowableValues = {"ko", "en"})
    private String langCode;

    @JsonIgnore
    @Schema(hidden = true)
    private String sourceContent;

    @JsonIgnore
    @Schema(hidden = true)
    private String origHash;

    @Schema(description = "번역된 독후감 내용")
    private String trnsCntn;

    @Schema(description = "기존 번역 캐시 사용 여부", example = "true")
    private boolean cached;
}
