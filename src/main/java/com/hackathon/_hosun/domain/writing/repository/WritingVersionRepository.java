package com.hackathon._hosun.domain.writing.repository;

import com.hackathon._hosun.domain.writing.entity.VersionType;
import com.hackathon._hosun.domain.writing.entity.WritingVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WritingVersionRepository extends JpaRepository<WritingVersion, Long> {

    long countByTraining_IdAndVersionType(Long trainingId, VersionType versionType);

    List<WritingVersion> findByTraining_IdOrderBySeqNoAsc(Long trainingId);

    Optional<WritingVersion> findTopByTraining_IdOrderBySeqNoDesc(Long trainingId);
}
