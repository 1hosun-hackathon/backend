package com.hackathon._hosun.domain.training;

import com.hackathon._hosun.domain.user.User;
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

    @Column(name = "writing_type", length = 30, nullable = false)
    private String writingType;

    @Lob
    @Column(name = "requirements")
    private String requirements;

    @Column(name = "timer_minutes")
    private Short timerMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TrainingStatus status;

    @Lob
    @Column(name = "temp_content")
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
}
