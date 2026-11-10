package com.hackathon._hosun.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(

        @Schema(description = "Access Token (JWT)")
        String accessToken,

        @Schema(description = "Access Token 만료 시간(초)", example = "1800")
        long expiresIn,

        @Schema(description = "로그인ID", example = "jisu01")
        String loginId
) {
}
