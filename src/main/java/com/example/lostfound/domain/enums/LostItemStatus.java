package com.example.lostfound.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LostItemStatus {
    SEARCHING("주인 찾는 중"),
    FOUND("보관 중"),
    RESOLVED("반환 완료");

    private final String label;
}
