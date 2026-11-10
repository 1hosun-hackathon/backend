package com.hackathon._hosun.domain.writing.dto;

import com.hackathon._hosun.domain.training.entity.TrainingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/** 3-2 초안 제출, 3-4 다시 검사하기 응답 (202) */
public record SubmissionResponse(

        @Schema(description = "제출된 버전 ID", example = "30")
        Long versionId,

        @Schema(description = "제출 회차 (0 = 초안)", example = "0")
        Integer seqNo,

        @Schema(description = "생성된 비교 분석 ID (4-1로 폴링)", example = "20")
        Long analysisId,

        @Schema(description = "훈련 상태", example = "ANALYZING")
        TrainingStatus status
) {
}
