package com.example.lostfound.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LostItemType {
    WALLET("지갑"),
    ELECTRONICS("전자기기"),
    ID_CARD("신분증/카드"),
    BAG("가방"),
    CLOTHING("의류"),
    KEYS("열쇠"),
    OTHER("기타");

    private final String label;
}
