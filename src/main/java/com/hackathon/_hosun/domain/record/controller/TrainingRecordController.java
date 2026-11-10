package com.hackathon._hosun.domain.record.controller;

import com.hackathon._hosun.domain.record.dto.HomeResponse;
import com.hackathon._hosun.domain.record.dto.TrainingDetailResponse;
import com.hackathon._hosun.domain.record.dto.TrainingListResponse;
import com.hackathon._hosun.domain.record.service.TrainingRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@Tag(name = "기록·대시보드", description = "훈련 기록 목록/상세/삭제, 홈 요약 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class TrainingRecordController {

    private final TrainingRecordService trainingRecordService;

    @Operation(summary = "훈련 기록 목록", description = "status(ALL 기본/COMPLETED/IN_PROGRESS)로 필터링, 최근 날짜순 페이지 조회.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (status 값 오류)")
    })
    @GetMapping("/api/v1/trainings")
    public TrainingListResponse list(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "ALL(기본) | COMPLETED | IN_PROGRESS") @RequestParam(required = false) String status,
            Pageable pageable) {
        return trainingRecordService.list(userId, status, pageable);
    }

    @Operation(summary = "훈련 상세", description = "COMPLETED 상태의 훈련만 조회 가능.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "TRAINING_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "TRAINING_INVALID_STATUS")
    })
    @GetMapping("/api/v1/trainings/{trainingId}/detail")
    public TrainingDetailResponse detail(
            @AuthenticationPrincipal Long userId, @PathVariable Long trainingId) {
        return trainingRecordService.detail(userId, trainingId);
    }

    @Operation(summary = "훈련 삭제", description = "deleted_at을 기록한다. 목록·대시보드 집계에서 제외된다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "TRAINING_NOT_FOUND")
    })
    @DeleteMapping("/api/v1/trainings/{trainingId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId, @PathVariable Long trainingId) {
        trainingRecordService.delete(userId, trainingId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "홈", description = "자기 사고 구성 비율 추이(최근 3회 평균, 첫/최근 완료 훈련 값)를 반환한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/api/v1/home")
    public HomeResponse home(@AuthenticationPrincipal Long userId) {
        return trainingRecordService.getHome(userId);
    }
}
