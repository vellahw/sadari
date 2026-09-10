import { keyframes, style } from "@vanilla-extract/css";
import { vars } from "@/app/styles/tokens.css";

const viewFadeIn = keyframes({
  "0%": {
    opacity: 0,
  },
  "100%": {
    opacity: 1,
  },
});

const contentPanelFadeIn = keyframes({
  "0%": {
    opacity: 0,
    filter: "blur(2px)",
  },
  "100%": {
    opacity: 1,
    filter: "blur(0)",
  },
});

export const viewFade = style({
  animation: `${viewFadeIn} 580ms ease-out both`,
});

export const contentSwitchFade = style({
  animation: `${contentPanelFadeIn} 260ms cubic-bezier(0.22, 1, 0.36, 1) both`,
});

const contentPanelFadeOut = keyframes({
  "0%": {
    opacity: 1,
    filter: "blur(0)",
  },
  "100%": {
    opacity: 0,
    filter: "blur(2px)",
  },
});

export const contentSwitchFadeOut = style({
  pointerEvents: "none",
  animation: `${contentPanelFadeOut} 90ms ease-in both`,
});

export const page = style({
  position: "relative",
  width: "100vw",
  marginLeft: "calc(50% - 50vw)",
  minHeight: "calc(100svh - 52px - 60px)",
  backgroundColor: vars.color.background,
  overflow: "hidden",

  selectors: {
    "&::before": {
      content: "",
      position: "absolute",
      top: "-36px",
      right: "-36px",
      left: "-36px",
      height: "var(--book-bg-fade-height, 650px)",
      zIndex: 0,
      backgroundImage: "var(--book-bg-image)",
      backgroundRepeat: "no-repeat",
      backgroundPosition: "center top",
      backgroundSize: "cover",
      filter: "blur(24px)",
      transform: "scale(1.12)",
      opacity: 0.86,
      pointerEvents: "none",
      maskImage:
        "linear-gradient(180deg, #000 0%, rgba(0, 0, 0, 0.94) 54%, rgba(0, 0, 0, 0.62) 72%, rgba(0, 0, 0, 0.28) 86%, rgba(0, 0, 0, 0.08) 95%, rgba(0, 0, 0, 0) 100%)",
      WebkitMaskImage:
        "linear-gradient(180deg, #000 0%, rgba(0, 0, 0, 0.94) 54%, rgba(0, 0, 0, 0.62) 72%, rgba(0, 0, 0, 0.28) 86%, rgba(0, 0, 0, 0.08) 95%, rgba(0, 0, 0, 0) 100%)",
    },
  },
});

export const detail = style({
  position: "relative",
  zIndex: 1,
  maxWidth: "600px",
  width: "100%",
  margin: "0 auto",
  padding: "28px 18px 36px",
  display: "flex",
  flexDirection: "column",
  gap: "24px",
});

export const header = style({
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  textAlign: "center",
  gap: "14px",
  padding: "40px 22px 30px",
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: "22px",
  backgroundColor: "rgba(255, 255, 255, 0.96)",
  boxShadow: "0 8px 22px rgba(0, 0, 0, 0.05)",
});

export const coverFrame = style({
  width: "126px",
  aspectRatio: "2 / 3",
  borderRadius: "8px",
  overflow: "hidden",
  backgroundColor: "#ffffff",
  boxShadow: "0 10px 24px rgba(0, 0, 0, 0.16)",
});

export const coverImage = style({
  display: "block",
  width: "100%",
  height: "100%",
  objectFit: "cover",
});

export const title = style({
  margin: 0,
  fontFamily: vars.font.heading,
  fontSize: "20px",
  lineHeight: 1.35,
  color: vars.color.black,
  wordBreak: "keep-all",
});

export const meta = style({
  margin: 0,
  fontFamily: vars.font.body,
  fontSize: "14px",
  lineHeight: 1.45,
  color: "#666666",
});

export const bookAverageSummary = style({
  minHeight: "19px",
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  gap: "4px",
  color: vars.color.black,
  lineHeight: 1,
});

export const bookAverageStar = style({
  width: "18px",
  height: "18px",
  display: "block",
  flexShrink: 0,
  color: "#ffd45c",
});

export const bookAverageScore = style({
  fontFamily: vars.font.semibold,
  fontSize: "14px",
  lineHeight: 1,
  letterSpacing: 0,
});

export const bookAverageLabel = style({
  marginRight: "2px",
  fontFamily: vars.font.body,
  fontSize: "12px",
  lineHeight: 1,
  color: vars.color.gray600,
  letterSpacing: 0,
});

export const bookAverageEmpty = style({
  fontFamily: vars.font.body,
  fontSize: "14px",
  lineHeight: 1.45,
  color: vars.color.gray600,
});

export const bookInfoButton = style({
  height: "34px",
  padding: "0 14px",
  border: `1px solid ${vars.color.gray400}`,
  borderRadius: vars.radius.xl,
  backgroundColor: "#ffffff",
  color: vars.color.black,
  fontFamily: vars.font.semibold,
  fontSize: "14px",
  lineHeight: 1,
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  gap: "6px",
  cursor: "pointer",
});

export const bookInfoActionRow = style({
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  gap: "8px",
  flexWrap: "wrap",
  width: "100%",
});

export const contentPanel = style({
  display: "flex",
  flexDirection: "column",
  gap: "24px",
  minHeight: "auto",
  padding: 0,
});

export const reportStatsSection = style({
  position: "relative",
  width: "100%",
  padding: "18px 14px",
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: "22px",
  backgroundColor: "#ffffff",
  boxShadow: "0 8px 22px rgba(0, 0, 0, 0.05)",
  boxSizing: "border-box",
});

export const reportStatsGrid = style({
  display: "grid",
  gridTemplateColumns: "repeat(4, minmax(0, 1fr))",
  gap: "8px",
});

export const bookInfoRows = style({
  display: "flex",
  flexDirection: "column",
});

export const bookInfoRow = style({
  minHeight: "50px",
  padding: "9px 6px",
  display: "grid",
  gridTemplateColumns: "86px minmax(0, 1fr)",
  alignItems: "center",
  gap: "12px",
  borderBottom: `1px solid ${vars.color.gray200}`,
  boxSizing: "border-box",
  selectors: {
    "&:last-child": {
      borderBottom: 0,
    },
  },
});

export const bookInfoLabel = style({
  fontFamily: vars.font.semibold,
  fontSize: "14px",
  lineHeight: 1.4,
  color: vars.color.gray600,
});

export const bookInfoValue = style({
  minWidth: 0,
  fontFamily: vars.font.semibold,
  fontSize: "16px",
  lineHeight: 1.45,
  color: vars.color.black,
  letterSpacing: 0,
  textAlign: "right",
  wordBreak: "break-word",
});

export const bookInfoSearchLink = style([
  bookInfoValue,
  {
    justifySelf: "end",
    maxWidth: "100%",
    padding: "4px 6px",
    border: 0,
    borderRadius: "6px",
    backgroundColor: "transparent",
    cursor: "pointer",
    textDecoration: "none",
    transition: "background-color 140ms ease, color 140ms ease",

    selectors: {
      "&:hover": {
        backgroundColor: vars.color.gray100,
        color: vars.color.brandText,
      },
      "&:focus-visible": {
        outline: "2px solid #78b991",
        outlineOffset: "2px",
      },
    },
  },
]);

export const reportStatsItem = style({
  position: "relative",
  minWidth: 0,
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  justifyContent: "center",
  gap: "4px",
  textAlign: "center",
});

export const reportStatsLabel = style({
  height: "30px",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  fontFamily: vars.font.medium,
  fontSize: "12px",
  lineHeight: 1.25,
  color: vars.color.gray600,
  letterSpacing: 0,
  whiteSpace: "normal",
  wordBreak: "keep-all",
});

export const reportStatsValue = style({
  minWidth: 0,
  minHeight: "32px",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  fontFamily: vars.font.heading,
  fontSize: "16px",
  lineHeight: 1.2,
  color: vars.color.black,
  letterSpacing: 0,
  whiteSpace: "normal",
  wordBreak: "keep-all",
});

export const reportStatusRead = style({
  color: vars.color.black,
});

export const reportStatusDone = style({
  color: "#72a980",
});

export const reportStatusStop = style({
  color: "#d98686",
});

export const reportGradeValue = style([
  reportStatsValue,
  {
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    gap: "3px",
  },
]);

export const reportGradeStar = style({
  width: "18px",
  height: "18px",
  display: "block",
  flexShrink: 0,
  color: "#ffd45c",
});

export const periodStatButton = style({
  position: "relative",
  width: "100%",
  minWidth: 0,
  padding: 0,
  border: 0,
  backgroundColor: "transparent",
  color: "inherit",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  cursor: "pointer",
  selectors: {
    "&:focus-visible": {
      outline: "2px solid #8ab4e8",
      outlineOffset: "4px",
      borderRadius: "8px",
    },
  },
});

export const periodTooltip = style({
  position: "absolute",
  top: "calc(100% + 12px)",
  right: 0,
  zIndex: 5,
  width: "max-content",
  maxWidth: "220px",
  padding: "9px 11px",
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: "8px",
  backgroundColor: "#ffffff",
  boxShadow: "0 8px 20px rgba(0, 0, 0, 0.10)",
  color: vars.color.gray700,
  fontFamily: vars.font.body,
  fontSize: "12px",
  lineHeight: 1.4,
  letterSpacing: 0,
  whiteSpace: "nowrap",
  opacity: 0,
  visibility: "hidden",
  transform: "translateY(-4px)",
  transition: "opacity 160ms ease, transform 160ms ease, visibility 160ms ease",
  pointerEvents: "none",
  selectors: {
    [`${periodStatButton}:hover &`]: {
      opacity: 1,
      visibility: "visible",
      transform: "translateY(0)",
    },
    [`${periodStatButton}:focus-visible &`]: {
      opacity: 1,
      visibility: "visible",
      transform: "translateY(0)",
    },
  },
});

export const periodTooltipOpen = style({
  opacity: 1,
  visibility: "visible",
  transform: "translateY(0)",
});

export const recordArea = style({
  position: "relative",
  display: "flex",
  flexDirection: "column",
  gap: "14px",
});

export const recordSection = style({
  position: "relative",
  width: "100%",
  minHeight: "180px",
  padding: "22px",
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: "22px",
  backgroundColor: "#ffffff",
  boxShadow: "0 8px 22px rgba(0, 0, 0, 0.05)",
  boxSizing: "border-box",
});

export const sectionTitle = style({
  margin: 0,
  fontFamily: vars.font.semibold,
  fontSize: "15px",
  lineHeight: 1.3,
  color: vars.color.black,
  textAlign: "left",
});

export const recordTitleRow = style({
  width: "100%",
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  marginBottom: "14px",
});

export const recordMetrics = style({
  top: "14px",
  right: "18px",
  display: "inline-flex",
  alignItems: "center",
  gap: "8px",
});

export const contentBox = style({
  margin: 0,
  padding: 0,
  fontFamily: vars.font.body,
  fontSize: "14px",
  lineHeight: 1.8,
  color: vars.color.black,
  whiteSpace: "pre-wrap",
  wordBreak: "break-word",
});

export const contentEditButton = style({
  width: "100%",
  minHeight: "118px",
  margin: 0,
  padding: 0,
  border: 0,
  backgroundColor: "transparent",
  fontFamily: vars.font.body,
  fontSize: "14px",
  lineHeight: 1.8,
  color: vars.color.black,
  display: "flex",
  alignItems: "flex-start",
  justifyContent: "flex-start",
  textAlign: "left",
  whiteSpace: "pre-wrap",
  wordBreak: "break-word",
  cursor: "text",
});

export const recordEditor = style({
  width: "100%",
  display: "flex",
  flexDirection: "column",
  gap: "8px",
});

export const recordByteCounter = style({
  alignSelf: "flex-end",
  fontFamily: vars.font.body,
  fontSize: "11px",
  lineHeight: 1.4,
  color: vars.color.gray500,
});

export const recordTextArea = style({
  width: "100%",
  minHeight: "118px",
  padding: 0,
  border: 0,
  outline: 0,
  overflow: "hidden",
  resize: "none",
  backgroundColor: "transparent",
  fontFamily: vars.font.body,
  fontSize: "14px",
  lineHeight: 1.8,
  color: vars.color.black,
  boxSizing: "border-box",
});

export const editActions = style({
  width: "100%",
  display: "flex",
  alignItems: "center",
  justifyContent: "flex-end",
  gap: "8px",
  padding: "0 2px",
});

export const recordActionButtons = style({
  width: "100%",
  display: "flex",
  alignItems: "center",
  justifyContent: "flex-end",
  gap: "8px",
  padding: "0 2px",
});
