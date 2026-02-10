package org.zerock.projecttraveler.dto.reviews;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewPostCreateRequest {

    private Long id; // ✅ 추가

    @NotBlank
    @Size(max = 200)
    private String title;

    // Quill HTML (hidden input content)
    @NotBlank
    private String content;

    // 단일 선택
    private String travelType;
    private String theme;
    private String period;
    private String level;

    // 다중 선택 (regionTags[])
    @Builder.Default
    private List<String> regionTags = new ArrayList<>();

    // 예산 (원 단위)
    private Integer budgetFlight;
    private Integer budgetLodging;
    private Integer budgetFood;
    private Integer budgetExtra;
}
