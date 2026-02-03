package org.zerock.projecttraveler.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "planner_budget")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlannerBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planner_id", nullable = false)
    private TravelPlanner planner;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "planned_amount")
    @Builder.Default
    private Integer plannedAmount = 0;

    @Column(name = "actual_amount")
    @Builder.Default
    private Integer actualAmount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // 사용 퍼센트
    public int getUsagePercent() {
        if (plannedAmount == null || plannedAmount == 0) return 0;
        return (int) ((actualAmount * 100.0) / plannedAmount);
    }

    // 예산 초과 여부
    public boolean isOverBudget() {
        return actualAmount != null && plannedAmount != null && actualAmount > plannedAmount;
    }
}
