package com.hackathon._hosun.domain.record.dto;

import com.hackathon._hosun.domain.training.entity.TrainingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record TrainingListResponse(

        List<Item> items,

        @Schema(description = "현재 페이지(0부터 시작)", example = "0")
        int page,

        @Schema(description = "페이지 크기", example = "10")
        int size,

        @Schema(description = "전체 개수", example = "4")
        long totalElements,

        @Schema(description = "다음 페이지 존재 여부", example = "false")
        boolean hasNext
) {

    public record Item(

            @Schema(description = "훈련 ID", example = "12")
            Long trainingId,

            @Schema(description = "글 주제", example = "환경 보호의 중요성")
            String topic,

            @Schema(description = "글 종류", example = "논설문")
            String writingType,

            @Schema(description = "훈련 상태")
            TrainingStatus status,

            @Schema(description = "대표 날짜(완료일 > 제출일 > 시작일 순)")
            LocalDateTime date,

            @Schema(description = "다시 검사한 횟수", example = "2")
            long recheckCount
    ) {
    }
}
