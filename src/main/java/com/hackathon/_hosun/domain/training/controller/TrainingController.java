package com.hackathon._hosun.domain.training.controller;

import com.hackathon._hosun.domain.training.dto.TrainingCreateRequest;
import com.hackathon._hosun.domain.training.dto.TrainingCreateResponse;
import com.hackathon._hosun.domain.training.dto.TrainingWorkspaceResponse;
import com.hackathon._hosun.domain.training.service.TrainingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "훈련", description = "훈련 생성, 훈련 작업 정보 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/trainings")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingService trainingService;

    @Operation(
            summary = "훈련 생성",
            description = "multipart/form-data. request 파트(JSON) + files 파트(0~5개, 각 10MB 이하, PDF·PPT·문서·이미지). "
                    + "생성 직후 과제 조건과 첨부파일(글자를 뽑을 수 있는 PDF·DOCX·PPTX·HWPX·TXT)로 AI 작성 글을 백그라운드에서 생성한다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    encoding = @Encoding(name = "request", contentType = MediaType.APPLICATION_JSON_VALUE))))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공 (status = DRAFTING)"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR / FILE_TYPE_NOT_ALLOWED"),
            @ApiResponse(responseCode = "413", description = "FILE_TOO_LARGE")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TrainingCreateResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestPart("request") TrainingCreateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        TrainingCreateResponse response = trainingService.create(userId, request, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "훈련 작업 정보",
            description = "초안 작성·다시 쓰기 화면 진입 시 호출. editorContent는 임시 저장본 > 최근 제출본 > 빈 값 순으로 채운다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "TRAINING_NOT_FOUND")
    })
    @GetMapping("/{trainingId}")
    public TrainingWorkspaceResponse getWorkspace(
            @AuthenticationPrincipal Long userId, @PathVariable Long trainingId) {
        return trainingService.getWorkspace(userId, trainingId);
    }
}
