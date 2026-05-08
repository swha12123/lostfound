package com.example.lostfound.dto;

import com.example.lostfound.domain.enums.LostItemCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LostItemCreateForm {

    @NotNull(message = "게시글 카테고리를 선택해 주세요.")
    private LostItemCategory category;

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이하로 입력해 주세요.")
    private String title;

    @Size(max = 2000, message = "설명은 2000자 이하로 입력해 주세요.")
    private String description;

    @NotBlank(message = "위치 설명은 필수입니다.")
    private String locationName;

    private Double latitude;
    private Double longitude;
}
