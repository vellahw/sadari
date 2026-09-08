package org.our.sadari.report.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.our.sadari.book.dto.BookDto;

/**
 * fileName       : ReportDto
 * author         : SeungHyeon.Kang
 * date           : 2026-07-17
 * description    : 독후감과 독서 목표 요청과 응답 데이터를 전달함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-07-17        SeungHyeon.Kang    최초 생성
 * 2026-07-28        SeungHyeon.Kang    DTO 문서화 규칙 정비
 * 2026-07-30        SeungHyeon.Kang    독후감 별점 0.5점 단위 설명 추가
 * 2026-08-14        SeungHyeon.Kang    공개 독후감 작성자 팔로우 여부 응답 추가
 * 2026-08-15        SeungHyeon.Kang    공개 목록 조회 조건 추가
 * 2026-08-20        SeungHyeon.Kang    책장 색상 기본값 검증 순서 정비
 * 2026-08-21        SeungHyeon.Kang    독후감별 알림 설정 응답 추가
 * 2026-08-31        HanWon.Jang        모임 독서 기간 중 시작일 수정 잠금 응답 추가
 * 2026-09-07        HanWon.Jang        독후감 번역 표시 정보 추가
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "독후감과 연결 도서 정보를 함께 전달하는 DTO")
public class ReportDto extends BookDto {

    @Schema(description = "작성자 사용자 번호", example = "31")
    private Long userNumb;

    @Schema(description = "독후감 번호", example = "1")
    private Long reptNumb;

    @JsonIgnore
    @Schema(hidden = true)
    private String trnsCacheYsno;

    @Schema(description = "현재 표시 언어 번역 보기 가능 여부", example = "Y", allowableValues = {"Y", "N"})
    private String trnsAvaiYsno;

    @Schema(description = "독서 상태 코드", example = "DONE", allowableValues = {"READ", "DONE", "STOP"})
    @NotBlank
    private String reptStat;

    @Schema(description = "독서 상태명", example = "다 읽었어요")
    private String reptStatName;

    @Schema(description = "독서 시작일", example = "2026-07-01")
    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}")
    private String reptStdt;

    @Schema(description = "모임 독서 기간 중 독서 시작일 수정 잠금 여부", example = "true")
    private Boolean reptStdtLocked;

    @Schema(description = "독서 종료일", example = "2026-07-23")
    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}")
    private String reptEndt;

    @Schema(description = "0점부터 5점까지 0.5점 단위의 독후감 별점", example = "4.5")
    private String reptGrde;

    @Schema(description = "책장 색상 코드", example = "BLUE")
    private String reptColr;

    @Schema(description = "책장 색상명 또는 색상값")
    private String reptColrName;

    @Schema(description = "공개 여부", example = "Y", allowableValues = {"Y", "N"})
    private String pubcYsno;

    @Schema(description = "공개 여부명", example = "공개")
    private String pubcYsnoName;

    @Schema(description = "독후감 좋아요 알림 여부", example = "Y", allowableValues = {"Y", "N"})
    private String likeAlimYsno;

    @Schema(description = "독후감 댓글과 답글 알림 여부", example = "Y", allowableValues = {"Y", "N"})
    private String replyAlimYsno;

    @Schema(description = "독후감 본문", example = "인물의 선택이 끝까지 긴장감을 유지했다.")
    @Size(max = 4000)
    private String reptCntn;

    @Schema(description = "독후감 작성자 닉네임", example = "reader31")
    private String userNick;

    @Schema(description = "독후감 작성자 프로필 이미지 경로", example = "/uploads/profile/sample.jpg")
    private String porfPath;

    @Schema(description = "독후감이 받은 좋아요 수", example = "12")
    private Long likeCnt;

    @Schema(description = "로그인 사용자의 좋아요 여부", example = "Y", allowableValues = {"Y", "N"})
    private String likeYsno;

    @Schema(description = "로그인 사용자의 독후감 작성자 팔로우 여부", example = "Y", allowableValues = {"Y", "N"})
    private String followYsno;

    @Schema(description = "독후감이 받은 댓글 수", example = "12")
    private Long replCnt;

    @Schema(description = "조회 기준일의 독서 기록 존재 여부", example = "Y", allowableValues = {"Y", "N"})
    private String readingYn;

    @Schema(description = "책 제목 또는 작가명 검색어", example = "히가시노 게이고")
    private String bookKeyword;

    @Schema(description = "독후감 목록 정렬 유형", example = "RELATION_DESC"
          , allowableValues = {"RELATION_DESC", "LATEST_DESC", "GRADE_DESC", "LIKE_DESC", "END_DATE_DESC", "START_DATE_DESC"})
    private String sortType;

    @Schema(description = "상세 조회 시 계산된 동시 수정 충돌 검사용 원본 해시")
    private String editVersion;

    @Schema(description = "목록 조회 시작 위치", example = "0", hidden = true)
    private Integer pageOffset;

    @Schema(description = "다음 페이지 판정을 포함한 조회 건수", example = "13", hidden = true)
    private Integer pageLimit;
}
