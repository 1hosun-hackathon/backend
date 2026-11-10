package com.hackathon._hosun.domain.analysis.event;

/** 훈련 생성 트랜잭션 커밋 후 AI 작성 글 생성을 시작하기 위한 이벤트 */
public record AiReferenceDraftRequestedEvent(Long aiDraftId) {
}
