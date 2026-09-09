import { useLayoutEffect, useRef, useState } from "react";
import * as styles from "./ReportListView.css";

type AnimatedReportContentProps = {
  content: string;
  expanded: boolean;
  previewHeight?: number;
};

/**
 * 독후감 미리보기와 실제 본문 높이 사이의 펼침 표시
 *
 * @author HanWon.Jang
 * @param props 본문과 펼침 상태 및 화면별 미리보기 높이
 * @return 높이를 제한한 독후감 본문 영역
 */
const AnimatedReportContent = ({ content, expanded, previewHeight = 70 }: AnimatedReportContentProps) => {
  const contentRef = useRef<HTMLParagraphElement>(null);
  const [contentHeight, setContentHeight] = useState(previewHeight);

  useLayoutEffect(() => {
    const contentElement = contentRef.current;
    if (!contentElement) return undefined;
    const updateHeight = (): void => setContentHeight(Math.max(contentElement.scrollHeight, previewHeight));
    updateHeight();
    const observer = new ResizeObserver(updateHeight);
    observer.observe(contentElement);
    // 본문 크기 감시가 더 이상 필요하지 않으면 관찰을 종료함
    return () => observer.disconnect();
  }, [content, previewHeight]);

  // 측정한 실제 높이를 사용해 짧은 미리보기와 전체 본문 사이를 전환함
  return (
    <div
      className={styles.reportContentWrap}
      style={{ maxHeight: `${expanded ? contentHeight : previewHeight}px` }}
    >
      <p className={styles.reportContent} ref={contentRef}>{content}</p>
    </div>
  );
};

export default AnimatedReportContent;
