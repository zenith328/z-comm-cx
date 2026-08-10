package com.zcommcx.order.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "cs_order")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true)
    private String orderNo;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String recipientPhone;

    private String zipcode;

    @Column(nullable = false)
    private String address1;

    private String address2;

    @Column(nullable = false)
    private LocalDateTime orderedAt;

    private LocalDateTime shippedAt;

    private LocalDateTime deliveredAt;

    @Column(columnDefinition = "TEXT")
    private String statusReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    public Order(String orderNo, String customerName, String customerPhone,
                 String recipientName, String recipientPhone, String zipcode, String address1, String address2) {
        this.orderNo = orderNo;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.status = OrderStatus.PAID;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.zipcode = zipcode;
        this.address1 = address1;
        this.address2 = address2;
        this.orderedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.assignOrder(this);
    }

    public void cancel(String reason) {
        this.status = OrderStatus.CANCELLED;
        this.statusReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void requestReturn(String reason) {
        this.status = OrderStatus.RETURN_REQUESTED;
        this.statusReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeShippingAddress(String recipientName, String recipientPhone, String zipcode, String address1, String address2) {
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.zipcode = zipcode;
        this.address1 = address1;
        this.address2 = address2;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isBeforeShipping() {
        return status == OrderStatus.PAID || status == OrderStatus.PREPARING;
    }

    public void ship() {
        this.status = OrderStatus.SHIPPING;
        this.shippedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void deliver() {
        this.status = OrderStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public long totalAmount() {
        return items.stream().mapToLong(item -> item.getUnitPrice() * item.getQuantity()).sum();
    }
}
