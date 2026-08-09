package com.example.lostfound.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentViewDto {
    private Long id;
    private String authorName;
    private String authorUsername;
    private String content;
    private LocalDateTime createdAt;
}
