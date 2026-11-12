package com.hackathon._hosun.domain.analysis.ai;

/**
 * 사용자 글 vs AI 작성 글 비교 분석 요청(3-2 초안 / 3-4 다시 검사).
 * seqNo 0이면 초안 분석, 1 이상이면 재검사이고 baseline·previousResult가 채워진다.
 */
public record AiComparisonRequest(
        Long analysisId,
        Long trainingId,
        String topic,
        String writingType,
        String requirements,
        Long versionId,
        int seqNo,
        String content,
        String aiWriting,
        /** 재검사일 때 최초 초안(seqNo 0). 초안 분석이면 null */
        Long baselineVersionId,
        String baselineContent,
        /** 재검사일 때 직전 회차 분석 결과 원본 JSON(AI 서버 4-2 응답). 없으면 null */
        String previousResult
) {

    public boolean isRevision() {
        return seqNo >= 1;
    }
}
