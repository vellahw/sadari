# 팔로우·좋아요·댓글을 공개 범위와 함께 설계한 방법

## 시작한 문제

소셜 기능은 관계 한 건을 저장하는 것보다 “지금 이 사용자가 이 대상에 반응할 수 있는가”를 판단하는 일이 더 중요했다. 독후감이 비공개로 바뀌거나 프로필 사진이 교체됐는데 예전 번호로 좋아요를 보낼 수 있고, 차단과 팔로우 요청이 동시에 들어오면 차단 이후에도 관계가 남을 수 있다.

본인 마이페이지와 다른 사용자 프로필이 같은 집계를 사용하되 공개 범위는 달라야 했고, 좋아요와 댓글 알림 실패가 원래 반응 저장까지 실패시키면 안 됐다.

## 대상 번호만 믿지 않았다

좋아요 요청은 대상 유형을 허용 목록으로 제한한다. 이후 현재 공개 독후감인지, 현재 사용 중인 프로필·배경 사진인지 서버에서 다시 조회한다. 작성자와 알림 설정도 요청값이 아니라 조회한 원본에서 가져온다.

```java
boolean report = LIKE_TARGET_REPORT.equals(request.getTagtType());
boolean profile = LIKE_TARGET_PROFILE_IMAGE.equals(request.getTagtType());
boolean background = LIKE_TARGET_BACKGROUND_IMAGE.equals(request.getTagtType());

if (!report && !profile && !background) {
    return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
}

LikeDto target = report
        ? reportMapper.getReportLikeDtl(request)
        : feedMapper.getImageLikeTarget(request);
```

현재 대상이 아니거나 양방향 차단 관계이면 반응을 거절한다. 댓글도 같은 방식으로 대상 유형과 현재 접근 가능성을 확인하고, 인증 사용자 번호를 작성자로 덮어쓴 뒤 평문 정규화·UTF-8 길이·비속어 검증을 통과한 내용만 저장한다.

## 관계 경쟁과 공개 범위

팔로우 등록은 중복 요청이 와도 같은 관계 하나로 수렴하도록 저장한다. 차단과 팔로우가 동시에 실행될 때는 두 사용자 식별자를 항상 같은 순서로 잠근다. 차단 트랜잭션은 양방향 팔로우, 처리 중인 모임 초대와 가입 신청도 정리해 이후 개인 관계가 남지 않게 한다.

피드는 로그인 사용자 본인과 팔로잉 사용자의 현재 공개 활동만 최신순으로 조회한다. 다음 페이지 판정을 위해 화면 크기보다 한 건 더 조회하고, 응답에서는 그 한 건을 제거한다. 알림에서 직접 진입하는 단건 조회도 같은 공개·차단 정책을 다시 적용한다.

프로필 통계는 본인 화면에서는 전체 독후감, 타인 화면에서는 공개 독후감만 집계한다. 클라이언트가 공개 여부를 선택하게 하지 않고 Controller와 Service가 조회 관계에 따라 범위를 정한다.

## 반응과 알림을 분리했다

좋아요가 새로 저장되면 커밋 이후 이벤트만 발행한다. 별도 비동기 작업이 알림 템플릿을 처리하며, Redis·알림 저장·FCM 실패는 로그로 격리한다. 따라서 알림 시스템의 일시 장애가 이미 유효한 좋아요를 롤백시키지 않는다.

이 구조의 대가는 알림이 즉시 도착하지 않거나 드물게 생략될 수 있다는 점이다. 현재는 사용자 반응의 정합성을 우선한 선택이며, 전달 보장이 더 중요해지면 Outbox 기반 재시도를 적용할 수 있다.

## 구현 근거

- [SocialServiceImpl.java](../../src/main/java/org/our/sadari/social/service/SocialServiceImpl.java)
- [ReplyServiceImpl.java](../../src/main/java/org/our/sadari/reply/service/ReplyServiceImpl.java)
- [FeedServiceImpl.java](../../src/main/java/org/our/sadari/feed/service/FeedServiceImpl.java)
- [UserBlockServiceImpl.java](../../src/main/java/org/our/sadari/social/service/UserBlockServiceImpl.java)
