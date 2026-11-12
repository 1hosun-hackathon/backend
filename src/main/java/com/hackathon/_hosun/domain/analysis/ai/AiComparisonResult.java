package com.hackathon._hosun.domain.analysis.ai;

import com.hackathon._hosun.domain.analysis.entity.Criterion;
import java.util.List;

/**
 * AI 비교 분석 결과. comparison_analysis + analysis_item(기준별) + thinking_question 으로 저장된다.
 * rawResult는 AI 서버 응답 원본 JSON으로, 다음 회차 재검사 때 previousResult로 그대로 보낸다.
 */
public record AiComparisonResult(
        String overallSummary,
        String strengths,
        String improvements,
        List<Item> items,
        List<String> questions,
        String rawResult
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
