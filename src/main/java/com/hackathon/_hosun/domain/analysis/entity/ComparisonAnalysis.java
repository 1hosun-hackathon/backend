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

    @Column(name = "overall_summary", columnDefinition = "TEXT")
    private String overallSummary;

    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    @Column(name = "improvements", columnDefinition = "TEXT")
    private String improvements;

    @Column(name = "self_thought_ratio", precision = 5, scale = 2)
    private BigDecimal selfThoughtRatio;

    @Column(name = "ai_dependency_ratio", precision = 5, scale = 2)
    private BigDecimal aiDependencyRatio;

    /** AI 서버 4-2 응답 원본 JSON. 다음 회차 재검사의 previousResult로 그대로 보낸다 */
    @Column(name = "raw_result", columnDefinition = "LONGTEXT")
    private String rawResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ProcessingStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static ComparisonAnalysis createPending(WritingVersion version, AiReferenceDraft aiDraft) {
        ComparisonAnalysis analysis = new ComparisonAnalysis();
        analysis.version = version;
        analysis.aiDraft = aiDraft;
        analysis.status = ProcessingStatus.PENDING;
        analysis.createdAt = LocalDateTime.now();
        return analysis;
    }

    /** 서버 재시작 복구·재시도용: 다시 처리 대기 상태로 되돌림 */
    public void resetToPending() {
        this.status = ProcessingStatus.PENDING;
    }

    public void startGenerating() {
        this.status = ProcessingStatus.GENERATING;
    }

    public void succeed(String overallSummary, String strengths, String improvements, String rawResult) {
        this.overallSummary = overallSummary;
        this.strengths = strengths;
        this.improvements = improvements;
        this.rawResult = rawResult;
        this.status = ProcessingStatus.SUCCESS;
    }

    /** 3-5 훈련 완료 시 마지막 분석에 자기 사고 비율·AI 의존도를 기록한다 (/home·/dashboard가 읽음) */
    public void recordRatios(BigDecimal selfThoughtRatio, BigDecimal aiDependencyRatio) {
        this.selfThoughtRatio = selfThoughtRatio;
        this.aiDependencyRatio = aiDependencyRatio;
    }

    public void fail() {
        this.status = ProcessingStatus.FAILED;
    }
}
