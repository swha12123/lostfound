package com.example.lostfound.strategy.itemtype;

import com.example.lostfound.domain.entity.LostItem;
import com.example.lostfound.domain.enums.LostItemType;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * 물품 카테고리별 검색 규칙과 표현 규칙을 정의한다.
 */
public interface LostItemTypeStrategy {

    /**
     * 이 전략이 담당하는 물품 카테고리를 반환한다.
     *
     * @return 지원하는 물품 카테고리
     */
    LostItemType supports();

    /**
     * 화면에 표시할 뱃지 CSS 클래스를 반환한다.
     *
     * @return 뱃지 CSS 클래스명
     */
    String badgeClass();

    /**
     * 상세 화면에 표시할 설명 문구를 반환한다.
     *
     * @return 물품 카테고리 설명
     */
    String description();

    /**
     * 지도 마커 색상 값을 반환한다.
     *
     * @return HEX 형식 마커 색상
     */
    String markerColorHex();

    /**
     * 검색어 매칭에 사용할 보조 키워드 목록을 반환한다.
     *
     * @return 보조 키워드 목록
     */
    List<String> searchKeywords();

    /**
     * 게시글이 현재 카테고리의 검색 규칙에 따라 검색어와 일치하는지 확인한다.
     *
     * @param item 대상 게시글
     * @param keyword 원본 검색어
     * @return 일치 여부
     */
    default boolean matchesKeyword(LostItem item, String keyword) {
        String normalizedKeyword = normalize(keyword);
        if (normalizedKeyword.isBlank()) {
            return true;
        }

        String searchableText = searchableText(item);
        List<String> aliases = searchKeywords().stream()
                .map(LostItemTypeStrategy::normalize)
                .filter(alias -> !alias.isBlank())
                .toList();

        // 공백으로 나뉜 모든 토큰이 본문이나 보조 키워드 중 하나와 일치해야 검색 성공으로 본다.
        for (String token : normalizedKeyword.split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }
            boolean matchedInText = searchableText.contains(token);
            boolean matchedInAliases = aliases.stream()
                    .anyMatch(alias -> alias.contains(token) || token.contains(alias));
            if (!matchedInText && !matchedInAliases) {
                return false;
            }
        }
        return true;
    }

    /**
     * 제목, 설명, 위치명을 합쳐 검색 대상 문자열을 만든다.
     *
     * @param item 대상 게시글
     * @return 검색용 문자열
     */
    private static String searchableText(LostItem item) {
        return Stream.of(item.getTitle(), item.getDescription(), item.getLocationName())
                .map(LostItemTypeStrategy::normalize)
                .filter(text -> !text.isBlank())
                .reduce("", (left, right) -> left + " " + right)
                .trim();
    }

    /**
     * 문자열을 소문자 및 trim 기준으로 정규화한다.
     *
     * @param value 원본 문자열
     * @return 정규화된 문자열
     */
    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
