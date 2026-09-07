# 독서 모임 가입 경쟁을 행 잠금처리

## 문제

정원이 한 자리 남은 모임에 두 사용자가 동시에 가입하면 두 요청 모두 ‘현재 인원 9명, 정원 10명’을 읽을 수 있습니다. 두 요청이 각각 한 명을 추가하면 정원은 11명이 됩니다. 초대도 좌석을 예약하지 않으면 초대받은 사용자가 수락할 때 이미 자리가 없어지는 문제가 생깁니다.

Sadari는 가입·초대·수락처럼 정원에 영향을 주는 경로에서 먼저 모임을 잠금 조회합니다. 잠금을 얻은 트랜잭션만 만료 초대를 정리하고 점유 좌석을 다시 계산합니다.

## 즉시 가입과 승인 가입

`ReadingClubServiceImpl.setJoin`은 가입 방식이 갈라지기 전까지 공통 검증을 수행합니다.

```java
@Override
@Transactional
public ResultData setJoin(
        Long userNumb, Long clubNumb, ReadingClubDto.JoinReqDto request) {
    // 가입 대상과 사용자 식별값을 검증함
    if (StringUtil.hasEmpty(userNumb, clubNumb, request)) {
        return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
    }

    // 정원과 중복 관계를 같은 트랜잭션에서 판단하도록 모임 행을 잠금
    ReadingClubDto.ClubViewDto club =
            readingClubMapper.getClubForUpdate(clubNumb);
    // 공개 운영 중이며 모집 중인 모임만 가입할 수 있음
    if (StringUtil.isEmpty(club) || !CLUB_ACTIVE.equals(club.getClubStat())
            || !CLUB_PUBLIC.equals(club.getClubVisb())
            || Constant.COMM_NO.equals(club.getRcrtYsno())) {
        return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
    }

    // 가입 사용자와 모임장이 차단 관계이면 두 가입 방식을 모두 막음
    if (userBlockService.isBlocked(userNumb, club.getOwnrNumb())) {
        return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
    }

    ReadingClubDto.MemberDto member =
            readingClubMapper.getClubMember(clubNumb, userNumb);
    // 현재 회원, 유효 초대, 재가입 제한 관계는 중복 가입을 허용하지 않음
    if (!canJoinAgain(member)) {
        return ResultData.fail(ResultEnum.COMMON_SAVE_REJECTED);
    }

    if (JOIN_OPEN.equals(club.getJoinType())) {
        // 만료 초대를 지워 좌석 집계를 최신화함
        readingClubMapper.delExpiredInvitation(clubNumb);
        // 활성 회원과 유효 예약석을 합친 점유 수를 다시 검사함
        if (readingClubMapper.getOccupiedSeatCnt(clubNumb) >= club.getMaxxMemb()) {
            return ResultData.fail(ResultEnum.COMMON_SAVE_REJECTED);
        }

        // 신규 등록 또는 자진 탈퇴 관계 재활성화가 반영되어야 함
        if (readingClubMapper.setActiveMember(clubNumb, userNumb) < 1) {
            throw new CustomException(
                    ResultEnum.COMMON_SAVE_REJECTED,
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ResultData alimResult = alimService.sendAlim(
                club.getOwnrNumb(),
                Constant.ALIM_SITU_FOLLOW_CLUB,
                Constant.ALIM_TEMP_CODE_CLUB_MEMBER_JOINED,
                Constant.ALIM_TARGET_READING_CLUB,
                clubNumb,
                null,
                Map.of("clubName", club.getClubName()));
        // 알림 저장 실패 시 회원 관계만 확정되지 않도록 전체를 롤백함
        if (StringUtil.isEmpty(alimResult)
                || alimResult.getCode() != RESULT_SUCCESS_CODE) {
            throw new CustomException(
                    ResultEnum.COMMON_SAVE_REJECTED,
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return getClubDtl(userNumb, clubNumb);
    }
```

핵심은 `getClubForUpdate`가 점유 수 조회보다 앞에 있다는 점입니다. 첫 요청이 잠금을 잡으면 두 번째 요청은 기다립니다. 첫 요청이 회원을 추가하고 커밋한 뒤 두 번째 요청이 새 점유 수를 읽으므로 마지막 좌석을 동시에 차지할 수 없습니다.

알림 실패에서 단순 실패 객체를 반환하지 않고 예외를 던지는 이유도 트랜잭션 때문입니다. 회원 저장 뒤 정상 반환하면 트랜잭션은 커밋됩니다. 예외를 던져야 즉시 가입과 알림 저장이 함께 롤백됩니다.

승인 가입은 같은 잠금과 공통 검증 뒤 질문·답변을 확인합니다.

```java
    // 승인 가입 이외 방식은 공개 페이지 직접 가입을 허용하지 않음
    if (!JOIN_APPROVAL.equals(club.getJoinType())) {
        return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
    }

    // 처리 중 신청 또는 거절 뒤 재신청 제한 기간에는 신청하지 못함
    if (!StringUtil.isEmpty(
            readingClubMapper.getBlockedApplication(clubNumb, userNumb))) {
        return ResultData.fail(ResultEnum.COMMON_SAVE_REJECTED);
    }

    ReadingClubDto.QuestionDto question =
            readingClubMapper.getClubQuestion(clubNumb);
    List<String> questions = toQuestionList(question);
    List<String> answers = normalizeTextList(request.getAnswerList(), 2000);
    // 현재 질문 수와 답변 수가 같고 모든 답변이 있어야 함
    if (questions.isEmpty() || questions.size() != answers.size()
            || hasEmptyText(answers)) {
        return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
    }

    for (String answer : answers) {
        // 한 답변이라도 비속어가 있으면 전체 신청을 거절함
        if (badWordDetectionService.findBadWord(answer).isPresent()) {
            return ResultData.fail(
                    ResultEnum.COMMON_BAD_WORD_INCLUDED, "가입 답변");
        }
    }

    // 신청 당시 질문과 답변을 함께 복사해 이후 질문 변경의 영향을 받지 않음
    if (readingClubMapper.setJoinApplication(
            toApplication(clubNumb, userNumb, questions, answers)) != 1) {
        throw new CustomException(
                ResultEnum.COMMON_SAVE_REJECTED,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
```

질문 식별자만 저장하지 않고 신청 당시 문구까지 복사합니다. 모임장이 질문을 수정해도 과거 신청서에서 사용자가 어떤 질문에 답했는지 보존하기 위해서입니다.

## 초대와 예약석

초대를 단순 메시지로 취급하면 정원을 보장할 수 없습니다. `setInvitation`은 초대 대상 전체가 들어갈 자리가 있을 때만 3일짜리 예약석을 만듭니다.

```java
@Override
@Transactional
public ResultData setInvitation(
        Long userNumb, Long clubNumb, ReadingClubDto.InviteReqDto request) {
    List<Long> targets = request.getUserNumbList();
    if (StringUtil.isEmpty(targets)) {
        return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
    }

    // 같은 사용자를 두 번 선택한 일괄 초대를 거절함
    Set<Long> uniqueTargets = new HashSet<>(targets);
    if (uniqueTargets.size() != targets.size()) {
        return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
    }

    // 좌석 예약을 직렬화하도록 모임 행을 잠금
    ReadingClubDto.ClubViewDto club =
            readingClubMapper.getClubForUpdate(clubNumb);
    if (!isOwner(club, userNumb) || !CLUB_ACTIVE.equals(club.getClubStat())
            || Constant.COMM_NO.equals(club.getRcrtYsno())) {
        return ResultData.fail(ResultEnum.COMMON_ACCESS_REJECTED);
    }

    // 만료 예약을 제거한 현재 점유 수에 전체 초대 수를 더해 검사함
    readingClubMapper.delExpiredInvitation(clubNumb);
    if (readingClubMapper.getOccupiedSeatCnt(clubNumb) + targets.size()
            > club.getMaxxMemb()) {
        return ResultData.fail(ResultEnum.COMMON_SAVE_REJECTED);
    }

    // 모든 대상이 맞팔이고 차단·기존 모임 관계가 없는지 먼저 검사함
    for (Long target : targets) {
        if (StringUtil.isEmpty(target)
                || userBlockService.isBlocked(userNumb, target)
                || readingClubMapper.getMutualFollowCnt(userNumb, target) == 0
                || !StringUtil.isEmpty(
                        readingClubMapper.getClubMember(clubNumb, target))) {
            return ResultData.fail(ResultEnum.COMMON_SAVE_REJECTED);
        }
    }

    // 전체 검증이 끝난 뒤에만 대상별 예약석과 알림을 생성함
    for (Long target : targets) {
        readingClubMapper.setInvitation(clubNumb, target, userNumb);
        ResultData alimResult = alimService.sendAlim(
                target,
                Constant.ALIM_SITU_FOLLOW_CLUB,
                Constant.ALIM_TEMP_CODE_INVITE_CLUB,
                Constant.ALIM_TARGET_READING_CLUB,
                clubNumb,
                null,
                Map.of("userName", club.getOwnrNick(),
                        "clubName", club.getClubName()));
        if (StringUtil.isEmpty(alimResult)
                || alimResult.getCode() != RESULT_SUCCESS_CODE) {
            throw new CustomException(
                    ResultEnum.COMMON_SAVE_REJECTED,
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    return ResultData.success();
}
```

검증 루프와 저장 루프를 분리했습니다. 세 명 중 마지막 사용자가 조건을 만족하지 않는다면 앞의 두 명에게 초대와 알림을 만든 뒤 실패하는 대신, 아무것도 저장하기 전에 전체 요청을 거절합니다.

## 초대 수락

```java
@Override
@Transactional
public ResultData uptInvitationAccepted(Long userNumb, Long clubNumb) {
    // 대상 모임 행을 잠가 삭제·가입·다른 수락과 경합하지 않게 함
    ReadingClubDto.ClubViewDto club =
            readingClubMapper.getClubForUpdate(clubNumb);

    // 수락 시점에 차단 관계가 생겼다면 남아 있는 초대도 활성화하지 않음
    if (!StringUtil.isEmpty(club)
            && userBlockService.isBlocked(userNumb, club.getOwnrNumb())) {
        return ResultData.fail(ResultEnum.COMMON_UPDATE_REJECTED);
    }

    // 모집 상태와 초대 만료를 수락 시점에 다시 확인함
    if (StringUtil.isEmpty(userNumb) || StringUtil.isEmpty(club)
            || Constant.COMM_NO.equals(club.getRcrtYsno())
            || readingClubMapper.uptInvitationAccepted(clubNumb, userNumb) == 0) {
        return ResultData.fail(ResultEnum.COMMON_UPDATE_REJECTED);
    }

    return getClubDtl(userNumb, clubNumb);
}
```

초대 화면을 열었을 때 유효했다는 사실은 수락 시점의 권한을 보장하지 않습니다. 차단 관계·모집 여부·만료 여부를 수락 트랜잭션 안에서 다시 검사합니다.

## 한계

행 잠금은 정합성을 높이지만 같은 모임의 가입과 초대 요청을 직렬화하므로 인기 모임에서 대기 시간이 늘 수 있습니다. 트랜잭션 안에서 질문 검증이나 알림 저장처럼 시간이 걸리는 작업도 잠금을 오래 유지합니다. 정원이 훨씬 큰 규모로 확장된다면 좌석 전용 카운터나 별도 예약 모델을 검토할 수 있지만, 현재 규모에서는 한 모임의 상태 전이를 한 잠금으로 이해할 수 있다는 단순성이 더 중요합니다.

## 관련 소스

- [ReadingClubServiceImpl.java](../../src/main/java/org/our/sadari/readingClub/service/ReadingClubServiceImpl.java)
- [ReadingClubMapper.java](../../src/main/java/org/our/sadari/readingClub/mapper/ReadingClubMapper.java)
- [독서 모임 정책 결정](../policies/reading-club-policy-decisions.md)
