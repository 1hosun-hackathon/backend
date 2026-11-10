package com.hackathon._hosun.domain.analysis.ai;

import java.net.http.HttpClient;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * FastAPI AI 서버 호출 구현체. ai.client=http 일 때 StubAiClient 대신 사용된다.
 * 요청/응답 JSON은 AiReferenceDraftRequest·AiComparisonRequest·AiComparisonResult 필드를 camelCase 그대로 쓴다.
 * 4xx/5xx·타임아웃·빈 응답은 예외로 던져지고, 호출한 쪽(Generator)에서 FAILED로 기록한다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.client", havingValue = "http")
public class HttpAiClient implements AiClient {

    private final RestClient restClient;
    private final String referenceDraftPath;
    private final String comparisonPath;

    public HttpAiClient(
            @Value("${ai.http.base-url}") String baseUrl,
            @Value("${ai.http.connect-timeout}") Duration connectTimeout,
            @Value("${ai.http.read-timeout}") Duration readTimeout,
            @Value("${ai.http.reference-draft-path}") String referenceDraftPath,
            @Value("${ai.http.comparison-path}") String comparisonPath) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout); // LLM 응답이 느릴 수 있어서 넉넉하게. 넘으면 FAILED
        // Boot 4는 RestClient.Builder 빈이 별도 starter에 있어서 직접 생성 (spring-web만으로 동작)
        // Buffering: 본문을 chunked 대신 Content-Length로 보내서 프록시·서버 호환성 확보
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new BufferingClientHttpRequestFactory(requestFactory))
                .build();
        this.referenceDraftPath = referenceDraftPath;
        this.comparisonPath = comparisonPath;
    }

    @Override
    public String generateReferenceDraft(AiReferenceDraftRequest request) {
        ReferenceDraftResponse response = post(referenceDraftPath, request, ReferenceDraftResponse.class);
        if (response.content() == null || response.content().isBlank()) {
            throw new IllegalStateException("AI 서버가 빈 AI 작성 글을 반환했습니다.");
        }
        return response.content();
    }

    @Override
    public AiComparisonResult analyzeComparison(AiComparisonRequest request) {
        return post(comparisonPath, request, AiComparisonResult.class);
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        T response = restClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(responseType);
        if (response == null) {
            throw new IllegalStateException("AI 서버 응답이 비어 있습니다: " + path);
        }
        return response;
    }

    /** POST {reference-draft-path} 응답: { "content": "..." } */
    record ReferenceDraftResponse(String content) {
    }
}
