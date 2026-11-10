package com.hackathon._hosun.domain.training.dto;

import com.hackathon._hosun.domain.training.entity.TrainingStatus;
import com.hackathon._hosun.domain.training.entity.WritingType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/** 2-2 훈련 작업 정보. 3-3 다시 쓰기 시작 응답도 이 형태를 그대로 쓴다. */
public record TrainingWorkspaceResponse(

        @Schema(description = "훈련 ID", example = "12")
        Long trainingId,

        @Schema(description = "글 주제", example = "기후변화와 탄소 중립 정책의 실효성")
        String topic,

        @Schema(description = "글 유형", example = "논설문")
        WritingType writingType,

        @Schema(description = "요구사항", example = "800~1,000자, 근거 2개 이상")
        String requirements,

        @Schema(description = "훈련 상태", example = "REWRITING")
        TrainingStatus status,

        @Schema(description = "타이머(분)", example = "15")
        Short timerMinutes,

        @Schema(description = "훈련 시작 시각")
        LocalDateTime startedAt,

        @Schema(description = "타이머 종료 시각 (타이머 미설정 시 null)")
        LocalDateTime timerEndsAt,

        @Schema(description = "입력창 초기값: 임시 저장본 > 최근 제출본 > 빈 값")
        String editorContent,

        @Schema(description = "editorContent 출처", example = "TEMP")
        EditorSource editorSource,

        @Schema(description = "임시 저장 시각")
        LocalDateTime tempSavedAt,

        @Schema(description = "최근 제출본 (없으면 null)")
        LatestVersion latestVersion,

        @Schema(description = "최근 비교 분석 ID (없으면 null)", example = "21")
        Long latestAnalysisId,

        @Schema(description = "첨부파일 목록")
        List<AttachmentResponse> attachments
) {

    public enum EditorSource {
        TEMP, VERSION, EMPTY
    }

    public record LatestVersion(
            @Schema(description = "버전 ID", example = "31") Long versionId,
            @Schema(description = "제출 회차 (0 = 초안)", example = "1") Integer seqNo) {
    }
}
