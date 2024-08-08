import React from 'react';
import { Link } from 'react-router-dom';
import '../css/layout/Header.css';

function Header() {
  return (
    <nav>
      <Link to={'/'} className='logo'>sadari</Link>
    </nav>
  );
}

export default Header;