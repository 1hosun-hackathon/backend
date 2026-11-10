package com.hackathon._hosun.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    EMPTY_CONTENT(HttpStatus.BAD_REQUEST, "내용이 비어 있습니다."),
    FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 형식입니다."),

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 리프레시 토큰입니다."),

    TRAINING_NOT_FOUND(HttpStatus.NOT_FOUND, "훈련을 찾을 수 없습니다."),
    ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "분석 결과를 찾을 수 없습니다."),

    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    TRAINING_INVALID_STATUS(HttpStatus.CONFLICT, "현재 상태에서는 수행할 수 없는 작업입니다."),
    NO_CHANGES(HttpStatus.CONFLICT, "변경된 내용이 없습니다."),
    ANALYSIS_NOT_READY(HttpStatus.CONFLICT, "분석이 아직 준비되지 않았습니다."),
    ANALYSIS_NOT_FAILED(HttpStatus.CONFLICT, "실패한 분석만 재시도할 수 있습니다."),

    FILE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "파일 용량이 너무 큽니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
