package com.hackathon._hosun.domain.analysis.repository;

import com.hackathon._hosun.domain.analysis.entity.AiReferenceDraft;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiReferenceDraftRepository extends JpaRepository<AiReferenceDraft, Long> {

    Optional<AiReferenceDraft> findTopByTraining_IdOrderByIdDesc(Long trainingId);
}
