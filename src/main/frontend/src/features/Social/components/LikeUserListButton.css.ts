import { keyframes, style } from "@vanilla-extract/css";
import { vars } from "@/app/styles/tokens.css";

export const countButton = style({
  minWidth: 0,
  height: "24px",
  padding: 0,
  border: 0,
  backgroundColor: "transparent",
  lineHeight: 1,
  cursor: "pointer",
  selectors: {
    "&:focus-visible": { outline: `2px solid ${vars.color.brand}`, outlineOffset: 2 },
  },
});

export const overlay = style({
  position: "fixed",
  inset: 0,
  width: "100vw",
  height: "100dvh",
  zIndex: 3100,
  padding: "0 16px",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  overflow: "hidden",
  overscrollBehavior: "contain",
  backgroundColor: "rgba(0, 0, 0, 0.34)",
  boxSizing: "border-box",
  animation: `${keyframes({
    from: { opacity: 0 },
    to: { opacity: 1 },
  })} 160ms ease-out`,
});

export const modal = style({
  width: "min(460px, 100%)",
  maxHeight: "calc(100dvh - 48px)",
  padding: "20px 18px 18px",
  overflow: "hidden",
  borderRadius: "22px",
  backgroundColor: vars.color.background,
  boxShadow: "0 22px 58px rgba(0, 0, 0, 0.24)",
  animation: `${keyframes({
    from: { opacity: 0, transform: "translateY(8px)" },
    to: { opacity: 1, transform: "translateY(0)" },
  })} 180ms ease-out`,
});

export const header = style({
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  gap: "12px",
});

export const title = style({
  margin: 0,
  color: vars.color.black,
  fontFamily: vars.font.heading,
  fontSize: "18px",
  lineHeight: 1.35,
});

export const list = style({
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
    "&::-webkit-scrollbar": { width: "6px" },
    "&::-webkit-scrollbar-track": { backgroundColor: "transparent" },
    "&::-webkit-scrollbar-thumb": {
      borderRadius: "999px",
      backgroundColor: "transparent",
      transition: "background-color 220ms ease",
    },
  },
});

export const listScrolling = style([
  list,
  {
    scrollbarColor: "rgba(0, 0, 0, 0.24) transparent",
    selectors: {
      "&::-webkit-scrollbar-thumb": { backgroundColor: "rgba(0, 0, 0, 0.24)" },
    },
  },
]);

export const item = style({
  width: "100%",
  minHeight: "58px",
  padding: "8px 0",
  border: 0,
  borderBottom: `1px solid ${vars.color.gray300}`,
  backgroundColor: "transparent",
  display: "grid",
  gridTemplateColumns: "minmax(0, 1fr) auto",
  alignItems: "center",
  gap: "10px",
  textAlign: "left",
  selectors: { "&:last-child": { borderBottom: 0 } },
});

export const profileButton = style({
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

export const avatar = style({
  width: "42px",
  height: "42px",
  borderRadius: "50%",
  objectFit: "cover",
  backgroundColor: vars.color.gray100,
});

export const text = style({ minWidth: 0, display: "flex", flexDirection: "column", gap: "3px" });
export const name = style({
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
  color: vars.color.black,
  fontFamily: vars.font.semibold,
  fontSize: "14px",
  lineHeight: 1.25,
});
export const intro = style({
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: "12px",
  lineHeight: 1.35,
});

export const statusButton = style({
  minWidth: "58px",
  minHeight: "30px",
  padding: "0 10px",
  borderRadius: "999px",
  backgroundColor: vars.color.brandBg,
  color: vars.color.brandText,
  fontFamily: vars.font.semibold,
  fontSize: "12px",
  cursor: "pointer",
  selectors: {
    "&:hover:not(:disabled)": { backgroundColor: vars.color.gray100 },
    "&[data-follow-status='팔로잉']": { color: "#2f9e44" },
    "&[data-follow-status='맞팔로우']": { color: "#2f9e44" },
    "&[data-follow-status='친구']": {
      color: "#2563eb",
      backgroundColor: "#eaf4ff",
    },
    "&[data-follow-status='친구']:hover:not(:disabled)": { backgroundColor: "#dbeafe" },
    "&:disabled": { cursor: "default", opacity: 0.62 },
  },
});

export const empty = style({
  margin: "12px 0 4px",
  padding: "24px 0",
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: "14px",
  lineHeight: 1.5,
  textAlign: "center",
});
