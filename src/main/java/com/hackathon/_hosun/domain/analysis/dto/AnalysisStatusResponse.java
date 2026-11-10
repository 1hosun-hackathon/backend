package com.hackathon._hosun.domain.analysis.dto;

import com.hackathon._hosun.domain.analysis.entity.ProcessingStatus;
import com.hackathon._hosun.domain.training.entity.TrainingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/** 4-1 분석 상태. Repository JPQL 생성자 표현식으로 바로 조회한다. */
public record AnalysisStatusResponse(

        @Schema(description = "비교 분석 ID", example = "20")
        Long analysisId,

        @Schema(description = "분석 상태 (PENDING | GENERATING | SUCCESS | FAILED)", example = "GENERATING")
        ProcessingStatus status,

        @Schema(description = "훈련 상태 (분석 성공 시 COMPARED)", example = "ANALYZING")
        TrainingStatus trainingStatus
) {
}
