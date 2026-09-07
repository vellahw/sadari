import { globalStyle, style } from "@vanilla-extract/css";
import { vars } from "@/app/styles/tokens.css";

// 피드 페이지의 공통 너비와 상하 여백을 정의함
export const page = style({
  width: "100%",
  maxWidth: "600px",
  margin: "0 auto",
  padding: "0 0 36px",
  boxSizing: "border-box",
});

// 공통 Container 여백을 상쇄해 홈과 같은 화면 위치에 검색 입력을 배치함
export const userSearchBar = style({
  width: "calc(100% + 32px)",
  marginLeft: "-16px",
});

// 피드 검색어 지우기 버튼을 검색 아이콘과 분리하고 기본 크기보다 조금 크게 표시함
globalStyle(`${userSearchBar} input[type="search"]::-webkit-search-cancel-button`, {
  marginRight: "6px",
  transform: "scale(1.2)",
  cursor: "pointer",
});

// 활성 사용자 검색의 로딩과 목록 및 추가 조회 영역을 세로로 배치함
export const userSearchResults = style({
  minHeight: "120px",
});

// 마이페이지 관계 목록과 같은 사용자 행을 페이지 흐름에 누적함
export const userSearchList = style({
  display: "flex",
  flexDirection: "column",
});

// 피드 카드가 일정한 간격으로 누적되는 목록 배치를 정의함
export const list = style({ display: "grid", gap: "14px" });

// 피드 유형별 콘텐츠를 담는 공통 카드 표면을 정의함
export const card = style({
  position: "relative",
  overflow: "hidden",
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: "22px",
  background: vars.color.background,
  boxSizing: "border-box",
});

// 피드 작성자 정보를 카드 상단에 배치하는 영역을 정의함
export const cardHeader = style({
  display: "flex",
  alignItems: "center",
  gap: "10px",
  padding: "16px 16px 12px",
});

// 다른 사용자 피드의 신고 및 차단 메뉴가 카드 우측 상단에 고정되도록 정의함
export const actionMenuWrap = style({
  display: "flex",
  flexShrink: 0,
});

// 다른 사람 독후감 카드와 같은 가로 점 세 개 아이콘 방향을 적용함
export const actionMenuTriggerIcon = style({
  transform: "rotate(90deg)",
});

// 피드 작성자 프로필로 이동하는 전체 너비 버튼 상태를 정의함
export const authorButton = style({
  display: "flex",
  flexDirection: "column",
  alignItems: "flex-start",
  gap: "6px",
  width: "100%",
  minWidth: 0,
  maxWidth: "100%",
  padding: 0,
  border: 0,
  background: "transparent",
  textAlign: "left",
  cursor: "pointer",
  selectors: {
    "&:hover": { opacity: 0.78 },
    "&:focus-visible": { outline: `2px solid ${vars.color.brand}`, outlineOffset: 3, borderRadius: 8 },
  },
});

// 프로필 사진과 닉네임을 한 행으로 정렬하는 영역을 정의함
export const authorIdentity = style({
  display: "inline-flex",
  alignItems: "center",
  gap: "8px",
  minWidth: 0,
  maxWidth: "100%",
});

// 피드 카드 상단에 표시할 작성자 프로필 사진 크기를 정의함
export const avatar = style({
  width: "30px",
  height: "30px",
  flexShrink: 0,
  borderRadius: "50%",
  objectFit: "cover",
  backgroundColor: vars.color.gray300,
});

// 긴 닉네임이 카드 너비를 넘지 않도록 표시 형식을 정의함
export const authorName = style({
  flex: "0 1 auto",
  minWidth: 0,
  maxWidth: "100%",
  color: vars.color.black,
  fontFamily: vars.font.semibold,
  fontSize: "16px",
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
});

// 독후감 저자 옆에 표시하는 공개 날짜의 보조 문구를 정의함
export const activityDate = style({
  flexShrink: 0,
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: "12px",
  lineHeight: 1.4,
  whiteSpace: "nowrap",
});

// 사진 피드 원본 보기를 실행하는 공통 미디어 버튼을 정의함
export const mediaButton = style({
  display: "grid",
  gap: "14px",
  width: "100%",
  padding: "0 16px 16px",
  border: 0,
  background: "transparent",
  textAlign: "left",
  cursor: "pointer",
  boxSizing: "border-box",
  selectors: {
    "&:hover": { background: vars.color.gray100 },
    "&:focus-visible": { outline: `2px solid ${vars.color.brand}`, outlineOffset: -2 },
  },
});

// 독후감 표지와 도서 정보를 한 행으로 배치하는 영역을 정의함
export const reportMediaRow = style({
  display: "grid",
  gridTemplateColumns: "50px minmax(0, 1fr)",
  alignItems: "center",
  gap: "14px",
  width: "100%",
  padding: "0 16px 16px",
  boxSizing: "border-box",
});

// 도서 제목 검색으로 이동하는 표지 링크 상태를 정의함
export const reportCoverLink = style({
  display: "block",
  borderRadius: "4px",
  selectors: {
    "&:hover": { opacity: 0.78 },
    "&:focus-visible": { outline: `2px solid ${vars.color.brand}`, outlineOffset: 3 },
  },
});

// 사진과 변경 설명을 세로로 배치하도록 공통 미디어 버튼을 확장함
export const backgroundMediaButton = style([
  mediaButton,
  {
    gridTemplateColumns: "minmax(0, 1fr)",
    gap: "6px",
  },
]);

// 프로필과 배경사진을 동일한 비율로 자르는 미디어 영역을 정의함
export const backgroundMediaWrap = style({
  position: "relative",
  display: "block",
  width: "100%",
  aspectRatio: "16 / 9",
  overflow: "hidden",
  borderRadius: "12px",
  background: vars.color.gray100,
});

// 도서 표지와 사진 피드가 공유하는 이미지 표시 방식을 정의함
export const media = style({
  display: "block",
  background: vars.color.gray100,
  objectFit: "cover",
});
// 공통 미디어 스타일에 독후감 표지 크기와 모서리를 결합함
export const reportMedia = style([media, { width: "50px", height: "74px", borderRadius: "4px" }]);
// 공통 미디어 스타일에 사진 피드 전체 크기와 모서리를 결합함
export const backgroundMedia = style([media, { width: "100%", height: "100%", borderRadius: "12px" }]);
// 사진 변경 유형과 날짜를 사진 아래 오른쪽에 표시하는 문구를 정의함
export const imageActivity = style({
  display: "block",
  justifySelf: "end",
  maxWidth: "100%",
  margin: 0,
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: "12px",
  lineHeight: 1.4,
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
});
// 독후감 도서 정보를 세로로 정렬하고 긴 문구의 축소를 허용함
export const mediaInfo = style({ minWidth: 0, alignSelf: "center", display: "flex", flexDirection: "column", gap: "3px" });
// 도서 저자와 독후감 공개 날짜를 한 행으로 배치함
export const bookAuthorRow = style({
  display: "flex",
  alignItems: "center",
  gap: "8px",
  minWidth: 0,
});
// 도서 정보 상세로 이동하는 텍스트 링크 상태를 정의함
export const bookInfoLink = style({
  display: "block",
  minWidth: 0,
  color: "inherit",
  textDecoration: "none",
  borderRadius: "4px",
  selectors: {
    "&:hover": { background: vars.color.gray100 },
    "&:focus-visible": { outline: `2px solid ${vars.color.brand}`, outlineOffset: 2 },
  },
});
// 긴 도서 제목을 한 줄 말줄임으로 표시하는 형식을 정의함
export const title = style({ display: "block", margin: 0, fontFamily: vars.font.semibold, fontSize: "14px", lineHeight: 1.25, color: vars.color.black, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" });
// 저자명 검색으로 이동하는 보조 링크 상태를 정의함
export const authorSearchLink = style({
  display: "block",
  minWidth: 0,
  width: "fit-content",
  maxWidth: "100%",
  borderRadius: "4px",
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: "12px",
  lineHeight: 1.25,
  overflow: "hidden",
  textDecoration: "none",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
  selectors: {
    "&:hover": { background: vars.color.gray100, color: vars.color.black },
    "&:focus-visible": { outline: `2px solid ${vars.color.brand}`, outlineOffset: 2 },
  },
});
// 독후감 평점과 독서 상태를 한 행으로 표시하는 영역을 정의함
export const ratingStatusRow = style({ minWidth: 0, minHeight: "24px", marginTop: "4px", display: "flex", alignItems: "center", justifyContent: "flex-start", gap: "8px" });
// 독후감 별점 아이콘과 값을 함께 표시하는 형식을 정의함
export const rating = style({ display: "inline-flex", alignItems: "center", gap: "3px", color: vars.color.black, fontFamily: vars.font.semibold, fontSize: "14px", lineHeight: 1.45 });
// 독후감 별점에 사용하는 아이콘 크기와 색상을 정의함
export const ratingIcon = style({ width: "18px", height: "18px", display: "block", flexShrink: 0, color: "#ffd45c" });

// 독후감 본문과 펼침 버튼을 카드 내부 여백에 배치함
export const contentSection = style({
  padding: "0 16px 12px",
});

// 독후감 본문에서 도서 정보 상세로 이동하는 링크 상태를 정의함
export const reportContentLink = style({
  display: "block",
  borderRadius: "4px",
  color: "inherit",
  textDecoration: "none",
  selectors: {
    "&:hover": { background: vars.color.gray100 },
    "&:focus-visible": { outline: `2px solid ${vars.color.brand}`, outlineOffset: 2 },
  },
});

// 피드 카드 하단의 번역과 교류 기능을 양쪽에 배치함
export const actions = style({
  width: "100%",
  minHeight: "24px",
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  gap: "8px",
  marginTop: "auto",
  padding: "0 16px 16px",
  boxSizing: "border-box",
});
// 피드 카드 우측의 좋아요와 댓글 기능을 한 묶음으로 정렬함
export const reactionActions = style({
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "flex-end",
  gap: "8px",
  marginLeft: "auto",
});
// 좋아요와 댓글 아이콘 버튼이 공유하는 크기와 상태를 정의함
export const actionButton = style({
  minWidth: "32px",
  height: "24px",
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  gap: "4px",
  padding: 0,
  border: 0,
  background: "transparent",
  color: "#ff747c",
  fontFamily: vars.font.body,
  fontSize: "14px",
  cursor: "pointer",
  selectors: {
    "&:hover": { background: vars.color.gray100 },
    "&:focus-visible": { outline: `2px solid ${vars.color.brand}`, outlineOffset: 1 },
  },
});
// 좋아요 아이콘과 사용자 수 버튼을 하나의 제어 영역으로 정렬함
export const likeActionGroup = style({
  minWidth: "32px",
  height: "24px",
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  gap: "4px",
  color: "#ff747c",
  fontFamily: vars.font.body,
  fontSize: "14px",
});
// 공통 기능 버튼에서 좋아요 아이콘에 필요한 최소 너비를 적용함
export const likeIconButton = style([actionButton, { minWidth: "16px", width: "16px" }]);
// 좋아요 사용자 수를 좋아요 아이콘과 같은 의미 색상으로 표시함
export const likeCountButton = style({ color: "#ff747c" });
// 공통 기능 버튼에 댓글 기능의 중립 색상을 적용함
export const commentButton = style([actionButton, { color: "#777777" }]);
// 좋아요와 댓글 기능에 사용하는 공통 아이콘 크기를 정의함
export const icon = style({ width: "16px", height: "16px", flexShrink: 0 });
// 팔로잉 공개 활동이 없는 피드 빈 상태 문구를 정의함
export const empty = style({ margin: "72px 20px", fontFamily: vars.font.body, fontSize: "14px", textAlign: "center", lineHeight: 1.7, color: vars.color.gray600, whiteSpace: "pre-line" });
// 피드 최초 조회 실패 문구를 부정 상태 색상으로 표시함
export const error = style({ margin: "56px 20px", fontFamily: vars.font.body, fontSize: "14px", textAlign: "center", color: vars.color.negativeText });
// 피드 최초 조회를 다시 실행하는 버튼 상태를 정의함
export const retry = style({
  marginTop: "14px",
  padding: "9px 16px",
  border: 0,
  borderRadius: "10px",
  background: vars.color.gray900,
  color: vars.color.background,
  fontFamily: vars.font.semibold,
  fontSize: "14px",
  cursor: "pointer",
  selectors: {
    "&:hover": { background: vars.color.darkGray },
    "&:focus-visible": { outline: `2px solid ${vars.color.brand}`, outlineOffset: 2 },
  },
});
