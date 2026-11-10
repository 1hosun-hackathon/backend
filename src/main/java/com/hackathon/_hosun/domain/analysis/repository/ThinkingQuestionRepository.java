package com.hackathon._hosun.domain.analysis.repository;

import com.hackathon._hosun.domain.analysis.entity.ThinkingQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThinkingQuestionRepository extends JpaRepository<ThinkingQuestion, Long> {
}
