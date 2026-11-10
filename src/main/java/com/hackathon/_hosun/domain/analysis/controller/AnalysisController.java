package com.hackathon._hosun.domain.analysis.controller;

import com.hackathon._hosun.domain.analysis.dto.AnalysisResultResponse;
import com.hackathon._hosun.domain.analysis.dto.AnalysisRetryResponse;
import com.hackathon._hosun.domain.analysis.dto.AnalysisStatusResponse;
import com.hackathon._hosun.domain.analysis.dto.AnalysisSummaryResponse;
import com.hackathon._hosun.domain.analysis.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "비교 분석", description = "분석 상태 폴링, 분석 결과, 회차별 목록, 재시도 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation(
            summary = "분석 상태",
            description = "초안 제출·다시 검사 후 2초 간격으로 폴링. SUCCESS면 4-2로 결과 조회, FAILED면 4-4로 재시도.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "ANALYSIS_NOT_FOUND")
    })
    @GetMapping("/api/v1/analyses/{analysisId}/status")
    public AnalysisStatusResponse getStatus(
            @AuthenticationPrincipal Long userId, @PathVariable Long analysisId) {
        return analysisService.getStatus(userId, analysisId);
    }

    @Operation(
            summary = "비교 분석 결과",
            description = "내 글·AI 작성 글·기준별 비교·생각해볼 질문. baseline은 재검사(seqNo ≥ 1)일 때만 최초 초안. "
                    + "items는 THESIS, EVIDENCE, STRUCTURE, EXPRESSION, ORIGINALITY 고정 순서. 점수는 포함하지 않음.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "ANALYSIS_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "ANALYSIS_NOT_READY (SUCCESS가 아님)")
    })
    @GetMapping("/api/v1/analyses/{analysisId}")
    public AnalysisResultResponse getResult(
            @AuthenticationPrincipal Long userId, @PathVariable Long analysisId) {
        return analysisService.getResult(userId, analysisId);
    }

    @Operation(summary = "회차별 분석 목록", description = "훈련의 모든 분석을 seqNo 오름차순으로 반환한다 (상태 무관).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "TRAINING_NOT_FOUND")
    })
    @GetMapping("/api/v1/trainings/{trainingId}/analyses")
    public List<AnalysisSummaryResponse> getByTraining(
            @AuthenticationPrincipal Long userId, @PathVariable Long trainingId) {
        return analysisService.getByTraining(userId, trainingId);
    }

    @Operation(
            summary = "분석 재시도",
            description = "FAILED 분석만 가능. 같은 분석을 PENDING으로 되돌려 다시 실행하고, AI 작성 글 생성이 실패했던 거면 그것부터 재생성한다. "
                    + "훈련은 ANALYZING 유지.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "재시도 시작"),
            @ApiResponse(responseCode = "404", description = "ANALYSIS_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "ANALYSIS_NOT_FAILED")
    })
    @PostMapping("/api/v1/analyses/{analysisId}/retry")
    public ResponseEntity<AnalysisRetryResponse> retry(
            @AuthenticationPrincipal Long userId, @PathVariable Long analysisId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(analysisService.retry(userId, analysisId));
    }
}
