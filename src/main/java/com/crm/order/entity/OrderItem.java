package com.crm.order.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 128)
    private String productName;

    @Column(length = 64)
    private String productCode;

    @Column(nullable = false)
    private Integer qty;

    @Column(nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal unitPrice;

    @Column(nullable = false, precision = 15, scale = 2)
    private java.math.BigDecimal amount;

    @Column(length = 16)
    private String unit;

    @Column(length = 256)
    private String remark;

    @CreationTimestamp
    private LocalDateTime createTime;
}
