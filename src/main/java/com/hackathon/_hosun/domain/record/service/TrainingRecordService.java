package com.hackathon._hosun.domain.record.service;

import com.hackathon._hosun.domain.analysis.entity.ComparisonAnalysis;
import com.hackathon._hosun.domain.analysis.repository.ComparisonAnalysisRepository;
import com.hackathon._hosun.domain.record.dto.HomeResponse;
import com.hackathon._hosun.domain.record.dto.TrainingDetailResponse;
import com.hackathon._hosun.domain.record.dto.TrainingListResponse;
import com.hackathon._hosun.domain.training.entity.Training;
import com.hackathon._hosun.domain.training.entity.TrainingStatus;
import com.hackathon._hosun.domain.training.repository.TrainingRepository;
import com.hackathon._hosun.domain.writing.entity.VersionType;
import com.hackathon._hosun.domain.writing.entity.WritingVersion;
import com.hackathon._hosun.domain.writing.repository.WritingVersionRepository;
import com.hackathon._hosun.global.exception.BusinessException;
import com.hackathon._hosun.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainingRecordService {

    private static final int HOME_RECENT_COUNT = 3;

    private final TrainingRepository trainingRepository;
    private final WritingVersionRepository writingVersionRepository;
    private final ComparisonAnalysisRepository comparisonAnalysisRepository;

    public TrainingListResponse list(Long userId, String status, Pageable pageable) {
        Page<Training> page = switch (normalizeStatus(status)) {
            case "COMPLETED" -> trainingRepository.findAllByUserAndStatus(userId, TrainingStatus.COMPLETED, pageable);
            case "IN_PROGRESS" ->
                    trainingRepository.findAllByUserAndStatusNot(userId, TrainingStatus.COMPLETED, pageable);
            default -> trainingRepository.findAllByUserAndStatus(userId, null, pageable);
        };

        List<TrainingListResponse.Item> items = page.getContent().stream()
                .map(t -> new TrainingListResponse.Item(
                        t.getId(),
                        t.getTopic(),
                        t.getWritingType().getLabel(),
                        t.getStatus(),
                        representativeDate(t),
                        writingVersionRepository.countByTraining_IdAndVersionType(t.getId(), VersionType.REVISION)))
                .toList();

        return new TrainingListResponse(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.hasNext());
    }

    public TrainingDetailResponse detail(Long userId, Long trainingId) { //상세정보
        Training training = findOwnedTraining(userId, trainingId);
        if (training.getStatus() != TrainingStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.TRAINING_INVALID_STATUS);
        }

        List<WritingVersion> versions = writingVersionRepository.findByTraining_IdOrderBySeqNoAsc(trainingId);
        WritingVersion draftVersion = versions.get(0);
        WritingVersion finalVersion = versions.get(versions.size() - 1);
        long recheckCount = writingVersionRepository.countByTraining_IdAndVersionType(trainingId, VersionType.REVISION);

        Long draftAnalysisId = comparisonAnalysisRepository.findByVersion_Id(draftVersion.getId())
                .map(ComparisonAnalysis::getId)
                .orElse(null);

        List<TrainingDetailResponse.RevisionSummary> revisions = versions.stream()
                .filter(v -> v.getSeqNo() > 0 && !v.getId().equals(finalVersion.getId()))
                .map(v -> new TrainingDetailResponse.RevisionSummary(
                        v.getId(),
                        v.getSeqNo(),
                        comparisonAnalysisRepository.findByVersion_Id(v.getId())
                                .map(ComparisonAnalysis::getId)
                                .orElse(null),
                        v.getContent(),
                        v.getCreatedAt()))
                .toList();

        return new TrainingDetailResponse(
                training.getId(),
                training.getTopic(),
                training.getWritingType().getLabel(),
                training.getCompletedAt(),
                recheckCount,
                new TrainingDetailResponse.DraftSummary(draftVersion.getId(), draftVersion.getContent(), draftVersion.getCreatedAt()),
                draftAnalysisId,
                new TrainingDetailResponse.FinalWritingSummary(
                        finalVersion.getId(), finalVersion.getSeqNo(), finalVersion.getContent(), finalVersion.getCreatedAt()),
                revisions);
    }

    @Transactional
    public void delete(Long userId, Long trainingId) { //삭제
        Training training = findOwnedTraining(userId, trainingId);
        training.delete();
    }

    public HomeResponse getHome(Long userId) { //홈화면에서 쓰는 조회, 최근 3개만 가져옴
        List<Training> recentCompleted = trainingRepository.findByUser_IdAndStatusAndDeletedAtIsNullOrderByCompletedAtDesc(
                userId, TrainingStatus.COMPLETED, PageRequest.of(0, HOME_RECENT_COUNT));

        if (recentCompleted.isEmpty()) {
            return new HomeResponse(null, null, null);
        }

        BigDecimal latestRatio = representativeRatio(recentCompleted.get(0));
        BigDecimal firstRatio = trainingRepository
                .findFirstByUser_IdAndStatusAndDeletedAtIsNullOrderByCompletedAtAsc(userId, TrainingStatus.COMPLETED)
                .map(this::representativeRatio)
                .orElse(null);
        BigDecimal selfThoughtRatio = average(recentCompleted);

        return new HomeResponse(selfThoughtRatio, firstRatio, latestRatio);
    }

    private BigDecimal average(List<Training> trainings) {
        List<BigDecimal> ratios = trainings.stream()
                .map(this::representativeRatio)
                .filter(Objects::nonNull)
                .toList();
        if (ratios.isEmpty()) {
            return null;
        }
        BigDecimal sum = ratios.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(ratios.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal representativeRatio(Training training) {
        return comparisonAnalysisRepository.findTopByVersion_Training_IdOrderByCreatedAtDesc(training.getId())
                .map(ComparisonAnalysis::getSelfThoughtRatio)
                .orElse(null);
    }

    private LocalDateTime representativeDate(Training training) {
        if (training.getCompletedAt() != null) {
            return training.getCompletedAt();
        }
        if (training.getDraftSubmittedAt() != null) {
            return training.getDraftSubmittedAt();
        }
        return training.getStartedAt();
    }

    private Training findOwnedTraining(Long userId, Long trainingId) {
        return trainingRepository.findByIdAndUser_IdAndDeletedAtIsNull(trainingId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRAINING_NOT_FOUND));
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank() || status.equals("ALL")) {
            return "ALL";
        }
        if (status.equals("COMPLETED") || status.equals("IN_PROGRESS")) {
            return status;
        }
        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "status는 ALL, COMPLETED, IN_PROGRESS 중 하나여야 합니다.");
    }
}
