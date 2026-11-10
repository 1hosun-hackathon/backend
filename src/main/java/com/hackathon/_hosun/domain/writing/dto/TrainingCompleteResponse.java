package com.hackathon._hosun.domain.writing.dto;

import com.hackathon._hosun.domain.training.entity.TrainingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record TrainingCompleteResponse(

        @Schema(description = "훈련 ID", example = "12")
        Long trainingId,

        @Schema(description = "훈련 상태", example = "COMPLETED")
        TrainingStatus status,

        @Schema(description = "최종 글 버전 ID", example = "32")
        Long finalVersionId,

        @Schema(description = "완료 시각")
        LocalDateTime completedAt
) {
}
