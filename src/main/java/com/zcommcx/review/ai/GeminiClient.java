package com.zcommcx.review.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zcommcx.aiusage.service.GeminiApiUsageService;
import com.zcommcx.common.exception.AiQuotaExceededException;
import com.zcommcx.config.GeminiProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Gemini generateContent 호출 + JSON 모드(responseSchema) 파싱을 담당하는 저수준 클라이언트.
 * 도메인별 프롬프트/스키마 구성은 호출자(ReviewClassifier, ReviewSummarizer 등)의 책임이다.
 */
@Component("reviewGeminiClient")
public class GeminiClient {

    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final GeminiApiUsageService usageService;

    public GeminiClient(GeminiProperties properties, ObjectMapper objectMapper, GeminiApiUsageService usageService) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
        this.usageService = usageService;
    }

    public JsonNode generateJson(String prompt, ObjectNode responseSchema) {
        String url = "%s/models/%s:generateContent?key=%s"
                .formatted(properties.baseUrl(), properties.model(), properties.apiKey());

        String rawResponse;
        try {
            rawResponse = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequestBody(prompt, responseSchema))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                throw new AiQuotaExceededException("Gemini API 사용량 한도를 초과했습니다.", e);
            }
            throw new GeminiClientException("Gemini API 호출에 실패했습니다.", e);
        } catch (RestClientException e) {
            throw new GeminiClientException("Gemini API 호출에 실패했습니다.", e);
        }

        return parseResultNode(rawResponse);
    }

    private ObjectNode buildRequestBody(String prompt, ObjectNode responseSchema) {
        ObjectNode root = objectMapper.createObjectNode();

        ArrayNode parts = objectMapper.createArrayNode();
        parts.add(objectMapper.createObjectNode().put("text", prompt));
        ArrayNode contents = objectMapper.createArrayNode();
        contents.add(objectMapper.createObjectNode().set("parts", parts));
        root.set("contents", contents);

        ObjectNode generationConfig = objectMapper.createObjectNode();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.set("responseSchema", responseSchema);
        generationConfig.set("thinkingConfig", objectMapper.createObjectNode().put("thinkingBudget", 1));
        root.set("generationConfig", generationConfig);

        return root;
    }

    private JsonNode parseResultNode(String rawResponse) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawResponse);
        } catch (JsonProcessingException e) {
            throw new GeminiClientException("Gemini 응답 파싱에 실패했습니다. rawResponse=" + rawResponse, e);
        }

        usageService.recordRequest(root.path("usageMetadata").path("totalTokenCount").asLong(0));

        try {
            String resultText = root.at("/candidates/0/content/parts/0/text").asText();
            return objectMapper.readTree(resultText);
        } catch (JsonProcessingException e) {
            throw new GeminiClientException("Gemini 응답 파싱에 실패했습니다. rawResponse=" + rawResponse, e);
        }
    }
}
