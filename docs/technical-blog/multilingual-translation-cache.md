# Google 번역 API를 온디맨드 캐시와 월 50만 자 제한으로 붙인 방법

## 문제

화면 메시지만 영어로 바꾸어도 데이터베이스에서 읽는 공지·메뉴·공통 명칭·알림 문구와 사용자가 작성한 독후감은 그대로 남습니다. 모든 독후감을 미리 번역하면 읽히지 않는 콘텐츠에도 비용이 발생하고, 원문이 수정됐을 때 번역이 오래된 상태로 남습니다.

Sadari는 한국어와 영어 두 언어만 지원합니다. 계정 설정이 아직 없는 기존 사용자는 최초 설정 조회 시 기기 언어를 기본값으로 확정합니다. 공개 독후감은 원문 언어와 현재 표시 언어가 다를 때만 ‘번역 보기’를 노출하고, 사용자가 버튼을 누르는 순간 번역을 생성해 원문 해시와 함께 캐시합니다.

## 브라우저 메시지 언어 결정

프론트엔드는 properties 파일을 읽어 메시지 맵을 만들고, 저장된 계정 언어가 없을 때만 기기 언어를 사용합니다.

```typescript
import koMessages from "./messages_ko.properties?raw";
import enMessages from "./messages_en.properties?raw";

const MESSAGE_LOCALE_STORAGE_KEY = "sadari:message-locale";

/** properties 형식 메시지 파일을 key-value 객체로 변환함 */
const parseProperties = (source: string) => {
  return source
    .split(/\r?\n/)
    .map((line) => line.trim())
    // 빈 줄과 주석은 메시지에서 제외함
    .filter((line) => line && !line.startsWith("#"))
    .reduce<Record<string, string>>((messages, line) => {
      const separatorIndex = line.indexOf("=");
      if (separatorIndex === -1) {
        return messages;
      }

      const key = line.slice(0, separatorIndex).trim();
      const value = line
        .slice(separatorIndex + 1)
        .trim()
        .replace(/\\n/g, "\n")
        .replace(/\\u([0-9a-fA-F]{4})/g, (_, hex) =>
          String.fromCharCode(parseInt(hex, 16)),
        );

      messages[key] = value;
      return messages;
    }, {});
};

const MESSAGE_SOURCES = {
  ko: parseProperties(koMessages),
  en: parseProperties(enMessages),
};
```

직접 만든 파서는 첫 번째 `=`만 구분자로 사용합니다. 메시지 값 안의 `=`은 보존하고, 줄바꿈과 유니코드 이스케이프만 복원합니다.

```typescript
/** 브라우저 언어가 영어로 시작하는지 Y/N으로 반환함 */
export const getDeviceEnglishYsno = (): "Y" | "N" =>
  navigator.language.toLowerCase().startsWith("en") ? "Y" : "N";

/** 저장된 계정 언어가 없으면 현재 기기 언어를 사용함 */
export const getMessageLocale = (): "en" | "ko" => {
  const savedLocale = window.localStorage.getItem(
    MESSAGE_LOCALE_STORAGE_KEY,
  );
  if (savedLocale === "en" || savedLocale === "ko") {
    return savedLocale;
  }

  return getDeviceEnglishYsno() === "Y" ? "en" : "ko";
};

/** 서버에서 확정한 계정 언어를 현재 브라우저에 저장함 */
export const setMessageLocale = (englishYsno: "Y" | "N"): void => {
  window.localStorage.setItem(
    MESSAGE_LOCALE_STORAGE_KEY,
    englishYsno === "Y" ? "en" : "ko",
  );
};

/** 현재 언어의 메시지를 찾고 없으면 한국어와 key 순서로 대체함 */
export const message = (key: string, params: MessageParams = []) => {
  const localeMessages = MESSAGE_SOURCES[getMessageLocale()];
  const fallbackMessages = MESSAGE_SOURCES.ko;
  const template = localeMessages[key] ?? fallbackMessages[key] ?? key;

  return params.reduce<string>(
    (result, param, index) =>
      result.split(`{${index}}`).join(String(param)),
    template,
  );
};
```

영문 키가 빠졌을 때 화면이 비는 대신 한국어로 대체합니다. 한국어에도 없으면 key를 보여 주므로 누락을 발견할 수 있습니다. API 요청 인터셉터는 `getMessageLocale()` 값을 `Accept-Language` 헤더에 넣어 서버 메시지와 데이터 언어도 같은 기준을 사용합니다.

## 최초 기기 언어를 계정 설정으로 확정

브라우저 저장값만 사용하면 다른 기기에서 로그인할 때마다 언어가 달라집니다. 서버는 계정 설정이 비어 있는 첫 조회에서 요청 언어를 저장합니다.

```java
@Override
@Transactional
public ResultData getUserSetting(Long userNumb) {
    if (StringUtil.isEmpty(userNumb)) {
        return ResultData.fail(ResultEnum.AUTH_FAIL);
    }

    UserSettingDto setting = userMapper.getUserSettingDtl(userNumb);
    if (StringUtil.isEmpty(setting)) {
        return ResultData.fail(ResultEnum.AUTH_FAIL);
    }

    // 아직 선택하지 않은 기존 계정은 최초 요청 기기 언어를 계정 설정으로 확정함
    if (StringUtil.isEmpty(setting.getEnglishYsno())) {
        setting.setEnglishYsno(LocaleUtil.getEnglishYsno());
        userMapper.uptUserLanguageSetting(setting);
    }

    return ResultData.success(setting);
}

@Override
@Transactional
public ResultData uptUserLanguageSetting(
        Long userNumb, UserSettingDto request) {
    if (StringUtil.isEmpty(userNumb) || StringUtil.isEmpty(request)
            || StringUtil.isEmpty(request.getEnglishYsno())) {
        return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
    }

    String englishYsno = request.getEnglishYsno()
            .trim().toUpperCase(Locale.ROOT);
    // 한국어와 영어를 나타내는 두 값만 허용함
    if (!Constant.COMM_YES.equals(englishYsno)
            && !Constant.COMM_NO.equals(englishYsno)) {
        return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
    }

    request.setUserNumb(userNumb);
    request.setEnglishYsno(englishYsno);
    userMapper.uptUserLanguageSetting(request);
    return ResultData.success(userMapper.getUserSettingDtl(userNumb));
}
```

현재 요구사항은 다수 언어 확장이 아니라 영문 추가이므로 별도 언어 설정 행을 늘리지 않고 영문 사용 여부 한 값으로 관리합니다. 언어 종류가 늘어날 계획이 생기면 이 계약은 언어 코드 방식으로 바꿔야 합니다.

## 설정 화면은 셀렉트박스로 표현

언어는 켜고 끄는 기능이 아니라 두 언어 중 하나를 선택하는 값이므로 공통 셀렉트 컴포넌트를 사용합니다.

```tsx
const languageOptions: readonly CustomSelectOption<
  UserSetting["englishYsno"]
>[] = [
  {
    value: "N",
    // "한국어"
    label: message("frontend.settings.language.korean"),
  },
  {
    value: "Y",
    // "영어"
    label: message("frontend.settings.language.english"),
  },
];

/** 표시 언어 셀렉트박스 선택값을 영어 사용 여부에 반영함 */
const handleLanguageChange = (
  englishYsno: UserSetting["englishYsno"],
) => {
  // 선택한 언어 값을 현재 설정에 반영함
  setSetting((current) =>
    current ? { ...current, englishYsno } : current,
  );
};
```

저장 성공 뒤에는 서버가 반환한 값을 브라우저에 기록하고 페이지를 다시 불러옵니다.

```tsx
const saved = await runBlockingOperation(
  () => section === "notifications"
    ? uptUserAlimSettingApi({
        likeAlimYsno: setting.likeAlimYsno,
        replyAlimYsno: setting.replyAlimYsno,
        followAlimYsno: setting.followAlimYsno,
        clubAlimYsno: setting.clubAlimYsno,
        chatAlimYsno: setting.chatAlimYsno,
        reportDueAlimYsno: setting.reportDueAlimYsno,
        reportLikeDefaultYsno: setting.reportLikeDefaultYsno,
        reportReplyDefaultYsno: setting.reportReplyDefaultYsno,
      })
    : section === "privacy"
      ? uptUserPrivacyApi({
          readingStatisticsYsno: setting.readingStatisticsYsno,
          readingGoalYsno: setting.readingGoalYsno,
          imageFeedYsno: setting.imageFeedYsno,
          reportPublicDefaultYsno: setting.reportPublicDefaultYsno,
        })
      : uptUserLanguageApi({ englishYsno: setting.englishYsno }),
  {
    title: message("frontend.settings.saving"),
    success: { title: message("frontend.settings.save.successTitle") },
  },
);

setSetting(saved);
setSavedSetting(saved);
if (section === "language") {
  // 서버가 확정한 값만 로컬 메시지 언어로 사용함
  setMessageLocale(saved.englishYsno);
  // 이미 렌더링된 모든 메시지와 데이터 요청을 같은 언어로 다시 시작함
  window.location.reload();
}
```

선택 순간에 화면 언어부터 바꾸면 서버 저장이 실패했을 때 계정 설정과 현재 화면이 달라집니다. 서버 저장 성공 이후에만 로컬 값을 바꾸는 이유입니다.

## 번역 버튼을 보여 줄 수 있는지 먼저 계산

독후감 목록을 반환할 때 서버는 카드마다 버튼 노출 여부를 계산합니다.

```java
public void applyReportAvailability(List<ReportDto> reports) {
    // 목록이 비어 있으면 Redis 조회도 하지 않음
    if (StringUtil.isEmpty(reports)) {
        return;
    }

    // 한 응답의 모든 카드가 같은 월간 잔여량을 사용하도록 한 번만 조회함
    long remainingChars = getRemainingChars();

    for (ReportDto report : reports) {
        report.setTrnsAvaiYsno(getAvailability(
                report.getLangCode(),
                report.getReptCntn(),
                report.getTrnsCacheYsno(),
                remainingChars));
    }
}

private String getAvailability(
        String sourceLanguage, String sourceContent,
        String cacheYsno, long remainingChars) {
    // 본문이 없거나 원문과 표시 언어가 같으면 번역 버튼을 숨김
    if (StringUtil.hasEmpty(sourceLanguage, sourceContent)
            || getTargetLanguage().equals(sourceLanguage)) {
        return Constant.COMM_NO;
    }

    // 이미 생성된 캐시는 API 키와 월간 잔여량 없이도 다시 볼 수 있음
    if (Constant.COMM_YES.equals(cacheYsno)) {
        return Constant.COMM_YES;
    }

    // 새 번역은 키가 있고 남은 글자 수가 원문 이상일 때만 노출함
    int sourceChars = sourceContent.codePointCount(
            0, sourceContent.length());
    boolean available = !StringUtil.isEmpty(translationApiKey)
            && remainingChars >= sourceChars;
    return available ? Constant.COMM_YES : Constant.COMM_NO;
}
```

월 50만 자가 모두 소진돼도 기존 캐시를 다시 읽는 데는 비용이 들지 않으므로 버튼을 유지합니다. 새 번역만 필요한 카드에서는 남은 글자 수보다 원문이 길면 버튼 자체를 숨깁니다. Redis 사용량을 읽지 못했을 때는 음수 잔여량으로 처리해 새 비용이 발생하지 않게 합니다.

## 버튼을 누른 순간 번역하고 캐시하기

`ReportTranslationService.setReportTranslation`은 공개 범위 재검증, 캐시 조회, 한도 예약, Google 호출, 저장을 한 흐름으로 처리합니다.

```java
@Transactional
public ResultData setReportTranslation(Long userNumb, Long reptNumb) {
    // 인증 사용자와 양수 독후감 번호를 먼저 확인함
    if (StringUtil.hasEmpty(userNumb, reptNumb) || reptNumb <= 0) {
        return ResultData.fail(ResultEnum.COMMON_INVALID_REQUEST);
    }

    // 공개 범위와 차단 관계를 다시 검증하고 같은 원문의 중복 번역을 잠금
    ReportTranslationDto source = getLockedSource(userNumb, reptNumb);
    if (StringUtil.isEmpty(source)) {
        return ResultData.fail(ResultEnum.FORBIDDEN);
    }

    String targetLanguage = getTargetLanguage();
    // 원문과 대상 언어가 같거나 본문이 없으면 번역하지 않음
    if (targetLanguage.equals(source.getSourceLangCode())
            || StringUtil.isEmpty(source.getSourceContent())) {
        return ResultData.fail(ResultEnum.REPORT_TRANSLATION_UNAVAILABLE);
    }

    source.setLangCode(targetLanguage);
    // 원문 수정 여부를 판단할 SHA-256 해시를 생성함
    source.setOrigHash(getSha256(source.getSourceContent()));
    ReportTranslationDto cachedTranslation =
            reportMapper.getReportTrnsDtl(source);

    // 같은 원문과 대상 언어의 캐시는 외부 호출과 한도 차감 없이 반환함
    if (!StringUtil.isEmpty(cachedTranslation)
            && source.getOrigHash().equals(
                    cachedTranslation.getOrigHash())) {
        cachedTranslation.setCached(true);
        return ResultData.success(cachedTranslation);
    }

    // API 키가 없으면 비용 경로에 진입하지 않음
    if (StringUtil.isEmpty(translationApiKey)) {
        return ResultData.fail(ResultEnum.REPORT_TRANSLATION_FAILED);
    }

    // Google 과금 기준에 맞춰 UTF-16 length가 아닌 코드 포인트를 셈
    int requestChars = source.getSourceContent().codePointCount(
            0, source.getSourceContent().length());
    // Redis Lua가 월간 한도를 원자적으로 예약한 요청만 외부 호출함
    if (!reserveChars(requestChars)) {
        return ResultData.fail(ResultEnum.REPORT_TRANSLATION_LIMITED);
    }

    try {
        String translatedContent = getGoogleTranslation(
                source.getSourceContent(),
                source.getSourceLangCode(), targetLanguage);
        source.setTrnsCntn(translatedContent);
        // 같은 독후감과 대상 언어의 캐시를 원문 해시와 함께 갱신함
        reportMapper.setReportTrns(source);
        source.setCached(false);
        return ResultData.success(source);
    }
    catch (HttpMessageConversionException
            | RestClientException | IllegalStateException e) {
        // 원문과 API 키를 로그에 남기지 않음
        log.error("독후감 번역 처리에 실패했습니다. 독후감 번호={}", reptNumb, e);
        return ResultData.fail(ResultEnum.REPORT_TRANSLATION_FAILED);
    }
}
```

원문 해시가 캐시 키의 일부이므로 작성자가 독후감을 수정하면 과거 번역을 반환하지 않습니다. 같은 원문에 대한 동시 요청은 잠금 조회로 직렬화해 둘 다 Google을 호출하는 상황을 줄입니다. 월간 사용량도 Java에서 읽고 나중에 증가하지 않고 Redis Lua에서 검사와 예약을 한 번에 처리합니다.

Google API 키는 URL 쿼리 문자열에 넣지 않고 요청 헤더에 전달합니다. 애플리케이션 설정에는 값이 필요하지만 문서와 저장소에는 실제 키를 기록하지 않습니다.

## 카드에서 원문과 번역문 전환

React 컴포넌트는 한 번 받은 번역문을 카드 번호별 상태에 저장합니다.

```tsx
/** 카드에 표시할 원문 또는 번역문을 반환함 */
const getVisibleContent = (report: ReportListItem): string => {
  // 번역 보기 상태이면서 번역문이 있으면 번역문을 표시함
  if (translatedReports[report.reptNumb]
      && translations[report.reptNumb]) {
    return translations[report.reptNumb];
  }

  // 원문 보기 상태이면 서버가 조회한 원문을 반환함
  return report.reportContent;
};

/** 번역 캐시를 조회하거나 생성한 뒤 표시 상태를 전환함 */
const handleTranslation = async (report: ReportListItem): Promise<void> => {
  // 현재 화면에 번역문이 있으면 네트워크 요청 없이 상태만 반전함
  if (translations[report.reptNumb]) {
    setTranslatedReports((current) => ({
      ...current,
      [report.reptNumb]: !current[report.reptNumb],
    }));
    return;
  }

  // 같은 카드의 중복 요청을 차단함
  if (pendingReportNumb === report.reptNumb) {
    return;
  }

  setPendingReportNumb(report.reptNumb);
  try {
    const translation = await setReportTranslationApi(report.reptNumb);
    setTranslations((current) => ({
      ...current,
      [report.reptNumb]: translation.trnsCntn,
    }));
    setTranslatedReports((current) => ({
      ...current,
      [report.reptNumb]: true,
    }));
  }
  catch (error) {
    await sweetError(
      message("frontend.report.translation.failedTitle"),
      getApiErrorMessage(error, message("frontend.common.tryAgain")),
    );
  }
  finally {
    setPendingReportNumb(undefined);
  }
};
```

첫 클릭은 서버 캐시를 읽거나 새 번역을 만듭니다. 같은 화면에서 ‘원문 보기’와 ‘번역 보기’를 다시 누를 때는 API를 호출하지 않고 로컬 상태만 바꿉니다. 본문 전환 애니메이션은 사용하지 않아 모바일에서 카드 높이가 바뀔 때 불필요한 슬라이드가 생기지 않습니다.

버튼은 카드 푸터의 첫 번째 자식이고, 좋아요·댓글은 오른쪽 묶음입니다.

```tsx
<footer className={styles.itemFooter}>
  {report.trnsAvaiYsno === "Y" ? (
    <button
      className={styles.translationButton}
      type="button"
      disabled={pendingReportNumb === report.reptNumb}
      onClick={() => void handleTranslation(report)}
    >
      {pendingReportNumb === report.reptNumb
        ? message("frontend.report.translation.loading")
        : message(translatedReports[report.reptNumb]
          ? "frontend.report.translation.original"
          : "frontend.report.translation.view")}
    </button>
  ) : <span />}

  <div className={styles.itemMetrics}>
    <button
      className={styles.metricIconButton}
      type="button"
      aria-label={message("frontend.common.like")}
      aria-pressed={report.likeYsno === "Y"}
      disabled={isLikePending}
      onClick={() => onLike(report)}
    >
      <img
        src={report.likeYsno === "Y"
          ? "/img/icons/icon-heart-fill.svg"
          : "/img/icons/icon-heart.svg"}
        alt=""
        aria-hidden="true"
      />
    </button>
    <button
      className={styles.commentButton}
      type="button"
      aria-label={message("frontend.book.publicReports.viewComments")}
      onClick={() => onOpenReply(report)}
    >
      <img src="/img/icons/icon-comment.svg" alt="" aria-hidden="true" />
      <span>{report.commentCountLabel}</span>
    </button>
  </div>
</footer>
```

번역 버튼이 없는 카드에도 빈 `span`을 두는 것은 오른쪽 지표 묶음의 위치를 유지하기 위해서입니다. 버튼 색상은 마이페이지의 보조 행동 버튼과 같은 옅은 회색 계열을 사용합니다.

## 비용과 정합성의 범위

월 50만 자는 Google 계정의 청구를 직접 막는 결제 한도가 아니라 Sadari 애플리케이션이 Redis로 적용하는 내부 상한입니다. 다른 애플리케이션이 같은 Google 프로젝트를 사용하거나 Redis 카운터가 초기화되면 실제 청구량과 다를 수 있습니다. 운영에서는 Google Cloud 예산 알림과 API 할당량도 함께 설정해야 합니다.

외부 호출이 성공한 뒤 캐시 저장이 실패하면 예약한 글자 수를 현재 코드가 자동 환급하지 않습니다. 중복 비용을 피하기 위해 보수적으로 차감한 상태를 유지하는 선택이지만, 실패율이 높아지면 호출 식별자와 정산 작업이 필요합니다.

영어권 도서 검색은 이 번역 기능에 포함되지 않습니다. 현재 도서 검색은 Kakao만 사용하며 Google Books 연동은 다음 단계입니다.

## 관련 소스

- [message.ts](../../src/main/frontend/src/app/messages/message.ts)
- [axios.ts](../../src/main/frontend/src/app/api/axios.ts)
- [UserSettingsPage.tsx](../../src/main/frontend/src/pages/Settings/UserSettingsPage.tsx)
- [UserServiceImpl.java](../../src/main/java/org/our/sadari/user/service/UserServiceImpl.java)
- [ReportTranslationService.java](../../src/main/java/org/our/sadari/report/service/ReportTranslationService.java)
- [ReportListView.tsx](../../src/main/frontend/src/components/ReportList/ReportListView.tsx)
