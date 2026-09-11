package com.zcommcx.order.service;

import com.zcommcx.common.exception.NotFoundException;
import com.zcommcx.inventory.service.InventoryService;
import com.zcommcx.member.service.MemberService;
import com.zcommcx.order.domain.Order;
import com.zcommcx.order.domain.OrderItem;
import com.zcommcx.order.domain.OrderRepository;
import com.zcommcx.order.domain.OrderStatus;
import com.zcommcx.order.web.dto.OrderCreateRequest;
import com.zcommcx.order.web.dto.OrderItemRequest;
import com.zcommcx.product.domain.Product;
import com.zcommcx.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final MemberService memberService;

    @Transactional
    public Order createOrder(OrderCreateRequest request) {
        Order order = new Order(
                generateOrderNo(),
                request.customerName(),
                request.customerPhone(),
                request.recipientName(),
                request.recipientPhone(),
                request.zipcode(),
                request.address1(),
                request.address2());

        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productService.getProduct(itemRequest.productId());
            inventoryService.reserve(product.getId(), itemRequest.quantity());
            order.addItem(new OrderItem(product, product.getName(), product.getPrice(), itemRequest.quantity()));
        }

        // CX-Pay(유일한 결제수단)에서 결제금액을 차감한다. 잔액이 부족하면 여기서 예외가 나서
        // 트랜잭션 전체(재고 예약 포함)가 롤백되고 주문은 생성되지 않는다.
        memberService.use(request.customerName(), request.customerPhone(), order.totalAmount(), order.getOrderNo());

        return orderRepository.save(order);
    }

    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("주문을 찾을 수 없습니다. (id=%d)".formatted(id)));
    }

    public Order getOrderByOrderNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new NotFoundException("주문을 찾을 수 없습니다. (orderNo=%s)".formatted(orderNo)));
    }

    public List<Order> listOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getOrdersByCustomer(String customerName, String customerPhone, LocalDate dateFrom, LocalDate dateTo) {
        return orderRepository.findByCustomerNameAndCustomerPhoneOrderByOrderedAtDesc(customerName, customerPhone).stream()
                .filter(order -> dateFrom == null || !order.getOrderedAt().toLocalDate().isBefore(dateFrom))
                .filter(order -> dateTo == null || !order.getOrderedAt().toLocalDate().isAfter(dateTo))
                .toList();
    }

    @Transactional
    public Order cancelOrder(Long id, String reason) {
        Order order = getOrder(id);
        if (!order.isBeforeShipping()) {
            throw new IllegalStateException("배송이 시작된 주문은 취소할 수 없습니다. (현재 상태=%s)".formatted(order.getStatus()));
        }
        for (OrderItem item : order.getItems()) {
            inventoryService.release(item.getProduct().getId(), item.getQuantity());
        }
        memberService.refund(order.getCustomerName(), order.getCustomerPhone(), order.totalAmount(), order.getOrderNo());
        order.cancel(reason);
        return order;
    }

    @Transactional
    public Order changeShippingAddress(Long id, String recipientName, String recipientPhone,
                                        String zipcode, String address1, String address2) {
        Order order = getOrder(id);
        if (!order.isBeforeShipping()) {
            throw new IllegalStateException("배송이 시작된 주문은 배송지를 변경할 수 없습니다. (현재 상태=%s)".formatted(order.getStatus()));
        }
        order.changeShippingAddress(recipientName, recipientPhone, zipcode, address1, address2);
        return order;
    }

    @Transactional
    public Order requestReturn(Long id, String reason) {
        Order order = getOrder(id);
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("배송 완료된 주문만 반품 접수할 수 있습니다. (현재 상태=%s)".formatted(order.getStatus()));
        }
        order.requestReturn(reason);
        return order;
    }

    /**
     * 관리자가 반품접수 상태의 주문을 최종 확정한다(실물 반품 확인 후). 이 시점에 CX-Pay로
     * 환불한다 — 반품접수 시점이 아니라 확정 시점에 환불하는 이유는, 접수만 하고 아직 실제
     * 반품 처리가 끝나기 전인데 미리 돈부터 돌려주면 안 되기 때문이다.
     */
    @Transactional
    public Order completeReturn(Long id) {
        Order order = getOrder(id);
        if (order.getStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new IllegalStateException("반품접수 상태의 주문만 반품 확정할 수 있습니다. (현재 상태=%s)".formatted(order.getStatus()));
        }
        memberService.refund(order.getCustomerName(), order.getCustomerPhone(), order.totalAmount(), order.getOrderNo());
        order.completeReturn();
        return order;
    }

    @Transactional
    public Order markShipped(Long id) {
        Order order = getOrder(id);
        if (!order.isBeforeShipping()) {
            throw new IllegalStateException("이미 배송이 시작된 주문입니다. (현재 상태=%s)".formatted(order.getStatus()));
        }
        for (OrderItem item : order.getItems()) {
            inventoryService.deduct(item.getProduct().getId(), item.getQuantity());
        }
        order.ship();
        return order;
    }

    @Transactional
    public Order markDelivered(Long id) {
        Order order = getOrder(id);
        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new IllegalStateException("배송중인 주문만 배송완료 처리할 수 있습니다. (현재 상태=%s)".formatted(order.getStatus()));
        }
        order.deliver();
        return order;
    }

    private String generateOrderNo() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
