package org.zerock.projecttraveler.repository.info;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.projecttraveler.entity.info.InfoPost;
import org.zerock.projecttraveler.entity.info.InfoTabType;

import java.util.List;
import java.util.Optional;

public interface InfoPostRepository extends JpaRepository<InfoPost, Long> {

    Optional<InfoPost> findByPostKey(String postKey);

    // ✅ 카드 목록: 등록/정렬 순서대로
    List<InfoPost> findByRegionKeyAndTabTypeOrderBySortOrderAsc(String regionKey, InfoTabType tabType);

    // ✅ 새 카드 추가 시 마지막 sortOrder 구하기
    long countByRegionKeyAndTabType(String regionKey, InfoTabType tabType);

    void deleteByPostKey(String postKey);


}
