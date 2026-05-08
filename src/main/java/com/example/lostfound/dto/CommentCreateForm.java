package com.example.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentCreateForm {

    @NotBlank(message = "작성자 이름은 필수입니다.")
    @Size(max = 30, message = "작성자 이름은 30자 이하로 입력해 주세요.")
    private String authorName;

    @NotBlank(message = "댓글 내용은 필수입니다.")
    @Size(max = 500, message = "댓글은 500자 이하로 입력해 주세요.")
    private String content;

    @NotBlank(message = "댓글 비밀번호는 필수입니다.")
    @Size(min = 4, max = 20, message = "댓글 비밀번호는 4자 이상 20자 이하로 입력해 주세요.")
    private String commentPassword;
}