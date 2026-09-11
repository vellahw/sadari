import { keyframes, style } from "@vanilla-extract/css";
import { vars } from "@/app/styles/tokens.css";
import * as modalControlStyles from "@/components/Modal/ModalControls.css";

// 독서 활동 조회 완료 시 콘텐츠를 자연스럽게 표시하는 전환 효과
const activityFadeIn = keyframes({
  "0%": {
    opacity: 0,
    transform: "translateY(8px)",
  },
  "100%": {
    opacity: 1,
    transform: "translateY(0)",
  },
});

// 이미지 업로드를 포함한 프로필 저장이 계속 진행 중임을 전달하는 회전 동작
const rotateProfileSaveSpinner = keyframes({
  to: {
    transform: "rotate(360deg)",
  },
});

export const page = style({
  width: "100%",
  minHeight: "calc(100svh - 52px - 60px)",
  padding: 0,
  backgroundColor: "#ffffff",
});

export const profileShell = style({
  width: "100%",
  maxWidth: "600px",
  minHeight: "calc(100vh - 110px)",
  margin: "12px 0",
  backgroundColor: "#ffffff",
  display: "flex",
  flexDirection: "column",
  alignItems: "stretch",
});

export const cover = style({
  position: "relative",
  width: "100%",
  margin: 0,
  height: "260px",
  overflow: "hidden",
  borderRadius: "22px",
  backgroundColor: "#d9e0e7",
  backgroundSize: "cover",
  backgroundPosition: "center",
});

export const coverImage = style({
  position: "absolute",
  inset: 0,
  display: "block",
  width: "100%",
  height: "100%",
  objectFit: "cover",
});

export const coverImageViewerButton = style({
  position: "absolute",
  inset: 0,
  zIndex: 1,
  display: "block",
  width: "100%",
  height: "100%",
  borderRadius: "inherit",
  selectors: {
    "&:hover": {
      backgroundColor: "rgba(21, 21, 21, 0.08)",
    },
  },
});

export const coverActionGroup = style({
  position: "absolute",
  right: "14px",
  bottom: "14px",
  zIndex: 2,
  display: "inline-flex",
  alignItems: "center",
  gap: "8px",
});

export const coverEditAction = style({
  position: "absolute",
  right: "14px",
  bottom: "14px",
  zIndex: 2,
});

export const coverProfileEditButton = style({
  minHeight: "30px",
  padding: "0 10px",
  border: "1px solid rgba(255, 255, 255, 0.72)",
  borderRadius: "999px",
  backgroundColor: "rgba(0, 0, 0, 0.48)",
  color: "#ffffff",
  fontFamily: vars.font.semibold,
  fontSize: "12px",
  display: "inline-flex",
  alignItems: "center",
  gap: "5px",
  cursor: "pointer",
  selectors: {
    "&:disabled": {
      cursor: "default",
      opacity: 0.62,
    },
  },
});

export const coverImageButton = style([
  coverProfileEditButton,
  {
    backgroundColor: "rgba(255, 255, 255, 0.92)",
    color: vars.color.black,
    borderColor: "rgba(255, 255, 255, 0.92)",
  },
]);

export const coverSaveButton = style([
  coverProfileEditButton,
  {
    backgroundColor: "rgba(255, 255, 255, 0.96)",
    borderColor: vars.color.gray700,
    borderWidth: "1px",
    color: vars.color.gray900,
    selectors: {
      "&:disabled": {
        cursor: "default",
        opacity: 0.62,
      },
    },
  },
]);

// 프로필 저장 요청이 끝날 때까지 버튼을 푸시 설정과 같은 골드색 진행 상태로 유지함
export const coverSaveButtonSaving = style([
  coverSaveButton,
  {
    minWidth: "126px",
    borderColor: "#d9b44a",
    backgroundColor: "rgba(255, 255, 255, 0.96)",
    color: "#d9b44a",
    selectors: {
      "&:disabled": {
        borderColor: "#d9b44a",
        backgroundColor: "rgba(255, 255, 255, 0.96)",
        color: "#d9b44a",
        cursor: "wait",
        opacity: 1,
      },
    },
  },
]);

// 저장 중 버튼의 현재 글자색을 상속하여 골드색 회전 표시를 제공함
export const profileSaveSpinner = style({
  width: "12px",
  height: "12px",
  flex: "0 0 auto",
  border: "2px solid currentColor",
  borderRightColor: "transparent",
  borderRadius: "50%",
  boxSizing: "border-box",
  animation: `${rotateProfileSaveSpinner} 700ms linear infinite`,
});

export const actionIcon = style({
  width: "14px",
  height: "14px",
  fill: "currentColor",
  flexShrink: 0,
});

export const coverEmptyText = style({
  position: "absolute",
  left: "50%",
  top: "50%",
  zIndex: 1,
  margin: 0,
  transform: "translate(-50%, -50%)",
  fontFamily: vars.font.body,
  fontSize: "12px",
  lineHeight: 1.4,
  color: "#7a8490",
  textAlign: "center",
  whiteSpace: "nowrap",
});

export const profileBody = style({
  position: "relative",
  display: "flex",
  flexDirection: "column",
  alignItems: "stretch",
  padding: "0 14px",
});

export const activityContent = style({
  width: "100%",
  display: "flex",
  flexDirection: "column",
  alignItems: "stretch",
  animation: `${activityFadeIn} 320ms ease-out both`,
});

export const socialProfileBody = style([
  profileBody,
  {
    padding: 0,
  },
]);

export const profileHeaderRow = style({
  width: "100%",
  marginTop: "-34px",
  display: "grid",
  gridTemplateColumns: "112px minmax(0, 1fr)",
  alignItems: "start",
  gap: "18px",
});

export const socialProfileHeaderRow = style([
  profileHeaderRow,
  {
    padding: "0 14px",
    boxSizing: "border-box",
  },
]);

export const avatarWrap = style({
  position: "relative",
  width: "100px",
  height: "100px",
  margin: 0,
});

export const profileImage = style({
  width: "100px",
  height: "100px",
  borderRadius: "50%",
  objectFit: "cover",
  border: "4px solid #ffffff",
  backgroundColor: "#ffffff",
  boxShadow: "0 10px 24px rgba(0, 0, 0, 0.16)",
});

export const metricButton = style({
  minWidth: "28px",
  height: "24px",
  padding: 0,
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  gap: "2px",
  backgroundColor: "transparent",
  color: "#ff747c",
  fontFamily: vars.font.body,
  fontSize: "12px",
  cursor: "pointer",
  selectors: {
    "&:disabled": {
      cursor: "default",
      opacity: 0.5,
    },
  },
});

export const likeMetricGroup = style({
  minWidth: "28px",
  height: "24px",
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  gap: "2px",
  color: "#ff747c",
  fontFamily: vars.font.body,
  fontSize: "12px",
});

export const likeIconButton = style({
  width: "14px",
  height: "24px",
  padding: 0,
  border: 0,
  backgroundColor: "transparent",
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  cursor: "pointer",
  selectors: {
    "&:disabled": { cursor: "default", opacity: 0.5 },
    "&:focus-visible": { outline: `2px solid ${vars.color.brand}`, outlineOffset: 1 },
  },
});

export const likeCountButton = style({
  color: "#ff747c",
});

export const metricIcon = style({
  width: "14px",
  height: "14px",
  flexShrink: 0,
});

export const commentButton = style([
  metricButton,
  {
    color: "#777777",
  },
]);


// 소셜 프로필의 세로 점 더보기 버튼을 마이페이지 프로필 수정 버튼과 같은 위치와 명암으로 표시함
export const socialProfileMoreButton = style([
  coverProfileEditButton,
  {
    width: "34px",
    minWidth: "34px",
    height: "34px",
    minHeight: "34px",
    padding: 0,
    justifyContent: "center",
  },
]);

// 배경과 프로필 사진 위에서도 독후감과 같은 좋아요 및 댓글 버튼을 선명하게 표시하는 공통 캡슐 영역임
const imageReactionBar = style({
  zIndex: 2,
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  gap: "4px",
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: "999px",
  backgroundColor: "rgba(255, 255, 255, 0.94)",
});

export const viewerImageReactionBar = style([
  imageReactionBar,
  {
    minHeight: "30px",
    padding: "2px 8px",
  },
]);

// 공통 가로 점 아이콘을 세로 점으로 회전하고 어두운 배경에서 흰색으로 표시함
export const socialProfileMoreIcon = style({
  width: "20px",
  height: "20px",
  transform: "rotate(90deg)",
  filter: "brightness(0) invert(1)",
});

// 배경사진 overflow 영역 안에서도 신고 메뉴가 잘리지 않도록 버튼 위쪽으로 펼침
export const socialProfileMoreMenu = style({
  top: "auto",
  right: 0,
  bottom: "calc(100% + 6px)",
  minWidth: "154px",
});

export const profileImageViewerButton = style({
  display: "block",
  width: "100px",
  height: "100px",
  borderRadius: "50%",
});

export const avatarCameraButton = style({
  position: "absolute",
  right: "-8px",
  bottom: "8px",
  width: "38px",
  height: "38px",
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: "50%",
  backgroundColor: "#ffffff",
  color: vars.color.black,
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  cursor: "pointer",
  boxShadow: "0 6px 16px rgba(0, 0, 0, 0.16)",
});

export const cameraIcon = style({
  width: "19px",
  height: "19px",
  fill: "currentColor",
});

export const hiddenInput = style({
  position: "absolute",
  width: "1px",
  height: "1px",
  padding: 0,
  margin: "-1px",
  overflow: "hidden",
  clip: "rect(0, 0, 0, 0)",
  whiteSpace: "nowrap",
  border: 0,
});

export const profileText = style({
  width: "100%",
  minWidth: 0,
  paddingTop: "42px",
  display: "flex",
  flexDirection: "column",
  alignItems: "flex-start",
  gap: "8px",
  textAlign: "left",
});

export const profileName = style({
  margin: 0,
  fontFamily: vars.font.heading,
  fontSize: "22px",
  lineHeight: 1.3,
  color: vars.color.black,
  wordBreak: "break-word",
});

export const profileIntro = style({
  margin: 0,
  maxWidth: "100%",
  fontFamily: vars.font.body,
  fontSize: "14px",
  lineHeight: 1.55,
  color: vars.color.gray600,
  wordBreak: "break-word",
});

export const socialFollowButton = style({
  position: "absolute",
  right: "-9px",
  bottom: "5px",
  height: "26px",
  minWidth: "54px",
  padding: "0 8px",
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: "999px",
  backgroundColor: "#ffffff",
  color: vars.color.black,
  fontFamily: vars.font.semibold,
  fontSize: "12px",
  whiteSpace: "nowrap",
  boxShadow: "0 6px 16px rgba(0, 0, 0, 0.16)",
  cursor: "pointer",
  selectors: {
    "&:hover:not(:disabled)": {
      backgroundColor: vars.color.gray100,
    },
    "&[data-follow-status='팔로잉']": {
      color: vars.color.brandText,
    },
    "&[data-follow-status='맞팔로우']": {
      color: vars.color.brandText,
    },
    "&[data-follow-status='친구']": {
      color: "#2563eb",
      backgroundColor: "#eaf4ff",
    },
    "&[data-follow-status='친구']:hover:not(:disabled)": {
      backgroundColor: "#dbeafe",
    },
    "&:disabled": {
      cursor: "default",
      opacity: 0.62,
    },
  },
});

export const profileNameInput = style({
  width: "100%",
  minWidth: 0,
  height: "40px",
  margin: 0,
  padding: "0 12px",
  border: `1px solid ${vars.color.gray300}`,
  borderRadius:'8px',
  backgroundColor: "#f8f9fa",
  color: vars.color.black,
  fontFamily: vars.font.heading,
  fontSize: "16px",
  lineHeight: 1.3,
  outline: "none",
  boxShadow: "inset 0 1px 0 rgba(255, 255, 255, 0.8)",
  selectors: {
    "&:focus": {
      borderColor: vars.color.gray600,
      backgroundColor: "#ffffff",
    },
  },
});

export const profileIntroInput = style({
  width: "100%",
  minWidth: 0,
  height: "80px",
  padding: "9px 12px",
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: '8px',
  backgroundColor: "#f8f9fa",
  color: "#666666",
  fontFamily: vars.font.body,
  fontSize: "14px",
  lineHeight: 1.55,
  outline: "none",
  resize: "none",
  boxShadow: "inset 0 1px 0 rgba(255, 255, 255, 0.8)",
  selectors: {
    "&:focus": {
      borderColor: vars.color.gray600,
      backgroundColor: "#ffffff",
    },
  },
});

export const monthlySummary = style({
  position: "relative",
  width: "100%",
  marginTop: "32px",
  padding: "20px",
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: "22px",
  backgroundColor: "#ffffff",
  boxShadow: "0 8px 22px rgba(0, 0, 0, 0.05)",
});

export const readingSummaryRow = style({
  minHeight: "66px",
  display: "grid",
  gridTemplateColumns: "minmax(0, 1fr) auto",
  alignItems: "center",
  gap: "12px",
});

export const goalAchievementSummary = style({
  padding: "0 8px 24px",
});

export const profileStatsSummary = style({
  padding: "9px 8px 14px",
});

export const myProfileStatsSummary = style([
  profileStatsSummary,
  {
    padding: "4px 8px 8px",
  },
]);

export const profileStatsTitle = style({
  margin: "0 0 16px",
  fontFamily: vars.font.semibold,
  fontSize: "14px",
  lineHeight: 1.3,
  textAlign: "center",
  color: vars.color.gray600,
});

export const profileStatsButton = style({
  width: "100%",
  minWidth: 0,
  padding: 0,
  border: 0,
  backgroundColor: "transparent",
  color: "inherit",
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  gap: "10px",
  cursor: "pointer",
  selectors: {
    "&:disabled": {
      cursor: "default",
    },
  },
});

export const currentReadingSection = style({
  display: "flex",
  flexDirection: "column",
  gap: "18px",
});

export const currentReadingTitle = style({
  margin: 0,
  padding: 0,
  fontFamily: vars.font.semibold,
  fontSize: "14px",
  lineHeight: 1.3,
  textAlign: "left",
  color: vars.color.black,
})
;

export const currentReadingList = style({
  display: "flex",
  flexDirection: "column",
  gap: 0,
});

export const currentReadingCard = style({
  width: "100%",
  minHeight: "102px",
  padding: "12px 8px",
  border: 0,
  borderBottom: `1px solid ${vars.color.gray200}`,
  borderRadius: 0,
  backgroundColor: "#ffffff",
  display: "flex",
  alignItems: "center",
  gap: "12px",
  textAlign: "left",
  selectors: {
    "&:last-child": {
      borderBottom: 0,
    },
  },
});

export const currentReadingButton = style([
  currentReadingCard,
  {
    cursor: "pointer",
    transition: "background-color 160ms ease",
    selectors: {
      "&:hover": {
        backgroundColor: "#f8f9fa",
      },
    },
  },
]);

export const currentReadingText = style({
  minWidth: 0,
  display: "flex",
  flexDirection: "column",
  gap: "4px",
});

export const myPageCurrentReadingText = style({
  flex: 1,
});

export const currentReadingMeta = style({
  display: "flex",
  alignItems: "flex-end",
  justifyContent: "space-between",
  gap: "8px",
});

export const currentReadingBookMetaGroup = style({
  minWidth: 0,
  display: "flex",
  flexDirection: "column",
  alignItems: "flex-start",
  gap: "4px",
});

export const currentReadingRemain = style({
  flexShrink: 0,
  fontFamily: vars.font.semibold,
  fontSize: "12px",
  lineHeight: 1.25,
  whiteSpace: "nowrap",
});

export const goalAchievementTitle = style({
  margin: "0 0 18px",
  padding: 0,
  fontFamily: vars.font.semibold,
  fontSize: "15px",
  lineHeight: 1.3,
  textAlign: "left",
  color: vars.color.black,
});

export const myPageSectionTitle = style({
  width: "100%",
  textAlign: "center",
});

export const socialSectionTitle = style({
  paddingLeft: "6px",
});

export const goalAchievementGrid = style({
  display: "grid",
  gridTemplateColumns: "repeat(4, minmax(0, 1fr))",
  gap: "8px",
});

export const goalAchievementItem = style({
  minWidth: 0,
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  gap: "10px",
});

export const goalAchievementLabel = style({
  fontFamily: vars.font.body,
  fontSize: "12px",
  lineHeight: 1.2,
  color: vars.color.gray600
});

export const goalAchievementCount = style({
  fontFamily: vars.font.heading,
  fontSize: "18px",
  lineHeight: 1.2,
  color: vars.color.black,
});

export const readingSummaryToggle = style({
  minWidth: 0,
  width: "100%",
  minHeight: "66px",
  padding: 0,
  border: 0,
  backgroundColor: "transparent",
  display: "grid",
  gridTemplateColumns: "44px minmax(0, 1fr) 28px",
  alignItems: "center",
  gap: "12px",
  textAlign: "left",
  cursor: "pointer",
});

export const readingSummaryToggleStatic = style([
  readingSummaryToggle,
  {
    cursor: "default",
    selectors: {
      "&:disabled": {
        opacity: 1,
      },
    },
  },
]);

export const readingSummaryChevron = style({
  width: "28px",
  height: "28px",
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: "50%",
  backgroundColor: vars.color.gray100,
  color: vars.color.gray600,
  lineHeight: 1,
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  transform: "rotate(0deg)",
  transition: "transform 200ms ease, color 180ms ease, background-color 180ms ease, border-color 180ms ease",
});

export const readingSummaryChevronOpen = style([
  readingSummaryChevron,
  {
    transform: "rotate(180deg)",
  },
]);

export const readingSummaryChevronIcon = style({
  width: "17px",
  height: "17px",
  fill: "currentColor",
});

export const readingSummaryDivider = style({
  width: "100%",
  height: "1px",
  backgroundColor: "#eef0f2",
});

export const readingSummaryPanel = style({
  display: "grid",
  gridTemplateRows: "0fr",
  opacity: 0,
  overflow: "hidden",
  transition: "grid-template-rows 220ms ease, opacity 180ms ease",
});

export const readingSummaryPanelOpen = style([
  readingSummaryPanel,
  {
    gridTemplateRows: "1fr",
    opacity: 1,
  },
]);

export const readingSummaryPanelInner = style({
  minHeight: 0,
  display: "flex",
  flexDirection: "column",
  gap: 0,
  padding: "0 0 12px",
});

export const readingSummaryReport = style({
  width: "100%",
  minHeight: "98px",
  padding: "10px 8px",
  border: 0,
  borderBottom: `1px solid ${vars.color.gray200}`,
  borderRadius: 0,
  backgroundColor: "#ffffff",
  display: "grid",
  gridTemplateColumns: "58px minmax(0, 1fr)",
  alignItems: "center",
  gap: "12px",
  textAlign: "left",
  cursor: "pointer",
  transition: "background-color 160ms ease",
  selectors: {
    "&:hover": {
      backgroundColor: vars.color.gray100,
    },
    "&:last-child": {
      borderBottom: 0,
    },
  },
});

export const readingSummaryReptStatic = style([
  readingSummaryReport,
  {
    cursor: "default",
    selectors: {
      "&:hover": {
        backgroundColor: "#ffffff",
        transform: "none",
      },
    },
  },
]);

export const readingSummaryReportPrivate = style([
  readingSummaryReport,
  {
    backgroundColor: "#f0f1f2",
    borderBottomColor: "#d9dcdf",
    color: "#9a9a9a",
    filter: "grayscale(0.65)",
    opacity: 0.72,
    selectors: {
      "&:hover": {
        backgroundColor: "#eceeef",
        borderBottomColor: "#d9dcdf",
        transform: "none",
      },
    },
  },
]);

export const readingSummaryCover = style({
  width: "50px",
  height: "74px",
  borderRadius: "4px",
  objectFit: "cover",
  backgroundColor: "#f0f1f2",
});

export const readingSummaryBookText = style({
  minWidth: 0,
  display: "flex",
  flexDirection: "column",
  gap: "3px",
});

export const readingSummaryBookTitle = style({
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
  fontFamily: vars.font.semibold,
  fontSize: "14px",
  lineHeight: 1.25,
  color: vars.color.black,
});

export const readingSummaryBookTitleButton = style([
  readingSummaryBookTitle,
  {
    width: "fit-content",
    maxWidth: "100%",
    padding: 0,
    border: 0,
    backgroundColor: "transparent",
    textAlign: "left",
    cursor: "pointer",
    selectors: {
      "&:hover": {
        textUnderlineOffset: "3px",
      },
      "&:focus-visible": {
        outline: "2px solid #8ab4e8",
        outlineOffset: "2px",
        textUnderlineOffset: "3px",
      },
    },
  },
]);

export const readingSummaryBookMeta = style({
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
  fontFamily: vars.font.body,
  fontSize: "12px",
  lineHeight: 1.25,
  color: "#777777",
});

export const readingSummaryMetaLine = style({
  minWidth: 0,
  display: "flex",
  alignItems: "center",
  gap: "5px",
  overflow: "hidden",
});

export const readingSummaryMetaText = style({
  minWidth: 0,
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
});

export const readingSummaryGrade = style({
  flexShrink: 0,
  color: "#ffd966",
  letterSpacing: 0,
});

export const readingSummaryEmpty = style({
  margin: "0 0 10px",
  fontFamily: vars.font.body,
  fontSize: "12px",
  lineHeight: 1.5,
  color: "#777777",
});

export const monthlyCalendarIcon = style({
  position: "relative",
  width: "44px",
  height: "44px",
  border: `2px solid ${vars.color.black}`,
  borderRadius: "8px",
  backgroundColor: "#ffffff",
  color: vars.color.black,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  flexShrink: 0,
});

export const monthlyCalendarRing = style({
  position: "absolute",
  top: "6px",
  left: "7px",
  right: "7px",
  height: "2px",
  backgroundColor: vars.color.gray900,
  selectors: {
    "&::before": {
      content: "",
      position: "absolute",
      left: "3px",
      top: "-6px",
      width: "3px",
      height: "7px",
      borderRadius: "999px",
      backgroundColor: vars.color.gray900,
    },
    "&::after": {
      content: "",
      position: "absolute",
      right: "3px",
      top: "-6px",
      width: "3px",
      height: "7px",
      borderRadius: "999px",
      backgroundColor: vars.color.gray900,
    },
  },
});

export const monthlyCalendarMonth = style({
  marginTop: "7px",
  fontFamily: vars.font.heading,
  fontSize: "11px",
  lineHeight: 1,
  color: vars.color.gray900,
});

export const monthlySummaryText = style({
  minWidth: 0,
  display: "flex",
  flexDirection: "column",
  gap: "4px",
});

export const monthlySummaryLabel = style({
  fontFamily: vars.font.body,
  fontSize: "12px",
  lineHeight: 1.3,
  color: "#777777",
});

export const monthlySummaryCount = style({
  fontFamily: vars.font.heading,
  fontSize: "20px",
  lineHeight: 1.2,
  color: vars.color.black,
});

const monthlyDiffBase = style({
  minWidth: "38px",
  height: "30px",
  padding: "0 9px",
  border: 0,
  borderRadius: "999px",
  fontFamily: vars.font.heading,
  fontSize: "14px",
  lineHeight: 1,
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  cursor: "pointer",
});

export const monthlyDiffUp = style([
  monthlyDiffBase,
  {
    backgroundColor: vars.color.brandBg,
    color: vars.color.brandText,
  },
]);

export const monthlyDiffDown = style([
  monthlyDiffBase,
  {
    backgroundColor: vars.color.negativeBg,
    color: vars.color.negativeText,
  },
]);

export const monthlyDiffNeutral = style({
  minWidth: "38px",
  height: "30px",
  padding: "0 9px",
  border: 0,
  borderRadius: "999px",
  backgroundColor: vars.color.gray100,
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: "12px",
  lineHeight: 1,
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  cursor: "pointer",
});

export const monthlyDiffTooltipWrap = style({
  position: "relative",
  display: "inline-flex",
  justifyContent: "flex-end",
});

export const monthlyDiffTooltip = style({
  position: "absolute",
  right: 0,
  top: "calc(100% + 9px)",
  zIndex: 5,
  width: "max-content",
  maxWidth: "190px",
  padding: "9px 11px",
  borderRadius: "10px",
  backgroundColor: "#ffffff",
  color: vars.color.black,
  fontFamily: vars.font.body,
  fontSize: "12px",
  lineHeight: 1.45,
  textAlign: "left",
  boxShadow: "0 10px 28px rgba(0, 0, 0, 0.14)",
  border: `1px solid ${vars.color.gray300}`,
  selectors: {
    "&::before": {
      content: "",
      position: "absolute",
      right: "16px",
      top: "-6px",
      width: "10px",
      height: "10px",
      backgroundColor: "#ffffff",
      borderLeft: `1px solid ${vars.color.gray300}`,
      borderTop: `1px solid ${vars.color.gray300}`,
      transform: "rotate(45deg)",
    },
  },
});

export const goalProgressRow = style({
  position: "relative",
  display: "grid",
  gridTemplateColumns: "minmax(0, 1fr) 38px",
  alignItems: "center",
  gap: "8px",
  padding: "0 0 16px 60px",
});

export const goalProgressTarget = style({
  position: "absolute",
  left: "-4px",
  top: "0",
  width: "52px",
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
  fontFamily: vars.font.body,
  fontSize: "12px",
  lineHeight: 1,
  color: vars.color.gray600,
  textAlign: "center",
  transform: "translateY(1px)",
});

export const goalProgressTrack = style({
  height: "12px",
  borderRadius: "999px",
  backgroundColor: vars.color.gray200,
  overflow: "hidden",
});

export const goalProgressFill = style({
  display: "block",
  height: "100%",
  minWidth: "0%",
  maxWidth: "100%",
  borderRadius: "999px",
  transition: "width 220ms ease, background-color 180ms ease",
});

export const goalProgressRate = style({
  fontFamily: vars.font.heading,
  fontSize: "14px",
  lineHeight: 1,
  textAlign: "center",
  whiteSpace: "nowrap",
  color: "#999999",
  transition: "color 180ms ease",
});

export const goalSettingButton = style({
  alignSelf: "flex-end",
  marginTop: "10px",
  border: 0,
  backgroundColor: "transparent",
  color: "#8a8a8a",
  fontFamily: vars.font.semibold,
  fontSize: "14px",
  cursor: "pointer",
  display: "inline-flex",
  alignItems: "center",
  gap: "2px",
  transition: "color 160ms ease, opacity 160ms ease",
  selectors: {
    "&:hover": {
      color: "#555555",
    },
  },
});

export const goalModalOverlay = style({
  position: "fixed",
  inset: 0,
  width: "100vw",
  height: "100dvh",
  zIndex: 1200,
  padding: "0 16px",
  backgroundColor: "rgba(0, 0, 0, 0.34)",
  display: "flex",
  // Center app modal dialogs vertically; popovers such as calendars/select boxes keep their own positioning
  alignItems: "center",
  justifyContent: "center",
  overflow: "hidden",
  overscrollBehavior: "contain",
  animation: `${keyframes({
    from: { opacity: 0 },
    to: { opacity: 1 },
  })} 160ms ease-out`,
});

export const goalModalOverlayClosing = style({
  animation: `${keyframes({
    from: { opacity: 1 },
    to: { opacity: 0 },
  })} 180ms ease-in forwards`,
});

export const goalModal = style({
  width: "min(600px, 100%)",
  maxHeight: "calc(100dvh - 48px)",
  overflowY: "auto",
  borderRadius: "22px",
  backgroundColor: "#ffffff",
  padding: "20px",
  boxShadow: "0 22px 58px rgba(0, 0, 0, 0.24)",
  animation: `${keyframes({
    from: { opacity: 0, transform: "translateY(8px)" },
    to: { opacity: 1, transform: "translateY(0)" },
  })} 180ms ease-out`,
});

export const followModal = style([
  goalModal,
  {
    width: "min(460px, 100%)",
    padding: "20px 18px 18px",
    overflow: "hidden",
  },
]);

export const followModalList = style({
  marginTop: "18px",
  maxHeight: "min(420px, calc(100dvh - 150px))",
  paddingRight: "8px",
  overflowY: "auto",
  overflowX: "hidden",
  borderRadius: "12px",
  scrollbarGutter: "stable",
  scrollbarWidth: "thin",
  scrollbarColor: "transparent transparent",
  display: "flex",
  flexDirection: "column",
  gap: "9px",
  selectors: {
    "&::-webkit-scrollbar": {
      width: "6px",
    },
    "&::-webkit-scrollbar-track": {
      backgroundColor: "transparent",
    },
    "&::-webkit-scrollbar-thumb": {
      borderRadius: "999px",
      backgroundColor: "transparent",
      transition: "background-color 220ms ease",
    },
  },
});

export const followModalListScrolling = style([
  followModalList,
  {
    scrollbarColor: "rgba(0, 0, 0, 0.24) transparent",
    selectors: {
      "&::-webkit-scrollbar-thumb": {
        backgroundColor: "rgba(0, 0, 0, 0.24)",
      },
    },
  },
]);

export const followModalItem = style({
  width: "100%",
  minHeight: "58px",
  padding: "8px 0",
  border: 0,
  borderBottom: "1px solid #eef0f2",
  backgroundColor: "transparent",
  display: "grid",
  gridTemplateColumns: "minmax(0, 1fr) auto",
  alignItems: "center",
  gap: "10px",
  textAlign: "left",
  selectors: {
    "&:last-child": {
      borderBottom: 0,
    },
  },
});

export const followModalProfileButton = style({
  minWidth: 0,
  padding: 0,
  border: 0,
  backgroundColor: "transparent",
  display: "grid",
  gridTemplateColumns: "42px minmax(0, 1fr)",
  alignItems: "center",
  gap: "10px",
  textAlign: "left",
  cursor: "pointer",
});

export const followModalAvatar = style({
  width: "42px",
  height: "42px",
  borderRadius: "50%",
  objectFit: "cover",
  backgroundColor: "#f0f1f2",
});

export const followModalText = style({
  minWidth: 0,
  display: "flex",
  flexDirection: "column",
  gap: "3px",
});

export const followModalName = style({
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
  fontFamily: vars.font.semibold,
  fontSize: "14px",
  lineHeight: 1.25,
  color: vars.color.black,
});

export const followModalIntro = style({
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
  fontFamily: vars.font.body,
  fontSize: "12px",
  lineHeight: 1.35,
  color: "#777777",
});

export const followModalEmpty = style({
  margin: "12px 0 4px",
  padding: "24px 0",
  fontFamily: vars.font.body,
  fontSize: "14px",
  lineHeight: 1.5,
  color:vars.color.gray600,
  textAlign: "center",
});

export const goalModalClosing = style({
  animation: `${keyframes({
    from: { opacity: 1, transform: "translateY(0)" },
    to: { opacity: 0, transform: "translateY(8px)" },
  })} 180ms ease-in forwards`,
});

export const goalHelpModal = style([
  goalModal,
  {
    height: "348px"
  }
]);

export const goalModalHeader = style({
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  gap: "12px",
});

export const goalModalHeaderActions = style({
  display: "inline-flex",
  alignItems: "center",
  gap: "10px",
});

export const goalHelpButton = style({
  minHeight: "28px",
  padding: "6px 8px",
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: "999px",
  backgroundColor: "#ffffff",
  color: vars.color.gray600,
  fontFamily: vars.font.medium,
  fontSize: "12px",
  lineHeight: 1,
  cursor: "pointer",
  transition: "border-color 160ms ease, color 160ms ease, background-color 160ms ease",
  selectors: {
    "&:hover": {
      borderColor: "#cfd4da",
      backgroundColor: "#f8f9fa",
      color: vars.color.black,
    },
  },
});

export const goalModalTitle = style({
  margin: 0,
  fontFamily: vars.font.heading,
  fontSize: "18px",
  lineHeight: 1.35,
  color: vars.color.black,
});

export const goalHelpBody = style({
  marginTop: "18px",
});

export const currentReadingModalBody = style({
  marginTop: "18px",
  display: "flex",
  flexDirection: "column",
});

export const currentReadingModalBookInfo = style({
  minHeight: "196px",
  padding: "16px",
  borderRadius: "12px",
  backgroundColor: "#f7f8f8",
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  justifyContent: "flex-start",
  gap: "12px",
  textAlign: "center",
});

export const currentReadingModalCover = style({
  width: "72px",
  height: "98px",
  borderRadius: "8px",
  objectFit: "cover",
  backgroundColor: "#eceeef",
  boxShadow: "0 8px 18px rgba(0, 0, 0, 0.12)",
});

export const currentReadingModalCoverPlaceholder = style({
  width: "72px",
  height: "98px",
  borderRadius: "8px",
  backgroundColor: "#eceeef",
  display: "block",
});

export const currentReadingModalBookText = style({
  width: "100%",
  minWidth: 0,
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  gap: "8px",
});

export const currentReadingModalBookTitle = style({
  margin: 0,
  fontFamily: vars.font.semibold,
  fontSize: "14px",
  lineHeight: 1.5,
  color: vars.color.black,
  maxWidth: "100%",
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
});

export const currentReadingModalBookMeta = style({
  margin: 0,
  fontFamily: vars.font.body,
  fontSize: "12px",
  lineHeight: 1.45,
  color: "#777777",
  maxWidth: "100%",
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
});

export const goalHelpLead = style({
  margin: "0 0 12px",
  fontFamily: vars.font.body,
  fontSize: "14px",
  lineHeight: 1.55,
  color: vars.color.gray600,
});

export const goalHelpList = style({
  margin: 0,
  paddingLeft: "18px",
  display: "flex",
  flexDirection: "column",
  gap: "9px",
  fontFamily: vars.font.body,
  fontSize: "14px",
  lineHeight: 1.55,
  color: vars.color.black,
});

export const goalModalBody = style({
  display: "grid",
  gap: "34px",
});

// 목표 설정 내용에 맞춘 팝업 너비와 영역 간격
export const goalSettingsModal = style({
  width: "min(467px, 100%)",
  boxSizing: "border-box",
  display: "flex",
  flexDirection: "column",
  gap: "30px",
});

export const goalSettingsActions = style([modalControlStyles.pairedActions, { gap: "10px" }]);

export const goalInputLabel = style({
  display: "flex",
  alignItems: "flex-start",
  justifyContent: "space-between",
  gap: "12px",
  fontFamily: vars.font.heading,
  fontSize: "16px",
  color: vars.color.black,
  textAlign: "left",
  "@media": {
    "screen and (max-width: 400px)": { flexWrap: "wrap" },
  },
});

export const goalLimitInfo = style({
  display: "flex",
  flexDirection: "column",
  alignItems: "flex-start",
  justifyContent: "center",
  gap: "6px",
});

export const goalPeriodHeading = style({ display: "flex", alignItems: "center", gap: "8px", minHeight: "22px" });

export const goalLimitPill = style({
  maxWidth: "100%",
  padding: "4px 8px",
  borderRadius: "999px",
  backgroundColor: vars.color.brandBg,
  color: vars.color.brandText,
  fontFamily: vars.font.medium,
  fontSize: "12px",
  lineHeight: 1,
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
});

export const goalLimitMuted = style({
  maxWidth: "100%",
  fontFamily: vars.font.medium,
  fontSize: "14px",
  lineHeight: 1.25,
  color: vars.color.gray600,
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
});

export const goalLimitDanger = style([
  goalLimitMuted,
  {
    color: vars.color.negativeText,
  },
]);

export const goalModalActions = style([
  modalControlStyles.pairedActions,
  {
    marginTop: "20px",
  },
]);
