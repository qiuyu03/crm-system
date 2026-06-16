package com.crm.order.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_order")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32, unique = true)
    private String orderNo;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false, length = 16)
    private String status = "待确认";

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 8)
    private String currency = "CNY";

    private LocalDate deliveryDate;

    private LocalDateTime shippingDate;

    @Column(length = 32)
    private String createdBy;

    @Column(length = 512)
    private String remark;

    @CreationTimestamp
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;
}
