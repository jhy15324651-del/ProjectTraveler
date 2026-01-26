package org.zerock.projecttraveler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.projecttraveler.dto.AttendanceDto;
import org.zerock.projecttraveler.entity.AttendanceDaily;
import org.zerock.projecttraveler.entity.User;
import org.zerock.projecttraveler.repository.AttendanceDailyRepository;
import org.zerock.projecttraveler.repository.UserRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AttendanceService {

    private final AttendanceDailyRepository attendanceRepository;
    private final UserRepository userRepository;

    /**
     * 출석 체크 (오늘 출석 안 했으면 기록)
     */
    @Transactional
    public boolean checkIn(Long userId) {
        LocalDate today = LocalDate.now();

        if (attendanceRepository.existsByUserIdAndAttendDate(userId, today)) {
            log.debug("이미 출석함: userId={}, date={}", userId, today);
            return false;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        AttendanceDaily attendance = AttendanceDaily.builder()
                .user(user)
                .attendDate(today)
                .build();

        attendanceRepository.save(attendance);
        log.info("출석 체크: userId={}, date={}", userId, today);

        return true;
    }

    /**
     * 출석 터치 (학습 시 자동 출석)
     */
    @Transactional
    public void touchAttendance(Long userId) {
        checkIn(userId);
    }

    /**
     * 오늘 출석 여부
     */
    public boolean hasCheckedInToday(Long userId) {
        return attendanceRepository.existsByUserIdAndAttendDate(userId, LocalDate.now());
    }

    /**
     * 총 출석일 수
     */
    public long getTotalAttendanceDays(Long userId) {
        return attendanceRepository.countByUserId(userId);
    }

    /**
     * 연속 출석일 수 계산
     */
    public int getConsecutiveAttendanceDays(Long userId) {
        List<LocalDate> dates = attendanceRepository.findAttendDatesByUserId(userId);

        if (dates.isEmpty()) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // 오늘이나 어제 출석 기록이 없으면 연속 출석이 끊긴 것
        if (!dates.contains(today) && !dates.contains(yesterday)) {
            return 0;
        }

        // 연속 출석일 계산
        int consecutive = 0;
        LocalDate checkDate = dates.contains(today) ? today : yesterday;

        for (LocalDate date : dates) {
            if (date.equals(checkDate)) {
                consecutive++;
                checkDate = checkDate.minusDays(1);
            } else if (date.isBefore(checkDate)) {
                break;
            }
        }

        return consecutive;
    }

    /**
     * 이번 달 출석일 수
     */
    public long getThisMonthAttendanceDays(Long userId) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();

        return attendanceRepository.findByUserIdAndMonth(userId, startDate, endDate).size();
    }

    /**
     * 출석률 계산 (이번 달 기준)
     */
    public int getAttendanceRate(Long userId) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate today = LocalDate.now();

        // 이번 달 시작일부터 오늘까지의 일수
        int totalDays = (int) ChronoUnit.DAYS.between(currentMonth.atDay(1), today) + 1;
        long attendedDays = getThisMonthAttendanceDays(userId);

        return totalDays > 0 ? (int) ((attendedDays * 100) / totalDays) : 0;
    }

    /**
     * 출석 통계
     */
    public AttendanceDto.Stats getStats(Long userId) {
        return AttendanceDto.Stats.builder()
                .totalDays(getTotalAttendanceDays(userId))
                .consecutiveDays(getConsecutiveAttendanceDays(userId))
                .thisMonthDays(getThisMonthAttendanceDays(userId))
                .attendanceRate(getAttendanceRate(userId))
                .checkedInToday(hasCheckedInToday(userId))
                .build();
    }

    /**
     * 월별 출석 현황
     */
    public AttendanceDto.MonthlyView getMonthlyView(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        LocalDate today = LocalDate.now();

        List<AttendanceDaily> attendances = attendanceRepository.findByUserIdAndMonth(userId, startDate, endDate);
        Set<Integer> attendedDays = attendances.stream()
                .map(a -> a.getAttendDate().getDayOfMonth())
                .collect(Collectors.toSet());

        List<AttendanceDto.DayInfo> days = new ArrayList<>();
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            days.add(AttendanceDto.DayInfo.builder()
                    .day(day)
                    .attended(attendedDays.contains(day))
                    .today(date.equals(today))
                    .future(date.isAfter(today))
                    .build());
        }

        return AttendanceDto.MonthlyView.builder()
                .year(year)
                .month(month)
                .days(days)
                .build();
    }

    /**
     * 최근 출석 기록
     */
    public List<AttendanceDto> getRecentHistory(Long userId, int days) {
        LocalDate fromDate = LocalDate.now().minusDays(days);
        return attendanceRepository.findRecentByUserId(userId, fromDate)
                .stream()
                .map(AttendanceDto::from)
                .collect(Collectors.toList());
    }
}
