package com.crm.notify.consumer;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class OrderCompletedMessage implements Serializable {
    private Long orderId;
    private Long customerId;
    private BigDecimal orderAmount;
}
