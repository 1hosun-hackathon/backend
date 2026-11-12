package com.hackathon._hosun.domain.analysis.event;

/** 초안 제출·다시 검사·재시도 트랜잭션 커밋 후 비교 분석을 시작하기 위한 이벤트 */
public record ComparisonAnalysisRequestedEvent(Long analysisId, AnalysisRunMode mode) {
}
