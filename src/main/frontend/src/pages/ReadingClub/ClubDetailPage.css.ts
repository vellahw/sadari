import { vars } from "@/app/styles/tokens.css";
import { globalStyle, style } from "@vanilla-extract/css";

export const page = style({
  display: "flex",
  flexDirection: "column",
  gap: 30,
  width: "100%",
  maxWidth: 600,
  margin: "0 auto",
  padding: "20px 0",
});

export const clubSummary = style({
  position: "relative",
  display: "flex",
  flexDirection: "column",
  gap: 14,
  padding: "20px 60px 20px 20px",
  borderRadius: 20,
  background: vars.color.gray100,
});

export const moreSelect = style({
  position: "absolute",
  top: 14,
  right: 14,
  zIndex: 10,
});

export const moreButton = style({
  width: 36,
  minWidth: 36,
  height: 36,
  padding: 0,
  justifyContent: "center",
  selectors: {
    "&:hover, &:focus-visible": {
      background: vars.color.gray200,
      outline: "none",
    },
  },
});

export const moreIcon = style({
  display: "block",
  width: 20,
  height: 20,
});

export const moreOptionList = style({
  minWidth: 120,
});

export const moreOption = style({
  minHeight: 36,
  fontSize: 14,
});

export const dangerOption = style({
  color: vars.color.negativeText,
  selectors: {
    "&:hover, &:focus-visible": {
      background: vars.color.negativeBg,
      color: vars.color.negativeText,
    },
  },
});

export const chips = style({ display: "flex", flexWrap: "wrap", gap: 6 });

export const chip = style({
  padding: "4px 14px",
  borderRadius: 999,
  background: vars.color.gray900,
  color: "#fff",
  fontFamily: vars.font.body,
  fontSize: 12,
  lineHeight: "18px",
});

export const summaryText = style({
  display: "flex",
  flexDirection: "column",
  gap: 4
});

export const detailTitle = style({
  margin: 0,
  color: vars.color.gray900,
  fontFamily: vars.font.heading,
  fontSize: 18,
  lineHeight: "27px",
});

export const meta = style({
  margin: 0,
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: 14,
  lineHeight: "21px",
});

export const description = style({
  margin: 0,
  color: vars.color.gray900,
  fontFamily: vars.font.body,
  fontSize: 14,
  lineHeight: "21px",
  whiteSpace: "pre-wrap",
});

export const section = style({ display: "flex", flexDirection: "column", gap: 12 });

export const sectionTitle = style({
  margin: 0,
  color: vars.color.gray900,
  fontFamily: vars.font.semibold,
  fontSize: 14,
  lineHeight: "21px",
});

export const currentReadingCard = style({
  display: "flex",
  minHeight: 256,
  boxSizing: "border-box",
  flexDirection: "column",
  gap: 16,
  padding: 20,
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: 22,
  background: vars.color.background,
});

export const readingCardHeader = style({
  position: "relative",
  display: "flex",
  minHeight: 23,
  alignItems: "center",
  justifyContent: "center",
});

export const readingOrder = style({
  color: vars.color.gray600,
  fontFamily: vars.font.semibold,
  fontSize: 14,
  fontWeight: 600,
  lineHeight: "20px",
  textAlign: "center",
});

export const dDay = style({
  position: "absolute",
  top: 0,
  right: 0,
  padding: "4px 8px",
  borderRadius: 200,
  background: vars.color.brandBg,
  color: vars.color.brandText,
  fontFamily: vars.font.semibold,
  fontSize: 12,
  lineHeight: "16px",
  selectors: {
    '&[data-ended="true"]': {
      background: vars.color.gray200,
      color: vars.color.gray600,
    },
  },
});

export const readingEmpty = style({
  display: "flex",
  flex: 1,
  flexDirection: "column",
  alignItems: "center",
  justifyContent: "center",
  gap: 12,
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: 14,
  lineHeight: "20px",
});

export const managementBtn = style({
  fontSize: "14px",
  fontFamily: vars.font.medium,
  color: vars.color.gray600,
  textDecoration: "none",
  alignItems: "center",
  justifyContent: "flex-end",
  marginTop: "8px",
  transition: 'color 160ms ease',
  selectors: {
    '&:hover':{
      color: vars.color.gray900
    }
  }
});

export const managementReadingBtn = style([
  managementBtn,
  {marginTop: "8px"}
]);

export const earlyCloseButton = style({
  marginTop: 8,
  display: "flex",
  flexDirection:"column",
  background: vars.color.brandBg,
  color: vars.color.brandText,
  // background: "#fff",
  width: "100%",
  minWidth: "76px",
  padding: "10px 16px",
  // border: `1px solid ${vars.color.gray300}`,
  borderRadius: "10px",
  fontFamily: vars.font.medium,
  fontSize: "16px",
  alignItems: "center",
  justifyContent: "center",
  cursor: "pointer",
  transition: "border-color 160ms ease, background-color 160ms ease",

  selectors: {
    "&:focus-visible": {
      outline: "2px solid #78b991",
      outlineOffset: "2px",
    },
    "&:disabled": {
      borderColor: vars.color.gray300,
      background: "#ffffff",
      color: vars.color.gray500,
      cursor: "default",
    },
    "&:hover:not(:disabled)": {
      backgroundColor: "#D1EED9",
    },
  },
});

export const earlyCloseButtonTitle = style({
  fontFamily: vars.font.heading
})

export const managementMembersBtn = style([
  managementBtn,
  {marginTop: "16px"}
]);

export const currentReadingContent = style({
  display: "flex",
  flex: 1,
  minWidth: 0,
  flexDirection: "column",
  gap: 15,
});

export const readingBook = style({
  display: "flex",
  width: "100%",
  height: 132,
  alignItems: "center",
  gap: 16,
});

export const currentBookImage = style({
  width: 90,
  height: 132,
  boxSizing: "border-box",
  flexShrink: 0,
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: 6,
  objectFit: "cover",
});

export const currentBookInformation = style({
  display: "flex",
  height: "100%",
  minWidth: 0,
  flex: 1,
  boxSizing: "border-box",
  flexDirection: "column",
  alignItems: "flex-start",
  justifyContent: "space-between",
  padding: "10px 0",
});

export const currentBookSummary = style({
  display: "flex",
  width: "100%",
  minWidth: 0,
  flexDirection: "column",
  gap: 6,
});

export const currentBookIdentity = style({
  display: "flex",
  minWidth: 0,
  flexDirection: "column",
  gap: 4,
});

export const currentBookTitle = style({
  display: "block",
  maxWidth: "100%",
  overflow: "hidden",
  color: vars.color.gray900,
  fontFamily: vars.font.semibold,
  fontSize: 16,
  lineHeight: "20px",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
});

export const currentBookAuthor = style({
  display: "block",
  maxWidth: "100%",
  overflow: "hidden",
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: 12,
  lineHeight: "16px",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
});

export const currentReadingPeriod = style({
  color: vars.color.gray900,
  fontFamily: vars.font.medium,
  fontSize: 14,
  lineHeight: "20px",
  whiteSpace: "nowrap",
});

export const myReadingStatus = style({
  display: "flex",
  flexDirection: "column",
  gap: 2,
});

export const myReadingStatusLabel = style({
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: 12,
  lineHeight: "16px",
});

export const myReadingStatusValue = style({
  display: "flex",
  alignItems: "center",
  gap: 4,
  color: vars.color.brandText,
  fontFamily: vars.font.body,
  fontSize: 14,
  lineHeight: "20px",
});

export const readingStatusDot = style({
  width: 5,
  height: 5,
  flexShrink: 0,
  borderRadius: "50%",
  background: vars.color.brandText,
});

export const readingStatusUnavailable = style({
  color: vars.color.gray600,
});

globalStyle(`${readingStatusUnavailable} > ${readingStatusDot}`, {
  background: vars.color.gray600,
});

export const goalStatus = style({
  display: "flex",
  width: "100%",
  flexDirection: "column",
  gap: 6,
});

export const goalProgressTrack = style({
  width: "100%",
  height: 10,
  overflow: "hidden",
  borderRadius: 200,
  background: vars.color.gray200,
});

export const goalProgressFill = style({
  display: "block",
  height: "100%",
  minWidth: 0,
  maxWidth: "100%",
  borderRadius: 200,
  transition: "width 220ms ease, background-color 180ms ease",
});

export const goalAchievementText = style({
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: 12,
  lineHeight: "16px",
});

export const memberHeader = style({
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  marginBottom: "8px",
});

export const chatButton = style({
  padding: "4px 10px",
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: 999,
  background:"#fff",
  color: vars.color.gray900,
  fontFamily: vars.font.medium,
  fontSize: 14,
  cursor: "pointer",
  display: 'flex',
  alignItems: 'center',
  gap: '4px',
});

export const memberSummary = style({ display: "flex", alignItems: "center", gap: 12, minHeight: 38 });

export const memberProfiles = style({
  display: "flex",
  flexWrap: "wrap",
  alignItems: "center",
  gap: 8,
  margin: 0,
  padding: 0,
  listStyle: "none",
});

export const memberProfileItem = style({
  width: 36,
  height: 36,
});

export const memberProfileLink = style({
  display: "block",
  width: "100%",
  height: "100%",
  borderRadius: "50%",
  transition: "filter 160ms ease",
  selectors: {
    "&:hover": {
      filter: "brightness(0.94)",
    },
    "&:focus-visible": {
      outline: `2px solid ${vars.color.brandText}`,
      outlineOffset: 2,
    },
  },
});

export const memberProfilesOverlapped = style({
});

globalStyle(`${memberProfilesOverlapped} > ${memberProfileItem}:not(:first-child)`, {
  marginLeft: "-18px",
});

export const memberProfileImage = style({
  display: "block",
  width: "100%",
  height: "100%",
  boxSizing: "border-box",
  border: `1px solid ${vars.color.gray300}`,
  outline: '2px solid #fff',
  borderRadius: "50%",
  background: vars.color.gray200,
  objectFit: "cover",
});

export const memberCountText = style({
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: 14,
});

export const clubNavigation = style({
  display: "flex",
  flexDirection: "column",
  gap: '4px',
});

export const navigationRow = style({
  display: "flex",
  width: "100%",
  padding: "10px 0",
  alignItems: "center",
  justifyContent: "space-between",
  background: "transparent",
  color: vars.color.black,
  fontFamily: vars.font.semibold,
  fontSize: 16,
  textAlign: "left",
  cursor: "pointer",
});

export const readingHistoryButton = style([
  navigationRow,
  {
    color: vars.color.gray600
  }
])

export const clubLeaveButton = style([
  navigationRow,
  {
    color: vars.color.negativeText
  }
])

export const showReviewsButton = style({
  fontFamily: vars.font.semibold,
  minHeight: 57,
})

export const navigationDescription = style({
  display: "block",
  marginTop: 4,
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: 12,
  fontWeight: 400,
});

export const ActionButtonArea = style({
  marginBottom: 12
});

export const JoinButtonArea = style([
  ActionButtonArea,

  {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    flexDirection: "column",
    gap: 8
  }
])

export const JoinButtonDescription = style({
  color: vars.color.gray600
})

export const joinModalOverlay = style({
  position: "fixed",
  inset: 0,
  zIndex: 2000,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  padding: "35px 16px",
  boxSizing: "border-box",
  background: "rgba(0, 0, 0, 0.6)",
  overscrollBehavior: "contain",
});

export const joinModal = style({
  display: "flex",
  width: "100%",
  maxWidth: 343,
  height: "min(915px, calc(100dvh - 70px))",
  maxHeight: 915,
  flexDirection: "column",
  padding: "30px 16px",
  boxSizing: "border-box",
  overflow: "hidden",
  borderRadius: 22,
  background: vars.color.background,
});

export const joinModalHeader = style({
  position: "relative",
  display: "flex",
  minHeight: 32,
  alignItems: "center",
  justifyContent: "center",
});

export const joinModalTitle = style({
  margin: 0,
  color: vars.color.black,
  fontFamily: vars.font.semibold,
  fontSize: 18,
  lineHeight: "24px",
});

export const joinModalClose = style({
  position: "absolute",
  top: 0,
  right: 0,
  display: "flex",
  width: 32,
  height: 32,
  alignItems: "center",
  justifyContent: "center",
  padding: 0,
  border: 0,
  borderRadius: "50%",
  background: vars.color.gray100,
  cursor: "pointer",
  selectors: {
    "&:hover": {
      background: vars.color.gray200,
    },
    "&:focus-visible": {
      outline: "2px solid #78b991",
      outlineOffset: 2,
    },
  },
});

globalStyle(`${joinModalClose} img`, {
  width: 14,
  height: 14,
});

export const joinModalContent = style({
  display: "flex",
  minHeight: 0,
  flex: 1,
  flexDirection: "column",
  padding: "32px 0 24px",
  overflowY: "auto",
  scrollbarWidth: "none",
});

globalStyle(`${joinModalContent}::-webkit-scrollbar`, {
  display: "none",
});

export const joinModalIntro = style({
  display: "flex",
  flexDirection: "column",
  gap: 10,
  marginBottom: 32,
});

export const joinModalHeading = style({
  color: vars.color.black,
  fontFamily: vars.font.heading,
  fontSize: 20,
  lineHeight: "34px",
  letterSpacing: "-0.26px",
});

export const joinModalDescription = style({
  margin: 0,
  color: vars.color.gray600,
  fontFamily: vars.font.medium,
  fontSize: 14,
  lineHeight: "21px",
});

export const joinQuestionList = style({
  display: "flex",
  flexDirection: "column",
  gap: 26,
});

export const joinQuestionItem = style({
  display: "flex",
  flexDirection: "column",
  gap: 10,
});

export const joinQuestionLabel = style({
  color: vars.color.black,
  fontFamily: vars.font.heading,
  fontSize: 16,
  lineHeight: "24px",
});

export const joinAnswerField = style({
  position: "relative",
});

export const joinAnswerInput = style({
  display: "block",
  width: "100%",
  height: 110,
  padding: "16px 16px 34px",
  boxSizing: "border-box",
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: 12,
  background: "#f8f9fa",
  color: vars.color.black,
  fontFamily: vars.font.body,
  fontSize: 14,
  lineHeight: "21px",
  resize: "none",
  transition: "background 160ms ease, border 160ms ease",
  selectors: {
    "&::placeholder": {
      color: vars.color.gray500,
    },
    "&:focus-visible": {
      outline: "none",
      border: `1px solid ${vars.color.gray400}`,
      background: "#fff"
    },
  },
});

export const joinAnswerCount = style({
  position: "absolute",
  right: 12,
  bottom: 12,
  color: vars.color.gray500,
  fontFamily: vars.font.body,
  fontSize: 10,
  lineHeight: "14px",
});

export const joinRetentionNotice = style({
  display: "flex",
  flexDirection: "column",
  gap: 4,
  marginTop: 26,
  padding: "20px 16px",
  borderRadius: 12,
  background: vars.color.gray100,
});

export const joinRetentionTitle = style({
  color: vars.color.gray600,
  fontFamily: vars.font.semibold,
  fontSize: 14,
  lineHeight: "20px",
});

export const joinRetentionDescription = style({
  margin: 0,
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: 12,
  lineHeight: "18px",
});

export const joinModalActions = style({
  flexShrink: 0,
});
