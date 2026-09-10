package org.our.sadari.alim.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.our.sadari.alim.dto.AlimDto;
import org.our.sadari.alim.mapper.AlimMapper;
import org.our.sadari.global.common.constant.Constant;
import org.our.sadari.global.common.result.ResultData;
import org.our.sadari.global.common.result.ResultEnum;
import org.our.sadari.global.common.util.StringUtil;
import org.our.sadari.push.service.PushService;
import org.our.sadari.social.service.UserBlockService;
import org.our.sadari.user.dto.UserSettingDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * fileName       : AlimServiceImpl
 * author         : SeungHyeon.Kang
 * date           : 2026-07-24
 * description    : 알림 업무 로직을 구현함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-07-24        SeungHyeon.Kang    최초 생성
 * 2026-07-29        HanWon.Jang        댓글 등록 알림 중복 차단 제외
 * 2026-08-12        SeungHyeon.Kang    알림 아이콘 처리 정리
 * 2026-08-14        SeungHyeon.Kang    사용자 알림 10개 단위 조회 반영
 * 2026-08-20        SeungHyeon.Kang    타이머 알림 중복 제외
 * 2026-08-25        SeungHyeon.Kang    사진 댓글 알림 중복 제외
 * 2026-08-27        SeungHyeon.Kang    권한 기반 알림과 사진 프로필 이동 계산
 * 2026-09-10        HanWon.Jang        채팅 열람과 알림 읽음 동기화
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlimServiceImpl implements AlimService {

    // 알림번호로 클릭 시점의 최종 목적지를 계산하는 프론트엔드 경로
    private static final String ALIM_TARGET_ROUTE = "/notification-target/";

    // 알림 페이지 크기 설정값
    private static final int ALIM_PAGE_SIZE = 10;
    // Alim 데이터 접근 객체
    private final AlimMapper alimMapper;
    // Push 업무 처리 서비스
    private final PushService pushService;
    // 사용자 간 양방향 차단 관계 조회 서비스
    private final UserBlockService userBlockService;

    /**
     * 로그인 사용자의 알림 목록을 조회함
     * 인증 정보가 없으면 다른 사용자의 알림을 조회할 수 없도록 AUTH_FAIL을 반환함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @return 알림 목록
     */
    @Override
    @Transactional(readOnly = true)
    public ResultData getMyAlimList(Long userNumb, int page) {
        // userNumb 값이 비어 있을 때 후속 참조를 차단하기 위한 분기임
        if (StringUtil.isEmpty(userNumb)) {
            // "인증에 실패했어요.\n다시 로그인 해주세요."
            return ResultData.fail(ResultEnum.AUTH_FAIL);
        }

        // 요청 크기가 최소 조회 건수보다 작지 않도록 보정함
        int currentPage = Math.max(page, 1);
        // 알림 목록 조회 조건을 담을 객체를 생성함
        AlimDto.AlimListReqDto req = new AlimDto.AlimListReqDto();
        // UserNumb 업무 값을 req DTO에 설정함
        req.setUserNumb(userNumb);
        // Page 업무 값을 req DTO에 설정함
        req.setPage(currentPage);
        // PageSize 업무 값을 req DTO에 설정함
        req.setPageSize(ALIM_PAGE_SIZE);
        // StartRow 업무 값을 req DTO에 설정함
        req.setStartRow(((currentPage - 1) * ALIM_PAGE_SIZE) + 1);
        // 다음 페이지가 있는지 판단해야 하므로 화면 표시 개수보다 1개 더 조회함
        req.setEndRow(currentPage * ALIM_PAGE_SIZE + 1);

        // MyAlimList 데이터를 DB에서 조회함
        List<AlimDto.AlimItemDto> searchedList = alimMapper.getMyAlimList(req);
        // searchedList 값이 비어 있을 때 후속 참조를 차단하기 위한 분기임
        if (StringUtil.isEmpty(searchedList)) {
            // 조회 결과가 없을 때 사용할 빈 목록을 생성함
            searchedList = Collections.emptyList();
        }

        // 처리된 데이터 건수를 확인함
        boolean hasNext = searchedList.size() > ALIM_PAGE_SIZE;
        // 요청한 페이지 크기만큼 알림 목록을 분리한 객체를 생성함
        List<AlimDto.AlimItemDto> visibleList = new ArrayList<>(hasNext ? searchedList.subList(0, ALIM_PAGE_SIZE) : searchedList);

        // 모든 알림은 저장 URL 없이 사용자별 알림번호를 해석하는 공통 경로를 화면에 제공함
        for (AlimDto.AlimItemDto alim : visibleList) {
            // 사용자별 알림번호로 프론트엔드 해석 경로를 설정함
            alim.setLinkUrlx(ALIM_TARGET_ROUTE + alim.getAlimNumb());
        }

        // 알림 목록과 다음 페이지 여부를 담을 객체를 생성함
        AlimDto.AlimListResDto res = new AlimDto.AlimListResDto();
        // List 업무 값을 res DTO에 설정함
        res.setList(visibleList);
        // HasNext 업무 값을 res DTO에 설정함
        res.setHasNext(hasNext);
        // NextPage 업무 값을 res DTO에 설정함
        res.setNextPage(currentPage + 1);
        // UnreadCnt 업무 값을 res DTO에 설정함
        res.setUnreadCnt(alimMapper.getUnreadAlimCnt(userNumb));
        // 로그인 사용자의 알림 목록을 조회 결과를 성공 응답으로 반환함
        return ResultData.success(res);
    }

    /**
     * 햄버거 메뉴 배지에서 사용할 미읽음 알림 수를 조회함
     * 목록 진입 전 숫자만 필요하므로 목록 조회와 읽음 처리를 함께 수행하지 않음
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @return 미읽음 알림 수
     */
    @Override
    @Transactional(readOnly = true)
    public ResultData getUnreadAlimCnt(Long userNumb) {
        // userNumb 값이 비어 있을 때 후속 참조를 차단하기 위한 분기임
        if (StringUtil.isEmpty(userNumb)) {
            // "인증에 실패했어요.\n다시 로그인 해주세요."
            return ResultData.fail(ResultEnum.AUTH_FAIL);
        }

        // 읽지 않은 알림 건수를 담을 객체를 생성함
        AlimDto.AlimUnreadCntDto res = new AlimDto.AlimUnreadCntDto();
        // UnreadCnt 업무 값을 res DTO에 설정함
        res.setUnreadCnt(alimMapper.getUnreadAlimCnt(userNumb));
        // 햄버거 메뉴 배지에서 사용할 미읽음 알림 수를 조회 결과를 성공 응답으로 반환함
        return ResultData.success(res);
    }

    /**
     * 인증 사용자의 알림번호로 클릭 시점의 콘텐츠 공개 여부와 팔로우 관계를 확인해 이동 주소를 계산함
     * 다른 사용자의 알림, 삭제된 콘텐츠 및 현재 접근할 수 없는 사진은 이동 대상으로 제공하지 않음
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @param alimNumb 이동할 사용자별 알림 번호
     * @return 현재 접근 권한이 반영된 내부 이동 주소
     */
    @Override
    @Transactional(readOnly = true)
    public ResultData getAlimTarget(Long userNumb, Long alimNumb) {
        // 인증 사용자와 양수 알림번호가 없으면 알림 소유권을 검증할 수 없으므로 요청을 거부함
        if (StringUtil.hasEmpty(userNumb, alimNumb) || userNumb <= 0 || alimNumb <= 0) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 인증 사용자와 알림번호를 함께 사용하여 다른 사용자의 동일 순번 알림이 조회되지 않게 함
        AlimDto.AlimTargetDto req = new AlimDto.AlimTargetDto();
        // 로그인 사용자 번호를 알림 소유권 조건으로 설정함
        req.setUserNumb(userNumb);
        // 사용자별 알림 번호를 조회 조건으로 설정함
        req.setAlimNumb(alimNumb);
        // 현재 원본 콘텐츠와 관계 상태를 조회함
        AlimDto.AlimTargetDto target = alimMapper.getAlimTargetDtl(req);

        // 소유한 미삭제 알림이 아니면 내부 대상 정보를 노출하지 않음
        if (StringUtil.isEmpty(target)) {
            // "접근할 수 없는 요청이에요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 대상 유형과 현재 소유자 및 접근 상태를 기준으로 최종 내부 경로를 계산함
        String linkUrlx = createTargetLink(userNumb, target);

        // 콘텐츠가 삭제되었거나 관계가 바뀌어 현재 접근할 수 없으면 이동 주소를 제공하지 않음
        if (StringUtil.isEmpty(linkUrlx)) {
            // "접근할 수 없는 요청이에요."
            return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
        }

        // 클릭 시점의 권한으로 계산한 최종 내부 경로를 응답에 설정함
        target.setLinkUrlx(linkUrlx);
        // 검증된 알림 이동 주소를 성공 응답으로 반환함
        return ResultData.success(target);
    }

    /**
     * 알림센터 항목 또는 푸시 알림을 클릭한 경우 해당 사용자의 알림 한 건을 읽음 처리함
     * 이미 읽은 알림에 같은 요청이 다시 들어와도 성공으로 응답하는 멱등 방식으로 처리함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @param req 읽음 처리할 사용자별 알림 번호
     * @return 읽음 처리 후 남은 미읽음 알림 수
     */
    @Override
    @Transactional
    public ResultData uptAlimRead(Long userNumb, AlimDto.AlimReadReqDto req) {
        // userNumb 값이 비어 있을 때 후속 참조를 차단하기 위한 분기임
        if (StringUtil.isEmpty(userNumb)) {
            // "인증에 실패했어요.\n다시 로그인 해주세요."
            return ResultData.fail(ResultEnum.AUTH_FAIL);
        }

        // req 값이 비어 있을 때 후속 참조를 차단하기 위한 분기임
        if (StringUtil.isEmpty(req) || StringUtil.isEmpty(req.getAlimNumb())) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // USER_NUMB는 요청 본문을 신뢰하지 않고 인증 정보로 덮어써 다른 사용자의 알림 변경을 차단함
        req.setUserNumb(userNumb);
        // AlimRead 데이터를 DB에서 수정함
        alimMapper.uptAlimRead(req);

        // 읽지 않은 알림 건수를 담을 객체를 생성함
        AlimDto.AlimUnreadCntDto res = new AlimDto.AlimUnreadCntDto();
        // UnreadCnt 업무 값을 res DTO에 설정함
        res.setUnreadCnt(alimMapper.getUnreadAlimCnt(userNumb));
        // 알림센터 항목 또는 푸시 알림을 클릭한 경우 해당 사용자의 알림 한 건을 읽음 처리 결과를 성공 응답으로 반환함
        return ResultData.success(res);
    }

    /**
     * 사용자가 모두 지우기 버튼을 누르면 아직 화면에 로드하지 않은 알림까지 전부 삭제 상태로 변경함
     * READ_YSNO와 READ_DATE는 변경하지 않아 읽음 이력과 삭제 이력을 서로 독립적으로 유지함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 로그인 사용자 번호
     * @return 모두 지우기 처리 결과
     */
    @Override
    @Transactional
    public ResultData delAllAlim(Long userNumb) {
        // userNumb 값이 비어 있을 때 후속 참조를 차단하기 위한 분기임
        if (StringUtil.isEmpty(userNumb)) {
            // "인증에 실패했어요.\n다시 로그인 해주세요."
            return ResultData.fail(ResultEnum.AUTH_FAIL);
        }

        // 모두 지우기는 읽음 여부를 변경하지 않고 삭제 여부만 갱신하여 알림센터 노출 대상에서 제외함
        alimMapper.delAllAlim(userNumb);

        // 읽지 않은 알림 건수를 담을 객체를 생성함
        AlimDto.AlimUnreadCntDto res = new AlimDto.AlimUnreadCntDto();
        // UnreadCnt 업무 값을 res DTO에 설정함
        res.setUnreadCnt(0);
        // 사용자가 모두 지우기 버튼을 누르면 아직 화면에 로드하지 않은 알림까지 전부 삭제 상태로 변경 결과를 성공 응답으로 반환함
        return ResultData.success(res);
    }

    /**
     * 이동 대상 식별값을 알림에 저장하고 커밋 이후 알림번호 기반 푸시 이동 경로를 발송함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 알림 수신자 번호
     * @param alimSitu 알림 상황 코드
     * @param tempCode 알림 템플릿 코드
     * @param tagtType 이동 대상 유형
     * @param tagtNumb 이동 대상 번호
     * @param messageNumb 알림 원본 댓글 또는 채팅 번호
     * @param replaceMap 화면 문구 치환값
     * @return 알림 저장 결과
     */
    @Override
    @Transactional
    public ResultData sendAlim(Long userNumb, String alimSitu, String tempCode, String tagtType
                             , Long tagtNumb, Long messageNumb, Map<String, Object> replaceMap) {
        // 수신자와 템플릿 및 지원 대상 정보가 없으면 이동할 수 없는 알림이 저장되므로 요청을 거부함
        if (StringUtil.hasEmpty(userNumb, alimSitu, tempCode, tagtType)
                || !isAlimTargetValid(tempCode, tagtType, tagtNumb)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 탈퇴 또는 영구 삭제 대기 회원에게는 알림과 푸시를 새로 만들지 않음
        if (alimMapper.getActiveAlimUserCnt(userNumb) == 0) {
            // 수신 제외 상태를 정상 생략 결과로 반환함
            return ResultData.success();
        }

        // 선택형 알림이 꺼져 있으면 알림센터 항목과 푸시를 모두 생성하지 않음
        if (!isUserAlimEnabled(userNumb, tempCode)) {
            return ResultData.success();
        }

        // 치환 문구가 없는 알림도 발송할 수 있어 null Map은 빈 Map으로 보정함
        // 이렇게 하면 호출부가 치환값 없는 알림을 보낼 때 불필요하게 new HashMap<>()을 만들 필요가 없음
        Map<String, Object> safeReplaceMap = StringUtil.isEmpty(replaceMap)
                ? Collections.emptyMap()
                : replaceMap;

        // TB_ALTEMP는 알림 상황과 템플릿 코드가 복합 PK이므로 두 값을 함께 조회 조건으로 사용함
        AlimDto.AlimTempDto tempReq = new AlimDto.AlimTempDto();
        // 알림 수신자의 계정 언어로 템플릿을 선택하도록 사용자 번호를 설정함
        tempReq.setUserNumb(userNumb);
        // AlimSitu 업무 값을 tempReq DTO에 설정함
        tempReq.setAlimSitu(alimSitu);
        // TempCode 업무 값을 tempReq DTO에 설정함
        tempReq.setTempCode(tempCode);
        // AlimTemp 데이터를 DB에서 조회함
        AlimDto.AlimTempDto temp = alimMapper.getAlimTemp(tempReq);

        // 사용 가능한 템플릿이 없으면 어떤 제목과 내용으로 발송해야 하는지 알 수 없으므로 알림을 저장하지 않음
        if (StringUtil.isEmpty(temp)) {
            // "조회 결과가 없어요."
            return ResultData.fail(ResultEnum.COMMON_NO_DATA);
        }

        // 알림 목록 항목을 담을 객체를 생성함
        AlimDto.AlimItemDto alim = new AlimDto.AlimItemDto();
        // UserNumb 업무 값을 alim DTO에 설정함
        alim.setUserNumb(userNumb);
        // AlimSitu 업무 값을 alim DTO에 설정함
        alim.setAlimSitu(alimSitu);
        // TempCode 업무 값을 alim DTO에 설정함
        alim.setTempCode(tempCode);
        // AlimTitl 업무 값을 alim DTO에 설정함
        alim.setAlimTitl(replaceTemplate(temp.getAlimTitl(), safeReplaceMap));
        // AlimCont 업무 값을 alim DTO에 설정함
        alim.setAlimCont(replaceTemplate(temp.getTempCont(), safeReplaceMap));
        // 클릭 시점의 관계와 공개 상태를 다시 판단할 이동 대상 유형을 설정함
        alim.setTagtType(tagtType);
        // 최종 이동 화면을 찾을 이동 대상 번호를 설정함
        alim.setTagtNumb(tagtNumb);
        // 채팅 읽음 범위와 댓글 강조 대상을 원본 유형에 따라 분리
        if (Constant.ALIM_TEMP_CODE_CLUB_CHAT_MESSAGE.equals(tempCode)) {
            alim.setChatNumb(messageNumb);
        } else {
            alim.setReplNumb(messageNumb);
        }
        // ReadYsno 업무 값을 alim DTO에 설정함
        alim.setReadYsno(Constant.COMM_NO);
        // DeltYsno 업무 값을 alim DTO에 설정함
        alim.setDeltYsno(Constant.COMM_NO);

        // 댓글은 등록 건마다 별도 이벤트이므로 같은 작성자와 독후감이어도 알림을 모두 저장함
        boolean isReplyReportAlim = Constant.ALIM_TEMP_CODE_REPLY_REPORT.equals(tempCode)
                || Constant.ALIM_TEMP_CODE_REPLY_PROFILE_IMAGE.equals(tempCode)
                || Constant.ALIM_TEMP_CODE_REPLY_BACKGROUND_IMAGE.equals(tempCode)
                || Constant.ALIM_TEMP_CODE_REPLY_TO_COMMENT.equals(tempCode)
                || Constant.ALIM_TEMP_CODE_CLUB_CHAT_MESSAGE.equals(tempCode);
        // 타이머 알림은 세션마다 별도 이벤트이므로 한 시간 안에도 각각 저장함
        boolean isBookTimerOverAlim = Constant.ALIM_TEMP_CODE_BOOK_TIMER_OVER.equals(tempCode);
        // 가입 신청과 즉시 가입 알림은 같은 모임에서 연속 발생해도 가입 이벤트마다 각각 저장함
        boolean isClubJoinEventAlim = Constant.ALIM_TEMP_CODE_CLUB_JOIN_REQUESTED.equals(tempCode)
                || Constant.ALIM_TEMP_CODE_CLUB_MEMBER_JOINED.equals(tempCode);

        // 좋아요와 팔로우처럼 반복 조작으로 발생할 수 있는 동일 알림만 1시간 동안 중복 차단함
        if (!isReplyReportAlim && !isBookTimerOverAlim && !isClubJoinEventAlim
                && alimMapper.dupSameAlimInHour(alim) > 0) {
            // 알림 수신자와 템플릿 식별값으로 TB_ALTEMP의 사용 가능한 템플릿을 찾고, #{key} 형식의 상용구를 Map 값으로 치환해 TB_ALIMXX에 저장 결과를 성공 응답으로 반환함
            return ResultData.success(alim);
        }

        // Alim 업무 값을 alimMapper DTO에 설정함
        alimMapper.setAlim(alim);
        // 생성된 사용자별 알림번호로 프론트엔드 해석 경로를 설정함
        alim.setLinkUrlx(ALIM_TARGET_ROUTE + alim.getAlimNumb());

        // 브라우저가 푸시 직후 미읽음 수를 조회해도 저장된 알림을 볼 수 있도록 DB commit 이후에 발송함
        schedulePushAfterCommit(alim);
        // 알림 수신자와 템플릿 식별값으로 TB_ALTEMP의 사용 가능한 템플릿을 찾고, #{key} 형식의 상용구를 Map 값으로 치환해 TB_ALIMXX에 저장 결과를 성공 응답으로 반환함
        return ResultData.success(alim);
    }

    /**
     * 알림 저장 트랜잭션이 완료된 뒤 FCM 푸시를 발송함
     * commit 전에 푸시를 보내면 열린 브라우저가 즉시 미읽음 수를 조회할 때 이전 값을 받을 수 있음
     *
     * @param alim 저장된 알림 정보
     */
    /**
     * 수신자의 전역 알림 설정을 템플릿 단위로 적용함
     * 가입 승인·거절·강제 퇴장과 타이머 종료는 업무상 필수 또는 명시적 신청 알림이라 설정 대상에서 제외함
     */
    private boolean isUserAlimEnabled(Long userNumb, String tempCode) {
        if (Constant.ALIM_TEMP_CODE_BOOK_TIMER_OVER.equals(tempCode)
                || Constant.ALIM_TEMP_CODE_CLUB_JOIN_APPROVED.equals(tempCode)
                || Constant.ALIM_TEMP_CODE_CLUB_JOIN_REJECTED.equals(tempCode)
                || Constant.ALIM_TEMP_CODE_CLUB_MEMBER_EXITED.equals(tempCode)) {
            return true;
        }

        UserSettingDto setting = alimMapper.getUserAlimSetting(userNumb);
        if (StringUtil.isEmpty(setting)) {
            return false;
        }

        if (Constant.ALIM_TEMP_CODE_LIKE_REPORT.equals(tempCode)
                || Constant.ALIM_TEMP_CODE_LIKE_PROFILE_IMAGE.equals(tempCode)
                || Constant.ALIM_TEMP_CODE_LIKE_BACKGROUND_IMAGE.equals(tempCode)
                || Constant.ALIM_TEMP_CODE_REPLY_LIKE.equals(tempCode)) {
            return Constant.COMM_YES.equals(setting.getLikeAlimYsno());
        }

        if (Constant.ALIM_TEMP_CODE_REPLY_REPORT.equals(tempCode)
                || Constant.ALIM_TEMP_CODE_REPLY_PROFILE_IMAGE.equals(tempCode)
                || Constant.ALIM_TEMP_CODE_REPLY_BACKGROUND_IMAGE.equals(tempCode)
                || Constant.ALIM_TEMP_CODE_REPLY_TO_COMMENT.equals(tempCode)) {
            return Constant.COMM_YES.equals(setting.getReplyAlimYsno());
        }

        if (Constant.ALIM_TEMP_CODE_FOLLOW_USER.equals(tempCode)) {
            return Constant.COMM_YES.equals(setting.getFollowAlimYsno());
        }

        if (Constant.ALIM_TEMP_CODE_INVITE_CLUB.equals(tempCode)
                || Constant.ALIM_TEMP_CODE_CLUB_JOIN_REQUESTED.equals(tempCode)
                || Constant.ALIM_TEMP_CODE_CLUB_MEMBER_JOINED.equals(tempCode)) {
            return Constant.COMM_YES.equals(setting.getClubAlimYsno());
        }

        if (Constant.ALIM_TEMP_CODE_CLUB_CHAT_MESSAGE.equals(tempCode)) {
            return Constant.COMM_YES.equals(setting.getChatAlimYsno());
        }

        if (Constant.ALIM_TEMP_CODE_REPORT_DATE_OVER.equals(tempCode)) {
            return Constant.COMM_YES.equals(setting.getReportDueAlimYsno());
        }

        // 새 템플릿은 별도 정책이 정해지기 전까지 기존 알림 동작을 유지함
        return true;
    }

    /**
     * 발신자와 수신자가 격리된 경우 알림 이력과 푸시를 모두 생성하지 않음
     *
     * @author HanWon.Jang
     * @param sendUserNumb 알림 발신자 번호
     * @param userNumb 알림 수신자 번호
     * @param alimSitu 알림 상황 코드
     * @param tempCode 알림 템플릿 코드
     * @param tagtType 이동 대상 유형
     * @param tagtNumb 이동 대상 번호
     * @param replyNumb 강조할 댓글 번호
     * @param replaceMap 템플릿 문구 치환값
     * @return 알림 저장 또는 차단 관계에 따른 정상 생략 결과
     */
    @Override
    @Transactional
    public ResultData sendUserAlim(Long sendUserNumb, Long userNumb, String alimSitu
                                  , String tempCode, String tagtType, Long tagtNumb
                                  , Long replyNumb, Map<String, Object> replaceMap) {
        // 발신자 정보가 없는 개인 알림 요청은 차단 판정을 우회하지 못하도록 거부함
        if (StringUtil.isEmpty(sendUserNumb)) {
            // "요청값이 올바르지 않아요."
            return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
        }

        // 어느 한쪽이라도 상대를 차단했으면 알림과 푸시를 사용자에게 알리지 않고 정상 생략함
        if (userBlockService.isBlocked(sendUserNumb, userNumb)) {
            // 차단 방향과 신원을 응답에 드러내지 않는 정상 생략 결과를 반환함
            return ResultData.success();
        }

        // 차단되지 않은 개인 소셜 알림은 기존 저장과 푸시 정책으로 처리함
        return sendAlim(userNumb, alimSitu, tempCode, tagtType, tagtNumb, replyNumb, replaceMap);
    }

    private void schedulePushAfterCommit(AlimDto.AlimItemDto alim) {

        Runnable sendPush = () -> {
            // 외부 연동이나 데이터 변환 실패를 예외 흐름으로 분리하기 위한 블록임
            try {
                // sendPush 업무 로직을 pushService에 위임함
                pushService.sendPush(
                        // getUserNumb 조회로 후속 처리에 필요한 데이터를 가져옴
                        alim.getUserNumb(),
                        // getAlimTitl 조회로 후속 처리에 필요한 데이터를 가져옴
                        alim.getAlimTitl(),
                        // getAlimCont 조회로 후속 처리에 필요한 데이터를 가져옴
                        alim.getAlimCont(),
                        // getLinkUrlx 조회로 후속 처리에 필요한 데이터를 가져옴
                        alim.getLinkUrlx(),
                        // getAlimNumb 조회로 후속 처리에 필요한 데이터를 가져옴
                        alim.getAlimNumb()
                );
            }

            // 예외 발생 시 기본값 보정 또는 공통 실패 흐름으로 전환함
            catch (RuntimeException e) {
                // 푸시는 부가 기능이므로 commit이 끝난 알림 저장 결과에는 영향을 주지 않음
                log.warn("FCM push send failed after notification commit. userNumb={}", alim.getUserNumb(), e);
            }
        };

        // 요청값이 업무에서 허용한 범위와 상태를 만족하는지 구분함
        if (TransactionSynchronizationManager.isSynchronizationActive()) {

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                /**
                 * 현재 트랜잭션이 커밋된 후 예약된 후처리를 실행함
                 *
                 * @author SeungHyeon.Kang
                 * @return 반환값이 없음
                 */
                @Override
                public void afterCommit() {
                    // 검증 대상 작업을 실행함
                    sendPush.run();
                }
            });

            // 현재 트랜잭션이 커밋된 후 예약된 후처리를 실행 결과를 반환함
            return;
        }

        // 검증 대상 작업을 실행함
        sendPush.run();
    }

    /**
     * 알림 원본 유형과 클릭 시점의 소유자 및 공개 상태로 최종 이동 경로를 계산함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 알림을 클릭한 로그인 사용자 번호
     * @param target 알림 원본 콘텐츠와 현재 관계 상태
     * @return 접근 가능한 내부 이동 경로이며 접근할 수 없으면 null
     */
    private String createTargetLink(Long userNumb, AlimDto.AlimTargetDto target) {
        // 개인 콘텐츠 알림은 클릭 시점에도 현재 소유자와의 양방향 차단 관계를 다시 검증함
        if (isContentTarget(target.getTagtType())
                && userBlockService.isBlocked(userNumb, target.getTargetUserNumb())) {
            // 차단 관계인 콘텐츠의 존재와 방향을 드러내지 않도록 이동 주소를 제공하지 않음
            return null;
        }

        // 사용자 알림은 클릭 시점에 대상과 양방향 차단 관계이면 프로필 이동을 허용하지 않음
        if (Constant.ALIM_TARGET_USER.equals(target.getTagtType())
                && userBlockService.isBlocked(userNumb, target.getTagtNumb())) {
            // 기존 알림은 보존하되 차단된 사용자 프로필 주소는 제공하지 않음
            return null;
        }

        // 독후감과 사진은 공통 콘텐츠 권한 계산으로 목적지를 결정함
        if (isContentTarget(target.getTagtType())) {
            // 현재 콘텐츠 소유자와 관계 상태가 반영된 경로를 반환함
            return createContentLink(userNumb, target);
        }

        // 팔로우 대상 사용자가 현재 활성 상태이면 프로필 화면으로 이동함
        if (Constant.ALIM_TARGET_USER.equals(target.getTagtType())
                && Constant.USER_STAT_ACTIVE.equals(target.getTargetUserStat())) {
            // 현재 활성 사용자의 프로필 경로를 반환함
            return "/social/profile/" + target.getTagtNumb();
        }

        // 모임 알림은 템플릿별 세부 화면과 현재 모임장 권한을 함께 계산함
        if (Constant.ALIM_TARGET_READING_CLUB.equals(target.getTagtType())) {
            // 모임 초대와 가입 상태에 대응하는 경로를 반환함
            return createClubLink(userNumb, target);
        }

        // 타이머 완료 알림은 대상 번호 없이 타이머 화면으로 이동함
        if (Constant.ALIM_TARGET_TIMER.equals(target.getTagtType())
                && Constant.ALIM_TEMP_CODE_BOOK_TIMER_OVER.equals(target.getTempCode())) {
            // 독서 타이머 경로를 반환함
            return "/timer";
        }

        // 지원하지 않는 대상 유형과 템플릿 조합에는 이동 주소를 제공하지 않음
        return null;
    }

    /**
     * 독후감과 사진 알림의 소유자 및 공개 상태를 기준으로 이동 경로를 계산함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 알림을 클릭한 로그인 사용자 번호
     * @param target 알림 콘텐츠와 현재 관계 상태
     * @return 접근 가능한 콘텐츠 이동 경로이며 접근할 수 없으면 null
     */
    private String createContentLink(Long userNumb, AlimDto.AlimTargetDto target) {
        // 원본 콘텐츠가 삭제되어 현재 소유자를 확인할 수 없으면 접근을 허용하지 않음
        if (StringUtil.isEmpty(target.getTargetUserNumb())) {
            // 삭제되거나 교체된 콘텐츠에는 이동 주소를 제공하지 않음
            return null;
        }

        boolean isOwner = userNumb.equals(target.getTargetUserNumb());

        // 독후감은 본인 여부와 현재 공개 및 팔로우 관계에 따라 세 화면 중 하나로 이동함
        if (Constant.LIKE_TARGET_REPORT.equals(target.getTagtType())) {
            // 작성자는 공개 상태와 관계없이 자신의 독후감 상세 화면으로 이동함
            if (isOwner) {
                // 본인 독후감 상세 경로에 필요하면 댓글 위치를 포함해 반환함
                return appendReplyTarget("/report/detail/" + target.getTagtNumb(), target.getReplNumb(), true);
            }

            // 타인의 독후감은 활성 작성자의 공개된 완료 또는 중단 독후감만 접근할 수 있음
            if (!Constant.USER_STAT_ACTIVE.equals(target.getTargetUserStat())
                    || !Constant.COMM_YES.equals(target.getPubcYsno())
                    || Constant.REPORT_STAT_READ.equals(target.getReptStat())) {
                // 현재 공개 독후감 접근 조건을 만족하지 않으면 이동 주소를 제공하지 않음
                return null;
            }

            // 현재 팔로우 관계가 유지되면 해당 독후감을 찾을 수 있는 피드로 이동함
            if (Constant.COMM_YES.equals(target.getFollowYsno())) {
                // 팔로우 독후감 피드 경로에 필요하면 댓글 위치를 포함해 반환함
                return appendFeedTarget(target);
            }

            // 비팔로우 사용자의 공개 독후감은 독후감 공개 대상 화면으로 이동함
            return appendReplyTarget(
                    "/report/public-reports/target/" + target.getTagtNumb(), target.getReplNumb(), false
            );
        }

        boolean isImageTarget = Constant.LIKE_TARGET_PROFILE_IMAGE.equals(target.getTagtType())
                || Constant.LIKE_TARGET_BACKGROUND_IMAGE.equals(target.getTagtType());

        // 현재 프로필 또는 배경사진은 활성 소유자의 공개 프로필 영역에서 팔로우 여부와 관계없이 접근할 수 있음
        if (isImageTarget && Constant.USER_STAT_ACTIVE.equals(target.getTargetUserStat())) {
            // 사진 소유자는 마이페이지에서 현재 사진 반응을 확인함
            if (isOwner) {
                // 본인 사진과 필요하면 강조 댓글을 포함한 마이페이지 경로를 반환함
                return appendImageTarget("/mypage/profile", target);
            }

            // 타인 사진과 필요하면 강조 댓글을 포함한 소셜 프로필 경로를 반환함
            return appendImageTarget("/social/profile/" + target.getTargetUserNumb(), target);
        }

        // 지원하지 않는 유형 또는 현재 관계로 볼 수 없는 사진에는 이동 주소를 제공하지 않음
        return null;
    }

    /**
     * 독서 모임 알림 템플릿을 내 모임 또는 멤버 관리 경로로 변환함
     * 멤버 관리 화면은 클릭 시점에도 현재 모임장인 수신자에게만 제공함
     *
     * @author SeungHyeon.Kang
     * @param userNumb 알림을 클릭한 로그인 사용자 번호
     * @param target 모임 번호와 템플릿 및 현재 모임장 정보
     * @return 현재 권한으로 접근 가능한 모임 경로이며 접근할 수 없으면 null
     */
    private String createClubLink(Long userNumb, AlimDto.AlimTargetDto target) {
        // 새 채팅 알림은 클릭 시점에도 활성 회원인 수신자만 해당 모임 채팅으로 이동함
        if (Constant.ALIM_TEMP_CODE_CLUB_CHAT_MESSAGE.equals(target.getTempCode())
                && userNumb.equals(target.getTargetUserNumb())
                && Constant.USER_STAT_ACTIVE.equals(target.getTargetUserStat())) {
            return "/reading-clubs/chat/" + target.getTagtNumb();
        }

        // 초대와 가입 처리 및 강제 퇴장 알림은 현재 관계가 바뀌어도 내 모임 상태를 확인하는 화면으로 이동함
        if (Constant.ALIM_TEMP_CODE_INVITE_CLUB.equals(target.getTempCode())
                || Constant.ALIM_TEMP_CODE_CLUB_JOIN_APPROVED.equals(target.getTempCode())
                || Constant.ALIM_TEMP_CODE_CLUB_JOIN_REJECTED.equals(target.getTempCode())
                || Constant.ALIM_TEMP_CODE_CLUB_MEMBER_EXITED.equals(target.getTempCode())) {
            // 받은 초대와 현재 모임 관계를 함께 확인할 수 있는 내 모임 경로를 반환함
            return "/reading-clubs/mine";
        }

        boolean isMemberManageAlim = Constant.ALIM_TEMP_CODE_CLUB_JOIN_REQUESTED.equals(target.getTempCode())
                || Constant.ALIM_TEMP_CODE_CLUB_MEMBER_JOINED.equals(target.getTempCode());

        // 신규 신청과 즉시 가입 알림의 멤버 관리 화면은 현재 모임장만 열 수 있음
        if (isMemberManageAlim && userNumb.equals(target.getTargetUserNumb())
                && Constant.USER_STAT_ACTIVE.equals(target.getTargetUserStat())) {
            // 현재 모임장의 멤버 관리 경로를 반환함
            return "/reading-clubs/manage/members/" + target.getTagtNumb();
        }

        // 지원하지 않는 모임 템플릿 또는 변경된 권한에는 이동 주소를 제공하지 않음
        return null;
    }

    /**
     * 알림 저장 전에 템플릿과 대상 유형 및 대상 번호 조합이 공통 라우터에서 처리 가능한지 확인함
     *
     * @author SeungHyeon.Kang
     * @param tempCode 알림 템플릿 코드
     * @param tagtType 이동 대상 유형
     * @param tagtNumb 이동 대상 번호
     * @return 공통 알림번호 라우터가 처리할 수 있는 조합 여부
     */
    private boolean isAlimTargetValid(String tempCode, String tagtType, Long tagtNumb) {
        // 타이머 완료 알림만 대상 번호 없이 고정 기능 화면으로 이동할 수 있음
        if (Constant.ALIM_TARGET_TIMER.equals(tagtType)) {
            // 타이머 대상 유형과 템플릿 조합 여부를 반환함
            return Constant.ALIM_TEMP_CODE_BOOK_TIMER_OVER.equals(tempCode)
                    && StringUtil.isEmpty(tagtNumb);
        }

        // 나머지 대상 유형은 조회와 URL 조합에 사용할 양수 식별번호가 필요함
        if (StringUtil.isEmpty(tagtNumb) || tagtNumb <= 0) {
            // 대상 번호가 없는 알림은 저장하지 않도록 거짓을 반환함
            return false;
        }

        // 콘텐츠 대상은 유형별로 허용된 좋아요와 댓글 및 독후감 기한 템플릿만 사용함
        if (isContentTarget(tagtType)) {
            // 콘텐츠 유형과 템플릿 조합 여부를 반환함
            return isContentTemplate(tagtType, tempCode);
        }

        // 사용자 프로필 대상은 팔로우 알림에만 사용함
        if (Constant.ALIM_TARGET_USER.equals(tagtType)) {
            // 팔로우 템플릿과 사용자 대상 조합 여부를 반환함
            return Constant.ALIM_TEMP_CODE_FOLLOW_USER.equals(tempCode);
        }

        // 독서 모임 대상은 등록된 여섯 가지 운영 알림에서 공통 사용함
        if (Constant.ALIM_TARGET_READING_CLUB.equals(tagtType)) {
            // 모임 템플릿 코드 지원 여부를 반환함
            return isClubTemplate(tempCode);
        }

        // 등록되지 않은 대상 유형은 알림 저장을 거부함
        return false;
    }

    /** 알림 콘텐츠 공통 권한 계산을 지원하는 대상 유형인지 확인함 */
    private boolean isContentTarget(String tagtType) {
        // 독후감과 현재 프로필 및 배경사진 대상 여부를 반환함
        return Constant.LIKE_TARGET_REPORT.equals(tagtType)
                || Constant.LIKE_TARGET_PROFILE_IMAGE.equals(tagtType)
                || Constant.LIKE_TARGET_BACKGROUND_IMAGE.equals(tagtType);
    }

    /** 콘텐츠 대상 유형과 알림 템플릿이 같은 업무 대상을 가리키는지 확인함 */
    private boolean isContentTemplate(String tagtType, String tempCode) {
        // 댓글 좋아요와 대댓글은 세 콘텐츠 유형에서 공통 템플릿을 사용함
        if (Constant.ALIM_TEMP_CODE_REPLY_LIKE.equals(tempCode)
                || Constant.ALIM_TEMP_CODE_REPLY_TO_COMMENT.equals(tempCode)) {
            // 공통 댓글 반응 템플릿은 모든 지원 콘텐츠 대상에서 사용할 수 있음
            return true;
        }

        // 독후감에는 독후감 좋아요와 댓글 및 기한 초과 템플릿만 허용함
        if (Constant.LIKE_TARGET_REPORT.equals(tagtType)) {
            // 독후감 대상별 템플릿 조합 여부를 반환함
            return Constant.ALIM_TEMP_CODE_LIKE_REPORT.equals(tempCode)
                    || Constant.ALIM_TEMP_CODE_REPLY_REPORT.equals(tempCode)
                    || Constant.ALIM_TEMP_CODE_REPORT_DATE_OVER.equals(tempCode);
        }

        // 프로필 사진에는 프로필 사진 좋아요와 댓글 템플릿만 허용함
        if (Constant.LIKE_TARGET_PROFILE_IMAGE.equals(tagtType)) {
            // 프로필 사진 대상별 템플릿 조합 여부를 반환함
            return Constant.ALIM_TEMP_CODE_LIKE_PROFILE_IMAGE.equals(tempCode)
                    || Constant.ALIM_TEMP_CODE_REPLY_PROFILE_IMAGE.equals(tempCode);
        }

        // 배경사진에는 배경사진 좋아요와 댓글 템플릿만 허용함
        if (Constant.LIKE_TARGET_BACKGROUND_IMAGE.equals(tagtType)) {
            // 배경사진 대상별 템플릿 조합 여부를 반환함
            return Constant.ALIM_TEMP_CODE_LIKE_BACKGROUND_IMAGE.equals(tempCode)
                    || Constant.ALIM_TEMP_CODE_REPLY_BACKGROUND_IMAGE.equals(tempCode);
        }

        // 지원하지 않는 콘텐츠와 템플릿 조합을 거부함
        return false;
    }

    /** 독서 모임 공통 라우터가 지원하는 템플릿 코드인지 확인함 */
    private boolean isClubTemplate(String tempCode) {
        // 초대와 가입 신청 및 처리와 강제 퇴장 템플릿 여부를 반환함
        return Constant.ALIM_TEMP_CODE_INVITE_CLUB.equals(tempCode)
                || Constant.ALIM_TEMP_CODE_CLUB_JOIN_APPROVED.equals(tempCode)
                || Constant.ALIM_TEMP_CODE_CLUB_JOIN_REJECTED.equals(tempCode)
                || Constant.ALIM_TEMP_CODE_CLUB_JOIN_REQUESTED.equals(tempCode)
                || Constant.ALIM_TEMP_CODE_CLUB_MEMBER_JOINED.equals(tempCode)
                || Constant.ALIM_TEMP_CODE_CLUB_MEMBER_EXITED.equals(tempCode)
                || Constant.ALIM_TEMP_CODE_CLUB_CHAT_MESSAGE.equals(tempCode);
    }

    /**
     * 피드가 특정 원본 콘텐츠와 댓글을 바로 찾을 수 있도록 숫자 식별값만 쿼리 파라미터로 조합함
     *
     * @author SeungHyeon.Kang
     * @param target 알림 원본 콘텐츠와 댓글 위치
     * @return 특정 피드 항목을 가리키는 내부 경로
     */
    private String appendFeedTarget(AlimDto.AlimTargetDto target) {
        String linkUrlx = "/feed?tagtType=" + target.getTagtType() + "&tagtNumb=" + target.getTagtNumb();

        // 댓글 번호가 있으면 피드 댓글 바텀시트가 해당 댓글을 강조할 수 있도록 포함함
        if (!StringUtil.isEmpty(target.getReplNumb()) && target.getReplNumb() > 0) {
            linkUrlx += "&replNumb=" + target.getReplNumb();
        }

        // 검증된 대상 유형과 숫자 식별값으로 만든 피드 내부 경로를 반환함
        return linkUrlx;
    }

    /**
     * 프로필 화면이 현재 사진과 강조 댓글을 바로 열 수 있도록 대상 식별값을 경로에 추가함
     *
     * @author SeungHyeon.Kang
     * @param baseLinkUrlx 본인 또는 다른 사용자 프로필 내부 경로
     * @param target 현재 사진 유형과 파일 번호 및 강조 댓글 번호
     * @return 현재 사진 반응을 가리키는 프로필 내부 경로
     */
    private String appendImageTarget(String baseLinkUrlx, AlimDto.AlimTargetDto target) {
        String linkUrlx = baseLinkUrlx + "?tagtType=" + target.getTagtType()
                + "&tagtNumb=" + target.getTagtNumb();

        // 댓글 번호가 있으면 프로필 사진 댓글 목록에서 해당 댓글을 강조하도록 포함함
        if (!StringUtil.isEmpty(target.getReplNumb()) && target.getReplNumb() > 0) {
            linkUrlx += "&replNumb=" + target.getReplNumb();
        }

        // 현재 사진과 댓글 위치가 포함된 프로필 내부 경로를 반환함
        return linkUrlx;
    }

    /**
     * 독후감 상세 또는 공개 대상 경로에 댓글 위치 쿼리를 추가함
     *
     * @author SeungHyeon.Kang
     * @param baseLinkUrlx 댓글 쿼리를 추가할 독후감 내부 경로
     * @param replyNumb 강조할 댓글 번호
     * @param showReplies 댓글 영역 자동 표시 여부를 별도 파라미터로 전달할지 여부
     * @return 필요하면 댓글 위치가 추가된 독후감 내부 경로
     */
    private String appendReplyTarget(String baseLinkUrlx, Long replyNumb, boolean showReplies) {
        // 댓글 번호가 없으면 독후감 화면만 이동하도록 기본 경로를 반환함
        if (StringUtil.isEmpty(replyNumb) || replyNumb <= 0) {
            // 댓글 위치가 없는 독후감 내부 경로를 반환함
            return baseLinkUrlx;
        }

        // 본인 독후감 상세 화면은 기존 화면 계약에 맞춰 댓글 표시 파라미터를 함께 전달함
        if (showReplies) {
            // 댓글 영역 표시와 강조 댓글 번호가 포함된 본인 독후감 경로를 반환함
            return baseLinkUrlx + "?showReplies=Y&replNumb=" + replyNumb;
        }

        // 공개 독후감 화면에 강조 댓글 번호를 포함한 경로를 반환함
        return baseLinkUrlx + "?replNumb=" + replyNumb;
    }

    /**
     * 템플릿 문구의 #{key} 상용구를 replaceMap의 값으로 치환함
     * replaceMap에는 수신자나 링크 같은 발송 제어값을 넣지 않고 화면에 표시될 문구 치환값만 넣음
     *
     * @author SeungHyeon.Kang
     * @param template 템플릿 원문
     * @param replaceMap 치환 값 Map
     * @return 치환 완료 문구
     */
    private String replaceTemplate(String template, Map<String, Object> replaceMap) {
        // template 값이 비어 있을 때 후속 참조를 차단하기 위한 분기임
        if (StringUtil.isEmpty(template)) {
            // 템플릿 문구의 #{key} 상용구를 replaceMap의 값으로 치환 결과를 반환함
            return "";
        }

        String replacedTemplate = template;

        // 목록 또는 문자열 항목을 누락 없이 순차 처리하기 위한 반복 블록임
        for (Map.Entry<String, Object> entry : replaceMap.entrySet()) {
            // entry.getKey( 값이 비어 있을 때 후속 참조를 차단하기 위한 분기임
            if (StringUtil.isEmpty(entry.getKey())) {

                continue;
            }

            // 대상 문자열에서 지정한 값을 치환함
            replacedTemplate = replacedTemplate.replace(
                    // 현재 항목의 키를 조회함
                    "#{" + entry.getKey() + "}",
                    // 필수 값이 비어 있는지 공통 기준으로 확인함
                    StringUtil.isEmpty(entry.getValue()) ? "" : String.valueOf(entry.getValue())
            );
        }

        // 템플릿 문구의 #{key} 상용구를 replaceMap의 값으로 치환 결과를 반환함
        return replacedTemplate;
    }

}
