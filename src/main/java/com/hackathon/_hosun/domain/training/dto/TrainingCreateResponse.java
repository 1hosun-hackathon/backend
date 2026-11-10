package com.hackathon._hosun.domain.training.dto;

import com.hackathon._hosun.domain.training.entity.TrainingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record TrainingCreateResponse(

        @Schema(description = "훈련 ID", example = "12")
        Long trainingId,

        @Schema(description = "훈련 상태", example = "DRAFTING")
        TrainingStatus status,

        @Schema(description = "훈련 시작 시각")
        LocalDateTime startedAt,

        @Schema(description = "타이머 종료 시각 (타이머 미설정 시 null)")
        LocalDateTime timerEndsAt,

        @Schema(description = "첨부파일 목록")
        List<AttachmentResponse> attachments
) {
}
