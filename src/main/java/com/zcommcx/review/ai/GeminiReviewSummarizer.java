package com.zcommcx.review.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zcommcx.config.GeminiProperties;
import com.zcommcx.review.domain.Review;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiReviewSummarizer implements ReviewSummarizer {

    private static final String NO_REVIEW_SUMMARY = "아직 참고할 수 있는 리뷰가 없습니다.";

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    private final GeminiProperties properties;

    @Override
    public ReviewSummaryResult summarize(List<Review> reviews, String query) {
        if (reviews.isEmpty()) {
            return new ReviewSummaryResult(NO_REVIEW_SUMMARY, 0);
        }

        List<Review> targetReviews = reviews.size() > properties.maxReviewsPerSummary()
                ? reviews.subList(0, properties.maxReviewsPerSummary())
                : reviews;

        String prompt = ReviewPrompts.summaryPrompt(targetReviews, query);

        JsonNode resultNode;
        try {
            resultNode = geminiClient.generateJson(prompt, buildResponseSchema());
        } catch (GeminiClientException e) {
            throw new ReviewSummaryException("Gemini 요약 호출에 실패했습니다.", e);
        }

        try {
            String summary = resultNode.get("summary").asText();
            return new ReviewSummaryResult(summary, targetReviews.size());
        } catch (NullPointerException e) {
            log.error("Gemini 요약 응답 파싱 실패. resultNode={}", resultNode, e);
            throw new ReviewSummaryException("Gemini 요약 응답 파싱에 실패했습니다.", e);
        }
    }

    private ObjectNode buildResponseSchema() {
        ObjectNode schemaProperties = objectMapper.createObjectNode();
        schemaProperties.set("summary", objectMapper.createObjectNode().put("type", "STRING"));

        ObjectNode responseSchema = objectMapper.createObjectNode();
        responseSchema.put("type", "OBJECT");
        responseSchema.set("properties", schemaProperties);
        responseSchema.putArray("required").add("summary");

        return responseSchema;
    }
}
