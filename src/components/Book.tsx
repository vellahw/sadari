import React, { useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';

interface BookProps {
  index: number;
  title: string;
  author: string;
};

function Book({ index, title, author }: BookProps) {

  const randomBrightColor = () => {
    const calc = (Math.random() * 24, 10) * 15;
    const colors = `hsl(${calc}, 16%, 57%)`;
    
    return colors;
  };

  return (
    <li className="book flex-center" data-index={index}>
      <Link className="wh-100 flex-center dir-column" to={`/detail/${index}`}>
        <p className="title">{title}</p>
        <p className="author">{author}</p>
      </Link>
    </li>
  );
}

export default Book;