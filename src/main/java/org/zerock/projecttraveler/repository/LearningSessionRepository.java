package org.zerock.projecttraveler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.projecttraveler.entity.LearningSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LearningSessionRepository extends JpaRepository<LearningSession, Long> {

    // 활성 세션 찾기
    @Query("SELECT ls FROM LearningSession ls WHERE ls.user.id = :userId AND ls.endedAt IS NULL ORDER BY ls.startedAt DESC")
    List<LearningSession> findActiveSessionsByUserId(@Param("userId") Long userId);

    // 최근 세션
    @Query("SELECT ls FROM LearningSession ls WHERE ls.user.id = :userId ORDER BY ls.startedAt DESC")
    List<LearningSession> findRecentByUserId(@Param("userId") Long userId);

    // 총 학습 시간 (초)
    @Query("SELECT COALESCE(SUM(ls.durationSec), 0) FROM LearningSession ls WHERE ls.user.id = :userId")
    long sumDurationByUserId(@Param("userId") Long userId);

    // 특정 기간 학습 시간
    @Query("SELECT COALESCE(SUM(ls.durationSec), 0) FROM LearningSession ls WHERE ls.user.id = :userId AND ls.startedAt >= :from AND ls.startedAt < :to")
    long sumDurationByUserIdAndPeriod(@Param("userId") Long userId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
