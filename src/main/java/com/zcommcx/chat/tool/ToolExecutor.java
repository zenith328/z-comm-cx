package com.zcommcx.chat.tool;

import com.zcommcx.common.exception.NotFoundException;
import com.zcommcx.guardrail.Guardrail;
import com.zcommcx.guardrail.GuardrailDecision;
import com.zcommcx.inventory.service.InventoryService;
import com.zcommcx.member.domain.Member;
import com.zcommcx.member.service.MemberService;
import com.zcommcx.order.domain.Order;
import com.zcommcx.order.domain.OrderItem;
import com.zcommcx.order.service.OrderService;
import com.zcommcx.order.web.dto.OrderCreateRequest;
import com.zcommcx.order.web.dto.OrderItemRequest;
import com.zcommcx.product.domain.Product;
import com.zcommcx.product.service.ProductService;
import com.zcommcx.review.ai.ReviewSummaryResult;
import com.zcommcx.review.domain.Review;
import com.zcommcx.review.domain.ReviewSortOption;
import com.zcommcx.review.service.ReviewService;
import org.springframework.data.domain.Page;
import com.zcommcx.ticket.domain.Ticket;
import com.zcommcx.ticket.domain.TicketCategory;
import com.zcommcx.ticket.service.TicketService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Gemini가 반환한 functionCall(name, args)을 실제 도메인 서비스 호출로 옮기는 실행기.
 * 가드레일 위반 등 비즈니스 예외는 예외를 던지는 대신 success=false 결과로 감싸서
 * 모델이 상황을 이해하고 escalate_to_human으로 이어갈 수 있게 한다.
 */
@Component
@RequiredArgsConstructor
public class ToolExecutor {

    private final OrderService orderService;
    private final TicketService ticketService;
    private final Guardrail guardrail;
    private final ObjectMapper objectMapper;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final ReviewService reviewService;
    private final MemberService memberService;

    public ObjectNode execute(String name, JsonNode args, String customerName, String customerPhone, String chatTranscript) {
        try {
            return switch (name) {
                case "get_products" -> getProducts(args);
                case "get_review_summary" -> getReviewSummary(args);
                case "get_reviews" -> getReviews(args);
                case "charge_balance" -> chargeBalance(args, customerName, customerPhone);
                case "create_order" -> createOrder(args, customerName, customerPhone);
                case "get_order_details" -> getOrderDetails(args);
                case "get_my_orders" -> getMyOrders(args, customerName, customerPhone);
                case "cancel_order" -> cancelOrder(args, customerName, customerPhone, chatTranscript);
                case "change_shipping_address" -> changeShippingAddress(args, chatTranscript);
                case "request_return" -> requestReturn(args, customerName, customerPhone, chatTranscript);
                case "escalate_to_human" -> escalateToHuman(args, customerName, customerPhone, chatTranscript);
                default -> errorResult("알 수 없는 tool 입니다: " + name);
            };
        } catch (NotFoundException | IllegalStateException | IllegalArgumentException e) {
            return errorResult(e.getMessage());
        }
    }

    private ObjectNode getProducts(JsonNode args) {
        List<Product> products = productService.searchForChat(text(args, "keyword"), text(args, "brand"), 10);
        // 여러 건이면 목록 요약이 목적이므로 설명을 짧게 자르고, 검색이 상품 하나로 좁혀졌으면
        // 고객이 그 상품 자체를 자세히 보고 싶어하는 것이므로 설명을 전문 그대로 준다.
        int descriptionLimit = products.size() == 1 ? Integer.MAX_VALUE : 300;

        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("count", products.size());
        ArrayNode productsNode = objectMapper.createArrayNode();
        for (Product product : products) {
            productsNode.add(productSummary(product, descriptionLimit));
        }
        result.set("products", productsNode);
        return result;
    }

    private ObjectNode productSummary(Product product, int descriptionLimit) {
        ObjectNode node = objectMapper.createObjectNode();
        // get_review_summary/get_reviews 호출 시 상품을 특정하는 키로 쓰인다.
        node.put("productCode", product.getProductCode());
        node.put("name", product.getName());
        node.put("brand", product.getBrand());
        node.put("category", product.getCategory());
        node.put("price", product.getPrice());
        node.put("description", truncate(product.getDescription(), descriptionLimit));
        int available = inventoryService.findByProductId(product.getId())
                .map(inv -> inv.getQuantity() - inv.getReservedQuantity())
                .orElse(0);
        node.put("availableQuantity", available);
        return node;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) + "..." : trimmed;
    }

    private ObjectNode getReviewSummary(JsonNode args) {
        String productCode = text(args, "productCode");
        String query = text(args, "query");
        ReviewSummaryResult summaryResult = reviewService.summarizeVisibleReviews(
                productCode, (query == null || query.isBlank()) ? "전반적으로 어떤 평가인지 요약해줘" : query);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("reviewCount", summaryResult.reviewCount());
        result.put("summary", summaryResult.summary());
        return result;
    }

    private ObjectNode getReviews(JsonNode args) {
        String productCode = text(args, "productCode");
        ReviewSortOption sort = ReviewSortOption.LATEST;
        String sortText = text(args, "sort");
        if (sortText != null && !sortText.isBlank()) {
            try {
                sort = ReviewSortOption.valueOf(sortText.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // 알 수 없는 정렬값이 오면 기본값(LATEST)으로 진행한다.
            }
        }
        Page<Review> reviewPage = reviewService.findVisiblePage(productCode, 0, 5, null, null, null, sort);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("totalCount", reviewPage.getTotalElements());
        ArrayNode reviewsNode = objectMapper.createArrayNode();
        for (Review review : reviewPage.getContent()) {
            reviewsNode.add(reviewSummary(review));
        }
        result.set("reviews", reviewsNode);
        return result;
    }

    private ObjectNode reviewSummary(Review review) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("rating", review.getRating());
        node.put("hasPhoto", review.isHasPhoto());
        node.put("content", truncate(review.getContent(), 300));
        node.put("createdAt", review.getCreatedAt().toLocalDate().toString());
        return node;
    }

    private ObjectNode chargeBalance(JsonNode args, String customerName, String customerPhone) {
        if (customerName == null || customerName.isBlank() || customerPhone == null || customerPhone.isBlank()) {
            return errorResult("로그인 정보가 없어 충전할 수 없습니다. 고객에게 로그인을 안내하거나 상담원에게 이관하세요.");
        }
        long amount = args != null && args.hasNonNull("amount") ? args.get("amount").asLong() : 0;
        if (amount <= 0) {
            return errorResult("충전 금액은 0보다 커야 합니다.");
        }
        Member member = memberService.charge(customerName, customerPhone, amount);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("chargedAmount", amount);
        result.put("balance", member.getBalance());
        return result;
    }

    private ObjectNode createOrder(JsonNode args, String customerName, String customerPhone) {
        if (customerName == null || customerName.isBlank() || customerPhone == null || customerPhone.isBlank()) {
            return errorResult("로그인 정보가 없어 주문을 생성할 수 없습니다. 고객에게 로그인을 안내하거나 상담원에게 이관하세요.");
        }
        Product product = productService.getByProductCode(text(args, "productCode"));
        int quantity = args != null && args.hasNonNull("quantity") ? args.get("quantity").asInt() : 1;
        if (quantity <= 0) {
            return errorResult("주문 수량은 1개 이상이어야 합니다.");
        }

        OrderCreateRequest request = new OrderCreateRequest(
                customerName, customerPhone,
                text(args, "recipientName"), text(args, "recipientPhone"),
                text(args, "zipcode"), text(args, "address1"), text(args, "address2"),
                List.of(new OrderItemRequest(product.getId(), quantity)));

        Order order = orderService.createOrder(request);
        return orderResult(order);
    }

    private ObjectNode getMyOrders(JsonNode args, String customerName, String customerPhone) {
        if (customerName == null || customerName.isBlank() || customerPhone == null || customerPhone.isBlank()) {
            return errorResult("로그인 정보가 없어 주문 목록을 조회할 수 없습니다. 고객에게 로그인을 안내하거나 상담원에게 이관하세요.");
        }
        LocalDate dateFrom = parseDate(text(args, "dateFrom"));
        LocalDate dateTo = parseDate(text(args, "dateTo"));
        List<Order> orders = orderService.getOrdersByCustomer(customerName, customerPhone, dateFrom, dateTo);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("count", orders.size());
        ArrayNode ordersNode = objectMapper.createArrayNode();
        for (Order order : orders) {
            ordersNode.add(orderSummary(order));
        }
        result.set("orders", ordersNode);
        return result;
    }

    private ObjectNode getOrderDetails(JsonNode args) {
        Order order = orderService.getOrderByOrderNo(text(args, "orderNo"));
        return orderResult(order);
    }

    private ObjectNode cancelOrder(JsonNode args, String customerName, String customerPhone, String chatTranscript) {
        Order order = orderService.getOrderByOrderNo(text(args, "orderNo"));
        String reason = text(args, "reason");

        GuardrailDecision shippingCheck = guardrail.checkBeforeShipping(order);
        if (shippingCheck.blocked()) {
            return escalateForGuardrail(
                    order, TicketCategory.CANCEL, reason, shippingCheck.reason(), customerName, customerPhone, chatTranscript);
        }

        orderService.cancelOrder(order.getId(), reason);
        ticketService.recordAiResolvedAction(
                order.getId(), TicketCategory.CANCEL,
                "고객 요청으로 주문을 취소했습니다. 사유: " + reason,
                "AI가 주문 취소를 완료했습니다.",
                chatTranscript);
        return orderResult(orderService.getOrder(order.getId()));
    }

    private ObjectNode changeShippingAddress(JsonNode args, String chatTranscript) {
        Order order = orderService.getOrderByOrderNo(text(args, "orderNo"));
        orderService.changeShippingAddress(
                order.getId(),
                text(args, "recipientName"),
                text(args, "recipientPhone"),
                text(args, "zipcode"),
                text(args, "address1"),
                text(args, "address2"));
        ticketService.recordAiResolvedAction(
                order.getId(), TicketCategory.ADDRESS_CHANGE,
                "고객 요청으로 배송지를 변경했습니다.",
                "AI가 배송지 변경을 완료했습니다.",
                chatTranscript);
        return orderResult(orderService.getOrder(order.getId()));
    }

    private ObjectNode requestReturn(JsonNode args, String customerName, String customerPhone, String chatTranscript) {
        Order order = orderService.getOrderByOrderNo(text(args, "orderNo"));
        String reason = text(args, "reason");

        GuardrailDecision windowCheck = guardrail.checkReturnWindow(order);
        if (windowCheck.blocked()) {
            return escalateForGuardrail(
                    order, TicketCategory.RETURN, reason, windowCheck.reason(), customerName, customerPhone, chatTranscript);
        }

        orderService.requestReturn(order.getId(), reason);
        ticketService.recordAiResolvedAction(
                order.getId(), TicketCategory.RETURN,
                "고객 요청으로 반품을 접수했습니다. 사유: " + reason,
                "AI가 반품 접수를 완료했습니다.",
                chatTranscript);
        return orderResult(orderService.getOrder(order.getId()));
    }

    private ObjectNode escalateForGuardrail(Order order, TicketCategory category, String customerReason,
                                             String guardrailReason, String customerName, String customerPhone,
                                             String chatTranscript) {
        String summary = "%s (고객 사유: %s)".formatted(guardrailReason, customerReason);
        Ticket ticket = ticketService.escalateToHuman(
                order.getId(), category, summary, customerName, customerPhone, chatTranscript);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("escalated", true);
        result.put("ticketNo", ticket.getTicketNo());
        result.put("message", guardrailReason + " 상담원에게 자동으로 이관되었습니다. (티켓번호: " + ticket.getTicketNo() + ")");
        return result;
    }

    private ObjectNode escalateToHuman(JsonNode args, String customerName, String customerPhone, String chatTranscript) {
        Long orderId = null;
        String orderNo = text(args, "orderNo");
        if (orderNo != null && !orderNo.isBlank()) {
            orderId = orderService.getOrderByOrderNo(orderNo).getId();
        }
        TicketCategory category = TicketCategory.valueOf(text(args, "category"));
        Ticket ticket = ticketService.escalateToHuman(
                orderId, category, text(args, "summary"), customerName, customerPhone, chatTranscript);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("ticketNo", ticket.getTicketNo());
        result.put("status", ticket.getStatus().name());
        return result;
    }

    private ObjectNode orderResult(Order order) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("orderNo", order.getOrderNo());
        result.put("status", order.getStatus().name());
        result.put("customerName", order.getCustomerName());
        result.put("recipientName", order.getRecipientName());
        result.put("recipientPhone", order.getRecipientPhone());
        result.put("zipcode", order.getZipcode());
        result.put("address1", order.getAddress1());
        result.put("address2", order.getAddress2());
        result.put("statusReason", order.getStatusReason());

        ArrayNode items = objectMapper.createArrayNode();
        for (OrderItem item : order.getItems()) {
            ObjectNode itemNode = objectMapper.createObjectNode();
            itemNode.put("productName", item.getProductNameSnapshot());
            itemNode.put("unitPrice", item.getUnitPrice());
            itemNode.put("quantity", item.getQuantity());
            items.add(itemNode);
        }
        result.set("items", items);
        return result;
    }

    private ObjectNode orderSummary(Order order) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("orderNo", order.getOrderNo());
        node.put("status", order.getStatus().name());
        node.put("orderedAt", order.getOrderedAt().toString());

        ArrayNode items = objectMapper.createArrayNode();
        for (OrderItem item : order.getItems()) {
            ObjectNode itemNode = objectMapper.createObjectNode();
            itemNode.put("productName", item.getProductNameSnapshot());
            itemNode.put("quantity", item.getQuantity());
            items.add(itemNode);
        }
        node.set("items", items);
        return node;
    }

    private LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다 (YYYY-MM-DD): " + text);
        }
    }

    private ObjectNode errorResult(String message) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", false);
        result.put("error", message == null ? "처리 중 오류가 발생했습니다." : message);
        return result;
    }

    private String text(JsonNode args, String field) {
        if (args == null || !args.hasNonNull(field)) {
            return null;
        }
        return args.get(field).asText();
    }
}
