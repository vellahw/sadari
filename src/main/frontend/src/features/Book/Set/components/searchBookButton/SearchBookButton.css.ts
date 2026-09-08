/**
 * fileName       : SearchBook.css
 * author         : HanWon.Jang
 * date           : 2026-03-22
 * description    : 책 검색하기 버튼 CSS
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-03-22       HanWon.Jang       최초 생성
 */
import { style } from "@vanilla-extract/css";
import { vars } from "@/app/styles/tokens.css";

export const searchBtn = style({
  width: "112px",
  height: "166px",
  borderRadius: "12px",
  backgroundColor: "#ffffff",
  border: `1px solid ${vars.color.gray300}`,
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  justifyContent: "center",
  textDecoration: "none",
  transition: "background-color 160ms ease",

  selectors: {
    "&:hover": {
      backgroundColor: vars.color.gray100
    }
  }
});

export const searchBtnText = style({
  color: vars.color.gray600,
  fontSize: vars.fontSize.body,
  marginTop: "14px",
  marginBottom: 0,
  textAlign: "center"
});
