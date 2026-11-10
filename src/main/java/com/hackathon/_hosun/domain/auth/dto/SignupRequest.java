package com.hackathon._hosun.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @Schema(description = "로그인ID (영문 소문자+숫자 4~30자)", example = "jisu01")
        @NotBlank(message = "아이디를 입력해주세요.")
        @Pattern(regexp = "^[a-z0-9]{4,30}$", message = "아이디는 영문 소문자와 숫자 4~30자여야 합니다.")
        String loginId,

        @Schema(description = "비밀번호 (8자 이상)", example = "pass1234!")
        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String password,

        @Schema(description = "비밀번호 확인", example = "pass1234!")
        @NotBlank(message = "비밀번호 확인을 입력해주세요.")
        String passwordConfirm
) {
}
