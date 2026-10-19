package com.trading.orderservice.repository;

import com.trading.orderservice.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity,String> {
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(String userId);
}
