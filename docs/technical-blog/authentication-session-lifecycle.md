# JWT와 Redis로 기기별 로그인 세션을 관리한 방법

## 시작한 문제

서명과 만료시간만 검증하는 JWT는 서버가 특정 기기의 토큰을 즉시 폐기하기 어렵다. 한 기기에서 로그아웃했는데 다른 기기까지 풀리거나, 반대로 전체 로그아웃 후에도 기존 Access Token이 만료될 때까지 살아 있는 문제를 피하고 싶었다.

여러 탭이 동시에 만료를 감지해 Refresh Token을 함께 재발급하면 첫 요청이 토큰을 회전시킨 뒤 나머지 요청이 실패하는 경쟁도 있었다. 계정 정지·비활성화·삭제 대기 상태는 토큰 자체가 유효해도 접근을 막아야 했다.

## JWT에 세션 식별자를 넣었다

로그인할 때 기기별 UUID 세션 식별자를 만들고 Access Token과 Refresh Token 모두에 `sid` claim으로 넣는다. Redis에는 세션별 Refresh Token과 사용자 정보, 사용자별 세션 목록을 분리해 저장한다.

```java
String sessionId = UUID.randomUUID().toString();
String accessToken = jwtProvider.createAccessToken(userNumb, role, sessionId);
String refreshToken = jwtProvider.createRefreshToken(userNumb, sessionId);

tokenRedisService.setLoginUserInfo(
        userNumb, sessionId, refreshToken, nickname, userStatus);
```

`JwtFilter`는 JWT 서명·만료뿐 아니라 Redis에 해당 사용자와 세션 조합이 활성인지 확인한다. 이어서 계정 상태를 확인해 정지, 비활성화, 삭제 대기 사용자의 접근을 제한한다. Redis에 계정 상태 캐시가 없으면 DB에서 복원하지만, 장애를 활성 상태로 간주하지 않도록 실패 방향을 구분했다.

## Refresh 회전을 원자적으로 만들었다

재발급은 제출된 Refresh Token과 Redis의 현재 토큰을 비교하고 새 토큰으로 교체한다. 여러 요청이 동시에 들어올 때는 Lua 스크립트가 비교와 회전을 하나의 명령으로 수행한다. 짧은 유예 구간에는 최초 회전 결과를 재사용해 여러 탭이 같은 인증 상태로 수렴한다.

프론트엔드에서도 한 탭 안의 재발급 Promise를 공유하고, 탭 간 로그아웃은 `BroadcastChannel`과 storage 이벤트로 전달한다. 서버의 원자성만으로 해결할 부분과 브라우저 경험을 위한 동기화를 나눴다.

현재 기기 로그아웃은 해당 세션만 삭제한다. 전체 로그아웃은 사용자별 세션 목록을 따라 모든 세션과 활성 푸시 구독을 정리한다. 즉시 폐기가 필요한 Access Token은 식별자를 남은 만료시간만큼 블랙리스트에 둔다.

## 계정 상태와 삭제 유예

비활성화와 영구 탈퇴 예약은 같은 로그아웃 처리로 끝내지 않는다. 영구 삭제는 기본 30일 유예를 두고, 재인증을 통과한 본인만 예약을 취소할 수 있다. Kakao 재인증 요청의 state는 Redis에 짧게 보관하고 읽은 직후 제거해 재사용을 막는다.

이 구조는 JWT의 완전한 무상태 장점을 포기한다. 대신 기기별 로그아웃, 즉시 세션 폐기, 계정 상태 변경 같은 서비스 요구를 명시적으로 다룰 수 있었다. Redis가 인증 가용성의 일부가 되므로 장애 정책과 모니터링이 중요하다.

## 구현 근거

- [JwtProvider.java](../../src/main/java/org/our/sadari/global/security/jwt/JwtProvider.java)
- [JwtFilter.java](../../src/main/java/org/our/sadari/global/security/jwt/JwtFilter.java)
- [TokenRedisService.java](../../src/main/java/org/our/sadari/global/security/jwt/TokenRedisService.java)
- [AuthLoginController.java](../../src/main/java/org/our/sadari/user/auth/controller/AuthLoginController.java)
- [인증과 보안 설계](../portfolio/auth-security.md)
