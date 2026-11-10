package com.hackathon._hosun.domain.analysis.entity;

import com.hackathon._hosun.domain.training.entity.Training;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_reference_draft")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiReferenceDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_draft_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_id", nullable = false)
    private Training training;

    @Column(name = "content", columnDefinition = "MEDIUMTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ProcessingStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static AiReferenceDraft createPending(Training training) {
        AiReferenceDraft draft = new AiReferenceDraft();
        draft.training = training;
        draft.status = ProcessingStatus.PENDING;
        draft.createdAt = LocalDateTime.now();
        return draft;
    }

    /** 서버 재시작 복구·재시도용: 다시 처리 대기 상태로 되돌림 */
    public void resetToPending() {
        this.status = ProcessingStatus.PENDING;
    }

    public void startGenerating() {
        this.status = ProcessingStatus.GENERATING;
    }

    public void succeed(String content) {
        this.content = content;
        this.status = ProcessingStatus.SUCCESS;
    }

    public void fail() {
        this.status = ProcessingStatus.FAILED;
    }
}
