package org.zerock.projecttraveler.dto.info;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InfoPostReorderRequest {

    private List<Item> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String postKey;
        private Integer sortOrder; // 0..n
    }
}
