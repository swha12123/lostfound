package com.example.lostfound.dto;

import com.example.lostfound.domain.enums.LostItemType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class LostItemListDto {
    private Long id;
    private String title;
    private String locationName;
    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;
    private boolean hasImage;
    private String firstImagePath;
    private String categoryLabel;
    private String categoryClass;
    private LostItemType itemType;
    private String itemTypeLabel;
    private String itemTypeClass;
    private String itemTypeMarkerColor;
    private String statusLabel;
    private String statusClass;
}
