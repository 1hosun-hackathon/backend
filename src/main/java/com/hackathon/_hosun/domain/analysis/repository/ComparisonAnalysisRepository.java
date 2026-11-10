package com.hackathon._hosun.domain.analysis.repository;

import com.hackathon._hosun.domain.analysis.dto.AnalysisStatusResponse;
import com.hackathon._hosun.domain.analysis.dto.AnalysisSummaryResponse;
import com.hackathon._hosun.domain.analysis.entity.ComparisonAnalysis;
import com.hackathon._hosun.domain.analysis.entity.ProcessingStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComparisonAnalysisRepository extends JpaRepository<ComparisonAnalysis, Long> {

    Optional<ComparisonAnalysis> findByVersion_Id(Long versionId);

    Optional<ComparisonAnalysis> findTopByVersion_Training_IdOrderByCreatedAtDesc(Long trainingId);

    List<ComparisonAnalysis> findByAiDraft_IdAndStatus(Long aiDraftId, ProcessingStatus status);

    List<ComparisonAnalysis> findByStatusIn(Collection<ProcessingStatus> statuses);

    // ===== 4번 분석 API: 모두 본인 소유 + 삭제되지 않은 훈련의 분석만 조회 =====

    /** 4-1 폴링용(2초 간격). 글 본문 없이 상태만 조회 */
    @Query("""
            SELECT new com.hackathon._hosun.domain.analysis.dto.AnalysisStatusResponse(
                       a.id, a.status, t.status)
            FROM ComparisonAnalysis a JOIN a.version v JOIN v.training t
            WHERE a.id = :analysisId AND t.user.id = :userId AND t.deletedAt IS NULL
            """)
    Optional<AnalysisStatusResponse> findStatusView(@Param("analysisId") Long analysisId, @Param("userId") Long userId);

    /** 4-2 결과 조회용. 내 글·AI 작성 글·훈련까지 한 번에 */
    @Query("""
            SELECT a FROM ComparisonAnalysis a
            JOIN FETCH a.version v JOIN FETCH v.training t JOIN FETCH a.aiDraft
            WHERE a.id = :analysisId AND t.user.id = :userId AND t.deletedAt IS NULL
            """)
    Optional<ComparisonAnalysis> findOwned(@Param("analysisId") Long analysisId, @Param("userId") Long userId);

    /** 4-4 재시도용. 중복 클릭으로 두 번 실행되지 않게 분석 행 잠금 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a FROM ComparisonAnalysis a JOIN a.version v JOIN v.training t
            WHERE a.id = :analysisId AND t.user.id = :userId AND t.deletedAt IS NULL
            """)
    Optional<ComparisonAnalysis> findOwnedForUpdate(@Param("analysisId") Long analysisId, @Param("userId") Long userId);

    /** 4-3 회차별 목록 (seqNo 오름차순) */
    @Query("""
            SELECT new com.hackathon._hosun.domain.analysis.dto.AnalysisSummaryResponse(
                       a.id, v.seqNo, a.status, a.createdAt)
            FROM ComparisonAnalysis a JOIN a.version v
            WHERE v.training.id = :trainingId
            ORDER BY v.seqNo ASC
            """)
    List<AnalysisSummaryResponse> findSummariesByTrainingId(@Param("trainingId") Long trainingId);
}
