package com.example.lostfound.dto;

import com.example.lostfound.domain.enums.LostItemCategory;
import com.example.lostfound.domain.enums.LostItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LostItemCreateForm {

    @NotNull(message = "게시글 카테고리를 선택해 주세요.")
    private LostItemCategory category;

    @NotNull(message = "물품 카테고리를 선택해 주세요.")
    private LostItemType itemType;

    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(max = 100, message = "제목은 100자 이하로 입력해 주세요.")
    private String title;

    @Size(max = 2000, message = "설명은 2000자 이하로 입력해 주세요.")
    private String description;

    @NotBlank(message = "위치 설명을 입력해 주세요.")
    private String locationName;

    @NotBlank(message = "연락처를 입력해 주세요.")
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호는 010-1234-5678 형식으로 입력해 주세요.")
    private String contactInfo;

    @NotNull(message = "지도에서 위치를 선택해 주세요.")
    private Double latitude;

    @NotNull(message = "지도에서 위치를 선택해 주세요.")
    private Double longitude;
}
