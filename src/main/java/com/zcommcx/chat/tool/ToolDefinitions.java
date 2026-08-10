package com.zcommcx.chat.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

/**
 * CS 자동 해결 에이전트가 Gemini Function Calling으로 호출할 수 있는 Tool(함수) 목록.
 * 실제 호출 실행은 {@link ToolExecutor}가 담당한다.
 */
@Component
public class ToolDefinitions {

    private final ObjectMapper objectMapper;

    public ToolDefinitions(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ArrayNode functionDeclarations() {
        ArrayNode declarations = objectMapper.createArrayNode();
        declarations.add(getOrderDetails());
        declarations.add(getMyOrders());
        declarations.add(cancelOrder());
        declarations.add(changeShippingAddress());
        declarations.add(requestReturn());
        declarations.add(escalateToHuman());
        return declarations;
    }

    private ObjectNode getMyOrders() {
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("dateFrom", stringProp("조회 시작일 (YYYY-MM-DD). 생략하면 전체 기간."));
        properties.set("dateTo", stringProp("조회 종료일 (YYYY-MM-DD). 생략하면 전체 기간."));
        return tool(
                "get_my_orders",
                "현재 대화 중인 고객 본인의 주문 목록을 조회한다. '내 주문', '오늘 주문한 것', '이번주 주문' 같은 요청에 사용한다. "
                        + "고객이 특정 주문번호를 말하지 않고 자신의 주문 목록/이력을 물을 때 이 tool을 쓴다. "
                        + "상대적인 날짜 표현은 시스템 안내에 있는 오늘 날짜를 기준으로 YYYY-MM-DD로 계산해서 dateFrom/dateTo에 넘긴다.",
                properties);
    }

    private ObjectNode getOrderDetails() {
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("orderNo", stringProp("고객이 알려준 주문번호"));
        return tool(
                "get_order_details",
                "주문번호로 주문 상세 정보(상태, 주문상품, 배송지, 취소/반품 사유 등)를 조회한다.",
                properties, "orderNo");
    }

    private ObjectNode cancelOrder() {
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("orderNo", stringProp("취소할 주문번호"));
        properties.set("reason", stringProp("취소 사유"));
        return tool(
                "cancel_order",
                "주문을 취소한다. 배송이 시작되지 않은 주문(PAID, PREPARING)만 취소할 수 있다. "
                        + "배송이 시작된 주문에 호출하면 오류가 반환되며, 이 경우 escalate_to_human으로 상담원에게 이관해야 한다.",
                properties, "orderNo", "reason");
    }

    private ObjectNode changeShippingAddress() {
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("orderNo", stringProp("배송지를 변경할 주문번호"));
        properties.set("recipientName", stringProp("새 수령인 이름"));
        properties.set("recipientPhone", stringProp("새 수령인 연락처"));
        properties.set("zipcode", stringProp("새 우편번호"));
        properties.set("address1", stringProp("새 기본주소"));
        properties.set("address2", stringProp("새 상세주소"));
        return tool(
                "change_shipping_address",
                "배송지를 변경한다. 배송이 시작되지 않은 주문(PAID, PREPARING)만 변경할 수 있다. "
                        + "배송이 시작된 주문에 호출하면 오류가 반환되며, 이 경우 escalate_to_human으로 상담원에게 이관해야 한다.",
                properties, "orderNo", "recipientName", "recipientPhone", "address1");
    }

    private ObjectNode requestReturn() {
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("orderNo", stringProp("반품 접수할 주문번호"));
        properties.set("reason", stringProp("반품 사유"));
        return tool(
                "request_return",
                "반품을 접수한다. 배송완료(DELIVERED) 상태의 주문만 반품 접수할 수 있다. "
                        + "그 외 상태에 호출하면 오류가 반환되며, 이 경우 escalate_to_human으로 상담원에게 이관해야 한다.",
                properties, "orderNo", "reason");
    }

    private ObjectNode escalateToHuman() {
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("orderNo", stringProp("관련 주문번호 (없으면 생략)"));
        ObjectNode category = stringProp("문의 유형");
        category.set("enum", objectMapper.createArrayNode()
                .add("CANCEL").add("ADDRESS_CHANGE").add("RETURN").add("INQUIRY").add("OTHER"));
        properties.set("category", category);
        properties.set("summary", stringProp("상담원이 이어서 처리할 수 있도록 하는 상황 요약"));
        return tool(
                "escalate_to_human",
                "AI가 직접 처리할 수 없는 요청(가드레일 위반, 고객이 상담원을 원하는 경우 등)을 "
                        + "CS 티켓으로 만들어 상담원에게 이관한다.",
                properties, "category", "summary");
    }

    private ObjectNode tool(String name, String description, ObjectNode properties, String... required) {
        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "OBJECT");
        parameters.set("properties", properties);
        ArrayNode requiredArray = objectMapper.createArrayNode();
        for (String field : required) {
            requiredArray.add(field);
        }
        parameters.set("required", requiredArray);

        ObjectNode declaration = objectMapper.createObjectNode();
        declaration.put("name", name);
        declaration.put("description", description);
        declaration.set("parameters", parameters);
        return declaration;
    }

    private ObjectNode stringProp(String description) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "STRING");
        node.put("description", description);
        return node;
    }
}
