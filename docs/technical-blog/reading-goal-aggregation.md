# 주간·월간·연간 독서 목표 집계를 SQL 2회로 줄인 방법

## 시작한 문제

마이페이지 한 화면에는 이번 주·이번 달·올해의 완료 권수, 직전 기간과의 차이, 목표 권수와 달성률, 누적 달성 횟수, 현재 읽는 책과 기간별 완료 목록이 함께 필요하다. 항목마다 조회하면 한 요청에서 최대 19회의 SQL이 실행됐다.

조회 횟수를 줄이는 것만으로는 충분하지 않았다. 연말과 연초가 겹치는 ISO 주차, 월말·연말의 배타적 종료 경계, 다른 사용자 화면의 목표 공개 설정까지 같은 기준으로 계산해야 했다.

## 기간을 먼저 고정했다

서비스에서 오늘을 기준으로 현재·이전·다음 주, 월, 연도의 시작일을 만든다. 주간 목표 키는 ISO `week-based-year`를 사용해 12월 말이 다음 해 첫 주에 포함되는 경우를 처리한다. 조회 조건에는 시작일 이상, 다음 기간 시작일 미만의 반열린 구간을 전달한다.

```java
LocalDate weekStart = today.with(GOAL_WEEK_FIELDS.dayOfWeek(), 1);
LocalDate monthStart = today.withDayOfMonth(1);
LocalDate yearStart = today.withDayOfYear(1);

request.setCurrentWeekStart(weekStart.toString());
request.setNextWeekStart(weekStart.plusWeeks(1).toString());
request.setCurrentMonthStart(monthStart.toString());
request.setNextMonthStart(monthStart.plusMonths(1).toString());
```

첫 번째 SQL은 조건부 집계로 현재·이전 기간의 완료 권수, 목표, 수정 횟수와 누적 달성 횟수를 한 행으로 반환한다. 두 번째 SQL은 현재 읽는 책과 올해 완료한 책만 한 번 조회한다. 서비스는 그 목록을 주·월·연 목록으로 분류한다.

## 목표 변경 정책

목표를 올리는 것은 언제든 허용하고, 낮추는 경우에만 기간 말 잠금과 수정 횟수 제한을 적용한다. 같은 값으로 저장하면 수정 횟수를 소비하지 않는다. 주간·월간·연간 저장은 하나의 트랜잭션 안에서 처리해 중간 유형만 반영되는 상태를 막았다.

“이전 목표 복사”는 현재 기간에 목표가 없는 유형만 직전 기간 값으로 생성한다. 이미 설정한 목표는 덮어쓰지 않고, 세 유형 모두 복사할 값이 없으면 잘못된 요청으로 응답한다.

다른 사용자 프로필에서는 독후감 공개 범위와 목표 공개 설정을 각각 확인한다. 목표가 비공개이면 권수·달성률·달성 횟수를 응답에서 제거하고, 본인 화면은 전체 범위를 유지한다.

## 확인한 결과

개발 DB에서 준비 10회 후 100회 측정한 JDBC 중앙값은 215.241ms에서 26.436ms로 줄었다. 격리 MySQL 10,000건에서는 314.315ms에서 175.091ms로 줄었다. 이 수치는 단일 연결 JDBC 측정이며 Spring MVC, 직렬화, 동시 요청을 포함한 운영 응답 시간은 아니다.

목록을 애플리케이션에서 다시 분류하는 비용은 생겼지만, 네트워크 왕복과 반복 SQL을 제거한 효과가 더 컸다. 데이터가 커질수록 통합 집계 자체의 비용이 늘 수 있어 실행 계획과 인덱스는 계속 관찰해야 한다.

## 구현 근거

- [ReportServiceImpl.java](../../src/main/java/org/our/sadari/report/service/ReportServiceImpl.java)
- [마이페이지 독서 요약 성능 측정](../performance/my-page-reading-summary-optimization.md)
- [ReadingSummaryQueryDto.java](../../src/main/java/org/our/sadari/myPage/dto/ReadingSummaryQueryDto.java)
