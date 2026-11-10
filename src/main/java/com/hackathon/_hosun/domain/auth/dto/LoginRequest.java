package com.hackathon._hosun.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @Schema(description = "로그인ID", example = "jisu01")
        @NotBlank(message = "아이디를 입력해주세요.")
        String loginId,

        @Schema(description = "비밀번호", example = "pass1234!")
        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password
) {
}
