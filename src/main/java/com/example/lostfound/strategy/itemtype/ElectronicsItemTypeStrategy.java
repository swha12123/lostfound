package com.example.lostfound.strategy.itemtype;

import com.example.lostfound.domain.enums.LostItemType;
import org.springframework.stereotype.Component;

@Component
public class ElectronicsItemTypeStrategy implements LostItemTypeStrategy {

    @Override
    public LostItemType supports() {
        return LostItemType.ELECTRONICS;
    }

    @Override
    public String badgeClass() {
        return "item-type-electronics";
    }

    @Override
    public String description() {
        return "휴대폰, 태블릿, 이어폰, 충전기 같은 전자기기 분류입니다.";
    }

    @Override
    public String markerColorHex() {
        return "#673ab7";
    }
}
