package com.hackathon._hosun.domain.writing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record TempSaveResponse(

        @Schema(description = "임시 저장 시각")
        LocalDateTime tempSavedAt
) {
}
