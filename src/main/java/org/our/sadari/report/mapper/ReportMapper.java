package org.our.sadari.report.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.our.sadari.myPage.dto.ReadingGoalDto;
import org.our.sadari.myPage.dto.ReadingSummaryQueryDto;
import org.our.sadari.report.dto.ReportAlimDto;
import org.our.sadari.report.dto.ReportDto;
import org.our.sadari.report.dto.ReportTranslationDto;
import org.our.sadari.social.dto.SocialDto;

/**
 * fileName       : ReportMapper
 * author         : SeungHyeon.Kang
 * date           : 2026-07-17
 * description    : 독후감과 독서 목표 데이터베이스 접근 메서드를 정의함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-07-17        SeungHyeon.Kang    최초 생성
 * 2026-08-01        SeungHyeon.Kang    최근 독후감·공개 조회 추가
 * 2026-08-04        SeungHyeon.Kang    독서 요약 공개 범위 조회 조건 문서화
 * 2026-08-14        SeungHyeon.Kang    독후감 관계 정리·정렬 추가
 * 2026-08-21        SeungHyeon.Kang    독후감별 알림 설정 조회·변경 추가
 * 2026-09-07        SeungHyeon.Kang    독후감 번역 캐시 조회·저장 추가
 * 2026-09-11        HanWon.Jang        진행 중인 모임 독후감 삭제 시 중도하차 처리 추가
 */
@Mapper
public interface ReportMapper {
    /**
     * 로그인 사용자의 독후감 목록을 검색어와 정렬 조건에 맞춰 조회함
     *
     * @author SeungHyeon.Kang
     * @param req 사용자 번호, 검색어, 정렬 조건을 담은 요청 DTO
     * @return 독후감 목록
     */
    List<ReportDto> getReportList(ReportDto req);

    /**
     * 본인 또는 다른 사용자 프로필의 기간별 독서량과 목표 달성 정보를 통합 조회함
     *
     * @author SeungHyeon.Kang
     * @param req 사용자 번호, 선택적 공개 여부, 기간 및 목표 기준값
     * @return 기간별 독서량과 목표 달성 집계
     */
    ReadingSummaryQueryDto getReadingSummary(ReadingSummaryQueryDto req);

    /**
     * 본인 또는 다른 사용자 프로필에 표시할 현재 읽는 책과 올해 완료한 책을 한 번에 조회함
     *
     * @author SeungHyeon.Kang
     * @param req 사용자 번호, 선택적 공개 여부, 현재 연도 기간 및 독서 상태
     * @return 독서 요약에 표시할 독후감 목록
     */
    List<ReportDto> getReadingSummaryList(ReadingSummaryQueryDto req);

    /**
     * 사용자, 목표 기간, 목표 유형에 해당하는 독서 목표를 조회함
     *
     * @author SeungHyeon.Kang
     * @param req 목표 조회 조건
     * @return 독서 목표 정보
     */
    ReadingGoalDto getReadingGoalDtl(ReadingGoalDto req);

    /**
     * 독서 목표를 신규 등록하거나 기존 목표를 갱신함
     *
     * @author SeungHyeon.Kang
     * @param req 저장할 목표 정보
     * @return 반영 건수
     */
    int setReadingGoal(ReadingGoalDto req);

    /**
     * 독후감 상세와 연결된 도서 정보를 조회함
     *
     * @author SeungHyeon.Kang
     * @param req 사용자 번호와 독후감 번호
     * @return 독후감 상세 정보
     */
    ReportDto getReportDtl(ReportDto req);

    /**
     * 로그인 사용자가 동일 ISBN으로 가장 최근에 작성한 독후감을 조회함
     *
     * @author SeungHyeon.Kang
     * @param req 로그인 사용자 번호와 조회할 ISBN
     * @return 동일 ISBN의 최근 독후감 정보
     */
    ReportDto getReportByIsbnDtl(ReportDto req);

    /**
     * 좋아요를 허용할 수 있는 독후감의 작성자와 좋아요 알림 설정을 조회함
     * TB_LIKEXX 변경은 SocialMapper에서 처리하지만, 대상 검증 기준은 TM_REPORT이므로 ReportMapper에서 관리함
     *
     * @author SeungHyeon.Kang
     * @param req 독후감 번호와 요청 사용자 번호
     * @return 좋아요 대상 작성자와 알림 설정
     */
    SocialDto.LikeDto getReportLikeDtl(SocialDto.LikeDto req);

    /**
     * ISBN 기준 활성 사용자의 공개 독후감을 팔로우 작성자 우선으로 조회함
     *
     * @author SeungHyeon.Kang
     * @param req ISBN과 로그인 사용자 번호
     * @return 공개 독후감 목록
     */
    List<ReportDto> getPublicReportList(ReportDto req);

    /** 알림이 지정한 공개 독후감 한 건과 도서 정보를 조회함 */
    ReportDto getPublicReportTarget(ReportDto req);

    /**
     * 공개 범위와 차단 관계를 검증한 독후감 원문을 잠금 조회함
     *
     * @author SeungHyeon.Kang
     * @param req 로그인 사용자 번호와 독후감 번호
     * @return 번역 가능한 독후감 원문
     */
    ReportTranslationDto getReportTrnsSource(ReportTranslationDto req);

    /**
     * 독후감 번호와 대상 언어에 해당하는 번역 캐시를 조회함
     *
     * @author SeungHyeon.Kang
     * @param req 독후감 번호와 번역 대상 언어
     * @return 저장된 번역 캐시
     */
    ReportTranslationDto getReportTrnsDtl(ReportTranslationDto req);

    /**
     * 독후감과 대상 언어별 번역 캐시를 신규 저장하거나 원문 변경 내용으로 갱신함
     *
     * @author SeungHyeon.Kang
     * @param req 독후감 번호와 대상 언어 및 번역 결과
     * @return 반영 건수
     */
    int setReportTrns(ReportTranslationDto req);

    /**
     * ISBN 기준으로 연결된 완료 또는 중단 독후감의 평균 별점을 조회함
     *
     * @author SeungHyeon.Kang
     * @param bookIsbn 조회할 도서 ISBN
     * @return 평균 별점
     */
    BigDecimal getPublicRatingAvgByIsbn(String bookIsbn);

    /**
     * 신규 독후감을 저장함
     *
     * @author SeungHyeon.Kang
     * @param reportDto 등록할 독후감 정보
     * @return 반영 건수
     */
    int setReport(ReportDto reportDto);

    /**
     * 기존 독후감을 수정함
     *
     * @author SeungHyeon.Kang
     * @param reportDto 수정할 독후감 정보
     * @return 반영 건수
     */
    int uptReport(ReportDto reportDto);

    /**
     * 로그인 사용자가 작성한 독후감의 좋아요 알림 여부를 변경함
     *
     * @author SeungHyeon.Kang
     * @param reportAlimDto 사용자 번호, 독후감 번호와 알림 사용 여부
     * @return 반영 건수
     */
    int uptLikeAlim(ReportAlimDto reportAlimDto);

    /**
     * 로그인 사용자가 작성한 독후감의 댓글 알림 여부를 변경함
     *
     * @author SeungHyeon.Kang
     * @param reportAlimDto 사용자 번호, 독후감 번호와 알림 사용 여부
     * @return 반영 건수
     */
    int uptReplyAlim(ReportAlimDto reportAlimDto);

    /**
     * 독후감의 읽기 상태와 별점 및 공개 여부를 빠르게 수정함
     *
     * @author SeungHyeon.Kang
     * @param reportDto 사용자 번호, 독후감 번호, 읽기 상태, 별점, 공개 여부
     * @return 반영 건수
     */
    int uptReptStatusGrade(ReportDto reportDto);

    /**
     * 진행 중인 모임 회차의 연결 독후감 삭제 전 참여 기록을 중도하차로 확정함
     *
     * @author HanWon.Jang
     * @param reportDto 사용자 번호와 독후감 번호
     * @return 반영 건수
     */
    int uptClubReadingDropout(ReportDto reportDto);

    /**
     * 독후감에 연결된 댓글과 답글의 좋아요를 삭제함
     *
     * @author SeungHyeon.Kang
     * @param reportDto 사용자 번호와 독후감 번호
     * @return 반영 건수
     */
    int delReportReplyLikes(ReportDto reportDto);

    /**
     * 독후감에 연결된 대댓글을 부모 댓글보다 먼저 삭제함
     *
     * @author SeungHyeon.Kang
     * @param reportDto 사용자 번호와 독후감 번호
     * @return 반영 건수
     */
    int delReportChildReplies(ReportDto reportDto);

    /**
     * 독후감에 연결된 최상위 댓글을 삭제함
     *
     * @author SeungHyeon.Kang
     * @param reportDto 사용자 번호와 독후감 번호
     * @return 반영 건수
     */
    int delReportReplies(ReportDto reportDto);

    /**
     * 독후감에 연결된 좋아요를 삭제함
     *
     * @author SeungHyeon.Kang
     * @param reportDto 사용자 번호와 독후감 번호
     * @return 반영 건수
     */
    int delReportLikes(ReportDto reportDto);

    /**
     * 로그인 사용자의 독후감을 삭제함
     *
     * @author SeungHyeon.Kang
     * @param reportDto 사용자 번호와 독후감 번호
     * @return 반영 건수
     */
    int delReport(ReportDto reportDto);
}
