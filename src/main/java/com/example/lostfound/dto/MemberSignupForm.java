package com.example.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberSignupForm {

    @NotBlank(message = "아이디를 입력해 주세요.")
    @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하로 입력해 주세요.")
    private String username;

    @NotBlank(message = "닉네임을 입력해 주세요.")
    @Size(max = 30, message = "닉네임은 30자 이하로 입력해 주세요.")
    private String displayName;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Size(min = 6, max = 50, message = "비밀번호는 6자 이상 50자 이하로 입력해 주세요.")
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력해 주세요.")
    private String passwordConfirm;
}
