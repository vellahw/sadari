/**
 * fileName       : ClubMemberManagementPage.css
 * author         : HanWon.Jang
 * date           : 2026-08-14
 * description    : 멤버와 가입 신청 관리 화면 및 모달 스타일을 정의함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-08-14        Hanwon.Jang        최초 생성
 * 2026-08-24        HanWon.Jang        퇴장 버튼과 사유 모달 스타일 추가
 */
import { vars } from "@/app/styles/tokens.css";
import { style } from "@vanilla-extract/css";

export const page = style({
  display: "flex",
  flexDirection: "column",
  gap: 38,
  width: "100%",
  maxWidth: 600,
  margin: "0 auto",
  minHeight: `calc(100vh - ${vars.headerHeight} - ${vars.navHeight})`,
  paddingTop: 20,
  paddingBottom: 28,
  boxSizing: "border-box",
  background: vars.color.background,
});

export const section = style({
  display: "flex",
  flexDirection: "column",
  gap: 12,
});

export const sectionTitle = style({
  margin: 0,
  color: vars.color.black,
  fontFamily: vars.font.semibold,
  fontSize: 16,
  lineHeight: "20px",
  letterSpacing: "-0.16px",
});

export const cardList = style({
  display: "flex",
  flexDirection: "column",
  gap: 12,
});

export const profileCard = style({
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  width: "100%",
  minHeight: 74,
  padding: 16,
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: 22,
  boxSizing: "border-box",
  background: vars.color.background,
});

export const profileSummary = style({
  display: "flex",
  minWidth: 0,
  alignItems: "center",
  gap: 10,
  borderRadius: 12,
  color: "inherit",
  textDecoration: "none",
  transition: "filter 160ms ease",
  selectors: {
    "&:hover": {
      filter: "brightness(0.82)",
    },
    "&:focus-visible": {
      outline: `2px solid ${vars.color.brandText}`,
      outlineOffset: 3,
    },
  },
});

export const avatar = style({
  width: 40,
  height: 40,
  flexShrink: 0,
  borderRadius: "50%",
  background: vars.color.gray100,
  objectFit: "cover",
});

export const profileName = style({
  overflow: "hidden",
  color: vars.color.black,
  fontFamily: vars.font.semibold,
  fontSize: 16,
  lineHeight: "20px",
  letterSpacing: "-0.16px",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
});

export const answerButton = style({
  display: "inline-flex",
  flexShrink: 0,
  alignItems: "center",
  justifyContent: "center",
  gap: 4,
  marginLeft: 10,
  padding: "6px 14px",
  border: 0,
  borderRadius: 200,
  background: vars.color.brandBg,
  color: vars.color.brandText,
  fontFamily: vars.font.medium,
  fontSize: 14,
  lineHeight: "14px",
  letterSpacing: "-0.14px",
  cursor: "pointer",
  transition: "filter 160ms ease",
  selectors: {
    "&:hover": {
      filter: "brightness(0.96)",
    },
    "&:focus-visible": {
      outline: "2px solid #78b991",
      outlineOffset: 2,
    },
  },
});

export const answerChevron = style({
  width: 12,
  height: 12,
  objectFit: "contain",
});

export const exitButton = style({
  flexShrink: 0,
  marginLeft: 10,
  borderRadius: "999px",
  border:"none",
  padding: "6px 14px",
  height: "fit-content"
});

export const emptyText = style({
  margin: 0,
  padding: "18px 0",
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: 14,
  lineHeight: "20px",
});

export const managementMenu = style({
  display: "flex",
  flexDirection: "column",
  gap: 8,
});

export const menuButton = style({
  display: "flex",
  width: "100%",
  minHeight: 62,
  alignItems: "center",
  justifyContent: "space-between",
  padding: "10px 0",
  border: 0,
  boxSizing: "border-box",
  background: vars.color.background,
  textAlign: "left",
  cursor: "pointer",
  textDecoration: "none",
  selectors: {
    "&:focus-visible": {
      outline: "2px solid #78b991",
      outlineOffset: 2,
    },
  },
});

export const menuText = style({
  display: "flex",
  minWidth: 0,
  flexDirection: "column",
  alignItems: "flex-start",
  gap: 4,
});

export const menuTitle = style({
  color: vars.color.black,
  fontFamily: vars.font.semibold,
  fontSize: 16,
  lineHeight: "20px",
});

export const menuDescription = style({
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: 12,
  lineHeight: "16px",
  letterSpacing: "-0.12px",
});

export const menuChevron = style({
  width: 18,
  height: 18,
  flexShrink: 0,
  objectFit: "contain",
});

export const restrictionButton = style([
  menuButton,
  {
    minHeight: 40,
  },
]);

export const restrictionTitle = style({
  color: vars.color.negativeText,
  fontFamily: vars.font.semibold,
  fontSize: 16,
  lineHeight: "20px",
});

export const overlay = style({
  position: "fixed",
  zIndex: 999,
  inset: 0,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  padding: 24,
  boxSizing: "border-box",
  background: "rgba(0, 0, 0, 0.34)",
});

export const inviteOverlay = style([
  overlay,
  {
    padding: 0,
  },
]);

// 답변보기 모달
export const modal = style({
  display: "flex",
  width: "100%",
  maxWidth: 600,
  maxHeight: "calc(100dvh - 48px)",
  flexDirection: "column",
  gap: 28,
  overflowY: "auto",
  padding: 20,
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: 22,
  boxSizing: "border-box",
  background: vars.color.background,
});

export const inviteModal = style([
  modal,
  {
    maxHeight: "100dvh",
    gap: 20,
    padding: 20,
    border: 0,
    margin: "0 16px"
  },
]);

export const modalHeader = style({
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  gap: 16,
});

export const modalTitle = style({
  margin: 0,
  color: vars.color.black,
  fontFamily: vars.font.heading,
  fontSize: 18,
  lineHeight: "24px",
});

export const inviteModalTitle = style([
  modalTitle,
  {
    fontFamily: vars.font.heading,
    lineHeight: "normal",
  },
]);

export const closeButton = style({
  display: "inline-flex",
  width: 36,
  height: 36,
  flexShrink: 0,
  alignItems: "center",
  justifyContent: "center",
  padding: 0,
  border: 0,
  borderRadius: "50%",
  background: vars.color.background,
  cursor: "pointer",
  selectors: {
    "&:hover": {
      background: vars.color.gray100,
    },
    "&:focus-visible": {
      outline: "2px solid #78b991",
      outlineOffset: 2,
    },
  },
});

export const inviteCloseButton = style([
  closeButton,
  {
    width: 26,
    height: 26,
  },
]);

export const closeIcon = style({
  width: 14,
  height: 14,
});

export const answerList = style({
  display: "flex",
  flexDirection: "column",
  gap: 16,
});

export const answerItem = style({
  display: "flex",
  flexDirection: "column",
  gap: 12,
  padding: 16,
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: 14,
  background: "#ffffff",
});

export const questionText = style({
  fontFamily: vars.font.semibold,
  fontSize: 16,
  lineHeight: "24px",
});

export const answerText = style({
  margin: 0,
  padding: "14px 16px",
  color: vars.color.black,
  fontFamily: vars.font.medium,
  fontSize: 16,
  lineHeight: "24px",
  borderRadius: 10,
  background: vars.color.gray100,
  whiteSpace: "pre-wrap",
});

export const modalActions = style({
  display: "flex",
  gap: 8,
  marginTop: 10
});

export const exitDescription = style({
  margin: 0,
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: 14,
  lineHeight: "20px",
  whiteSpace: "pre-line",
});

export const exitReasonLabel = style({
  color: vars.color.black,
  fontFamily: vars.font.semibold,
  fontSize: 14,
  lineHeight: "20px",
});

export const exitReasonInput = style({
  width: "100%",
  minHeight: 120,
  padding: 14,
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: 12,
  boxSizing: "border-box",
  color: vars.color.black,
  background: vars.color.background,
  fontFamily: vars.font.body,
  fontSize: 14,
  lineHeight: "20px",
  resize: "vertical",
  selectors: {
    "&:focus-visible": {
      outline: "2px solid #78b991",
      outlineOffset: 2,
    },
  },
});

export const candidateList = style({
  display: "flex",
  flexDirection: "column",
  gap: 10,
});

export const invitationCard = style({
  display: "flex",
  width: "100%",
  minHeight: 74,
  alignItems: "center",
  gap: 10,
  padding: 16,
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: 22,
  boxSizing: "border-box",
  background: vars.color.background,
});

export const candidateInfo = style({
  display: "flex",
  minWidth: 0,
  flex: 1,
  flexDirection: "column",
  gap: 2,
});

export const invitationInterest = style({
  overflow: "hidden",
  color: vars.color.gray600,
  fontFamily: vars.font.body,
  fontSize: 12,
  lineHeight: "12px",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
});

export const inviteActionButton = style({
  minWidth: 76,
  height: 26,
  flexShrink: 0,
  padding: "0 14px",
  border: 0,
  borderRadius: 200,
  background: vars.color.brandBg,
  color: vars.color.brandText,
  lineHeight: "14px",
  selectors: {
    "&:hover:not(:disabled)": {
      background: vars.color.brandBg,
      filter: "brightness(0.96)",
    },
  },
});

export const cancelInviteButton = style({
  minWidth: 76,
  height: 26,
  flexShrink: 0,
  padding: "0 14px",
  border: `1px solid ${vars.color.gray300}`,
  borderRadius: 200,
  background: vars.color.background,
  color: vars.color.gray600,
  lineHeight: "14px",
});
