package com.hackathon._hosun.domain.analysis.ai;

import com.hackathon._hosun.domain.analysis.entity.Criterion;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * AI 서버 4-2 응답(원본 JSON)을 검증하고 AiComparisonResult로 바꾼다.
 * 저장할 수 없는 응답이면 AiResponseInvalidException을 던져 분석을 FAILED로 만든다.
 * partial=true여도 아래 검증을 통과하면 정상 결과로 저장한다.
 */
public class AiComparisonResultParser {

    static final int QUESTION_MAX_LENGTH = 500; // thinking_question.content 컬럼 길이
    private static final int SCORE_MIN = 1;
    private static final int SCORE_MAX = 5;

    private final JsonMapper jsonMapper;

    public AiComparisonResultParser(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public AiComparisonResult parse(String rawResult) {
        JsonNode root = jsonMapper.readTree(rawResult);

        Map<Criterion, Integer> scores = parseScores(root.path("scores"));
        List<AiComparisonResult.Item> items = parseItems(root.path("items"), scores);

        return new AiComparisonResult(
                nullableText(root, "overallSummary"),
                requiredText(root, "strengths"),
                requiredText(root, "improvements"),
                items,
                parseQuestions(root.path("questions")),
                rawResult);
    }

    private Map<Criterion, Integer> parseScores(JsonNode scoresNode) {
        if (!scoresNode.isArray()) {
            throw invalid("scores가 없습니다.");
        }
        Map<Criterion, Integer> scores = new EnumMap<>(Criterion.class);
        for (JsonNode node : scoresNode) {
            Criterion criterion = criterion(node);
            JsonNode score = node.path("userScore");
            if (!score.isIntegralNumber() || score.intValue() < SCORE_MIN || score.intValue() > SCORE_MAX) {
                throw invalid("userScore가 1~5 범위가 아닙니다: " + criterion);
            }
            if (scores.put(criterion, score.intValue()) != null) {
                throw invalid("scores에 기준이 중복됐습니다: " + criterion);
            }
        }
        return scores;
    }

    private List<AiComparisonResult.Item> parseItems(JsonNode itemsNode, Map<Criterion, Integer> scores) {
        if (!itemsNode.isArray()) {
            throw invalid("items가 없습니다.");
        }
        Map<Criterion, AiComparisonResult.Item> items = new EnumMap<>(Criterion.class);
        for (JsonNode node : itemsNode) {
            Criterion criterion = criterion(node);
            Integer userScore = scores.get(criterion);
            if (userScore == null) {
                throw invalid("기준의 점수가 없습니다: " + criterion);
            }
            AiComparisonResult.Item item = new AiComparisonResult.Item(
                    criterion,
                    requiredText(node, "userSummary"),
                    requiredText(node, "aiSummary"),
                    requiredText(node, "comparison"),
                    nullableText(node, "reflectPoint"),
                    userScore);
            if (items.put(criterion, item) != null) {
                throw invalid("items에 기준이 중복됐습니다: " + criterion);
            }
        }
        if (items.size() != Criterion.values().length) {
            throw invalid("items에 빠진 기준이 있습니다: " + items.keySet());
        }
        return List.copyOf(items.values()); // EnumMap이라 명세 순서(THESIS → ORIGINALITY)
    }

    private List<String> parseQuestions(JsonNode questionsNode) {
        if (!questionsNode.isArray()) {
            throw invalid("questions가 없습니다.");
        }
        List<String> questions = new ArrayList<>();
        for (JsonNode node : questionsNode) {
            String content = requiredText(node, "content");
            if (content.length() > QUESTION_MAX_LENGTH) {
                throw invalid("질문이 " + QUESTION_MAX_LENGTH + "자를 넘습니다.");
            }
            if (!content.isBlank()) {
                questions.add(content);
            }
        }
        return questions;
    }

    private Criterion criterion(JsonNode node) {
        String value = node.path("criterion").asString("");
        try {
            return Criterion.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw invalid("알 수 없는 기준입니다: " + value);
        }
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isString()) {
            throw invalid(field + "가 없습니다.");
        }
        return value.stringValue();
    }

    private String nullableText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isString() ? value.stringValue() : null;
    }

    private static AiResponseInvalidException invalid(String message) {
        return new AiResponseInvalidException(message);
    }

    /** AI 응답이 저장할 수 없는 형식일 때 */
    public static class AiResponseInvalidException extends RuntimeException {
        public AiResponseInvalidException(String message) {
            super("AI 응답 검증 실패: " + message);
        }
    }
}
