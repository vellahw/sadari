# JWT와 Redis로 기기별 로그인 세션을 관리한 방법

## 문제

JWT 서명과 만료 시각만 검사하면 서버에서 로그아웃한 토큰도 만료 전까지 사용할 수 있습니다. 한 사용자가 휴대전화와 PC에서 동시에 로그인할 때 한 기기만 로그아웃하는 기능도 구현하기 어렵습니다. 계정이 정지되거나 영구 삭제 대기 상태로 바뀌어도 이미 발급된 토큰의 내용은 바뀌지 않습니다.

Sadari는 JWT에 기기별 세션 식별자를 넣고 Redis에 활성 세션을 저장합니다. 요청마다 JWT 자체와 Redis 세션을 함께 확인하고, 계정 상태에 따라 접근 가능한 API도 제한합니다.

## Access Token과 Refresh Token의 구분

`JwtProvider`는 두 토큰에 같은 세션 식별자를 넣되 용도를 별도 claim으로 고정합니다.

```java
public String createAccessToken(Long userNumb, String role, String sessionId) {
    Date now = new Date();
    return Jwts.builder()
            // 토큰의 주체는 로그인 회원임
            .subject(String.valueOf(userNumb))
            // Access Token 단위 식별자를 별도로 발급함
            .id(UUID.randomUUID().toString())
            .claim("role", role)
            // 같은 기기 로그인 세션을 가리키는 식별자임
            .claim("sid", sessionId)
            // Refresh Token이 API 인증에 사용되지 않게 용도를 기록함
            .claim("token_use", TOKEN_USE_ACCESS)
            .issuedAt(now)
            .expiration(new Date(
                    now.getTime() + accessTokenValidityMilliSeconds))
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact();
}

public String createRefreshToken(Long userNumb, String sessionId) {
    Date now = new Date();
    return Jwts.builder()
            .subject(String.valueOf(userNumb))
            .id(UUID.randomUUID().toString())
            .claim("sid", sessionId)
            // Refresh 전용 토큰임을 명시함
            .claim("token_use", TOKEN_USE_REFRESH)
            .issuedAt(now)
            .expiration(new Date(
                    now.getTime() + refreshTokenValidityMilliSeconds))
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact();
}
```

`sid`는 사용자 식별자와 다릅니다. 같은 사용자가 두 기기에서 로그인하면 사용자 번호는 같지만 `sid`는 다릅니다. 특정 `sid`만 Redis에서 제거하면 다른 기기의 세션은 유지할 수 있습니다.

Access Token 검증에서는 서명과 만료뿐 아니라 토큰 용도와 권한도 확인합니다.

```java
public boolean validateAccessToken(String token) {
    try {
        Claims claims = getClaims(token);
        String role = claims.get("role", String.class);
        // Access 용도와 권한이 모두 있어야 API 인증 토큰으로 인정함
        return TOKEN_USE_ACCESS.equals(
                    claims.get("token_use", String.class))
                && role != null && !role.isBlank();
    }
    catch (Exception e) {
        // 서명, 만료, 형식 검증 중 하나라도 실패하면 인증하지 않음
        return false;
    }
}
```

Refresh Token을 Access Token 위치에 넣어도 서명은 유효할 수 있습니다. `token_use`를 검사하지 않으면 수명이 긴 Refresh Token이 일반 API 인증에 사용되는 권한 상승 문제가 생길 수 있습니다.

## Redis 세션 생성

로그인 성공 시 세션 정보와 사용자별 세션 색인을 한 Lua 명령으로 저장합니다.

```java
public void setLoginUserInfo(
        Long userNumb, String sessionId, String refreshToken,
        String userNick, String userStat, Long ttlSeconds) {
    // 불완전한 세션이나 만료되지 않는 세션 생성을 차단함
    if (StringUtil.isEmpty(userNumb) || StringUtil.isEmpty(sessionId)
            || StringUtil.isEmpty(refreshToken) || StringUtil.isEmpty(userStat)
            || StringUtil.isEmpty(ttlSeconds) || ttlSeconds <= 0) {
        throw new IllegalArgumentException(
                "Login user Redis values are invalid.");
    }

    // 세션, 사용자별 세션 목록, 닉네임과 상태 캐시를 원자적으로 반영함
    redisTemplate.execute(
            SET_LOGIN_USER_SCRIPT,
            List.of(getSessionKey(sessionId), getUserSessionKey(userNumb),
                    getUserNickKey(userNumb), getUserStatusKey(userNumb)),
            String.valueOf(userNumb),
            refreshToken,
            sessionId,
            StringUtil.isEmpty(userNick) ? "" : userNick,
            String.valueOf(ttlSeconds),
            userStat
    );
}
```

세션 저장과 사용자별 세션 목록 추가를 별도 명령으로 실행하면 중간 실패 시 목록만 있거나 세션만 있는 불일치가 생깁니다. Lua로 함께 처리해 로그인 성공의 Redis 상태를 한 단위로 만듭니다. TTL은 Refresh Token 수명과 연결해 서버 세션이 토큰보다 오래 남지 않게 합니다.

## 요청 필터에서 세션과 계정 상태 확인

`JwtFilter.doFilterInternal`은 다음 순서로 인증을 구성합니다.

```java
@Override
protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
    // Authorization 헤더에서 Access Token을 추출함
    String token = extractAccessToken(request);

    // 서명·만료·용도와 로그아웃 블랙리스트를 먼저 확인함
    if (!StringUtil.isEmpty(token)
            && jwtProvider.validateAccessToken(token)
            && !tokenRedisService.hasAccessTokenBlacklist(
                    jwtProvider.getTokenId(token))) {
        Long userNumb = jwtProvider.getUserNumb(token);
        String sessionId = jwtProvider.getSessionId(token);

        // Redis에서 제거된 기기 세션이면 인증 객체를 만들지 않음
        if (!tokenRedisService.isSessionActive(userNumb, sessionId)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication =
                jwtProvider.getAuthentication(token);
        // 한 요청에서 계정 상태를 한 번만 조회함
        String userStat = tokenRedisService.getUserStatus(userNumb);

        // 상태 캐시가 없을 때 정상 상태로 추정하지 않고 원본 저장소를 확인함
        if (StringUtil.isEmpty(userStat)) {
            UserDto savedUser = userMapper.getUserByNumb(userNumb);
            if (StringUtil.isEmpty(savedUser)
                    || StringUtil.isEmpty(savedUser.getUserStat())) {
                filterChain.doFilter(request, response);
                return;
            }

            userStat = savedUser.getUserStat();
            tokenRedisService.uptUserStatus(userNumb, userStat);
        }

        // 영구 삭제 대기 회원은 상태 확인·취소·로그아웃 경로만 허용함
        if (Constant.USER_STAT_DELETE_PENDING.equals(userStat)
                && !isDeletePendingPath(request.getRequestURI())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // 비활성화 회원은 재로그인과 로그아웃 경로만 허용함
        if (Constant.USER_STAT_WITHDRAWN.equals(userStat)
                && !isWithdrawnAllowedPath(request.getRequestURI())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // 모든 검증이 끝난 뒤에만 인증 객체를 등록함
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
}
```

순서가 중요합니다. JWT를 파싱하기 전에 빈 값을 걸러내고, 유효한 JWT에서도 세션이 살아 있는지 확인합니다. 계정 상태 캐시가 없을 때 `ACTIVE`로 간주하지 않고 원본을 조회하는 것은 보안상 실패 개방을 피하기 위한 선택입니다.

상태가 제한된 계정도 로그아웃이나 탈퇴 취소 같은 복구 경로는 사용할 수 있어야 합니다. 그래서 인증 자체를 모두 제거하는 대신 상태별 허용 경로를 좁게 정의합니다.

## Refresh Token 동시 회전

브라우저의 여러 탭이 Access Token 만료를 동시에 감지하면 같은 Refresh Token으로 재발급 요청을 보낼 수 있습니다.

```java
public String rotateRefreshToken(
        Long userNumb, String sessionId, String presentedToken,
        String proposedToken, long ttlSeconds) {
    // 회전에 필요한 값이나 설정이 올바르지 않으면 검증 실패로 반환함
    if (StringUtil.isEmpty(userNumb) || StringUtil.isEmpty(sessionId)
            || StringUtil.isEmpty(presentedToken)
            || StringUtil.isEmpty(proposedToken)
            || ttlSeconds <= 0 || refreshRotationGraceSeconds < 0) {
        return null;
    }

    // Redis 서버 시각의 유예 구간에서는 동시 요청에 같은 최신 토큰을 반환함
    return redisTemplate.execute(
            ROTATE_REFRESH_SCRIPT,
            List.of(getSessionKey(sessionId), getUserSessionKey(userNumb)),
            String.valueOf(userNumb),
            presentedToken,
            proposedToken,
            String.valueOf(ttlSeconds),
            String.valueOf(refreshRotationGraceSeconds)
    );
}
```

첫 요청이 토큰을 교체한 직후 두 번째 요청이 기존 토큰을 제출하면 무조건 탈취로 판단할 수도 있습니다. 그러나 여러 탭과 서비스 워커의 정상 동시 요청도 같은 모양입니다. 짧은 유예 구간에서는 이미 저장된 최신 토큰을 반환해 정상 요청을 합치고, 유예 시간이 지난 기존 토큰은 거절합니다.

## 탈퇴 재인증의 일회성 상태값

탈퇴는 로그인 상태만으로 즉시 적용하지 않고 Kakao 재인증을 거칩니다.

```java
// OAuth 요청과 콜백을 연결할 예측 불가능한 상태값을 생성함
String state = UUID.randomUUID().toString();

try {
    // 탈퇴 요청 본문을 서버 저장 문자열로 변환함
    String requestJson = objectMapper.writeValueAsString(request);
    // 콜백 검증에 사용할 일회성 요청을 10분 동안만 저장함
    redisTemplate.opsForValue().set(
            getWithdrawalStateKey(state), requestJson,
            WITHDRAWAL_STATE_TTL);
}
catch (Exception e) {
    log.error("회원 탈퇴 재인증 상태 저장 실패. userNumb={}, message={}",
            userNumb, e.getMessage());
    return ResultData.fail(ResultEnum.COMMON_SAVE_REJECTED);
}

request.setAuthUrl(kakaoAuthProvider.getKakaoAuthorizationUrl(state));
return ResultData.success(request);
```

콜백에서는 상태값을 읽은 직후 삭제해 재사용을 막습니다. 영구 삭제는 즉시 물리 삭제하지 않고 기본 30일 유예 상태로 전환해 사용자가 취소할 시간을 둡니다.

## 한계

요청마다 Redis 세션을 확인하므로 Redis 지연이 인증 경로 지연으로 이어집니다. 대신 즉시 로그아웃과 계정 상태 반영을 얻었습니다. Redis 장애 정책, 다중 지역 구성, Refresh Token 탈취 감지는 운영 규모에 따라 추가 설계가 필요합니다. 로그에는 토큰·인가 코드·외부 응답 원문을 남기지 않습니다.

## 관련 소스

- [JwtProvider.java](../../src/main/java/org/our/sadari/global/security/jwt/JwtProvider.java)
- [JwtFilter.java](../../src/main/java/org/our/sadari/global/security/jwt/JwtFilter.java)
- [TokenRedisService.java](../../src/main/java/org/our/sadari/global/security/jwt/TokenRedisService.java)
- [AuthServiceImpl.java](../../src/main/java/org/our/sadari/user/auth/service/AuthServiceImpl.java)
- [UserWithdrawalServiceImpl.java](../../src/main/java/org/our/sadari/user/service/UserWithdrawalServiceImpl.java)
