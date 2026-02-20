package org.zerock.projecttraveler.dto.reviews;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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

    private Long id; // ✅ 수정 시 사용

    @NotBlank
    @Size(max = 200)
    private String title;

    // Quill HTML
    @NotBlank
    private String content;

    // 단일 선택
    private String travelType;
    private String period;
    private String level;

    // 다중 선택
    @Builder.Default
    private List<String> regionTags = new ArrayList<>();

    @NotEmpty(message = "테마를 1개 이상 선택해주세요.")
    @Builder.Default
    private List<String> themes = new ArrayList<>();

    // 예산 (원 단위)
    private Integer budgetFlight;
    private Integer budgetLodging;
    private Integer budgetFood;
    private Integer budgetExtra;
}