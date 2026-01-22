// 온라인학습 페이지 JavaScript

// 강좌 필터링
function filterCourses(category) {
    const cards = document.querySelectorAll('.course-card');
    const buttons = {
        'all': document.getElementById('btn-all'),
        'language': document.getElementById('btn-language'),
        'culture': document.getElementById('btn-culture'),
        'travel': document.getElementById('btn-travel')
    };
    
    // 모든 버튼 스타일 초기화
    Object.values(buttons).forEach(btn => {
        btn.className = 'btn btn-secondary';
    });
    
    // 선택된 버튼 활성화
    buttons[category].className = 'btn btn-primary';
    
    // 카드 필터링
    cards.forEach(card => {
        if (category === 'all') {
            card.style.display = 'block';
        } else {
            if (card.dataset.category === category) {
                card.style.display = 'block';
            } else {
                card.style.display = 'none';
            }
        }
    });
}

// 강좌 시작
function startCourse(courseName) {
    alert(`"${courseName}" 강좌를 시작합니다!\n\n실제 프로젝트에서는 강의 영상이나 학습 콘텐츠로 이동합니다.`);
}
