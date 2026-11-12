package com.hackathon._hosun.domain.writing.controller;

import com.hackathon._hosun.domain.training.dto.TrainingWorkspaceResponse;
import com.hackathon._hosun.domain.training.service.TrainingService;
import com.hackathon._hosun.domain.writing.dto.SubmissionResponse;
import com.hackathon._hosun.domain.writing.dto.TempSaveResponse;
import com.hackathon._hosun.domain.writing.dto.TrainingCompleteResponse;
import com.hackathon._hosun.domain.writing.dto.WritingContentRequest;
import com.hackathon._hosun.domain.writing.service.WritingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "글 작성", description = "임시 저장, 초안 제출, 다시 쓰기, 다시 검사, 훈련 완료 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/trainings/{trainingId}")
@RequiredArgsConstructor
public class WritingController {

    private final WritingService writingService;
    private final TrainingService trainingService;

    @Operation(summary = "임시 저장", description = "DRAFTING·REWRITING 상태에서 입력창 내용을 1건 덮어써서 저장한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "저장 성공"),
            @ApiResponse(responseCode = "404", description = "TRAINING_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "TRAINING_INVALID_STATUS")
    })
    @PutMapping("/temp")
    public TempSaveResponse saveTemp(
            @AuthenticationPrincipal Long userId, @PathVariable Long trainingId,
            @RequestBody WritingContentRequest request) {
        return writingService.saveTemp(userId, trainingId, request.content());
    }

    @Operation(
            summary = "초안 제출",
            description = "DRAFTING → ANALYZING. 시간과 관계없이 제출 가능. 초안(seqNo 0)과 비교 분석(PENDING)을 만들고 비동기로 분석한다. "
                    + "분석 진행 상황은 4-1 분석 상태 API로 폴링한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "제출 성공, 분석 시작"),
            @ApiResponse(responseCode = "400", description = "EMPTY_CONTENT / VALIDATION_ERROR(3000자 초과)"),
            @ApiResponse(responseCode = "404", description = "TRAINING_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "TRAINING_INVALID_STATUS")
    })
    @PostMapping("/draft")
    public ResponseEntity<SubmissionResponse> submitDraft(
            @AuthenticationPrincipal Long userId, @PathVariable Long trainingId,
            @RequestBody WritingContentRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(writingService.submitDraft(userId, trainingId, request.content()));
    }

    @Operation(
            summary = "다시 쓰기 시작",
            description = "COMPARED → REWRITING. 이미 REWRITING이면 그대로 200(멱등). 응답은 2-2 훈련 작업 정보와 동일.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404", description = "TRAINING_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "TRAINING_INVALID_STATUS")
    })
    @PostMapping("/rewrite")
    public TrainingWorkspaceResponse startRewriting(
            @AuthenticationPrincipal Long userId, @PathVariable Long trainingId) {
        writingService.startRewriting(userId, trainingId);
        return trainingService.getWorkspace(userId, trainingId);
    }

    @Operation(
            summary = "다시 검사하기",
            description = "REWRITING → ANALYZING. 새 수정본과 새 분석을 만든다(AI 작성 글은 재사용). 직전 제출본과 같으면 거절.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "제출 성공, 분석 시작"),
            @ApiResponse(responseCode = "400", description = "EMPTY_CONTENT / VALIDATION_ERROR(3000자 초과)"),
            @ApiResponse(responseCode = "404", description = "TRAINING_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "NO_CHANGES / TRAINING_INVALID_STATUS")
    })
    @PostMapping("/revisions")
    public ResponseEntity<SubmissionResponse> submitRevision(
            @AuthenticationPrincipal Long userId, @PathVariable Long trainingId,
            @RequestBody WritingContentRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(writingService.submitRevision(userId, trainingId, request.content()));
    }

    @Operation(
            summary = "훈련 완료",
            description = "REWRITING → COMPLETED. 입력창 내용을 최종 글로 확정한다. 마지막 제출본과 다르면 분석 없이 수정본 1건을 추가 저장.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "완료"),
            @ApiResponse(responseCode = "400", description = "EMPTY_CONTENT / VALIDATION_ERROR(3000자 초과)"),
            @ApiResponse(responseCode = "404", description = "TRAINING_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "TRAINING_INVALID_STATUS"),
            @ApiResponse(responseCode = "502", description = "AI_SERVER_ERROR (자기 사고 비율 계산 실패, 완료되지 않음)")
    })
    @PostMapping("/complete")
    public TrainingCompleteResponse complete(
            @AuthenticationPrincipal Long userId, @PathVariable Long trainingId,
            @RequestBody WritingContentRequest request) {
        return writingService.complete(userId, trainingId, request.content());
    }
}
