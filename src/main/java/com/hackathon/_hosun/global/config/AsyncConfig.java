package com.hackathon._hosun.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** AI 작성 글 생성·비교 분석 등 백그라운드 작업용. 실행기는 Spring Boot 기본 applicationTaskExecutor 사용 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
