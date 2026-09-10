import {globalStyle, style} from "@vanilla-extract/css";
import {vars} from "@/app/styles/tokens.css";
import * as reportListStyles from "@/components/ReportList/ReportListView.css";

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
  // width: "calc(100% + 32px)",
  // marginLeft: "-16px",
  margin: "0 auto 18px",
  padding: 0,
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
export const list = style({
  display: "grid",
  gap: "24px"
});

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

// 독후감 카드의 작성자와 도서 정보 사이 피그마 간격
export const reportHeader = style({paddingBottom: "16px"});

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
});

export const authorIdentity = style({
  display: "inline-flex",
  alignItems: "center",
  gap: "8px",
  minWidth: 0,
  maxWidth: "100%",
});

export const avatar = style({
  width: "30px",
  height: "30px",
  flexShrink: 0,
  borderRadius: "50%",
  objectFit: "cover",
  backgroundColor: vars.color.gray300,
});

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

export const activityDate = style({
  flexShrink: 0,
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: "14px",
  lineHeight: 1.4,
  whiteSpace: "nowrap",
});

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
});

export const reportMediaRow = style({
  display: "grid",
  gridTemplateColumns: "84px minmax(0, 1fr)",
  alignItems: "center",
  gap: "10px",
  width: "100%",
  padding: "8px 16px 26px",
  boxSizing: "border-box",
});

export const reportCoverLink = style({
  display: "block",
  borderRadius: "6px",
});

export const backgroundMediaButton = style([
  mediaButton,
  {
    gridTemplateColumns: "minmax(0, 1fr)",
    gap: "6px",
  },
]);

export const backgroundMediaWrap = style({
  position: "relative",
  display: "block",
  width: "100%",
  aspectRatio: "16 / 9",
  overflow: "hidden",
  borderRadius: "12px",
  background: vars.color.gray100,
});

export const media = style({
  display: "block",
  background: vars.color.gray100,
  objectFit: "cover",
});

export const reportMedia = style([
  media,
  {
    width: "84px",
    height: "124px",
    borderRadius: "6px",
    border: `1px solid ${vars.color.gray300}`,
    boxSizing: "border-box"
  }
]);

export const backgroundMedia = style([
  media,
  {
    width: "100%",
    height: "100%",
    borderRadius: "12px"
  }
]);

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

export const mediaInfo = style({
  minWidth: 0,
  alignSelf: "center",
  display: "flex",
  flexDirection: "column",
  gap: "9px"
});

export const bookIdentity = style({
  display: "flex",
  flexDirection: "column",
  gap: "4px",
  minWidth: 0,
});

export const bookInfoLink = style({
  display: "block",
  minWidth: 0,
  color: "inherit",
  textDecoration: "none",
  borderRadius: "4px",
});

export const title = style({
  display: "block",
  margin: 0,
  fontFamily: vars.font.semibold,
  fontSize: "16 px",
  lineHeight: 1.25,
  color: vars.color.black,
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap"
});

export const authorSearchLink = style({
  display: "block",
  minWidth: 0,
  width: "fit-content",
  maxWidth: "100%",
  borderRadius: "4px",
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: "14px",
  lineHeight: 1.25,
  overflow: "hidden",
  textDecoration: "none",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
});

export const ratingStatusRow = style({
  minWidth: 0,
  display: "flex",
  alignItems: "center",
  flexWrap: "wrap",
  gap: "6px"
});

export const rating = style({
  display: "inline-flex",
  alignItems: "center",
  gap: "4px",
  color: vars.color.black,
  fontFamily: vars.font.semibold,
  fontSize: "14px",
  lineHeight: 1
});

export const ratingIcon = style({width: "14px", height: "14px", display: "block", flexShrink: 0});

export const contentSection = style({
  padding: "0 16px 10px",
});

globalStyle(`.${contentSection} .${reportListStyles.reportContent}`, {
  lineHeight: 1.8,
  color: vars.color.black
});

export const translationButton = style([reportListStyles.translationButton, {
  color: vars.color.gray600,
  fontSize: "12px",
  letterSpacing: "-0.12px",
}]);

export const reportContentLink = style({
  display: "block",
  borderRadius: "4px",
  color: "inherit",
  textDecoration: "none",
});

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

export const reactionActions = style({
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "flex-end",
  gap: "8px",
  marginLeft: "auto",
});

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
});

export const likeActionGroup = style({
  minWidth: "32px",
  height: "24px",
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  gap: "4px",
  color: "#FF8386",
  fontFamily: vars.font.body,
  fontSize: "14px",
});

export const likeIconButton = style([
  actionButton,
  {
    minWidth: "16px",
    width: "16px"
  }
]);

export const likeCountButton = style({
  color: "#d84a5f",
  fontFamily: vars.font.body
});

export const commentButton = style([
  actionButton,
  {
    color: "#777777"
  }]);

export const icon = style({
  width: "16px",
  height: "16px",
  flexShrink: 0
});

export const empty = style({
  margin: "72px 20px",
  fontFamily: vars.font.body,
  fontSize: "14px",
  textAlign: "center",
  lineHeight: 1.7,
  color: vars.color.gray600,
  whiteSpace: "pre-line"
});

export const error = style({
  margin: "56px 20px",
  fontFamily: vars.font.body,
  fontSize: "14px",
  textAlign: "center",
  color: vars.color.negativeText
});

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
});
