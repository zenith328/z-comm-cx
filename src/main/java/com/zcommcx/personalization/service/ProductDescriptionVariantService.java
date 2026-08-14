package com.zcommcx.personalization.service;

import com.zcommcx.common.exception.NotFoundException;
import com.zcommcx.member.domain.Gender;
import com.zcommcx.personalization.ai.GeneratedDescription;
import com.zcommcx.personalization.ai.ProductDescriptionGenerator;
import com.zcommcx.personalization.domain.CustomerSegment;
import com.zcommcx.personalization.domain.DescriptionVariantStatus;
import com.zcommcx.personalization.domain.ProductDescriptionVariant;
import com.zcommcx.personalization.domain.ProductDescriptionVariantRepository;
import com.zcommcx.personalization.domain.SegmentKeyword;
import com.zcommcx.personalization.domain.SegmentKeywordRepository;
import com.zcommcx.product.domain.Product;
import com.zcommcx.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductDescriptionVariantService {

    private final ProductDescriptionVariantRepository variantRepository;
    private final SegmentKeywordRepository keywordRepository;
    private final ProductService productService;
    private final ProductDescriptionGenerator descriptionGenerator;

    public List<ProductDescriptionVariant> listByProduct(Long productId) {
        productService.getProduct(productId);
        return variantRepository.findByProductId(productId);
    }

    /**
     * FO에 보여줄 상세설명을 고른다. 고객의 성별+연령이 어느 세그먼트에 해당하고 그 세그먼트의
     * 설명이 승인(APPROVED)돼 있으면 그걸 쓰고, 아니면(성별/연령 미입력, 매칭 세그먼트 없음,
     * 미승인 등) 기본 상세설명으로 대체한다.
     */
    public ResolvedDescription resolveDescription(Long productId, Gender gender, Integer age) {
        Product product = productService.getProduct(productId);

        Optional<ProductDescriptionVariant> approvedVariant = CustomerSegment.forGenderAndAge(gender, age)
                .flatMap(segment -> variantRepository.findByProductIdAndSegment(productId, segment))
                .filter(variant -> variant.getStatus() == DescriptionVariantStatus.APPROVED);

        if (approvedVariant.isPresent()) {
            return new ResolvedDescription(approvedVariant.get().getContent(), true);
        }

        return new ResolvedDescription(product.getDescription(), false);
    }

    /**
     * 상품의 기본 상세설명 + 해당 세그먼트에 (전체 상품 공통으로) 등록해 둔 키워드를 함께 AI에
     * 전달해 세그먼트별 상세설명 초안(DRAFT)을 생성/재생성한다. 재생성 시 기존 승인 상태는 초기화된다.
     */
    @Transactional
    public ProductDescriptionVariant generate(Long productId, CustomerSegment segment) {
        Product product = productService.getProduct(productId);
        if (product.getDescription() == null || product.getDescription().isBlank()) {
            throw new IllegalStateException("기본 상품 상세설명을 먼저 입력해야 합니다.");
        }
        String keywords = keywordRepository.findById(segment)
                .map(SegmentKeyword::getKeywords)
                .orElse(null);

        GeneratedDescription generated = descriptionGenerator.generate(
                product.getName(), product.getBrand(), product.getDescription(), segment, keywords);

        return variantRepository.findByProductIdAndSegment(productId, segment)
                .map(existing -> {
                    existing.regenerate(generated.content(), generated.fitScore(), generated.fitScoreReason());
                    return existing;
                })
                .orElseGet(() -> variantRepository.save(new ProductDescriptionVariant(
                        product, segment, generated.content(), generated.fitScore(), generated.fitScoreReason())));
    }

    /** 6개 세그먼트를 한 번에 생성/재생성한다. 개별 생성과 동일한 로직을 세그먼트마다 반복 적용한다. */
    @Transactional
    public List<ProductDescriptionVariant> generateAll(Long productId) {
        return Arrays.stream(CustomerSegment.values())
                .map(segment -> generate(productId, segment))
                .toList();
    }

    @Transactional
    public ProductDescriptionVariant approve(Long productId, CustomerSegment segment) {
        ProductDescriptionVariant variant = variantRepository.findByProductIdAndSegment(productId, segment)
                .orElseThrow(() -> new NotFoundException(
                        "생성된 세그먼트 설명이 없습니다. 먼저 AI 생성을 실행하세요. (segment=%s)".formatted(segment)));
        variant.approve();
        return variant;
    }

    /**
     * 관리자가 내용을 직접 고쳐 쓴다. AI 생성 없이 처음부터 수동으로 입력해도 되고, 생성된 걸
     * 다듬어도 된다. 내용이 바뀌었으니 재생성과 마찬가지로 승인 상태는 초기화하고, AI가 매겼던
     * 적합도 점수는 더 이상 이 내용을 반영하지 않으므로 비운다.
     */
    @Transactional
    public ProductDescriptionVariant editContent(Long productId, CustomerSegment segment, String content) {
        return variantRepository.findByProductIdAndSegment(productId, segment)
                .map(existing -> {
                    existing.editManually(content);
                    return existing;
                })
                .orElseGet(() -> variantRepository.save(
                        new ProductDescriptionVariant(productService.getProduct(productId), segment, content, null, null)));
    }

    /** 세그먼트 설명을 지워 "미생성" 상태로 되돌린다. */
    @Transactional
    public void delete(Long productId, CustomerSegment segment) {
        variantRepository.findByProductIdAndSegment(productId, segment).ifPresent(variantRepository::delete);
    }
}
