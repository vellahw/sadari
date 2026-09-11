/**
 * fileName       : ActionButton
 * author         : Hanwon.Jang
 * date           : 2026-08-10
 * description    : 모든 화면의 기능 명령 버튼과 선택적 아이콘 위치를 공통 구조로 제공함
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-08-10        Hanwon.Jang    최초 생성
 */

import { clsx } from "clsx";
import type { ButtonHTMLAttributes, ReactNode } from "react";
import * as styles from "./ActionButton.css";

type ActionButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: keyof typeof styles.variant;
  size?: keyof typeof styles.size;
  width?: keyof typeof styles.width;
  icon?: ReactNode;
  iconPosition?: "left" | "right";
};

export function ActionButton({
  variant = "primary",
  size = "md",
  width = "auto",
  icon,
  iconPosition = "left",
  className,
  children,
  type = "button",
  ...buttonProps
}: ActionButtonProps) {
  // 선택한 공통 스타일과 왼쪽 아이콘을 적용한 화면 명령 버튼을 반환함
  return (
    <button
      {...buttonProps}
      className={clsx(
        styles.button,
        styles.variant[variant],
        styles.size[size],
        styles.width[width],
        className,
      )}
      type={type}
    >
      {/* 버튼 텍스트 왼쪽의 선택적 장식 아이콘 영역 */}
      {iconPosition === "left" && (
        <span className={styles.icon} aria-hidden="true">
          {icon}
        </span>
      )}
      {/* 버튼 명령 텍스트 영역 */}
      <span className={styles.label}>{children}</span>
      {/* 버튼 텍스트 오른쪽의 선택적 장식 아이콘 영역 */}
      {iconPosition === "right" && (
        <span className={styles.icon} aria-hidden="true">
          {icon}
        </span>
      )}
    </button>
  );
}
