package com.zcommcx.avatar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 상품별 "방송 특가" 설정(상품당 1건). 쇼호스트 방송이 실제 라이브가 아니라 온디맨드라 방송
 * 시간대 대신 "이 고객이 방송에 들어온 시점부터 validMinutes분"을 방송 중으로 보고, 그 사이에만
 * 쓸 수 있는 특가({@link ShowhostOffer})를 고객별로 발급한다. 설정이 없는 상품은 특가 없음.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "showhost_discount")
public class ShowhostDiscount {

    public static final int MIN_RATE = 1;
    public static final int MAX_RATE = 90;
    public static final int MIN_VALID_MINUTES = 5;
    public static final int MAX_VALID_MINUTES = 180;
    public static final int DEFAULT_RATE = 10;
    public static final int DEFAULT_VALID_MINUTES = 30;

    @Id
    @Column(name = "product_code", nullable = false)
    private String productCode;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "discount_rate", nullable = false)
    private int discountRate;

    @Column(name = "valid_minutes", nullable = false)
    private int validMinutes;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ShowhostDiscount(String productCode, boolean enabled, int discountRate, int validMinutes) {
        this.productCode = productCode;
        update(enabled, discountRate, validMinutes);
    }

    /** 설정이 아직 없는 상품의 기본값(꺼짐). 저장하지 않고 조회 응답용으로만 쓴다. */
    public static ShowhostDiscount disabledDefault(String productCode) {
        return new ShowhostDiscount(productCode, false, DEFAULT_RATE, DEFAULT_VALID_MINUTES);
    }

    public void update(boolean enabled, int discountRate, int validMinutes) {
        if (discountRate < MIN_RATE || discountRate > MAX_RATE) {
            throw new IllegalArgumentException(
                    "할인율은 %d~%d%% 사이로 입력해 주세요.".formatted(MIN_RATE, MAX_RATE));
        }
        if (validMinutes < MIN_VALID_MINUTES || validMinutes > MAX_VALID_MINUTES) {
            throw new IllegalArgumentException(
                    "유효 시간은 %d~%d분 사이로 입력해 주세요.".formatted(MIN_VALID_MINUTES, MAX_VALID_MINUTES));
        }
        this.enabled = enabled;
        this.discountRate = discountRate;
        this.validMinutes = validMinutes;
        this.updatedAt = LocalDateTime.now();
    }

    /** 정가에 할인율을 적용한 가격(원 미만 버림). */
    public static long discountedPrice(long price, int discountRate) {
        return price * (100 - discountRate) / 100;
    }
}
