package com.hackathon._hosun.domain.analysis.ai;

/** 사용자 글 vs AI 작성 글 비교 분석 요청. 필요한 값이 더 있으면 AI 담당자와 맞춰서 필드 추가 */
public record AiComparisonRequest(
        Long trainingId,
        String topic,
        String writingType,
        String requirements,
        String userWriting,
        String aiWriting,
        /** 재검사(seqNo ≥ 1)일 때 최초 초안. 초안 분석이면 null */
        String baselineWriting
) {
}
