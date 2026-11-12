package com.hackathon._hosun.domain.analysis.service;

import com.hackathon._hosun.domain.analysis.ai.AiClient;
import com.hackathon._hosun.domain.analysis.ai.AiComparisonRequest;
import com.hackathon._hosun.domain.analysis.ai.AiComparisonResult;
import com.hackathon._hosun.domain.analysis.entity.AiReferenceDraft;
import com.hackathon._hosun.domain.analysis.entity.AnalysisItem;
import com.hackathon._hosun.domain.analysis.entity.ComparisonAnalysis;
import com.hackathon._hosun.domain.analysis.entity.ProcessingStatus;
import com.hackathon._hosun.domain.analysis.entity.ThinkingQuestion;
import com.hackathon._hosun.domain.analysis.event.AnalysisRunMode;
import com.hackathon._hosun.domain.analysis.event.ComparisonAnalysisRequestedEvent;
import com.hackathon._hosun.domain.analysis.repository.AnalysisItemRepository;
import com.hackathon._hosun.domain.analysis.repository.ComparisonAnalysisRepository;
import com.hackathon._hosun.domain.analysis.repository.ThinkingQuestionRepository;
import com.hackathon._hosun.domain.training.entity.Training;
import com.hackathon._hosun.domain.writing.entity.WritingVersion;
import com.hackathon._hosun.domain.writing.repository.WritingVersionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 비교 분석 백그라운드 실행. PENDING → GENERATING → SUCCESS | FAILED, 성공 시 훈련은 ANALYZING → COMPARED.
 * AI 작성 글이 아직 생성 중이면 PENDING으로 두고, AI 작성 글 생성이 끝날 때 AiReferenceDraftGenerator가 다시 호출한다.
 * 분석 재시도(4-4)에서는 status를 PENDING으로 되돌린 뒤 RETRY로 generate()를 호출하면 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComparisonAnalysisGenerator {

    private final ComparisonAnalysisRepository comparisonAnalysisRepository;
    private final AnalysisItemRepository analysisItemRepository;
    private final ThinkingQuestionRepository thinkingQuestionRepository;
    private final WritingVersionRepository writingVersionRepository;
    private final AiClient aiClient;
    private final TransactionTemplate transactionTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequested(ComparisonAnalysisRequestedEvent event) {
        generate(event.analysisId(), event.mode());
    }

    public void generate(Long analysisId, AnalysisRunMode mode) {
        AiComparisonRequest request = transactionTemplate.execute(status -> startGenerating(analysisId));
        if (request == null) {
            return;
        }

        try {
            AiComparisonResult result = switch (mode) {
                case SUBMIT -> aiClient.analyzeComparison(request);
                case RETRY -> aiClient.retryComparison(request);
                case RESUME -> aiClient.resumeComparison(request);
            };
            transactionTemplate.executeWithoutResult(status -> saveResult(analysisId, result));
        } catch (Exception e) {
            log.error("비교 분석 실패: analysisId={}", analysisId, e);
            transactionTemplate.executeWithoutResult(status -> findAnalysis(analysisId).fail());
        }
    }

    /** 바로 분석할 수 없으면 null 반환 (PENDING이 아님 / AI 작성 글 대기 중 / AI 작성 글 실패) */
    private AiComparisonRequest startGenerating(Long analysisId) {
        ComparisonAnalysis analysis = findAnalysis(analysisId);
        if (analysis.getStatus() != ProcessingStatus.PENDING) {
            return null;
        }

        AiReferenceDraft aiDraft = analysis.getAiDraft();
        switch (aiDraft.getStatus()) {
            case PENDING, GENERATING -> {
                log.info("AI 작성 글 생성 대기 중이라 분석 보류: analysisId={}", analysisId);
                return null;
            }
            case FAILED -> {
                log.warn("AI 작성 글 생성 실패로 분석 실패 처리: analysisId={}", analysisId);
                analysis.fail();
                return null;
            }
            case SUCCESS -> analysis.startGenerating();
        }

        WritingVersion version = analysis.getVersion();
        Training training = version.getTraining();
        WritingVersion baseline = null;
        String previousResult = null;
        if (version.getSeqNo() >= 1) {
            List<WritingVersion> versions = writingVersionRepository.findByTraining_IdOrderBySeqNoAsc(training.getId());
            baseline = versions.get(0);
            previousResult = versions.stream()
                    .filter(v -> v.getSeqNo() == version.getSeqNo() - 1)
                    .findFirst()
                    .flatMap(v -> comparisonAnalysisRepository.findByVersion_Id(v.getId()))
                    .map(ComparisonAnalysis::getRawResult)
                    .orElse(null);
        }
        return new AiComparisonRequest(
                analysisId,
                training.getId(),
                training.getTopic(),
                training.getWritingType().getLabel(),
                training.getRequirements(),
                version.getId(),
                version.getSeqNo(),
                version.getContent(),
                aiDraft.getContent(),
                baseline == null ? null : baseline.getId(),
                baseline == null ? null : baseline.getContent(),
                previousResult);
    }

    private void saveResult(Long analysisId, AiComparisonResult result) {
        ComparisonAnalysis analysis = findAnalysis(analysisId);
        analysis.succeed(result.overallSummary(), result.strengths(), result.improvements(), result.rawResult());

        analysisItemRepository.saveAll(result.items().stream()
                .map(i -> AnalysisItem.create(
                        analysis, i.criterion(), i.userSummary(), i.aiSummary(),
                        i.comparison(), i.reflectPoint(), i.userScore()))
                .toList());

        List<String> questions = result.questions();
        for (int i = 0; i < questions.size(); i++) {
            thinkingQuestionRepository.save(ThinkingQuestion.create(analysis, (short) (i + 1), questions.get(i)));
        }

        analysis.getVersion().getTraining().markCompared();
    }

    private ComparisonAnalysis findAnalysis(Long analysisId) {
        return comparisonAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new IllegalStateException("비교 분석이 없습니다: " + analysisId));
    }
}
