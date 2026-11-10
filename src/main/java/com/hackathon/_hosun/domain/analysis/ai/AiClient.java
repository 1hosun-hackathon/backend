package com.hackathon._hosun.domain.analysis.ai;

/**
 * AI 모델 호출 진입점. AI 담당자가 실제 구현체를 만들면 여기에 붙인다.
 * 구현체를 추가할 때는 @ConditionalOnProperty(name = "ai.client", havingValue = "...") 를 달고
 * application.yaml 의 ai.client 값을 바꾸면 StubAiClient 대신 사용된다.
 */
public interface AiClient {

    /**
     * 과제 조건(주제·글 유형·요구사항·첨부파일)만으로 AI 작성 글을 생성한다.
     * 실패 시 예외를 던지면 ai_reference_draft.status 가 FAILED 로 기록된다.
     */
    String generateReferenceDraft(AiReferenceDraftRequest request);
}
