package org.our.sadari.feed.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.our.sadari.feed.dto.FeedDto;
import org.our.sadari.feed.mapper.FeedMapper;
import org.our.sadari.global.common.constant.Constant;
import org.our.sadari.global.common.dto.PageDto;
import org.our.sadari.global.common.result.ResultData;
import org.our.sadari.report.service.ReportTranslationService;

/**
 * fileName       : FeedServiceImplTest
 * author         : SeungHyeon.Kang
 * date           : 2026-08-27
 * description    : 본인과 팔로잉 피드의 페이지 및 알림 대상 단건 조회 조건을 검증함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-08-27        SeungHyeon.Kang    최초 생성
 */
@ExtendWith(MockitoExtension.class)
class FeedServiceImplTest {

    // 피드 데이터 접근 객체
    @Mock
    private FeedMapper feedMapper;

    // 공개 독후감 번역 가능 여부 처리 서비스
    @Mock
    private ReportTranslationService reportTranslationService;

    // 본인과 팔로잉 피드 조회 단위 테스트 대상
    private FeedServiceImpl feedService;

    /**
     * 각 테스트가 독립된 Mock Mapper를 사용하는 피드 서비스를 구성함
     *
     * @author SeungHyeon.Kang
     */
    @BeforeEach
    void setUp() {
        // 피드 조회 단위 테스트 대상을 생성함
        feedService = new FeedServiceImpl(feedMapper, reportTranslationService);
    }

    /**
     * 첫 피드 페이지 조회에 인증 사용자와 다음 페이지 판정 조건을 전달하는지 검증함
     *
     * @author SeungHyeon.Kang
     */
    @Test
    void getFeedListUsesScope() {
        // 첫 페이지에 반환할 공개 피드 한 건을 생성함
        FeedDto item = new FeedDto();
        // 독후감 피드 대상 유형을 설정함
        item.setTagtType(Constant.LIKE_TARGET_REPORT);
        // 독후감 피드 대상 번호를 설정함
        item.setTagtNumb(157L);
        // Mapper가 공개 범위를 적용한 피드 한 건을 반환하도록 구성함
        when(feedMapper.getFeedList(any(FeedDto.class))).thenReturn(List.of(item));

        // 로그인 사용자의 첫 피드 페이지를 조회함
        ResultData result = feedService.getFeedList(31L, 1);

        // 피드 페이지 조회 성공 응답을 확인함
        assertEquals(200, result.getCode());
        // 공통 페이지 응답을 조회함
        PageDto<?> page = (PageDto<?>) result.getData();
        // 첫 페이지에 공개 피드 한 건이 반환되는지 확인함
        assertEquals(List.of(item), page.list());
        // 다음 페이지가 없는지 확인함
        assertFalse(page.hasNext());
        // Mapper에 전달된 피드 조회 조건을 캡처함
        ArgumentCaptor<FeedDto> requestCaptor = ArgumentCaptor.forClass(FeedDto.class);
        // 공개 피드 목록 Mapper가 호출됐는지 확인함
        verify(feedMapper).getFeedList(requestCaptor.capture());
        // 로그인 사용자 번호가 본인과 팔로잉 범위 기준으로 전달됐는지 확인함
        assertEquals(31L, requestCaptor.getValue().getLoginUserNumb());
        // 다음 페이지 판정을 위해 화면 크기보다 한 건 더 조회하는지 확인함
        assertEquals(11, requestCaptor.getValue().getPageLimit());
    }

    /**
     * 알림 이동 대상 단건 조회에 정확한 유형과 번호 및 로그인 사용자 범위를 전달하는지 검증함
     *
     * @author SeungHyeon.Kang
     */
    @Test
    void getFeedDtlUsesExactTarget() {
        // 알림 링크가 지정한 프로필 사진 피드 한 건을 생성함
        FeedDto item = new FeedDto();
        // 프로필 사진 대상 유형을 설정함
        item.setTagtType(Constant.LIKE_TARGET_PROFILE_IMAGE);
        // 프로필 사진 대상 번호를 설정함
        item.setTagtNumb(157L);
        // Mapper가 공개 범위 안의 정확한 피드 한 건을 반환하도록 구성함
        when(feedMapper.getFeedList(any(FeedDto.class))).thenReturn(List.of(item));

        // 알림 링크가 지정한 프로필 사진 피드를 조회함
        ResultData result = feedService.getFeedDtl(31L, Constant.LIKE_TARGET_PROFILE_IMAGE, 157L);

        // 대상 피드 조회 성공 응답을 확인함
        assertEquals(200, result.getCode());
        // 조회된 피드 항목을 확인함
        assertEquals(item, result.getData());
        // Mapper에 전달된 대상 조회 조건을 캡처함
        ArgumentCaptor<FeedDto> requestCaptor = ArgumentCaptor.forClass(FeedDto.class);
        // 공개 범위가 적용되는 기존 피드 Mapper가 호출됐는지 확인함
        verify(feedMapper).getFeedList(requestCaptor.capture());
        // 로그인 사용자 번호가 공개 범위 기준으로 전달됐는지 확인함
        assertEquals(31L, requestCaptor.getValue().getLoginUserNumb());
        // 알림 링크의 대상 유형이 정확한 조회 조건으로 전달됐는지 확인함
        assertEquals(Constant.LIKE_TARGET_PROFILE_IMAGE, requestCaptor.getValue().getTagtType());
        // 알림 링크의 대상 번호가 정확한 조회 조건으로 전달됐는지 확인함
        assertEquals(157L, requestCaptor.getValue().getTagtNumb());
        // 단건 조회 한도로 제한했는지 확인함
        assertEquals(1, requestCaptor.getValue().getPageLimit());
    }
}
