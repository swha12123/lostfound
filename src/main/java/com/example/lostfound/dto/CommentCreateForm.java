package com.example.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentCreateForm {

    @NotBlank(message = "댓글 내용을 입력해 주세요.")
    @Size(max = 500, message = "댓글은 500자 이하로 입력해 주세요.")
    private String content;
}
