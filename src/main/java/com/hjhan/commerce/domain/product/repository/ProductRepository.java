package com.hjhan.commerce.domain.product.repository;

import com.hjhan.commerce.domain.product.entity.Product;
import com.hjhan.commerce.domain.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // category와 stock을 함께 조회 (N+1 방지)
    @Query("SELECT p FROM Product p JOIN FETCH p.category LEFT JOIN FETCH p.stock WHERE p.status = :status")
    Page<Product> findAllByStatusWithDetails(@Param("status") ProductStatus status, Pageable pageable);

    @Query("SELECT p FROM Product p JOIN FETCH p.category LEFT JOIN FETCH p.stock WHERE p.id = :id")
    java.util.Optional<Product> findByIdWithDetails(@Param("id") Long id);
}
