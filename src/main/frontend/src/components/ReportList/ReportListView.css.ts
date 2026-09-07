import { style } from "@vanilla-extract/css";
import { vars } from "@/app/styles/tokens.css";

const statusPill = style({
  flexShrink: 0,
  padding: "4px 8px",
  borderRadius: "999px",
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  fontFamily: vars.font.medium,
  fontSize: "12px",
  lineHeight: 1,
  letterSpacing: "-1%",
  whiteSpace: "nowrap",
});

export const page = style({
  width: "100%",
  minHeight: "100svh",
  backgroundColor: "#ffffff",
  paddingBottom: '60px'
});

export const content = style({
  display: "flex",
  flexDirection: "column",
});

export const header = style({
  position: "sticky",
  top: `calc(${vars.headerHeight} - var(--header-scroll-offset, 0px))`,
  zIndex: 996,
  width: "100svw",
  height: "90px",
  marginLeft: "calc(50% - 50svw)",
  backgroundColor: "#ffffff",
  boxShadow: "0px 3px 10px rgba(0, 0, 0, 0.08)",
});

export const headerWrap = style({
  width: "100%",
  maxWidth: "600px",
  height: "100%",
  margin: "0 auto",
  padding: "10px 24px",
  display: "flex",
  gap: "12px",
  alignItems: "center",
});

export const coverFrame = style({
  width: "47px",
  height: "70px",
  borderRadius: "3px",
  overflow: "hidden",
  flexShrink: 0,
  backgroundColor: vars.color.gray100,
});

export const coverImage = style({
  width: "100%",
  height: "100%",
  objectFit: "cover",
  display: "block",
});

export const headingArea = style({
  minWidth: 0,
  display: "flex",
  flexDirection: "column",
  gap: "3px",
});

export const bookTitle = style({
  margin: 0,
  fontFamily: vars.font.heading,
  fontSize: "16px",
  lineHeight: 1.45,
  color: vars.color.black,
  display: "-webkit-box",
  overflow: "hidden",
  WebkitLineClamp: 2,
  WebkitBoxOrient: "vertical",
});

export const authorRatingLine = style({
  display: "flex",
  alignItems: "center",
  gap: "6px",
  minWidth: 0,
});

export const meta = style({
  minWidth: 0,
  margin: 0,
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: "12px",
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
});

export const metaSeparator = style({
  flexShrink: 0,
  color: vars.color.gray500,
  fontSize: "9px",
});

export const ratingSummary = style({
  display: "inline-flex",
  alignItems: "center",
  gap: "3px",
  color: vars.color.gray600,
  fontFamily: vars.font.medium,
  fontSize: "12px",
});

export const ratingStar = style({
  color: "#f6c944",
  fontSize: "12px",
  lineHeight: 1,
});

export const filters = style({
  display: "flex",
  alignItems: "center",
  gap: "8px",
  minHeight: "42px",
  padding: "18px 0",
});

export const list = style({
  display: "flex",
  flexDirection: "column",
  gap: "14px",

});

export const item = style({
  position: "relative",
  display: "flex",
  flexDirection: "column",
  gap: "10px",
  minHeight: "172px",
  padding: "16px",
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: "22px",
  backgroundColor: "#ffffff",
});

export const itemTop = style({
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  gap: "8px",
  minWidth: 0,
});

export const profileButton = style({
  flex: "1 1 auto",
  minWidth: 0,
  overflow: "hidden",
  padding: 0,
  border: 0,
  backgroundColor: "transparent",
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "flex-start",
  gap: "8px",
  textAlign: "left",
  cursor: "pointer",
});

export const profileImage = style({
  width: "30px",
  height: "30px",
  flexShrink: 0,
  borderRadius: "50%",
  objectFit: "cover",
  backgroundColor: vars.color.gray300,
});


export const writer = style({
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

export const statusWrap = style({
  flex: "1 1 auto",
  minWidth: 0,
  overflow: "hidden",
  display: "flex",
  alignItems: "center",
  gap: "8px",
});

export const actionMenuWrap = style({
  display: "flex",
  flexShrink: 0,
});

export const actionMenuTriggerIcon = style({
  transform: "rotate(90deg)",
});


export const statusDone = style([
  statusPill,
  {
    border: `1px solid ${vars.color.brand}`,
    backgroundColor: vars.color.brandBg,
    color: vars.color.brandText,
  },
]);

export const statusReading = style([
  statusPill,
  {
    border: `1px solid ${vars.color.gray600}`,
    backgroundColor: vars.color.gray600,
    color: "#ffffff",
  },
]);

export const statusStopped = style([
  statusPill,
  {
    border: `1px solid ${vars.color.gray300}`,
    backgroundColor: vars.color.gray100,
    color: vars.color.gray600,
  },
]);

export const reportRating = style({
  flexShrink: 0,
  display: "inline-flex",
  alignItems: "center",
  gap: "3px",
  color: vars.color.gray900,
  fontFamily: vars.font.medium,
  fontSize: "14px",
});

export const reportRatingIcon = style({
  display: "block",
  width: "14px",
  height: "14px",
});

export const reportContentWrap = style({
  maxHeight: "70px",
  overflow: "clip",
});

export const reportContentWrapOpen = style([
  reportContentWrap,
  {
    maxHeight: "3000px",
  },
]);

export const reportContent = style({
  margin: 0,
  color: "#565656",
  fontFamily: vars.font.body,
  fontSize: "16px",
  lineHeight: "1.45",
  letterSpacing: '-1%',
  whiteSpace: "pre-wrap",
  wordBreak: "break-word",
});

export const expandButton = style({
  position: "absolute",
  bottom: "14px",
  left: "50%",
  width: "20px",
  height: "20px",
  padding: 0,
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  backgroundColor: "transparent",
  color: vars.color.black,
  transform: "translateX(-50%)",
  cursor: "pointer",
});

export const expandArrow = style({
  transition: "transform 180ms ease",
});

export const expandArrowOpen = style([
  expandArrow,
  {
    transform: "rotate(180deg)",
  },
]);

export const itemMetrics = style({
  minHeight: "24px",
  display: "flex",
  alignItems: "center",
  justifyContent: "flex-end",
  gap: "8px",
});

export const itemFooter = style({
  width: "100%",
  minHeight: "24px",
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  gap: "12px",
  marginTop: "auto",
});

export const translationButton = style({
  flexShrink: 0,
  minHeight: "24px",
  padding: 0,
  border: 0,
  backgroundColor: "transparent",
  color: "#8a8a8a",
  fontFamily: vars.font.semibold,
  fontSize: "14px",
  cursor: "pointer",
  transition: "color 160ms ease, opacity 160ms ease",
  selectors: {
    "&:hover:not(:disabled)": { color: "#555555" },
    "&:disabled": { cursor: "default", opacity: 0.5 },
    "&:focus-visible": { outline: `2px solid ${vars.color.brand}`, outlineOffset: 2 },
  },
});

export const metricButton = style({
  minWidth: "32px",
  height: "24px",
  padding: 0,
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  gap: "4px",
  backgroundColor: "transparent",
  color: "#ff747c",
  fontFamily: vars.font.body,
  fontSize: "14px",
  cursor: "pointer",
  selectors: {
    "&:disabled": {
      cursor: "default",
      opacity: 0.5,
    },
  },
});

export const metricGroup = style({
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

export const metricIconButton = style({
  width: "16px",
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
    "&:focus-visible": { outline: `2px solid ${vars.color.brand}`, outlineOffset: 2 },
  },
});

export const metricCountButton = style({
  color: "#ff747c",
});

export const metricIcon = style({
  width: "16px",
  height: "16px",
  flexShrink: 0,
});

export const commentButton = style([
  metricButton,
  {
    color: "#777777",
  },
]);

export const commentIcon = style({
  width: "16px",
  height: "16px",
  flexShrink: 0,
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 1.5,
  strokeLinejoin: "round",
});

export const empty = style({
  margin: "48px 0",
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: "14px",
  textAlign: "center",
});
