package com.hackathon._hosun.domain.analysis.ai;

import java.util.List;

/** AI 작성 글 생성 요청. 필요한 값이 더 있으면 AI 담당자와 맞춰서 필드 추가 */
public record AiReferenceDraftRequest(
        Long trainingId,
        String topic,
        String writingType,
        String requirements,
        List<Attachment> attachments
) {

    public record Attachment(String fileName, String fileType, String fileUrl) {
    }
}
