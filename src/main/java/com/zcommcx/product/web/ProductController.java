package com.zcommcx.product.web;

import com.zcommcx.common.web.dto.PageResponse;
import com.zcommcx.inventory.domain.Inventory;
import com.zcommcx.inventory.service.InventoryService;
import com.zcommcx.inventory.web.dto.InventoryResponse;
import com.zcommcx.inventory.web.dto.RestockRequest;
import com.zcommcx.product.domain.Product;
import com.zcommcx.product.service.ProductService;
import com.zcommcx.product.web.dto.ProductCreateRequest;
import com.zcommcx.product.web.dto.ProductDescriptionUpdateRequest;
import com.zcommcx.product.web.dto.ProductResponse;
import com.zcommcx.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ReviewService reviewService;
    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<ProductResponse> registerProduct(@Valid @RequestBody ProductCreateRequest request) {
        ProductResponse response = toResponse(productService.registerFromUrl(request.url()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public PageResponse<ProductResponse> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String brand) {
        Page<Product> products = productService.findAll(page, size, productCode, brand);
        return PageResponse.from(products, this::toResponse);
    }

    @GetMapping("/brands")
    public List<String> getBrands() {
        return productService.findAllBrands();
    }

    @GetMapping("/{id}")
    public ProductResponse getProduct(@PathVariable Long id) {
        return toResponse(productService.getProduct(id));
    }

    @PutMapping("/{id}/description")
    public ProductResponse updateDescription(@PathVariable Long id, @Valid @RequestBody ProductDescriptionUpdateRequest request) {
        return toResponse(productService.updateDescription(id, request.description()));
    }

    @GetMapping("/{id}/inventory")
    public InventoryResponse getInventory(@PathVariable Long id) {
        return InventoryResponse.from(inventoryService.getByProductId(id));
    }

    @PostMapping("/{id}/inventory/restock")
    public InventoryResponse restock(@PathVariable Long id, @Valid @RequestBody RestockRequest request) {
        return InventoryResponse.from(inventoryService.restock(id, request.quantity()));
    }

    @PostMapping("/{id}/inventory/out-of-stock")
    public InventoryResponse markOutOfStock(@PathVariable Long id) {
        return InventoryResponse.from(inventoryService.markOutOfStock(id));
    }

    private ProductResponse toResponse(Product product) {
        long reviewCount = reviewService.countVisibleReviews(product.getProductCode());
        Inventory inventory = inventoryService.findByProductId(product.getId()).orElse(null);
        Integer quantity = inventory != null ? inventory.getQuantity() : null;
        Integer reserved = inventory != null ? inventory.getReservedQuantity() : null;
        return ProductResponse.from(product, reviewCount, quantity, reserved);
    }
}
