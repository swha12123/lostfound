package com.example.lostfound.strategy.itemtype;

import com.example.lostfound.domain.enums.LostItemType;

public interface LostItemTypeStrategy {

    LostItemType supports();

    String badgeClass();

    String description();

    String markerColorHex();
}
