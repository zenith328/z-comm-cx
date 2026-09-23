package com.zcommcx.avatar.service;

import com.zcommcx.avatar.domain.ShowhostDiscount;
import com.zcommcx.avatar.domain.ShowhostDiscountRepository;
import com.zcommcx.avatar.domain.ShowhostOffer;
import com.zcommcx.avatar.domain.ShowhostOfferRepository;
import com.zcommcx.common.exception.NotFoundException;
import com.zcommcx.product.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * 쇼호스트 방송 특가: 운영자의 상품별 설정({@link ShowhostDiscount}), 방송을 시청한 고객에게
 * 발급하는 특가({@link ShowhostOffer}), 주문 시 특가 사용 처리를 담당한다. 할인가는 항상 서버가
 * 계산한다 — 클라이언트는 특가 ID만 보내고, 가격을 보내지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShowhostDiscountService {

    // "하루 1회" 발급 기준일. 서버(Render)가 UTC라 한국 날짜로 끊는다.
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final ShowhostDiscountRepository discountRepository;
    private final ShowhostOfferRepository offerRepository;

    /** 설정이 없으면 꺼진 기본값을 돌려준다(저장하지 않음). */
    public ShowhostDiscount getSetting(String productCode) {
        return discountRepository.findById(productCode)
                .orElseGet(() -> ShowhostDiscount.disabledDefault(productCode));
    }

    @Transactional
    public ShowhostDiscount updateSetting(String productCode, boolean enabled, int discountRate, int validMinutes) {
        return discountRepository.findById(productCode)
                .map(existing -> {
                    existing.update(enabled, discountRate, validMinutes);
                    return existing;
                })
                .orElseGet(() -> discountRepository.save(
                        new ShowhostDiscount(productCode, enabled, discountRate, validMinutes)));
    }

    /**
     * 고객이 방송에 들어오면 호출한다. 오늘 이미 발급한 특가가 있으면(만료/사용 여부와 상관없이)
     * 그대로 돌려주고, 없을 때 특가가 켜져 있으면 새로 발급한다. 특가가 꺼져 있고 오늘 받은 것도
     * 없으면 빈 값.
     */
    @Transactional
    public Optional<ShowhostOffer> issueOrGetToday(String productCode, String customerName, String customerPhone) {
        Optional<ShowhostOffer> today = findToday(productCode, customerName, customerPhone);
        if (today.isPresent()) {
            return today;
        }
        ShowhostDiscount setting = getSetting(productCode);
        if (!setting.isEnabled()) {
            return Optional.empty();
        }
        ShowhostOffer offer = new ShowhostOffer(
                productCode, customerName, customerPhone, setting.getDiscountRate(),
                LocalDate.now(SERVICE_ZONE), LocalDateTime.now(), setting.getValidMinutes());
        return Optional.of(offerRepository.save(offer));
    }

    /** 주문 화면에서 특가 적용 여부를 보여줄 때 쓴다. 발급은 하지 않는다. */
    public Optional<ShowhostOffer> findToday(String productCode, String customerName, String customerPhone) {
        return offerRepository.findByProductCodeAndCustomerNameAndCustomerPhoneAndIssuedDate(
                productCode, customerName, customerPhone, LocalDate.now(SERVICE_ZONE));
    }

    /**
     * 주문 생성 트랜잭션 안에서 호출한다. 본인·같은 상품·미사용·미만료 특가인지 확인한 뒤 사용
     * 처리하고, 적용할 단가(할인가)를 돌려준다. 조건이 안 맞으면 예외를 던져 주문 전체를 롤백한다.
     */
    @Transactional
    public long redeem(Long offerId, Product product, String customerName, String customerPhone, String orderNo) {
        ShowhostOffer offer = offerRepository.findByIdForUpdate(offerId)
                .orElseThrow(() -> new NotFoundException("방송 특가를 찾을 수 없습니다. (id=%d)".formatted(offerId)));
        LocalDateTime now = LocalDateTime.now();

        if (!offer.getProductCode().equals(product.getProductCode()) || !offer.isOwnedBy(customerName, customerPhone)) {
            throw new IllegalArgumentException("이 주문에 사용할 수 없는 방송 특가입니다.");
        }
        if (offer.isUsed()) {
            throw new IllegalStateException("이미 사용한 방송 특가입니다.");
        }
        if (offer.isExpired(now)) {
            throw new IllegalStateException("방송 특가 사용 시간이 지났습니다. 정가로 주문하려면 상품 화면에서 다시 주문해 주세요.");
        }
        if (product.getPrice() == null) {
            throw new IllegalStateException("가격 정보가 없는 상품에는 방송 특가를 적용할 수 없습니다.");
        }

        offer.markUsed(orderNo, now);
        return ShowhostDiscount.discountedPrice(product.getPrice(), offer.getDiscountRate());
    }
}
