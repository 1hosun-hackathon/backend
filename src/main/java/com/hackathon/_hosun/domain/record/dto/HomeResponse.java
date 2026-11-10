package com.hackathon._hosun.domain.record.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record HomeResponse(

        @Schema(description = "최근 완료 3회 평균 자기 사고 구성 비율(%)", example = "71.0")
        BigDecimal selfThoughtRatio,

        @Schema(description = "첫 완료 훈련의 자기 사고 구성 비율(%)", example = "63.0")
        BigDecimal firstRatio,

        @Schema(description = "최근 완료 훈련의 자기 사고 구성 비율(%)", example = "78.0")
        BigDecimal latestRatio
) {
}
