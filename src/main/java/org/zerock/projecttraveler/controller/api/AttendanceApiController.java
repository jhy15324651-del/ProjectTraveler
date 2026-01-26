package org.zerock.projecttraveler.controller.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zerock.projecttraveler.dto.ApiResponse;
import org.zerock.projecttraveler.dto.AttendanceDto;
import org.zerock.projecttraveler.security.SecurityUtils;
import org.zerock.projecttraveler.service.AttendanceService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceApiController {

    private final AttendanceService attendanceService;

    /**
     * 출석 체크
     */
    @PostMapping("/check-in")
    public ResponseEntity<ApiResponse<AttendanceDto.Stats>> checkIn() {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        try {
            boolean isNew = attendanceService.checkIn(userId);
            AttendanceDto.Stats stats = attendanceService.getStats(userId);

            if (isNew) {
                return ResponseEntity.ok(ApiResponse.success("출석 체크 완료!", stats));
            } else {
                return ResponseEntity.ok(ApiResponse.success("이미 오늘 출석했습니다.", stats));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 출석 터치 (학습 시 자동 호출)
     */
    @PostMapping("/touch")
    public ResponseEntity<ApiResponse<Void>> touch() {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        attendanceService.touchAttendance(userId);
        return ResponseEntity.ok(ApiResponse.success("출석이 기록되었습니다."));
    }

    /**
     * 출석 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AttendanceDto.Stats>> getStats() {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        AttendanceDto.Stats stats = attendanceService.getStats(userId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 월별 출석 현황 조회
     */
    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<AttendanceDto.MonthlyView>> getMonthlyView(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        LocalDate now = LocalDate.now();
        int y = year != null ? year : now.getYear();
        int m = month != null ? month : now.getMonthValue();

        AttendanceDto.MonthlyView view = attendanceService.getMonthlyView(userId, y, m);
        return ResponseEntity.ok(ApiResponse.success(view));
    }
}
