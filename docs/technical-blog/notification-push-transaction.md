# 알림 저장과 FCM 발송 시점을 분리한 방법

## 문제

좋아요나 댓글이 저장되기 전에 푸시가 먼저 도착하면 사용자가 알림을 눌러도 아직 대상 데이터를 조회할 수 없습니다. 반대로 FCM 장애를 본 업무 트랜잭션의 실패로 취급하면 좋아요는 정상적으로 저장할 수 있는데도 사용자 요청 전체가 실패합니다.

Sadari는 알림센터에 표시할 데이터를 먼저 저장하고, 데이터베이스 커밋 이후에 푸시를 보냅니다. 좋아요는 본 업무 응답 속도와 더 분리하기 위해 트랜잭션 커밋 이벤트와 비동기 작업자를 사용합니다.

## 수신자와 템플릿 검증

`AlimServiceImpl.sendAlim`은 알림 한 건의 저장 계약을 담당합니다.

```java
@Override
@Transactional
public ResultData sendAlim(
        Long userNumb, String alimSitu, String tempCode, String tagtType,
        Long tagtNumb, Long replyNumb, Map<String, Object> replaceMap) {
    // 수신자와 템플릿 및 지원 대상 정보가 없으면 이동 불가능한 알림을 거부함
    if (StringUtil.hasEmpty(userNumb, alimSitu, tempCode, tagtType)
            || !isAlimTargetValid(tempCode, tagtType, tagtNumb)) {
        return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
    }

    // 탈퇴 또는 영구 삭제 대기 회원에게는 알림과 푸시를 만들지 않음
    if (alimMapper.getActiveAlimUserCnt(userNumb) == 0) {
        return ResultData.success();
    }

    // 사용자가 끈 선택형 알림은 알림센터 항목과 푸시를 함께 생략함
    if (!isUserAlimEnabled(userNumb, tempCode)) {
        return ResultData.success();
    }

    // 치환값이 없어도 호출부가 별도 빈 Map을 만들 필요가 없도록 보정함
    Map<String, Object> safeReplaceMap = StringUtil.isEmpty(replaceMap)
            ? Collections.emptyMap()
            : replaceMap;

    AlimDto.AlimTempDto tempReq = new AlimDto.AlimTempDto();
    // 수신자의 계정 언어로 템플릿을 선택함
    tempReq.setUserNumb(userNumb);
    tempReq.setAlimSitu(alimSitu);
    tempReq.setTempCode(tempCode);
    AlimDto.AlimTempDto temp = alimMapper.getAlimTemp(tempReq);

    // 번역된 제목과 본문을 만들 템플릿이 없으면 저장하지 않음
    if (StringUtil.isEmpty(temp)) {
        return ResultData.fail(ResultEnum.COMMON_NO_DATA);
    }
```

비활성 수신자와 꺼진 알림은 실패가 아니라 정상 생략으로 반환합니다. 호출한 좋아요·댓글 기능이 ‘알림을 받지 않기로 한 사용자’ 때문에 실패해서는 안 되기 때문입니다. 반면 필수 식별값이나 템플릿 누락은 개발·데이터 오류일 수 있어 실패 결과로 구분합니다.

## 언어별 템플릿을 완성된 알림으로 저장하기

```java
    AlimDto.AlimItemDto alim = new AlimDto.AlimItemDto();
    alim.setUserNumb(userNumb);
    alim.setAlimSitu(alimSitu);
    alim.setTempCode(tempCode);
    // #{key} 자리에 호출부가 전달한 검증된 표시값을 치환함
    alim.setAlimTitl(replaceTemplate(temp.getAlimTitl(), safeReplaceMap));
    alim.setAlimCont(replaceTemplate(temp.getTempCont(), safeReplaceMap));
    // 클릭 시점에 공개 범위와 관계를 다시 판단할 대상 정보를 저장함
    alim.setTagtType(tagtType);
    alim.setTagtNumb(tagtNumb);
    alim.setReplNumb(replyNumb);
    alim.setReadYsno(Constant.COMM_NO);
    alim.setDeltYsno(Constant.COMM_NO);

    // 댓글·타이머·가입은 발생 건마다 의미가 있으므로 중복 제거에서 제외함
    boolean isReplyReportAlim =
            Constant.ALIM_TEMP_CODE_REPLY_REPORT.equals(tempCode)
            || Constant.ALIM_TEMP_CODE_REPLY_TO_COMMENT.equals(tempCode)
            || Constant.ALIM_TEMP_CODE_CLUB_CHAT_MESSAGE.equals(tempCode);
    boolean isBookTimerOverAlim =
            Constant.ALIM_TEMP_CODE_BOOK_TIMER_OVER.equals(tempCode);
    boolean isClubJoinEventAlim =
            Constant.ALIM_TEMP_CODE_CLUB_JOIN_REQUESTED.equals(tempCode)
            || Constant.ALIM_TEMP_CODE_CLUB_MEMBER_JOINED.equals(tempCode);

    // 좋아요·팔로우처럼 반복 조작 가능한 동일 알림만 한 시간 중복 차단함
    if (!isReplyReportAlim && !isBookTimerOverAlim && !isClubJoinEventAlim
            && alimMapper.dupSameAlimInHour(alim) > 0) {
        return ResultData.success(alim);
    }

    alimMapper.setAlim(alim);
    // 프론트엔드는 알림 번호로 현재 접근 가능한 최종 화면을 해석함
    alim.setLinkUrlx(ALIM_TARGET_ROUTE + alim.getAlimNumb());

    // 커밋 뒤에만 브라우저 푸시를 발송함
    schedulePushAfterCommit(alim);
    return ResultData.success(alim);
}
```

알림에는 렌더링 전 템플릿이 아니라 치환이 끝난 제목과 본문을 저장합니다. 사용자의 언어 설정이나 닉네임이 나중에 바뀌어도 ‘당시 받은 알림’ 문구는 유지됩니다.

중복 정책을 한 규칙으로 통일하지 않은 이유도 코드에 드러납니다. 좋아요를 취소했다가 다시 누른 알림은 짧은 시간에 반복될 수 있지만, 댓글 두 건과 독서 타이머 두 세션은 각각 별도 사건입니다. 같은 중복 제거를 적용하면 실제 사건을 잃습니다.

## 좋아요 커밋 이후 이벤트

좋아요 서비스는 알림 세부 구현을 직접 실행하지 않고 이벤트를 발행합니다. 리스너는 트랜잭션이 실제로 커밋된 경우에만 호출됩니다.

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class LikeAlimListener {

    // 좋아요 응답 스레드와 분리하여 알림을 처리할 작업자
    private final LikeAlimWorker likeAlimWorker;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLikeAlim(LikeAlimEvent event) {
        // 잘못된 이벤트는 비동기 실행기에 전달하지 않음
        if (event == null) {
            return;
        }

        try {
            // 좋아요 알림 저장과 푸시 처리를 별도 실행 스레드에 위임함
            likeAlimWorker.sendLikeAlim(event);
        }
        catch (RuntimeException e) {
            // 실행기 포화가 이미 커밋된 좋아요 결과를 되돌리지 않게 함
            log.warn("Like notification dispatch failed. sender={}, target={}",
                    event.getSendUserNumb(), event.getTargetUserNumb(), e);
        }
    }
}
```

`AFTER_COMMIT`이 없으면 좋아요 저장이 뒤에서 롤백되어도 알림 작업이 실행될 수 있습니다. 리스너의 `try/catch`는 비동기 실행기 자체가 작업을 받지 못한 경우까지 본 업무와 분리합니다.

## 비동기 작업자

```java
@Async
public void sendLikeAlim(LikeAlimEvent event) {
    // 필수 식별값이 없으면 알림 저장소에 접근하지 않음
    if (StringUtil.isEmpty(event)
            || StringUtil.hasEmpty(event.getSendUserNumb(),
                    event.getTargetUserNumb(), event.getTempCode())) {
        return;
    }

    try {
        String sendUserNick = event.getSendUserNick();

        // 이벤트에 닉네임이 없으면 로그인 캐시에서 조회함
        if (StringUtil.isEmpty(sendUserNick)) {
            sendUserNick = tokenRedisService.getUserNick(
                    event.getSendUserNumb());
        }

        // 치환값을 만들 수 없으면 미완성 알림을 저장하지 않음
        if (StringUtil.isEmpty(sendUserNick)) {
            return;
        }

        Map<String, Object> replaceMap = new HashMap<>();
        replaceMap.put("userName", sendUserNick);

        // 좋아요와 분리된 알림 트랜잭션에서 저장과 푸시 예약을 처리함
        alimService.sendUserAlim(
                event.getSendUserNumb(), event.getTargetUserNumb(),
                Constant.ALIM_SITU_LIKE, event.getTempCode(),
                event.getTagtType(), event.getTagtNumb(),
                event.getReplyNumb(), replaceMap);
    }
    catch (RuntimeException e) {
        // 알림 저장·Redis·FCM 실패는 좋아요 완료 결과와 분리함
        log.warn("Like notification processing failed. sender={}, target={}",
                event.getSendUserNumb(), event.getTargetUserNumb(), e);
    }
}
```

작업자는 `void`를 반환합니다. HTTP 요청은 이미 좋아요 성공으로 끝났기 때문에 알림 결과를 사용자 응답에 합칠 수 없습니다. 실패는 로그로 남지만 자동 재처리 큐는 없습니다.

## 보장 범위와 한계

현재 구조가 보장하는 것은 ‘커밋되지 않은 좋아요에 푸시를 보내지 않는다’와 ‘푸시 장애가 좋아요를 실패시키지 않는다’입니다. 데이터베이스 커밋 직후 프로세스가 종료되면 메모리 이벤트는 사라질 수 있어 알림 전달을 정확히 한 번 보장하지는 않습니다. 알림 유실까지 복구해야 하는 규모가 되면 본 트랜잭션에 이벤트 레코드를 함께 저장하고 별도 작업자가 전달하는 아웃박스 패턴이 필요합니다.

## 관련 소스

- [AlimServiceImpl.java](../../src/main/java/org/our/sadari/alim/service/AlimServiceImpl.java)
- [LikeAlimListener.java](../../src/main/java/org/our/sadari/alim/event/LikeAlimListener.java)
- [LikeAlimWorker.java](../../src/main/java/org/our/sadari/alim/event/LikeAlimWorker.java)
- [알림·푸시 정책](../policies/notification-push-policy.md)
