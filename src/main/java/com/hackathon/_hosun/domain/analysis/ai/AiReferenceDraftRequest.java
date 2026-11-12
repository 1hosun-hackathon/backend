package com.hackathon._hosun.domain.analysis.ai;

/** AI 작성 글 생성 요청(2-1). 첨부파일은 AI가 글에 반영하지 않아서 보내지 않는다. */
public record AiReferenceDraftRequest(
        Long trainingId,
        String topic,
        String writingType,
        String requirements,
        Integer timerMinutes
) {
}
