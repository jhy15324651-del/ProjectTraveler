package org.zerock.projecttraveler.dto.reviews;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReviewPostSearchRequest {

    // 텍스트 검색어 (title/content)
    private String q;

    // 지역태그 OR (regionTags)
    private List<String> tags;

    // 예산 총합 범위 (budgetTotal)
    private Integer minBudget;
    private Integer maxBudget;

    // 페이징
    private Integer page = 0;
    private Integer size = 12;
}
