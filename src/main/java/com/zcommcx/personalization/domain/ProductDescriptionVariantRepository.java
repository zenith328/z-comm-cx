package com.zcommcx.personalization.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductDescriptionVariantRepository extends JpaRepository<ProductDescriptionVariant, Long> {

    List<ProductDescriptionVariant> findByProductId(Long productId);

    Optional<ProductDescriptionVariant> findByProductIdAndSegment(Long productId, CustomerSegment segment);
}
