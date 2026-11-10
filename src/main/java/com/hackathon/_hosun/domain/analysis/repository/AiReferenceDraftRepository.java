package com.hackathon._hosun.domain.analysis.repository;

import com.hackathon._hosun.domain.analysis.entity.AiReferenceDraft;
import com.hackathon._hosun.domain.analysis.entity.ProcessingStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiReferenceDraftRepository extends JpaRepository<AiReferenceDraft, Long> {

    Optional<AiReferenceDraft> findTopByTraining_IdOrderByIdDesc(Long trainingId);

    List<AiReferenceDraft> findByStatusIn(Collection<ProcessingStatus> statuses);

    /** 4-4 재시도용. 같은 AI 작성 글을 쓰는 분석 여러 개를 동시에 재시도해도 AI 작성 글은 한 번만 재생성 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM AiReferenceDraft d WHERE d.id = :id")
    Optional<AiReferenceDraft> findForUpdate(@Param("id") Long id);
}
