package com.example.lostfound.strategy.itemtype;

import com.example.lostfound.domain.enums.LostItemType;
import org.springframework.stereotype.Component;

@Component
public class IdCardItemTypeStrategy implements LostItemTypeStrategy {

    @Override
    public LostItemType supports() {
        return LostItemType.ID_CARD;
    }

    @Override
    public String badgeClass() {
        return "item-type-id-card";
    }

    @Override
    public String description() {
        return "학생증, 주민등록증, 체크카드처럼 확인이 중요한 카드류 분류입니다.";
    }

    @Override
    public String markerColorHex() {
        return "#0d9488";
    }
}
