package com.example.lostfound.strategy.itemtype;

import com.example.lostfound.domain.enums.LostItemType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class LostItemTypeStrategyResolver {

    private final Map<LostItemType, LostItemTypeStrategy> strategiesByType;

    public LostItemTypeStrategyResolver(List<LostItemTypeStrategy> strategies) {
        this.strategiesByType = new EnumMap<>(LostItemType.class);
        for (LostItemTypeStrategy strategy : strategies) {
            this.strategiesByType.put(strategy.supports(), strategy);
        }
    }

    public LostItemTypeStrategy resolve(LostItemType itemType) {
        LostItemType targetType = itemType != null ? itemType : LostItemType.OTHER;
        LostItemTypeStrategy strategy = strategiesByType.get(targetType);
        if (strategy == null) {
            throw new IllegalArgumentException("지원하지 않는 물품 카테고리입니다: " + targetType);
        }
        return strategy;
    }
}
