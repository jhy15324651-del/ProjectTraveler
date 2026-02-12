package org.zerock.projecttraveler.dto.info;

import jakarta.persistence.Column;
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

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;


}
