package org.our.sadari.readingClub.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * fileName       : ClubChatViewServiceTest
 * author         : HanWon.Jang
 * date           : 2026-09-10
 * description    : 채팅 화면별 열람 만료와 알림 누락 방지 검증
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-09-10        HanWon.Jang        열람 상태 경계 검증
 */
@ExtendWith(MockitoExtension.class)
class ClubChatViewServiceTest {

    // 화면 상태 저장소와 정렬 집합 연산 대역
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ZSetOperations<String, String> zSet;
    // 열람 상태 테스트 대상
    private ClubChatViewService service;
    // 테스트 모임과 수신자만 사용하는 저장소 키
    private static final String VIEW_KEY = "reading-club:chat:view:10:20";

    /** 독립된 저장소 대역과 정책값 구성 @author HanWon.Jang */
    @BeforeEach
    void setUp() {
        service = new ClubChatViewService(redisTemplate);
        ReflectionTestUtils.setField(service, "viewTtlSeconds", 15L);
        when(redisTemplate.opsForZSet()).thenReturn(zSet);
    }

    /** 현재 화면 갱신과 만료된 화면만 정리하는 경계 검증 @author HanWon.Jang */
    @Test
    void uptViewRefreshesLease() {
        // 호출 시각을 기준으로 만료 시각과 정리 범위 비교
        long before = System.currentTimeMillis();
        service.uptChatView(10L, 20L, "view-a", true);
        long after = System.currentTimeMillis();
        ArgumentCaptor<Double> expires = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> cleanup = ArgumentCaptor.forClass(Double.class);
        verify(zSet).add(eq(VIEW_KEY), eq("view-a"), expires.capture());
        verify(zSet).removeRangeByScore(eq(VIEW_KEY), eq(Double.NEGATIVE_INFINITY), cleanup.capture());
        assertTrue(expires.getValue() >= before + 15000 && expires.getValue() <= after + 15000);
        assertTrue(cleanup.getValue() >= before && cleanup.getValue() <= after);
        verify(redisTemplate).expire(VIEW_KEY, Duration.ofSeconds(15));
    }

    /** 다른 탭의 열람 상태를 보존하는 화면별 종료 검증 @author HanWon.Jang */
    @Test
    void uptViewClosesOnlyOne() {
        service.uptChatView(10L, 20L, "view-a", false);
        verify(zSet).remove(VIEW_KEY, "view-a");
        verify(redisTemplate, never()).delete(VIEW_KEY);
    }

    /** 만료된 화면을 제외한 하나 이상의 열람 화면 판단 검증 @author HanWon.Jang */
    @Test
    void isViewingUsesExpiry() {
        // 조회 기준 시각 이후에 만료되는 화면만 열람으로 인정
        long before = System.currentTimeMillis();
        when(zSet.count(eq(VIEW_KEY), anyDouble(), eq(Double.POSITIVE_INFINITY))).thenReturn(1L, 0L);
        assertTrue(service.isViewing(10L, 20L));
        assertFalse(service.isViewing(10L, 20L));
        ArgumentCaptor<Double> cutoff = ArgumentCaptor.forClass(Double.class);
        verify(zSet, org.mockito.Mockito.times(2)).count(eq(VIEW_KEY), cutoff.capture(), eq(Double.POSITIVE_INFINITY));
        assertTrue(cutoff.getAllValues().stream().allMatch(value -> value > before));
    }

    /** 저장소 장애가 수신 알림을 누락시키지 않는지 검증 @author HanWon.Jang */
    @Test
    void isViewingFailsOpen() {
        when(zSet.count(eq(VIEW_KEY), anyDouble(), eq(Double.POSITIVE_INFINITY)))
                .thenThrow(new DataAccessResourceFailureException("test"));
        assertFalse(service.isViewing(10L, 20L));
    }
}
