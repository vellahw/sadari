# Aho-Corasick과 이미지 재인코딩으로 콘텐츠 입력을 검증

## 문제

사용자 입력 검증은 단순 금칙어 `contains`와 확장자 검사만으로 끝나지 않습니다. 비속어 사이에 특수문자를 넣거나 같은 글자를 반복하면 단순 비교를 우회할 수 있습니다. 이미지 파일명과 `Content-Type`은 요청자가 바꿀 수 있고, 작은 압축 파일이 디코딩 뒤 거대한 픽셀 메모리를 요구할 수도 있습니다.

Sadari는 텍스트를 여러 형태로 정규화한 뒤 Aho-Corasick 자동자로 탐색합니다. 이미지는 실제 시그니처·디코더 형식·크기·해상도를 확인하고, 검증된 픽셀만 새 JPEG 또는 PNG로 재인코딩해 저장합니다.

## 입력 하나를 여러 표현으로 검사하기

`BadWordDetectionService.findBadWord`는 숫자를 보존하는 표현과 제거하는 표현을 각각 만듭니다.

```java
public Optional<String> findBadWord(String value) {
    // 빈 입력은 캐시 조회와 정규화 연산 없이 통과시킴
    if (StringUtil.isEmpty(value)) {
        return Optional.empty();
    }

    // 만료 시 내부에서 사전을 다시 읽어 자동자를 재생성함
    BadWordCache cache = getBadWordCache();

    // 공백 경계와 한글·영문만 남기고 특수문자와 숫자를 제거함
    String normalizedWithoutDigits = normalizeBadWord(value, false);
    // 숫자 포함 비속어 검사를 위해 숫자는 남기고 특수문자만 제거함
    String normalizedWithDigits = normalizeBadWord(value, true);

    // 일반 사전과 숫자 포함 사전을 순서대로 검사함
    return getRepeatedBadWordDtl(
                    cache.badWordMatcher(),
                    cache.exceptionWordMatcher(),
                    normalizedWithoutDigits)
            .or(() -> getRepeatedBadWordDtl(
                    cache.digitBadWordMatcher(),
                    cache.digitExceptionWordMatcher(),
                    normalizedWithDigits));
}
```

숫자를 무조건 제거하면 숫자 자체가 의미 있는 금칙 표현을 놓칠 수 있고, 항상 남기면 문자 사이에 숫자를 넣은 우회를 복원하지 못합니다. 그래서 두 표현과 두 사전을 분리합니다.

반복 문자 우회는 원본 정규화 문자열을 먼저 검사한 뒤 필요할 때만 추가 변환합니다.

```java
private Optional<String> getRepeatedBadWordDtl(
        AhoCorasickMatcher matcher,
        AhoCorasickMatcher exceptionMatcher,
        String value) {
    // 정상 정규화 문자열을 가장 먼저 한 번 탐색함
    Optional<String> matchedWord =
            findBadWord(matcher, exceptionMatcher, value);
    if (matchedWord.isPresent()) {
        return matchedWord;
    }

    // 같은 문자가 반복되지 않으면 추가 정규화 비용을 사용하지 않음
    if (!REPEATED_CHARACTER_PATTERN.matcher(value).find()) {
        return Optional.empty();
    }

    // 늘인 글자를 한 글자로 축약함
    String collapsedRepeatedValue =
            REPEATED_CHARACTER_PATTERN.matcher(value).replaceAll("$1");
    // 사이에 삽입한 반복 구간을 제거함
    String removedRepeatedValue =
            REPEATED_CHARACTER_PATTERN.matcher(value).replaceAll("");

    return findBadWord(matcher, exceptionMatcher, collapsedRepeatedValue)
            .or(() -> findBadWord(
                    matcher, exceptionMatcher, removedRepeatedValue));
}
```

정상 입력 대부분은 첫 탐색 한 번으로 끝납니다. 반복 패턴이 있을 때만 두 번째·세 번째 표현을 만듭니다. 예외 허용어도 별도 자동자로 찾고, 실제 비속어의 위치가 허용어 범위 안에 포함될 때만 제외합니다. 문장에 허용어가 하나 있다는 이유로 다른 위치의 비속어까지 통과시키지 않습니다.

## 사전 캐시의 동시 갱신

```java
private BadWordCache getBadWordCache() {
    long now = System.currentTimeMillis();
    BadWordCache currentCache = badWordCache;

    // 정상 요청은 동기화 없이 현재 자동자를 사용함
    if (!currentCache.isExpired(now)) {
        return currentCache;
    }

    synchronized (this) {
        currentCache = badWordCache;

        // 기다리는 동안 다른 스레드가 갱신했는지 다시 확인함
        if (!currentCache.isExpired(now)) {
            return currentCache;
        }

        // 비속어와 예외 허용어를 같은 생명주기로 다시 읽음
        List<String> reloadedBadWords = loadBadWordsFromCodeList();
        List<String> reloadedExceptionWords = getExceptionWordList();
        List<String> digitBadWords = reloadedBadWords.stream()
                .filter(this::hasDigit)
                .toList();
        List<String> digitExceptionWords = reloadedExceptionWords.stream()
                .filter(this::hasDigit)
                .toList();

        // 네 사전을 요청마다 만들지 않고 캐시 갱신 시점에 컴파일함
        AhoCorasickMatcher badWordMatcher =
                AhoCorasickMatcher.from(reloadedBadWords);
        AhoCorasickMatcher exceptionWordMatcher =
                AhoCorasickMatcher.from(reloadedExceptionWords);
        AhoCorasickMatcher digitBadWordMatcher =
                AhoCorasickMatcher.from(digitBadWords);
        AhoCorasickMatcher digitExceptionWordMatcher =
                AhoCorasickMatcher.from(digitExceptionWords);

        // 완성된 불변 묶음의 참조를 한 번에 교체함
        BadWordCache reloadedCache = new BadWordCache(
                badWordMatcher, exceptionWordMatcher,
                digitBadWordMatcher, digitExceptionWordMatcher,
                now + BAD_WORD_CACHE_TTL_MILLIS);
        badWordCache = reloadedCache;
        return reloadedCache;
    }
}
```

Double Checked Locking을 사용해 캐시가 유효한 대부분의 요청은 `synchronized`에 들어가지 않습니다. 만료 시점에 여러 요청이 몰려도 첫 스레드만 사전을 읽고, 대기하던 스레드는 잠금 안의 두 번째 검사에서 새 캐시를 사용합니다.

## 파일명보다 바이트를 믿기

업로드 진입점은 원본 이름을 메타정보용으로만 정리하고 실제 형식은 바이트에서 판정합니다.

```java
public Long setUploadedImage(
        MultipartFile imageFile, String imageType, Long regiUser)
        throws IOException {
    // 수정 화면에서 파일을 바꾸지 않았다면 기존 파일을 유지함
    if (StringUtil.isEmpty(imageFile) || imageFile.isEmpty()) {
        return null;
    }

    // 프로필과 배경 이미지 이외의 임의 용도를 차단함
    validateImageType(imageType);
    // 경로와 제어문자를 제거한 이름은 표시용 메타정보로만 사용함
    String originalName =
            normalizeOriginalName(imageFile.getOriginalFilename());
    // 실제 바이트를 검증하고 안전한 이미지로 다시 만듦
    ValidatedImage validatedImage =
            validateAndNormalizeImage(imageFile.getBytes());
    return setValidatedImage(
            validatedImage, originalName, imageType, regiUser);
}
```

확장자가 `.jpg`라는 사실과 브라우저가 `image/jpeg`라고 보낸 사실은 허용 근거가 아닙니다. 두 값은 모두 요청자가 정할 수 있습니다.

## 이미지 검증과 재인코딩

```java
private ValidatedImage validateAndNormalizeImage(byte[] originalBytes) {
    // 압축 파일 자체의 크기를 가장 먼저 제한함
    if (StringUtil.isEmpty(originalBytes) || originalBytes.length == 0
            || originalBytes.length > maxImageBytes) {
        throw new InvalidImageFileException("Image file size is invalid.");
    }

    // 파일 선두 바이트로 JPEG 또는 PNG를 판별함
    ImageFormat imageFormat = detectImageFormat(originalBytes);
    if (StringUtil.isEmpty(imageFormat)) {
        throw new InvalidImageFileException(
                "Only JPEG and PNG image signatures are allowed.");
    }

    // JPEG 촬영 방향은 메타정보를 제거하기 전에 읽어 둠
    int exifOrientation = getExifOrientation(originalBytes, imageFormat);

    try (ByteArrayInputStream byteInput =
                    new ByteArrayInputStream(originalBytes);
         ImageInputStream imageInput =
                    ImageIO.createImageInputStream(byteInput)) {
        if (StringUtil.isEmpty(imageInput)) {
            throw new InvalidImageFileException(
                    "Image stream could not be created.");
        }

        Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
        // 시그니처만 흉내 낸 파일은 실제 이미지로 인정하지 않음
        if (!readers.hasNext()) {
            throw new InvalidImageFileException("Image decoder was not found.");
        }

        ImageReader reader = readers.next();
        try {
            reader.setInput(imageInput, true, true);
            // 헤더와 실제 디코더가 판단한 형식이 같아야 함
            if (!imageFormat.readerFormatName()
                    .equalsIgnoreCase(reader.getFormatName())) {
                throw new InvalidImageFileException(
                        "Image signature and decoder format do not match.");
            }

            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            // 곱셈 오버플로를 예외로 처리하며 전체 픽셀 수를 계산함
            long pixelCount = Math.multiplyExact((long) width, (long) height);
            // 픽셀을 메모리에 풀기 전에 차원과 전체 픽셀 수를 제한함
            if (width <= 0 || height <= 0
                    || width > maxImageDimension
                    || height > maxImageDimension
                    || pixelCount > maxImagePixels) {
                throw new InvalidImageFileException(
                        "Image dimensions are invalid.");
            }

            BufferedImage decodedImage = reader.read(0);
            if (StringUtil.isEmpty(decodedImage)) {
                throw new InvalidImageFileException("Image decoding failed.");
            }

            // EXIF 회전을 픽셀에 적용한 뒤 메타정보와 원본 부가 바이트를 버림
            BufferedImage orientedImage =
                    uptImageOrientation(decodedImage, exifOrientation);
            ByteArrayOutputStream normalizedOutput =
                    new ByteArrayOutputStream();
            boolean encoded = ImageIO.write(
                    orientedImage, imageFormat.imageIoName(), normalizedOutput);

            // 재인코딩 결과도 저장 크기 제한을 다시 적용함
            if (!encoded || normalizedOutput.size() == 0
                    || normalizedOutput.size() > maxImageBytes) {
                throw new InvalidImageFileException(
                        "Normalized image size is invalid.");
            }

            return new ValidatedImage(
                    normalizedOutput.toByteArray(),
                    imageFormat.mimeType(), imageFormat.extension());
        }
        finally {
            reader.dispose();
        }
    }
    catch (InvalidImageFileException e) {
        throw e;
    }
    catch (IOException | ArithmeticException e) {
        throw new InvalidImageFileException("Image validation failed.", e);
    }
}
```

검사 순서가 메모리 안전성과 연결됩니다. 먼저 압축 바이트 크기와 헤더를 검사하고, 디코더에서는 전체 픽셀을 읽기 전에 폭·높이·픽셀 수를 확인합니다. 그 뒤 실제 픽셀을 디코딩하고 새 파일로 인코딩합니다. 원본 끝에 붙은 실행 바이트와 불필요한 메타정보는 새 파일에 복사되지 않습니다.

## 임시 이미지 경로

프로필 편집 중 저장하지 않은 이미지는 서버가 생성한 UUID만 식별자로 허용합니다.

```java
private void validateDraftToken(String draftToken) {
    // 경로 문자나 임의 파일명을 사용할 수 없도록 UUID 파싱 결과만 허용함
    try {
        UUID.fromString(draftToken);
    }
    catch (IllegalArgumentException e) {
        throw new InvalidImageFileException(
                "Profile image draft token is invalid.", e);
    }
}

private Path getProfileDraftDir(Long userNumb, String imageType) {
    Path draftDirectory = profileImageDraftRootPath
            .resolve(String.valueOf(userNumb))
            .resolve(getUploadDirectoryName(imageType))
            .normalize();

    // 계산 경로가 지정한 임시 루트를 벗어나면 파일 접근을 차단함
    if (!draftDirectory.startsWith(profileImageDraftRootPath)) {
        throw new InvalidImageFileException(
                "Profile image draft path is invalid.");
    }
    return draftDirectory;
}
```

UUID 검증과 정규화 뒤 루트 경로 검사를 함께 사용합니다. 둘 중 하나만 믿지 않고 사용자 입력이 경로 탐색 문자열로 바뀌는 가능성을 두 단계에서 차단합니다.

## 한계

비속어 정규화는 우회 탐지를 높이는 대신 정상 표현을 과탐지할 수 있습니다. 예외 허용어를 위치 범위로 적용하지만 언어 문맥 전체를 이해하지는 않습니다. 이미지 재인코딩도 CPU와 메모리를 사용하므로 업로드 크기·차원 제한과 요청 제한을 함께 운영해야 합니다. 애니메이션 이미지나 JPEG·PNG 외 형식은 현재 의도적으로 지원하지 않습니다.

## 관련 소스

- [BadWordDetectionService.java](../../src/main/java/org/our/sadari/global/common/service/BadWordDetectionService.java)
- [FileService.java](../../src/main/java/org/our/sadari/global/file/service/FileService.java)
- [콘텐츠·파일 정책](../policies/content-file-policy.md)
