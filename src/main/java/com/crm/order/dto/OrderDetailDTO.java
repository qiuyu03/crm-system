package com.crm.order.dto;

import com.crm.order.entity.Order;
import com.crm.order.entity.OrderItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDetailDTO {
    private Long id;
    private String orderNo;
    private Long customerId;
    private String status;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDate deliveryDate;
    private LocalDateTime shippingDate;
    private String createdBy;
    private String remark;
    private LocalDateTime createTime;
    private List<OrderItem> items;

    public static OrderDetailDTO from(Order o, List<OrderItem> items) {
        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setId(o.getId());
        dto.setOrderNo(o.getOrderNo());
        dto.setCustomerId(o.getCustomerId());
        dto.setStatus(o.getStatus());
        dto.setTotalAmount(o.getTotalAmount());
        dto.setCurrency(o.getCurrency());
        dto.setDeliveryDate(o.getDeliveryDate());
        dto.setShippingDate(o.getShippingDate());
        dto.setCreatedBy(o.getCreatedBy());
        dto.setRemark(o.getRemark());
        dto.setCreateTime(o.getCreateTime());
        dto.setItems(items);
        return dto;
    }
}
