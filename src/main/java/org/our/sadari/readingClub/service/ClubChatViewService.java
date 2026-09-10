package org.our.sadari.readingClub.service;

import java.time.Duration;
import org.our.sadari.global.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * fileName       : ClubChatViewService
 * author         : HanWon.Jang
 * date           : 2026-09-10
 * description    : 화면별 만료 시각으로 모임 채팅 열람 여부 확인
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-09-10        HanWon.Jang        채팅 열람 중 알림 생략 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClubChatViewService {

    // 여러 서버와 브라우저 화면이 공유하는 열람 상태 저장소
    private final StringRedisTemplate redisTemplate;
    // 비정상 종료된 화면의 열람 상태를 해제할 유효 시간
    @Value("${reading-club.chat-view-ttl-seconds}")
    private long viewTtlSeconds;

    /**
     * 권한 확인이 끝난 화면의 열람 상태 갱신
     * @author HanWon.Jang
     * @param clubNumb 모임 번호
     * @param userNumb 인증 사용자 번호
     * @param viewId 브라우저 화면별 식별값
     * @param viewing 현재 화면 표시 여부
     */
    public void uptChatView(Long clubNumb, Long userNumb, String viewId
                            , boolean viewing) {
        // 검증되지 않은 내부 호출의 저장소 접근 차단
        if (StringUtil.hasEmpty(clubNumb, userNumb, viewId)) {
            return;
        }
        // 사용자와 모임별로 분리하여 다른 화면의 열람 상태 보존
        String key = "reading-club:chat:view:" + clubNumb + ":" + userNumb;
        try {
            // 화면 종료 시 해당 화면만 제거하여 다른 탭의 상태 보존
            if (!viewing) {
                redisTemplate.opsForZSet().remove(key, viewId);
                return;
            }
            // 만료된 화면 정리와 현재 화면의 유효 시간 연장
            long now = System.currentTimeMillis();
            redisTemplate.opsForZSet().removeRangeByScore(key, Double.NEGATIVE_INFINITY, now);
            redisTemplate.opsForZSet().add(key, viewId, now + Duration.ofSeconds(viewTtlSeconds).toMillis());
            redisTemplate.expire(key, Duration.ofSeconds(viewTtlSeconds));
        } catch (DataAccessException exception) {
            // 일시적인 저장소 장애가 채팅 읽음 처리에 미치는 영향 차단
            log.warn("채팅 열람 상태 갱신 실패: {}", exception.getClass().getSimpleName());
        }
    }

    /**
     * 하나 이상의 유효한 화면에서 해당 채팅방 열람 여부 확인
     * @author HanWon.Jang
     * @param clubNumb 모임 번호
     * @param userNumb 알림 수신 후보 번호
     * @return 유효한 열람 화면 존재 여부
     */
    public boolean isViewing(Long clubNumb, Long userNumb) {
        try {
            // 만료된 화면은 키 정리 지연과 관계없이 알림 수신 대상에 포함
            Long count = redisTemplate.opsForZSet().count("reading-club:chat:view:" + clubNumb + ":" + userNumb,
                    System.currentTimeMillis() + 1, Double.POSITIVE_INFINITY);
            return !StringUtil.isEmpty(count) && count > 0;
        } catch (DataAccessException exception) {
            // 열람 여부 확인 실패 시 알림 누락 방지를 위해 정상 발송
            log.warn("채팅 열람 상태 조회 실패: {}", exception.getClass().getSimpleName());
            return false;
        }
    }
}
