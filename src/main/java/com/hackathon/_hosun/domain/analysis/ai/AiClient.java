package com.hackathon._hosun.domain.analysis.ai;

/**
 * AI 모델 호출 진입점. 구현체는 application.yaml 의 ai.client 값으로 고른다.
 * - stub: StubAiClient (더미 응답, 기본값)
 * - http: HttpAiClient (FastAPI AI 서버 호출, 주소·경로·타임아웃은 ai.http.*)
 */
public interface AiClient {

    /**
     * 과제 조건(주제·글 유형·요구사항·첨부파일)만으로 AI 작성 글을 생성한다.
     * 실패 시 예외를 던지면 ai_reference_draft.status 가 FAILED 로 기록된다.
     */
    String generateReferenceDraft(AiReferenceDraftRequest request);

    /**
     * 사용자 글과 AI 작성 글을 비교 분석한다.
     * 실패 시 예외를 던지면 comparison_analysis.status 가 FAILED 로 기록된다.
     */
    AiComparisonResult analyzeComparison(AiComparisonRequest request);
}
