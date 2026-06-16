package com.crm.notify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertMessage implements Serializable {
    private Long orderId;
    private String orderNo;
    private String alertType;   // 生产异常 / 物流延误 / 交期预警
    private String message;
    private String responsible; // 责任人（用于飞书@）
}
