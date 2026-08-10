package com.zcommcx.review.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zcommcx.review.domain.Review;
import com.zcommcx.review.domain.ReviewClassification;
import com.zcommcx.review.domain.ReviewSentiment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiReviewClassifier implements ReviewClassifier {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    @Override
    public ReviewAiResult classify(Review review) {
        String prompt = ReviewPrompts.classificationPrompt(review);

        JsonNode resultNode;
        try {
            resultNode = geminiClient.generateJson(prompt, buildResponseSchema());
        } catch (GeminiClientException e) {
            throw new ReviewAiClassificationException("Gemini 분류 호출에 실패했습니다. reviewId=" + review.getId(), e);
        }

        try {
            boolean visible = resultNode.get("visible").asBoolean();
            ReviewClassification classification =
                    ReviewClassification.valueOf(resultNode.get("classification").asText());
            ReviewSentiment sentiment = ReviewSentiment.valueOf(resultNode.get("sentiment").asText());
            int riskScore = resultNode.get("risk_score").asInt();
            String reason = resultNode.get("reason").asText();
            return new ReviewAiResult(visible, classification, sentiment, riskScore, reason);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("Gemini 분류 응답 파싱 실패. reviewId={}, resultNode={}", review.getId(), resultNode, e);
            throw new ReviewAiClassificationException("Gemini 분류 응답 파싱에 실패했습니다. reviewId=" + review.getId(), e);
        }
    }

    private ObjectNode buildResponseSchema() {
        ObjectNode schemaProperties = objectMapper.createObjectNode();
        schemaProperties.set("visible", objectMapper.createObjectNode().put("type", "BOOLEAN"));

        ObjectNode classificationProp = objectMapper.createObjectNode().put("type", "STRING");
        ArrayNode enumValues = objectMapper.createArrayNode();
        for (ReviewClassification value : ReviewClassification.values()) {
            enumValues.add(value.name());
        }
        classificationProp.set("enum", enumValues);
        schemaProperties.set("classification", classificationProp);

        ObjectNode sentimentProp = objectMapper.createObjectNode().put("type", "STRING");
        ArrayNode sentimentValues = objectMapper.createArrayNode();
        for (ReviewSentiment value : ReviewSentiment.values()) {
            sentimentValues.add(value.name());
        }
        sentimentProp.set("enum", sentimentValues);
        schemaProperties.set("sentiment", sentimentProp);

        schemaProperties.set("risk_score", objectMapper.createObjectNode().put("type", "INTEGER"));
        schemaProperties.set("reason", objectMapper.createObjectNode().put("type", "STRING"));

        ObjectNode responseSchema = objectMapper.createObjectNode();
        responseSchema.put("type", "OBJECT");
        responseSchema.set("properties", schemaProperties);
        ArrayNode required = objectMapper.createArrayNode();
        required.add("visible").add("classification").add("sentiment").add("risk_score").add("reason");
        responseSchema.set("required", required);

        return responseSchema;
    }
}
