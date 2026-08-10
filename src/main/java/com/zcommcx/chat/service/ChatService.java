package com.zcommcx.chat.service;

import com.zcommcx.chat.tool.ToolDefinitions;
import com.zcommcx.chat.tool.ToolExecutor;
import com.zcommcx.gemini.client.GeminiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 사용자 발화 하나를 처리하는 동안 필요한 만큼 Gemini function calling 왕복을 반복하는 오케스트레이터.
 * 세션별 대화 히스토리는 데모 목적으로 메모리에만 보관한다(서버 재시작 시 사라짐).
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_TOOL_CALL_ROUNDS = 5;

    private static final Map<String, String> TOOL_LABELS = Map.ofEntries(
            Map.entry("get_order_details", "주문 조회"),
            Map.entry("get_my_orders", "내 주문 목록 조회"),
            Map.entry("cancel_order", "주문 취소"),
            Map.entry("change_shipping_address", "배송지 변경"),
            Map.entry("request_return", "반품 접수"),
            Map.entry("escalate_to_human", "상담원 이관"));

    private static final String SYSTEM_INSTRUCTION_TEMPLATE = """
            당신은 온라인 쇼핑몰의 CS 상담 AI다. 오늘 날짜는 %s이다.
            고객의 주문 취소/배송지 변경/반품 요청을 도구(tool)를 호출해 직접 처리한다.
            - 고객이 "취소/반품 가능한가요?" 처럼 가능 여부만 묻는 경우에는, get_order_details로 주문 상태를 확인해서
              그 상태만으로 가능/불가능을 안내하라(각 tool 설명에 나온 조건 기준). 이 경우 cancel_order/change_shipping_address/
              request_return을 호출하지 마라 — 실제로 처리를 요청한 게 아니므로 실행하거나 상담원에게 이관할 필요가 없다.
            - 고객이 실제로 "취소해줘", "반품 접수해줘", "배송지 바꿔줘" 처럼 처리를 명확히 요청한 경우에만 해당 tool을 호출해서 실행하라.
            - 처리를 실제로 요청했는데 도구 호출 결과의 success가 false면, 그 이유를 고객에게 간단히 설명하고 escalate_to_human 도구를 호출해 상담원에게 이관하라.
            - "내 주문", "오늘/이번주 주문한 것" 처럼 고객 본인의 주문 목록을 묻는 요청에는 get_my_orders를 사용하라.
              상대적인 날짜 표현은 위 오늘 날짜를 기준으로 YYYY-MM-DD로 계산해서 dateFrom/dateTo에 넘겨라.
            - 특정 주문번호를 말한 경우가 아니면, 주문 취소/배송지 변경/반품처럼 특정 주문 하나를 다루는 요청에는 먼저 고객에게 주문번호를 물어봐라.
            - 항상 정중하고 간결한 한국어로 답하라.
            """;

    private final GeminiClient geminiClient;
    private final ToolDefinitions toolDefinitions;
    private final ToolExecutor toolExecutor;
    private final ObjectMapper objectMapper;
    private final Map<String, ArrayNode> sessionHistories = new ConcurrentHashMap<>();

    public String chat(String sessionId, String message, String customerName, String customerPhone) {
        ArrayNode history = sessionHistories.computeIfAbsent(sessionId, id -> objectMapper.createArrayNode());
        history.add(contentOf("user", textPart(message)));
        String systemInstruction = SYSTEM_INSTRUCTION_TEMPLATE.formatted(LocalDate.now());

        for (int round = 0; round < MAX_TOOL_CALL_ROUNDS; round++) {
            JsonNode response = geminiClient.generateContent(history, toolDefinitions.functionDeclarations(), systemInstruction);
            JsonNode modelContent = response.at("/candidates/0/content");
            if (modelContent.isMissingNode()) {
                throw new IllegalStateException("Gemini로부터 유효한 응답을 받지 못했습니다. response=" + response);
            }
            history.add(modelContent);

            ArrayNode parts = (ArrayNode) modelContent.get("parts");
            List<JsonNode> functionCalls = extractFunctionCalls(parts);
            if (functionCalls.isEmpty()) {
                return extractText(parts);
            }

            String chatTranscript = buildTranscript(history);
            ArrayNode functionResponseParts = objectMapper.createArrayNode();
            for (JsonNode call : functionCalls) {
                String name = call.get("name").asText();
                JsonNode args = call.get("args");
                ObjectNode result = toolExecutor.execute(name, args, customerName, customerPhone, chatTranscript);
                functionResponseParts.add(functionResponsePart(name, result));
            }
            history.add(contentOf("user", functionResponseParts));
        }

        throw new IllegalStateException("도구 호출이 너무 많이 반복되어 대화를 처리할 수 없습니다.");
    }

    /**
     * Gemini contents(히스토리)를 운영자가 읽을 수 있는 대화록으로 변환한다.
     * functionResponse 파트는 tool 실행 결과(내부 데이터)일 뿐 실제 발화가 아니므로 대화록에서 제외한다.
     */
    private String buildTranscript(ArrayNode history) {
        StringBuilder transcript = new StringBuilder();
        for (JsonNode content : history) {
            String role = content.path("role").asText();
            String speaker = "model".equals(role) ? "AI" : "고객";
            for (JsonNode part : content.path("parts")) {
                if (part.has("text")) {
                    transcript.append(speaker).append(": ").append(part.get("text").asText()).append('\n');
                } else if (part.has("functionCall")) {
                    String toolName = part.get("functionCall").get("name").asText();
                    transcript.append("AI (처리): ").append(TOOL_LABELS.getOrDefault(toolName, toolName)).append('\n');
                }
            }
        }
        return transcript.toString();
    }

    private List<JsonNode> extractFunctionCalls(ArrayNode parts) {
        List<JsonNode> calls = new ArrayList<>();
        for (JsonNode part : parts) {
            if (part.has("functionCall")) {
                calls.add(part.get("functionCall"));
            }
        }
        return calls;
    }

    private String extractText(ArrayNode parts) {
        StringBuilder text = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.has("text")) {
                text.append(part.get("text").asText());
            }
        }
        return text.toString();
    }

    private ObjectNode contentOf(String role, JsonNode... parts) {
        ArrayNode partsArray = objectMapper.createArrayNode();
        for (JsonNode part : parts) {
            partsArray.add(part);
        }
        return contentOf(role, partsArray);
    }

    private ObjectNode contentOf(String role, ArrayNode parts) {
        ObjectNode content = objectMapper.createObjectNode();
        content.put("role", role);
        content.set("parts", parts);
        return content;
    }

    private ObjectNode textPart(String text) {
        return objectMapper.createObjectNode().put("text", text);
    }

    private ObjectNode functionResponsePart(String name, ObjectNode result) {
        ObjectNode functionResponse = objectMapper.createObjectNode();
        functionResponse.put("name", name);
        functionResponse.set("response", result);
        return objectMapper.createObjectNode().set("functionResponse", functionResponse);
    }
}
