import { style } from "@vanilla-extract/css";
import { vars } from "@/app/styles/tokens.css";

export const page = style({
  display: "flex",
  width: "100%",
  flexDirection: "column",
  gap: 12,
  padding: "20px 0 24px",
});

export const historyList = style({
  display: "flex",
  flexDirection: "column",
  gap: 12,
  margin: 0,
  padding: 0,
  listStyle: "none",
});

export const historyItem = style({
  display: "block",
});

export const historyCard = style({
  appearance: "none",
  display: "flex",
  width: "100%",
  // minHeight: 174,
  gap: 16,
  padding: 20,
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: 22,
  backgroundColor: vars.color.background,
  color: "inherit",
  cursor: "pointer",
  fontFamily: "inherit",
  textAlign: "left",
  selectors: {
    "&:disabled": {
      cursor: "default",
    },
  },
});

export const bookCover = style({
  display: "block",
  width: 90,
  height: 132,
  flexShrink: 0,
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: 6,
  backgroundColor: vars.color.gray100,
  objectFit: "cover",
});

export const historyContent = style({
  display: "flex",
  minWidth: 0,
  flex: 1,
  flexDirection: "column",
  justifyContent: "space-between",
  padding: "10px 0",
});

export const bookSummary = style({
  display: "flex",
  minWidth: 0,
  flexDirection: "column",
  alignItems: "flex-start",
  gap: 6,
});

export const bookIdentity = style({
  display: "flex",
  width: "100%",
  minWidth: 0,
  flexDirection: "column",
  gap: 4,
});

export const bookTitle = style({
  maxWidth: "100%",
  overflow: "hidden",
  color: vars.color.black,
  fontFamily: vars.font.semibold,
  fontSize: 16,
  lineHeight: 1.35,
  letterSpacing: "-0.16px",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
});

export const bookAuthor = style({
  maxWidth: "100%",
  overflow: "hidden",
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: 12,
  lineHeight: 1.35,
  letterSpacing: "-0.12px",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
});

export const readingPeriod = style({
  color: vars.color.black,
  fontFamily: vars.font.medium,
  fontSize: 14,
  lineHeight: 1.35,
  letterSpacing: "-0.14px",
  whiteSpace: "nowrap",
});

export const progressArea = style({
  display: "flex",
  width: "100%",
  flexDirection: "column",
  gap: 6,
});

export const progressTrack = style({
  width: "100%",
  height: 10,
  overflow: "hidden",
  borderRadius: 200,
  backgroundColor: vars.color.gray200,
});

export const progressFill = style({
  display: "block",
  height: "100%",
  borderRadius: "inherit",
  transition: "width 240ms ease",
});

export const progressDescription = style({
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: 12,
  lineHeight: 1.35,
  letterSpacing: "-0.12px",
});

export const stateMessage = style({
  margin: "72px 0 0",
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: 14,
  lineHeight: 1.5,
  textAlign: "center",
});

export const loadingList = style({
  display: "flex",
  flexDirection: "column",
  gap: 12,
});

export const loadingMore = style({
  padding: "10px 0",
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: 12,
  textAlign: "center",
});

export const invalidAccess = style({
  padding: "72px 0",
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: 14,
  textAlign: "center",
});

export const compactCard = style({
  "@media": {
    "screen and (max-width: 360px)": {
      gap: 12,
      padding: 16,
    },
  },
});
