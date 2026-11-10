package com.hackathon._hosun.domain.training.repository;

import com.hackathon._hosun.domain.training.entity.Training;
import com.hackathon._hosun.domain.training.entity.TrainingStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrainingRepository extends JpaRepository<Training, Long> {

    long countByUser_IdAndStatusAndDeletedAtIsNull(Long userId, TrainingStatus status);

    Optional<Training> findByIdAndUser_IdAndDeletedAtIsNull(Long id, Long userId);

    /** 상태 전이용. 중복 제출(더블 클릭 등)로 버전·분석이 두 번 생기지 않게 행 잠금 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Training t WHERE t.id = :id AND t.user.id = :userId AND t.deletedAt IS NULL")
    Optional<Training> findForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    List<Training> findByUser_IdAndStatusAndDeletedAtIsNullOrderByCompletedAtDesc(
            Long userId, TrainingStatus status, Pageable pageable);

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
