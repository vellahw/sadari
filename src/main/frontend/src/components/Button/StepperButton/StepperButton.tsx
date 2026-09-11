import type {ChangeEventHandler, MouseEventHandler} from "react";
import * as styles from "./StepperButton.css";

/**
 * fileName       : StepperButton
 * author         : Hanwon.Jang
 * date           : 2026-09-11
 * description    : 스태퍼 버튼 컴포넌트
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-09-11        Hanwon.Jang    최초 생성
 */

type StepperButtonProps = {
  minusAriaLabel: string;
  plusAriaLabel: string;
  minusOnClick: MouseEventHandler<HTMLButtonElement>;
  plusOnClick: MouseEventHandler<HTMLButtonElement>;
  inputId: string;
  inputValue: string;
  inputPlaceholder: string;
  inputOnChange: ChangeEventHandler<HTMLInputElement>;
};

const StepperButton = ({
  minusAriaLabel,
  plusAriaLabel,
  minusOnClick,
  plusOnClick,
  inputId,
  inputValue,
  inputPlaceholder,
  inputOnChange,
}: StepperButtonProps) => {

  return (
    <div className={styles.goalStepper}>
      <button
        className={`${styles.goalStepperButton} ${styles.goalStepperDecreaseButton}`}
        type="button"
        aria-label={minusAriaLabel}
        onClick={minusOnClick}
      >
        <svg width="14" height="2" viewBox="0 0 14 2" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M0.850098 0.849609H12.8501" stroke="#293038" strokeWidth="1.7" strokeLinecap="round"/>
        </svg>
      </button>
      <input
        id={inputId}
        className={styles.goalInput}
        inputMode="numeric"
        value={inputValue}
        placeholder={inputPlaceholder}
        onChange={inputOnChange}
      />
      <button
        className={`${styles.goalStepperButton} ${styles.goalStepperIncreaseButton}`}
        type="button"
        aria-label={plusAriaLabel}
        onClick={plusOnClick}
      >
        <svg width="14" height="13" viewBox="0 0 14 13" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path
            d="M13 7.42857H8V12.0714C8 12.3177 7.89464 12.5539 7.70711 12.728C7.51957 12.9022 7.26522 13 7 13C6.73478 13 6.48043 12.9022 6.29289 12.728C6.10536 12.5539 6 12.3177 6 12.0714V7.42857H1C0.734784 7.42857 0.48043 7.33074 0.292893 7.1566C0.105357 6.98246 0 6.74627 0 6.5C0 6.25373 0.105357 6.01754 0.292893 5.8434C0.48043 5.66926 0.734784 5.57143 1 5.57143H6V0.928571C6 0.682299 6.10536 0.446113 6.29289 0.271972C6.48043 0.0978311 6.73478 0 7 0C7.26522 0 7.51957 0.0978311 7.70711 0.271972C7.89464 0.446113 8 0.682299 8 0.928571V5.57143H13C13.2652 5.57143 13.5196 5.66926 13.7071 5.8434C13.8946 6.01754 14 6.25373 14 6.5C14 6.74627 13.8946 6.98246 13.7071 7.1566C13.5196 7.33074 13.2652 7.42857 13 7.42857Z"
            fill="#293038"/>
        </svg>
      </button>
    </div>
  );
};

export default StepperButton;
