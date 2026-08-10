package com.zcommcx.order.web;

import com.zcommcx.order.domain.Order;
import com.zcommcx.order.service.OrderService;
import com.zcommcx.order.web.dto.CancelOrderRequest;
import com.zcommcx.order.web.dto.OrderCreateRequest;
import com.zcommcx.order.web.dto.OrderResponse;
import com.zcommcx.order.web.dto.ReturnRequest;
import com.zcommcx.order.web.dto.ShippingAddressChangeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody OrderCreateRequest request) {
        return OrderResponse.from(orderService.createOrder(request));
    }

    @GetMapping
    public List<OrderResponse> list() {
        return orderService.listOrders().stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return OrderResponse.from(orderService.getOrder(id));
    }

    @GetMapping("/by-order-no/{orderNo}")
    public OrderResponse getByOrderNo(@PathVariable String orderNo) {
        return OrderResponse.from(orderService.getOrderByOrderNo(orderNo));
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id, @Valid @RequestBody CancelOrderRequest request) {
        return OrderResponse.from(orderService.cancelOrder(id, request.reason()));
    }

    @PutMapping("/{id}/shipping-address")
    public OrderResponse changeShippingAddress(@PathVariable Long id, @Valid @RequestBody ShippingAddressChangeRequest request) {
        Order order = orderService.changeShippingAddress(
                id, request.recipientName(), request.recipientPhone(), request.zipcode(), request.address1(), request.address2());
        return OrderResponse.from(order);
    }

    @PostMapping("/{id}/return")
    public OrderResponse requestReturn(@PathVariable Long id, @Valid @RequestBody ReturnRequest request) {
        return OrderResponse.from(orderService.requestReturn(id, request.reason()));
    }

    @PostMapping("/{id}/ship")
    public OrderResponse ship(@PathVariable Long id) {
        return OrderResponse.from(orderService.markShipped(id));
    }

    @PostMapping("/{id}/deliver")
    public OrderResponse deliver(@PathVariable Long id) {
        return OrderResponse.from(orderService.markDelivered(id));
    }
}
