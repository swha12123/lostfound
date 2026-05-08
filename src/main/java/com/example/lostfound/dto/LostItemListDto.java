package com.example.lostfound.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class LostItemListDto {
    private Long id;
    private String title;
    private String locationName;
    private LocalDateTime createdAt;
    private boolean hasImage;
    private String firstImagePath;
    private String categoryLabel;
    private String categoryClass;
    private String statusLabel;
    private String statusClass;
}
