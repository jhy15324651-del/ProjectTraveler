package org.zerock.projecttraveler.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "planner_share",
       uniqueConstraints = @UniqueConstraint(columnNames = {"planner_id", "shared_user_id"}))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlannerShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planner_id", nullable = false)
    private TravelPlanner planner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_user_id", nullable = false)
    private User sharedUser;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private Permission permission = Permission.VIEW;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum Permission {
        VIEW,   // 보기만 가능
        EDIT    // 편집 가능
    }
}
