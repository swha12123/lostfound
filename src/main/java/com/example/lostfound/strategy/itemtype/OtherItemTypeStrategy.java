package com.example.lostfound.strategy.itemtype;

import com.example.lostfound.domain.enums.LostItemType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OtherItemTypeStrategy implements LostItemTypeStrategy {

    @Override
    public LostItemType supports() {
        return LostItemType.OTHER;
    }

    @Override
    public String badgeClass() {
        return "item-type-other";
    }

    @Override
    public String description() {
        return "정해진 분류에 딱 맞지 않는 일반 물품 분류입니다.";
    }

    @Override
    public String markerColorHex() {
        return "#64748b";
    }

    @Override
    public List<String> searchKeywords() {
        return List.of("기타", "other", "misc");
    }
}
