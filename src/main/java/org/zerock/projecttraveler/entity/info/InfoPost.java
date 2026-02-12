// InfoPost.java (추가/수정 부분만 예시)
package org.zerock.projecttraveler.entity.info;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfoPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String postKey;

    @Column(nullable = false)
    private String regionKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InfoTabType tabType;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String summary;

    @Lob
    private String contentHtml;

    private String createdBy;

    // ✅ 정렬용 (등록순 + 관리자 reorder)
    @Column(nullable = false)
    private Integer sortOrder;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;


}
