package com.example.lostfound.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LostItemCategory {
    REPORT("분실물 제보", LostItemStatus.FOUND),
    SEARCH("분실물 찾기", LostItemStatus.SEARCHING);

    private final String label;
    private final LostItemStatus defaultStatus;
}
