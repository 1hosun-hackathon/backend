package com.hackathon._hosun.domain.training.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 글 유형. API에서는 한글 라벨("논설문")로 주고받고, DB에는 enum 이름(ARGUMENTATIVE)으로 저장한다.
 */
@Getter
@RequiredArgsConstructor
public enum WritingType {

    // TODO: 카테고리 확정되면 추가. 현재는 명세서 예시에 있는 값만 정의
    ARGUMENTATIVE("논설문");

    @JsonValue
    private final String label;

    @JsonCreator
    public static WritingType fromLabel(String label) {
        return Arrays.stream(values())
                .filter(type -> type.label.equals(label))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 글 유형입니다: " + label));
    }
}
