package com.example.lostfound.strategy.itemtype;

import com.example.lostfound.domain.enums.LostItemType;
import org.springframework.stereotype.Component;

import java.util.List;

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

    @Override
    public List<String> searchKeywords() {
        return List.of("전자기기", "이어폰", "에어팟", "충전기", "휴대폰", "스마트폰", "노트북", "태블릿", "마우스", "키보드", "electronic");
    }
}
