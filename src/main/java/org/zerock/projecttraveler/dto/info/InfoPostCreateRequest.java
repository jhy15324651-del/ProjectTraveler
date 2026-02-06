package org.zerock.projecttraveler.dto.info;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InfoPostCreateRequest {
    private String regionKey;   // asahikawa
    private String tabType;     // FOOD / SPOT / HISTORY
    private String title;       // 카드 제목
    private String summary;     // 카드 요약
    private String contentHtml; // (옵션) 처음부터 본문 넣을 수도 있음
}
