package com.hackathon._hosun.domain.analysis.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.hackathon._hosun.domain.analysis.ai.AiComparisonResultParser.AiResponseInvalidException;
import com.hackathon._hosun.domain.analysis.entity.Criterion;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpAiClientTest {

    private static final String BASE = "http://ai.test";

    private MockRestServiceServer server;
    private HttpAiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = HttpAiClient.configure(RestClient.builder().baseUrl(BASE), "test-key");
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        client = new HttpAiClient(restClient, restClient, Duration.ZERO, Duration.ofSeconds(5), Duration.ZERO);
    }

    @Test
    void AI_작성_글_생성은_헤더를_붙여_multipart로_요청하고_aiWriting_content를_반환한다() {
        server.expect(requestTo(BASE + "/api/v1/trainings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-Key", "test-key"))
                .andExpect(header("ngrok-skip-browser-warning", "true"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andExpect(request -> {
                    String body = ((MockClientHttpRequest) request).getBodyAsString(StandardCharsets.UTF_8);
                    assertThat(body).contains("name=\"request\"", "Content-Type: application/json",
                            "\"trainingId\":8", "\"writingType\":\"논설문\"", "\"timerMinutes\":15");
                    assertThat(body).doesNotContain("name=\"files\"");
                })
                .andRespond(json("""
                        { "trainingId": 8, "aiWriting": { "content": "AI 글" }, "charCount": 4,
                          "promptVersion": "v4", "model": "m" }"""));

        String content = client.generateReferenceDraft(
                new AiReferenceDraftRequest(8L, "탄소 중립", "논설문", null, 15, null));

        assertThat(content).isEqualTo("AI 글");
        server.verify();
    }

    @Test
    void AI_작성_글_생성은_첨부파일을_files_파트로_보낸다() {
        server.expect(requestTo(BASE + "/api/v1/trainings"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andExpect(request -> {
                    String body = ((MockClientHttpRequest) request).getBodyAsString(StandardCharsets.UTF_8);
                    assertThat(body).contains(
                            "name=\"files\"; filename=\"IPCC_요약.txt\"", "Content-Type: text/plain", "탄소세 자료 본문",
                            "filename=\"메모.hwpx\"", "Content-Type: application/octet-stream");
                })
                .andRespond(json("""
                        { "aiWriting": { "content": "AI 글" },
                          "attachments": [ { "fileName": "IPCC_요약.txt", "fileSize": 10, "used": true, "usedChars": 10 } ],
                          "warnings": ["첨부파일 '메모.hwpx'을 읽지 못해 AI 작성 글에 반영하지 않았어요(손상 또는 암호)."] }"""));

        String content = client.generateReferenceDraft(new AiReferenceDraftRequest(8L, "탄소 중립", "논설문", null, null,
                List.of(
                        new AiReferenceDraftRequest.Attachment(
                                "IPCC_요약.txt", "text/plain", "탄소세 자료 본문".getBytes(StandardCharsets.UTF_8)),
                        new AiReferenceDraftRequest.Attachment("메모.hwpx", null, new byte[] {1, 2, 3}))));

        assertThat(content).isEqualTo("AI 글");
        server.verify();
    }

    @Test
    void 초안_분석은_제출_후_상태를_폴링하고_결과를_가져온다() {
        expectSubmitDraft();
        expectStatus("GENERATING");
        expectStatus("SUCCESS");
        expectResult(resultJson(4));

        AiComparisonResult result = client.analyzeComparison(draftRequest());

        assertThat(result.items()).hasSize(5);
        assertThat(result.items()).extracting(AiComparisonResult.Item::criterion)
                .containsExactly(Criterion.values());
        assertThat(result.items()).allSatisfy(i -> assertThat(i.userScore()).isEqualTo(4));
        assertThat(result.items().get(0).reflectPoint()).isNull();
        assertThat(result.overallSummary()).isNull();
        assertThat(result.questions()).containsExactly("질문1", "질문2");
        assertThat(result.rawResult()).contains("\"scores\"");
        server.verify();
    }

    @Test
    void 재검사는_revisions로_baseline과_previousResult_원본을_보낸다() {
        server.expect(requestTo(BASE + "/api/v1/trainings/8/revisions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.seqNo").value(1))
                .andExpect(jsonPath("$.baseline.versionId").value(30))
                .andExpect(jsonPath("$.baseline.content").value("최초 초안"))
                .andExpect(jsonPath("$.previousResult.analysisId").value(20))
                .andExpect(jsonPath("$.previousResult.scores[0].userScore").value(3))
                .andRespond(withStatus(HttpStatus.ACCEPTED));
        expectStatus("SUCCESS");
        expectResult(resultJson(5));

        AiComparisonResult result = client.analyzeComparison(new AiComparisonRequest(
                20L, 8L, "주제", "논설문", null, 31L, 1, "수정본", "AI 글",
                30L, "최초 초안", resultJson(3)));

        assertThat(result.items().get(0).userScore()).isEqualTo(5);
        server.verify();
    }

    @Test
    void 재검사인데_직전_결과가_없으면_제출하지_않고_실패한다() {
        assertThatThrownBy(() -> client.analyzeComparison(new AiComparisonRequest(
                21L, 8L, "주제", "논설문", null, 31L, 1, "수정본", "AI 글", 30L, "최초 초안", null)))
                .isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @Test
    void 폴링_중_AI_서버가_분석을_잊으면_같은_analysisId로_다시_제출한다() {
        expectSubmitDraft();
        server.expect(requestTo(BASE + "/api/v1/analyses/20/status"))
                .andRespond(error(HttpStatus.NOT_FOUND, "ANALYSIS_NOT_FOUND", true));
        expectSubmitDraft();
        expectStatus("SUCCESS");
        expectResult(resultJson(4));

        assertThat(client.analyzeComparison(draftRequest()).items()).hasSize(5);
        server.verify();
    }

    @Test
    void AI_서버에서_분석이_FAILED면_에러_코드를_담아_실패한다() {
        expectSubmitDraft();
        server.expect(requestTo(BASE + "/api/v1/analyses/20/status"))
                .andRespond(json("""
                        { "analysisId": 20, "status": "FAILED", "trainingStatus": "ANALYZING",
                          "error": { "code": "LLM_TIMEOUT", "message": "시간 초과", "retryable": true } }"""));

        assertThatThrownBy(() -> client.analyzeComparison(draftRequest()))
                .isInstanceOfSatisfying(AiServerException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("LLM_TIMEOUT");
                    assertThat(e.isRetryable()).isTrue();
                });
        server.verify();
    }

    @Test
    void 제출이_4xx면_에러_본문의_code로_예외를_던진다() {
        server.expect(requestTo(BASE + "/api/v1/trainings/8/draft"))
                .andRespond(error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", false));

        assertThatThrownBy(() -> client.analyzeComparison(draftRequest()))
                .isInstanceOfSatisfying(AiServerException.class, e -> {
                    assertThat(e.getHttpStatus()).isEqualTo(401);
                    assertThat(e.getCode()).isEqualTo("UNAUTHORIZED");
                });
        server.verify();
    }

    @Test
    void 제출이_QUEUE_FULL이면_잠시_후_다시_제출한다() {
        server.expect(requestTo(BASE + "/api/v1/trainings/8/draft"))
                .andRespond(error(HttpStatus.TOO_MANY_REQUESTS, "QUEUE_FULL", true));
        expectSubmitDraft();
        expectStatus("SUCCESS");
        expectResult(resultJson(4));

        assertThat(client.analyzeComparison(draftRequest()).items()).hasSize(5);
        server.verify();
    }

    @Test
    void 제출_재시도_횟수를_넘기면_실패한다() {
        for (int i = 0; i < 3; i++) {
            server.expect(requestTo(BASE + "/api/v1/trainings/8/draft"))
                    .andRespond(error(HttpStatus.SERVICE_UNAVAILABLE, "LLM_UNAVAILABLE", true));
        }

        assertThatThrownBy(() -> client.analyzeComparison(draftRequest()))
                .isInstanceOfSatisfying(AiServerException.class,
                        e -> assertThat(e.getCode()).isEqualTo("LLM_UNAVAILABLE"));
        server.verify();
    }

    @Test
    void JSON이_아닌_5xx_응답도_일시_오류로_보고_다시_보낸다() {
        server.expect(requestTo(BASE + "/api/v1/trainings"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY).contentType(MediaType.TEXT_HTML).body("<html>ngrok</html>"));
        server.expect(requestTo(BASE + "/api/v1/trainings"))
                .andRespond(json("{ \"aiWriting\": { \"content\": \"AI 글\" } }"));

        assertThat(client.generateReferenceDraft(new AiReferenceDraftRequest(8L, "주제", "논설문", null, null, null)))
                .isEqualTo("AI 글");
        server.verify();
    }

    @Test
    void 폴링_중_RATE_LIMITED면_쉬었다가_계속_조회한다() {
        expectSubmitDraft();
        server.expect(requestTo(BASE + "/api/v1/analyses/20/status"))
                .andRespond(error(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", true));
        expectStatus("SUCCESS");
        expectResult(resultJson(4));

        assertThat(client.analyzeComparison(draftRequest()).items()).hasSize(5);
        server.verify();
    }

    @Test
    void 결과_조회가_일시_오류면_상태부터_다시_조회해서_가져온다() {
        expectSubmitDraft();
        expectStatus("SUCCESS");
        server.expect(requestTo(BASE + "/api/v1/analyses/20"))
                .andRespond(error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL", true));
        expectStatus("SUCCESS");
        expectResult(resultJson(4));

        assertThat(client.analyzeComparison(draftRequest()).items()).hasSize(5);
        server.verify();
    }

    @Test
    void 재시도는_retry를_호출하고_결과를_기다린다() {
        server.expect(requestTo(BASE + "/api/v1/analyses/20/retry"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.ACCEPTED));
        expectStatus("PENDING");
        expectStatus("SUCCESS");
        expectResult(resultJson(4));

        assertThat(client.retryComparison(draftRequest()).items()).hasSize(5);
        server.verify();
    }

    @Test
    void 재시도에서_AI_서버가_분석을_모르면_다시_제출한다() {
        server.expect(requestTo(BASE + "/api/v1/analyses/20/retry"))
                .andRespond(error(HttpStatus.NOT_FOUND, "ANALYSIS_NOT_FOUND", true));
        expectSubmitDraft();
        expectStatus("SUCCESS");
        expectResult(resultJson(4));

        assertThat(client.retryComparison(draftRequest()).items()).hasSize(5);
        server.verify();
    }

    @Test
    void 재시도에서_AI_서버는_실패가_아니면_결과만_이어서_기다린다() {
        server.expect(requestTo(BASE + "/api/v1/analyses/20/retry"))
                .andRespond(error(HttpStatus.CONFLICT, "ANALYSIS_NOT_FAILED", false));
        expectStatus("SUCCESS");
        expectResult(resultJson(4));

        assertThat(client.retryComparison(draftRequest()).items()).hasSize(5);
        server.verify();
    }

    @Test
    void 이어서_기다리기는_제출_없이_상태부터_조회한다() {
        expectStatus("GENERATING");
        expectStatus("SUCCESS");
        expectResult(resultJson(4));

        assertThat(client.resumeComparison(draftRequest()).items()).hasSize(5);
        server.verify();
    }

    @Test
    void 기준이_빠진_응답은_검증에서_실패한다() {
        expectSubmitDraft();
        expectStatus("SUCCESS");
        expectResult(resultJson(4).replace("\"criterion\": \"ORIGINALITY\", \"label\"", "\"criterion\": \"THESIS\", \"label\""));

        assertThatThrownBy(() -> client.analyzeComparison(draftRequest()))
                .isInstanceOf(AiResponseInvalidException.class);
    }

    @Test
    void 점수가_범위를_벗어난_응답은_검증에서_실패한다() {
        expectSubmitDraft();
        expectStatus("SUCCESS");
        expectResult(resultJson(9));

        assertThatThrownBy(() -> client.analyzeComparison(draftRequest()))
                .isInstanceOf(AiResponseInvalidException.class);
    }

    @Test
    void 자기_사고_비율은_complete_응답의_비율을_반환한다() {
        server.expect(requestTo(BASE + "/api/v1/trainings/8/complete"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.content").value("최종 글"))
                .andExpect(jsonPath("$.finalVersionId").value(32))
                .andExpect(jsonPath("$.draft.versionId").value(30))
                .andExpect(jsonPath("$.aiWriting.content").value("AI 글"))
                .andRespond(json("""
                        { "trainingId": 8, "status": "COMPLETED", "finalVersionId": 32,
                          "completedAt": "2026-11-11T21:30:00+09:00",
                          "selfThoughtRatio": 71.0, "aiDependencyRatio": 29.0,
                          "sentences": [], "method": "설명", "promptVersion": "v4", "model": "bge-m3" }"""));

        AiRatioResult ratio = client.calculateRatio(
                new AiRatioRequest(8L, "최종 글", 32L, 30L, "최초 초안", "AI 글"));

        assertThat(ratio.selfThoughtRatio()).isEqualByComparingTo(new BigDecimal("71.0"));
        assertThat(ratio.aiDependencyRatio()).isEqualByComparingTo(new BigDecimal("29.0"));
        server.verify();
    }

    // ===== helpers =====

    private static AiComparisonRequest draftRequest() {
        return new AiComparisonRequest(20L, 8L, "주제", "논설문", "800~1,000자", 30L, 0, "초안", "AI 글",
                null, null, null);
    }

    private void expectSubmitDraft() {
        server.expect(requestTo(BASE + "/api/v1/trainings/8/draft"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.analysisId").value(20))
                .andExpect(jsonPath("$.versionId").value(30))
                .andExpect(jsonPath("$.content").value("초안"))
                .andExpect(jsonPath("$.aiWriting.content").value("AI 글"))
                .andExpect(jsonPath("$.requirements").value("800~1,000자"))
                .andRespond(withStatus(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"versionId\": 30, \"seqNo\": 0, \"analysisId\": 20, \"status\": \"ANALYZING\"}"));
    }

    private void expectStatus(String status) {
        server.expect(requestTo(BASE + "/api/v1/analyses/20/status"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(json("{\"analysisId\": 20, \"status\": \"" + status
                        + "\", \"trainingStatus\": \"ANALYZING\", \"error\": null}"));
    }

    private void expectResult(String body) {
        server.expect(requestTo(BASE + "/api/v1/analyses/20"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(json(body));
    }

    private static org.springframework.test.web.client.response.DefaultResponseCreator json(String body) {
        return withSuccess(body, MediaType.APPLICATION_JSON);
    }

    private static org.springframework.test.web.client.response.DefaultResponseCreator error(
            HttpStatus status, String code, boolean retryable) {
        return withStatus(status).contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\": {\"code\": \"" + code + "\", \"message\": \"메시지\", \"retryable\": " + retryable + "}}");
    }

    /** AI 서버 4-2 응답 예시. overallSummary·THESIS reflectPoint는 null, 명세 외 필드 포함 */
    private static String resultJson(int userScore) {
        StringBuilder items = new StringBuilder();
        StringBuilder scores = new StringBuilder();
        for (Criterion c : Criterion.values()) {
            String sep = items.isEmpty() ? "" : ", ";
            String reflect = c == Criterion.THESIS ? "null" : "\"보완 방향\"";
            items.append(sep).append("""
                    { "criterion": "%s", "label": "라벨", "description": "설명",
                      "userSummary": "내 요약", "aiSummary": "AI 요약", "comparison": "비교", "reflectPoint": %s }"""
                    .formatted(c.name(), reflect));
            scores.append(sep).append("""
                    { "criterion": "%s", "userScore": %d, "aiScore": 4, "better": "tie", "evidence": [] }"""
                    .formatted(c.name(), userScore));
        }
        return """
                { "analysisId": 20, "trainingId": 8, "seqNo": 0, "createdAt": "2026-11-11T21:00:00+09:00",
                  "myWriting": { "versionId": 30, "content": "초안" }, "aiWriting": { "content": "AI 글" },
                  "baseline": null, "overallSummary": null, "strengths": "강점1\\n강점2", "improvements": "보완",
                  "items": [%s],
                  "questions": [ { "seqNo": 1, "content": "질문1" }, { "seqNo": 2, "content": "질문2" } ],
                  "scores": [%s],
                  "reflectionReviews": [], "scoreDelta": {}, "partial": false, "warnings": [],
                  "promptVersion": "v4", "model": "gemma4:e2b-it-qat" }"""
                .formatted(items, scores);
    }
}
