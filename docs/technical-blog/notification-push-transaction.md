# 알림 저장과 FCM 발송 시점을 분리한 방법

## 시작한 문제

알림 데이터를 저장하기 전에 FCM 푸시를 보내면 사용자가 푸시를 누른 순간 아직 알림이 조회되지 않을 수 있다. 반대로 외부 푸시 실패를 DB 트랜잭션 실패로 취급하면 좋아요·댓글·가입 같은 원래 업무까지 롤백될 수 있다.

같은 팔로우나 좋아요를 반복 조작할 때 알림이 계속 쌓이는 문제도 있었지만, 댓글과 타이머처럼 매번 의미가 다른 이벤트까지 일괄 중복 제거하면 안 됐다.

## 먼저 알림을 확정했다

`AlimServiceImpl.sendAlim`은 수신자의 계정 상태와 알림 설정을 확인하고, 사용자 언어에 맞는 템플릿을 조회한다. 템플릿의 치환값을 서버에서 채운 뒤 알림을 저장하고 생성된 알림 번호를 내부 이동 경로에 사용한다.

```java
alim.setAlimTitl(replaceTemplate(template.getAlimTitl(), values));
alim.setAlimCont(replaceTemplate(template.getTempCont(), values));
alimMapper.setAlim(alim);

schedulePushAfterCommit(alim);
```

푸시는 트랜잭션 커밋 이후에만 실행한다. 브라우저가 푸시 직후 미읽음 수와 알림 대상을 조회해도 커밋된 데이터를 볼 수 있게 하기 위해서다. 좋아요는 원래 트랜잭션이 커밋된 후 이벤트를 발행하고 별도 비동기 작업이 알림 저장과 푸시를 처리한다.

## 중복 기준을 업무별로 나눴다

좋아요와 팔로우처럼 토글이나 반복 조작으로 같은 내용이 발생할 수 있는 알림은 한 시간 동안 동일 알림을 차단한다. 댓글·답글, 모임 채팅, 타이머 종료, 가입 신청과 즉시 가입은 각각 독립 사건이므로 중복 방지에서 제외한다.

수신자가 비활성화 또는 삭제 대기 상태이면 새 알림을 만들지 않는다. 선택형 알림이 꺼져 있어도 알림센터와 푸시를 함께 생략한다. 반면 가입 승인·거절, 강제 퇴장, 타이머 종료처럼 업무 결과를 알려야 하는 항목은 필수 알림으로 분리했다.

알림 클릭 시에는 저장 당시 URL을 그대로 신뢰하지 않는다. 인증 사용자와 알림 번호로 소유권을 확인하고, 현재 콘텐츠 공개 여부와 팔로우·차단 관계를 다시 조회해 최종 내부 경로를 계산한다. 콘텐츠가 삭제되거나 관계가 바뀌었으면 이동을 거절한다.

## 선택의 한계

커밋 이후 비동기 실행은 원래 업무의 응답 시간을 보호하지만, 실행기 포화나 프로세스 종료 시 부가 알림이 생략될 수 있다. 현재는 원래 반응의 정합성을 우선했다. 반드시 전달해야 하는 알림까지 범위가 넓어지면 DB Outbox와 재시도 상태를 알림 도메인에도 적용해야 한다.

## 구현 근거

- [AlimServiceImpl.java](../../src/main/java/org/our/sadari/alim/service/AlimServiceImpl.java)
- [LikeAlimListener.java](../../src/main/java/org/our/sadari/alim/event/LikeAlimListener.java)
- [LikeAlimWorker.java](../../src/main/java/org/our/sadari/alim/event/LikeAlimWorker.java)
- [PushServiceImpl.java](../../src/main/java/org/our/sadari/push/service/PushServiceImpl.java)
- [알림과 스케줄러](../portfolio/notification-scheduler.md)
