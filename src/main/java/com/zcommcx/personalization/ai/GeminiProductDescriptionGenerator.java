package com.zcommcx.personalization.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zcommcx.personalization.domain.CustomerSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiProductDescriptionGenerator implements ProductDescriptionGenerator {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    @Override
    public GeneratedDescription generate(String productName, String brand, String baseDescription, CustomerSegment segment, String keywords) {
        String prompt = ProductDescriptionPrompts.generatePrompt(productName, brand, baseDescription, segment, keywords);

        JsonNode resultNode;
        try {
            resultNode = geminiClient.generateJson(prompt, buildResponseSchema());
        } catch (GeminiClientException e) {
            throw new ProductDescriptionGenerationException("Gemini 상세설명 생성 호출에 실패했습니다.", e);
        }

        String content;
        try {
            content = resultNode.get("description").asText();
        } catch (NullPointerException e) {
            log.error("Gemini 상세설명 응답 파싱 실패. resultNode={}", resultNode, e);
            throw new ProductDescriptionGenerationException("Gemini 상세설명 응답 파싱에 실패했습니다.", e);
        }

        // 적합도 자체평가는 부가 정보라 파싱에 실패해도 본문 생성 자체를 실패로 취급하지 않는다.
        Integer fitScore = resultNode.path("fitScore").isInt() ? resultNode.path("fitScore").asInt() : null;
        String fitScoreReason = resultNode.path("fitScoreReason").isTextual()
                ? resultNode.path("fitScoreReason").asText()
                : null;

        return new GeneratedDescription(content, fitScore, fitScoreReason);
    }

    private ObjectNode buildResponseSchema() {
        ObjectNode schemaProperties = objectMapper.createObjectNode();
        schemaProperties.set("description", objectMapper.createObjectNode().put("type", "STRING"));
        schemaProperties.set("fitScore", objectMapper.createObjectNode().put("type", "INTEGER"));
        schemaProperties.set("fitScoreReason", objectMapper.createObjectNode().put("type", "STRING"));

        ObjectNode responseSchema = objectMapper.createObjectNode();
        responseSchema.put("type", "OBJECT");
        responseSchema.set("properties", schemaProperties);
        responseSchema.putArray("required").add("description").add("fitScore").add("fitScoreReason");

        return responseSchema;
    }
}
