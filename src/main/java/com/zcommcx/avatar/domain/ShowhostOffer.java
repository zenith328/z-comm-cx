package com.zcommcx.avatar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 방송을 시청한 고객에게 발급한 방송 특가. 회원(이름+전화번호)·상품별로 하루 1건만 발급해서,
 * 새로고침으로 타이머를 계속 초기화하지 못하게 한다. 할인율은 발급 시점 값을 고정해 저장한다 —
 * 운영자가 도중에 할인율을 바꿔도 이미 방송에서 안내받은 고객의 조건은 바뀌지 않는다.
 * 주문 1건에 사용하면 끝이고, 그 주문을 취소해도 되살리지 않는다.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "showhost_offer",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_showhost_offer_product_customer_date",
                columnNames = {"product_code", "customer_name", "customer_phone", "issued_date"}))
public class ShowhostOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false)
    private String productCode;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Column(name = "discount_rate", nullable = false)
    private int discountRate;

    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    // 이 특가로 결제한 주문번호. 운영자가 어느 주문에 방송 특가가 적용됐는지 추적할 때 쓴다.
    @Column(name = "order_no")
    private String orderNo;

    public ShowhostOffer(
            String productCode, String customerName, String customerPhone, int discountRate,
            LocalDate issuedDate, LocalDateTime issuedAt, int validMinutes) {
        this.productCode = productCode;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.discountRate = discountRate;
        this.issuedDate = issuedDate;
        this.issuedAt = issuedAt;
        this.expiresAt = issuedAt.plusMinutes(validMinutes);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired(LocalDateTime now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isOwnedBy(String name, String phone) {
        return customerName.equals(name) && customerPhone.equals(phone);
    }

    public void markUsed(String orderNo, LocalDateTime now) {
        this.usedAt = now;
        this.orderNo = orderNo;
    }
}
