# Google 번역 API를 온디맨드 캐시와 월 50만 자 제한으로 붙인 방법

## 시작한 문제

화면 메시지만 영어로 바꾸면 다국어 기능이 끝나는 줄 알았지만, 공지·메뉴·공통코드·알림 템플릿처럼 DB에서 내려오는 문구와 사용자가 작성한 독후감은 그대로 남았다. 공개 독후감을 설정 언어로 자동 번역하면 읽기 경험은 좋아지지만 목록 조회마다 번역 API를 호출하면 비용과 응답 시간이 통제되지 않는다.

그래서 번역은 자동 실행하지 않고 사용자가 카드 왼쪽 아래의 “번역 보기”를 눌렀을 때만 생성하기로 했다. 하트·댓글과 반대편에 배치해 콘텐츠 보조 동작과 소셜 반응을 구분했다.

## 표시 언어와 작성 언어를 나눴다

처음 계정 설정이 없으면 `navigator.language`가 영어로 시작하는지 확인해 기본 표시 언어를 정한다. 사용자가 설정 화면의 셀렉트박스에서 한국어 또는 영어를 저장하면 계정 설정과 브라우저 메시지 locale을 함께 갱신하고 화면을 다시 로드한다.

프론트엔드는 한국어·영어 properties 파일을 읽어 같은 메시지 키로 화면 문구를 선택한다. 서버는 요청 locale을 사용해 공통코드명, 메뉴, 공지 분류와 알림 템플릿의 언어를 결정한다. 독후감을 등록하거나 수정할 때는 그 시점의 사용자 표시 언어를 원문 언어로 저장한다.

```typescript
export const getMessageLocale = (): "en" | "ko" => {
  const saved = localStorage.getItem("sadari:message-locale");
  if (saved === "en" || saved === "ko") return saved;
  return navigator.language.toLowerCase().startsWith("en") ? "en" : "ko";
};
```

## 버튼을 누른 순간만 번역했다

공개 독후감 목록과 피드 조회 시 서버는 원문 언어, 현재 표시 언어, 유효한 캐시, 이번 달 잔여 글자 수를 확인해 버튼 표시 여부만 계산한다. 원문과 표시 언어가 같거나 본문이 없으면 숨긴다. 유효한 캐시가 있으면 잔여량과 API 키 상태에 관계없이 다시 볼 수 있다.

새 번역 요청에서는 공개 범위, 활성 계정과 차단 관계를 서버가 다시 검증한다. 같은 독후감 원문을 잠금 조회해 동시 요청을 직렬화하고, 원문 SHA-256 해시와 대상 언어가 일치하는 캐시가 있으면 Google을 호출하지 않는다.

```java
ReportTranslationDto cached = reportMapper.getReportTrnsDtl(source);
if (!StringUtil.isEmpty(cached)
        && source.getOrigHash().equals(cached.getOrigHash())) {
    cached.setCached(true);
    return ResultData.success(cached);
}
```

캐시가 없으면 Google Cloud Translation Basic API에 일반 텍스트로 요청한다. API 키는 URL이 아니라 `X-goog-api-key` 헤더에 넣고 서버 설정에서만 읽는다. 번역문은 HTML entity를 해제한 평문으로 저장한다. 프론트엔드는 같은 화면에서 받은 번역문을 독후감 번호별로 보관해 이후 “원문 보기/번역 보기” 전환에는 추가 요청을 보내지 않는다. 전환 자체에는 슬라이드 애니메이션을 적용하지 않았다.

## 월 50만 자를 넘기지 않는 방법

비용 상한은 요청 횟수가 아니라 Google 과금 기준에 맞춘 유니코드 코드 포인트 수로 계산한다. Redis Lua 스크립트가 현재 사용량 확인과 이번 요청 글자 수 증가를 원자적으로 처리한다. 동시 요청이 들어와도 둘 다 남은 한도를 통과해 초과 집행되는 경쟁을 막는다.

월 경계는 설정한 Google 쿼터 시간대를 사용하고, 카운터는 다음 달 경계 뒤 자동 만료된다. 남은 글자보다 긴 새 원문은 목록 단계에서 번역 버튼 자체를 숨긴다. Redis 장애나 API 키 누락도 새 번역을 허용하지 않는 방향으로 처리한다. 이미 저장된 캐시는 비용이 들지 않으므로 계속 노출한다.

## 현재 범위와 다음 단계

한국어·영어 UI, DB 관리 문구, 사용자 언어 설정, 독후감 원문 언어와 온디맨드 번역 캐시는 구현되어 있다. 번역 API의 실제 비용과 품질은 Google Cloud 계정의 사용량 화면에서 별도로 관찰해야 하며, 저장소 테스트만으로 운영 비용을 검증했다고 보지는 않는다.

영어권 도서 검색은 아직 Kakao 제공자를 사용한다. 다음 단계는 Google Books 응답을 기존 화면 DTO로 변환하고, ISBN과 언어를 함께 사용해 언어별 도서 정보를 재사용하도록 저장 조회 조건을 맞추는 것이다. 별점은 그 변경 이후에도 ISBN 기준으로 모든 언어 독후감을 합산하는 정책을 유지한다.

## 구현 근거

- [message.ts](../../src/main/frontend/src/app/messages/message.ts)
- [UserSettingsPage.tsx](../../src/main/frontend/src/pages/Settings/UserSettingsPage.tsx)
- [ReportTranslationService.java](../../src/main/java/org/our/sadari/report/service/ReportTranslationService.java)
- [ReportListView.tsx](../../src/main/frontend/src/components/ReportList/ReportListView.tsx)
- [ReportServiceImpl.java](../../src/main/java/org/our/sadari/report/service/ReportServiceImpl.java)
