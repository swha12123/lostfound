package com.example.lostfound.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LostItemStatisticsDto {
    private long searchingCount;
    private long foundCount;
    private long resolvedCount;
    private long totalCount;
}
