package com.zcommcx.personalization.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zcommcx.personalization.domain.CustomerSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiSegmentKeywordSuggester implements SegmentKeywordSuggester {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    @Override
    public List<String> suggest(CustomerSegment segment, List<String> reviewExcerpts, String existingKeywords) {
        String prompt = SegmentKeywordPrompts.suggestPrompt(segment, reviewExcerpts, existingKeywords);

        JsonNode resultNode;
        try {
            resultNode = geminiClient.generateJson(prompt, buildResponseSchema());
        } catch (GeminiClientException e) {
            throw new SegmentKeywordSuggestionException("Gemini 키워드 제안 호출에 실패했습니다.", e);
        }

        List<String> keywords = new ArrayList<>();
        JsonNode keywordsNode = resultNode.path("keywords");
        if (keywordsNode.isArray()) {
            keywordsNode.forEach(node -> {
                if (node.isTextual() && !node.asText().isBlank()) {
                    keywords.add(node.asText());
                }
            });
        } else {
            log.warn("Gemini 키워드 제안 응답 파싱 실패. resultNode={}", resultNode);
        }
        return keywords;
    }

    private ObjectNode buildResponseSchema() {
        ObjectNode keywordItems = objectMapper.createObjectNode().put("type", "STRING");

        ObjectNode keywordsArray = objectMapper.createObjectNode();
        keywordsArray.put("type", "ARRAY");
        keywordsArray.set("items", keywordItems);

        ObjectNode schemaProperties = objectMapper.createObjectNode();
        schemaProperties.set("keywords", keywordsArray);

        ObjectNode responseSchema = objectMapper.createObjectNode();
        responseSchema.put("type", "OBJECT");
        responseSchema.set("properties", schemaProperties);
        responseSchema.putArray("required").add("keywords");

        return responseSchema;
    }

    @Override
    public List<String> suggestFallback(CustomerSegment segment, String existingKeywords) {
        String prompt = SegmentKeywordPrompts.fallbackPrompt(segment, existingKeywords);

        JsonNode resultNode;
        try {
            resultNode = geminiClient.generateJson(prompt, buildResponseSchema());
        } catch (GeminiClientException e) {
            throw new SegmentKeywordSuggestionException("Gemini 일반 지식 기반 키워드 제안 호출에 실패했습니다.", e);
        }

        List<String> keywords = new ArrayList<>();
        JsonNode keywordsNode = resultNode.path("keywords");
        if (keywordsNode.isArray()) {
            keywordsNode.forEach(node -> {
                if (node.isTextual() && !node.asText().isBlank()) {
                    keywords.add(node.asText());
                }
            });
        } else {
            log.warn("Gemini 일반 지식 기반 키워드 제안 응답 파싱 실패. resultNode={}", resultNode);
        }
        return keywords;
    }
}
