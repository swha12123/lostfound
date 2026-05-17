package com.example.lostfound.strategy.itemtype;

import com.example.lostfound.domain.enums.LostItemType;
import org.springframework.stereotype.Component;

@Component
public class KeysItemTypeStrategy implements LostItemTypeStrategy {

    @Override
    public LostItemType supports() {
        return LostItemType.KEYS;
    }

    @Override
    public String badgeClass() {
        return "item-type-keys";
    }

    @Override
    public String description() {
        return "집 열쇠, 자동차 키, 키링이 포함된 분류입니다.";
    }

    @Override
    public String markerColorHex() {
        return "#16a34a";
    }
}
