package com.zcommcx.gemini.client;

import com.zcommcx.gemini.config.GeminiProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Gemini generateContent 호출을 담당하는 저수준 클라이언트.
 * Function Calling을 위한 tools/systemInstruction/멀티턴 contents 조립은 호출자(ChatService)의 책임이다.
 */
@Component
public class GeminiClient {

    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GeminiClient(GeminiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    public JsonNode generateContent(ArrayNode contents, ArrayNode functionDeclarations, String systemInstruction) {
        String url = "%s/models/%s:generateContent?key=%s"
                .formatted(properties.baseUrl(), properties.model(), properties.apiKey());

        String rawResponse;
        try {
            rawResponse = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequestBody(contents, functionDeclarations, systemInstruction))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            throw new GeminiClientException("Gemini API 호출에 실패했습니다.", e);
        }

        try {
            return objectMapper.readTree(rawResponse);
        } catch (JsonProcessingException e) {
            throw new GeminiClientException("Gemini 응답 파싱에 실패했습니다. rawResponse=" + rawResponse, e);
        }
    }

    private ObjectNode buildRequestBody(ArrayNode contents, ArrayNode functionDeclarations, String systemInstruction) {
        ObjectNode root = objectMapper.createObjectNode();
        root.set("contents", contents);

        if (functionDeclarations != null) {
            ArrayNode tools = objectMapper.createArrayNode();
            tools.add(objectMapper.createObjectNode().set("functionDeclarations", functionDeclarations));
            root.set("tools", tools);
        }

        if (systemInstruction != null) {
            ArrayNode parts = objectMapper.createArrayNode();
            parts.add(objectMapper.createObjectNode().put("text", systemInstruction));
            ObjectNode instruction = objectMapper.createObjectNode();
            instruction.set("parts", parts);
            root.set("systemInstruction", instruction);
        }

        return root;
    }
}
