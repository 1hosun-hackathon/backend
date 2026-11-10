package com.hackathon._hosun.domain.training.dto;

import com.hackathon._hosun.domain.training.entity.TrainingAttachment;
import io.swagger.v3.oas.annotations.media.Schema;

public record AttachmentResponse(

        @Schema(description = "첨부파일 ID", example = "3")
        Long attachmentId,

        @Schema(description = "원본 파일명", example = "IPCC_요약.pdf")
        String fileName,

        @Schema(description = "파일 크기(byte)", example = "482113")
        Long fileSize
) {

    public static AttachmentResponse from(TrainingAttachment attachment) {
        return new AttachmentResponse(attachment.getId(), attachment.getFileName(), attachment.getFileSize());
    }
}
