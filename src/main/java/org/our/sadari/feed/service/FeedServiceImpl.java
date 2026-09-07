package org.our.sadari.feed.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.our.sadari.feed.dto.FeedDto;
import org.our.sadari.feed.mapper.FeedMapper;
import org.our.sadari.global.common.dto.PageDto;
import org.our.sadari.global.common.result.ResultData;
import org.our.sadari.global.common.result.ResultEnum;
import org.our.sadari.global.common.util.StringUtil;
import org.our.sadari.report.service.ReportTranslationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * fileName       : FeedServiceImpl
 * author         : SeungHyeon.Kang
 * date           : 2026-08-25
 * description    : 본인과 팔로잉 피드 조회 업무 로직을 구현함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-08-25        SeungHyeon.Kang         최초 생성
 * 2026-08-26        SeungHyeon.Kang         주석 규칙 정비
 * 2026-08-27        SeungHyeon.Kang         본인 피드와 알림 대상 단건 조회 추가
 * 2026-09-07        HanWon.Jang              독후감 번역 가능 여부 반영
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedServiceImpl implements FeedService {

    // 피드 한 페이지에 화면으로 반환할 활동 수
    private static final int FEED_PAGE_SIZE = 10;

    // 피드와 알림 이동에서 조회를 허용하는 공개 활동 대상 유형
    private static final Set<String> FEED_TARGET_TYPES = Set.of(
            org.our.sadari.global.common.constant.Constant.LIKE_TARGET_REPORT,
            org.our.sadari.global.common.constant.Constant.LIKE_TARGET_PROFILE_IMAGE,
            org.our.sadari.global.common.constant.Constant.LIKE_TARGET_BACKGROUND_IMAGE
    );

    // 피드 목록과 교류 집계 데이터 접근 객체
    private final FeedMapper feedMapper;
    // 공개 독후감 번역 가능 여부 처리 서비스
    private final ReportTranslationService reportTranslationService;

    /**
     * 로그인 사용자 본인과 팔로우하는 활성 사용자의 공개 활동 피드를 페이지 단위로 조회함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @param page 조회할 피드 페이지 번호
     * @return 본인과 팔로잉 피드 페이지 조회 결과
     */
    @Override
    public ResultData getFeedList(Long userNumb, int page) {
        // 인증 사용자 번호가 없으면 피드 공개 범위를 판정할 수 없어 조회를 중단함
        if (StringUtil.isEmpty(userNumb)) {
            // "인증에 실패했어요.\n다시 로그인 해주세요."
            return ResultData.fail(ResultEnum.AUTH_FAIL);
        }

        // 잘못된 페이지 번호가 전달돼도 첫 페이지부터 조회하도록 보정함
        int safePage = Math.max(page, 1);
        // 인증 사용자와 페이지 조건을 Mapper에 전달할 요청 객체를 생성함
        FeedDto request = new FeedDto();
        // 로그인 사용자를 기준으로 팔로잉 관계와 좋아요 여부를 조회함
        request.setLoginUserNumb(userNumb);
        // 요청 페이지 앞에 있는 피드 수만큼 조회 시작 위치를 이동함
        request.setPageOffset((safePage - 1) * FEED_PAGE_SIZE);
        // 다음 페이지 존재 여부를 판정하기 위해 화면 표시 수보다 한 건 더 조회함
        request.setPageLimit(FEED_PAGE_SIZE + 1);

        // 공개 범위와 페이지 조건이 적용된 본인 및 팔로잉 피드를 최신 활동순으로 조회함
        List<FeedDto> result = feedMapper.getFeedList(request);
        // 화면 표시 수를 초과한 한 건이 있으면 다음 페이지가 존재하는 것으로 판정함
        boolean hasNext = result.size() > FEED_PAGE_SIZE;
        // 다음 페이지 판정용 추가 한 건은 화면 응답에서 제외함
        List<FeedDto> visibleList = hasNext
                ? new ArrayList<>(result.subList(0, FEED_PAGE_SIZE))
                : result;
        // 현재 표시 언어와 월간 잔여량 및 캐시 상태로 독후감 피드의 번역 버튼 노출 여부를 설정함
        reportTranslationService.applyFeedAvailability(visibleList);

        // 화면 표시 목록과 현재 페이지 및 다음 페이지 여부를 공통 페이지 응답으로 반환함
        return ResultData.success(new PageDto<>(visibleList, safePage, hasNext));
    }

    /**
     * 알림 링크가 지정한 현재 공개 피드 대상 한 건을 조회함
     * 일반 목록에서 팔로우하지 않는 작성자도 허용하되 탈퇴·비공개·교체된 대상은 우회 조회하지 않음
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @param tagtType 조회할 피드 대상 유형
     * @param tagtNumb 조회할 피드 대상 번호
     * @return 알림 이동 대상 피드 항목 조회 결과
     */
    @Override
    public ResultData getFeedDtl(Long userNumb, String tagtType, Long tagtNumb) {
        // 인증 사용자 번호가 없으면 피드 공개 범위를 판정할 수 없어 조회를 중단함
        if (StringUtil.isEmpty(userNumb)) {
            // "인증에 실패했어요.\n다시 로그인 해주세요."
            return ResultData.fail(ResultEnum.AUTH_FAIL);
        }

        // 지원하지 않는 대상 유형이나 유효하지 않은 번호로 임의 조회하는 요청을 거부함
        if (StringUtil.isEmpty(tagtType) || !FEED_TARGET_TYPES.contains(tagtType) || StringUtil.isEmpty(tagtNumb)
                || tagtNumb <= 0) {
            // "잘못된 요청이에요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 인증 사용자와 정확한 피드 대상 조건을 Mapper에 전달할 요청 객체를 생성함
        FeedDto request = new FeedDto();
        // 로그인 사용자를 기준으로 활성 계정과 현재 공개 콘텐츠 범위를 적용함
        request.setLoginUserNumb(userNumb);
        // 알림 링크에 포함된 대상 유형과 번호를 정확한 단건 조건으로 설정함
        request.setTagtType(tagtType);
        request.setTagtNumb(tagtNumb);
        // 단건 조회는 첫 번째 일치 항목만 반환하도록 조회 범위를 제한함
        request.setPageOffset(0);
        request.setPageLimit(1);

        // 현재 공개 콘텐츠 여부와 정확한 대상 조건이 적용된 피드 한 건을 조회함
        List<FeedDto> result = feedMapper.getFeedList(request);

        // 현재 공개 상태인 대상이 없으면 만료되거나 접근할 수 없는 알림으로 처리함
        if (result.isEmpty()) {
            // "조회된 데이터가 없어요."
            return ResultData.fail(ResultEnum.COMMON_NO_DATA);
        }

        // 알림 직접 진입 카드에도 목록과 같은 번역 버튼 노출 정책을 적용함
        reportTranslationService.applyFeedAvailability(result);
        // 알림 링크가 지정한 첫 번째 피드 항목을 반환함
        return ResultData.success(result.get(0));
    }
}
