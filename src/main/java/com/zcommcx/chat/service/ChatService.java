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
            Map.entry("get_products", "상품 조회"),
            Map.entry("get_review_summary", "리뷰 요약"),
            Map.entry("get_order_details", "주문 조회"),
            Map.entry("get_my_orders", "내 주문 목록 조회"),
            Map.entry("cancel_order", "주문 취소"),
            Map.entry("change_shipping_address", "배송지 변경"),
            Map.entry("request_return", "반품 접수"),
            Map.entry("escalate_to_human", "상담원 이관"));

    private static final String SYSTEM_INSTRUCTION_TEMPLATE = """
            당신은 온라인 쇼핑몰의 CS 상담 AI다. 오늘 날짜는 %s이다.
            고객의 주문 취소/배송지 변경/반품 요청을 도구(tool)를 호출해 직접 처리하고, 상품 문의에도 답한다.
            - 고객이 "상품 보여줘", "~있어?", "무슨 브랜드 있어?" 처럼 상품을 찾거나 둘러보고 싶어하면 get_products를
              사용하라. 상품명 키워드나 브랜드로 좁혀서 찾을 수 있고, 둘 다 없으면 최신 상품을 보여준다. 결과가 여러 건이면
              전부 나열하지 말고 상품명/가격/재고 위주로 간결하게 요약해서 안내하라. 이건 조회일 뿐이므로 확인 없이 바로
              실행해도 된다.
            - 고객이 "리뷰 어때", "후기 보여줘", "장점/단점 알려줘"처럼 특정 상품의 리뷰 내용을 직접 물어볼 때만
              get_review_summary를 사용하라. productCode는 get_products 결과에서 얻는다. 상품을 보여줄 때
              리뷰 요약을 먼저 나서서 붙이지 마라 — 고객이 명시적으로 요청했을 때만 호출한다.
            - 고객이 "취소/반품 가능한가요?" 처럼 가능 여부만 묻는 경우에는, get_order_details로 주문 상태를 확인해서
              그 상태만으로 가능/불가능을 안내하라(각 tool 설명에 나온 조건 기준). 이 경우 cancel_order/change_shipping_address/
              request_return을 호출하지 마라 — 실제로 처리를 요청한 게 아니므로 실행하거나 상담원에게 이관할 필요가 없다.
            - 고객이 "취소해줘", "반품 접수해줘", "배송지 바꿔줘" 처럼 처리를 명확히 요청해도, 곧바로 tool을 호출하지 말고
              먼저 무엇을 어떻게 처리할 것인지(주문번호와 처리 내용)를 요약해서 고객에게 한 번 더 확인을 요청하라.
              예: "주문번호 ORD-1234를 취소해 드릴까요? 취소 후에는 되돌릴 수 없습니다." 음성 인식 오류로 실제 의도와
              다르게 전달됐을 수 있으니, 확인 없이 바로 실행하지 않는다. 고객이 "네", "맞아요", "진행해주세요"처럼
              명확히 확인한 다음에만 실제로 해당 tool을 호출해서 실행하라. 아직 확인받지 않았다면 tool을 호출하지
              말고 확인 질문만 하라.
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
            JsonNode partsNode = modelContent.get("parts");
            // 안전 필터에 걸리거나(finishReason=SAFETY 등) 응답이 비정상적으로 잘리면
            // content는 있어도 parts가 없을 수 있다. 검증 전에 history에 먼저 넣으면
            // 이후 이 세션의 모든 턴이 오염된 히스토리를 계속 Gemini에 보내게 되므로,
            // history.add()보다 반드시 먼저 검증한다.
            if (modelContent.isMissingNode() || partsNode == null || !partsNode.isArray()) {
                String finishReason = response.at("/candidates/0/finishReason").asText("UNKNOWN");
                throw new IllegalStateException(
                        "Gemini로부터 유효한 응답을 받지 못했습니다(finishReason=" + finishReason + "). response=" + response);
            }
            ArrayNode parts = (ArrayNode) partsNode;
            history.add(modelContent);

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
