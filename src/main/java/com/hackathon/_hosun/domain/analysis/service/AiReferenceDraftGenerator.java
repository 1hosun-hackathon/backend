package com.hackathon._hosun.domain.analysis.service;

import com.hackathon._hosun.domain.analysis.ai.AiClient;
import com.hackathon._hosun.domain.analysis.ai.AiReferenceDraftRequest;
import com.hackathon._hosun.domain.analysis.entity.AiReferenceDraft;
import com.hackathon._hosun.domain.analysis.entity.ProcessingStatus;
import com.hackathon._hosun.domain.analysis.event.AiReferenceDraftRequestedEvent;
import com.hackathon._hosun.domain.analysis.event.AnalysisRunMode;
import com.hackathon._hosun.domain.analysis.entity.ComparisonAnalysis;
import com.hackathon._hosun.domain.analysis.repository.AiReferenceDraftRepository;
import com.hackathon._hosun.domain.analysis.repository.ComparisonAnalysisRepository;
import com.hackathon._hosun.domain.training.entity.Training;
import com.hackathon._hosun.domain.training.repository.TrainingAttachmentRepository;
import com.hackathon._hosun.global.storage.FileStorage;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * AI 작성 글 백그라운드 생성.
 * PENDING → GENERATING → SUCCESS | FAILED. AI 호출 동안에는 트랜잭션(DB 커넥션)을 잡지 않는다.
 * 끝나면 이 AI 작성 글을 기다리던(PENDING) 비교 분석을 이어서 실행하거나, 실패 시 함께 실패 처리한다.
 * 분석 재시도(4-4)에서 AI 작성 글부터 다시 만들어야 하면 status를 PENDING으로 되돌린 뒤 generate()를 호출하면 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiReferenceDraftGenerator {

    private final AiReferenceDraftRepository aiReferenceDraftRepository;
    private final ComparisonAnalysisRepository comparisonAnalysisRepository;
    private final ComparisonAnalysisGenerator comparisonAnalysisGenerator;
    private final TrainingAttachmentRepository trainingAttachmentRepository;
    private final FileStorage fileStorage;
    private final AiClient aiClient;
    private final TransactionTemplate transactionTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequested(AiReferenceDraftRequestedEvent event) {
        generate(event.aiDraftId());
    }

    public void generate(Long aiDraftId) {
        GenerationTarget target = transactionTemplate.execute(status -> startGenerating(aiDraftId));
        if (target == null) {
            return;
        }

        try {
            // 파일 읽기(디스크·버킷)도 트랜잭션 밖에서 한다
            AiReferenceDraftRequest request = target.request().withAttachments(loadAttachments(target.files()));
            String content = aiClient.generateReferenceDraft(request);
            transactionTemplate.executeWithoutResult(status -> findDraft(aiDraftId).succeed(content));
        } catch (Exception e) {
            log.error("AI 작성 글 생성 실패: aiDraftId={}", aiDraftId, e);
            transactionTemplate.executeWithoutResult(status -> {
                findDraft(aiDraftId).fail();
                waitingAnalyses(aiDraftId).forEach(ComparisonAnalysis::fail);
            });
            return;
        }

        // AI 작성 글 생성 중에 제출된 초안이 있으면 지금 분석 시작
        List<Long> waitingIds = transactionTemplate.execute(status ->
                waitingAnalyses(aiDraftId).stream().map(ComparisonAnalysis::getId).toList());
        // AI 서버에 아직 제출된 적 없는 분석들이라 새로 제출
        waitingIds.forEach(id -> comparisonAnalysisGenerator.generate(id, AnalysisRunMode.SUBMIT));
    }

    private List<ComparisonAnalysis> waitingAnalyses(Long aiDraftId) {
        return comparisonAnalysisRepository.findByAiDraft_IdAndStatus(aiDraftId, ProcessingStatus.PENDING);
    }

    /**
     * 첨부파일 원본을 저장소에서 읽는다. 없거나 못 읽는 파일은 빼고 과제 조건·나머지 파일로 생성을 계속한다.
     * (AI 서버도 글자를 못 뽑는 파일은 경고만 남기고 넘어가므로 같은 방식)
     */
    private List<AiReferenceDraftRequest.Attachment> loadAttachments(List<StoredFile> files) {
        List<AiReferenceDraftRequest.Attachment> attachments = new ArrayList<>();
        for (StoredFile file : files) {
            try {
                attachments.add(new AiReferenceDraftRequest.Attachment(
                        file.fileName(), file.contentType(), fileStorage.load(file.fileUrl())));
            } catch (RuntimeException e) {
                log.warn("첨부파일을 읽지 못해 AI 작성 글 참고 자료에서 제외: fileUrl={}", file.fileUrl(), e);
            }
        }
        return attachments;
    }

    /** PENDING이 아니면 이미 처리 중/완료된 것이므로 null 반환 */
    private GenerationTarget startGenerating(Long aiDraftId) {
        AiReferenceDraft draft = findDraft(aiDraftId);
        if (draft.getStatus() != ProcessingStatus.PENDING) {
            log.info("PENDING 상태가 아니라 생성 건너뜀: aiDraftId={}, status={}", aiDraftId, draft.getStatus());
            return null;
        }
        draft.startGenerating();

        Training training = draft.getTraining();
        AiReferenceDraftRequest request = new AiReferenceDraftRequest(
                training.getId(),
                training.getTopic(),
                training.getWritingType().getLabel(),
                training.getRequirements(),
                training.getTimerMinutes() == null ? null : training.getTimerMinutes().intValue(),
                List.of());
        List<StoredFile> files = trainingAttachmentRepository.findByTraining_IdOrderByIdAsc(training.getId())
                .stream()
                .map(a -> new StoredFile(a.getFileName(), a.getFileUrl(), a.getFileType()))
                .toList();
        return new GenerationTarget(request, files);
    }

    private record GenerationTarget(AiReferenceDraftRequest request, List<StoredFile> files) {
    }

    private record StoredFile(String fileName, String fileUrl, String contentType) {
    }

    private AiReferenceDraft findDraft(Long aiDraftId) {
        return aiReferenceDraftRepository.findById(aiDraftId)
                .orElseThrow(() -> new IllegalStateException("AI 작성 글이 없습니다: " + aiDraftId));
    }
}
