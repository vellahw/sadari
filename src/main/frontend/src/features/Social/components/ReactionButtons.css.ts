import { style } from "@vanilla-extract/css";
import { vars } from "@/app/styles/tokens.css";

export const likeButtonContainer = style({
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  gap: 2,
});

export const likeButton = style({
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  minWidth: "17px",
  height: "24px",
  padding: 0,
  border: 0,
  backgroundColor: "transparent",
  color: "#d84a5f",
  fontFamily: vars.font.semibold,
  fontSize: "12px",
  lineHeight: 1,
  cursor: "pointer",
  flexShrink: 0,

  selectors: {
    "&:disabled": {
      cursor: "default",
      opacity: 0.55,
    },
  },
});

export const likeCount = style({
  minWidth: "14px",
  height: "24px",
  color: "#FF8386",
  fontFamily: vars.font.semibold,
  fontSize: "12px",
  textAlign: "left",
});

export const commentIndicator = style({
  minWidth: "34px",
  height: "24px",
  padding: 0,
  border: 0,
  background: "transparent",
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  gap: "4px",
  cursor: "pointer",
  color: "#777777",
  fontFamily: vars.font.semibold,
  fontSize: "12px",
  lineHeight: 1,
});

export const commentIcon = style({
  display: "block",
});

export const commentCount = style({
  minWidth: "10px",
  textAlign: "left",
});

