package com.hackathon._hosun.domain.analysis.dto;

import com.hackathon._hosun.domain.analysis.entity.Criterion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/** 4-2 비교 분석 결과. 점수(userScore)는 대시보드 집계 전용이라 포함하지 않는다. */
public record AnalysisResultResponse(

        @Schema(description = "비교 분석 ID", example = "21")
        Long analysisId,

        @Schema(description = "훈련 ID", example = "12")
        Long trainingId,

        @Schema(description = "제출 회차 (0 = 초안)", example = "1")
        Integer seqNo,

        @Schema(description = "분석 생성 시각")
        LocalDateTime createdAt,

        @Schema(description = "분석 대상인 내 글")
        MyWriting myWriting,

        @Schema(description = "AI 작성 글")
        AiWriting aiWriting,

        @Schema(description = "최초 초안. 재검사(seqNo ≥ 1)일 때만, 초안 분석이면 null")
        Baseline baseline,

        @Schema(description = "전체 요약")
        String overallSummary,

        @Schema(description = "강점")
        String strengths,

        @Schema(description = "개선점")
        String improvements,

        @Schema(description = "기준별 비교 (THESIS, EVIDENCE, STRUCTURE, EXPRESSION, ORIGINALITY 고정 순서)")
        List<Item> items,

        @Schema(description = "생각해볼 질문")
        List<Question> questions
) {

    public record MyWriting(Long versionId, String content) {
    }

    public record AiWriting(String content) {
    }

    public record Baseline(Long versionId, String content) {
    }

    public record Item(
            @Schema(example = "THESIS") Criterion criterion,
            @Schema(example = "논지") String label,
            @Schema(example = "핵심 주장과 관점") String description,
            String userSummary,
            String aiSummary,
            String comparison,
            String reflectPoint) {
    }

    public record Question(@Schema(example = "1") Short seqNo, String content) {
    }
}
