import { vars } from "@/app/styles/tokens.css";
import { globalStyle, style } from "@vanilla-extract/css";

export const overlay = style({
  position: "fixed",
  zIndex: 1201,
  inset: 0,
  padding: 16,
  boxSizing: "border-box",
  display: "grid",
  placeItems: "center",
  overflow: "hidden",
  overscrollBehavior: "contain",
  pointerEvents: "none",
});

export const page = style({
  width: "100%",
  display: "flex",
  justifyContent: "center",
});

export const backgroundOverlay = style({
  position: "fixed",
  zIndex: 1200,
  inset: 0,
  backgroundColor: "rgb(0 0 0 / 60%)",
  pointerEvents: "auto",
});

export const surface = style({
  position: "relative",
  zIndex: 1,
  width: "100%",
  maxWidth: 600,
  maxHeight: "calc(100dvh - 32px)",
  padding: "20px 16px",
  borderRadius: 22,
  backgroundColor: vars.color.background,
  boxSizing: "border-box",
  display: "flex",
  flexDirection: "column",
  gap: 20,
  overflowX: "hidden",
  overflowY: "auto",
  scrollbarWidth: "none",
  pointerEvents: "auto",
  selectors: {
    "&::before": {
      content: "",
      position: "absolute",
      top: -36,
      right: -36,
      left: -36,
      height: 650,
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

export const successionSurface = style([
  surface,
  {
    minHeight: "min(720px, calc(100dvh - 32px))",
  },
]);

export const pageSurface = style([
  surface,
  {
    maxHeight: "none",
    borderRadius: 0,
    overflow: "visible",
    pointerEvents: "auto",
    padding: "20px 0",

  },
]);

globalStyle(`${surface}::-webkit-scrollbar`, {
  display: "none",
});

globalStyle(`${surface} > *`, {
  position: "relative",
  zIndex: 1,
  flexShrink: 0,
});

const articleSurface = {
  padding: 20,
  borderRadius: 22,
  backgroundColor: vars.color.background,
  boxSizing: "border-box" as const,
};

export const header = style({
  position: "relative",
  width: "100%",
  minHeight: 24,
});

export const title = style({
  margin: 0,
  color: vars.color.black,
  fontFamily: vars.font.heading,
  fontSize: 20,
  lineHeight: "44px",
  letterSpacing: "-0.2px",
  textAlign: "center",
  whiteSpace: "nowrap",
});

export const closeButton = style({
  position: "absolute",
  top: 0,
  right: 0,
  width: 28,
  height: 28,
  padding: 0,
  border: 0,
  borderRadius: "50%",
  background: vars.color.gray100,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  cursor: "pointer",
  transition: "background 160ms ease",
  selectors: {
    "&:hover": {
      background: vars.color.gray200,
    },
    "&:focus-visible": {
      outline: `2px solid ${vars.color.brand}`,
      outlineOffset: 2,
    },
  },
});

export const closeIcon = style({
  width: 14,
  height: 14,
});

export const readingCard = style({
  ...articleSurface,
  width: "100%",
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  gap: 16,
});

export const bookSummary = style({
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  gap: 10,
});

export const bookCover = style({
  display: "block",
  width: 93,
  height: 137,
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: 6,
  objectFit: "cover",
  boxSizing: "border-box",
});

export const bookIdentity = style({
  width: 301,
  maxWidth: "100%",
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  gap: 6,
  textAlign: "center",
});

export const bookTitle = style({
  maxWidth: "100%",
  overflow: "hidden",
  color: vars.color.black,
  fontFamily: vars.font.semibold,
  fontSize: 18,
  lineHeight: 1.2,
  letterSpacing: "-0.18px",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
});

export const bookAuthor = style({
  maxWidth: "100%",
  overflow: "hidden",
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: 14,
  lineHeight: 1.2,
  letterSpacing: "-0.14px",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
});

export const readingPeriod = style({
  color: vars.color.black,
  fontFamily: vars.font.medium,
  fontSize: 14,
  lineHeight: 1.2,
  letterSpacing: "-0.14px",
});

export const progressArea = style({
  width: "100%",
  display: "flex",
  flexDirection: "column",
  gap: 4,
});

export const progressRow = style({
  display: "flex",
  alignItems: "center",
  gap: 6,
});

export const progressTrack = style({
  flex: 1,
  height: 12,
  overflow: "hidden",
  borderRadius: 70,
  backgroundColor: vars.color.gray200,
});

export const progressFill = style({
  display: "block",
  height: "100%",
  borderRadius: "inherit",
  backgroundColor: vars.color.brand,
  transition: "width 240ms ease",
});

export const progressRate = style({
  minWidth: 34,
  color: vars.color.brandText,
  fontFamily: vars.font.heading,
  fontSize: 12,
  lineHeight: 1.35,
  textAlign: "right",
  letterSpacing: "-0.12px",
});

export const progressDescription = style({
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: 12,
  lineHeight: 1.35,
  letterSpacing: "-0.12px",
});

export const achievementCard = style({
  ...articleSurface,
  width: "100%",
  display: "flex",
  flexDirection: "column",
  gap: 22,
});

export const achievementTitleRow = style({
  display: "flex",
  alignItems: "center",
  gap: 4,
});

export const verifiedIcon = style({
  width: 22,
  height: 22,
  flexShrink: 0,
});

export const achievementTitle = style({
  color: vars.color.black,
  fontFamily: vars.font.semibold,
  fontSize: 16,
  lineHeight: 1.35,
});

export const achievementProfiles = style({
  minHeight: 36,
  display: "flex",
  alignItems: "center",
  gap: 4,
});

export const achievementProfile = style({
  width: 36,
  height: 36,
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: "50%",
  objectFit: "cover",
  boxSizing: "border-box",
});

export const additionalAchievementCount = style({
  width: 36,
  height: 36,
  borderRadius: "50%",
  backgroundColor: vars.color.gray200,
  color: vars.color.gray700,
  fontFamily: vars.font.semibold,
  fontSize: 12,
  lineHeight: "36px",
  textAlign: "center",
});

export const noAchievement = style({
  margin: 0,
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: 14,
  lineHeight: 1.5,
});

export const summaryCard = style({
  ...articleSurface,
  width: "100%",
  display: "flex",
  flexDirection: "column",
  gap: 14,
});

export const summaryTitle = style({
  color: vars.color.black,
  fontFamily: vars.font.semibold,
  fontSize: 14,
  lineHeight: 1.2,
  letterSpacing: "-0.14px",
});

export const summaryList = style({
  display: "grid",
  gridTemplateColumns: "repeat(3, minmax(0, 1fr))",
  gap: 15,
  margin: 0,
});

export const summaryItem = style({
  minWidth: 0,
  minHeight: 96,
  padding: "20px 16px",
  borderRadius: 14,
  backgroundColor: vars.color.gray100,
  boxSizing: "border-box",
  display: "flex",
  flexDirection: "column",
  gap: 8,
});

globalStyle(`${summaryItem} dt`, {
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: 14,
  lineHeight: 1.2,
  letterSpacing: "-0.14px",
});

globalStyle(`${summaryItem} dd`, {
  margin: 0,
  color: vars.color.black,
  fontFamily: vars.font.heading,
  fontSize: 26,
  lineHeight: 1.2,
  letterSpacing: "-0.26px",
  whiteSpace: "nowrap",
});

export const resultNavigation = style({
  width: "100%",
  display: "flex",
  flexDirection: "column",
  gap: 7,
  padding: "0 16px"
});

export const navigationRow = style({
  minHeight: 40,
  padding: "10px 0",
  boxSizing: "border-box",
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  color: vars.color.black,
  fontFamily: vars.font.heading,
  fontSize: 16,
  lineHeight: 1.25,
  letterSpacing: "-0.16px",
});

export const navigationButton = style([
  navigationRow,
  {
    width: "100%",
    border: 0,
    background: "transparent",
    cursor: "pointer",
    textAlign: "left",
  },
]);

export const navigationRowMuted = style([
  navigationRow,
  {
    width: "100%",
    border: 0,
    background: "transparent",
    color: vars.color.gray600,
    fontFamily: vars.font.semibold,
    cursor: "pointer",
    textAlign: "left",
  },
]);

globalStyle(`${navigationRow} img, ${navigationButton} img, ${navigationRowMuted} img`, {
  width: 18,
  height: 18,
  flexShrink: 0,
});

globalStyle(`body:has(${overlay})`, {
  overflow: "hidden",
});
