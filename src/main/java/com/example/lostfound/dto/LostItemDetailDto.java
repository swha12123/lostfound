package com.example.lostfound.dto;

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
    private Double latitude;
    private Double longitude;
    private boolean approved;
    private boolean resolved;
    private String categoryLabel;
    private String categoryClass;
    private String statusLabel;
    private String statusClass;
    private LocalDateTime createdAt;
    private List<String> imagePaths;
    private List<CommentViewDto> comments;
}
