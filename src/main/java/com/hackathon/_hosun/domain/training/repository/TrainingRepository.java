package com.hackathon._hosun.domain.training.repository;

import com.hackathon._hosun.domain.training.entity.Training;
import com.hackathon._hosun.domain.training.entity.TrainingStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrainingRepository extends JpaRepository<Training, Long> {

    long countByUser_IdAndStatusAndDeletedAtIsNull(Long userId, TrainingStatus status);

    Optional<Training> findByIdAndUser_IdAndDeletedAtIsNull(Long id, Long userId);

    List<Training> findTop3ByUser_IdAndStatusAndDeletedAtIsNullOrderByCompletedAtDesc(
            Long userId, TrainingStatus status);

    Optional<Training> findFirstByUser_IdAndStatusAndDeletedAtIsNullOrderByCompletedAtAsc(
            Long userId, TrainingStatus status);

    @Query("""
            SELECT t FROM Training t
            WHERE t.user.id = :userId
              AND t.deletedAt IS NULL
              AND (:status IS NULL OR t.status = :status)
            ORDER BY COALESCE(t.completedAt, t.draftSubmittedAt, t.startedAt) DESC
            """)
    Page<Training> findAllByUserAndStatus(
            @Param("userId") Long userId, @Param("status") TrainingStatus status, Pageable pageable);

    @Query("""
            SELECT t FROM Training t
            WHERE t.user.id = :userId
              AND t.deletedAt IS NULL
              AND t.status <> :excludedStatus
            ORDER BY COALESCE(t.completedAt, t.draftSubmittedAt, t.startedAt) DESC
            """)
    Page<Training> findAllByUserAndStatusNot(
            @Param("userId") Long userId, @Param("excludedStatus") TrainingStatus excludedStatus, Pageable pageable);
}
