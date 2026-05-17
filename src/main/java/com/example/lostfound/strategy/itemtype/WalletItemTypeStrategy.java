package com.example.lostfound.strategy.itemtype;

import com.example.lostfound.domain.enums.LostItemType;
import org.springframework.stereotype.Component;

@Component
public class WalletItemTypeStrategy implements LostItemTypeStrategy {

    @Override
    public LostItemType supports() {
        return LostItemType.WALLET;
    }

    @Override
    public String badgeClass() {
        return "item-type-wallet";
    }

    @Override
    public String description() {
        return "지갑, 카드지갑, 동전지갑 같은 소지품 분류입니다.";
    }

    @Override
    public String markerColorHex() {
        return "#228be6";
    }
}
