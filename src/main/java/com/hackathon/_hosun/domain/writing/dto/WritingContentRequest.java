package com.hackathon._hosun.domain.writing.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 3-1 임시 저장, 3-2 초안 제출, 3-4 다시 검사하기, 3-5 훈련 완료 공통 요청 */
public record WritingContentRequest(

        @Schema(description = "입력창 내용", example = "탄소 중립 정책은 ...")
        String content
) {
}
