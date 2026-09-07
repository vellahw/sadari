# 주간·월간·연간 독서 목표 집계를 SQL 2회로 단축

## 문제

마이페이지의 독서 요약은 현재 주·월·연의 완료 권수만 보여 주지 않습니다. 이전 기간과의 차이, 각 기간 목표, 달성률, 누적 달성 횟수, 현재 읽는 책, 기간별 완료 목록까지 한 화면에서 필요합니다.

이 값을 각각 조회하면 집계 쿼리가 계속 늘어납니다. 실제 개선 전 구조는 요약 화면 한 번에 19회의 SQL이 필요했습니다. Sadari는 숫자 집계를 한 번, 목록을 한 번 조회한 뒤 애플리케이션에서 기간별 목록을 나눠 총 2회로 줄였습니다.

## 조회 진입점

`ReportServiceImpl.getMonthlyReadingSummary`가 두 번의 조회와 응답 조립을 연결합니다.

```java
@Override
public ResultData getMonthlyReadingSummary(Long userNumb, String pubcYsno) {
    // 독서량과 목표 기간 계산의 기준 날짜를 조회함
    LocalDate today = LocalDate.now();
    // 통합 집계 SQL에 전달할 기간과 공통코드 조건을 생성함
    ReadingSummaryQueryDto queryReq =
            getReadingSummaryQueryReq(userNumb, pubcYsno, today);
    // 기간별 독서량과 목표 및 누적 달성 횟수를 한 번에 조회함
    ReadingSummaryQueryDto queryResult =
            reportMapper.getReadingSummary(queryReq);

    // 집계 결과가 없어도 0으로 채운 화면 응답을 만들 수 있게 보정함
    if (StringUtil.isEmpty(queryResult)) {
        queryResult = new ReadingSummaryQueryDto();
    }

    // 통합 집계 결과를 화면 응답 형식으로 변환함
    MonthlyReadingSummaryDto summary =
            getReadingSummaryResponse(queryResult, today);
    // 다른 사용자 화면에서는 소유자의 독서 목표 공개 설정을 적용함
    applyReadingGoalPrivacy(summary, userNumb, pubcYsno);
    // 현재 읽는 책과 올해 완료한 책을 한 번에 조회함
    List<ReportDto> reportList = reportMapper.getReadingSummaryList(queryReq);
    // 한 번 조회한 목록을 현재 주와 월 및 연도 화면 목록으로 분류함
    applyReadingSummary(summary, reportList, today);

    return ResultData.success(summary);
}
```

첫 번째 Mapper 호출은 조건부 집계로 숫자를 만듭니다. 두 번째 호출은 올해 범위의 기록을 가져옵니다. Java는 같은 목록을 다시 데이터베이스에 질의하지 않고 날짜를 비교해 주간·월간·연간 목록으로 분류합니다.

`queryResult`가 `null`일 때 빈 DTO를 만드는 이유도 화면 계약 때문입니다. 기록이 없는 정상 사용자는 오류가 아니라 모든 수치가 0인 요약을 받아야 합니다.

## 기간 경계를 한곳에서 만들기

집계 쿼리와 목록 분류가 서로 다른 기준일을 사용하면 월요일이나 월말에 숫자와 목록이 달라질 수 있습니다. 요청 시작 시 만든 `today` 하나를 다음 메서드로 전달합니다.

```java
private ReadingSummaryQueryDto getReadingSummaryQueryReq(
        Long userNumb, String pubcYsno, LocalDate today) {

    // 현재 주 시작일을 계산함
    LocalDate currentWeekStart = today.with(GOAL_WEEK_FIELDS.dayOfWeek(), 1);
    // 현재 월 시작일을 계산함
    LocalDate currentMonthStart = today.withDayOfMonth(1);
    // 현재 연도 시작일을 계산함
    LocalDate currentYearStart = today.withDayOfYear(1);

    ReadingSummaryQueryDto req = new ReadingSummaryQueryDto();
    req.setUserNumb(userNumb);
    // 집계에 사용할 완료 독서 상태를 설정함
    req.setDoneStat(Constant.REPORT_STAT_DONE);
    // 현재 읽는 책 조회에 사용할 독서 상태를 설정함
    req.setReadStat(Constant.REPORT_STAT_READ);
    // 다른 사용자 화면에서는 공개 독후감만 집계함
    req.setPubcYsno(pubcYsno);

    req.setWeekGoalType(Constant.GOAL_TYPE_WEEK);
    req.setMonthGoalType(Constant.GOAL_TYPE_MONTH);
    req.setYearGoalType(Constant.GOAL_TYPE_YEAR);

    req.setCurrentWeekStart(currentWeekStart.toString());
    req.setNextWeekStart(currentWeekStart.plusWeeks(1).toString());
    req.setPreviousWeekStart(currentWeekStart.minusWeeks(1).toString());
    req.setCurrentMonthStart(currentMonthStart.toString());
    req.setNextMonthStart(currentMonthStart.plusMonths(1).toString());
    req.setPreviousMonthStart(currentMonthStart.minusMonths(1).toString());
    req.setCurrentYearStart(currentYearStart.toString());
    req.setNextYearStart(currentYearStart.plusYears(1).toString());
    req.setPreviousYearStart(currentYearStart.minusYears(1).toString());
    return req;
}
```

각 기간은 마지막 날짜를 만들지 않고 다음 기간 시작일을 함께 전달합니다. 집계에서는 `현재 시작 이상, 다음 시작 미만`으로 비교할 수 있어 시각 정밀도와 윤년·월말 계산을 데이터베이스 표현에 의존하지 않습니다.

## 다른 사용자에게 목표를 숨기는 위치

같은 요약 API가 본인 마이페이지와 다른 사용자 프로필에서 사용됩니다. `pubcYsno`가 없으면 본인 조회이고, 값이 있으면 공개 범위를 적용하는 조회입니다.

```java
private void applyReadingGoalPrivacy(
        MonthlyReadingSummaryDto summary, Long userNumb, String pubcYsno) {
    // 본인 화면은 목표 정보를 그대로 사용함
    if (StringUtil.isEmpty(pubcYsno)) {
        summary.setGoalPublicYsno(Constant.COMM_YES);
        return;
    }

    UserSettingDto setting = userMapper.getUserSettingDtl(userNumb);
    boolean isPublic = !StringUtil.isEmpty(setting)
            && Constant.COMM_YES.equals(setting.getReadingGoalYsno());
    summary.setGoalPublicYsno(isPublic ? Constant.COMM_YES : Constant.COMM_NO);
    if (isPublic) {
        return;
    }

    // 비공개 사용자의 목표값과 달성 정보를 응답에서 제거함
    summary.setWeekGoalCnt(null);
    summary.setMonthGoalCnt(null);
    summary.setYearGoalCnt(null);
    summary.setWeekGoalRate(0);
    summary.setMonthGoalRate(0);
    summary.setYearGoalRate(0);
    summary.setTotalGoalAchvCnt(0);
}
```

프론트엔드에서만 목표 영역을 숨기면 API 응답에는 개인 목표가 남습니다. 서비스에서 응답 DTO 자체를 지워 직접 호출에서도 같은 공개 정책을 보장합니다.

## 세 목표를 한 트랜잭션으로 저장하기

주간·월간·연간 목표는 한 화면에서 함께 저장하므로 하나라도 유효하지 않으면 전체 요청을 거절합니다.

```java
@Override
@Transactional
public ResultData setReadingGoal(Long userNumb, ReadingGoalDto readingGoalDto) {
    // 주간, 월간, 연간 목표 중 하나라도 유효하지 않으면 저장 요청 전체를 거절함
    if (!isValidReadingGoal(readingGoalDto)) {
        return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
    }

    LocalDate today = LocalDate.now();
    ResultEnum weekResult = setReadingGoalByType(
            userNumb, today, Constant.GOAL_TYPE_WEEK,
            readingGoalDto.getWeekGoalCnt());
    if (!StringUtil.isEmpty(weekResult)) {
        return ResultData.fail(weekResult);
    }

    ResultEnum monthResult = setReadingGoalByType(
            userNumb, today, Constant.GOAL_TYPE_MONTH,
            readingGoalDto.getMonthGoalCnt());
    if (!StringUtil.isEmpty(monthResult)) {
        return ResultData.fail(monthResult);
    }

    ResultEnum yearResult = setReadingGoalByType(
            userNumb, today, Constant.GOAL_TYPE_YEAR,
            readingGoalDto.getYearGoalCnt());
    if (!StringUtil.isEmpty(yearResult)) {
        return ResultData.fail(yearResult);
    }

    // 저장 직후 다시 계산한 최신 요약을 반환함
    return getMonthlyReadingSummary(userNumb, null);
}
```

기간별 저장 로직은 공통 메서드로 모았지만 호출 순서는 명시적으로 남겼습니다. 중간에 정책 위반이 발견되면 뒤 기간을 저장하지 않습니다. 데이터베이스 예외가 발생하면 `@Transactional`이 앞에서 저장된 목표도 함께 롤백합니다.

## 이전 목표 복사

‘지난 목표 불러오기’는 지난주·지난달·작년의 기준일을 각각 계산한 뒤 현재 기간에 목표가 없는 항목만 복사합니다.

```java
LocalDate today = LocalDate.now();
LocalDate currentWeekStart = today.with(GOAL_WEEK_FIELDS.dayOfWeek(), 1);
LocalDate previousWeekStart = currentWeekStart.minusWeeks(1);
LocalDate currentMonthStart = today.withDayOfMonth(1);
LocalDate previousMonthStart = currentMonthStart.minusMonths(1);
LocalDate currentYearStart = today.withDayOfYear(1);
LocalDate previousYearStart = currentYearStart.minusYears(1);

int copiedCount = 0;
copiedCount += copyPrevReadingGoal(
        userNumb, today, currentWeekStart, previousWeekStart,
        Constant.GOAL_TYPE_WEEK);
copiedCount += copyPrevReadingGoal(
        userNumb, today, currentMonthStart, previousMonthStart,
        Constant.GOAL_TYPE_MONTH);
copiedCount += copyPrevReadingGoal(
        userNumb, today, currentYearStart, previousYearStart,
        Constant.GOAL_TYPE_YEAR);

if (copiedCount == 0) {
    return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
}
return getMonthlyReadingSummary(userNumb, null);
```

기간마다 길이가 다르므로 단순히 일수를 빼지 않습니다. `minusWeeks`, `minusMonths`, `minusYears`를 사용해 달력 의미를 코드에 드러냅니다. 복사된 항목이 하나도 없으면 성공처럼 보이지 않도록 실패를 반환합니다.

## 결과와 한계

한 화면의 SQL 실행 수는 19회에서 2회로 줄었습니다. 이는 Mapper 호출 구조를 기준으로 확인한 수치이며 운영 트래픽의 응답 시간 개선율을 의미하지는 않습니다. 조건부 집계 쿼리는 단순 쿼리보다 길어졌기 때문에 기간 조건과 실행 계획을 변경할 때는 숫자 집계와 목록 조회를 함께 회귀 테스트해야 합니다.

현재 `LocalDate.now()`는 시스템 기본 시간대를 사용합니다. 배포 환경의 시간대가 서울로 고정된다는 전제가 깨질 수 있다면 도서 인기 집계처럼 명시적인 시간대를 사용하도록 통일하는 것이 다음 개선점입니다.

## 관련 소스

- [ReportServiceImpl.java](../../src/main/java/org/our/sadari/report/service/ReportServiceImpl.java)
- [ReportMapper.java](../../src/main/java/org/our/sadari/report/mapper/ReportMapper.java)
- [마이페이지 독서 통계 정책](../policies/my-page-reading-statistics-policy.md)
