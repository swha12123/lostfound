package com.example.lostfound.strategy.itemtype;

import com.example.lostfound.domain.enums.LostItemType;
import org.springframework.stereotype.Component;

import java.util.List;

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

    @Override
    public List<String> searchKeywords() {
        return List.of("의류", "옷", "상의", "하의", "후드", "패딩", "외투", "점퍼", "자켓", "모자", "목도리", "clothing");
    }
}
