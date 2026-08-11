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
public class GeminiProductDescriptionTextCleaner implements ProductDescriptionTextCleaner {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    @Override
    public String clean(String rawText) {
        JsonNode resultNode;
        try {
            resultNode = geminiClient.generateJson(
                    ProductDescriptionPrompts.cleanupScrapedTextPrompt(rawText), buildResponseSchema());
        } catch (GeminiClientException e) {
            throw new ProductDescriptionGenerationException("웹페이지 텍스트 정리 호출에 실패했습니다.", e);
        }

        try {
            return resultNode.get("text").asText();
        } catch (NullPointerException e) {
            log.error("웹페이지 텍스트 정리 응답 파싱 실패. resultNode={}", resultNode, e);
            throw new ProductDescriptionGenerationException("웹페이지 텍스트 정리 응답 파싱에 실패했습니다.", e);
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
