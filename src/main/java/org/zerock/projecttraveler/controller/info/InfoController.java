package org.zerock.projecttraveler.controller.info;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.zerock.projecttraveler.entity.info.InfoPost;
import org.zerock.projecttraveler.entity.info.InfoTabType;
import org.zerock.projecttraveler.repository.info.InfoPostRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class InfoController {

    private final InfoPostRepository infoPostRepository;

    @GetMapping("/info")
    public String info(
            @RequestParam(value = "region", required = false, defaultValue = "asahikawa") String region,
            @RequestParam(value = "tab", required = false, defaultValue = "food") String tab,
            Model model
    ) {

        /* ======================================================
           1) 지역 데이터 (단일 Source of Truth)
           ====================================================== */
        List<Map<String, String>> regions = List.of(
                // 홋카이도
                Map.of("key", "asahikawa", "label", "아사히카와", "group", "홋카이도"),
                Map.of("key", "sapporo",   "label", "삿포로",     "group", "홋카이도"),
                Map.of("key", "hakodate",  "label", "하코다테",   "group", "홋카이도"),

                // 혼슈
                Map.of("key", "tokyo",     "label", "도쿄",       "group", "혼슈"),
                Map.of("key", "osaka",     "label", "오사카",     "group", "혼슈"),
                Map.of("key", "nagoya",    "label", "나고야",     "group", "혼슈"),
                Map.of("key", "hiroshima", "label", "히로시마",   "group", "혼슈"),
                Map.of("key", "kyoto",     "label", "교토",       "group", "혼슈"),

                // 시코쿠
                Map.of("key", "kochi",     "label", "고치",       "group", "시코쿠"),
                Map.of("key", "matsuyama", "label", "마쓰야마",   "group", "시코쿠"),
                Map.of("key", "takamatsu", "label", "다카마쓰",   "group", "시코쿠"),

                // 큐슈 (+ 오키나와 포함)
                Map.of("key", "kitakyushu","label", "기타큐슈",   "group", "큐슈"),
                Map.of("key", "nagasaki",  "label", "나가사키",   "group", "큐슈"),
                Map.of("key", "kumamoto",  "label", "구마모토",   "group", "큐슈"),
                Map.of("key", "fukuoka",   "label", "후쿠오카",   "group", "큐슈"),
                Map.of("key", "okinawa",   "label", "오키나와",   "group", "큐슈")
        );

        /* ======================================================
           2) 그룹 목록 (사이드바 아코디언 순서 제어)
           ====================================================== */
        List<String> groups = List.of("홋카이도", "혼슈", "시코쿠", "큐슈");

        /* ======================================================
           3) region / tab 유효성 보정
           ====================================================== */
        Set<String> regionKeys = regions.stream()
                .map(r -> r.get("key"))
                .collect(Collectors.toSet());

        final String resolvedRegion = regionKeys.contains(region) ? region : "asahikawa";

        List<String> validTabs = List.of("food", "spot", "history");
        if (!validTabs.contains(tab)) tab = "food";

        /* ======================================================
           4) 현재 region이 속한 그룹 → 기본 오픈 그룹
           ====================================================== */
        String openGroup = regions.stream()
                .filter(r -> r.get("key").equals(resolvedRegion))
                .map(r -> r.get("group"))
                .findFirst()
                .orElse(groups.get(0));

        /* ======================================================
           5) 지역 메타 정보 (타이틀 / 설명)
           ====================================================== */
        Map<String, Map<String, String>> regionMeta = Map.ofEntries(
                Map.entry("asahikawa", Map.of("title","아사히카와","desc","홋카이도의 중심 도시로 아사히카와 라멘으로 유명합니다.")),
                Map.entry("sapporo",   Map.of("title","삿포로","desc","눈축제와 미식으로 유명한 홋카이도의 대표 도시입니다.")),
                Map.entry("hakodate",  Map.of("title","하코다테","desc","야경과 해산물로 유명한 항구 도시입니다.")),

                Map.entry("tokyo",     Map.of("title","도쿄","desc","일본의 수도로 쇼핑과 문화의 중심지입니다.")),
                Map.entry("osaka",     Map.of("title","오사카","desc","먹거리와 활기가 넘치는 서일본 대표 도시입니다.")),
                Map.entry("nagoya",    Map.of("title","나고야","desc","산업과 미식이 공존하는 중부권 핵심 도시입니다.")),
                Map.entry("hiroshima", Map.of("title","히로시마","desc","역사적 의미와 평화의 메시지를 가진 도시입니다.")),
                Map.entry("kyoto",     Map.of("title","교토","desc","전통과 사찰, 일본 문화의 정수 도시입니다.")),

                Map.entry("kochi",     Map.of("title","고치","desc","자연과 바다가 어우러진 시코쿠 남부 도시입니다.")),
                Map.entry("matsuyama", Map.of("title","마쓰야마","desc","도고 온천과 성으로 유명한 도시입니다.")),
                Map.entry("takamatsu", Map.of("title","다카마쓰","desc","우동과 항구 풍경이 유명한 관문 도시입니다.")),

                Map.entry("kitakyushu",Map.of("title","기타큐슈","desc","규슈 북부의 산업·항만 도시입니다.")),
                Map.entry("nagasaki",  Map.of("title","나가사키","desc","이국적인 항구 분위기와 역사를 지닌 도시입니다.")),
                Map.entry("kumamoto",  Map.of("title","구마모토","desc","구마모토성과 자연 경관이 인상적인 도시입니다.")),
                Map.entry("fukuoka",   Map.of("title","후쿠오카","desc","교통·쇼핑·먹거리 모두 강점인 규슈 대표 도시입니다.")),
                Map.entry("okinawa",   Map.of("title","오키나와","desc","에메랄드빛 바다의 일본 대표 휴양지입니다."))
        );

        Map<String, String> meta = regionMeta.getOrDefault(
                resolvedRegion,
                Map.of("title", "여행지 정보", "desc", "")
        );

        /* ======================================================
           6) DB 카드 목록 조회
           - ✅ 탭 3개 모두 모델에 내려준다 (HTML 최종본이 cardsFood/Spot/History를 기대)
           - ✅ 정렬: sortOrder ASC
           ====================================================== */
        List<InfoPost> cardsFood = infoPostRepository
                .findByRegionKeyAndTabTypeOrderBySortOrderAsc(resolvedRegion, InfoTabType.FOOD);

        List<InfoPost> cardsSpot = infoPostRepository
                .findByRegionKeyAndTabTypeOrderBySortOrderAsc(resolvedRegion, InfoTabType.SPOT);

        List<InfoPost> cardsHistory = infoPostRepository
                .findByRegionKeyAndTabTypeOrderBySortOrderAsc(resolvedRegion, InfoTabType.HISTORY);

        /* ======================================================
           7) Model 전달
           ====================================================== */
        model.addAttribute("activePage", "info");

        model.addAttribute("regions", regions);
        model.addAttribute("groups", groups);
        model.addAttribute("openGroup", openGroup);

        model.addAttribute("region", resolvedRegion);
        model.addAttribute("tab", tab);

        model.addAttribute("regionTitle", meta.get("title"));
        model.addAttribute("regionDesc", meta.get("desc"));

        // ✅ HTML 최종본과 매칭되는 이름
        model.addAttribute("cardsFood", cardsFood);
        model.addAttribute("cardsSpot", cardsSpot);
        model.addAttribute("cardsHistory", cardsHistory);

        return "info/info";
    }
}
