package com.hackathon._hosun.domain.auth.dto;

public record LoginResponse(String accessToken, long expiresIn, String loginId) {
}
