package com.example.lostfound.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LostItemStatus {
    SEARCHING("찾고 있는 물건"),
    FOUND("습득한 물건"),
    RESOLVED("주인 찾은 물건");

    private final String label;
}
