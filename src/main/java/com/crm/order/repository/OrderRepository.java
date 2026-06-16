package com.crm.order.repository;

import com.crm.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 按客户查最近订单（联合索引 idx_customer_create 生效）
    List<Order> findTop10ByCustomerIdOrderByCreateTimeDesc(Long customerId);

    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    Page<Order> findByStatus(String status, Pageable pageable);
}
