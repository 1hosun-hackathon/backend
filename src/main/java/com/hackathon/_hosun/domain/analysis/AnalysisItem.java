package com.hackathon._hosun.domain.analysis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
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

    @Lob
    @Column(name = "user_summary")
    private String userSummary;

    @Lob
    @Column(name = "ai_summary")
    private String aiSummary;

    @Lob
    @Column(name = "comparison")
    private String comparison;

    @Lob
    @Column(name = "reflect_point")
    private String reflectPoint;

    @Column(name = "user_score")
    private Integer userScore;
}
