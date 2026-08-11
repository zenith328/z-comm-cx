package com.zcommcx.personalization.web;

import com.zcommcx.member.domain.Gender;
import com.zcommcx.personalization.service.ProductDescriptionVariantService;
import com.zcommcx.personalization.web.dto.ProductDescriptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * FO 상품상세 화면이 실제로 보여줄 상세설명을 조회한다. gender/age를 주면 해당 세그먼트의
 * 승인된 설명을, 없으면 기본 상세설명을 반환한다 (프론트는 로그인한 회원의 성별/연령을
 * 쿼리파라미터로 그대로 전달한다).
 */
@RestController
@RequestMapping("/api/products/{productId}/description")
@RequiredArgsConstructor
public class ProductDescriptionController {

    private final ProductDescriptionVariantService variantService;

    @GetMapping
    public ProductDescriptionResponse get(
            @PathVariable Long productId,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) Integer age) {
        return ProductDescriptionResponse.from(variantService.resolveDescription(productId, gender, age));
    }
}
