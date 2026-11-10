package com.hackathon._hosun.domain.record.service;

import com.hackathon._hosun.domain.analysis.entity.ComparisonAnalysis;
import com.hackathon._hosun.domain.analysis.repository.ComparisonAnalysisRepository;
import com.hackathon._hosun.domain.training.entity.Training;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 훈련 하나를 대표하는 비교 분석(가장 최근에 생성된 분석)을 찾고,
 * 그 자기 사고 구성 비율을 계산하는 로직. /home, /dashboard가 공통으로 사용.
 */
@Service
@RequiredArgsConstructor
public class SelfThoughtRatioService {

    private final ComparisonAnalysisRepository comparisonAnalysisRepository;

    public ComparisonAnalysis representativeAnalysis(Training training) {
        return comparisonAnalysisRepository
                .findTopByVersion_Training_IdOrderByCreatedAtDesc(training.getId())
                .orElse(null);
    }

    public BigDecimal representativeSelfThoughtRatio(Training training) {
        ComparisonAnalysis analysis = representativeAnalysis(training);
        return analysis == null ? null : analysis.getSelfThoughtRatio();
    }

    public BigDecimal average(List<Training> trainings) {
        List<BigDecimal> ratios = trainings.stream()
                .map(this::representativeSelfThoughtRatio)
                .filter(Objects::nonNull)
                .toList();
        if (ratios.isEmpty()) {
            return null;
        }
        BigDecimal sum = ratios.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(ratios.size()), 2, RoundingMode.HALF_UP);
    }
}
