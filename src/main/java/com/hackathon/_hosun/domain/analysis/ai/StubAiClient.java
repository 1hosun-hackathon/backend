package com.hackathon._hosun.domain.analysis.ai;

import com.hackathon._hosun.domain.analysis.entity.Criterion;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 실제 AI 모델이 붙기 전까지 쓰는 더미 구현. 프론트·흐름 테스트용 고정 문구를 돌려준다. */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.client", havingValue = "stub", matchIfMissing = true)
public class StubAiClient implements AiClient {

    @Override
    public String generateReferenceDraft(AiReferenceDraftRequest request) {
        log.info("[StubAiClient] AI 작성 글 더미 생성: trainingId={}", request.trainingId());
        return "[임시 AI 작성 글] 주제: " + request.topic() + " / 글 유형: " + request.writingType()
                + "\n실제 AI 모델 연동 전 더미 응답입니다.";
    }

    @Override
    public AiComparisonResult analyzeComparison(AiComparisonRequest request) {
        log.info("[StubAiClient] 비교 분석 더미 생성: analysisId={}", request.analysisId());
        List<AiComparisonResult.Item> items = Arrays.stream(Criterion.values())
                .map(c -> new AiComparisonResult.Item(
                        c, "[임시] 내 글 요약", "[임시] AI 글 요약", "[임시] 비교", "[임시] 반영 포인트", 3))
                .toList();
        return new AiComparisonResult(
                "[임시] 전체 요약", "[임시] 강점", "[임시] 개선점",
                items,
                List.of("[임시] 질문 1", "[임시] 질문 2", "[임시] 질문 3"),
                null);
    }

    @Override
    public AiComparisonResult retryComparison(AiComparisonRequest request) {
        return analyzeComparison(request);
    }

    @Override
    public AiComparisonResult resumeComparison(AiComparisonRequest request) {
        return analyzeComparison(request);
    }

    @Override
    public AiRatioResult calculateRatio(AiRatioRequest request) {
        log.info("[StubAiClient] 자기 사고 비율 더미 계산: trainingId={}", request.trainingId());
        return new AiRatioResult(new BigDecimal("70.0"), new BigDecimal("30.0"));
    }
}
