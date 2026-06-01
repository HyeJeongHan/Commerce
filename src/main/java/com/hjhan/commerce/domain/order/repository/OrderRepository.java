package com.hjhan.commerce.domain.order.repository;

import com.hjhan.commerce.domain.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o JOIN FETCH o.orderItems oi JOIN FETCH oi.product WHERE o.member.id = :memberId")
    Page<Order> findAllByMemberIdWithItems(@Param("memberId") Long memberId, Pageable pageable);

    @Query("SELECT o FROM Order o JOIN FETCH o.orderItems oi JOIN FETCH oi.product WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);
}
