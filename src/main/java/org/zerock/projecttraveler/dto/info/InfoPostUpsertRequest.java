package org.zerock.projecttraveler.dto.info;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class InfoPostUpsertRequest {
    private String regionKey;   // ex) asahikawa
    private String tabType;     // ex) FOOD / SPOT / HISTORY
    private String title;
    private String summary;     // 지금은 비워도 됨
    private List<BlockDto> blocks = new ArrayList<>();

    @Data
    public static class BlockDto {
        private String type;    // "text" or "image"
        private String content; // text일 때
        private String src;     // image일 때
    }

    // ✅ 추가: Quill HTML 그대로 저장
    private String contentHtml;
}
