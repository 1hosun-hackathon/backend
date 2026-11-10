package com.hackathon._hosun.domain.analysis.repository;

import com.hackathon._hosun.domain.analysis.entity.AnalysisItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisItemRepository extends JpaRepository<AnalysisItem, Long> {
}
