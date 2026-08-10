package com.zcommcx.product.service;

import com.zcommcx.common.exception.NotFoundException;
import com.zcommcx.inventory.service.InventoryService;
import com.zcommcx.product.domain.Product;
import com.zcommcx.product.domain.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductInfoScraper scraper;
    private final ProductRepository productRepository;
    private final ExternalReviewImportService reviewImportService;
    private final InventoryService inventoryService;

    /**
     * 이미 등록된 상품코드면 최신 정보로 갱신하고, 처음이면 새로 등록한다.
     * 처음 등록하는 경우에만 외부 리뷰를 함께 가져오고, 재고를 0으로 생성한다
     * (재등록 시 중복 등록/재고 초기화 방지).
     */
    @Transactional
    public Product registerFromUrl(String url) {
        ProductInfo info = scraper.fetch(url);
        return productRepository.findByProductCode(info.productCode())
                .map(existing -> {
                    existing.updateFrom(info);
                    return existing;
                })
                .orElseGet(() -> {
                    Product product = productRepository.save(new Product(info));
                    reviewImportService.importReviews(info.sourceUrl(), info.productCode());
                    inventoryService.createForProduct(product, 0);
                    return product;
                });
    }

    public Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("상품을 찾을 수 없습니다. (id=%d)".formatted(id)));
    }

    public Page<Product> findAll(int page, int size, String productCode, String brand) {
        Pageable pageable = PageRequest.of(page, size);
        if (brand != null && !brand.isBlank()) {
            return productRepository.findByBrandOrderByCreatedAtDesc(brand.trim(), pageable);
        }
        if (productCode == null || productCode.isBlank()) {
            return productRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return productRepository.findByProductCodeContainingIgnoreCaseOrderByCreatedAtDesc(
                productCode.trim(), pageable);
    }

    public List<String> findAllBrands() {
        return productRepository.findDistinctBrandsOrderByProductCountDesc();
    }
}
