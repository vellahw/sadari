package org.our.sadari.readingClub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.our.sadari.book.dto.BookDto;
import org.our.sadari.global.common.dto.PageDto;
import org.our.sadari.report.dto.ReportDto;

/**
 * fileName       : ReadingClubDto
 * author         : SeungHyeon.Kang
 * date           : 2026-08-05
 * description    : 독서 모임 1차 기능의 요청과 응답 데이터를 전달함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-08-05        SeungHyeon.Kang    최초 생성
 * 2026-08-14        SeungHyeon.Kang,Hanwon.Jang    모임원·독서 등록 DTO 추가
 * 2026-08-15        Hanwon.Jang    현재 독서 목표 현황 응답 필드 추가
 * 2026-08-20        Hanwon.Jang        현재 독후감 편집·독서 관리 추가
 * 2026-08-22        HanWon.Jang        종료 결과·독후감 페이지 추가
 * 2026-08-23        HanWon.Jang        이전 독서 기록 페이지 추가
 * 2026-08-24        HanWon.Jang        모임원 퇴장 기능 추가
 * 2026-08-25        HanWon.Jang        다음 도서 추천·투표 DTO 추가
 * 2026-08-26        HanWon.Jang        다음 도서 투표 정책 DTO 추가
 * 2026-09-01        HanWon.Jang        공개 모임 이전 독서 기록 권한 추가
 * 2026-09-04        SeungHyeon.Kang    모임 채팅 읽음 수·강제 퇴장 이력 DTO 추가
 * 2026-09-10        HanWon.Jang        채팅 열람과 알림 읽음 동기화
 */
@Schema(description = "독서 모임 API DTO 컨테이너", hidden = true)
public final class ReadingClubDto {

    /** 다음 도서 추천 요청과 목록 항목을 전달함 */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @Schema(description = "다음 도서 추천 정보")
    public static class BookRecommendationDto extends BookDto {

        @Schema(description = "추천 번호")
        private Long recmNumb;

        @Schema(description = "추천 사용자 닉네임")
        private String userNick;

        @Schema(description = "로그인 사용자의 추천 여부")
        private String mineYsno;

        @Schema(description = "로그인 사용자의 투표 여부")
        private String voteYsno;

        @Schema(description = "현재 투표 주기의 후보별 득표 수")
        private Integer voteCnt;
    }

    /** 다음 도서 투표 대상을 전달함 */
    @Data
    @Schema(description = "다음 도서 투표 요청")
    public static class BookVoteReqDto {

        @NotNull
        @Schema(description = "추천 번호")
        private Long recmNumb;
    }

    /** 다음 도서 투표 화면의 후보와 정책 상태를 전달함 */
    @Data
    @Schema(description = "다음 도서 투표 화면 정보")
    public static class BookVotePageDto {

        @Schema(description = "현재 투표 후보 목록")
        private List<BookRecommendationDto> candidateList;

        @Schema(description = "yyyy-MM-dd 형식의 투표 마감일")
        private String voteDeadline;

        @Schema(description = "투표 마감일까지 남은 일수")
        private Integer dDay;

        @Schema(description = "도서 후보 등록 가능 여부")
        private boolean canRecommend;

        @Schema(description = "로그인 사용자의 후보 등록 여부")
        private boolean hasRecommended;

        @Schema(description = "로그인 사용자의 투표 완료 여부")
        private boolean hasVoted;
    }

    /** 다음 도서 투표 주기의 기준 회차를 전달함 */
    @Data
    @Schema(hidden = true)
    public static class BookVoteRuleDto {

        // 최신 독서 회차 등록 일시
        private LocalDateTime roundRegiDate;

        // 최신 독서 회차 목표 종료 일시
        private LocalDateTime goalEndt;

        // 현재 투표 주기 시작 일시
        private LocalDateTime cycleStdt;

        // yyyy-MM-dd 형식의 현재 투표 마감일
        private String voteDeadline;

        // 투표 마감일까지 남은 일수
        private Integer dDay;

        // 후보 등록과 삭제 가능 여부
        private boolean canRecommend;
    }

    /**
     * fileName       : ReadingCreateReqDto
     * author         : Hanwon.Jang
     * date           : 2026-08-14
     * description    : 모임 독서 회차와 멤버별 독후감 생성에 필요한 도서와 목표 기간을 전달함
     * ===========================================================
     * DATE              AUTHOR             NOTE
     * -----------------------------------------------------------
     * 2026-08-14        Hanwon.Jang    최초 생성
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @Schema(description = "모임 독서 등록 요청")
    public static class ReadingCreateReqDto extends BookDto {

        @NotBlank
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}")
        @Schema(description = "목표 독서 시작일", example = "2026-08-14")
        private String goalStdt;

        @NotBlank
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}")
        @Schema(description = "목표 독서 종료일", example = "2026-08-31")
        private String goalEndt;

        @NotBlank
        @Size(max = 64)
        @Schema(description = "중복 등록 방지 키")
        private String idemKeyx;

        @Schema(description = "생성된 모임 독서 회차 번호", hidden = true)
        private Long rondNumb;
    }

    /**
     * fileName       : ReadingUpdateReqDto
     * author         : Hanwon.Jang
     * date           : 2026-08-20
     * description    : 현재 모임 독서의 도서와 목표 기간 수정값을 전달함
     * ===========================================================
     * DATE              AUTHOR             NOTE
     * -----------------------------------------------------------
     * 2026-08-20        Hanwon.Jang        최초 생성
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @Schema(description = "모임 독서 수정 요청")
    public static class ReadingUpdateReqDto extends BookDto {

        @NotBlank
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}")
        @Schema(description = "목표 독서 시작일", example = "2026-08-14")
        private String goalStdt;

        @NotBlank
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}")
        @Schema(description = "목표 독서 종료일", example = "2026-08-31")
        private String goalEndt;
    }

    /**
     * fileName       : ReadingManageDto
     * author         : Hanwon.Jang
     * date           : 2026-08-20
     * description    : 모임 독서 수정 검증에 필요한 잠긴 회차 정보를 전달함
     * ===========================================================
     * DATE              AUTHOR             NOTE
     * -----------------------------------------------------------
     * 2026-08-20        Hanwon.Jang        최초 생성
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @Schema(description = "모임 독서 관리 정보")
    public static class ReadingManageDto extends BookDto {

        @Schema(description = "모임 번호")
        private Long clubNumb;

        @Schema(description = "모임별 회차 번호")
        private Long rondNumb;

        @Schema(description = "회차 상태")
        private String rondStat;

        @Schema(description = "목표 독서 시작일")
        private String goalStdt;

        @Schema(description = "목표 독서 종료일")
        private String goalEndt;
    }

    /**
     * fileName       : ClubCreateReqDto
     * author         : SeungHyeon.Kang
     * date           : 2026-08-05
     * description    : 독서 모임 생성 입력값을 전달함
     * ===========================================================
     * DATE              AUTHOR             NOTE
     * -----------------------------------------------------------
     * 2026-08-05        SeungHyeon.Kang    최초 생성
     * 2026-08-14        Hanwon.Jang        현재 독서 회차와 도서 요약 필드 추가
     */
    @Data
    @Schema(description = "독서 모임 생성 요청")
    public static class ClubCreateReqDto {

        @Schema(description = "생성된 모임 번호", hidden = true)
        private Long clubNumb;

        @NotBlank
        @Size(max = 100)
        @Schema(description = "모임명", example = "주말 소설 읽기")
        private String clubName;

        @NotBlank
        @Size(max = 2000)
        @Schema(description = "모임 소개")
        private String clubCntn;

        @NotBlank
        @Schema(description = "공개 범위", allowableValues = {"PUBLIC", "PRIVATE"})
        private String clubVisb;

        @NotBlank
        @Schema(description = "가입 방식", allowableValues = {"OPEN", "APPROVAL", "INVITE"})
        private String joinType;

        @NotNull
        @Min(2)
        @Max(100)
        @Schema(description = "모임 정원", example = "10")
        private Integer maxxMemb;

        @NotEmpty
        @Size(max = 3)
        @Schema(description = "관심분야 세부코드 1~3개")
        private List<@NotBlank String> categoryList;

        @Size(max = 5)
        @Schema(description = "승인형 가입 질문 1~5개")
        private List<@NotBlank @Size(max = 500) String> questionList;
    }

    /**
     * fileName       : ClubViewDto
     * author         : SeungHyeon.Kang
     * date           : 2026-08-05
     * description    : 독서 모임 목록과 상세 화면에 필요한 현재 상태를 전달함
     * ===========================================================
     * DATE              AUTHOR             NOTE
     * -----------------------------------------------------------
     * 2026-08-05        SeungHyeon.Kang    최초 생성
     * 2026-08-14        Hanwon.Jang        현재 독서 관련 추가
     * 2026-08-15        Hanwon.Jang        현재 독서 기간과 목표 달성 현황 추가
     * 2026-08-29        HanWon.Jang        현재 회차 독후감 현황 추가
     * 2026-09-04        SeungHyeon.Kang    관리자 모집 중지 상태 추가
     */
    @Data
    @Schema(description = "독서 모임 조회 항목")
    public static class ClubViewDto {

        @Schema(description = "모임 번호")
        private Long clubNumb;

        @Schema(description = "현재 모임장 사용자 번호")
        private Long ownrNumb;

        @Schema(description = "모임장 닉네임")
        private String ownrNick;

        @Schema(description = "모임명")
        private String clubName;

        @Schema(description = "모임 소개")
        private String clubCntn;

        @Schema(description = "공개 범위")
        private String clubVisb;

        @Schema(description = "가입 방식")
        private String joinType;

        @Schema(description = "모임 운영 상태")
        private String clubStat;

        @Schema(description = "신규 회원 모집 가능 여부", allowableValues = {"Y", "N"})
        private String rcrtYsno;

        @Schema(description = "모임 정원")
        private Integer maxxMemb;

        @Schema(description = "활성 모임원 수")
        private Integer memberCnt;

        @Schema(description = "유효한 초대 예약석 수")
        private Integer invitedCnt;

        @Schema(description = "로그인 사용자의 모임원 상태")
        private String membStat;

        @Schema(description = "로그인 사용자의 모임원 역할")
        private String membRole;

        @Schema(description = "로그인 사용자의 가입 신청 상태")
        private String joinStat;

        @Schema(description = "로그인 사용자의 관심분야 정확 일치 수")
        private Integer matchCnt;

        @Schema(description = "현재 예정 또는 진행 중인 모임별 회차 번호")
        private Long currentRondNumb;

        @Schema(description = "현재 모임 독서 회차 상태")
        private String currentRondStat;

        @Schema(description = "현재 독서 또는 다음 독서의 모임 내 순번")
        private Long readingOrdr;

        @Schema(description = "현재 독서 도서 제목")
        private String currentBookTitl;

        @Schema(description = "현재 독서 도서 번호")
        private Long currentBookNumb;

        @Schema(description = "현재 독서 도서 저자")
        private String currentBookAthr;

        @Schema(description = "현재 독서 도서 표지 이미지 URL")
        private String currentBookCvim;

        @Schema(description = "현재 독서 도서 출판사")
        private String currentBookPubl;

        @Schema(description = "현재 독서 도서 ISBN")
        private String currentBookIsbn;

        @Schema(description = "현재 독서 도서 설명")
        private String currentBookDesc;

        @Schema(description = "현재 독서 도서 출간일")
        private String currentPublDate;

        @Schema(description = "현재 독서 도서 변경 가능 여부")
        private Boolean currentBookChangeAllowed;

        @Schema(description = "현재 독서 공동 목표 시작 일시")
        private LocalDateTime currentGoalStdt;

        @Schema(description = "현재 독서 공동 목표 종료 일시")
        private LocalDateTime currentGoalEndt;

        @Schema(description = "로그인 사용자의 현재 독서 상태")
        private String currentReportStat;

        @Schema(description = "로그인 사용자의 현재 독서 독후감 번호")
        private Long currentReportNumb;

        @Schema(description = "현재 진행 회차의 활성 모임원 완료 독후감 수")
        private Integer currentReportCnt;

        @Schema(description = "현재 독서 목표를 달성한 활성 참여 인원 수")
        private Integer currentGoalAchvCnt;

        @Schema(description = "현재 독서에 참여 중인 활성 인원 수")
        private Integer currentGoalMembCnt;

        @Schema(description = "카테고리 코드와 이름 목록")
        private List<CategoryDto> categoryList;

        @Schema(description = "승인 가입 질문 목록")
        private List<String> questionList;

        @Schema(description = "모임 생성 일시")
        private LocalDateTime regiDate;
    }

    /**
     * fileName       : CategoryDto
     * author         : SeungHyeon.Kang
     * date           : 2026-08-05
     * description    : 모임 카테고리 세부코드와 화면 표시명을 전달함
     * ===========================================================
     * DATE              AUTHOR             NOTE
     * -----------------------------------------------------------
     * 2026-08-05        SeungHyeon.Kang    최초 생성
     */
    @Data
    @Schema(description = "모임 카테고리 항목")
    public static class CategoryDto {

        @Schema(description = "관심분야 세부코드")
        private String intrCode;

        @Schema(description = "관심분야 세부코드명")
        private String intrName;

        @Schema(description = "대분류명")
        private String intrCnam;

        @Schema(description = "모임 내 노출 순서")
        private Integer sortOrdr;

    }

    /**
     * fileName       : QuestionDto
     * author         : SeungHyeon.Kang
     * date           : 2026-08-05
     * description    : 모임당 한 행으로 저장한 승인 가입 질문을 전달함
     * ===========================================================
     * DATE              AUTHOR             NOTE
     * -----------------------------------------------------------
     * 2026-08-05        SeungHyeon.Kang    최초 생성
     */
    @Data
    @Schema(description = "모임 가입 질문", hidden = true)
    public static class QuestionDto {

        // 질문이 적용되는 모임 번호
        private Long clubNumb;
        // 첫 번째 가입 질문
        private String quesFirs;
        // 두 번째 가입 질문
        private String quesSeco;
        // 세 번째 가입 질문
        private String quesThir;
        // 네 번째 가입 질문
        private String quesFour;
        // 다섯 번째 가입 질문
        private String quesFift;
    }

    /**
     * fileName       : MemberDto
     * author         : SeungHyeon.Kang
     * date           : 2026-08-05
     * description    : 동일 모임과 사용자의 현재 회원 또는 초대 관계를 전달함
     * ===========================================================
     * DATE              AUTHOR             NOTE
     * -----------------------------------------------------------
     * 2026-08-05        SeungHyeon.Kang    최초 생성
     */
    @Data
    @Schema(description = "모임 회원 관계", hidden = true)
    public static class MemberDto {

        // 회원 관계가 귀속되는 모임 번호
        private Long clubNumb;
        // 모임 회원 또는 초대 대상 사용자 번호
        private Long userNumb;
        // 모임장 또는 일반 회원 역할
        private String membRole;
        // 활성 회원 또는 초대 대기 상태
        private String membStat;
        // 초대 예약석 만료 일시
        private LocalDateTime exprDate;
        // 탈퇴 후 재가입 차단 여부
        private String blocYsno;
    }

    /**
     * fileName       : MemberProfileDto
     * author         : SeungHyeon.Kang
     * date           : 2026-08-13
     * description    : 모임 상세 화면에 노출할 활성 모임원의 프로필 정보를 전달함
     * ===========================================================
     * DATE              AUTHOR             NOTE
     * -----------------------------------------------------------
     * 2026-08-13        SeungHyeon.Kang    최초 생성
     */
    @Data
    @Schema(description = "모임원 프로필")
    public static class MemberProfileDto {

        @Schema(description = "모임원 사용자 번호")
        private Long userNumb;

        @Schema(description = "모임원 닉네임")
        private String userNick;

        @Schema(description = "모임원 프로필 이미지 경로")
        private String porfPath;

        @Schema(description = "모임 내 역할", allowableValues = {"OWNER", "MEMBER"})
        private String membRole;
    }

    /** 모임원이 전송할 채팅 본문과 중복 방지 키를 전달함 */
    @Data
    @Schema(description = "모임 채팅 전송 요청")
    public static class ClubChatReqDto {

        @NotBlank
        @Size(max = 2000)
        @Schema(description = "채팅 본문")
        private String chatCntn;

        @NotBlank
        @Size(max = 64)
        @Schema(description = "클라이언트 중복 방지 키")
        private String clntUuid;
    }

    /** 모임원이 마지막으로 확인한 채팅 번호를 전달함 */
    @Data
    @Schema(description = "모임 채팅 읽음 처리 요청")
    public static class ClubChatReadReqDto {

        @Positive
        @Schema(description = "마지막으로 확인한 채팅 번호")
        private Long chatNumb;

        @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
        @Schema(description = "브라우저 채팅 화면별 UUID")
        private String viewId;

        @Schema(description = "현재 채팅 화면 표시 여부")
        private Boolean viewing;
    }

    /** 모임 채팅 목록 항목을 전달함 */
    @Data
    @Schema(description = "모임 채팅 항목")
    public static class ClubChatDto {

        @Schema(description = "채팅 번호")
        private Long chatNumb;

        @Schema(description = "모임 번호")
        private Long clubNumb;

        @Schema(description = "작성자 사용자 번호")
        private Long userNumb;

        @Schema(description = "작성자 닉네임")
        private String userNick;

        @Schema(description = "작성자 프로필 이미지 경로")
        private String porfPath;

        @Schema(description = "채팅 유형")
        private String chatType;

        @Schema(description = "채팅 본문")
        private String chatCntn;

        @Schema(description = "클라이언트 중복 방지 키")
        private String clntUuid;

        @Schema(description = "로그인 사용자의 작성 여부", allowableValues = {"Y", "N"})
        private String mineYsno;

        @Schema(description = "현재 활성 모임원 중 메시지를 읽지 않은 인원 수")
        private Integer unreadCnt;

        @Schema(description = "등록 일시")
        private LocalDateTime regiDate;
    }

    /** 모임장 강제 퇴장 사유를 전달함 */
    @Data
    @Schema(description = "모임원 강제 퇴장 요청")
    public static class MemberKickReqDto {

        @NotBlank
        @Size(max = 1000)
        @Schema(description = "강제 퇴장 사유")
        private String kickRson;
    }

    /**
     * fileName       : MemberExitHistoryDto
     * author         : HanWon.Jang
     * date           : 2026-08-24
     * description    : 퇴장한 모임원의 프로필과 재가입 제한 상태를 전달함
     * ===========================================================
     * DATE              AUTHOR             NOTE
     * -----------------------------------------------------------
     * 2026-08-24        HanWon.Jang        최초 생성
     */
    @Data
    @Schema(description = "모임원 퇴장 내역")
    public static class MemberExitHistoryDto {

        @Schema(description = "모임별 강제 퇴장 이력 번호")
        private Long kickNumb;

        @Schema(description = "퇴장한 사용자 번호")
        private Long userNumb;

        @Schema(description = "퇴장한 사용자 닉네임")
        private String userNick;

        @Schema(description = "퇴장한 사용자 프로필 이미지 경로")
        private String porfPath;

        @Schema(description = "퇴장 일시")
        private LocalDateTime exitDate;

        @Schema(description = "강제 퇴장 사유")
        private String kickRson;

        @Schema(description = "재가입 제한 해제 일시")
        private LocalDateTime rlesDate;

        @Schema(description = "재가입 제한 여부", allowableValues = {"Y", "N"})
        private String blocYsno;
    }

    /**
     * fileName       : ReadingGoalResultDto
     * author         : HanWon.Jang
     * date           : 2026-08-22
     * description    : 종료된 모임 독서 회차의 목표 결과와 달성자 정보를 전달함
     * ===========================================================
     * DATE              AUTHOR             NOTE
     * -----------------------------------------------------------
     * 2026-08-22        HanWon.Jang        최초 생성
     */
    @Data
    @Schema(description = "종료된 모임 독서 목표 결과")
    public static class ReadingGoalResultDto {

        @Schema(description = "모임 번호")
        private Long clubNumb;

        @Schema(description = "모임별 회차 번호")
        private Long rondNumb;

        @Schema(description = "모임 내 독서 순번")
        private Long readingOrdr;

        @Schema(description = "도서 제목")
        private String bookTitl;

        @Schema(description = "도서 저자")
        private String bookAthr;

        @Schema(description = "도서 표지 이미지 URL")
        private String bookCvim;

        @Schema(description = "목표 독서 시작일")
        private String goalStdt;

        @Schema(description = "목표 독서 종료일")
        private String goalEndt;

        @Schema(description = "공개 가능한 회차 참여 인원 수")
        private Integer partCnt;

        @Schema(description = "공개 가능한 목표 달성 인원 수")
        private Integer goalAchvCnt;

        @Schema(description = "본문을 작성한 공개 가능한 독후감 수")
        private Integer reportCnt;

        @Schema(description = "로그인 사용자의 목표 달성 여부")
        private Boolean myGoalAchieved;

        @Schema(description = "공개 가능한 목표 달성자 프로필 목록")
        private List<MemberProfileDto> achievementMemberList;
    }

    /**
     * fileName       : ReadingHistoryDto
     * author         : HanWon.Jang
     * date           : 2026-08-23
     * description    : 종료된 모임 독서 회차의 도서와 목표 달성 집계를 전달함
     * ===========================================================
     * DATE              AUTHOR             NOTE
     * -----------------------------------------------------------
     * 2026-08-23        HanWon.Jang        최초 생성
     * 2026-09-01        HanWon.Jang        목표 결과 상세 조회 가능 여부 추가
     */
    @Data
    @Schema(description = "이전 모임 독서 기록")
    public static class ReadingHistoryDto {

        @Schema(description = "모임 번호")
        private Long clubNumb;

        @Schema(description = "모임별 회차 번호")
        private Long rondNumb;

        @Schema(description = "도서 제목")
        private String bookTitl;

        @Schema(description = "도서 저자")
        private String bookAthr;

        @Schema(description = "도서 표지 이미지 URL")
        private String bookCvim;

        @Schema(description = "목표 독서 시작일")
        private String goalStdt;

        @Schema(description = "목표 독서 종료일")
        private String goalEndt;

        @Schema(description = "공개 가능한 회차 참여 인원 수")
        private Integer partCnt;

        @Schema(description = "공개 가능한 목표 달성 인원 수")
        private Integer goalAchvCnt;

        @Schema(description = "로그인 사용자의 회차 목표 결과 상세 조회 가능 여부")
        private Boolean resultAccessible;
    }

    /**
     * fileName       : ReadingRoundReportPageDto
     * author         : HanWon.Jang
     * date           : 2026-08-22
     * description    : 모임 독서 회차의 도서 정보와 완료 독후감 페이지를 전달함
     * ===========================================================
     * DATE              AUTHOR             NOTE
     * -----------------------------------------------------------
     * 2026-08-22        HanWon.Jang        최초 생성
     */
    @Data
    @Schema(description = "모임 독서 회차 완료 독후감 페이지")
    public static class ReadingRoundReportPageDto {

        @Schema(description = "모임 번호")
        private Long clubNumb;

        @Schema(description = "모임별 회차 번호")
        private Long rondNumb;

        @Schema(description = "모임 내 독서 순번")
        private Long readingOrdr;

        @Schema(description = "도서 제목")
        private String bookTitl;

        @Schema(description = "도서 저자")
        private String bookAthr;

        @Schema(description = "도서 표지 이미지 URL")
        private String bookCvim;

        @Schema(description = "회차 완료 독후감 평균 별점")
        private String ratingAverage;

        @Schema(description = "회차 완료 독후감 페이지")
        private PageDto<ReportDto> reportPage;
    }

    /**
     * fileName       : JoinReqDto
     * author         : SeungHyeon.Kang
     * date           : 2026-08-05
     * description    : 공개 승인형 모임의 질문별 장문 답변을 전달함
     * ===========================================================
     * DATE              AUTHOR             NOTE
     * -----------------------------------------------------------
     * 2026-08-05        SeungHyeon.Kang    최초 생성
     */
    @Data
    @Schema(description = "모임 가입 요청")
    public static class JoinReqDto {

        @Size(max = 5)
        @Schema(description = "가입 질문 순서와 같은 장문 답변 목록")
        private List<@NotBlank @Size(max = 2000) String> answerList;
    }

    /**
     * fileName       : InviteReqDto
     * author         : SeungHyeon.Kang
     * date           : 2026-08-05
     * description    : 모임장이 맞팔로우 사용자에게 발송할 초대 대상을 전달함
     * ===========================================================
     * DATE              AUTHOR             NOTE
     * -----------------------------------------------------------
     * 2026-08-05        SeungHyeon.Kang    최초 생성
     */
    @Data
    @Schema(description = "맞팔로우 초대 요청")
    public static class InviteReqDto {

        @NotEmpty
        @Schema(description = "초대할 맞팔로우 사용자 번호")
        private List<@NotNull Long> userNumbList;
    }

    /**
     * fileName       : ApplicationDecisionReqDto
     * author         : SeungHyeon.Kang
     * date           : 2026-08-05
     * description    : 모임장이 가입 신청에 내리는 승인 또는 거절 결정을 전달함
     * ===========================================================
     * DATE              AUTHOR             NOTE
     * -----------------------------------------------------------
     * 2026-08-05        SeungHyeon.Kang    최초 생성
     */
    @Data
    @Schema(description = "가입 신청 처리 요청")
    public static class ApplicationDecisionReqDto {

        @NotBlank
        @Schema(description = "처리 상태", allowableValues = {"APPROVED", "REJECTED"})
        private String joinStat;
    }

    /**
     * fileName       : InviteCandidateDto
     * author         : SeungHyeon.Kang
     * date           : 2026-08-05
     * description    : 모임장의 맞팔 초대 후보 정보를 전달함
     * ===========================================================
     * DATE              AUTHOR             NOTE
     * -----------------------------------------------------------
     * 2026-08-05        SeungHyeon.Kang    최초 생성
     */
    @Data
    @Schema(description = "맞팔로우 초대 후보")
    public static class InviteCandidateDto {

        @Schema(description = "사용자 번호")
        private Long userNumb;

        @Schema(description = "닉네임")
        private String userNick;

        @Schema(description = "프로필 이미지 경로")
        private String porfPath;

        @Schema(description = "한 줄 소개")
        private String intrCntn;

        @Schema(description = "관심분야 표시 문구")
        private String intrText;
    }

    /**
     * fileName       : SentInvitationDto
     * author         : Hanwon.Jang
     * date           : 2026-08-14
     * description    : 모임장이 발송한 유효한 초대 대상 정보를 전달함
     * ===========================================================
     * DATE              AUTHOR             NOTE
     * -----------------------------------------------------------
     * 2026-08-14        Hanwon.Jang    최초 생성
     */
    @Data
    @Schema(description = "보낸 모임 초대")
    public static class SentInvitationDto {

        @Schema(description = "초대 대상 사용자 번호")
        private Long userNumb;

        @Schema(description = "초대 대상 닉네임")
        private String userNick;

        @Schema(description = "초대 대상 프로필 이미지 경로")
        private String porfPath;

        @Schema(description = "관심분야 표시 문구")
        private String intrText;

        @Schema(description = "초대 발송 일시")
        private LocalDateTime invtDate;

        @Schema(description = "초대 만료 일시")
        private LocalDateTime exprDate;
    }

    /**
     * fileName       : InvitationDto
     * author         : SeungHyeon.Kang
     * date           : 2026-08-05
     * description    : 로그인 사용자에게 도착한 유효한 모임 초대를 전달함
     * ===========================================================
     * DATE              AUTHOR             NOTE
     * -----------------------------------------------------------
     * 2026-08-05        SeungHyeon.Kang    최초 생성
     */
    @Data
    @Schema(description = "수신 모임 초대")
    public static class InvitationDto {

        @Schema(description = "모임 번호")
        private Long clubNumb;

        @Schema(description = "모임명")
        private String clubName;

        @Schema(description = "초대 발송자 닉네임")
        private String senderNick;

        @Schema(description = "초대 발송 일시")
        private LocalDateTime invtDate;

        @Schema(description = "초대 만료 일시")
        private LocalDateTime exprDate;
    }

    /**
     * fileName       : ApplicationDto
     * author         : SeungHyeon.Kang
     * date           : 2026-08-05
     * description    : 모임장이 심사할 처리 중 가입 신청의 질문과 답변을 전달함
     * ===========================================================
     * DATE              AUTHOR             NOTE
     * -----------------------------------------------------------
     * 2026-08-05        SeungHyeon.Kang    최초 생성
     */
    @Data
    @Schema(description = "가입 신청 심사 항목")
    public static class ApplicationDto {

        @Schema(description = "모임 번호")
        private Long clubNumb;

        @Schema(description = "모임별 신청 번호")
        private Long applNumb;

        @Schema(description = "신청 사용자 번호")
        private Long userNumb;

        @Schema(description = "신청자 닉네임")
        private String userNick;

        @Schema(description = "신청자 프로필 이미지 경로")
        private String porfPath;

        @Schema(description = "신청 당시 질문 목록")
        private List<String> questionList;

        @Schema(description = "처리 전 장문 답변 목록")
        private List<String> answerList;

        @Schema(description = "가입 신청 상태")
        private String joinStat;

        @Schema(description = "가입 신청 일시")
        private LocalDateTime applDate;

        // 첫 번째 가입 질문 사본
        private String quesFirs;
        // 두 번째 가입 질문 사본
        private String quesSeco;
        // 세 번째 가입 질문 사본
        private String quesThir;
        // 네 번째 가입 질문 사본
        private String quesFour;
        // 다섯 번째 가입 질문 사본
        private String quesFift;
        // 첫 번째 가입 답변
        private String ansrFirs;
        // 두 번째 가입 답변
        private String ansrSeco;
        // 세 번째 가입 답변
        private String ansrThir;
        // 네 번째 가입 답변
        private String ansrFour;
        // 다섯 번째 가입 답변
        private String ansrFift;
    }

    private ReadingClubDto() {
        // DTO 컨테이너 인스턴스 생성을 차단함
    }
}
