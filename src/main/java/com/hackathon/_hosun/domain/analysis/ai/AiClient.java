package com.hackathon._hosun.domain.analysis.ai;

/**
 * AI 모델 호출 진입점. 구현체는 application.yaml 의 ai.client 값으로 고른다.
 * - stub: StubAiClient (더미 응답, 기본값)
 * - http: HttpAiClient (FastAPI AI 서버 호출, 설정은 ai.http.*)
 * 실패 시 예외를 던지면 호출한 쪽에서 FAILED로 기록한다.
 */
public interface AiClient {

    /** 과제 조건만으로 AI 작성 글을 생성한다(2-1). */
    String generateReferenceDraft(AiReferenceDraftRequest request);

    /** 새 분석을 제출하고 결과가 나올 때까지 기다린다(3-2 / 3-4). */
    AiComparisonResult analyzeComparison(AiComparisonRequest request);

    /** 실패한 분석을 다시 실행하고 결과가 나올 때까지 기다린다(4-4). */
    AiComparisonResult retryComparison(AiComparisonRequest request);

    /** 서버 재시작으로 끊긴 분석을 이어서 기다린다. AI 서버가 분석을 잊었으면 다시 제출한다. */
    AiComparisonResult resumeComparison(AiComparisonRequest request);

    /** 최종 글의 자기 사고 비율·AI 의존도를 계산한다(3-5, 동기). */
    AiRatioResult calculateRatio(AiRatioRequest request);
}
