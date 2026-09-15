package com.zcommcx.avatar.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zcommcx.product.domain.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiShowhostGenerator {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public String generateScript(Product product, String reviewSummary) {
        return generate(ShowhostPrompts.scriptPrompt(product, reviewSummary), "script");
    }

    public String answerQuestion(Product product, String reviewSummary, String question) {
        return generate(ShowhostPrompts.qnaPrompt(product, reviewSummary, question), "answer");
    }

    private String generate(String prompt, String field) {
        JsonNode resultNode;
        try {
            resultNode = geminiClient.generateJson(prompt, buildResponseSchema(field));
        } catch (GeminiClientException e) {
            throw new ShowhostGenerationException("Gemini 쇼호스트 생성 호출에 실패했습니다.", e);
        }

        try {
            return resultNode.get(field).asText();
        } catch (NullPointerException e) {
            log.error("Gemini 쇼호스트 응답 파싱 실패. resultNode={}", resultNode, e);
            throw new ShowhostGenerationException("Gemini 쇼호스트 응답 파싱에 실패했습니다.", e);
        }
    }

    private ObjectNode buildResponseSchema(String field) {
        ObjectNode schemaProperties = objectMapper.createObjectNode();
        schemaProperties.set(field, objectMapper.createObjectNode().put("type", "STRING"));

        ObjectNode responseSchema = objectMapper.createObjectNode();
        responseSchema.put("type", "OBJECT");
        responseSchema.set("properties", schemaProperties);
        responseSchema.putArray("required").add(field);
        return responseSchema;
    }
}
