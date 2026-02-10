package org.zerock.projecttraveler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.zerock.projecttraveler.entity.PlannerItinerary;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryBudgetSummary {

    private PlannerItinerary.Category category;
    private String categoryName;
    private String categoryClassName;
    private String categoryEmoji;
    private int amount;
    private int percent;

    public static CategoryBudgetSummary of(PlannerItinerary.Category category, int amount, int totalBudget) {
        String categoryName = getCategoryDisplayName(category);
        String categoryClassName = category != null ? category.name().toLowerCase() : "other";
        String categoryEmoji = getCategoryEmoji(category);
        int percent = totalBudget > 0 ? Math.round((float) amount / totalBudget * 100) : 0;

        return CategoryBudgetSummary.builder()
                .category(category)
                .categoryName(categoryName)
                .categoryClassName(categoryClassName)
                .categoryEmoji(categoryEmoji)
                .amount(amount)
                .percent(percent)
                .build();
    }

    private static String getCategoryDisplayName(PlannerItinerary.Category category) {
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

    private static String getCategoryEmoji(PlannerItinerary.Category category) {
        if (category == null) return "📌";
        return switch (category) {
            case ATTRACTION -> "🏛️";
            case RESTAURANT -> "🍽️";
            case ACCOMMODATION -> "🛏️";
            case TRANSPORT -> "🚗";
            case SHOPPING -> "🛍️";
            case OTHER -> "📌";
        };
    }
}
