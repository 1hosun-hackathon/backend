package com.hackathon._hosun.domain.auth.controller;

import com.hackathon._hosun.domain.auth.dto.CheckLoginIdResponse;
import com.hackathon._hosun.domain.auth.dto.LoginRequest;
import com.hackathon._hosun.domain.auth.dto.LoginResponse;
import com.hackathon._hosun.domain.auth.dto.ReissueResponse;
import com.hackathon._hosun.domain.auth.dto.SignupRequest;
import com.hackathon._hosun.domain.auth.dto.SignupResponse;
import com.hackathon._hosun.domain.auth.service.AuthService;
import com.hackathon._hosun.global.exception.BusinessException;
import com.hackathon._hosun.global.exception.ErrorCode;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/check-login-id")
    public CheckLoginIdResponse checkLoginId(@RequestParam String loginId) {
        return authService.checkLoginId(loginId);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request);

        LoginResponse body = new LoginResponse(result.accessToken(), result.expiresIn(), result.loginId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie(result.refreshToken()).toString())
                .body(body);
    }

    @PostMapping("/reissue")
    public ResponseEntity<ReissueResponse> reissue(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
        if (refreshToken == null) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        AuthService.ReissueResult result = authService.reissue(refreshToken);

        ReissueResponse body = new ReissueResponse(result.accessToken(), result.expiresIn());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie(result.refreshToken()).toString())
                .body(body);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Long userId) {
        authService.logout(userId);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshTokenCookie().toString())
                .build();
    }

    private ResponseCookie refreshTokenCookie(String value) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofDays(AuthService.REFRESH_TOKEN_EXPIRE_DAYS))
                .build();
    }

    private ResponseCookie expiredRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }
}
