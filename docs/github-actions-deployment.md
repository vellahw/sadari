# GitHub Actions 운영 배포 설정

이 프로젝트는 `main` 브랜치에 push되면 다음 순서로 배포됩니다.

1. Java 17과 Node.js 24 환경에서 WAR 빌드를 검증합니다.
2. Docker 이미지를 빌드해 `ghcr.io/<owner>/<repository>`에 커밋 SHA와 `latest` 태그로 올립니다.
3. EC2의 `~/sadari`에 운영 `.env`, `docker-compose.yml`, Firebase 서비스 계정 파일을 전송합니다.
4. EC2가 새 이미지를 pull하고 Docker Compose로 애플리케이션과 Redis를 실행합니다.
5. `http://127.0.0.1:<APP_PORT>/` 응답을 최대 2분 동안 확인합니다.

실제 운영 값은 GitHub 저장소의 `Settings > Secrets and variables > Actions`에 등록합니다.
가능하면 `production` Environment를 만들고 승인 규칙과 아래 Secrets/Variables를 그 Environment에 등록합니다.

## Actions Secrets

| 이름 | 내용 |
| --- | --- |
| `EC2_HOST` | EC2 Public IP 또는 배포용 도메인 |
| `EC2_USER` | SSH 사용자명. Amazon Linux는 보통 `ec2-user`, Ubuntu는 `ubuntu` |
| `EC2_SSH_PRIVATE_KEY` | EC2 key pair의 PEM 전체 내용 |
| `GHCR_USERNAME` | GHCR 이미지를 읽을 GitHub 사용자명 |
| `GHCR_TOKEN` | 해당 패키지에 `read:packages` 권한이 있는 GitHub PAT |
| `DB_URL` | MySQL 8.4 JDBC URL |
| `DB_USERNAME` | 운영 DB 계정 |
| `DB_PASSWORD` | 운영 DB 비밀번호 |
| `FRONT_DOMAIN` | 외부에서 접속하는 프론트 HTTPS Origin |
| `BACK_DOMAIN` | 외부에서 접속하는 백엔드 HTTPS Origin |
| `JWT_SECRET` | JWT 서명용 충분히 긴 무작위 비밀키 |
| `KAKAO_REST_API_KEY` | Kakao 로그인과 도서 검색 API에 함께 사용하는 REST API 키 |
| `KAKAO_JAVASCRIPT_KEY` | Kakao JavaScript 키 |
| `KAKAO_NATIVE_APP_KEY` | Kakao Native App 키. 사용하지 않으면 빈 값 가능 |
| `GOOGLE_TRANSLATION_API_KEY` | Cloud Translation API 전용 키. Google Cloud 키 이름은 `sadari-translation-server` |
| `GOOGLE_BOOKS_API_KEY` | Books API 전용 키. Google Cloud 키 이름은 `sadari-books-server` |
| `FIREBASE_WEB_API_KEY` | Firebase Web App의 `apiKey` |
| `FIREBASE_WEB_AUTH_DOMAIN` | Firebase Web App의 `authDomain` |
| `FIREBASE_WEB_PROJECT_ID` | Firebase Web App의 `projectId` |
| `FIREBASE_WEB_STORAGE_BUCKET` | Firebase Web App의 `storageBucket` |
| `FIREBASE_WEB_MESSAGING_SENDER_ID` | Firebase Web App의 `messagingSenderId` |
| `FIREBASE_WEB_APP_ID` | Firebase Web App의 `appId` |
| `FIREBASE_VAPID_PUBLIC_KEY` | Firebase Cloud Messaging의 Web Push 공개키 |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Firebase Admin SDK 서비스 계정 JSON 전체 원문 |
| `AWS_ACCESS_KEY_ID` | S3 전용 IAM 사용자의 Access Key ID |
| `AWS_SECRET_ACCESS_KEY` | S3 전용 IAM 사용자의 Secret Access Key |

`GITHUB_TOKEN`은 Actions 실행 시 GitHub가 자동 발급하므로 직접 등록하지 않습니다. 이 토큰은
워크플로에서 GHCR 이미지 push에 사용됩니다. EC2의 pull에는 별도 `GHCR_TOKEN`이 필요합니다.

## Actions Variables

아래 값은 비밀정보가 아니며 등록하지 않으면 표의 기본값이 사용됩니다.

| 이름 | 기본값 | 용도 |
| --- | --- | --- |
| `EC2_SSH_PORT` | `22` | EC2 SSH 포트 |
| `APP_PORT` | `8080` | EC2에서 외부에 연결할 애플리케이션 포트 |
| `DB_CONNECTION_TIMEOUT` | `60000` | DB 커넥션 획득 제한시간(ms) |
| `DB_MAXIMUM_데이터베이스 연결 풀 크기` | `10` | Hikari 최대 커넥션 수 |
| `DB_MINIMUM_IDLE` | `2` | Hikari 최소 유휴 커넥션 수 |
| `DB_MAXIMUM_POOL_SIZE` | `10` | Hikari 최대 커넥션 수 |
| `HTTP_CONNECT_TIMEOUT_MILLIS` | `3000` | 외부 HTTP 서버 연결 제한시간(ms) |
| `HTTP_READ_TIMEOUT_MILLIS` | `5000` | 외부 HTTP 서버 응답 제한시간(ms) |
| `REDIS_HOST` | 필수 | Redis 서버 호스트 |
| `REDIS_PORT` | `6379` | Redis 서버 포트 |
| `REDIS_PASSWORD` | 빈 값 | Redis 인증 비밀번호 |
| `JWT_ACCESS_TOKEN_SECONDS` | `1800` | Access Token 유효시간(초) |
| `JWT_REFRESH_TOKEN_SECONDS` | `86400` | Refresh Token 유효시간(초) |
| `JWT_REFRESH_ROTATION_GRACE_SECONDS` | `10` | 다중 탭 동시 재발급을 동일 회전 결과로 처리하는 유예시간(초) |
| `WITHDRAWAL_HARD_DELETE_WAIT_DAYS` | `30` | 영구 탈퇴 신청 후 회원 데이터를 물리 삭제하기까지의 유예기간(일) |
| `TIMER_ATTENDANCE_MIN_SECONDS` | `600` | 하루 독서 출석 인정에 필요한 최소 누적 시간(초) |
| `TIMER_MAX_SESSION_SECONDS` | `28800` | 단일 독서 타이머 세션과 목표시간 알림에 적용하는 최대 시간(초) |
| `TIMER_ZONE_ID` | `Asia/Seoul` | 일별 독서 시간과 주간 출석 경계를 계산하는 시간대 |
| `TIMER_DETAIL_RETENTION_DAYS` | `365` | 완료된 독서 타이머 세션 상세 보존기간(일) |
| `BOOK_SEARCH_CACHE_HIT_RATE_LIMIT_PER_MINUTE` | `300` | 회원별 60초 캐시 적중 도서 검색 요청 한도 |
| `BOOK_SEARCH_CACHE_MISS_RATE_LIMIT_PER_MINUTE` | `60` | 회원별 60초 캐시 미적중 도서 검색 요청 한도 |
| `BOOK_SEARCH_RATE_LIMIT_PER_DAY` | `200` | 캐시 미적중 시 차감하는 회원별 24시간 외부 도서 검색 실제 호출 한도 |
| `BOOK_SEARCH_PROVIDER_CALL_LIMIT_PER_DAY` | `27000` | 공급자별 앱 전체 24시간 외부 도서 검색 실제 호출 보호 한도 |
| `BOOK_SEARCH_CACHE_TTL_SECONDS` | `600` | 사용자와 연결하지 않은 도서 검색 결과 Redis 캐시 유효시간(초) |
| `BOOK_SEARCH_POPULAR_KEYWORD_WINDOW_DAYS` | `7` | 인기 검색어 점수 합산과 회원별 동일 검색어 중복 제한 기간(일) |
| `BOOK_SEARCH_POPULAR_KEYWORD_MIN_USER_COUNT` | `3` | 인기 검색어 공용 화면 노출에 필요한 최소 고유 회원 수 |
| `BOOK_SEARCH_POPULAR_KEYWORD_MAX_SIZE` | `10` | 검색 화면에 전달할 인기 검색어 최대 건수 |
| `MULTIPART_MAX_FILE_SIZE` | `10MB` | 단일 업로드 파일 제한 |
| `MULTIPART_MAX_REQUEST_SIZE` | `21MB` | 전체 multipart 요청 제한 |
| `UPLOAD_MAX_IMAGE_BYTES` | `10485760` | 디코딩 전 이미지 최대 바이트 수 |
| `UPLOAD_MAX_IMAGE_PIXELS` | `20000000` | 이미지 최대 전체 픽셀 수 |
| `UPLOAD_MAX_IMAGE_DIMENSION` | `8192` | 이미지 한 변의 최대 픽셀 수 |
| `COOKIE_SECURE` | `true` | HTTPS 쿠키 전용 여부 |
| `COOKIE_SAME_SITE` | `None` | 인증 쿠키 SameSite 정책 |
| `FIREBASE_CREDENTIALS_PATH` | 필수 | Firebase 서비스 계정 자격증명 파일 경로 |
| `SCHEDULER_ENABLED` | `true` | 운영 스케줄러 실행 여부 |
| `SCHEDULER_MAX_SIZE` | `100` | 한 번의 스케줄 실행 최대 처리 건수 |
| `SERVER_PORT` | `8080` | Spring 서버 포트 |
| `LOGGING_LEVEL_ROOT` | `info` | 루트 로그 레벨 |
| `LOGGING_LEVEL_APP` | `info` | 프로젝트 패키지 로그 레벨 |
| `COMPLAINT_RESULT_MAX_SIZE` | `5` | 한 번의 팝업에 표시할 미확인 신고 조치 결과 최대 건수 |

## 프로필 고정 설정

- 사용자·관리자 앱의 `application-loc.yml`은 RDS MySQL 8.4에 연결합니다. 기존 DB 설정은
  주석으로 보존하고 `spring.datasource.url`, `username`, `password`에 실제 접속값을 직접 입력합니다.
  이는 사용자가 요청한 로컬 전용 설정이며 두 YML은 Git에서 제외합니다. 실제 비밀번호는 공개
  예시와 배포 문서에 기록하지 않습니다. 로컬 앱은 `rds.env`를 자동으로 읽지 않습니다.
- `rds.env.example`은 환경변수 기반 연결을 선택할 때의 참고 예시입니다. 운영 프로필은 기존
  `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 환경변수 방식을 유지합니다.
- RDS 연결은 `sslMode=VERIFY_IDENTITY`를 사용합니다. AWS 공식
  [CA 인증서 묶음](https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem)의 해당 리전
  루트 인증서를 Java PKCS12 신뢰 저장소에 등록하고 JDBC URL의 `trustCertificateKeyStoreUrl`에
  절대 파일 URL을 지정합니다. 예시의 `changeit`은 공개 CA 인증서만 담은 저장소의 무결성 확인값이며
  DB 비밀번호와 다릅니다. 신뢰 저장소는 로컬 `.gradle/rds` 아래에 보관할 수 있습니다.
- 1GiB RDS를 여러 로컬 앱에서 공유할 때 `loc`의 `spring.datasource.hikari`에는
  `maximum-pool-size: 5`, `minimum-idle: 1`, `connection-timeout: 60000`밀리초를 직접 설정합니다. 운영의 기본값 `10`, `2`,
  `60000`은 유지하며, 로컬 파일과 인증서 경로는 GitHub Actions나 Docker에 전달하지 않습니다.
- 전환 후 두 앱을 재시작하고 DB 조회와 Tailnet 화면을 확인합니다. 연결이 실패하면 인증서 경로,
  비밀번호, RDS 상태와 보안 그룹을 확인하며 인증서 검증을 끄지 않습니다. 기존 DB로 복귀하려면
  앱을 중지한 뒤 해당 환경의 접속 정보를 별도 비공개 설정으로 지정하고 다시 시작합니다.
- Tailnet 장치에서 로컬 OAuth를 검증할 때는 `application-loc.yml`의 `domain.front`와
  `domain.back` 기본값을 같은 `https://<tailscale-device>.<tailnet>.ts.net` 주소로 설정하고
  `app.cookie.secure=true`, `app.cookie.same-site=Lax`를 사용합니다.
- `application-loc.yml`의 `domain.local-front`는 localhost 로그인 완료 후 이동할 Vite 주소인
  `http://localhost:5173`으로 고정합니다.
- `loc` 프로필은 `/api/oauth/local-login?userNumb=<test-user-number>` 간편 로그인 URL을 제공합니다.
  활성 회원만 DB 원본 권한으로 로그인시키며 비활성화, 영구 탈퇴 대기 및 이용정지 회원은 상태 변경 없이
  차단합니다. 해당 Controller와 Service는 `loc` 프로필이 활성화되고 운영 프로필은 비활성화된 경우에만
  등록되므로 두 프로필이 잘못 함께 활성화되더라도 운영 환경에는 Endpoint가 생성되지 않습니다.
- Vite 개발 서버는 `application-loc.yml`의 `domain.proxy=http://127.0.0.1:8080`을 읽어 `/api` 요청을
  로컬 Spring 서버로 전달합니다. Vite는 Tailscale Serve 대상과 동일한 `127.0.0.1:5173`에 고정되며,
  포트가 이미 사용 중이면 다른 포트로 이동하지 않고 시작에 실패하여 잘못된 프록시 연결을 차단합니다.
- Vite 개발 프록시는 원래 브라우저 Host를 `X-Forwarded-Host`로 전달합니다. 로컬 로그인 Controller는
  이 값을 허용 주소 선택에만 사용하여 localhost 요청은 `domain.local-front`, 그 외 loc 요청은
  설정된 `domain.front` 호스트와 일치할 때만 Tailnet 주소로 이동시킵니다. 그 외 Host에서는 회원 조회와
  세션 발급을 시작하지 않으며 요청값으로 임의 리다이렉트 주소를 받지 않습니다.
- Vite Host 허용 목록에는 `domain.front`의 Tailnet 호스트만 추가하여 휴대폰 요청을 허용하고 임의 Host
  헤더 요청은 차단합니다.
- Vite의 내부 전달에서는 브라우저의 개발 Origin을 제거하여 POST와 PUT 요청이 외부 CORS 요청으로
  오인되지 않게 하며, Spring은 `domain.back`의 Tailscale 기본값으로 OAuth 콜백 URI를 생성합니다.
- 프런트엔드는 HTTPS를 사용하는 `*.ts.net` 개발 주소에서도 서비스워커를 등록하여 Tailnet 장치의
  PWA 설치와 오프라인 앱 셸 검증을 허용합니다.
- 카카오 개발자 콘솔의 Redirect URI에는
  `https://<tailscale-device>.<tailnet>.ts.net/api/oauth/callback/kakao`를 등록해야 합니다.
- `application-prod.yml`의 `DB_URL`은 MySQL JDBC URL을 사용하고 `DB_PASSWORD`는
  GitHub Actions Secret으로 전달합니다.
- 로컬과 운영의 `book.search.url`은 종료된 네이버 도서 API의 대체 공급자인 카카오 도서 검색
  `https://dapi.kakao.com/v3/search/book`으로 고정하며 인증에는 기존 `KAKAO_REST_API_KEY` Secret을 사용합니다.
- Google 번역은 `GOOGLE_TRANSLATION_API_KEY`가 있을 때 활성화하고, 키가 없으면 기존 번역 캐시만 표시하며 신규 번역 버튼은 숨깁니다. 영어 설정의 도서 검색에는 `GOOGLE_BOOKS_API_KEY`가 필요하며 키가 없으면 외부 요청 없이 검색 실패 응답을 반환합니다.
- Google Cloud Console의 `API 및 서비스 > 사용자 인증 정보`에서 `sadari-translation-server` 값은 `GOOGLE_TRANSLATION_API_KEY`, `sadari-books-server` 값은 `GOOGLE_BOOKS_API_KEY`에 각각 등록합니다.
- Google 번역은 Cloud Translation Basic v2 서버 주소를 사용하고 앱 전체 월간 신규 번역을 500,000 유니코드 코드 포인트로 고정합니다. 월간 경계는 Google 쿼터 기준 시간대와 맞추며, Redis에서 사용량을 확인할 수 없으면 신규 Google 호출을 중단합니다.
- 운영 도서 검색은 한국어 Kakao에서 최대 50권, 영어 Google Books에서 최대 40권을 조회하며 캐시 적중 300회·미적중 60회의 회원별 60초 제한, 회원별 일간 제한, 공급자별 앱 전체 실제 호출 제한과 10분 공용 캐시를 Redis에서 관리합니다.
- 도서 인기 검색어는 최근 7일의 일별 Redis 점수를 합산하고 동일 회원의 같은 검색어를 기간 내 한 번만 반영하며 최소 3명 이상인 상위 10건을 제공합니다.
- 운영의 `book.search.popular-keyword-user-dedup-enabled`는 순위 조작 방지를 위해 `true`로 고정하며 환경변수로 노출하지 않습니다.
- 로컬의 `book.search.popular-keyword-user-dedup-enabled`는 한 계정의 반복 검색으로 화면을 검증할 수 있도록 `false`를 사용하고 최소 노출 인원은 `1`로 설정합니다.
- `BOOK_SEARCH_PROVIDER_CALL_LIMIT_PER_DAY` 기본값은 카카오 도서 검색 일일 30,000건 중 3,000건을 장애 대응과 운영 확인용으로 남기는 `27,000`입니다.
- 도서 검색 제한값, 캐시 유효시간과 인기 검색어 집계 기준은 공개 가능한 운영 정책이므로 Actions Variables로 관리합니다. Redis가 검색 제한을 확인할 수 없으면 외부 호출을 중단하고 인기 검색어 집계나 조회만 실패하면 일반 도서 검색은 유지합니다.
- `application-loc.yml`은 탈퇴 기능 검증을 위해 `withdrawal.hard-delete-wait-days`를 `0`으로 설정하고
  `withdrawal.hard-delete-test-enabled`를 `true`로 설정합니다.
- `application-loc.yml`은 Git에서 제외되므로 각 개발 환경의 로컬 파일에 위 두 값을 직접 유지해야 합니다.
- Tailnet OAuth 검증용 `application-loc.yml`은 공유 DB의 삭제 위험을 차단하도록
  `scheduler.enabled=false`를 사용합니다.
- 영구 탈퇴 테스트 스케줄러가 필요한 경우에는 격리된 로컬 DB를 연결한 뒤에만 일시적으로
  `scheduler.enabled=true`를 사용해야 합니다.
- `application-prod.yml`은 `withdrawal.hard-delete-test-enabled`를 `false`로 고정합니다.
  이 값은 GitHub Actions 환경변수로 노출하지 않으므로 운영 배포에서 로컬 테스트 스케줄러를
  활성화할 수 없습니다.
- 독서 모임의 종료 회차 확정 스케줄은 로컬과 운영에서 매분 실행하도록 고정합니다.
  `scheduler.round-completion-cron`은 서비스 내부 일자 경계 처리를 빠르게 확정하기 위한 값이며
  운영 중 임의 변경 대상이 아니므로 Actions Variable이나 Secret으로 노출하지 않습니다.
- 운영 유예기간은 `영구 탈퇴 데이터 삭제_처리 대기 일수` Actions Variable로 조정할 수 있으며,
  등록하지 않으면 30일을 사용합니다.
- 사용자 앱과 로컬 전용 관리자 앱의 `application-loc.yml`에 있는 `complaint.auto-action` 임계치는 기능 검증을 위해 독후감, 댓글, 프로필 사진, 배경사진 및 한줄소개 모두 `1`건으로 고정합니다.
- 운영 배포하는 사용자 앱의 `application-prod.yml`에는 같은 다섯 대상의 임계치를 모두 `5`건으로 고정합니다.
  이 값은 운영 중 임의 변경으로 조치 기준이 달라지지 않도록 Actions Variable이나 Secret으로 노출하지 않습니다.
- 로컬과 운영의 `complaint.evidence.retention-days`는 `180`, `cleanup-batch-size`는 `100`,
  증거 정리 스케줄은 매일 `04:20`으로 고정합니다. 미처리 신고와 연결된 증거는 보존하고,
  연결된 신고가 모두 종결된 뒤 최근 처리일로부터 180일이 지난 증거만 물리 삭제합니다.
  이 값들은 신고 감사 정책의 일부이므로 Actions Variable이나 Secret으로 노출하지 않습니다.
- 자동 조치 및 증거 보관 기능을 배포하기 전에 `scripts/db/mysql/01-create.sql`의
  신고 이력, 자동 조치 이력, 관리자 전용 이미지 증거 테이블과
  `승인된 비공개 기준정보 패키지`의 신고 조치 결과, 신고 처리 결과, 신고 대상 유형 공통코드를 먼저 반영합니다.

## EC2 사전 조건

- Docker Engine과 Docker Compose v2가 설치되어 있어야 합니다.
- 배포 사용자가 `sudo` 없이 `docker` 명령을 실행할 수 있어야 합니다.
- `curl`이 설치되어 있어야 배포 후 상태 검증이 가능합니다.
- EC2 보안 그룹에서 SSH 포트는 필요한 관리 IP로 제한하고, 서비스 포트는 로드밸런서나
  리버스 프록시를 통해 공개하는 구성을 권장합니다.
- EC2에서 MySQL RDS의 `3306` 포트로 접근할 수 있어야 하고, RDS 보안 그룹은 EC2 보안 그룹을
  소스로 허용해야 합니다.
- PWA와 Secure Cookie, Firebase Web Push를 사용하려면 최종 서비스 도메인에 HTTPS가 적용되어야 합니다.

## 독서 타이머 8시간 및 목표 알림 배포

- 애플리케이션 배포 전에 `scripts/db/mysql/01-create.sql`의 중요도 순서대로 독서 타이머 세션을 재구성해야 합니다. 기존 테이블 끝에 컬럼을 단순 추가하지 않습니다.
- 유지보수 창에서 애플리케이션을 중지하고 DB 스냅샷을 만든 뒤, 교체 테이블을 기준 DDL로 생성해 기존 10개 컬럼을 명시적으로 복사합니다. 신규 알림 목표 독서 시간 초, 목표시간 알림 예정 일시, 알림 발송 일시는 기존 세션에 `NULL`로 둡니다.
- 원본과 교체 테이블의 전체 행 수, 사용자별 활성 세션 수, 확정 독서 시간 초 합계, FK 및 인덱스를 대조한 뒤 원자적 이름 교환으로 전환합니다. 검증 전 원본 테이블을 삭제하지 않습니다.
- 재구성된 테이블에는 업무 조회 최적화 인덱스 (독서 타이머 상태 코드, 알림 발송 일시, 목표시간 알림 예정 일시, 독서 타이머 세션 번호)`가 있어야 합니다.
- `승인된 비공개 기준정보 패키지`를 적용해 독서 타이머 기간 초과 스케줄러 분류값과 알림 템플릿을 등록합니다. 기존 동일 코드의 관리자 문구와 사용 여부는 덮어쓰지 않습니다.
- GitHub Actions Variable `TIMER_MAX_SESSION_SECONDS`를 별도로 등록했다면 `28800`으로 변경합니다. 기존 `14400` 값이 남아 있으면 화면과 서버가 8시간 설정을 거부합니다.

## 기기별 인증 세션 전환

`sid` 기반 기기별 세션을 처음 배포할 때는 기존 사용자별 단일 Refresh Token 키와 새 세션 키가 일시적으로 함께 존재할 수 있습니다.

1. 모든 애플리케이션 인스턴스를 새 버전으로 교체하고 구버전 인스턴스가 요청을 처리하지 않는지 확인합니다.
2. `sid`가 없는 기존 JWT는 새 버전에서 인증할 수 없으므로 사용자가 한 번 다시 로그인할 수 있음을 배포 공지와 점검 항목에 포함합니다.
3. Redis에서 `SCAN`을 사용해 구형 `auth:refresh:*` 키의 존재와 TTL을 확인합니다. 운영 Redis에서 전체 키를 한 번에 조회하는 `KEYS` 명령은 사용하지 않습니다.
4. 구형 키의 TTL이 남아 있으면 자연 만료를 기다릴 수 있습니다. 즉시 정리해야 하면 구버전 인스턴스 종료를 확인한 뒤 `UNLINK`를 사용해 비동기로 삭제합니다.
5. 새 로그인 후 `auth:session:{sid}`, `auth:user:sessions:{userNumb}`, `auth:user:nick:{userNumb}`와 `auth:user:status:{userNumb}`가 생성되는지 확인합니다.
6. 현재 디바이스 로그아웃은 현재 `auth:session:{sid}`만 제거하고, 전체 디바이스 로그아웃은 회원별 Set에 연결된 모든 세션을 제거하는지 확인합니다.

현재 버전은 구형 `auth:refresh:{userNumb}`를 읽거나 생성하지 않으며 새 로그아웃 처리에서도 해당 키를 삭제하지 않습니다. 구버전과 새 버전을 동시에 운영하는 동안 구형 키를 먼저 삭제하면 구버전 사용자의 재발급이 실패하므로 배포 순서를 지켜야 합니다.

## 최초 설정 순서

1. GitHub에서 `production` Environment를 생성합니다.
2. 위 Secrets와 필요한 Variables를 등록합니다.
3. GHCR pull용 PAT를 만들고 `read:packages` 권한을 부여합니다.
4. EC2에 Docker, Compose v2, curl을 설치하고 배포 사용자를 docker 그룹에 추가합니다.
5. `main` 브랜치에 push하거나 Actions 화면에서 `Sadari CI/CD`를 수동 실행합니다.

현재 `SadariApplicationTests`는 Git에서 제외된 로컬 설정과 실제 DB/Redis를 요구하므로 CI에서
자동 실행하지 않습니다. 추후 Testcontainers나 독립 `application-test.yml`을 추가하면 워크플로의
`-x test`를 제거해 통합 테스트까지 배포 차단 조건으로 사용할 수 있습니다.

## S3 파일 저장소 설정

운영 환경의 영구 이미지는 비공개 S3 버킷에 저장합니다. 브라우저는 S3 객체 URL에 직접 접근하지 않고 기존 `/uploads/{type}/{yyMMdd}/{uuid}.{ext}` 경로를 호출하며, 백엔드가 IAM 권한으로 객체를 조회해 전달합니다. 따라서 버킷의 모든 퍼블릭 액세스 차단을 활성화하고 CORS와 공개 버킷 정책은 설정하지 않습니다.

S3 인증은 GitHub Actions Secrets의 `AWS_ACCESS_KEY_ID`와 `AWS_SECRET_ACCESS_KEY`를 운영 `.env`에 주입하고 AWS SDK 정적 자격 증명 공급자로 사용합니다. 해당 Access Key를 발급한 IAM 사용자에는 대상 버킷 객체의 `s3:GetObject`, `s3:PutObject`, `s3:DeleteObject`만 허용합니다. 현재 구현은 버킷 목록 조회를 수행하지 않으므로 `s3:ListBucket` 권한은 필요하지 않습니다.

장기 Access Key는 유출 시 만료 전까지 계속 사용할 수 있으므로 저장소나 로그에 기록하지 않고 GitHub Secrets와 운영 서버의 권한이 제한된 `.env`에만 보관합니다. 키를 교체할 때는 IAM에서 새 키를 발급하고 두 Actions Secrets를 함께 변경한 뒤 배포 검증이 끝난 후 이전 키를 비활성화합니다.

Actions Variables에는 다음 값을 등록합니다.

| 이름 | 기본값 | 용도 |
| --- | --- | --- |
| `STORAGE_PROVIDER` | `s3` | 운영 파일 저장소 구현 |
| `STORAGE_LOCAL_ROOT` | `C:/shared/sadari-uploads` | Windows에서 `local` 저장소를 선택했을 때의 공용 루트 디렉터리 |
| `STORAGE_S3_BUCKET` | 없음 | 영구 이미지를 저장할 비공개 S3 버킷 이름 |
| `STORAGE_S3_REGION` | `ap-northeast-2` | S3 버킷 리전 |
| `STORAGE_S3_ENDPOINT` | 빈 값 | AWS S3에서는 비워 두며 S3 호환 저장소 전환 시에만 지정 |
| `STORAGE_S3_PATH_STYLE_ACCESS` | `false` | AWS S3에서는 `false`, 일부 S3 호환 저장소에서는 `true` |

`vars.STORAGE_PROVIDER`가 없거나 빈 값이면 워크플로의 `STORAGE_PROVIDER`는 `s3`가 됩니다. 이 선택은 Secret 유무와 무관합니다. `STORAGE_PROVIDER=s3`일 때 `STORAGE_S3_BUCKET` Variable과 `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` Secrets는 필수이며 배포 묶음 생성 전에 누락 여부를 검증합니다. 따라서 Secret Key가 없으면 `local`로 전환되는 것이 아니라 배포가 실패합니다. `local`일 때는 세 값을 요구하지 않습니다.

로컬 `loc` 프로파일도 사용자·관리자 애플리케이션 모두 기본적으로 `STORAGE_PROVIDER=s3`를 사용합니다. 로컬 디스크를 사용할 때는 두 애플리케이션에 `STORAGE_PROVIDER=local`과 동일한 `STORAGE_LOCAL_ROOT`를 명시합니다.

관리자 애플리케이션도 같은 `STORAGE_PROVIDER`, `STORAGE_LOCAL_ROOT`, `STORAGE_S3_*`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` 이름을 사용합니다. Windows에서 `local`을 선택할 때 두 앱의 기본값은 `C:/shared/sadari-uploads`입니다. `s3`을 선택하면 실행 장비와 관계없이 같은 버킷을 지정하며, `local`을 선택하면 두 애플리케이션 프로세스가 실제로 접근할 수 있는 동일한 절대 디렉터리 또는 공유 볼륨을 `STORAGE_LOCAL_ROOT`로 지정합니다. 서로 다른 장비의 로컬 디스크는 같은 경로 문자열만으로 파일을 공유할 수 없습니다.

`C:/shared/sadari-uploads`는 Windows 절대경로이므로 macOS에서는 같은 위치로 사용할 수 없습니다. Mac mini 디스크를 직접 사용할 때는 두 앱 모두 `STORAGE_PROVIDER=local`, `STORAGE_LOCAL_ROOT=/Users/Shared/sadari-uploads`처럼 macOS 절대경로를 지정합니다. Mac mini의 S3 호환 저장소로 전환할 때는 `STORAGE_S3_ENDPOINT`와 `STORAGE_S3_PATH_STYLE_ACCESS`를 해당 제품 설정에 맞게 변경합니다.

기존 로컬 영구 이미지 파일은 자동 이전하지 않습니다. 운영 컨테이너의 `sadari-uploads` Named Volume 연결은 제거했으며, 배포 전환 전에 기존 파일 보존이 필요한 경우 별도 마이그레이션을 수행해야 합니다. 프로필 편집 중 생성되는 30분 임시 이미지는 공개 경로와 분리된 컨테이너 임시 디렉터리에 계속 저장하며 재배포 시 소실될 수 있습니다.

### 채팅 열람 상태와 알림 읽음 처리

채팅 열람 유효 시간은 로컬과 운영 설정의 `reading-club.chat-view-ttl-seconds`에서 초 단위로 관리하며 기본값은 15입니다. 화면은 3초마다 열람 상태를 갱신하므로 유효 시간은 갱신 주기보다 충분히 길게 유지합니다. Redis를 공유하는 서버들은 동일한 시각 기준과 설정을 사용해야 합니다. Redis 장애 시 알림 생략을 중단하고 일반 수신 조건으로 발송합니다.

배포 전 [초기 테이블 정의](../scripts/db/mysql/01-create.sql)의 알림 원본 채팅 번호 필드를 기존 데이터베이스에 먼저 반영합니다. 기존 알림은 원본 연결이 비어 있어도 조회할 수 있고, 해당 채팅방의 최신 메시지까지 읽으면 함께 읽음 처리합니다. 앱을 이전 버전으로 되돌려도 추가 필드는 유지할 수 있습니다.
