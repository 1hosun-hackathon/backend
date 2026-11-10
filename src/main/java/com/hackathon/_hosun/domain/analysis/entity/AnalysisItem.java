package com.hackathon._hosun.domain.analysis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "analysis_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private ComparisonAnalysis analysis;

    @Enumerated(EnumType.STRING)
    @Column(name = "criterion", length = 20, nullable = false)
    private Criterion criterion;

    @Column(name = "user_summary", columnDefinition = "TEXT")
    private String userSummary;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "comparison", columnDefinition = "TEXT")
    private String comparison;

    @Column(name = "reflect_point", columnDefinition = "TEXT")
    private String reflectPoint;

    @Column(name = "user_score", columnDefinition = "TINYINT")
    private Integer userScore;

    public static AnalysisItem create(
            ComparisonAnalysis analysis, Criterion criterion, String userSummary, String aiSummary,
            String comparison, String reflectPoint, Integer userScore) {
        AnalysisItem item = new AnalysisItem();
        item.analysis = analysis;
        item.criterion = criterion;
        item.userSummary = userSummary;
        item.aiSummary = aiSummary;
        item.comparison = comparison;
        item.reflectPoint = reflectPoint;
        item.userScore = userScore;
        return item;
    }
}
