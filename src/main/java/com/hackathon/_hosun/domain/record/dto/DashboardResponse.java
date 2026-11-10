package com.hackathon._hosun.domain.record.dto;

import com.hackathon._hosun.domain.analysis.entity.Criterion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardResponse(

        @Schema(description = "완료된 훈련 수", example = "4")
        long completedCount,

        @Schema(description = "최근 완료 3회 평균 자기 사고 구성 비율(%)", example = "71.0")
        BigDecimal selfThoughtRatio,

        @Schema(description = "연속 완료 일수(KST)", example = "4")
        int streakDays,

        @Schema(description = "완료 훈련 추이(최대 10개, 오래된 순)")
        List<TrendItem> trend,

        @Schema(description = "AI 의존도 변화(최근-첫, %p)", example = "-12.0")
        BigDecimal aiDependencyChange,

        @Schema(description = "자기 사고 구성 비율 변화(최근-첫, %p)", example = "12.0")
        BigDecimal selfThoughtChange,

        @Schema(description = "가장 많이 개선된 항목")
        CriterionSummary mostImproved,

        @Schema(description = "최근 3회 평균이 가장 낮은(보완이 필요한) 항목")
        CriterionSummary focusArea,

        @Schema(description = "최근 완료 4개")
        List<RecentItem> recent
) {

    public record TrendItem(
            Long trainingId, LocalDateTime completedAt, BigDecimal selfThoughtRatio, BigDecimal aiDependencyRatio) {
    }

    public record CriterionSummary(Criterion criterion, String label) {
    }

    public record RecentItem(Long trainingId, String topic, LocalDateTime completedAt, long recheckCount) {
    }
}
