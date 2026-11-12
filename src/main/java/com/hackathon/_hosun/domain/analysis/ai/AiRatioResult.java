package com.hackathon._hosun.domain.analysis.ai;

import java.math.BigDecimal;

/** 자기 사고 비율·AI 의존도. 둘 다 0~100(%), 합은 100 */
public record AiRatioResult(BigDecimal selfThoughtRatio, BigDecimal aiDependencyRatio) {
}
