package org.our.sadari.alim.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/**
 * fileName       : AlimDto
 * author         : SeungHyeon.Kang
 * date           : 2026-07-24
 * description    : 알림 요청과 응답 데이터를 전달함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-07-24        SeungHyeon.Kang    최초 생성
 * 2026-07-28        SeungHyeon.Kang    DTO 문서화 규칙 정비
 * 2026-08-04        SeungHyeon.Kang       외부 알림 발송 요청 DTO 제거
 * 2026-08-12        SeungHyeon.Kang    알림 아이콘 조인 응답 필드 추가
 * 2026-08-14        SeungHyeon.Kang    알림 목록 10개 단위 조회 설명 반영
 * 2026-08-27        SeungHyeon.Kang    동적 알림 이동 대상 필드와 응답 DTO 추가
 * 2026-09-10        HanWon.Jang        채팅 열람과 알림 읽음 동기화
 */
@Schema(description = "알림 API 요청과 응답 DTO 컨테이너", hidden = true)
public class AlimDto {

    /**
     * TB_ALTEMP에서 알림 템플릿을 조회할 때 사용하는 DTO임
     * 알림 상황과 템플릿 코드가 복합 PK이므로 두 값을 항상 함께 넘겨 템플릿을 특정함
     *
     * @author SeungHyeon.Kang
     */
    // 알림 상황과 템플릿 코드로 조회한 알림 템플릿
    @Data
    @Schema(description = "알림 템플릿 조회 DTO")
    public static class AlimTempDto {

        @JsonIgnore
        @Schema(hidden = true)
        private Long userNumb;

        @Schema(description = "알림 상황", example = "LIKE")
        private String alimSitu;

        @Schema(description = "알림 템플릿 코드", example = "LIKE_REPORT")
        private String tempCode;

        @Schema(description = "템플릿명")
        private String tempTitl;

        @Schema(description = "알림 제목")
        private String alimTitl;

        @Schema(description = "알림 내용")
        private String tempCont;

        @Schema(description = "사용 여부", example = "Y", allowableValues = {"Y", "N"})
        private String useeYsno;
    }

    /**
     * TB_ALIMXX에 실제 사용자 알림을 저장하고 조회할 때 사용하는 DTO임
     * ALIM_NUMB는 사용자별 순번이므로 발송 시점에 Mapper에서 해당 사용자의 다음 번호를 계산함
     *
     * @author SeungHyeon.Kang
     */
    // 사용자에게 발송되어 알림센터에 표시되는 알림
    @Data
    @Schema(description = "사용자 알림 DTO")
    public static class AlimItemDto {

        @Schema(description = "수신 사용자 번호", example = "31")
        private Long userNumb;

        @Schema(description = "사용자별 알림 순번", example = "1")
        private Long alimNumb;

        @Schema(description = "알림 상황", example = "LIKE")
        private String alimSitu;

        @Schema(description = "알림 템플릿 코드", example = "LIKE_REPORT")
        private String tempCode;

        @Schema(description = "알림 제목")
        private String alimTitl;

        @Schema(description = "알림 내용")
        private String alimCont;

        @Schema(description = "이동 URL")
        private String linkUrlx;

        @Schema(description = "알림 이동 대상 유형", example = "REPORT", hidden = true)
        @JsonIgnore
        private String tagtType;

        @Schema(description = "알림 이동 대상 번호", example = "157", hidden = true)
        @JsonIgnore
        private Long tagtNumb;

        @Schema(description = "알림에서 강조할 댓글 번호", example = "8", hidden = true)
        @JsonIgnore
        private Long replNumb;

        @Schema(description = "알림 원본 채팅 번호", hidden = true)
        @JsonIgnore
        private Long chatNumb;

        @Schema(description = "읽음 여부", example = "N", allowableValues = {"Y", "N"})
        private String readYsno;

        @Schema(description = "읽은 일시")
        private String readDate;

        @Schema(description = "발송 일시")
        private String sendDate;

        @Schema(description = "삭제 여부", example = "N", allowableValues = {"Y", "N"})
        private String deltYsno;


        @Schema(description = "알림 아이콘 MIME 유형", example = "image/svg+xml")
        private String alimIconMimeType;

        @Schema(description = "Base64로 인코딩되는 알림 아이콘 바이너리")
        private byte[] alimIconData;
    }

    /**
     * 알림 클릭 시점의 소유자와 공개 및 팔로우 상태를 조회해 최종 이동 주소를 계산할 때 사용함
     * 외부 응답에는 계산이 끝난 내부 상대 경로만 노출함
     *
     * @author SeungHyeon.Kang
     */
    // 알림번호로 조회한 현재 콘텐츠 접근 상태와 최종 이동 주소
    @Data
    @Schema(description = "알림 이동 대상 DTO")
    public static class AlimTargetDto {

        @Schema(description = "알림 수신 사용자 번호", example = "31", hidden = true)
        @JsonIgnore
        private Long userNumb;

        @Schema(description = "사용자별 알림 번호", example = "1", hidden = true)
        @JsonIgnore
        private Long alimNumb;

        @Schema(description = "알림 템플릿 코드", example = "LIKE_REPORT", hidden = true)
        @JsonIgnore
        private String tempCode;

        @Schema(description = "알림 이동 대상 유형", example = "REPORT", hidden = true)
        @JsonIgnore
        private String tagtType;

        @Schema(description = "알림 이동 대상 번호", example = "157", hidden = true)
        @JsonIgnore
        private Long tagtNumb;

        @Schema(description = "알림에서 강조할 댓글 번호", example = "8", hidden = true)
        @JsonIgnore
        private Long replNumb;

        @Schema(description = "이동 대상 소유 사용자 번호", example = "32", hidden = true)
        @JsonIgnore
        private Long targetUserNumb;

        @Schema(description = "이동 대상 소유 사용자 상태", example = "ACTIVE", hidden = true)
        @JsonIgnore
        private String targetUserStat;

        @Schema(description = "독후감 공개 여부", example = "Y", hidden = true)
        @JsonIgnore
        private String pubcYsno;

        @Schema(description = "독후감 독서 상태", example = "DONE", hidden = true)
        @JsonIgnore
        private String reptStat;

        @Schema(description = "알림 수신자의 현재 팔로우 여부", example = "Y", hidden = true)
        @JsonIgnore
        private String followYsno;

        @Schema(description = "클릭 시점의 권한으로 계산한 이동 URL", example = "/feed?tagtType=REPORT&tagtNumb=157")
        private String linkUrlx;
    }

    /**
     * 알림 목록을 10개 단위로 끊어 조회하기 위한 요청 DTO임
     * 화면에서 스크롤로 다음 페이지를 요청할 때도 같은 DTO를 사용하며, 실제 조회 범위는 서비스에서 보정함
     *
     * @author SeungHyeon.Kang
     */
    // 로그인 사용자의 알림 목록 페이징 조회 조건
    @Data
    @Schema(description = "알림 목록 페이징 조회 DTO")
    public static class AlimListReqDto {

        @Schema(description = "사용자 번호", example = "31")
        private Long userNumb;

        @Schema(description = "페이지 번호", example = "1")
        private int page;

        @Schema(description = "페이지 크기", example = "20")
        private int pageSize;

        @Schema(description = "시작 행", example = "1")
        private int startRow;

        @Schema(description = "종료 행", example = "21")
        private int endRow;
    }

    /**
     * 알림 목록 응답 DTO임
     * 목록 조회 자체는 읽음 상태를 변경하지 않으며 삭제되지 않은 알림을 읽음 여부와 함께 전달함
     *
     * @author SeungHyeon.Kang
     */
    // 알림 목록과 다음 페이지 및 미읽음 건수
    @Data
    @Schema(description = "알림 목록 응답 DTO")
    public static class AlimListResDto {

        @Schema(description = "알림 목록")
        private List<AlimItemDto> list;

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        private boolean hasNext;

        @Schema(description = "다음 페이지 번호", example = "2")
        private int nextPage;

        @Schema(description = "미읽음 알림 수", example = "3")
        private int unreadCnt;
    }

    /**
     * 사용자가 클릭한 알림 한 건을 읽음 처리하기 위한 DTO임
     * USER_NUMB는 인증 정보로 서비스에서 설정하고 외부 요청은 사용자별 ALIM_NUMB만 전달함
     *
     * @author SeungHyeon.Kang
     */
    // 사용자가 클릭한 단일 알림의 읽음 처리 조건
    @Data
    @Schema(description = "알림 개별 읽음 처리 DTO")
    public static class AlimReadReqDto {

        @Schema(description = "사용자 번호", example = "31")
        private Long userNumb;

        @NotNull
        @Schema(description = "읽음 처리할 사용자별 알림 번호", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long alimNumb;
    }

    /**
     * 미읽음 알림 수만 내려줄 때 사용하는 응답 DTO임
     * 햄버거 메뉴에서는 목록 전체를 조회하지 않고 숫자만 표시해야 하므로 별도 DTO로 분리함
     *
     * @author SeungHyeon.Kang
     */
    // 로그인 사용자의 미읽음 알림 건수
    @Data
    @Schema(description = "미읽음 알림 수 DTO")
    public static class AlimUnreadCntDto {

        @Schema(description = "미읽음 알림 수", example = "3")
        private int unreadCnt;
    }

}
