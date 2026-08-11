package com.zcommcx.personalization.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiProductDescriptionImageExtractor implements ProductDescriptionImageExtractor {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    @Override
    public String extractText(byte[] imageBytes, String mimeType) {
        JsonNode resultNode;
        try {
            resultNode = geminiClient.generateJson(
                    ProductDescriptionPrompts.extractFromImagePrompt(), imageBytes, mimeType, buildResponseSchema());
        } catch (GeminiClientException e) {
            throw new ProductDescriptionGenerationException("이미지에서 텍스트 추출 호출에 실패했습니다.", e);
        }

        try {
            return resultNode.get("text").asText();
        } catch (NullPointerException e) {
            log.error("이미지 텍스트 추출 응답 파싱 실패. resultNode={}", resultNode, e);
            throw new ProductDescriptionGenerationException("이미지 텍스트 추출 응답 파싱에 실패했습니다.", e);
        }
    }

    private ObjectNode buildResponseSchema() {
        ObjectNode schemaProperties = objectMapper.createObjectNode();
        schemaProperties.set("text", objectMapper.createObjectNode().put("type", "STRING"));

        ObjectNode responseSchema = objectMapper.createObjectNode();
        responseSchema.put("type", "OBJECT");
        responseSchema.set("properties", schemaProperties);
        responseSchema.putArray("required").add("text");

        return responseSchema;
    }
}
