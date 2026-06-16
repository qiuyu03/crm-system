package com.crm.notify.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_alert")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 32)
    private String alertType;

    @Column(nullable = false, length = 512)
    private String message;

    @Column(nullable = false, length = 16)
    private String status = "未处理";

    @Column(length = 32)
    private String handler;

    private LocalDateTime handleTime;

    private LocalDateTime notifiedAt;

    @CreationTimestamp
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;
}
