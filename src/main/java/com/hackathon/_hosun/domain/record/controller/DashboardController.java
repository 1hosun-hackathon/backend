package com.hackathon._hosun.domain.record.controller;

import com.hackathon._hosun.domain.record.dto.DashboardResponse;
import com.hackathon._hosun.domain.record.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "기록·대시보드", description = "성장 대시보드 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
            summary = "성장 대시보드",
            description = "완료 훈련 수, 자기 사고 구성 비율 추이, 연속 완료 일수, 개선/보완 항목을 반환한다. "
                    + "완료된 훈련이 없으면 빈 상태(0/null/[])로 응답한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/api/v1/dashboard")
    public DashboardResponse dashboard(@AuthenticationPrincipal Long userId) {
        return dashboardService.getDashboard(userId);
    }
}
