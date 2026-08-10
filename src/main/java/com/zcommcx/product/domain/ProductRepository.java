package com.zcommcx.product.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByProductCode(String productCode);

    Page<Product> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Product> findByProductCodeContainingIgnoreCaseOrderByCreatedAtDesc(String productCode, Pageable pageable);

    Page<Product> findByBrandOrderByCreatedAtDesc(String brand, Pageable pageable);

    @Query("SELECT p.brand FROM Product p WHERE p.brand IS NOT NULL AND p.brand <> '' "
            + "GROUP BY p.brand ORDER BY COUNT(p) DESC")
    List<String> findDistinctBrandsOrderByProductCountDesc();
}
