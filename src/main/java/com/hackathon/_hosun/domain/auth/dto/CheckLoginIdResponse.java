package com.hackathon._hosun.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CheckLoginIdResponse(

        @Schema(description = "사용 가능 여부 (true면 사용 가능)", example = "true")
        boolean available
) {
}
