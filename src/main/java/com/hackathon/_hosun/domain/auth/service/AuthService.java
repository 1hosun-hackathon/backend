package com.hackathon._hosun.domain.auth.service;

import com.hackathon._hosun.domain.auth.dto.CheckLoginIdResponse;
import com.hackathon._hosun.domain.auth.dto.LoginRequest;
import com.hackathon._hosun.domain.auth.dto.SignupRequest;
import com.hackathon._hosun.domain.auth.dto.SignupResponse;
import com.hackathon._hosun.domain.user.entity.User;
import com.hackathon._hosun.domain.user.repository.UserRepository;
import com.hackathon._hosun.global.exception.BusinessException;
import com.hackathon._hosun.global.exception.ErrorCode;
import com.hackathon._hosun.global.security.jwt.JwtTokenProvider;
import com.hackathon._hosun.global.security.jwt.RefreshTokenProvider;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    // ponytail: 명세서에 refresh token 만료 기간이 없어 14일로 가정. 바뀌면 여기만 수정
    public static final long REFRESH_TOKEN_EXPIRE_DAYS = 14;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenProvider refreshTokenProvider;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        User user = User.create(request.loginId(), passwordEncoder.encode(request.password()));
        User saved = userRepository.save(user);
        return new SignupResponse(saved.getId(), saved.getLoginId());
    }

    public CheckLoginIdResponse checkLoginId(String loginId) {
        return new CheckLoginIdResponse(!userRepository.existsByLoginId(loginId));
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        User user = userRepository.findByLoginIdAndDeletedAtIsNull(request.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getLoginId());
        String refreshToken = refreshTokenProvider.generate();
        user.updateRefreshToken(
                refreshTokenProvider.hash(refreshToken),
                LocalDateTime.now().plusDays(REFRESH_TOKEN_EXPIRE_DAYS));

        return new LoginResult(
                accessToken,
                jwtTokenProvider.getAccessTokenExpireSeconds(),
                user.getLoginId(),
                refreshToken);
    }

    @Transactional
    public ReissueResult reissue(String refreshToken) {
        User user = userRepository.findByRefreshTokenHash(refreshTokenProvider.hash(refreshToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (user.getRefreshTokenExpiresAt() == null
                || user.getRefreshTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getLoginId());
        String newRefreshToken = refreshTokenProvider.generate();
        user.updateRefreshToken(
                refreshTokenProvider.hash(newRefreshToken),
                LocalDateTime.now().plusDays(REFRESH_TOKEN_EXPIRE_DAYS));

        return new ReissueResult(newAccessToken, jwtTokenProvider.getAccessTokenExpireSeconds(), newRefreshToken);
    }

    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        user.clearRefreshToken();
    }

    public record LoginResult(String accessToken, long expiresIn, String loginId, String refreshToken) {
    }

    public record ReissueResult(String accessToken, long expiresIn, String refreshToken) {
    }
}
