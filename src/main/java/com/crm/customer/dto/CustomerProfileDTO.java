package com.crm.customer.dto;

import com.crm.customer.entity.Customer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CustomerProfileDTO {

    // 客户基础信息
    private Long id;
    private String name;
    private String company;
    private String contact;
    private String email;
    private String level;
    private Integer score;
    private BigDecimal totalAmount;
    private Integer orderCount;
    private Integer creditDays;

    // 最近订单摘要（最多10条）
    private List<OrderSummary> recentOrders;

    // 预警历史（最多20条）
    private List<AlertSummary> recentAlerts;

    @Data
    public static class OrderSummary {
        private Long orderId;
        private String orderNo;
        private String status;
        private BigDecimal totalAmount;
        private LocalDateTime createTime;
    }

    @Data
    public static class AlertSummary {
        private Long alertId;
        private String alertType;
        private String message;
        private String status;
        private LocalDateTime createTime;
    }

    public static CustomerProfileDTO from(Customer c) {
        CustomerProfileDTO dto = new CustomerProfileDTO();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setCompany(c.getCompany());
        dto.setContact(c.getContact());
        dto.setEmail(c.getEmail());
        dto.setLevel(c.getLevel());
        dto.setScore(c.getScore());
        dto.setTotalAmount(c.getTotalAmount());
        dto.setOrderCount(c.getOrderCount());
        dto.setCreditDays(c.getCreditDays());
        return dto;
    }
}
