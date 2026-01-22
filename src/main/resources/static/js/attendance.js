// 출석체크 페이지 JavaScript

// 출석 데이터 (실제로는 서버에서 가져와야 함)
let attendanceData = {
    totalDays: 24,
    consecutiveDays: 5,
    thisMonth: 18,
    lastCheckIn: null,
    checkedDates: [] // 출석한 날짜 배열
};

// 페이지 로드 시 실행
window.addEventListener('DOMContentLoaded', function() {
    loadAttendanceData();
    generateCalendar();
    generateAttendanceHistory();
    checkTodayAttendance();
});

// 로컬 스토리지에서 출석 데이터 로드
function loadAttendanceData() {
    const savedData = localStorage.getItem('attendanceData');
    if (savedData) {
        attendanceData = JSON.parse(savedData);
    }
    
    // 통계 업데이트
    document.getElementById('totalDays').textContent = attendanceData.totalDays;
    document.getElementById('consecutiveDays').textContent = attendanceData.consecutiveDays;
    document.getElementById('thisMonth').textContent = attendanceData.thisMonth;
}

// 출석 데이터 저장
function saveAttendanceData() {
    localStorage.setItem('attendanceData', JSON.stringify(attendanceData));
}

// 오늘 이미 출석했는지 확인
function checkTodayAttendance() {
    const today = new Date().toDateString();
    const lastCheckIn = attendanceData.lastCheckIn;
    
    if (lastCheckIn === today) {
        document.getElementById('checkInBtn').disabled = true;
        document.getElementById('checkInBtn').textContent = '출석 완료 ✓';
        document.getElementById('checkInBtn').style.background = '#95A5A6';
        document.getElementById('checkInMessage').textContent = '오늘은 이미 출석하셨습니다!';
        document.getElementById('checkInMessage').style.color = 'var(--primary-red)';
    }
}

// 출석하기
function checkAttendance() {
    const today = new Date();
    const todayStr = today.toDateString();
    
    // 이미 출석했는지 확인
    if (attendanceData.lastCheckIn === todayStr) {
        alert('오늘은 이미 출석하셨습니다!');
        return;
    }
    
    // 연속 출석일 계산
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);
    const yesterdayStr = yesterday.toDateString();
    
    if (attendanceData.lastCheckIn === yesterdayStr) {
        attendanceData.consecutiveDays++;
    } else if (attendanceData.lastCheckIn !== null) {
        attendanceData.consecutiveDays = 1;
    } else {
        attendanceData.consecutiveDays = 1;
    }
    
    // 출석 정보 업데이트
    attendanceData.totalDays++;
    attendanceData.thisMonth++;
    attendanceData.lastCheckIn = todayStr;
    
    // 출석한 날짜 추가
    if (!attendanceData.checkedDates) {
        attendanceData.checkedDates = [];
    }
    attendanceData.checkedDates.push(todayStr);
    
    // 저장
    saveAttendanceData();
    
    // UI 업데이트
    loadAttendanceData();
    generateCalendar();
    generateAttendanceHistory();
    
    // 출석 완료 메시지
    document.getElementById('checkInBtn').disabled = true;
    document.getElementById('checkInBtn').textContent = '출석 완료 ✓';
    document.getElementById('checkInBtn').style.background = '#95A5A6';
    document.getElementById('checkInMessage').textContent = `축하합니다! 연속 ${attendanceData.consecutiveDays}일 출석 중입니다! 🎉`;
    document.getElementById('checkInMessage').style.color = 'var(--primary-red)';
}

// 캘린더 생성
function generateCalendar() {
    const calendar = document.getElementById('calendar');
    calendar.innerHTML = '';
    
    const today = new Date();
    const year = today.getFullYear();
    const month = today.getMonth();
    
    // 요일 헤더
    const weekdays = ['일', '월', '화', '수', '목', '금', '토'];
    weekdays.forEach(day => {
        const dayHeader = document.createElement('div');
        dayHeader.style.cssText = 'text-align: center; font-weight: 600; padding: 10px; color: var(--dark-gray);';
        if (day === '일') dayHeader.style.color = 'var(--primary-red)';
        if (day === '토') dayHeader.style.color = '#4A90E2';
        dayHeader.textContent = day;
        calendar.appendChild(dayHeader);
    });
    
    // 이번 달 첫 날과 마지막 날
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    
    // 첫 날 이전 빈 칸
    for (let i = 0; i < firstDay.getDay(); i++) {
        const emptyCell = document.createElement('div');
        calendar.appendChild(emptyCell);
    }
    
    // 날짜 생성
    for (let day = 1; day <= lastDay.getDate(); day++) {
        const dateCell = document.createElement('div');
        const currentDate = new Date(year, month, day);
        const dateStr = currentDate.toDateString();
        
        // 출석한 날인지 확인
        const isChecked = attendanceData.checkedDates && attendanceData.checkedDates.includes(dateStr);
        const isPast = currentDate < new Date(today.getFullYear(), today.getMonth(), today.getDate());
        const isToday = currentDate.toDateString() === today.toDateString();
        
        let bgColor = 'var(--white)';
        let borderColor = 'var(--border-gray)';
        let textColor = 'var(--dark-gray)';
        
        if (isChecked) {
            bgColor = 'var(--primary-red)';
            textColor = 'var(--white)';
        } else if (isPast) {
            bgColor = 'var(--border-gray)';
            textColor = '#999';
        }
        
        if (isToday) {
            borderColor = 'var(--primary-red)';
        }
        
        dateCell.style.cssText = `
            text-align: center;
            padding: 15px;
            background: ${bgColor};
            border: 2px solid ${borderColor};
            border-radius: 8px;
            color: ${textColor};
            font-weight: 600;
        `;
        
        dateCell.textContent = day;
        calendar.appendChild(dateCell);
    }
}

// 출석 히스토리 생성
function generateAttendanceHistory() {
    const tbody = document.getElementById('attendanceHistory');
    tbody.innerHTML = '';
    
    // 최근 출석 기록 (임시 데이터)
    const history = [];
    if (attendanceData.checkedDates && attendanceData.checkedDates.length > 0) {
        // 최근 10개만 표시
        const recentDates = attendanceData.checkedDates.slice(-10).reverse();
        recentDates.forEach((dateStr, index) => {
            const date = new Date(dateStr);
            const row = tbody.insertRow();
            
            row.innerHTML = `
                <td>${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일</td>
                <td>${date.getHours() || 9}:${String(date.getMinutes() || 30).padStart(2, '0')}</td>
                <td>${attendanceData.consecutiveDays - index}일</td>
                <td><span class="badge badge-complete">출석 완료</span></td>
            `;
        });
    } else {
        // 기본 데이터
        const today = new Date();
        for (let i = 0; i < 5; i++) {
            const date = new Date(today);
            date.setDate(date.getDate() - i);
            
            const row = tbody.insertRow();
            row.innerHTML = `
                <td>${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일</td>
                <td>09:${String(Math.floor(Math.random() * 60)).padStart(2, '0')}</td>
                <td>${5 - i}일</td>
                <td><span class="badge badge-complete">출석 완료</span></td>
            `;
        }
    }
}
