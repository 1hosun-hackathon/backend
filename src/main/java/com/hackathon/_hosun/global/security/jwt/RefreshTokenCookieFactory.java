package com.hackathon._hosun.global.security.jwt;

import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieFactory {

    public static final String COOKIE_NAME = "refreshToken";

    // ponytail: 명세서에 refresh token 만료 기간이 없어 14일로 가정. 바뀌면 여기만 수정
    public static final long EXPIRE_DAYS = 14;

    public ResponseCookie create(String refreshToken) {
        return build(refreshToken, Duration.ofDays(EXPIRE_DAYS));
    }

    public ResponseCookie createExpired() {
        return build("", Duration.ZERO);
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
