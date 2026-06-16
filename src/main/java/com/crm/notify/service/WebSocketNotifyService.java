package com.crm.notify.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class WebSocketNotifyService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 推送订单状态变更给订阅了该订单的前端客户端。
     * 前端订阅：/topic/orders/{orderId}/status
     */
    public void pushOrderStatusChange(Long orderId, String newStatus) {
        Map<String, Object> payload = Map.of(
            "orderId", orderId,
            "status", newStatus,
            "timestamp", System.currentTimeMillis()
        );
        messagingTemplate.convertAndSend("/topic/orders/" + orderId + "/status", payload);
    }

    /**
     * 推送新预警通知给所有已连接的客户端。
     * 前端订阅：/topic/alerts
     */
    public void pushAlert(Long orderId, String orderNo, String alertType, String message) {
        Map<String, Object> payload = Map.of(
            "orderId", orderId,
            "orderNo", orderNo,
            "alertType", alertType,
            "message", message,
            "timestamp", System.currentTimeMillis()
        );
        messagingTemplate.convertAndSend("/topic/alerts", payload);
    }
}
