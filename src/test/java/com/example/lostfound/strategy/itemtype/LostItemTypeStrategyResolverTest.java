package com.example.lostfound.strategy.itemtype;

import com.example.lostfound.domain.entity.LostItem;
import com.example.lostfound.domain.enums.LostItemCategory;
import com.example.lostfound.domain.enums.LostItemType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LostItemTypeStrategyResolverTest {

    private final LostItemTypeStrategyResolver resolver = new LostItemTypeStrategyResolver(List.of(
            new WalletItemTypeStrategy(),
            new ElectronicsItemTypeStrategy(),
            new IdCardItemTypeStrategy(),
            new BagItemTypeStrategy(),
            new ClothingItemTypeStrategy(),
            new KeysItemTypeStrategy(),
            new OtherItemTypeStrategy()
    ));

    @Test
    void resolveReturnsWalletStrategyWithMarkerColor() {
        LostItemTypeStrategy strategy = resolver.resolve(LostItemType.WALLET);

        assertInstanceOf(WalletItemTypeStrategy.class, strategy);
        assertEquals("item-type-wallet", strategy.badgeClass());
        assertEquals("#228be6", strategy.markerColorHex());
    }

    @Test
    void resolveReturnsOtherStrategy() {
        LostItemTypeStrategy strategy = resolver.resolve(LostItemType.OTHER);

        assertInstanceOf(OtherItemTypeStrategy.class, strategy);
        assertEquals(LostItemType.OTHER, strategy.supports());
    }

    @Test
    void strategyMatchesKeywordUsingTypeAliases() {
        LostItem item = LostItem.create(
                LostItemCategory.REPORT,
                LostItemType.ELECTRONICS,
                "분실물 보관 중",
                "충전 케이블과 본체가 함께 있었습니다.",
                "학생회관 앞",
                null,
                null,
                null
        );

        LostItemTypeStrategy strategy = resolver.resolve(LostItemType.ELECTRONICS);

        assertTrue(strategy.matchesKeyword(item, "전자기기"));
        assertTrue(strategy.matchesKeyword(item, "충전기"));
    }
}
