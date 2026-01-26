package org.zerock.projecttraveler.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningCompleteRequest {

    @NotNull(message = "강좌 ID는 필수입니다.")
    private Long courseId;

    @NotNull(message = "레슨 ID는 필수입니다.")
    private Long lessonId;
}
