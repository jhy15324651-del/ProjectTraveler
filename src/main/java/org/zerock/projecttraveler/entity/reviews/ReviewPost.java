package org.zerock.projecttraveler.entity.reviews;

import jakarta.persistence.*;
import lombok.*;
import org.zerock.projecttraveler.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "review_post")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 작성자(로그인 붙이면 User 엔티티로 ManyToOne 바꾸면 됨)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id") // review_post.writer_id
    private User writer;


    @Column(nullable = false, length = 200)
    private String title;

    // Quill HTML 저장
    @Lob
    @Column(nullable = false)
    private String content;

    // 단일 선택 메타데이터
    @Column(length = 30)
    private String travelType;

    @Column(length = 30)
    private String theme;

    @Column(length = 30)
    private String period;

    @Column(length = 30)
    private String level;

    // 다중 선택 지역 태그: join table로 분리하지 않고 "리스트"로 간단 저장
    @ElementCollection
    @CollectionTable(
            name = "review_post_region_tag",
            joinColumns = @JoinColumn(name = "post_id")
    )
    @Column(name = "tag", length = 30)
    @Builder.Default
    private List<String> regionTags = new ArrayList<>();

    // 예산 항목 (원 단위로 저장)
    private Integer budgetFlight;
    private Integer budgetLodging;
    private Integer budgetFood;
    private Integer budgetExtra;

    private Integer budgetTotal;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        recalcTotal();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        recalcTotal();
    }

    private void recalcTotal() {
        int f = (budgetFlight == null) ? 0 : budgetFlight;
        int l = (budgetLodging == null) ? 0 : budgetLodging;
        int fo = (budgetFood == null) ? 0 : budgetFood;
        int e = (budgetExtra == null) ? 0 : budgetExtra;
        this.budgetTotal = f + l + fo + e;
    }

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Transient
    private String thumbnailUrl;

    @Transient
    private String summary;

    @Transient
    private Integer regionMatchCount;

}
