# Spring 트랜잭션으로 도서와 독후감을 함께 저장한 방법

## 문제

독후감 등록은 한 행을 추가하는 작업으로 끝나지 않습니다. 선택한 도서가 없으면 도서 정보를 먼저 만들고, 있으면 기존 식별자를 재사용해야 합니다. 공개 여부·알림 기본값·작성 언어·독서 상태·평점 정책도 저장 직전에 한 번 더 확정해야 합니다.

이 과정에서 도서만 저장되고 독후감 저장이 실패하면 사용자가 작성하지 않은 도서 데이터가 남습니다. 반대로 수정 화면에서는 다른 탭에서 먼저 저장한 내용을 나중 요청이 덮어쓸 수 있습니다. 등록은 트랜잭션으로 묶고, 수정은 버전값을 조건에 포함해 충돌을 감지했습니다.

## 등록 메서드

`ReportServiceImpl.setReport`의 실제 실행 순서입니다.

```java
@Override
@Transactional
public ResultData setReport(Long userNumb, ReportDto reportDto) {
    // 등록 요청의 도서 필수값이 누락되면 도서와 독후감 저장을 모두 중단함
    if (hasInvalidBookFields(reportDto)) {
        // "선택한 책 정보가 올바르지 않습니다. 다른 책을 선택해주세요."
        return ResultData.fail(ResultEnum.COMMON_REPORT_BOOK_INVALID);
    }

    // 인증에서 확인한 사용자 번호를 요청 DTO에 설정함
    reportDto.setUserNumb(userNumb);
    // 화면에서 색상을 보내지 않았을 때 사용할 기본 색상을 설정함
    setDefaultReportColor(reportDto);
    // 신규 독후감에 사용자 공개 및 알림 기본값을 적용함
    applyNewReportDefaults(reportDto);
    // 독후감 입력값에서 허용하지 않는 스크립트 내용을 제거함
    sanitizeReport(reportDto, true);
    // 읽는 중 독후감은 공개 목록과 평점 집계에 들어가지 않도록 저장값을 제한함
    applyReadingStatusPolicy(reportDto);

    // 정규화가 끝난 최종 저장값을 검증함
    ReportValidationResult validationResult = validateReport(reportDto, true);
    if (!StringUtil.isEmpty(validationResult)) {
        return ResultData.fail(validationResult.resultEnum(), validationResult.args());
    }

    // ISBN 기준 등록된 도서가 없을 때만 도서 정보를 신규 생성함
    if (bookMapper.dupBook(reportDto) == 0) {
        bookMapper.setBook(reportDto);
    }
    else {
        // 이미 있는 도서라면 독후감이 참조할 식별자를 재사용함
        reportDto.setBookNumb(
                bookMapper.getBookNumbByIsbn(reportDto.getBookIsbn()));
    }

    // 도서 식별자가 확정된 DTO로 독후감을 저장함
    reportMapper.setReport(reportDto);
    // 저장 뒤 생성 키가 없으면 성공으로 응답하지 않음
    if (StringUtil.isEmpty(reportDto.getReptNumb())) {
        // "저장에 실패했어요. 다시 시도해주세요."
        return ResultData.fail(ResultEnum.COMMON_SAVE_REJECTED);
    }

    return ResultData.success(reportDto.getReptNumb());
}
```

코드의 배치 자체가 정책 순서입니다.

1. `hasInvalidBookFields`가 도서 제목·ISBN처럼 도서를 식별하는 입력을 먼저 검사합니다. 이 단계에서는 데이터 변경이 없습니다.
2. `userNumb`는 요청 본문을 신뢰하지 않고 인증 정보에서 주입합니다.
3. 기본값을 먼저 적용한 뒤 XSS 정리와 독서 상태 정책을 적용합니다. 검증기가 실제 저장될 최종 값을 검사하게 하려는 순서입니다.
4. 기존 도서가 없으면 도서를 만들고, 있으면 생성하지 않고 식별자만 가져옵니다.
5. `@Transactional` 때문에 도서 등록 중 데이터베이스 예외가 발생하거나 독후감 등록에서 예외가 발생하면 두 작업이 함께 롤백됩니다.
6. 마지막에는 ORM처럼 반환 객체를 새로 만들지 않고 Mapper가 DTO에 채운 생성 키를 확인합니다.

## 기본값과 작성 언어

신규 독후감의 공개·알림 기본값은 프론트엔드가 보내는 값을 그대로 쓰지 않고 사용자 설정에서 가져옵니다.

```java
private void applyNewReportDefaults(ReportDto reportDto) {
    UserSettingDto setting = userMapper.getUserSettingDtl(reportDto.getUserNumb());

    // 등록 시점의 사용자 표시 언어를 독후감 원문 언어로 설정함
    setReportLanguage(reportDto, setting);

    // 화면이 공개 여부를 보내지 않았을 때만 사용자 기본값을 적용함
    if (StringUtil.isEmpty(reportDto.getPubcYsno())
            || reportDto.getPubcYsno().isBlank()) {
        reportDto.setPubcYsno(StringUtil.isEmpty(setting)
                ? Constant.COMM_NO
                : setting.getReportPublicDefaultYsno());
    }

    // 사용자 설정이 없으면 반응 알림을 받는 안전한 기본값을 사용함
    reportDto.setLikeAlimYsno(StringUtil.isEmpty(setting)
            ? Constant.COMM_YES
            : setting.getReportLikeDefaultYsno());
    reportDto.setReplyAlimYsno(StringUtil.isEmpty(setting)
            ? Constant.COMM_YES
            : setting.getReportReplyDefaultYsno());
}

private void setReportLanguage(ReportDto reportDto, UserSettingDto setting) {
    // 영어 설정인 사용자는 영어 원문, 나머지는 한국어 원문으로 저장함
    String languageCode = !StringUtil.isEmpty(setting)
            && Constant.COMM_YES.equals(setting.getEnglishYsno()) ? "en" : "ko";
    // 번역 방향 판정에 사용할 원문 언어 코드를 저장함
    reportDto.setLangCode(languageCode);
}
```

`langCode`는 번역 결과의 언어가 아니라 작성 당시 원문의 언어입니다. 사용자가 나중에 화면 언어를 바꾸더라도 이미 작성한 독후감의 원문 언어가 바뀌면 안 되기 때문입니다. 이 값이 있어야 목록에서 원문과 현재 표시 언어가 다를 때만 ‘번역 보기’를 노출할 수 있습니다.

## 독서 상태가 평점과 공개 범위를 바꾸는 이유

읽는 중인 기록에 별점과 공개 여부를 허용하면 아직 끝나지 않은 독서가 인기 도서 평점과 공개 피드에 섞입니다. 저장 시점에 상태 정책을 강제합니다.

```java
private void applyReadingStatusPolicy(ReportDto reportDto) {
    // 읽는 중이 아니면 사용자가 입력한 공개 여부와 평점을 유지함
    if (!Constant.REPORT_STAT_READ.equals(reportDto.getReptStat())) {
        return;
    }

    // 읽는 중 기록은 공개 목록에서 제외함
    reportDto.setPubcYsno(Constant.COMM_NO);
    // 아직 완료하지 않은 책은 평점 집계에 포함하지 않음
    reportDto.setReptGrde("0");
}
```

컨트롤러나 화면에서만 숨기지 않고 저장 서비스에서 값을 바꾸는 이유는 API를 직접 호출해도 같은 규칙이 적용되어야 하기 때문입니다. 수정으로 완료 상태에서 읽는 중 상태로 되돌릴 때도 이 메서드를 다시 호출해 기존 평점과 공개 여부를 제거합니다.

## 수정 충돌 감지

수정 메서드는 URL의 독후감 번호와 로그인 사용자를 DTO에 직접 넣고, 화면이 조회했던 버전값을 요구합니다.

```java
@Override
@Transactional
public ResultData uptReport(Long userNumb, Long reptNumb, ReportDto reportDto) {
    // 수정 대상과 화면이 읽은 버전이 없으면 수정하지 않음
    if (StringUtil.isEmpty(reptNumb) || StringUtil.isEmpty(reportDto)
            || StringUtil.isEmpty(reportDto.getEditVersion())) {
        return ResultData.fail(ResultEnum.COMMON_NO_DATA);
    }

    // 본문에 임의 번호가 있어도 URL과 인증 정보로 대상을 다시 확정함
    reportDto.setUserNumb(userNumb);
    reportDto.setReptNumb(reptNumb);
    setDefaultReportColor(reportDto);
    setDefaultPublicFlag(reportDto);
    setReportLanguage(reportDto, userMapper.getUserSettingDtl(userNumb));
    sanitizeReport(reportDto, false);
    applyReadingStatusPolicy(reportDto);

    ReportValidationResult validationResult = validateReport(reportDto, true);
    if (!StringUtil.isEmpty(validationResult)) {
        return ResultData.fail(validationResult.resultEnum(), validationResult.args());
    }

    // 소유자와 버전 조건이 맞는 행이 없으면 다른 저장과 충돌한 것으로 판단함
    if (reportMapper.uptReport(reportDto) == 0) {
        return ResultData.fail(ResultEnum.COMMON_EDIT_CONFLICT);
    }

    return ResultData.success(reportDto.getReptNumb());
}
```

영향받은 행이 0인 경우를 단순 ‘데이터 없음’으로 처리하지 않고 편집 충돌로 반환합니다. 다른 탭이나 기기에서 저장한 최신 내용을 조용히 덮어쓰지 않도록 사용자에게 다시 조회할 기회를 주는 낙관적 잠금 방식입니다.

## 실패 경로와 한계

데이터베이스 작업이 예외를 던지면 Spring 트랜잭션이 롤백합니다. 다만 `reportMapper.setReport`가 예외 없이 끝났는데 생성 키만 비어 있는 비정상 상황에서는 현재 코드가 실패 결과를 반환할 뿐 명시적으로 롤백 전용 상태를 표시하지 않습니다. Mapper 계약상 발생하지 않아야 하지만 방어 수준을 높이려면 예외를 던지거나 현재 트랜잭션을 rollback-only로 표시하는 편이 안전합니다.

또한 도서 구조는 언어 구분을 수용하지만, 현재 기존 도서를 찾는 Mapper 호출은 ISBN만 사용합니다. 동일 ISBN의 언어별 메타데이터를 따로 저장하려면 도서 검색 연동 전에 조회 조건을 ISBN과 언어의 조합으로 변경해야 합니다.

## 관련 소스

- [ReportServiceImpl.java](../../src/main/java/org/our/sadari/report/service/ReportServiceImpl.java)
- [ReportMapper.java](../../src/main/java/org/our/sadari/report/mapper/ReportMapper.java)
- [BookMapper.java](../../src/main/java/org/our/sadari/book/mapper/BookMapper.java)
