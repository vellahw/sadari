# Spring 트랜잭션으로 도서와 독후감을 함께 저장한 방법

## 시작한 문제

외부 검색에서 선택한 책은 아직 내부 도서 정보에 없을 수 있다. 이때 도서만 저장되고 독후감 저장이 실패하면 사용자가 한 번도 기록하지 않은 도서 데이터가 남는다. 반대로 독후감부터 저장할 수는 없으므로 두 작업의 성공 조건을 하나로 묶어야 했다.

수정 화면에서는 다른 탭이나 기기에서 먼저 바꾼 내용을 뒤늦은 저장이 덮어쓰는 문제도 있었다. 읽는 중인 기록이 공개 목록이나 평균 별점에 섞이지 않게 하는 상태 정책 역시 프론트엔드 제어만으로는 충분하지 않았다.

## 서비스가 책임지는 저장 경계

`ReportServiceImpl.setReport`를 쓰기 트랜잭션으로 두고, 입력 정규화와 검증을 통과한 뒤 도서 존재 여부를 확인한다. 도서가 없으면 먼저 만들고 생성된 식별자를 사용하며, 이미 있으면 기존 식별자를 재사용한다. 마지막 독후감 저장까지 실패하면 앞선 도서 저장도 함께 롤백된다. 아래 코드는 검증을 통과한 뒤의 저장 분기만 남긴 축약본이다.

```java
@Transactional
public ResultData setReport(Long userNumb, ReportDto reportDto) {
    sanitizeReport(reportDto, true);
    applyReadingStatusPolicy(reportDto);

    if (bookMapper.dupBook(reportDto) == 0) {
        bookMapper.setBook(reportDto);
    } else {
        reportDto.setBookNumb(bookMapper.getBookNumbByIsbn(reportDto.getBookIsbn()));
    }

    reportMapper.setReport(reportDto);
    return ResultData.success(reportDto.getReptNumb());
}
```

실제 메서드는 도서 필수값, 독서 기간, UTF-8 바이트 길이, 공개 여부, 별점, 비속어를 DB 접근 전에 검증한다. 인증 사용자 번호는 요청 본문을 신뢰하지 않고 서버가 주입한다.

## 상태별 불변식

읽는 중 기록은 아직 평가가 끝나지 않았기 때문에 서버에서 평점을 집계 제외값으로 바꾸고 공개 여부를 비공개로 강제한다. 완료 또는 중단 상태에서만 사용자가 선택한 평점과 공개 여부를 유지한다. 이 규칙을 등록과 수정 모두에 적용해 조작된 요청도 같은 결과로 수렴시켰다.

도서 평균 평점은 도서 정보의 언어와 분리했다. 동일 ISBN에 연결된 모든 언어의 완료·중단 독후감을 합산하므로, 한국어 도서 정보 화면과 영어 도서 정보 화면에서 서로 다른 평점이 생기지 않는다.

## 수정 충돌을 감지한 방법

상세 조회 시 독후감의 편집 대상 필드를 해시한 버전을 내려준다. 수정 쿼리는 사용자 식별자와 독후감 식별자뿐 아니라 이 편집 버전까지 조건으로 사용한다. 조회 이후 다른 저장이 먼저 일어나면 반영 행이 0건이 되고, 서비스는 일반 저장 실패가 아니라 편집 충돌로 응답한다.

이 방식은 별도 버전 숫자를 증가시키지 않아도 되지만, 해시에 포함하는 필드가 바뀌면 조회와 수정 조건을 함께 관리해야 한다. 또한 현재 도서 재사용 조회는 ISBN 중심이다. 영어권 도서 검색을 추가할 때는 원본 DDL의 언어별 도서 식별 정책과 서비스 조회 조건을 일치시키는 보완이 필요하다.

## 구현 근거

- [ReportServiceImpl.java](../../src/main/java/org/our/sadari/report/service/ReportServiceImpl.java)
- [ReportMapper.xml](../../src/main/java/org/our/sadari/report/mapper/ReportMapper.xml)
- [BookMapper.xml](../../src/main/java/org/our/sadari/book/mapper/BookMapper.xml)
- [백엔드와 데이터 설계](../portfolio/backend-data.md)
