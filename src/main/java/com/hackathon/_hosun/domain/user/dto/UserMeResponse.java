package com.hackathon._hosun.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record UserMeResponse(

        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "로그인ID", example = "jisu01")
        String loginId,

        @Schema(description = "완료된(삭제되지 않은) 훈련 수", example = "4")
        long completedCount,

        @Schema(description = "가입일시")
        LocalDateTime createdAt
) {
}
