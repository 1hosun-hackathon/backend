package com.hackathon._hosun.domain.writing.service;

import com.hackathon._hosun.domain.analysis.ai.AiClient;
import com.hackathon._hosun.domain.analysis.ai.AiRatioRequest;
import com.hackathon._hosun.domain.analysis.ai.AiRatioResult;
import com.hackathon._hosun.domain.analysis.entity.AiReferenceDraft;
import com.hackathon._hosun.domain.analysis.entity.ComparisonAnalysis;
import com.hackathon._hosun.domain.analysis.event.AnalysisRunMode;
import com.hackathon._hosun.domain.analysis.event.ComparisonAnalysisRequestedEvent;
import com.hackathon._hosun.domain.analysis.repository.AiReferenceDraftRepository;
import com.hackathon._hosun.domain.analysis.repository.ComparisonAnalysisRepository;
import com.hackathon._hosun.domain.training.entity.Training;
import com.hackathon._hosun.domain.training.repository.TrainingRepository;
import com.hackathon._hosun.domain.writing.dto.SubmissionResponse;
import com.hackathon._hosun.domain.writing.dto.TempSaveResponse;
import com.hackathon._hosun.domain.writing.dto.TrainingCompleteResponse;
import com.hackathon._hosun.domain.writing.entity.VersionType;
import com.hackathon._hosun.domain.writing.entity.WritingVersion;
import com.hackathon._hosun.domain.writing.repository.WritingVersionRepository;
import com.hackathon._hosun.global.exception.BusinessException;
import com.hackathon._hosun.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WritingService {

    private static final int DRAFT_SEQ_NO = 0;

    private final TrainingRepository trainingRepository;
    private final WritingVersionRepository writingVersionRepository;
    private final ComparisonAnalysisRepository comparisonAnalysisRepository;
    private final AiReferenceDraftRepository aiReferenceDraftRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AiClient aiClient;

    /** 3-1 임시 저장: temp_content 1건 덮어쓰기. 빈 내용도 저장 가능 */
    public TempSaveResponse saveTemp(Long userId, Long trainingId, String content) {
        if (content == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "content는 필수입니다.");
        }
        Training training = findForUpdate(userId, trainingId);
        training.saveTemp(content);
        return new TempSaveResponse(training.getTempSavedAt());
    }

    /** 3-2 초안 제출: DRAFTING → ANALYZING. 초안(seqNo 0) + 분석(PENDING) 생성 후 비동기 분석 */
    public SubmissionResponse submitDraft(Long userId, Long trainingId, String content) {
        requireContent(content);
        Training training = findForUpdate(userId, trainingId);
        training.submitDraft();

        WritingVersion version = writingVersionRepository.save(
                WritingVersion.create(training, VersionType.DRAFT, DRAFT_SEQ_NO, content));
        return requestAnalysis(training, version);
    }

    /** 3-3 다시 쓰기 시작: COMPARED → REWRITING (이미 REWRITING이면 그대로) */
    public void startRewriting(Long userId, Long trainingId) {
        findForUpdate(userId, trainingId).startRewriting();
    }

    /** 3-4 다시 검사하기: REWRITING → ANALYZING. 새 수정본 + 새 분석, AI 작성 글은 재사용 */
    public SubmissionResponse submitRevision(Long userId, Long trainingId, String content) {
        requireContent(content);
        Training training = findForUpdate(userId, trainingId);
        training.submitRevision(); // 상태 검사를 먼저 해서 잘못된 상태면 NO_CHANGES보다 409 TRAINING_INVALID_STATUS 우선

        WritingVersion latest = findLatestVersion(trainingId);
        if (sameContent(latest.getContent(), content)) {
            throw new BusinessException(ErrorCode.NO_CHANGES);
        }

        WritingVersion version = writingVersionRepository.save(
                WritingVersion.create(training, VersionType.REVISION, latest.getSeqNo() + 1, content));
        return requestAnalysis(training, version);
    }

    /**
     * 3-5 훈련 완료: REWRITING → COMPLETED. 마지막 제출본과 다르면 분석 없이 수정본 1건 추가.
     * AI 서버로 자기 사고 비율·AI 의존도를 계산해 마지막 분석에 기록한다(동기, 1초 이내).
     * AI 호출이 실패하면 완료 전체를 롤백하고 502로 응답해서, 완료된 훈련에는 항상 비율이 있게 한다.
     */
    public TrainingCompleteResponse complete(Long userId, Long trainingId, String content) {
        requireContent(content);
        Training training = findForUpdate(userId, trainingId);
        training.complete();

        WritingVersion finalVersion = findLatestVersion(trainingId);
        if (!sameContent(finalVersion.getContent(), content)) {
            finalVersion = writingVersionRepository.save(
                    WritingVersion.create(training, VersionType.REVISION, finalVersion.getSeqNo() + 1, content));
        }
        recordRatios(training, finalVersion);
        return new TrainingCompleteResponse(
                training.getId(), training.getStatus(), finalVersion.getId(), training.getCompletedAt());
    }

    private void recordRatios(Training training, WritingVersion finalVersion) {
        WritingVersion draft = writingVersionRepository.findByTraining_IdAndSeqNo(training.getId(), DRAFT_SEQ_NO)
                .orElseThrow(() -> new IllegalStateException("초안이 없습니다: trainingId=" + training.getId()));
        AiReferenceDraft aiDraft = aiReferenceDraftRepository.findTopByTraining_IdOrderByIdDesc(training.getId())
                .orElseThrow(() -> new IllegalStateException("AI 작성 글이 없습니다: trainingId=" + training.getId()));
        // REWRITING까지 왔으면 마지막 분석은 SUCCESS (분석 성공 시에만 COMPARED → REWRITING)
        ComparisonAnalysis latestAnalysis = comparisonAnalysisRepository
                .findTopByVersion_Training_IdOrderByCreatedAtDesc(training.getId())
                .orElseThrow(() -> new IllegalStateException("비교 분석이 없습니다: trainingId=" + training.getId()));

        AiRatioResult ratio;
        try {
            ratio = aiClient.calculateRatio(new AiRatioRequest(
                    training.getId(), finalVersion.getContent(), finalVersion.getId(),
                    draft.getId(), draft.getContent(), aiDraft.getContent()));
        } catch (Exception e) {
            log.error("자기 사고 비율 계산 실패: trainingId={}", training.getId(), e);
            throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
        }
        latestAnalysis.recordRatios(ratio.selfThoughtRatio(), ratio.aiDependencyRatio());
    }

    private SubmissionResponse requestAnalysis(Training training, WritingVersion version) {
        AiReferenceDraft aiDraft = aiReferenceDraftRepository.findTopByTraining_IdOrderByIdDesc(training.getId())
                .orElseThrow(() -> new IllegalStateException("AI 작성 글이 없습니다: trainingId=" + training.getId()));
        ComparisonAnalysis analysis = comparisonAnalysisRepository.save(
                ComparisonAnalysis.createPending(version, aiDraft));
        // 커밋 이후 비동기로 실행됨 (ComparisonAnalysisGenerator)
        eventPublisher.publishEvent(new ComparisonAnalysisRequestedEvent(analysis.getId(), AnalysisRunMode.SUBMIT));

        return new SubmissionResponse(version.getId(), version.getSeqNo(), analysis.getId(), training.getStatus());
    }

    private Training findForUpdate(Long userId, Long trainingId) {
        return trainingRepository.findForUpdate(trainingId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRAINING_NOT_FOUND));
    }

    private WritingVersion findLatestVersion(Long trainingId) {
        return writingVersionRepository.findTopByTraining_IdOrderBySeqNoDesc(trainingId)
                .orElseThrow(() -> new IllegalStateException("제출본이 없습니다: trainingId=" + trainingId));
    }

    private void requireContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.EMPTY_CONTENT);
        }
    }

    /** 앞뒤 공백·줄바꿈만 다른 건 같은 글로 본다 */
    private boolean sameContent(String a, String b) {
        return a.strip().equals(b.strip());
    }
}
