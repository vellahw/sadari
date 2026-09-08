package org.our.sadari.book.mapper;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.our.sadari.book.dto.BookDto;
import org.our.sadari.book.dto.PopularBookDto;

/**
 * fileName       : BookMapper
 * author         : SeungHyeon.Kang
 * date           : 2026-07-17
 * description    : 도서 데이터베이스 접근 메서드를 정의함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-07-17        SeungHyeon.Kang    최초 생성
 * 2026-08-16        SeungHyeon.Kang    기간별 인기 도서 조회 추가
 * 2026-09-08        HanWon.Jang        ISBN과 언어 기준 도서 식별 반영
 */
@Mapper
public interface BookMapper {
    /**
     * ISBN과 언어 기준으로 이미 등록된 도서가 있는지 확인함
     *
     * @author SeungHyeon.Kang
     * @param bookDto ISBN과 언어 코드를 포함한 도서 정보
     * @return 중복 도서 수
     */
    int dupBook(BookDto bookDto);

    /**
     * ISBN과 언어 기준으로 기존 도서 번호를 조회함
     *
     * @author SeungHyeon.Kang
     * @param bookDto 조회할 ISBN과 언어 코드를 포함한 도서 정보
     * @return 도서 번호
     */
    Long getBookNumbByIsbn(BookDto bookDto);

    /**
     * 신규 도서 정보를 등록함
     *
     * @author SeungHyeon.Kang
     * @param bookDto 등록할 도서 정보
     * @return 반영 건수
     */
    int setBook(BookDto bookDto);

    /**
     * 선택 기간의 독후감 고유 작성자 수 기준 인기 도서를 최대 10권 조회함
     *
     * @author SeungHyeon.Kang
     * @param periodStart 집계 기간의 시작 일시
     * @param nextPeriodStart 다음 집계 기간의 시작 일시
     * @return 독후감 작성자 수가 많은 순서의 인기 도서 목록
     */
    List<PopularBookDto> getPopularBookList(@Param("periodStart") LocalDateTime periodStart
                                           , @Param("nextPeriodStart") LocalDateTime nextPeriodStart);

}
