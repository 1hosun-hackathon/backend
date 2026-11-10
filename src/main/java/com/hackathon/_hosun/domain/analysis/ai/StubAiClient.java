package com.hackathon._hosun.domain.analysis.ai;

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
}
