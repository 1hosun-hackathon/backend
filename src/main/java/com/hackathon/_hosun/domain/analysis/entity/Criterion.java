package com.hackathon._hosun.domain.analysis.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 비교 분석 기준. 선언 순서가 4-2 응답 items 순서(명세 고정 순서)다.
 */
@Getter
@RequiredArgsConstructor
public enum Criterion {

    // TODO: description은 명세에 THESIS만 있어서 나머지는 임시 문구. 기획 확정되면 교체
    THESIS("논지", "핵심 주장과 관점"),
    EVIDENCE("근거", "주장을 뒷받침하는 근거와 자료"),
    STRUCTURE("구조", "글의 흐름과 구성"),
    EXPRESSION("표현", "문장과 어휘의 명확성"),
    ORIGINALITY("독창성", "나만의 생각과 시각");

    private final String label;
    private final String description;
}
