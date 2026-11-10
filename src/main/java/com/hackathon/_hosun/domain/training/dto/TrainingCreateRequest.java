package com.hackathon._hosun.domain.training.dto;

import com.hackathon._hosun.domain.training.entity.WritingType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TrainingCreateRequest(

        @Schema(description = "글 주제 (200자 이하)", example = "기후변화와 탄소 중립 정책의 실효성")
        @NotBlank(message = "주제를 입력해주세요.")
        @Size(max = 200, message = "주제는 200자 이하여야 합니다.")
        String topic,

        @Schema(description = "글 유형", example = "논설문")
        @NotNull(message = "글 유형을 선택해주세요.")
        WritingType writingType,

        @Schema(description = "요구사항", example = "800~1,000자, 근거 2개 이상")
        String requirements,

        @Schema(description = "타이머(분). 없으면 타이머 미사용", example = "15")
        @Positive(message = "타이머는 1분 이상이어야 합니다.")
        Short timerMinutes
) {
}
