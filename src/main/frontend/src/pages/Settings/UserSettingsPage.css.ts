import { style } from "@vanilla-extract/css";
import { vars } from "@/app/styles/tokens.css";

export const page = style({
  display: "flex",
  flexDirection: "column",
  width: "100%",
  maxWidth: "600px",
  minHeight: "calc(100svh - 112px)",
  margin: "0 auto",
  padding: "10px 18px 36px",
  boxSizing: "border-box",
  backgroundColor: "#ffffff",
});

export const header = style({ marginBottom: "22px" });

export const pageDescription = style({
  margin: 0,
  color: vars.color.black,
  fontFamily: vars.font.semibold,
  fontSize: "16px",
  lineHeight: 1.55,
});

export const section = style({ marginBottom: "28px" });

export const sectionTitle = style({
  margin: 0,
  padding: "0 0 12px",
  color: vars.color.black,
  fontFamily: vars.font.semibold,
  fontSize: "16px",
});

export const settingRow = style({
  position: "relative",
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  gap: "16px",
  minHeight: "76px",
  padding: "14px 0",
  boxSizing: "border-box",
  borderBottom: `1px solid ${vars.color.gray300}`,
  cursor: "pointer",
  transition: "background-color 160ms ease",
  selectors: {
    "&:hover": { backgroundColor: vars.color.gray100 },
    "&:has(input:disabled)": { cursor: "default", opacity: 0.6 },
    "&:has(input:disabled):hover": { backgroundColor: "transparent" },
    "&:last-child": { borderBottom: 0 },
  },
});

export const settingText = style({ display: "grid", gap: "4px", minWidth: 0 });

export const settingTitle = style({
  color: vars.color.black,
  fontFamily: vars.font.semibold,
  fontSize: "16px",
  lineHeight: 1.4,
});

export const settingDescription = style({ color: vars.color.gray600, fontSize: "14px", lineHeight: 1.5 });

export const switchInput = style({
  position: "absolute",
  width: "1px",
  height: "1px",
  opacity: 0,
});

export const switchTrack = style({
  position: "relative",
  flex: "0 0 auto",
  width: "44px",
  height: "24px",
  borderRadius: "12px",
  backgroundColor: vars.color.gray300,
  transition: "background-color 160ms ease",
  selectors: {
    [`${switchInput}:focus-visible + &`]: { outline: "2px solid #78b991", outlineOffset: "2px" },
    [`${switchInput}:checked + &`]: { backgroundColor: vars.color.brand },
    "&::after": {
      content: "",
      position: "absolute",
      top: "2px",
      left: "2px",
      width: "20px",
      height: "20px",
      borderRadius: "50%",
      backgroundColor: "#ffffff",
      boxShadow: "0 1px 4px rgba(0, 0, 0, 0.2)",
      transition: "transform 160ms ease",
    },
    [`${switchInput}:checked + &::after`]: { transform: "translateX(20px)" },
  },
});

export const languageField = style({
  display: "grid",
  gap: "12px",
});

export const languageSelect = style({ width: "100%" });

export const languageSelectTrigger = style({
  width: "100%",
  height: "48px",
  padding: "0 14px",
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: "12px",
  backgroundColor: "#ffffff",
  selectors: {
    "&:hover": { backgroundColor: vars.color.gray100 },
    "&:focus-visible": { outline: "2px solid #78b991", outlineOffset: "2px" },
  },
});

export const languageOptionList = style({
  right: "auto",
  left: 0,
  width: "100%",
  boxSizing: "border-box",
});

export const saveArea = style({ marginTop: "auto" });
