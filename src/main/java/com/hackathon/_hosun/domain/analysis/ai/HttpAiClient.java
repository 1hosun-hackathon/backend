package com.hackathon._hosun.domain.analysis.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * FastAPI AI 서버 호출 구현체. ai.client=http 일 때 StubAiClient 대신 사용된다.
 * 경로·필드는 AI 저장소 docs/api.md "노션 API 명세 경로"(/api/v1) 기준.
 *
 * 비교 분석은 AI 서버에서 비동기라 제출(202) → 4-1 상태 폴링 → SUCCESS면 4-2 결과 조회 순으로 기다린다.
 * AI 서버는 분석을 메모리에만 1시간 보관하고 재시작하면 잊으므로, ANALYSIS_NOT_FOUND면 같은 analysisId로 다시 제출한다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.client", havingValue = "http")
public class HttpAiClient implements AiClient {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String NGROK_SKIP_WARNING_HEADER = "ngrok-skip-browser-warning";
    private static final int MAX_RESUBMITS = 2;            // 폴링 중 AI 서버가 분석을 잊었을 때 다시 제출하는 최대 횟수
    private static final int MAX_CONSECUTIVE_POLL_ERRORS = 3; // 폴링 중 네트워크 오류를 몇 번까지 넘길지

    private final RestClient restClient;
    private final RestClient referenceDraftClient;
    private final Duration pollInterval;
    private final Duration pollTimeout;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final AiComparisonResultParser parser = new AiComparisonResultParser(jsonMapper);

    @Autowired
    public HttpAiClient(
            @Value("${ai.http.base-url}") String baseUrl,
            @Value("${ai.http.api-key}") String apiKey,
            @Value("${ai.http.connect-timeout}") Duration connectTimeout,
            @Value("${ai.http.read-timeout}") Duration readTimeout,
            @Value("${ai.http.reference-draft-read-timeout}") Duration referenceDraftReadTimeout,
            @Value("${ai.http.poll-interval}") Duration pollInterval,
            @Value("${ai.http.poll-timeout}") Duration pollTimeout) {
        this(
                builder(baseUrl, apiKey, connectTimeout, readTimeout).build(),
                // 2-1은 동기로 수십 초 걸려서 타임아웃을 따로 길게 둔다
                builder(baseUrl, apiKey, connectTimeout, referenceDraftReadTimeout).build(),
                pollInterval,
                pollTimeout);
        if (apiKey.isBlank()) {
            log.warn("ai.http.api-key가 비어 있습니다. AI 서버가 API 키를 요구하면 401로 실패합니다.");
        }
    }

    /** 테스트에서 MockRestServiceServer에 묶인 RestClient를 넣기 위한 생성자 */
    HttpAiClient(RestClient restClient, RestClient referenceDraftClient, Duration pollInterval, Duration pollTimeout) {
        this.restClient = restClient;
        this.referenceDraftClient = referenceDraftClient;
        this.pollInterval = pollInterval;
        this.pollTimeout = pollTimeout;
    }

    static RestClient.Builder builder(String baseUrl, String apiKey, Duration connectTimeout, Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        // Boot 4는 RestClient.Builder 빈이 별도 starter에 있어서 직접 생성 (spring-web만으로 동작)
        // Buffering: 본문을 chunked 대신 Content-Length로 보내서 프록시(ngrok)·서버 호환성 확보
        return configure(RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new BufferingClientHttpRequestFactory(requestFactory)), apiKey);
    }

    /** 공통 헤더·에러 처리. 테스트에서도 같은 설정을 쓰도록 분리 */
    static RestClient.Builder configure(RestClient.Builder builder, String apiKey) {
        return builder
                .defaultHeader(API_KEY_HEADER, apiKey)
                .defaultHeader(NGROK_SKIP_WARNING_HEADER, "true") // ngrok 경고 페이지(HTML) 대신 API 응답 받기
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    throw toAiServerException(response);
                });
    }

    // ===== 2-1 AI 작성 글 생성 =====

    @Override
    public String generateReferenceDraft(AiReferenceDraftRequest request) {
        TrainingCreateResponse response = referenceDraftClient.post()
                .uri("/api/v1/trainings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new TrainingCreateBody(
                        request.trainingId(), request.topic(), request.writingType(),
                        request.requirements(), request.timerMinutes()))
                .retrieve()
                .body(TrainingCreateResponse.class);
        if (response == null || response.aiWriting() == null || isBlank(response.aiWriting().content())) {
            throw new IllegalStateException("AI 서버가 빈 AI 작성 글을 반환했습니다.");
        }
        return response.aiWriting().content();
    }

    // ===== 3-2 / 3-4 / 4-4 비교 분석 =====

    @Override
    public AiComparisonResult analyzeComparison(AiComparisonRequest request) {
        submit(request);
        return awaitResult(request);
    }

    @Override
    public AiComparisonResult retryComparison(AiComparisonRequest request) {
        try {
            restClient.post()
                    .uri("/api/v1/analyses/{analysisId}/retry", request.analysisId())
                    .retrieve()
                    .toBodilessEntity();
        } catch (AiServerException e) {
            if (e.is(AiServerException.ANALYSIS_NOT_FOUND)) {
                // 제출이 AI 서버에 닿지 않았거나, AI 서버 재시작·1시간 만료로 잊음 → 처음부터 다시 제출
                log.info("AI 서버에 분석이 없어 다시 제출: analysisId={}", request.analysisId());
                submit(request);
            } else if (e.is(AiServerException.ANALYSIS_NOT_FAILED)) {
                // 백엔드만 실패로 봤고(폴링 시간 초과·저장 실패 등) AI 서버는 진행 중이거나 성공 → 결과만 다시 기다림
                log.info("AI 서버에서는 실패가 아니라 결과를 이어서 기다림: analysisId={}", request.analysisId());
            } else {
                throw e;
            }
        }
        return awaitResult(request);
    }

    @Override
    public AiComparisonResult resumeComparison(AiComparisonRequest request) {
        // 상태 조회부터 시작. AI 서버가 잊었으면(404) awaitResult 안에서 다시 제출한다
        return awaitResult(request);
    }

    private void submit(AiComparisonRequest request) {
        Object body;
        String path;
        AiWriting aiWriting = isBlank(request.aiWriting()) ? null : new AiWriting(request.aiWriting());
        if (request.isRevision()) {
            if (request.previousResult() == null) {
                throw new IllegalStateException("직전 회차 분석 결과가 없어 다시 검사를 요청할 수 없습니다: analysisId="
                        + request.analysisId());
            }
            path = "/api/v1/trainings/{trainingId}/revisions";
            body = new RevisionSubmitBody(
                    request.topic(), request.writingType(), request.requirements(),
                    request.analysisId(), request.versionId(), request.content(), aiWriting,
                    request.seqNo(),
                    new Writing(request.baselineVersionId(), request.baselineContent()),
                    jsonMapper.readTree(request.previousResult()));
        } else {
            path = "/api/v1/trainings/{trainingId}/draft";
            body = new DraftSubmitBody(
                    request.topic(), request.writingType(), request.requirements(),
                    request.analysisId(), request.versionId(), request.content(), aiWriting);
        }
        restClient.post()
                .uri(path, request.trainingId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    /** 4-1을 poll-interval 간격으로 조회하다가 SUCCESS면 4-2 결과를 가져온다. poll-timeout을 넘으면 실패 */
    private AiComparisonResult awaitResult(AiComparisonRequest request) {
        long deadline = System.nanoTime() + pollTimeout.toNanos();
        int resubmits = 0;
        int consecutiveErrors = 0;
        while (true) {
            AnalysisStatus status = null;
            try {
                status = restClient.get()
                        .uri("/api/v1/analyses/{analysisId}/status", request.analysisId())
                        .retrieve()
                        .body(AnalysisStatus.class);
                consecutiveErrors = 0;
            } catch (AiServerException e) {
                if (!e.is(AiServerException.ANALYSIS_NOT_FOUND) || resubmits >= MAX_RESUBMITS) {
                    throw e;
                }
                resubmits++;
                log.info("AI 서버가 분석을 잊어서 다시 제출({}회): analysisId={}", resubmits, request.analysisId());
                submit(request);
            } catch (ResourceAccessException e) {
                // 네트워크 순간 오류(ngrok 등)는 몇 번까지 넘긴다. AI 서버 쪽 분석은 계속 진행 중
                if (++consecutiveErrors > MAX_CONSECUTIVE_POLL_ERRORS) {
                    throw e;
                }
                log.warn("AI 분석 상태 조회 실패({}회), 계속 시도: analysisId={}", consecutiveErrors, request.analysisId());
            }

            if (status != null) {
                switch (status.status()) {
                    case "SUCCESS" -> {
                        return fetchResult(request.analysisId());
                    }
                    case "FAILED" -> throw failedException(status);
                    default -> { // PENDING, GENERATING: 계속 기다림
                    }
                }
            }

            if (System.nanoTime() > deadline) {
                throw new IllegalStateException("AI 분석 대기 시간(" + pollTimeout + ")을 넘었습니다: analysisId="
                        + request.analysisId());
            }
            sleep(pollInterval);
        }
    }

    private AiComparisonResult fetchResult(Long analysisId) {
        String raw = restClient.get()
                .uri("/api/v1/analyses/{analysisId}", analysisId)
                .retrieve()
                .body(String.class);
        if (isBlank(raw)) {
            throw new IllegalStateException("AI 서버 분석 결과가 비어 있습니다: analysisId=" + analysisId);
        }
        return parser.parse(raw);
    }

    private static AiServerException failedException(AnalysisStatus status) {
        Map<String, Object> error = status.error() == null ? Map.of() : status.error();
        return new AiServerException(
                200,
                String.valueOf(error.getOrDefault("code", "ANALYSIS_FAILED")),
                String.valueOf(error.getOrDefault("message", "AI 서버에서 분석이 실패했습니다.")),
                Boolean.TRUE.equals(error.get("retryable")));
    }

    // ===== 3-5 자기 사고 비율 =====

    @Override
    public AiRatioResult calculateRatio(AiRatioRequest request) {
        CompleteResponse response = restClient.post()
                .uri("/api/v1/trainings/{trainingId}/complete", request.trainingId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CompleteBody(
                        request.finalContent(),
                        request.finalVersionId(),
                        new Writing(request.draftVersionId(), request.draftContent()),
                        new AiWriting(request.aiWriting())))
                .retrieve()
                .body(CompleteResponse.class);
        if (response == null || response.selfThoughtRatio() == null || response.aiDependencyRatio() == null) {
            throw new IllegalStateException("AI 서버가 자기 사고 비율을 반환하지 않았습니다: trainingId="
                    + request.trainingId());
        }
        return new AiRatioResult(response.selfThoughtRatio(), response.aiDependencyRatio());
    }

    // ===== 공통 =====

    /** 에러 본문 {"error": {"code", "message", "retryable"}}. FastAPI 기본 422({"detail": ...})면 code는 null */
    private static AiServerException toAiServerException(ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        String body;
        try (InputStream in = response.getBody()) {
            body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        try {
            JsonNode error = JsonMapper.builder().build().readTree(body).path("error");
            if (error.isObject()) {
                return new AiServerException(
                        status,
                        error.path("code").asString(null),
                        error.path("message").asString(""),
                        error.path("retryable").asBoolean(false));
            }
        } catch (RuntimeException ignored) {
            // JSON이 아니면 아래에서 본문 앞부분만 메시지로 남긴다
        }
        return new AiServerException(status, null, abbreviate(body), status >= 500);
    }

    private static String abbreviate(String body) {
        return body.length() > 300 ? body.substring(0, 300) + "..." : body;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI 분석 대기 중 인터럽트", e);
        }
    }

    // ===== AI 서버 요청/응답 형식 (JSON camelCase) =====

    record AiWriting(String content) {
    }

    record Writing(Long versionId, String content) {
    }

    record TrainingCreateBody(Long trainingId, String topic, String writingType, String requirements,
                              Integer timerMinutes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TrainingCreateResponse(AiWriting aiWriting) {
    }

    record DraftSubmitBody(String topic, String writingType, String requirements,
                           Long analysisId, Long versionId, String content, AiWriting aiWriting) {
    }

    record RevisionSubmitBody(String topic, String writingType, String requirements,
                              Long analysisId, Long versionId, String content, AiWriting aiWriting,
                              int seqNo, Writing baseline, JsonNode previousResult) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AnalysisStatus(Long analysisId, String status, Map<String, Object> error) {
    }

    record CompleteBody(String content, Long finalVersionId, Writing draft, AiWriting aiWriting) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CompleteResponse(BigDecimal selfThoughtRatio, BigDecimal aiDependencyRatio) {
    }
}
