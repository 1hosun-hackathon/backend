package com.hackathon._hosun.domain.analysis.entity;

import com.hackathon._hosun.domain.writing.entity.WritingVersion;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comparison_analysis")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComparisonAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private WritingVersion version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_draft_id", nullable = false)
    private AiReferenceDraft aiDraft;

    @Lob
    @Column(name = "overall_summary")
    private String overallSummary;

    @Lob
    @Column(name = "strengths")
    private String strengths;

    @Lob
    @Column(name = "improvements")
    private String improvements;

    @Column(name = "self_thought_ratio", precision = 5, scale = 2)
    private BigDecimal selfThoughtRatio;

    @Column(name = "ai_dependency_ratio", precision = 5, scale = 2)
    private BigDecimal aiDependencyRatio;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ProcessingStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
