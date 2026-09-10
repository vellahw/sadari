import { message } from "@/app/messages/message";
import type { LikeTargetType } from "@/features/Social/api/socialApi";
import LikeUserListButton from "./LikeUserListButton";
import * as styles from "./ReactionButtons.css";

type LikeButtonProps = {
  tagtType: LikeTargetType;
  tagtNumb: number;
  liked: boolean;
  countLabel: string | number;
  disabled?: boolean;
  onClick: () => void;
};

type CommentButtonProps = {
  count: number | undefined;
  onClick: () => void;
};

/**
 * 좋아요 전환 버튼과 좋아요 사용자 목록 버튼 표시
 *
 * @author HanWon.Jang
 * @param props 대상 식별값과 좋아요 상태 및 전환 처리
 * @return 좋아요 아이콘과 사용자 수 영역
 */
export const LikeButton = (props: LikeButtonProps) => {
  // 현재 좋아요 상태에 대응하는 하트 아이콘
  const iconSource = props.liked ? "/img/icons/icon-heart-fill.svg" : "/img/icons/icon-heart.svg";

  // 좋아요 전환과 사용자 목록을 별도 버튼으로 제공하는 영역
  return (
    <div className={styles.likeButtonContainer}>
      {/* 좋아요 전환 영역 */}
      <button
        className={styles.likeButton}
        type="button"
        aria-label={/* "좋아요" */ message("frontend.common.like")}
        aria-pressed={props.liked}
        disabled={props.disabled}
        onClick={props.onClick}
      >
        <img src={iconSource} alt="" />
      </button>
      {/* 좋아요 사용자 목록 영역 */}
      <LikeUserListButton
        className={styles.likeCount}
        tagtType={props.tagtType}
        tagtNumb={props.tagtNumb}
        countLabel={props.countLabel}
      />
    </div>
  );
};

/**
 * 댓글 수와 댓글 목록 열기 버튼 표시
 *
 * @author HanWon.Jang
 * @param props 댓글 수와 목록 열기 처리
 * @return 댓글 아이콘과 댓글 수 버튼
 */
export const CommentButton = (props: CommentButtonProps) => {
  // 댓글 목록을 여는 아이콘과 댓글 수 영역
  return (
    <div>
      {/* 댓글 목록 열기 영역 */}
      <button
        className={styles.commentIndicator}
        type="button"
        aria-label={/* "댓글" */ message("frontend.common.comment")}
        onClick={props.onClick}
      >
        <img className={styles.commentIcon} src="/img/icons/icon-comment.svg" alt="" />
        <span className={styles.commentCount}>{props.count}</span>
      </button>
    </div>
  );
};
