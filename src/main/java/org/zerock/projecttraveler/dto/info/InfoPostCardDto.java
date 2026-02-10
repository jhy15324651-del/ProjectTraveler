package org.zerock.projecttraveler.dto.info;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InfoPostCardDto {
    private String key;  // postKey
    private String title;
    private String summary;
    private Integer sortOrder;
}
