package com.hackathon._hosun.domain.analysis.dto;

import com.hackathon._hosun.domain.analysis.entity.ProcessingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record AnalysisRetryResponse(

        @Schema(description = "비교 분석 ID", example = "21")
        Long analysisId,

        @Schema(description = "분석 상태 (재시도 직후 PENDING)", example = "PENDING")
        ProcessingStatus status
) {
}
