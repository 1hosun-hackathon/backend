package com.hackathon._hosun.domain.user.controller;

import com.hackathon._hosun.domain.user.dto.UserMeResponse;
import com.hackathon._hosun.domain.user.service.UserService;
import com.hackathon._hosun.global.security.jwt.RefreshTokenCookieFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증·회원", description = "마이페이지, 회원탈퇴 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    @Operation(summary = "내 정보", description = "로그인한 사용자 본인의 정보를 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요 (access token 없음/무효)")
    })
    @GetMapping("/me")
    public UserMeResponse me(@AuthenticationPrincipal Long userId) {
        return userService.getMe(userId);
    }

    @Operation(summary = "회원탈퇴", description = "deleted_at을 기록하고 refresh token을 폐기한다. 훈련 기록은 보존된다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "탈퇴 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요 (access token 없음/무효)")
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Long userId) {
        userService.withdraw(userId);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.createExpired().toString())
                .build();
    }
}
