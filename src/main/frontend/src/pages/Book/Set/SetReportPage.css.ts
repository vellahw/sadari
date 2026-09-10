import { style } from "@vanilla-extract/css";
import { vars } from "@/app/styles/tokens.css";

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
      height: "var(--book-bg-fade-height, 680px)",
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

export const form = style({
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

export const contentPanel = style({
  display: "flex",
  flexDirection: "column",
  gap: "16px",
  minHeight: "auto",
});

export const recordSection = style({
  position: "relative",
  width: "100%",
  minHeight: "280px",
  padding: "20px 20px 24px",
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: "22px",
  backgroundColor: "#ffffff",
  boxShadow: "0 8px 22px rgba(0, 0, 0, 0.05)",
  boxSizing: "border-box",
});

export const formActions = style({
  width: "100%",
  display: "grid",
  gap: "8px",
  gridTemplateColumns: "1fr 1fr"
});

export const searchBookArea = style({
  minHeight: "360px",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  borderRadius: "14px",
  backgroundColor: "rgb(255, 255, 255)",
  boxShadow: "0 18px 38px rgba(0, 0, 0, 0.18)",
});

export const hiddenInput = style({
  position: "absolute",
  opacity: 0,
  pointerEvents: "none",
});

export const textAreaWrap = style({
  position: "relative",
});

export const textArea = style({
  width: "100%",
  minHeight: "230px",
  resize: "none",
  border: 0,
  outline: 0,
  backgroundColor: "transparent",
  padding: 0,
  fontFamily: vars.font.body,
  fontSize: "14px",
  lineHeight: 1.6,
  color: vars.color.black,
  boxSizing: "border-box",
});

export const counter = style({
  position: "absolute",
  top: "-22px",
  right: "4px",
  fontSize: "11px",
  color: vars.color.gray500,
});

export const switchTrack = style({
  display: "inline-flex",
  alignItems: "center",
  position: "relative",
  flexShrink: 0,
  width: "52px",
  height: "30px",
  borderRadius: "999px",
  backgroundColor: vars.color.gray400,
  cursor: "pointer",
  transition: "background-color 0.18s ease",

  selectors: {
    [`${hiddenInput}:checked + &`]: {
      backgroundColor: vars.color.black,
    },
  },
});

export const switchThumb = style({
  position: "absolute",
  top: "3px",
  left: "3px",
  width: "24px",
  height: "24px",
  borderRadius: "50%",
  backgroundColor: "#ffffff",
  boxShadow: "0 2px 6px rgba(0, 0, 0, 0.18)",
  transition: "transform 0.18s ease",

  selectors: {
    [`${hiddenInput}:checked + ${switchTrack} &`]: {
      transform: "translateX(22px)",
    },
  },
});

export const topBar = style({
  display: "none",
});

export const backButton = style({
  display: "none",
});

export const brand = style({
  display: "none",
});
