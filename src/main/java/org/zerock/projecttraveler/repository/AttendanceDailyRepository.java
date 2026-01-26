package org.zerock.projecttraveler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.projecttraveler.entity.AttendanceDaily;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceDailyRepository extends JpaRepository<AttendanceDaily, Long> {

    Optional<AttendanceDaily> findByUserIdAndAttendDate(Long userId, LocalDate attendDate);

    boolean existsByUserIdAndAttendDate(Long userId, LocalDate attendDate);

    List<AttendanceDaily> findByUserIdOrderByAttendDateDesc(Long userId);

    // 총 출석일 수
    @Query("SELECT COUNT(a) FROM AttendanceDaily a WHERE a.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    // 특정 월의 출석 기록
    @Query("SELECT a FROM AttendanceDaily a WHERE a.user.id = :userId AND a.attendDate >= :startDate AND a.attendDate <= :endDate ORDER BY a.attendDate ASC")
    List<AttendanceDaily> findByUserIdAndMonth(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // 최근 N일간 출석 기록
    @Query("SELECT a FROM AttendanceDaily a WHERE a.user.id = :userId AND a.attendDate >= :fromDate ORDER BY a.attendDate DESC")
    List<AttendanceDaily> findRecentByUserId(@Param("userId") Long userId, @Param("fromDate") LocalDate fromDate);

    // 연속 출석일 계산을 위한 쿼리 (최근 출석 기록)
    @Query("SELECT a.attendDate FROM AttendanceDaily a WHERE a.user.id = :userId ORDER BY a.attendDate DESC")
    List<LocalDate> findAttendDatesByUserId(@Param("userId") Long userId);
}
