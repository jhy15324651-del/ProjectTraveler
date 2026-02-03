package org.zerock.projecttraveler.dto.reviews;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReviewPostSearchRequest {
    private String q;

    private String travelType;
    private String theme;

    //중복선택가능
    private List<String> periods;
    private List<String> levels;
    private List<String> tags;

    //예산
    private Integer minBudget;
    private Integer maxBudget;

    private Integer page = 0;
    private Integer size = 12;
}

