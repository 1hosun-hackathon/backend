package com.hackathon._hosun.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ReissueResponse(

        @Schema(description = "새로 발급된 Access Token (JWT)")
        String accessToken,

        @Schema(description = "Access Token 만료 시간(초)", example = "1800")
        long expiresIn
) {
}
