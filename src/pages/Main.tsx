import '../css/pages/Main.css';
import ComButton from './../components/ComButton';
import Book from './../components/Book';

function Main() {

  return (
    <div className="wrap wh-100">
      <div className="bg wh-100 el-flex dir-column">
        <div className="w-100">
          <ul className="books-wrap wh-100 el-flex">
            <Book index={1} title="돌이킬 수 있는" author="문목하" />
            <Book index={2} title="동급생" author="히가시..." />
          </ul>
        </div>
        <div className="w-100"></div>
      </div>
      <ComButton title="기록하기" className="record-btn flex-center" />
    </div>
  );
}

export default Main;
