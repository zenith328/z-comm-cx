package com.zcommcx.avatar.web;

import com.zcommcx.avatar.service.ShowhostDiscountService;
import com.zcommcx.avatar.web.dto.ShowhostDiscountRequest;
import com.zcommcx.avatar.web.dto.ShowhostDiscountResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 상품별 방송 특가 설정. 조회는 고객 방송 화면(특가 안내 멘트)과 운영자 상품상세관리 "쇼호스트 방송"
 * 탭이 함께 쓰고, 변경은 운영자 탭에서만 한다.
 */
@RestController
@RequestMapping("/api/products/{productCode}/showhost/discount")
@RequiredArgsConstructor
public class ShowhostDiscountController {

    private final ShowhostDiscountService discountService;

    @GetMapping
    public ShowhostDiscountResponse get(@PathVariable String productCode) {
        return ShowhostDiscountResponse.from(discountService.getSetting(productCode));
    }

    @PutMapping
    public ShowhostDiscountResponse update(
            @PathVariable String productCode, @Valid @RequestBody ShowhostDiscountRequest request) {
        return ShowhostDiscountResponse.from(discountService.updateSetting(
                productCode, request.enabled(), request.discountRate(), request.validMinutes()));
    }
}
