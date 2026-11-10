package com.hackathon._hosun.domain.training.repository;

import com.hackathon._hosun.domain.training.entity.Training;
import com.hackathon._hosun.domain.training.entity.TrainingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingRepository extends JpaRepository<Training, Long> {

    long countByUser_IdAndStatusAndDeletedAtIsNull(Long userId, TrainingStatus status);
}
