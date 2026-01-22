// 나의 강의실 페이지 JavaScript

// 강좌 이어하기
function continueCourse(courseName) {
    alert(`"${courseName}" 강좌를 이어서 학습합니다!\n\n실제 프로젝트에서는 마지막 학습 위치로 이동합니다.`);
    // 실제로는 강의 페이지로 이동
    // window.location.href = `course-player.html?course=${encodeURIComponent(courseName)}`;
}

// 강좌 복습하기
function reviewCourse(courseName) {
    alert(`"${courseName}" 강좌를 복습합니다!\n\n실제 프로젝트에서는 강의 처음부터 다시 시작합니다.`);
    // 실제로는 강의 페이지로 이동
    // window.location.href = `course-player.html?course=${encodeURIComponent(courseName)}&review=true`;
}
