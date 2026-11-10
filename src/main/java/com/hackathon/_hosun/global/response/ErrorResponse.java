package com.hackathon._hosun.global.response;

import com.hackathon._hosun.global.exception.ErrorCode;
import java.util.List;
import lombok.Getter;

@Getter
public class ErrorResponse {

    private final String code;
    private final String message;
    private final List<FieldError> errors;

    //공통 에러 응답 포맷 api명세서에는 rsdata로 묶여있지 않아서 에러코드만 묶기 위한 포맷
    private ErrorResponse(String code, String message, List<FieldError> errors) {
        this.code = code;
        this.message = message;
        this.errors = errors;
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.name(), message, List.of());
    }

    public static ErrorResponse ofValidation(ErrorCode errorCode, List<FieldError> errors) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), errors);
    }

    @Getter
    public static class FieldError {
        private final String field;
        private final String reason;

        public FieldError(String field, String reason) {
            this.field = field;
            this.reason = reason;
        }
    }
}
