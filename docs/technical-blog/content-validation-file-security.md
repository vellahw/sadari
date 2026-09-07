# Aho-Corasick과 이미지 재인코딩으로 콘텐츠 입력을 검증한 방법

## 시작한 문제

닉네임, 한줄소개, 독후감과 댓글은 여러 화면에서 생성된다. 화면별 정규식만 두면 새 API가 추가될 때 검증이 빠지기 쉽고, 공백·기호·숫자를 섞은 비속어 우회도 막기 어렵다.

이미지는 확장자와 브라우저 Content-Type만 확인해서는 실제 파일 형식을 알 수 없다. 시그니처만 흉내 낸 파일, 지나치게 큰 해상도로 메모리를 소모하는 이미지, EXIF 방향과 메타데이터를 그대로 가진 원본을 안전한 공개 자산으로 취급할 수 없었다.

## 문자열은 한 번 순회했다

활성 비속어와 예외 단어를 DB에서 읽어 Aho-Corasick 자동자를 구성한다. 여러 단어를 입력마다 순차 검색하지 않고, 실패 링크를 따라 문자열을 한 번 순회하며 후보를 찾는다. 공백·기호·반복 문자 정규화 결과와 숫자를 유지한 결과를 각각 검사해 우회 패턴을 줄였다.

```java
BadWordCache cache = getBadWordCache();
return findBadWord(cache.badWordMatcher(), cache.exceptionWordMatcher(), normalized)
        .or(() -> findBadWord(
                cache.digitBadWordMatcher(),
                cache.digitExceptionWordMatcher(),
                normalizedWithDigits));
```

사전은 10분 동안 메모리에 캐시한다. 만료 시 이중 확인 잠금으로 한 요청만 자동자를 다시 만들고 나머지는 완성된 캐시를 재사용한다. 서비스의 등록·수정 경계에서 공통 검증하므로 프론트엔드 요청을 직접 조작해도 같은 정책이 적용된다.

예외 단어가 포함된 경우 무조건 허용하지 않고 실제 매칭 범위와 정규화 결과를 함께 판정한다. 현재 사전 품질에 따라 과탐지와 미탐지가 달라질 수 있으므로 운영에서 사전 변경 이력과 신고 결과를 함께 관찰해야 한다.

## 이미지는 새 파일로 만들었다

업로드 처리는 다음 순서로 진행한다.

1. JPEG·PNG 선두 시그니처를 확인한다.
2. ImageIO가 판독한 실제 형식과 시그니처가 일치하는지 검증한다.
3. 파일 크기, 가로·세로, 전체 픽셀 수를 제한한다.
4. JPEG EXIF 방향을 실제 픽셀에 반영한다.
5. 서버가 새 JPEG 또는 PNG로 재인코딩한다.
6. UUID 기반 객체 키로 비공개 저장소에 보관한다.

재인코딩은 원본 메타데이터와 해석하지 않은 부가 데이터를 그대로 배포하지 않기 위한 경계다. 프로필 원본 편집 흐름은 서버 임시 공간에 최대 30분 보관하고 브라우저에는 축소 미리보기만 전달한다. 최종 저장, 로그아웃, 비활성화 또는 만료 시 임시 파일을 정리한다.

DB가 새 파일을 참조한 뒤에만 기존 파일을 삭제하고, 트랜잭션이 롤백되면 요청 중 생성한 새 파일을 보상 삭제한다. DB와 물리 파일 저장소가 하나의 트랜잭션 자원이 아니므로 완료 시점을 명시적으로 연결한 것이다.

## 구현 근거

- [BadWordDetectionService.java](../../src/main/java/org/our/sadari/global/common/service/BadWordDetectionService.java)
- [FileService.java](../../src/main/java/org/our/sadari/global/file/service/FileService.java)
- [FileResourceController.java](../../src/main/java/org/our/sadari/global/file/controller/FileResourceController.java)
- [콘텐츠·파일 보안 정책](../policies/content-file-policy.md)
