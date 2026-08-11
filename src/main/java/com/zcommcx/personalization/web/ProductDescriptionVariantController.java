package com.zcommcx.personalization.web;

import com.zcommcx.personalization.domain.CustomerSegment;
import com.zcommcx.personalization.domain.ProductDescriptionVariant;
import com.zcommcx.personalization.service.ProductDescriptionVariantService;
import com.zcommcx.personalization.web.dto.ProductDescriptionVariantEditRequest;
import com.zcommcx.personalization.web.dto.ProductDescriptionVariantResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products/{productId}/description-variants")
@RequiredArgsConstructor
public class ProductDescriptionVariantController {

    private final ProductDescriptionVariantService service;

    @GetMapping
    public List<ProductDescriptionVariantResponse> list(@PathVariable Long productId) {
        Map<CustomerSegment, ProductDescriptionVariant> bySegment = service.listByProduct(productId).stream()
                .collect(Collectors.toMap(ProductDescriptionVariant::getSegment, Function.identity()));
        return List.of(CustomerSegment.values()).stream()
                .map(segment -> ProductDescriptionVariantResponse.of(segment, bySegment.get(segment)))
                .toList();
    }

    @PostMapping("/{segment}/generate")
    public ProductDescriptionVariantResponse generate(@PathVariable Long productId, @PathVariable CustomerSegment segment) {
        return ProductDescriptionVariantResponse.of(segment, service.generate(productId, segment));
    }

    @PostMapping("/generate-all")
    public List<ProductDescriptionVariantResponse> generateAll(@PathVariable Long productId) {
        return service.generateAll(productId).stream()
                .map(variant -> ProductDescriptionVariantResponse.of(variant.getSegment(), variant))
                .toList();
    }

    @PostMapping("/{segment}/approve")
    public ProductDescriptionVariantResponse approve(@PathVariable Long productId, @PathVariable CustomerSegment segment) {
        return ProductDescriptionVariantResponse.of(segment, service.approve(productId, segment));
    }

    @PutMapping("/{segment}")
    public ProductDescriptionVariantResponse edit(
            @PathVariable Long productId,
            @PathVariable CustomerSegment segment,
            @Valid @RequestBody ProductDescriptionVariantEditRequest request) {
        return ProductDescriptionVariantResponse.of(segment, service.editContent(productId, segment, request.content()));
    }

    @DeleteMapping("/{segment}")
    public ProductDescriptionVariantResponse delete(@PathVariable Long productId, @PathVariable CustomerSegment segment) {
        service.delete(productId, segment);
        return ProductDescriptionVariantResponse.of(segment, null);
    }
}
