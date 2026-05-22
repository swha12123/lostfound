package com.example.lostfound.strategy.itemtype;

import com.example.lostfound.domain.enums.LostItemType;
import org.springframework.stereotype.Component;

import java.util.List;

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

    @Override
    public List<String> searchKeywords() {
        return List.of("가방", "백팩", "배낭", "크로스백", "에코백", "파우치", "bag");
    }
}
