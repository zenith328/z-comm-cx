package com.zcommcx.guardrail;

import com.zcommcx.order.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * AI가 재량으로 판단하지 않고 서버가 강제하는 하드 룰. 위반 시 실제 처리(취소/반품)는 진행하지 않고
 * 상담원에게 자동 이관하는 것까지가 이 컴포넌트의 책임이다 (이관 실행은 호출한 쪽에서 담당).
 */
@Component
@RequiredArgsConstructor
public class Guardrail {

    private final GuardrailProperties properties;

    public GuardrailDecision checkBeforeShipping(Order order) {
        if (!order.isBeforeShipping()) {
            return GuardrailDecision.block(
                    "이미 배송이 시작된 주문(현재 상태=%s)이라 AI가 직접 취소할 수 없습니다."
                            .formatted(order.getStatus()));
        }
        return GuardrailDecision.pass();
    }

    public GuardrailDecision checkReturnWindow(Order order) {
        LocalDateTime deliveredAt = order.getDeliveredAt();
        if (deliveredAt == null) {
            return GuardrailDecision.pass();
        }
        long daysSinceDelivery = Duration.between(deliveredAt, LocalDateTime.now()).toDays();
        if (daysSinceDelivery > properties.returnWindowDays()) {
            return GuardrailDecision.block(
                    "배송완료 후 %d일이 지나 반품 접수 기한(%d일)이 만료되었습니다."
                            .formatted(daysSinceDelivery, properties.returnWindowDays()));
        }
        return GuardrailDecision.pass();
    }
}
