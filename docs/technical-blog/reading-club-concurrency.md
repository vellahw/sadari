# 독서 모임 가입 경쟁을 행 잠금으로 막은 방법

## 시작한 문제

정원이 한 자리 남은 독서 모임에 두 사용자가 동시에 가입하면 두 요청 모두 화면에서 “자리 있음”을 본다. 서버에서도 조회 후 저장을 별도로 처리하면 둘 다 성공해 정원을 초과할 수 있다. 가입 신청 승인, 초대 수락, 즉시 가입처럼 진입 경로가 여러 개라 한 경로만 막아서는 해결되지 않는다.

초대는 아직 가입하지 않은 사용자에게 자리를 보장해야 하므로, 만료되지 않은 초대도 예약 좌석으로 계산해야 했다. 반대로 만료 초대까지 계속 세면 실제로 비어 있는 모임에 가입하지 못한다.

## 정원 판정을 잠금 뒤로 옮겼다

모임 행을 `FOR UPDATE`로 조회한 뒤 활성 멤버 수와 유효 초대 수를 다시 계산한다. 정원 확인과 멤버·신청·초대 상태 변경을 같은 트랜잭션 안에서 처리해, 같은 모임의 가입 경로를 직렬화했다.

```java
ReadingClubDto.ClubViewDto club = readingClubMapper.getClubForUpdate(clubNumb);
readingClubMapper.delExpiredInvitation(clubNumb);

if (readingClubMapper.getOccupiedSeatCnt(clubNumb) >= club.getMaxxMemb()) {
    return ResultData.fail(ResultEnum.COMMON_SAVE_REJECTED);
}

if (readingClubMapper.setActiveMember(clubNumb, userNumb) < 1) {
    throw new CustomException(
            ResultEnum.COMMON_SAVE_REJECTED,
            HttpStatus.INTERNAL_SERVER_ERROR);
}
```

실제 구현은 공개형·승인형 가입 정책, 모임장 여부, 기존 멤버, 중복 신청, 차단 관계와 재가입 제한까지 잠금 이후에 검증한다. 승인형 모임에서는 신청 상태를 저장하고, 즉시 가입형에서는 활성 멤버를 생성한다.

## 초대와 신청을 같은 좌석 모델로 봤다

모임장이 초대를 보내면 대상 사용자를 위한 예약 좌석으로 센다. 초대 수락은 새 멤버 행을 무조건 추가하지 않고 기존 초대 멤버 상태를 활성 상태로 전환한다. 초대가 만료되거나 거절되면 예약에서 제외한다.

가입 신청 승인도 신청 행을 잠금 조회하고 최신 상태를 확인한 뒤 처리한다. 승인 시점에 정원이 찼다면 오래된 관리 화면의 승인을 거절한다. 이 때문에 화면에 보인 숫자가 아니라 트랜잭션 안에서 다시 읽은 값이 최종 기준이다.

사용자 차단도 모임 관계에 영향을 준다. 차단 시 처리 중인 초대와 가입 신청을 정리하고, 이후 후보·목록 조회에서도 양방향 차단 관계를 제외한다. 모임장 권한은 요청 본문이 아니라 현재 멤버십에서 확인한다.

## 잠금 범위를 정한 이유

전체 모임을 전역 잠금으로 묶지 않고 경쟁이 발생하는 같은 모임 행만 잠근다. 서로 다른 모임의 가입은 병렬로 처리할 수 있고, 같은 모임의 정원 변경만 순서를 갖는다. 두 사용자를 함께 잠그는 차단 로직은 작은 사용자 번호부터 같은 순서로 잠가 교착 가능성을 낮췄다.

이 방식은 단일 MySQL 원본에서 강한 정합성을 얻기 쉽지만, 트래픽이 커지면 인기 모임 한 곳에 잠금 대기가 집중될 수 있다. 현재 프로젝트 규모에서는 정원 초과를 허용하지 않는 것이 처리량보다 중요하다고 판단했다.

## 구현 근거

- [ReadingClubServiceImpl.java](../../src/main/java/org/our/sadari/readingClub/service/ReadingClubServiceImpl.java)
- [ReadingClubMapper.xml](../../src/main/java/org/our/sadari/readingClub/mapper/ReadingClubMapper.xml)
- [독서 모임 설계](../architecture/reading-club-design.md)
