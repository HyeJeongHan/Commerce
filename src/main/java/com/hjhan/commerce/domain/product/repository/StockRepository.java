package com.hjhan.commerce.domain.product.repository;

import com.hjhan.commerce.domain.product.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProductId(Long productId);
}
