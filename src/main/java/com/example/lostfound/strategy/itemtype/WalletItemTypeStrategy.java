package com.example.lostfound.strategy.itemtype;

import com.example.lostfound.domain.enums.LostItemType;
import org.springframework.stereotype.Component;

import java.util.List;

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

    @Override
    public List<String> searchKeywords() {
        return List.of("지갑", "반지갑", "장지갑", "카드지갑", "wallet", "카드", "현금", "동전");
    }
}
