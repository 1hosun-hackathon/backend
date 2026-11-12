package com.hackathon._hosun.domain.analysis.ai;

import java.util.List;

/**
 * AI 작성 글 생성 요청(2-1).
 * attachments는 AI 서버가 글자를 뽑아 참고 자료로 쓴다(PDF·DOCX·PPTX·HWPX·TXT 등). 못 읽는 형식은 AI 서버가 경고만 남기고 넘어간다.
 */
public record AiReferenceDraftRequest(
        Long trainingId,
        String topic,
        String writingType,
        String requirements,
        Integer timerMinutes,
        List<Attachment> attachments
) {

    public AiReferenceDraftRequest {
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }

    public AiReferenceDraftRequest withAttachments(List<Attachment> attachments) {
        return new AiReferenceDraftRequest(trainingId, topic, writingType, requirements, timerMinutes, attachments);
    }

    /** 저장소에서 읽은 첨부파일 원본 */
    public record Attachment(String fileName, String contentType, byte[] data) {
    }
}
