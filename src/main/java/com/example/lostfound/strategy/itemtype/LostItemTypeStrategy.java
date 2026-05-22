package com.example.lostfound.strategy.itemtype;

import com.example.lostfound.domain.entity.LostItem;
import com.example.lostfound.domain.enums.LostItemType;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public interface LostItemTypeStrategy {

    LostItemType supports();

    String badgeClass();

    String description();

    String markerColorHex();

    List<String> searchKeywords();

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

    private static String searchableText(LostItem item) {
        return Stream.of(item.getTitle(), item.getDescription(), item.getLocationName())
                .map(LostItemTypeStrategy::normalize)
                .filter(text -> !text.isBlank())
                .reduce("", (left, right) -> left + " " + right)
                .trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
