/** 모임 채팅 화면 스타일 */
import {vars} from "@/app/styles/tokens.css";
import {globalStyle, style} from "@vanilla-extract/css";

export const page = style({
  display: "flex",
  width: "calc(100% + 32px)",
  maxWidth: 600,
  height: `calc(var(--app-viewport-height, 100dvh) - ${vars.headerHeight} - ${vars.navHeight} - max(${vars.space.sm}, env(safe-area-inset-bottom, 0px)))`,
  minHeight: 0,
  margin: "0 -16px",
  padding: "20px 16px 0",
  boxSizing: "border-box",
  flexDirection: "column",
  overflow: "hidden",
});

export const currentBookCard = style({
  display: "grid",
  gridTemplateColumns: "30px minmax(0, 1fr) 18px",
  minHeight: 58,
  marginBottom: 14,
  padding: "8px 12px",
  boxSizing: "border-box",
  alignItems: "center",
  gap: 10,
  borderRadius: 16,
  background: "#eef8f2",
  color: vars.color.black,
  textDecoration: "none",
  selectors: {
    "&:hover": {background: vars.color.brandBg},
    "&:focus-visible": {outline: "2px solid #78b991", outlineOffset: 2},
  },
});

export const currentBookImage = style({
  width: 30,
  height: 42,
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: 4,
  objectFit: "cover",
});

export const currentBookInformation = style({display: "flex", minWidth: 0, flexDirection: "column", gap: 3});
export const currentBookLabel = style({color: vars.color.brandText, fontFamily: vars.font.medium, fontSize: 10});
export const currentBookTitle = style({overflow: "hidden", color: vars.color.black, fontFamily: vars.font.semibold, fontSize: 14, lineHeight: "20px", textOverflow: "ellipsis", whiteSpace: "nowrap"});
export const currentBookArrow = style({color: vars.color.black, fontFamily: vars.font.medium, fontSize: 18, textAlign: "right"});

export const messagePanel = style({
  minHeight: 0,
  flex: 1,
  overflowY: "auto",
  overscrollBehavior: "contain",
  WebkitOverflowScrolling: "touch",
  padding: "0 0 20px",
  background: vars.color.background,
});

export const dateDivider = style({margin: "4px 0 18px", color: vars.color.gray500, fontFamily: vars.font.body, fontSize: 10, textAlign: "center"});
export const messageList = style({display: "flex", flexDirection: "column", gap: 16, margin: 0, padding: 0, listStyle: "none"});
export const messageRow = style({display: "flex", alignItems: "flex-start", gap: 8});
export const myMessageRow = style([messageRow, {justifyContent: "flex-end"}]);
export const avatar = style({width: 30, height: 30, marginTop: 18, flexShrink: 0, borderRadius: "50%", objectFit: "cover"});
export const messageContent = style({display: "flex", maxWidth: "82%", minWidth: 0, flexDirection: "column", alignItems: "flex-start", gap: 4});
export const myMessageContent = style([messageContent, {alignItems: "flex-end"}]);
export const sender = style({paddingLeft: 2, color: vars.color.gray600, fontFamily: vars.font.medium, fontSize: 10});
export const messageLine = style({display: "flex", maxWidth: "100%", alignItems: "flex-end", gap: 6});
export const myMessageLine = style([messageLine, {flexDirection: "row-reverse"}]);
export const bubble = style({padding: "10px 14px", borderRadius: 14, background: "#f1f1f1", color: vars.color.black, fontFamily: vars.font.body, fontSize: 14, lineHeight: "20px", whiteSpace: "pre-wrap", overflowWrap: "anywhere"});
export const myBubble = style([bubble, {background: "#e6f7ee", color: vars.color.brandText}]);
export const messageMeta = style({display: "flex", marginBottom: 2, flexDirection: "column", alignItems: "flex-start", gap: 1});
export const myMessageMeta = style([messageMeta, {alignItems: "flex-end"}]);
export const unreadCount = style({color: vars.color.brandText, fontFamily: vars.font.semibold, fontSize: 10, lineHeight: "12px", whiteSpace: "nowrap"});
export const time = style({color: vars.color.gray500, fontFamily: vars.font.body, fontSize: 10, lineHeight: "12px", whiteSpace: "nowrap"});
export const empty = style({margin: "140px 0", color: vars.color.gray600, fontFamily: vars.font.body, fontSize: 14, textAlign: "center"});

export const composer = style({
  display: "flex",
  width: "calc(100% + 32px)",
  minWidth: 0,
  margin: "auto -16px 0",
  padding: "10px 16px",
  boxSizing: "border-box",
  flexShrink: 0,
  alignItems: "center",
  gap: 8,
  borderTop: `1px solid ${vars.color.gray300}`,
  background: "#ffffff",
});
export const input = style({
  minWidth: 0,
  minHeight: 40,
  maxHeight: 100,
  flex: 1,
  padding: "9px 14px",
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: 21,
  boxSizing: "border-box",
  color: vars.color.black,
  background: "#ffffff",
  fontFamily: vars.font.body,
  fontSize: 16,
  lineHeight: "20px",
  outline: "none",
  resize: "none",
  selectors: {
    "&::placeholder": {color: vars.color.gray500},
  },
});
export const sendButton = style({
  width: 36,
  minWidth: 36,
  height: 36,
  padding: 0,
  borderColor: vars.color.brandText,
  borderRadius: "50%",
  background: vars.color.brandText,
  color: vars.color.background,
  selectors: {
    "&:hover:not(:disabled)": {background: "#267a56"},
    "&:disabled": {borderColor: vars.color.brandText, background: vars.color.brandText, color: vars.color.background, opacity: 0.45},
  },
});
export const sendIcon = style({fontFamily: vars.font.semibold, fontSize: 16, lineHeight: 1});
export const srOnly = style({position: "absolute", width: 1, height: 1, padding: 0, margin: -1, overflow: "hidden", clip: "rect(0, 0, 0, 0)", whiteSpace: "nowrap", border: 0});

globalStyle(`html[data-club-chat-input-focused] ${page}`, {
  height: `calc(var(--app-viewport-height, 100dvh) - ${vars.headerHeight})`,
});
