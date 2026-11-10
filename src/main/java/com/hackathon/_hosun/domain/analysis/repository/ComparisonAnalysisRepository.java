package com.hackathon._hosun.domain.analysis.repository;

import com.hackathon._hosun.domain.analysis.entity.ComparisonAnalysis;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComparisonAnalysisRepository extends JpaRepository<ComparisonAnalysis, Long> {

    Optional<ComparisonAnalysis> findByVersion_Id(Long versionId);

    Optional<ComparisonAnalysis> findTopByVersion_Training_IdOrderByCreatedAtDesc(Long trainingId);
}
