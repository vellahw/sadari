import React from 'react';
import Main from '../pages/Main';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import '../css/core/Default.css';
import '../css/core/Variable.css';
import Header from 'layout/Header';

function Router() {
  return (
    <BrowserRouter>
      <Header />
      <Routes>
        <Route path="/" element={<Main />} />
      </Routes>
    </BrowserRouter>
  );
}

export default Router;