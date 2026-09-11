import { keyframes, style } from "@vanilla-extract/css";
import { vars } from "@/app/styles/tokens.css.ts";

const sheetEnter = keyframes({
  from: {
    transform: "translateY(100%)",
  },
  to: {
    transform: "translateY(0)",
  },
});

export const sheetLayer = style({
  position: "fixed",
  top: 0,
  right: 0,
  bottom: "auto",
  left: 0,
  width: "100%",
  height: "var(--app-viewport-height, 100dvh)",
  maxWidth: "100vw",
  zIndex: 3100,
  display: "flex",
  justifyContent: "center",
  alignItems: "flex-end",
  overflowX: "hidden",
});

export const sheetBackdrop = style({
  position: "absolute",
  inset: 0,
  width: "100%",
  height: "100%",
  border: 0,
  backgroundColor: "rgba(0, 0, 0, 0.48)",
  cursor: "default",
});

export const commentSheet = style({
  position: "relative",
  zIndex: 1,
  width: "100%",
  maxWidth: "600px",
  minWidth: 0,
  height: "85%",
  boxSizing: "border-box",
  borderRadius: "22px 22px 0 0",
  backgroundColor: "#ffffff",
  display: "flex",
  flexDirection: "column",
  overflow: "hidden",
  willChange: "transform",
  animation: `${sheetEnter} 240ms cubic-bezier(0.22, 1, 0.36, 1)`,
});

export const sheetHandle = style({
  position: "relative",
  width: "100%",
  height: "30px",
  flexShrink: 0,
  cursor: "grab",
  touchAction: "none",
  userSelect: "none",
  selectors: {
    "&::after": {
      content: "",
      position: "absolute",
      top: "12px",
      left: "50%",
      width: "40px",
      height: "4px",
      borderRadius: "999px",
      backgroundColor: vars.color.gray300,
      transform: "translateX(-50%)",
    },
    "&:active": {
      cursor: "grabbing",
    },
    "&:focus-visible": {
      outline: `2px solid ${vars.color.gray500}`,
      outlineOffset: "-2px",
    },
  },
});

export const commentSheetBody = style({
  width: "100%",
  minHeight: 0,
  minWidth: 0,
  boxSizing: "border-box",
  flex: 1,
  padding: "10px 20px 24px",
  display: "flex",
  flexDirection: "column",
  overflowY: "auto",
  overflowX: "hidden",
  overscrollBehavior: "contain",
  WebkitOverflowScrolling: "touch",
  "@media": {
    "screen and (max-width: 480px)": {
      padding: "10px 14px 20px",
    },
  },
});

export const commentEmpty = style({
  flex: 1,
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  justifyContent: "center",
});

export const commentLoading = style({
  width: "100%",
  minHeight: 0,
  flex: 1,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
});

export const commentEmptyIcon = style({
  width: "42px",
  height: "42px",
  marginBottom: "12px",
  opacity: 0.72,
});

export const commentEmptyTitle = style({
  color: vars.color.black,
  fontFamily: vars.font.semibold,
  fontSize: "16px",
});

export const commentEmptyText = style({
  maxWidth: "250px",
  marginTop: "6px",
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: "14px",
  lineHeight: 1.55,
  textAlign: "center",
  wordBreak: "keep-all",
});

export const replyList = style({
  width: "100%",
  minWidth: 0,
  margin: 0,
  padding: 0,
  display: "flex",
  flexDirection: "column",
  gap: "10px",
  listStyle: "none",
});

export const replyThread = style({
  width: "100%",
  minWidth: 0,
  display: "flex",
  flexDirection: "column",
  gap: "10px",
});

export const replyItem = style({
  position: "relative",
  width: "100%",
  minWidth: 0,
  maxWidth: "100%",
  boxSizing: "border-box",
  display: "flex",
  alignItems: "flex-start",
  justifyContent: "space-between",
  gap: "12px",
  minHeight: "92px",
  backgroundColor: "#ffffff",
  "@media": {
    "screen and (max-width: 480px)": {
      gap: "8px",
    },
  },
});

export const childReplyItem = style([
  replyItem,
  {
    width: "calc(100% - 44px)",
    minHeight: "56px",
    marginLeft: "44px",
    alignItems: "flex-start",
    "@media": {
      "screen and (max-width: 480px)": {
        width: "calc(100% - 28px)",
        marginLeft: "28px",
      },
    },
  },
]);

export const childReplyList = style({
  width: "100%",
  minWidth: 0,
  display: "flex",
  flexDirection: "column",
  gap: "8px",
});

export const replyItemWrap = style({
  width: "100%",
  minWidth: 0,
  flex: 1,
  display: "flex",
  gap: "12px",
  "@media": {
    "screen and (max-width: 480px)": {
      gap: "10px",
    },
  },
});

export const deletedReplyItem = style({
  minHeight: "56px",
});

export const deletedReplyItemWrap = style({
  width: "100%",
  minWidth: 0,
  display: "flex",
  alignItems: "center",
  gap: "12px",
});

export const replyWriterProfileImgArea = style({
  display: "inline-flex",
});

export const replyProfileLink = style({
  display: "inline-flex",
  flexShrink: 0,
  borderRadius: "50%",
  selectors: {
    "&:focus-visible": {
      outline: `2px solid ${vars.color.brand}`,
      outlineOffset: "2px",
    },
  },
});

export const replyBody = style({
  minWidth: 0,
  display: "flex",
  flexDirection: "column",
});

export const replyTextArea = style({
  display: "flex",
  flexDirection: "column",
  gap: "4px",
});

export const replyWriterRow = style({
  minWidth: 0,
  display: "flex",
  alignItems: "center",
  gap: "6px",
});

export const replyProfileImage = style({
  width: "32px",
  height: "32px",
  flexShrink: 0,
  borderRadius: "50%",
  objectFit: "cover",
  backgroundColor: vars.color.gray300,
});

export const replyWriter = style({
  display: "inline-block",
  minWidth: 0,
  maxWidth: "190px",
  color: vars.color.black,
  fontFamily: vars.font.semibold,
  fontSize: "16px",
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
  textDecoration: "none",
  selectors: {
    "&:focus-visible": {
      outline: `2px solid ${vars.color.brand}`,
      outlineOffset: "2px",
    },
  },
});


export const replyContent = style({
  minWidth: 0,
  margin: 0,
  fontFamily: vars.font.body,
  fontSize: "16px",
  lineHeight: "1.4",
  letterSpacing: "-1%",
  whiteSpace: "pre-wrap",
  wordBreak: "break-word",
  overflowWrap: "anywhere",
});

export const deletedReplyContent = style([
  replyContent,
  {
    color: vars.color.gray600,
    fontStyle: "italic",
  },
]);

export const replyMentionLink = style({
  color: vars.color.brandText,
  textDecoration: "none",
  selectors: {
    "&:focus-visible": {
      outline: `2px solid ${vars.color.brand}`,
      outlineOffset: "1px",
    },
  },
});

export const replyItemMetrics = style({
  width: "100%",
  minHeight: "24px",
  display: "flex",
  flexDirection: "column",
  gap: "10px",
  marginTop: "6px",
});

export const replyDate = style({
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: "14px",
  lineHeight: 1,
});

export const replyMetricButton = style({
  padding: 0,
  display: "inline-flex",
  alignItems: "center",
  gap: "4px",
  backgroundColor: "transparent",
  color: vars.color.gray600,
  fontFamily: vars.font.medium,
  fontSize: "14px",
  cursor: "pointer",
});

export const replyMoreButton = style([
  replyMetricButton,
  {
    selectors: {
      "&::before":
          {
            content: "''",
            height: "1px",
            width: "30px",
            backgroundColor: vars.color.gray400
          }
    }
  },
]);

export const replyLikeButton = style([
  replyMetricButton,
  {
    color: "#ff747c",
    minWidth: "20px",
    selectors: {
      "&:disabled": {
        cursor: "wait",
        opacity: 0.55,
      },
    },
  }
])

export const replyLikeGroup = style({
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  color: "#ff747c",
  fontFamily: vars.font.body,
  fontSize: "12px",
  lineHeight: 1,
});

export const replyLikeCount = style({
  height: "18px",
  minWidth: "20px",
  color: "#ff747c",
  fontSize: "12px",
});

export const replyItemActions = style({
  flexShrink: 0,
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  gap: "4px",
});

export const actionMenuRoot = style({
  position: "relative",
  display: "inline-flex",
});

export const actionMenuTrigger = style({
  width: "18px",
  minWidth: "18px",
  height: "18px",
  padding: 0,
  border: 0,
  borderRadius: "50%",
  backgroundColor: "transparent",
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  gap: 0,
  cursor: "pointer",
  // selectors: {
  //   "&:hover, &:focus-visible": {
  //     backgroundColor: vars.color.gray100,
  //     outline: "none",
  //   },
  // },
});

export const actionMenuIcon = style({
  width: "18px",
  height: "18px",
  display: "block",
});

export const actionMenu = style({
  position: "absolute",
  top: "calc(100% + 4px)",
  right: 0,
  zIndex: 30,
  minWidth: "112px",
  padding: "5px",
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: "16px",
  backgroundColor: "#ffffff",
  boxShadow: "0 8px 24px rgba(0, 0, 0, 0.12)",
  display: "flex",
  flexDirection: "column",
  gap: "2px",
});

export const actionMenuOption = style({
  width: "100%",
  minHeight: "34px",
  padding: "0 10px",
  border: 0,
  borderRadius: "10px",
  backgroundColor: "transparent",
  color: vars.color.black,
  fontFamily: vars.font.body,
  fontSize: "14px",
  textAlign: "left",
  whiteSpace: "nowrap",
  cursor: "pointer",
  selectors: {
    "&:hover, &:focus-visible": {
      backgroundColor: vars.color.gray100,
      color: vars.color.black,
      outline: "none",
    },
  },
});

export const actionMenuOptionDanger = style([
  actionMenuOption,
  {
    color: "#c94b4b",
    selectors: {
      "&:hover, &:focus-visible": {
        backgroundColor: "#fff1f1",
        color: "#a93636",
      },
      "&:disabled": {
        color: vars.color.gray500,
        cursor: "default",
      },
    },
  },
]);

export const commentComposer = style({
  width: "100%",
  minWidth: 0,
  boxSizing: "border-box",
  flexShrink: 0,
  borderTop: `1px solid ${vars.color.gray300}`,
  backgroundColor: "#ffffff",
});

export const commentEditHeader = style({
  minHeight: "30px",
  padding: "8px 18px 0",
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  color: vars.color.gray600,
  fontFamily: vars.font.medium,
  fontSize: "14px",
});

export const commentEditCancelButton = style({
  padding: "3px 6px",
  border: 0,
  backgroundColor: "transparent",
  color: vars.color.gray600,
  fontFamily: vars.font.medium,
  fontSize: "14px",
  cursor: "pointer",
  selectors: {
    "&:hover, &:focus-visible": {
      color: vars.color.black,
      outline: "none",
      textDecoration: "underline",
    },
  },
});

export const commentForm = style({
  width: "100%",
  minWidth: 0,
  boxSizing: "border-box",
  flexShrink: 0,
  padding: "10px 16px calc(10px + env(safe-area-inset-bottom))",
  display: "flex",
  alignItems: "center",
  gap: "8px",
  "@media": {
    "screen and (max-width: 360px)": {
      paddingLeft: "12px",
      paddingRight: "12px",
    },
  },
});

export const commentInput = style({
  minWidth: 0,
  height: "40px",
  flex: 1,
  padding: "0 14px",
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: "21px",
  backgroundColor: "#ffffff",
  color: vars.color.black,
  fontFamily: vars.font.body,
  fontSize: "16px",
  outline: "none",
  selectors: {
    "&::placeholder": {
      color: vars.color.gray500,
    },
    "&:focus": {
      borderColor: vars.color.gray600,
    },
  },
});

export const commentSubmitButton = style({
  flexShrink: 0,
  height: "40px",
  padding: "0 24px",
  border: `1px solid ${vars.color.gray600}`,
  borderRadius: "21px",
  backgroundColor: "#ffffff",
  color: vars.color.gray700,
  fontFamily: vars.font.medium,
  fontSize: "16px",
  cursor: "pointer",
  "@media": {
    "screen and (max-width: 360px)": {
      padding: "0 16px",
    },
  },
  selectors: {
    "&:disabled": {
      borderColor: vars.color.gray300,
      backgroundColor: vars.color.gray100,
      color: vars.color.gray500,
      cursor: "default",
    },
  },
});

export const focusedReplyItem = style({
  borderRadius: "12px",
  backgroundColor: vars.color.gray100,
  transition: "background-color 0.2s ease",
});
