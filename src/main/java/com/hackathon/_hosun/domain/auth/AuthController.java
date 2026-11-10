package com.hackathon._hosun.domain.auth;

import com.hackathon._hosun.domain.auth.dto.CheckLoginIdResponse;
import com.hackathon._hosun.domain.auth.dto.LoginRequest;
import com.hackathon._hosun.domain.auth.dto.LoginResponse;
import com.hackathon._hosun.domain.auth.dto.SignupRequest;
import com.hackathon._hosun.domain.auth.dto.SignupResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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

        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, result.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofDays(AuthService.REFRESH_TOKEN_EXPIRE_DAYS))
                .build();

        LoginResponse body = new LoginResponse(result.accessToken(), result.expiresIn(), result.loginId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(body);
    }
}
