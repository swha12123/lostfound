package com.example.lostfound.dto;

import com.example.lostfound.domain.enums.LostItemType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class LostItemDetailDto {
    private Long id;
    private String title;
    private String description;
    private String locationName;
    private String contactInfo;
    private Double latitude;
    private Double longitude;
    private boolean approved;
    private boolean resolved;
    private String categoryLabel;
    private String categoryClass;
    private LostItemType itemType;
    private String itemTypeLabel;
    private String itemTypeClass;
    private String itemTypeDescription;
    private String statusLabel;
    private String statusClass;
    private LocalDateTime createdAt;
    private List<String> imagePaths;
    private List<CommentViewDto> comments;
}
