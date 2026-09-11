import {style} from "@vanilla-extract/css";
import {vars} from "@/app/styles/tokens.css.ts";

export const goalStepper = style({
  width: "194px",
  height: "42px",
  flexShrink: 0,
  boxSizing: "border-box",
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: "8px",
  backgroundColor: "#fff",
  overflow: "hidden",
  display: "grid",
  gridTemplateColumns: "40px minmax(0, 1fr) 40px",
  alignItems: "stretch",
  transition: "border-color 160ms ease, background-color 160ms ease",
  "@media": {
    "screen and (max-width: 400px)": { width: "100%" },
  },
});

export const goalStepperButton = style({
  border: 0,
  backgroundColor: vars.color.gray100,
  color: "#555555",
  fontFamily: vars.font.heading,
  fontSize: "18px",
  lineHeight: 1,
  cursor: "pointer",
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  transition: "background-color 160ms ease, color 160ms ease",
});

export const goalStepperDecreaseButton = style({
  selectors: {
    "&:hover": {
      backgroundColor: vars.color.gray200,
      color: vars.color.black,
    },
  },
});

export const goalStepperIncreaseButton = style({
  selectors: {
    "&:hover": {
      backgroundColor: vars.color.gray200,
      color: vars.color.black,
    },
  },
});

export const goalInput = style({
  width: "100%",
  minWidth: 0,
  height: "40px",
  padding: "0 4px",
  border: 0,
  borderLeft: `1px solid ${vars.color.gray300}`,
  borderRight: `1px solid ${vars.color.gray300}`,
  backgroundColor: "transparent",
  color: vars.color.black,
  fontFamily: vars.font.medium,
  fontSize: "14px",
  textAlign: "center",
  outline: "none",
  selectors: {
    "&::placeholder": {
      color: vars.color.gray600,
    },
  },
});
