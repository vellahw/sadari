package org.our.sadari.feed.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import org.our.sadari.global.common.constant.Constant;
import org.our.sadari.global.file.util.FileUrlUtil;

/**
 * fileName       : FeedDto
 * author         : SeungHyeon.Kang
 * date           : 2026-08-25
 * description    : 본인과 팔로잉 피드 항목 및 조회 조건을 전달함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-08-25        SeungHyeon.Kang         최초 생성
 * 2026-08-26        SeungHyeon.Kang         화면용 이미지 경로·주석 정비
 * 2026-08-27        SeungHyeon.Kang         알림 대상 단건 조회 조건 확장
 * 2026-08-28        HanWon.Jang             본인 피드 식별값 추가
 * 2026-09-07        HanWon.Jang             독후감 번역 표시 정보 추가
 */
@Data
@Schema(description = "본인과 팔로잉 피드 항목 DTO")
public class FeedDto {

    @Schema(description = "로그인 사용자 번호", hidden = true)
    private Long loginUserNumb;

    @Schema(description = "피드 대상 유형", allowableValues = {"REPORT", "PROFILE_IMAGE", "BACKGROUND_IMAGE"})
    private String tagtType;

    @Schema(description = "피드 대상 번호")
    private Long tagtNumb;

    @Schema(description = "피드 작성자 사용자 번호")
    private Long userNumb;

    @Schema(description = "피드 작성자 닉네임")
    private String userNick;

    @Schema(description = "로그인 사용자 본인 작성 여부", allowableValues = {"Y", "N"})
    private String meYsno;

    @Schema(description = "피드 작성자 프로필 이미지 경로")
    private String porfPath;

    @Schema(description = "활동 발생 일시")
    private LocalDateTime activityDate;

    @Schema(description = "독후감 번호")
    private Long reptNumb;

    @Schema(description = "독후감 작성 언어 코드", example = "ko", allowableValues = {"ko", "en"})
    private String langCode;

    @JsonIgnore
    @Schema(hidden = true)
    private String trnsCacheYsno;

    @Schema(description = "현재 표시 언어 번역 보기 가능 여부", example = "Y", allowableValues = {"Y", "N"})
    private String trnsAvaiYsno;

    @Schema(description = "독서 상태 코드", allowableValues = {"READ", "DONE", "STOP"})
    private String reptStat;

    @Schema(description = "독서 상태명")
    private String reptStatName;

    @Schema(description = "독후감 평점")
    private String reptGrde;

    @Schema(description = "독후감 내용")
    private String reptCntn;

    @Schema(description = "도서 번호")
    private Long bookNumb;

    @Schema(description = "도서 제목")
    private String bookTitl;

    @Schema(description = "도서 저자")
    private String bookAthr;

    @Schema(description = "도서 표지 이미지 URL")
    private String bookCvim;

    @Schema(description = "프로필 또는 배경 이미지 경로")
    private String contentImagePath;

    /**
     * 피드 배경사진 카드에서 사용할 축소 이미지 경로를 반환함
     *
     * @author SeungHyeon.Kang
     * @return 배경사진이면 화면용 파생본 경로, 다른 유형이면 원본 경로
     */
    @JsonProperty(value = "contentImageDisplayPath", access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "일반 화면용 피드 이미지 경로", accessMode = Schema.AccessMode.READ_ONLY)
    public String getContentDisplayPath() {
        // 프로필 사진 피드에는 배경사진 전용 파생 URL을 적용하지 않음
        if (!Constant.FILE_TYPE_BACKGROUND.equals(tagtType)) {
            // 기존 프로필 사진 표시 경로를 유지함
            return contentImagePath;
        }

        // 배경사진 피드에는 긴 변 1600px 파생본 URL을 제공함
        return FileUrlUtil.getBgDisplayPath(contentImagePath);
    }

    @Schema(description = "좋아요 수")
    private Long likeCnt;

    @Schema(description = "로그인 사용자 좋아요 여부", allowableValues = {"Y", "N"})
    private String likeYsno;

    @Schema(description = "댓글 수")
    private Long replCnt;

    @Schema(description = "조회 시작 위치", hidden = true)
    private Integer pageOffset;

    @Schema(description = "다음 페이지 판정을 포함한 조회 건수", hidden = true)
    private Integer pageLimit;
}
