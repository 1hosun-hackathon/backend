package com.hackathon._hosun.domain.analysis.service;

import com.hackathon._hosun.domain.analysis.dto.AnalysisResultResponse;
import com.hackathon._hosun.domain.analysis.dto.AnalysisRetryResponse;
import com.hackathon._hosun.domain.analysis.dto.AnalysisStatusResponse;
import com.hackathon._hosun.domain.analysis.dto.AnalysisSummaryResponse;
import com.hackathon._hosun.domain.analysis.entity.AiReferenceDraft;
import com.hackathon._hosun.domain.analysis.entity.AnalysisItem;
import com.hackathon._hosun.domain.analysis.entity.ComparisonAnalysis;
import com.hackathon._hosun.domain.analysis.entity.Criterion;
import com.hackathon._hosun.domain.analysis.entity.ProcessingStatus;
import com.hackathon._hosun.domain.analysis.event.AiReferenceDraftRequestedEvent;
import com.hackathon._hosun.domain.analysis.event.ComparisonAnalysisRequestedEvent;
import com.hackathon._hosun.domain.analysis.repository.AiReferenceDraftRepository;
import com.hackathon._hosun.domain.analysis.repository.AnalysisItemRepository;
import com.hackathon._hosun.domain.analysis.repository.ComparisonAnalysisRepository;
import com.hackathon._hosun.domain.analysis.repository.ThinkingQuestionRepository;
import com.hackathon._hosun.domain.training.repository.TrainingRepository;
import com.hackathon._hosun.domain.writing.entity.WritingVersion;
import com.hackathon._hosun.domain.writing.repository.WritingVersionRepository;
import com.hackathon._hosun.global.exception.BusinessException;
import com.hackathon._hosun.global.exception.ErrorCode;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisService {

    private static final int DRAFT_SEQ_NO = 0;

    private final ComparisonAnalysisRepository comparisonAnalysisRepository;
    private final AnalysisItemRepository analysisItemRepository;
    private final ThinkingQuestionRepository thinkingQuestionRepository;
    private final AiReferenceDraftRepository aiReferenceDraftRepository;
    private final WritingVersionRepository writingVersionRepository;
    private final TrainingRepository trainingRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** 4-1 분석 상태 (프론트 2초 폴링) */
    public AnalysisStatusResponse getStatus(Long userId, Long analysisId) {
        return comparisonAnalysisRepository.findStatusView(analysisId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND));
    }

    /** 4-2 비교 분석 결과 */
    public AnalysisResultResponse getResult(Long userId, Long analysisId) {
        ComparisonAnalysis analysis = comparisonAnalysisRepository.findOwned(analysisId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND));
        if (analysis.getStatus() != ProcessingStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.ANALYSIS_NOT_READY);
        }

        WritingVersion version = analysis.getVersion();
        Long trainingId = version.getTraining().getId();

        // 재검사(seqNo ≥ 1)일 때만 최초 초안을 기준선으로 같이 내려줌
        AnalysisResultResponse.Baseline baseline = null;
        if (version.getSeqNo() > DRAFT_SEQ_NO) {
            baseline = writingVersionRepository.findByTraining_IdAndSeqNo(trainingId, DRAFT_SEQ_NO)
                    .map(v -> new AnalysisResultResponse.Baseline(v.getId(), v.getContent()))
                    .orElse(null);
        }

        // Criterion 선언 순서 = 명세 고정 순서 (THESIS, EVIDENCE, STRUCTURE, EXPRESSION, ORIGINALITY)
        List<AnalysisResultResponse.Item> items = analysisItemRepository.findByAnalysis_Id(analysisId).stream()
                .sorted(Comparator.comparing(AnalysisItem::getCriterion))
                .map(this::toItem)
                .toList();

        List<AnalysisResultResponse.Question> questions =
                thinkingQuestionRepository.findByAnalysis_IdOrderBySeqNoAsc(analysisId).stream()
                        .map(q -> new AnalysisResultResponse.Question(q.getSeqNo(), q.getContent()))
                        .toList();

        return new AnalysisResultResponse(
                analysis.getId(),
                trainingId,
                version.getSeqNo(),
                analysis.getCreatedAt(),
                new AnalysisResultResponse.MyWriting(version.getId(), version.getContent()),
                new AnalysisResultResponse.AiWriting(analysis.getAiDraft().getContent()),
                baseline,
                analysis.getOverallSummary(),
                analysis.getStrengths(),
                analysis.getImprovements(),
                items,
                questions);
    }

    /** 4-3 회차별 분석 목록 (seqNo 오름차순, 상태 무관) */
    public List<AnalysisSummaryResponse> getByTraining(Long userId, Long trainingId) {
        trainingRepository.findByIdAndUser_IdAndDeletedAtIsNull(trainingId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRAINING_NOT_FOUND));
        return comparisonAnalysisRepository.findSummariesByTrainingId(trainingId);
    }

    /**
     * 4-4 분석 재시도. FAILED만 가능, 같은 분석 행을 PENDING으로 되돌려 다시 실행한다.
     * AI 작성 글 생성이 실패했던 거면 AI 작성 글부터 다시 만든다 (끝나면 이 분석이 자동으로 이어서 실행됨).
     */
    @Transactional
    public AnalysisRetryResponse retry(Long userId, Long analysisId) {
        ComparisonAnalysis analysis = comparisonAnalysisRepository.findOwnedForUpdate(analysisId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND));
        if (analysis.getStatus() != ProcessingStatus.FAILED) {
            throw new BusinessException(ErrorCode.ANALYSIS_NOT_FAILED);
        }
        analysis.resetToPending();

        AiReferenceDraft aiDraft = aiReferenceDraftRepository.findForUpdate(analysis.getAiDraft().getId())
                .orElseThrow(() -> new IllegalStateException("AI 작성 글이 없습니다: analysisId=" + analysisId));
        if (aiDraft.getStatus() == ProcessingStatus.FAILED) {
            aiDraft.resetToPending();
            eventPublisher.publishEvent(new AiReferenceDraftRequestedEvent(aiDraft.getId()));
        } else {
            // AI 작성 글이 생성 중이면 분석은 PENDING으로 대기했다가 AI 작성 글 완료 시 이어서 실행됨
            eventPublisher.publishEvent(new ComparisonAnalysisRequestedEvent(analysis.getId()));
        }
        return new AnalysisRetryResponse(analysis.getId(), analysis.getStatus());
    }

    private AnalysisResultResponse.Item toItem(AnalysisItem item) {
        Criterion criterion = item.getCriterion();
        return new AnalysisResultResponse.Item(
                criterion,
                criterion.getLabel(),
                criterion.getDescription(),
                item.getUserSummary(),
                item.getAiSummary(),
                item.getComparison(),
                item.getReflectPoint());
    }
}
