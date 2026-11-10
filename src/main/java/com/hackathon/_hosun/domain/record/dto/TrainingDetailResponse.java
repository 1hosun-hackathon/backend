package com.hackathon._hosun.domain.record.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record TrainingDetailResponse(

        @Schema(description = "훈련 ID", example = "12")
        Long trainingId,

        @Schema(description = "글 주제", example = "환경 보호의 중요성")
        String topic,

        @Schema(description = "글 종류", example = "논설문")
        String writingType,

        @Schema(description = "훈련 완료 시각")
        LocalDateTime completedAt,

        @Schema(description = "다시 검사한 횟수", example = "2")
        long recheckCount,

        @Schema(description = "최초 초안")
        DraftSummary draft,

        @Schema(description = "초안에 대한 비교 분석 ID (4-2 조회용)", example = "20")
        Long draftAnalysisId,

        @Schema(description = "최종 확정 글")
        FinalWritingSummary finalWriting,

        @Schema(description = "최종 글을 뺀 중간 수정본 목록")
        List<RevisionSummary> revisions
) {

    public record DraftSummary(Long versionId, String content, LocalDateTime createdAt) {
    }

    public record FinalWritingSummary(Long versionId, Integer seqNo, String content, LocalDateTime createdAt) {
    }

    public record RevisionSummary(
            Long versionId, Integer seqNo, Long analysisId, String content, LocalDateTime createdAt) {
    }
}
