package com.zcommcx.avatar.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface ShowhostOfferRepository extends JpaRepository<ShowhostOffer, Long> {

    Optional<ShowhostOffer> findByProductCodeAndCustomerNameAndCustomerPhoneAndIssuedDate(
            String productCode, String customerName, String customerPhone, LocalDate issuedDate);

    // 같은 특가로 주문 두 건이 동시에 들어와 둘 다 할인받는 일이 없도록 사용 처리 시 행을 잠근다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from ShowhostOffer o where o.id = :id")
    Optional<ShowhostOffer> findByIdForUpdate(@Param("id") Long id);
}
