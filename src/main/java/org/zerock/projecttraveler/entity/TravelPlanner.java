package org.zerock.projecttraveler.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "travel_planner")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelPlanner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 200)
    private String destination;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image", length = 500)
    private String coverImage;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private Visibility visibility = Visibility.PRIVATE;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private Template template = Template.BLANK;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "like_count")
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "total_budget")
    @Builder.Default
    private Integer totalBudget = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 10)
    @Builder.Default
    private Currency currency = Currency.KRW;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "planner", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayIndex ASC, sortOrder ASC")
    @Builder.Default
    private List<PlannerItinerary> itineraries = new ArrayList<>();

    @OneToMany(mappedBy = "planner", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<PlannerChecklist> checklists = new ArrayList<>();

    @OneToMany(mappedBy = "planner", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<PlannerBudget> budgets = new ArrayList<>();

    @OneToMany(mappedBy = "planner", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PlannerShare> shares = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Visibility {
        PUBLIC,     // 전체 공개
        PRIVATE     // 비공개 (허용된 사용자만)
    }

    public enum Template {
        BLANK,      // 빈 플래너
        CITY,       // 도시 탐방
        NATURE,     // 자연 여행
        FOOD,       // 맛집 투어
        CULTURE,    // 문화 체험
        SHOPPING    // 쇼핑 여행
    }

    public enum Currency {
        KRW("₩", "원"),
        USD("$", "달러"),
        JPY("¥", "엔");

        private final String symbol;
        private final String name;

        Currency(String symbol, String name) {
            this.symbol = symbol;
            this.name = name;
        }

        public String getSymbol() {
            return symbol;
        }

        public String getDisplayName() {
            return name;
        }
    }

    // 여행 일수 계산
    public int getDays() {
        if (startDate == null || endDate == null) return 0;
        return (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;
    }

    // 공개 여부 확인
    public boolean isPublic() {
        return visibility == Visibility.PUBLIC;
    }

    // 작성자 이름
    public String getAuthorName() {
        return user != null ? user.getFullName() : "알 수 없음";
    }

    // 템플릿 한글명
    public String getTemplateDisplayName() {
        if (template == null) return "빈 플래너";
        return switch (template) {
            case BLANK -> "빈 플래너";
            case CITY -> "도시 탐방";
            case NATURE -> "자연 여행";
            case FOOD -> "맛집 투어";
            case CULTURE -> "문화 체험";
            case SHOPPING -> "쇼핑 여행";
        };
    }
}
