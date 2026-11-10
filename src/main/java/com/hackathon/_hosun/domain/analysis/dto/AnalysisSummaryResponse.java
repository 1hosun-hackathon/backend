package com.hackathon._hosun.domain.analysis.dto;

import com.hackathon._hosun.domain.analysis.entity.ProcessingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 4-3 회차별 분석 목록 항목. Repository JPQL 생성자 표현식으로 바로 조회한다. */
public record AnalysisSummaryResponse(

        @Schema(description = "비교 분석 ID", example = "20")
        Long analysisId,

        @Schema(description = "제출 회차 (0 = 초안)", example = "0")
        Integer seqNo,

        @Schema(description = "분석 상태", example = "SUCCESS")
        ProcessingStatus status,

        @Schema(description = "분석 생성 시각")
        LocalDateTime createdAt
) {
}
