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
    public String generate(String productName, String brand, String baseDescription, CustomerSegment segment, String keywords) {
        String prompt = ProductDescriptionPrompts.generatePrompt(productName, brand, baseDescription, segment, keywords);

        JsonNode resultNode;
        try {
            resultNode = geminiClient.generateJson(prompt, buildResponseSchema());
        } catch (GeminiClientException e) {
            throw new ProductDescriptionGenerationException("Gemini 상세설명 생성 호출에 실패했습니다.", e);
        }

        try {
            return resultNode.get("description").asText();
        } catch (NullPointerException e) {
            log.error("Gemini 상세설명 응답 파싱 실패. resultNode={}", resultNode, e);
            throw new ProductDescriptionGenerationException("Gemini 상세설명 응답 파싱에 실패했습니다.", e);
        }
    }

    private ObjectNode buildResponseSchema() {
        ObjectNode schemaProperties = objectMapper.createObjectNode();
        schemaProperties.set("description", objectMapper.createObjectNode().put("type", "STRING"));

        ObjectNode responseSchema = objectMapper.createObjectNode();
        responseSchema.put("type", "OBJECT");
        responseSchema.set("properties", schemaProperties);
        responseSchema.putArray("required").add("description");

        return responseSchema;
    }
}
