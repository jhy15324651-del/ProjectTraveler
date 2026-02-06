package org.zerock.projecttraveler.dto.info;

import lombok.*;
import org.zerock.projecttraveler.entity.info.InfoPost;

import java.util.List;

@Data
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InfoPostDto {
    private String key;            // ex) asahikawa-ramen
    private String regionKey;
    private String tabType;
    private String postKey;
    private String title;
    private String summary;
    private String contentHtml;

    public static InfoPostDto from(InfoPost p) {
        return InfoPostDto.builder()
                .key(p.getPostKey())          // JS에서 post.key 로 씀
                .postKey(p.getPostKey())      // 혼용 방지용
                .regionKey(p.getRegionKey())
                .tabType(p.getTabType().name())
                .title(p.getTitle())
                .summary(p.getSummary())
                .contentHtml(p.getContentHtml())
                .build();
    }
}
