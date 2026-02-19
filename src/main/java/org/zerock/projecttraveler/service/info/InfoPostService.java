package org.zerock.projecttraveler.service.info;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.zerock.projecttraveler.dto.info.*;
import org.zerock.projecttraveler.entity.info.InfoPost;
import org.zerock.projecttraveler.entity.info.InfoTabType;
import org.zerock.projecttraveler.repository.info.InfoPostRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InfoPostService {

    private final InfoPostRepository infoPostRepository;

    @Transactional
    public void upsertPost(String postKey, InfoPostUpsertRequest req, String adminUsername) {
        InfoTabType tab = InfoTabType.valueOf(req.getTabType());

        InfoPost post = infoPostRepository.findByPostKey(postKey)
                .orElseGet(() -> {
                    int nextOrder = (int) infoPostRepository.countByRegionKeyAndTabType(req.getRegionKey(), tab);
                    return InfoPost.builder()
                            .postKey(postKey)
                            .createdBy(adminUsername)
                            .sortOrder(nextOrder)
                            .build();
                });

        // region/tab은 패널 저장에서 항상 오니까 그대로 OK (원하면 null 체크 추가)
        post.setRegionKey(req.getRegionKey());
        post.setTabType(tab);

        if (req.getTitle() != null) post.setTitle(req.getTitle());
        if (req.getSummary() != null) post.setSummary(req.getSummary());

        // ✅ 핵심: contentHtml은 null이면 기존 값 유지
        if (req.getContentHtml() != null) {
            post.setContentHtml(req.getContentHtml());
        }

        // ✅ 썸네일: 이미 방어 잘 되어있음
        if (req.getThumbnailUrl() != null && !req.getThumbnailUrl().trim().isEmpty()) {
            post.setThumbnailUrl(req.getThumbnailUrl().trim());
        }

        infoPostRepository.save(post);
    }

    /**
     * ✅ 카드 생성: postKey 서버에서 발급 + sortOrder는 맨 뒤
     */
    @Transactional
    public String createPost(InfoPostCreateRequest req, String adminUsername) {
        if (req.getRegionKey() == null || req.getTabType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "regionKey/tabType required");
        }

        InfoTabType tab = InfoTabType.valueOf(req.getTabType());
        int nextOrder = (int) infoPostRepository.countByRegionKeyAndTabType(req.getRegionKey(), tab);

        String postKey = makePostKey(req.getRegionKey(), tab);

        InfoPost post = InfoPost.builder()
                .postKey(postKey)
                .regionKey(req.getRegionKey())
                .tabType(tab)
                .title(nvl(req.getTitle()))
                .summary(nvl(req.getSummary()))
                .contentHtml(nvl(req.getContentHtml()))
                .createdBy(adminUsername)
                .sortOrder(nextOrder)
                .build();

        infoPostRepository.save(post);
        return postKey;
    }

    /**
     * ✅ 카드 삭제: 삭제 후 같은 region/tab의 sortOrder를 0..n 재정렬
     */
    @Transactional
    public void deletePost(String postKey) {
        InfoPost target = infoPostRepository.findByPostKey(postKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No postKey=" + postKey));

        String regionKey = target.getRegionKey();
        InfoTabType tabType = target.getTabType();

        infoPostRepository.delete(target);

        // ✅ 재정렬(권장): gap 방지
        List<InfoPost> rest = infoPostRepository.findByRegionKeyAndTabTypeOrderBySortOrderAsc(regionKey, tabType);
        for (int i = 0; i < rest.size(); i++) {
            rest.get(i).setSortOrder(i);
        }
    }

    /**
     * ✅ 순서 저장(관리자)
     */
    @Transactional
    public void reorder(String regionKey, String tabType, InfoPostReorderRequest req) {
        if (req == null || req.getItems() == null) return;

        InfoTabType t = InfoTabType.valueOf(tabType);

        for (InfoPostReorderRequest.Item item : req.getItems()) {
            if (item.getPostKey() == null) continue;

            InfoPost post = infoPostRepository.findByPostKey(item.getPostKey())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No postKey=" + item.getPostKey()));

            if (!regionKey.equals(post.getRegionKey()) || post.getTabType() != t) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "region/tab mismatch for " + item.getPostKey());
            }

            post.setSortOrder(item.getSortOrder());
        }
    }

    private String makePostKey(String regionKey, InfoTabType tab) {
        // 예: asahikawa-FOOD-3f2a9c
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return regionKey + "-" + tab.name() + "-" + suffix;
    }

    private String nvl(String s) {
        return s == null ? "" : s.trim();
    }

    @Transactional(readOnly = true)
    public List<InfoPostCardDto> listCards(String regionKey, String tabType) {

        InfoTabType tt;
        try {
            tt = InfoTabType.valueOf(String.valueOf(tabType).toUpperCase());
        } catch (Exception e) {
            tt = InfoTabType.FOOD;
        }

        return infoPostRepository
                .findByRegionKeyAndTabTypeOrderBySortOrderAsc(regionKey, tt)
                .stream()
                .map(p -> InfoPostCardDto.builder()
                        .key(p.getPostKey())
                        .title(p.getTitle())
                        .summary(p.getSummary())
                        .sortOrder(p.getSortOrder())
                        // ✅ THUMB FIX 2) 카드 DTO에 thumbnailUrl 내려주기
                        .thumbnailUrl(p.getThumbnailUrl())
                        .build())
                .toList();
    }

    public InfoPostDto getPostDto(String postKey) {
        InfoPost post = infoPostRepository.findByPostKey(postKey)
                .orElseThrow(() -> new IllegalArgumentException("postKey not found: " + postKey));

        return InfoPostDto.from(post);
    }
}
