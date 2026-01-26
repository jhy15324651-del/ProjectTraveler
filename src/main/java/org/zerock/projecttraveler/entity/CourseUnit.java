package org.zerock.projecttraveler.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "course_unit")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "duration_sec")
    @Builder.Default
    private Integer durationSec = 0;

    @OneToMany(mappedBy = "unit", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<Lesson> lessons = new ArrayList<>();

    // 유닛 내 레슨 수
    public int getLessonCount() {
        return lessons != null ? lessons.size() : 0;
    }

    // 시간 포맷 (예: "약 45분")
    public String getFormattedDuration() {
        if (durationSec == null || durationSec == 0) {
            // 레슨 시간 합산
            int total = lessons.stream()
                    .mapToInt(l -> l.getDurationSec() != null ? l.getDurationSec() : 0)
                    .sum();
            if (total == 0) return "미정";
            int minutes = total / 60;
            return String.format("약 %d분", minutes);
        }
        int minutes = durationSec / 60;
        return String.format("약 %d분", minutes);
    }
}