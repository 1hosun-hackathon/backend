package com.hackathon._hosun.domain.analysis.service;

import com.hackathon._hosun.domain.analysis.ai.AiClient;
import com.hackathon._hosun.domain.analysis.ai.AiReferenceDraftRequest;
import com.hackathon._hosun.domain.analysis.entity.AiReferenceDraft;
import com.hackathon._hosun.domain.analysis.entity.ProcessingStatus;
import com.hackathon._hosun.domain.analysis.event.AiReferenceDraftRequestedEvent;
import com.hackathon._hosun.domain.analysis.repository.AiReferenceDraftRepository;
import com.hackathon._hosun.domain.training.entity.Training;
import com.hackathon._hosun.domain.training.repository.TrainingAttachmentRepository;
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
 * 분석 재시도(4-4)에서 AI 작성 글부터 다시 만들어야 하면 generate()를 호출하면 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiReferenceDraftGenerator {

    private final AiReferenceDraftRepository aiReferenceDraftRepository;
    private final TrainingAttachmentRepository trainingAttachmentRepository;
    private final AiClient aiClient;
    private final TransactionTemplate transactionTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequested(AiReferenceDraftRequestedEvent event) {
        generate(event.aiDraftId());
    }

    public void generate(Long aiDraftId) {
        AiReferenceDraftRequest request = transactionTemplate.execute(status -> startGenerating(aiDraftId));
        if (request == null) {
            return;
        }

        try {
            String content = aiClient.generateReferenceDraft(request);
            transactionTemplate.executeWithoutResult(status -> findDraft(aiDraftId).succeed(content));
        } catch (Exception e) {
            log.error("AI 작성 글 생성 실패: aiDraftId={}", aiDraftId, e);
            transactionTemplate.executeWithoutResult(status -> findDraft(aiDraftId).fail());
        }
    }

    /** PENDING이 아니면 이미 처리 중/완료된 것이므로 null 반환 */
    private AiReferenceDraftRequest startGenerating(Long aiDraftId) {
        AiReferenceDraft draft = findDraft(aiDraftId);
        if (draft.getStatus() != ProcessingStatus.PENDING) {
            log.info("PENDING 상태가 아니라 생성 건너뜀: aiDraftId={}, status={}", aiDraftId, draft.getStatus());
            return null;
        }
        draft.startGenerating();

        Training training = draft.getTraining();
        var attachments = trainingAttachmentRepository.findByTraining_IdOrderByIdAsc(training.getId()).stream()
                .map(a -> new AiReferenceDraftRequest.Attachment(a.getFileName(), a.getFileType(), a.getFileUrl()))
                .toList();
        return new AiReferenceDraftRequest(
                training.getId(),
                training.getTopic(),
                training.getWritingType().getLabel(),
                training.getRequirements(),
                attachments);
    }

    private AiReferenceDraft findDraft(Long aiDraftId) {
        return aiReferenceDraftRepository.findById(aiDraftId)
                .orElseThrow(() -> new IllegalStateException("AI 작성 글이 없습니다: " + aiDraftId));
    }
}
