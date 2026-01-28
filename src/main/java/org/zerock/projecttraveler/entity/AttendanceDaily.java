package org.zerock.projecttraveler.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_daily",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "attend_date"}))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "attend_date", nullable = false)
    private LocalDate attendDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (attendDate == null) {
            attendDate = LocalDate.now();
        }
    }
}
