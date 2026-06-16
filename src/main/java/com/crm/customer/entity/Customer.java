package com.crm.customer.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 128)
    private String company;

    @Column(length = 32)
    private String contact;

    @Column(length = 64)
    private String email;

    @Column(nullable = false, length = 1)
    private String level = "C";

    @Column(nullable = false)
    private Integer score = 0;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer orderCount = 0;

    @Column(nullable = false)
    private Integer creditDays = 30;

    @Column(nullable = false)
    private Integer overdueCount = 0;

    @Column(length = 512)
    private String remark;

    @CreationTimestamp
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;
}
