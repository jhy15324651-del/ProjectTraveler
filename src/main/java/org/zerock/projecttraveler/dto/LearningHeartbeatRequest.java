package org.zerock.projecttraveler.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningHeartbeatRequest {

    @NotNull(message = "강좌 ID는 필수입니다.")
    private Long courseId;

    @NotNull(message = "레슨 ID는 필수입니다.")
    private Long lessonId;

    @Min(value = 0, message = "재생 위치는 0 이상이어야 합니다.")
    private int positionSec;

    @Min(value = 0, message = "시청 시간은 0 이상이어야 합니다.")
    private int deltaWatchedSec;
}
