package com.example.lostfound.strategy.itemtype;

import com.example.lostfound.domain.enums.LostItemType;
import org.springframework.stereotype.Component;

@Component
public class BagItemTypeStrategy implements LostItemTypeStrategy {

    @Override
    public LostItemType supports() {
        return LostItemType.BAG;
    }

    @Override
    public String badgeClass() {
        return "item-type-bag";
    }

    @Override
    public String description() {
        return "백팩, 에코백, 파우치처럼 가방류 물품 분류입니다.";
    }

    @Override
    public String markerColorHex() {
        return "#d97706";
    }
}
