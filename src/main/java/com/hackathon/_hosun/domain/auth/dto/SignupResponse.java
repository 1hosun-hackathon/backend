package com.hackathon._hosun.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SignupResponse(

        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "로그인ID", example = "jisu01")
        String loginId
) {
}
