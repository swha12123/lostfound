package com.example.lostfound.strategy.itemtype;

import com.example.lostfound.domain.enums.LostItemType;
import org.springframework.stereotype.Component;

@Component
public class ClothingItemTypeStrategy implements LostItemTypeStrategy {

    @Override
    public LostItemType supports() {
        return LostItemType.CLOTHING;
    }

    @Override
    public String badgeClass() {
        return "item-type-clothing";
    }

    @Override
    public String description() {
        return "외투, 모자, 머플러처럼 착용 물품 분류입니다.";
    }

    @Override
    public String markerColorHex() {
        return "#dc267f";
    }
}
