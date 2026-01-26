package org.zerock.projecttraveler.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyLearningSummaryDto {

    // 수강 중인 강좌 수
    private long inProgressCourseCount;

    // 완료한 강좌 수
    private long completedCourseCount;

    // 평균 진도율 (%)
    private int averageProgressPercent;

    // 연속 출석일
    private int consecutiveAttendanceDays;

    // 총 학습 시간 (초)
    private long totalLearningTimeSec;

    // 총 학습 시간 (포맷: "45시간 30분")
    private String totalLearningTimeFormatted;

    // 총 출석일
    private long totalAttendanceDays;

    // 이번 달 출석일
    private long thisMonthAttendanceDays;

    // 출석률 (%)
    private int attendanceRate;

    // 총 학습 시간을 포맷팅하는 헬퍼 메서드
    public static String formatLearningTime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        if (hours > 0 && minutes > 0) {
            return String.format("%d시간 %d분", hours, minutes);
        } else if (hours > 0) {
            return String.format("%d시간", hours);
        } else if (minutes > 0) {
            return String.format("%d분", minutes);
        } else {
            return "0분";
        }
    }
}
