package com.hackathon._hosun.domain.auth.dto;

public record ReissueResponse(String accessToken, long expiresIn) {
}
