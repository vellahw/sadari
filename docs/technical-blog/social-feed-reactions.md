# 팔로우·좋아요·댓글을 공개 범위와 함께 설계한 방법

## 문제

소셜 기능은 버튼 하나를 토글하는 것처럼 보이지만 저장 전에 확인해야 할 조건이 많습니다. 자기 자신을 팔로우하면 안 되고, 차단된 사용자 사이에 새 관계가 생기면 안 됩니다. 좋아요는 화면이 보낸 대상 유형을 그대로 믿을 수 없고, 댓글은 현재 접근 가능한 콘텐츠인지 다시 검사해야 합니다.

Sadari는 관계를 저장하는 서비스에서 권한과 현재 상태를 재검증합니다. 피드 조회 역시 로그인 사용자를 기준으로 공개 범위를 적용하고, 페이지당 한 건을 더 읽어 다음 페이지 유무를 계산합니다.

## 팔로우와 차단의 경쟁 조건

`SocialServiceImpl.setFollow`는 팔로우 저장 전에 두 사용자 행을 일정한 순서로 잠급니다.

```java
@Override
@Transactional
public ResultData setFollow(SocialDto.FollowDto req) {
    // 유효한 사용자 번호가 있으면 차단 등록과 같은 순서로 사용자 쌍을 잠금
    if (!StringUtil.isEmpty(req)
            && !StringUtil.hasEmpty(req.getUserNumb(), req.getFlowNumb())
            && !req.getUserNumb().equals(req.getFlowNumb())) {
        // 차단 완료 뒤 팔로우 관계가 남는 경쟁 조건을 막음
        userBlockService.lockUsers(req.getUserNumb(), req.getFlowNumb());
    }

    // 자기 자신, 비활성 사용자, 차단 관계를 저장 전에 검증함
    ResultData invalidResult = validateFollowUsers(req);
    if (!StringUtil.isEmpty(invalidResult)) {
        return invalidResult;
    }

    // 중복 관계는 새 행을 만들지 않는 Mapper 계약을 사용함
    int insertCnt = socialMapper.setFollow(req);

    // 실제로 새 관계가 만들어진 경우에만 알림을 발송함
    if (insertCnt > 0) {
        sendFollowAlim(req);
    }

    // 저장 뒤 계산한 버튼 상태를 반환함
    return ResultData.success(
            createFollowStatus(socialMapper.getFollowStatusName(req)));
}
```

첫 번째 `if`는 검증이 아니라 잠금 대상이 성립하는지 확인하는 방어 코드입니다. 두 사용자를 잠근 뒤 차단 여부를 검사하므로, 한 요청이 팔로우를 추가하는 동안 다른 요청이 차단을 추가해 두 관계가 동시에 남는 상황을 줄입니다. `lockUsers`는 사용자 번호를 같은 순서로 잠가 서로 반대 방향 요청이 교착 상태를 만들 가능성도 낮춥니다.

`insertCnt > 0`은 알림 중복을 막는 경계입니다. 이미 팔로우한 상태에서 같은 요청이 다시 들어오면 최종 버튼 상태는 성공으로 반환하되 새 알림은 만들지 않습니다. 사용자의 재시도는 멱등하게 처리하고, 상대방에게는 실제 상태 변화만 알립니다.

## 좋아요 토글

좋아요는 대상이 존재한다는 것만으로 허용하지 않습니다. 공개 독후감과 현재 프로필·배경 이미지처럼 서비스가 지원하는 대상만 `validateLikeTarget`을 통과합니다.

```java
@Override
@Transactional
public ResultData setLike(SocialDto.LikeDto req) {
    // 대상 유형, 대상 번호, 공개 범위와 접근 권한을 검증함
    ResultData invalidResult = validateLikeTarget(req);
    if (!StringUtil.isEmpty(invalidResult)) {
        return invalidResult;
    }

    // 기존 좋아요가 있으면 취소함
    if (socialMapper.dupLike(req) > 0) {
        socialMapper.delLike(req);
    }
    else {
        // 기존 좋아요가 없으면 새 관계를 저장함
        socialMapper.setLike(req);
        // 좋아요 커밋 이후에만 알림을 처리하도록 후처리 이벤트를 등록함
        sendLikeAlim(req);
    }

    // 변경 뒤의 개수와 내 좋아요 여부를 다시 조회해 반환함
    return ResultData.success(socialMapper.getLikeDtl(req));
}
```

좋아요 취소에는 알림이 없고 등록에만 알림 이벤트가 있습니다. 알림을 같은 트랜잭션에서 즉시 외부 푸시로 보내지 않고 커밋 이후 이벤트로 넘기는 이유는 [알림 글](notification-push-transaction.md)에서 별도로 다룹니다.

현재 구현은 ‘조회 후 삭제 또는 등록’ 구조입니다. 저장소의 중복 방지 제약이 최종 중복을 막더라도 같은 사용자의 동시 토글 두 건이 사용자가 예상한 순서와 다르게 끝날 수 있습니다. 빠른 연속 클릭은 프론트엔드의 처리 중 상태로 줄이고 있지만, 여러 기기까지 완전히 직렬화하려면 대상 관계 잠금이나 원자적 토글 계약을 추가해야 합니다.

## 댓글 입력 검증

댓글은 화면에서 이미 본 대상이라도 등록 시점에 접근 가능성을 다시 확인합니다.

```java
@Override
@Transactional
public ResultData setReply(Long userNumb, ReplyDto replyDto) {
    // 인증 사용자 번호가 없으면 작성자를 특정할 수 없으므로 등록을 중단함
    if (StringUtil.isEmpty(userNumb)) {
        return ResultData.fail(ResultEnum.AUTH_FAIL);
    }

    normalizeReplyTarget(replyDto);

    // 대상 유형과 번호 및 댓글 내용은 모두 필수임
    if (StringUtil.isEmpty(replyDto) || StringUtil.isEmpty(replyDto.getTagtType())
            || StringUtil.isEmpty(replyDto.getTagtNumb())
            || StringUtil.isEmpty(replyDto.getReplCntn())) {
        return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
    }

    // 사진 댓글은 활성 사용자의 현재 사진인지 API 경계에서 다시 검증함
    if (!hasImageReplyAccess(
            userNumb, replyDto.getTagtType(), replyDto.getTagtNumb())) {
        return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
    }

    // 공백만 있는 댓글을 저장하지 않도록 평문을 정규화함
    String normalizedContent =
            StringUtil.normalizePlainText(replyDto.getReplCntn());
    // 문자 수가 아니라 실제 저장 기준인 UTF-8 바이트 길이를 검사함
    if (StringUtil.isEmpty(normalizedContent)
            || XssUtil.utf8ByteLength(normalizedContent) > REPLY_CONTENT_MAX_BYTES) {
        return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
    }

    // 부모 댓글 번호가 있다면 양수만 허용함
    if (!StringUtil.isEmpty(replyDto.getUperNumb())
            && replyDto.getUperNumb() <= 0) {
        return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
    }

    // 정규화한 내용에서 비속어를 검사함
    Optional<String> badWord =
            badWordDetectionService.findBadWord(normalizedContent);
    if (badWord.isPresent()) {
        return ResultData.fail(
                ResultEnum.COMMON_BAD_WORD_INCLUDED, badWord.get());
    }

    // 작성자는 인증 정보로 덮고 검증된 본문만 저장함
    replyDto.setUserNumb(userNumb);
    replyDto.setReplCntn(normalizedContent);
    replyDto.setDeltYsno(Constant.COMM_NO);

    int insertCnt = replyMapper.setReply(replyDto);
    if (insertCnt == 0) {
        return ResultData.fail(ResultEnum.COMMON_SAVE_REJECTED);
    }

    sendReplyTargetAlim(userNumb, replyDto);
    return ResultData.success(replyDto.getReplNumb());
}
```

검증 순서에는 비용 차이가 반영되어 있습니다. 빈 값과 길이를 먼저 검사한 뒤 비속어 자동자를 실행하고, 모든 검증이 끝난 후에만 저장합니다. 작성자 번호도 요청 본문이 아니라 인증 정보로 설정합니다. 댓글 저장과 알림 저장은 트랜잭션 안에서 처리되므로 알림이 필수인 경로는 실패 시 댓글만 남지 않게 해야 합니다.

## 피드 페이지 계산

피드는 전체 개수를 매번 세지 않고 요청 크기보다 한 건 더 조회합니다.

```java
public ResultData getFeedList(Long userNumb, int page) {
    if (StringUtil.isEmpty(userNumb)) {
        return ResultData.fail(ResultEnum.AUTH_FAIL);
    }

    int safePage = Math.max(page, 1);
    FeedDto request = new FeedDto();
    request.setLoginUserNumb(userNumb);
    request.setPageOffset((safePage - 1) * FEED_PAGE_SIZE);
    // 다음 페이지 존재 여부를 판정하기 위해 화면 표시 수보다 한 건 더 조회함
    request.setPageLimit(FEED_PAGE_SIZE + 1);

    List<FeedDto> result = feedMapper.getFeedList(request);
    boolean hasNext = result.size() > FEED_PAGE_SIZE;
    List<FeedDto> visibleList = hasNext
            ? new ArrayList<>(result.subList(0, FEED_PAGE_SIZE))
            : result;

    // 현재 표시 언어와 번역 한도로 각 독후감 카드의 버튼 여부를 계산함
    reportTranslationService.applyFeedAvailability(visibleList);
    return ResultData.success(new PageDto<>(visibleList, safePage, hasNext));
}
```

`FEED_PAGE_SIZE + 1`번째 항목은 화면에 보내지 않고 `hasNext` 계산에만 사용합니다. 별도의 전체 건수 쿼리가 필요 없다는 장점이 있습니다. 다만 오프셋 페이지 방식이므로 데이터가 매우 많아지면 뒤 페이지 비용이 커지고, 조회 중 새 글이 들어오면 항목이 겹치거나 건너뛸 수 있습니다. 그 시점에는 최신 활동 시각과 식별자를 함께 쓰는 커서 페이지로 전환할 수 있습니다.

## 관련 소스

- [SocialServiceImpl.java](../../src/main/java/org/our/sadari/social/service/SocialServiceImpl.java)
- [ReplyServiceImpl.java](../../src/main/java/org/our/sadari/reply/service/ReplyServiceImpl.java)
- [FeedServiceImpl.java](../../src/main/java/org/our/sadari/feed/service/FeedServiceImpl.java)
- [ReportListView.tsx](../../src/main/frontend/src/components/ReportList/ReportListView.tsx)
