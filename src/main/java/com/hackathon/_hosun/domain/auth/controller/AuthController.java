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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "인증·회원", description = "회원가입, 로그인, 토큰 재발급, 로그아웃 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "로그인ID/비밀번호로 회원가입한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "가입 성공"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR / PASSWORD_MISMATCH"),
            @ApiResponse(responseCode = "409", description = "DUPLICATE_LOGIN_ID")
    })
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "아이디 중복 확인", description = "로그인ID 사용 가능 여부를 확인한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/check-login-id")
    public CheckLoginIdResponse checkLoginId(
            @Parameter(description = "중복 확인할 로그인ID", example = "jisu01") @RequestParam String loginId) {
        return authService.checkLoginId(loginId);
    }

    @Operation(summary = "로그인", description = "성공 시 access token은 응답 본문으로, refresh token은 HttpOnly 쿠키로 발급한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "INVALID_CREDENTIALS")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request);

        LoginResponse body = new LoginResponse(result.accessToken(), result.expiresIn(), result.loginId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie(result.refreshToken()).toString())
                .body(body);
    }

    @Operation(
            summary = "토큰 재발급",
            description = "쿠키의 refresh token을 검증한 뒤 access token을 재발급하고 refresh token을 rotation한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "401", description = "INVALID_REFRESH_TOKEN / EXPIRED_REFRESH_TOKEN")
    })
    @PostMapping("/reissue")
    public ResponseEntity<ReissueResponse> reissue(
            @Parameter(hidden = true)
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

    @Operation(summary = "로그아웃", description = "refresh token을 폐기하고 쿠키를 만료시킨다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요 (access token 없음/무효)")
    })
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
