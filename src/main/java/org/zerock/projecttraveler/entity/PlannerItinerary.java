package org.zerock.projecttraveler.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "planner_itinerary")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlannerItinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planner_id", nullable = false)
    private TravelPlanner planner;

    @Column(name = "day_index", nullable = false)
    private Integer dayIndex;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(length = 10)
    private String time;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 300)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private Category category = Category.OTHER;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    private Integer cost = 0;

    @Builder.Default
    private Boolean completed = false;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum Category {
        ATTRACTION,     // 관광지
        RESTAURANT,     // 식당
        ACCOMMODATION,  // 숙소
        TRANSPORT,      // 교통
        SHOPPING,       // 쇼핑
        OTHER           // 기타
    }

    // 카테고리 한글명
    public String getCategoryDisplayName() {
        if (category == null) return "기타";
        return switch (category) {
            case ATTRACTION -> "관광지";
            case RESTAURANT -> "식당";
            case ACCOMMODATION -> "숙소";
            case TRANSPORT -> "교통";
            case SHOPPING -> "쇼핑";
            case OTHER -> "기타";
        };
    }

    // 카테고리 CSS 클래스명
    public String getCategoryClassName() {
        if (category == null) return "other";
        return category.name().toLowerCase();
    }
}
