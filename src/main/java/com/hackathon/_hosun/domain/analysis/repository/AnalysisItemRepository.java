package com.hackathon._hosun.domain.analysis.repository;

import com.hackathon._hosun.domain.analysis.entity.AnalysisItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisItemRepository extends JpaRepository<AnalysisItem, Long> {

    List<AnalysisItem> findByAnalysis_Id(Long analysisId);
}
