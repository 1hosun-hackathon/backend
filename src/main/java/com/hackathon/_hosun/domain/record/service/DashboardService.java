package com.hackathon._hosun.domain.record.service;

import com.hackathon._hosun.domain.analysis.entity.AnalysisItem;
import com.hackathon._hosun.domain.analysis.entity.ComparisonAnalysis;
import com.hackathon._hosun.domain.analysis.entity.Criterion;
import com.hackathon._hosun.domain.analysis.repository.AnalysisItemRepository;
import com.hackathon._hosun.domain.record.dto.DashboardResponse;
import com.hackathon._hosun.domain.training.entity.Training;
import com.hackathon._hosun.domain.training.entity.TrainingStatus;
import com.hackathon._hosun.domain.training.repository.TrainingRepository;
import com.hackathon._hosun.domain.writing.entity.VersionType;
import com.hackathon._hosun.domain.writing.repository.WritingVersionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int SELF_THOUGHT_RECENT_COUNT = 3;
    private static final int TREND_COUNT = 10;
    private static final int RECENT_COUNT = 4;

    private final TrainingRepository trainingRepository;
    private final WritingVersionRepository writingVersionRepository;
    private final AnalysisItemRepository analysisItemRepository;
    private final SelfThoughtRatioService selfThoughtRatioService;

    public DashboardResponse getDashboard(Long userId) {
        long completedCount = trainingRepository.countByUser_IdAndStatusAndDeletedAtIsNull(userId, TrainingStatus.COMPLETED);
        if (completedCount == 0) {
            return new DashboardResponse(0, null, 0, List.of(), null, null, null, null, List.of());
        }

        List<Training> recentForRatio = trainingRepository.findByUser_IdAndStatusAndDeletedAtIsNullOrderByCompletedAtDesc(
                userId, TrainingStatus.COMPLETED, PageRequest.of(0, SELF_THOUGHT_RECENT_COUNT));
        BigDecimal selfThoughtRatio = selfThoughtRatioService.average(recentForRatio);

        List<Training> trendSource = trainingRepository.findByUser_IdAndStatusAndDeletedAtIsNullOrderByCompletedAtDesc(
                userId, TrainingStatus.COMPLETED, PageRequest.of(0, TREND_COUNT));
        List<DashboardResponse.TrendItem> trend = trendSource.stream()
                .sorted(Comparator.comparing(Training::getCompletedAt))
                .map(this::toTrendItem)
                .toList();

        Training first = trainingRepository
                .findFirstByUser_IdAndStatusAndDeletedAtIsNullOrderByCompletedAtAsc(userId, TrainingStatus.COMPLETED)
                .orElse(null);
        Training latest = recentForRatio.get(0);

        ComparisonAnalysis firstAnalysis = first == null ? null : selfThoughtRatioService.representativeAnalysis(first);
        ComparisonAnalysis latestAnalysis = selfThoughtRatioService.representativeAnalysis(latest);

        BigDecimal selfThoughtChange = change(firstAnalysis, latestAnalysis, ComparisonAnalysis::getSelfThoughtRatio);
        BigDecimal aiDependencyChange = change(firstAnalysis, latestAnalysis, ComparisonAnalysis::getAiDependencyRatio);

        DashboardResponse.CriterionSummary mostImproved = mostImproved(first, latest);
        DashboardResponse.CriterionSummary focusArea = focusArea(recentForRatio);

        List<DashboardResponse.RecentItem> recent = trainingRepository
                .findByUser_IdAndStatusAndDeletedAtIsNullOrderByCompletedAtDesc(
                        userId, TrainingStatus.COMPLETED, PageRequest.of(0, RECENT_COUNT))
                .stream()
                .map(this::toRecentItem)
                .toList();

        int streakDays = streakDays(userId);

        return new DashboardResponse(
                completedCount, selfThoughtRatio, streakDays, trend,
                aiDependencyChange, selfThoughtChange, mostImproved, focusArea, recent);
    }

    private int streakDays(Long userId) {
        List<Training> completed = trainingRepository.findByUser_IdAndStatusAndDeletedAtIsNull(userId, TrainingStatus.COMPLETED);
        Set<LocalDate> completedDates = completed.stream()
                .map(t -> t.getCompletedAt().toLocalDate())
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now(KST);
        LocalDate cursor = completedDates.contains(today) ? today : today.minusDays(1);
        if (!completedDates.contains(cursor)) {
            return 0;
        }

        int streak = 0;
        while (completedDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private DashboardResponse.CriterionSummary mostImproved(Training first, Training latest) {
        if (first == null || first.getId().equals(latest.getId())) {
            return null;
        }
        Map<Criterion, Integer> firstScores = scoresByCriterion(first);
        Map<Criterion, Integer> latestScores = scoresByCriterion(latest);

        Criterion best = null;
        int bestDelta = Integer.MIN_VALUE;
        for (Criterion criterion : Criterion.values()) {
            Integer firstScore = firstScores.get(criterion);
            Integer latestScore = latestScores.get(criterion);
            if (firstScore == null || latestScore == null) {
                continue;
            }
            int delta = latestScore - firstScore;
            if (delta > bestDelta) {
                bestDelta = delta;
                best = criterion;
            }
        }
        return best == null ? null : new DashboardResponse.CriterionSummary(best, label(best));
    }

    private DashboardResponse.CriterionSummary focusArea(List<Training> recentTrainings) {
        Map<Criterion, List<Integer>> scoresByCriterion = new EnumMap<>(Criterion.class);
        for (Training training : recentTrainings) {
            scoresByCriterion(training).forEach((criterion, score) ->
                    scoresByCriterion.computeIfAbsent(criterion, k -> new java.util.ArrayList<>()).add(score));
        }

        Criterion worst = null;
        double worstAverage = Double.MAX_VALUE;
        for (Map.Entry<Criterion, List<Integer>> entry : scoresByCriterion.entrySet()) {
            double average = entry.getValue().stream().mapToInt(Integer::intValue).average().orElse(0);
            if (average < worstAverage) {
                worstAverage = average;
                worst = entry.getKey();
            }
        }
        return worst == null ? null : new DashboardResponse.CriterionSummary(worst, label(worst));
    }

    private Map<Criterion, Integer> scoresByCriterion(Training training) {
        ComparisonAnalysis analysis = selfThoughtRatioService.representativeAnalysis(training);
        if (analysis == null) {
            return Map.of();
        }
        return analysisItemRepository.findByAnalysis_Id(analysis.getId()).stream()
                .collect(Collectors.toMap(AnalysisItem::getCriterion, AnalysisItem::getUserScore));
    }

    private BigDecimal change(
            ComparisonAnalysis first, ComparisonAnalysis latest, Function<ComparisonAnalysis, BigDecimal> extractor) {
        if (first == null || latest == null) {
            return null;
        }
        BigDecimal firstValue = extractor.apply(first);
        BigDecimal latestValue = extractor.apply(latest);
        if (firstValue == null || latestValue == null) {
            return null;
        }
        return latestValue.subtract(firstValue).setScale(2, RoundingMode.HALF_UP);
    }

    private DashboardResponse.TrendItem toTrendItem(Training training) {
        ComparisonAnalysis analysis = selfThoughtRatioService.representativeAnalysis(training);
        BigDecimal selfThoughtRatio = analysis == null ? null : analysis.getSelfThoughtRatio();
        BigDecimal aiDependencyRatio = analysis == null ? null : analysis.getAiDependencyRatio();
        return new DashboardResponse.TrendItem(training.getId(), training.getCompletedAt(), selfThoughtRatio, aiDependencyRatio);
    }

    private DashboardResponse.RecentItem toRecentItem(Training training) {
        long recheckCount = writingVersionRepository.countByTraining_IdAndVersionType(training.getId(), VersionType.REVISION);
        return new DashboardResponse.RecentItem(training.getId(), training.getTopic(), training.getCompletedAt(), recheckCount);
    }

    private String label(Criterion criterion) {
        return switch (criterion) {
            case THESIS -> "논지";
            case EVIDENCE -> "근거";
            case STRUCTURE -> "구조";
            case EXPRESSION -> "표현";
            case ORIGINALITY -> "독창성";
        };
    }
}
