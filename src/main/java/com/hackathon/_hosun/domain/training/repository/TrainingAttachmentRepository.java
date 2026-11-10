package com.hackathon._hosun.domain.training.repository;

import com.hackathon._hosun.domain.training.entity.TrainingAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingAttachmentRepository extends JpaRepository<TrainingAttachment, Long> {

    List<TrainingAttachment> findByTraining_IdOrderByIdAsc(Long trainingId);
}
