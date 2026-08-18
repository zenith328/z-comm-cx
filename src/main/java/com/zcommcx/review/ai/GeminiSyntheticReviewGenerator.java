package com.zcommcx.review.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiSyntheticReviewGenerator implements SyntheticReviewGenerator {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    @Override
    public List<GeneratedSyntheticReview> generate(
            String productName, String brand, String category, String description, int count) {
        String prompt = FitGuidePrompts.syntheticReviewsPrompt(productName, brand, category, description, count);

        JsonNode resultNode;
        try {
            resultNode = geminiClient.generateJson(prompt, buildResponseSchema());
        } catch (GeminiClientException e) {
            throw new FitProfileGenerationException("Gemini 합성 리뷰 생성 호출에 실패했습니다.", e);
        }

        List<GeneratedSyntheticReview> reviews = new ArrayList<>();
        JsonNode reviewsNode = resultNode.path("reviews");
        if (reviewsNode.isArray()) {
            reviewsNode.forEach(node -> {
                String content = node.path("content").asText("");
                int rating = Math.min(5, Math.max(1, node.path("rating").asInt(5)));
                if (!content.isBlank()) {
                    reviews.add(new GeneratedSyntheticReview(content, rating));
                }
            });
        } else {
            log.warn("Gemini 합성 리뷰 응답 파싱 실패. resultNode={}", resultNode);
        }
        return reviews;
    }

    private ObjectNode buildResponseSchema() {
        ObjectNode reviewItemProperties = objectMapper.createObjectNode();
        reviewItemProperties.set("content", objectMapper.createObjectNode().put("type", "STRING"));
        reviewItemProperties.set("rating", objectMapper.createObjectNode().put("type", "INTEGER"));

        ObjectNode reviewItem = objectMapper.createObjectNode();
        reviewItem.put("type", "OBJECT");
        reviewItem.set("properties", reviewItemProperties);
        reviewItem.putArray("required").add("content").add("rating");

        ObjectNode reviewsArray = objectMapper.createObjectNode();
        reviewsArray.put("type", "ARRAY");
        reviewsArray.set("items", reviewItem);

        ObjectNode schemaProperties = objectMapper.createObjectNode();
        schemaProperties.set("reviews", reviewsArray);

        ObjectNode responseSchema = objectMapper.createObjectNode();
        responseSchema.put("type", "OBJECT");
        responseSchema.set("properties", schemaProperties);
        responseSchema.putArray("required").add("reviews");

        return responseSchema;
    }
}
