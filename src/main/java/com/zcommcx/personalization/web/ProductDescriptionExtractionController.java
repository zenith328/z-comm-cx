package com.zcommcx.personalization.web;

import com.zcommcx.personalization.service.ProductDescriptionUrlExtractionService;
import com.zcommcx.personalization.web.dto.DescriptionExtractRequest;
import com.zcommcx.personalization.web.dto.DescriptionExtractResponse;
import com.zcommcx.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 이미지든 일반 상품 페이지든 URL 하나로 기본 상세설명 텍스트를 채울 수 있게 한다.
 * 어느 쪽인지는 서비스 내부에서 Content-Type으로 자동 판별한다.
 */
@RestController
@RequestMapping("/api/products/{productId}/description")
@RequiredArgsConstructor
public class ProductDescriptionExtractionController {

    private final ProductDescriptionUrlExtractionService urlExtractionService;
    private final ProductService productService;

    @PostMapping("/extract-from-url")
    public DescriptionExtractResponse extractFromUrl(
            @PathVariable Long productId, @Valid @RequestBody DescriptionExtractRequest request) {
        productService.getProduct(productId);
        return new DescriptionExtractResponse(urlExtractionService.extractFromUrl(request.url()));
    }
}
