# Sadari 주요 기능 기술 글

README의 주요 기능 표에서 바로 이동할 수 있도록 실제 구현에서 고민한 문제와 선택을 기능별로 정리했습니다. 각 글은 현재 저장소의 핵심 코드를 주석과 함께 직접 제시하고, 실행 순서대로 분기·트랜잭션·실패 경로·한계를 해설합니다. 운영 환경에서 확인하지 않은 결과는 구현 완료와 구분해 적었습니다.

| 기능 | 기술 글 | 핵심 주제 |
| --- | --- | --- |
| 도서 | [Kakao 도서 검색 API 쿼터를 Redis로 보호한 방법](book-search-and-ranking.md) | 50권 선조회, 공용 캐시, Lua 호출 제한, 인기 도서 집계 |
| 독서 기록 | [Spring 트랜잭션으로 도서와 독후감을 함께 저장한 방법](reading-report-transaction.md) | 원자적 저장, 상태별 정책, 낙관적 충돌 감지 |
| 독서 목표 | [주간·월간·연간 독서 목표 집계를 SQL 2회로 줄인 방법](reading-goal-aggregation.md) | 기간 경계, 조건부 집계, 이전 목표 복사 |
| 소셜 | [팔로우·좋아요·댓글을 공개 범위와 함께 설계한 방법](social-feed-reactions.md) | 서버 권한 검증, 대상 유형, 차단 관계, 후처리 알림 |
| 독서 모임 | [독서 모임 가입 경쟁을 행 잠금으로 막은 방법](reading-club-concurrency.md) | 정원 경쟁, 초대 좌석, 권한과 상태 전이 |
| 알림 | [알림 저장과 FCM 발송 시점을 분리한 방법](notification-push-transaction.md) | 템플릿, 중복 방지, 커밋 이후 푸시 |
| 계정 | [JWT와 Redis로 기기별 로그인 세션을 관리한 방법](authentication-session-lifecycle.md) | 세션 식별자, Refresh 회전, 로그아웃, 계정 상태 |
| 콘텐츠 안전 | [Aho-Corasick과 이미지 재인코딩으로 콘텐츠 입력을 검증한 방법](content-validation-file-security.md) | 비속어 탐지, 이미지 시그니처, EXIF, 비공개 저장 |
| 다국어·번역 | [Google 번역 API를 온디맨드 캐시와 월 50만 자 제한으로 붙인 방법](multilingual-translation-cache.md) | 기기 언어 기본값, 원문 언어, 번역 캐시, 비용 상한 |

화면보다 서버의 정합성·동시성·비용 제어가 핵심인 글이라 별도 화면 캡처는 넣지 않았습니다. UI 배치가 구현 판단의 일부인 다국어 글은 실제 컴포넌트와 스타일 소스를 함께 연결했습니다.
