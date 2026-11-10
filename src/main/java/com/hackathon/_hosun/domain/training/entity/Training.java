package com.hackathon._hosun.domain.training.entity;

import com.hackathon._hosun.domain.user.entity.User;
import com.hackathon._hosun.global.exception.BusinessException;
import com.hackathon._hosun.global.exception.ErrorCode;
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
@Table(name = "training")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Training {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "training_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "topic", length = 200, nullable = false)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(name = "writing_type", length = 30, nullable = false)
    private WritingType writingType;

    @Column(name = "requirements", columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "timer_minutes")
    private Short timerMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TrainingStatus status;

    @Column(name = "temp_content", columnDefinition = "MEDIUMTEXT")
    private String tempContent;

    @Column(name = "temp_saved_at")
    private LocalDateTime tempSavedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "draft_submitted_at")
    private LocalDateTime draftSubmittedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static Training create(
            User user, String topic, WritingType writingType, String requirements, Short timerMinutes) {
        Training training = new Training();
        training.user = user;
        training.topic = topic;
        training.writingType = writingType;
        training.requirements = requirements;
        training.timerMinutes = timerMinutes;
        training.status = TrainingStatus.DRAFTING;
        training.startedAt = LocalDateTime.now();
        return training;
    }

    /** started_at + timer_minutes. 타이머 미설정이면 null */
    public LocalDateTime getTimerEndsAt() {
        if (timerMinutes == null || startedAt == null) {
            return null;
        }
        return startedAt.plusMinutes(timerMinutes);
    }

    /** 3-1 임시 저장. DRAFTING·REWRITING에서만 가능 */
    public void saveTemp(String content) {
        requireStatus(TrainingStatus.DRAFTING, TrainingStatus.REWRITING);
        this.tempContent = content;
        this.tempSavedAt = LocalDateTime.now();
    }

    /** 3-2 초안 제출. DRAFTING → ANALYZING */
    public void submitDraft() {
        requireStatus(TrainingStatus.DRAFTING);
        this.status = TrainingStatus.ANALYZING;
        this.draftSubmittedAt = LocalDateTime.now();
        clearTemp();
    }

    /** 분석 성공 시. ANALYZING → COMPARED (이미 다른 상태면 무시) */
    public void markCompared() {
        if (this.status == TrainingStatus.ANALYZING) {
            this.status = TrainingStatus.COMPARED;
        }
    }

    /** 3-3 다시 쓰기 시작. COMPARED → REWRITING, 이미 REWRITING이면 그대로(멱등) */
    public void startRewriting() {
        requireStatus(TrainingStatus.COMPARED, TrainingStatus.REWRITING);
        this.status = TrainingStatus.REWRITING;
    }

    /** 3-4 다시 검사하기. REWRITING → ANALYZING */
    public void submitRevision() {
        requireStatus(TrainingStatus.REWRITING);
        this.status = TrainingStatus.ANALYZING;
        clearTemp();
    }

    /** 3-5 훈련 완료. REWRITING → COMPLETED */
    public void complete() {
        requireStatus(TrainingStatus.REWRITING);
        this.status = TrainingStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        clearTemp();
    }

    private void clearTemp() {
        this.tempContent = null;
        this.tempSavedAt = null;
    }

    private void requireStatus(TrainingStatus... allowed) {
        for (TrainingStatus s : allowed) {
            if (this.status == s) {
                return;
            }
        }
        throw new BusinessException(ErrorCode.TRAINING_INVALID_STATUS);
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
