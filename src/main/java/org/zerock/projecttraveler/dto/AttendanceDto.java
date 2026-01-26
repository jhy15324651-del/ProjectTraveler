package org.zerock.projecttraveler.dto;

import lombok.*;
import org.zerock.projecttraveler.entity.AttendanceDaily;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceDto {

    private Long id;
    private LocalDate attendDate;
    private String attendDateFormatted;
    private LocalDateTime createdAt;
    private String createdAtFormatted;

    // 통계 정보
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stats {
        private long totalDays;
        private int consecutiveDays;
        private long thisMonthDays;
        private int attendanceRate;
        private boolean checkedInToday;
    }

    // 월별 출석 현황
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyView {
        private int year;
        private int month;
        private List<DayInfo> days;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayInfo {
        private int day;
        private boolean attended;
        private boolean today;
        private boolean future;
    }

    public static AttendanceDto from(AttendanceDaily attendance) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        return AttendanceDto.builder()
                .id(attendance.getId())
                .attendDate(attendance.getAttendDate())
                .attendDateFormatted(attendance.getAttendDate().format(dateFormatter))
                .createdAt(attendance.getCreatedAt())
                .createdAtFormatted(attendance.getCreatedAt().format(timeFormatter))
                .build();
    }
}
