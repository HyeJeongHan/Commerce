package com.hjhan.commerce.domain.product.repository;

import com.hjhan.commerce.domain.product.entity.Stock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    // 비관적 락: 재고 row를 읽는 순간 다른 트랜잭션이 수정 못하게 잠금
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stock s WHERE s.product.id = :productId")
    Optional<Stock> findByProductIdWithLock(@Param("productId") Long productId);

    Optional<Stock> findByProductId(Long productId);
}
