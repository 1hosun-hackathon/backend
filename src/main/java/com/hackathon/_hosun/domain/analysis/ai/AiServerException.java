package com.hackathon._hosun.domain.analysis.ai;

import lombok.Getter;

/**
 * AI 서버가 돌려준 에러. 본문 {"error": {"code", "message", "retryable"}}를 담는다.
 * 분기는 HTTP 상태가 아니라 code로 판단한다(형식 오류가 400/422로 문서마다 달라서).
 */
@Getter
public class AiServerException extends RuntimeException {

    public static final String ANALYSIS_NOT_FOUND = "ANALYSIS_NOT_FOUND";
    public static final String ANALYSIS_NOT_FAILED = "ANALYSIS_NOT_FAILED";
    public static final String ANALYSIS_NOT_READY = "ANALYSIS_NOT_READY";

    private final int httpStatus;
    private final String code;
    private final boolean retryable;

    public AiServerException(int httpStatus, String code, String message, boolean retryable) {
        super("AI 서버 오류 [" + httpStatus + " " + code + "] " + message);
        this.httpStatus = httpStatus;
        this.code = code;
        this.retryable = retryable;
    }

    public boolean is(String code) {
        return code.equals(this.code);
    }
}
