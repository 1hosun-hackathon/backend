package com.hackathon._hosun.domain.analysis.event;

/** 비교 분석을 AI 서버에 어떻게 실행할지 */
public enum AnalysisRunMode {
    /** 새로 제출 (3-2 초안 제출, 3-4 다시 검사) */
    SUBMIT,
    /** 실패한 분석 재시도 (4-4). AI 서버 /retry 사용 */
    RETRY,
    /** 서버 재시작으로 끊긴 분석을 이어서 대기 */
    RESUME
}
