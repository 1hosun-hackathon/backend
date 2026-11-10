package com.hackathon._hosun.domain.analysis.ai;

import com.hackathon._hosun.domain.analysis.entity.Criterion;
import java.math.BigDecimal;
import java.util.List;

/** AI 비교 분석 결과. comparison_analysis + analysis_item(기준별) + thinking_question 으로 저장된다. */
public record AiComparisonResult(
        String overallSummary,
        String strengths,
        String improvements,
        BigDecimal selfThoughtRatio,
        BigDecimal aiDependencyRatio,
        List<Item> items,
        List<String> questions
) {

    public record Item(
            Criterion criterion,
            String userSummary,
            String aiSummary,
            String comparison,
            String reflectPoint,
            Integer userScore
    ) {
    }
}
