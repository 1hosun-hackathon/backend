package com.hackathon._hosun.domain.analysis.ai;

/** 훈련 완료(3-5) 시 자기 사고 비율·AI 의존도 계산 요청 */
public record AiRatioRequest(
        Long trainingId,
        String finalContent,
        Long finalVersionId,
        Long draftVersionId,
        String draftContent,
        String aiWriting
) {
}
