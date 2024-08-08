import React from 'react';


interface ButtonProps {
  title: string; // 버튼 안 텍스트
  className: string; // 버튼 클래스명
}

function ComButton({ title, className }: ButtonProps) {
  return (
    <button className={className}>{title}</button>
  );
}

export default ComButton;