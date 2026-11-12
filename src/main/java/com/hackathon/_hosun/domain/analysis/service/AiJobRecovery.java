package com.hackathon._hosun.domain.analysis.service;

import com.hackathon._hosun.domain.analysis.entity.AiReferenceDraft;
import com.hackathon._hosun.domain.analysis.entity.ComparisonAnalysis;
import com.hackathon._hosun.domain.analysis.entity.ProcessingStatus;
import com.hackathon._hosun.domain.analysis.repository.AiReferenceDraftRepository;
import com.hackathon._hosun.domain.analysis.repository.ComparisonAnalysisRepository;
import com.hackathon._hosun.domain.analysis.event.AnalysisRunMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 서버 재시작 시 끝나지 못한 AI 작업 복구.
 * AI 호출 중(GENERATING)에 서버가 꺼졌거나, 커밋 직후 이벤트가 실행되기 전에 꺼진(PENDING) 작업을 다시 실행한다.
 * GENERATING이던 분석은 AI 서버에서 아직 진행 중이거나 끝났을 수 있어서 다시 제출하지 않고 이어서 기다린다(RESUME).
 * ponytail: 서버 1대 기준. 여러 대로 늘리면 다른 서버가 처리 중인 작업까지 건드리므로 분산 락 등으로 바꿔야 함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiJobRecovery {

    private static final Set<ProcessingStatus> UNFINISHED = Set.of(ProcessingStatus.PENDING, ProcessingStatus.GENERATING);

    private final AiReferenceDraftRepository aiReferenceDraftRepository;
    private final ComparisonAnalysisRepository comparisonAnalysisRepository;
    private final AiReferenceDraftGenerator aiReferenceDraftGenerator;
    private final ComparisonAnalysisGenerator comparisonAnalysisGenerator;
    private final TransactionTemplate transactionTemplate;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void recover() {
        List<Long> draftIds = transactionTemplate.execute(status -> resetDrafts());
        Map<Long, AnalysisRunMode> analyses = transactionTemplate.execute(status -> resetAnalyses());
        if (draftIds.isEmpty() && analyses.isEmpty()) {
            return;
        }
        log.info("미완료 AI 작업 복구: AI 작성 글 {}건, 비교 분석 {}건", draftIds.size(), analyses.size());

        // AI 작성 글 먼저. 이걸 기다리는 분석은 generate() 안에서 보류됐다가 AI 작성 글이 끝나면 이어서 실행된다
        draftIds.forEach(aiReferenceDraftGenerator::generate);
        analyses.forEach(comparisonAnalysisGenerator::generate);
    }

    private List<Long> resetDrafts() {
        List<AiReferenceDraft> drafts = aiReferenceDraftRepository.findByStatusIn(UNFINISHED);
        drafts.forEach(AiReferenceDraft::resetToPending);
        return drafts.stream().map(AiReferenceDraft::getId).toList();
    }

    /** 분석 ID → 실행 방식. PENDING은 AI 서버에 제출 전이라 SUBMIT, GENERATING은 제출됐을 수 있어 RESUME */
    private Map<Long, AnalysisRunMode> resetAnalyses() {
        Map<Long, AnalysisRunMode> modes = new LinkedHashMap<>();
        for (ComparisonAnalysis analysis : comparisonAnalysisRepository.findByStatusIn(UNFINISHED)) {
            modes.put(analysis.getId(),
                    analysis.getStatus() == ProcessingStatus.GENERATING ? AnalysisRunMode.RESUME : AnalysisRunMode.SUBMIT);
            analysis.resetToPending();
        }
        return modes;
    }
}
