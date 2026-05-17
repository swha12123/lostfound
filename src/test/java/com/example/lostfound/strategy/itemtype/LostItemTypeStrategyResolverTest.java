package com.example.lostfound.strategy.itemtype;

import com.example.lostfound.domain.enums.LostItemType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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
}
