package com.zcommcx.review.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zcommcx.review.domain.FitLevel;
import com.zcommcx.review.domain.Review;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiFitProfileGenerator implements FitProfileGenerator {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    @Override
    public FitProfileResult generateFromReviews(List<Review> reviews, FitAxisLabels.Labels labels) {
        return generate(FitGuidePrompts.fromReviewsPrompt(reviews, labels));
    }

    @Override
    public FitProfileResult generateFromDescription(
            String productName, String category, String description, FitAxisLabels.Labels labels) {
        return generate(FitGuidePrompts.fromDescriptionPrompt(productName, category, description, labels));
    }

    private FitProfileResult generate(String prompt) {
        JsonNode resultNode;
        try {
            resultNode = geminiClient.generateJson(prompt, buildResponseSchema());
        } catch (GeminiClientException e) {
            throw new FitProfileGenerationException("Gemini 핏 가이드 생성 호출에 실패했습니다.", e);
        }

        try {
            return new FitProfileResult(
                    parseFitLevel(resultNode, "shoulderFit"),
                    parseFitLevel(resultNode, "chestFit"),
                    parseFitLevel(resultNode, "lengthFit"),
                    resultNode.get("recommendedBodyType").asText(),
                    resultNode.get("summary").asText());
        } catch (NullPointerException e) {
            log.error("Gemini 핏 가이드 응답 파싱 실패. resultNode={}", resultNode, e);
            throw new FitProfileGenerationException("Gemini 핏 가이드 응답 파싱에 실패했습니다.", e);
        }
    }

    /** AI가 스키마 밖의 값을 반환하는 등 예상치 못한 응답이어도 전체 생성을 실패시키지 않고 UNKNOWN으로 처리한다. */
    private FitLevel parseFitLevel(JsonNode resultNode, String field) {
        try {
            return FitLevel.valueOf(resultNode.get(field).asText());
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("핏 레벨 파싱 실패, UNKNOWN으로 대체. field={}, value={}", field, resultNode.get(field));
            return FitLevel.UNKNOWN;
        }
    }

    private ObjectNode buildResponseSchema() {
        ObjectNode schemaProperties = objectMapper.createObjectNode();
        schemaProperties.set("shoulderFit", objectMapper.createObjectNode().put("type", "STRING"));
        schemaProperties.set("chestFit", objectMapper.createObjectNode().put("type", "STRING"));
        schemaProperties.set("lengthFit", objectMapper.createObjectNode().put("type", "STRING"));
        schemaProperties.set("recommendedBodyType", objectMapper.createObjectNode().put("type", "STRING"));
        schemaProperties.set("summary", objectMapper.createObjectNode().put("type", "STRING"));

        ObjectNode responseSchema = objectMapper.createObjectNode();
        responseSchema.put("type", "OBJECT");
        responseSchema.set("properties", schemaProperties);
        responseSchema.putArray("required")
                .add("shoulderFit").add("chestFit").add("lengthFit").add("recommendedBodyType").add("summary");

        return responseSchema;
    }
}
