package com.hackathon._hosun.global.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon._hosun.global.exception.ErrorCode;
import com.hackathon._hosun.global.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    // Spring Boot 4의 기본 ObjectMapper 빈은 Jackson 3(tools.jackson) 타입이라,
    // Jackson 2 ObjectMapper를 직접 생성해서 씀(이 클래스 안에서만 쓰는 용도라 app 전역 설정과 무관)
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        ErrorCode errorCode = ErrorCode.INVALID_CREDENTIALS;
        response.setStatus(errorCode.getStatus().value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ErrorResponse.of(errorCode)));
    }
}
