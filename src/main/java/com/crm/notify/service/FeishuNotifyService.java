package com.crm.notify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuNotifyService {

    @Value("${feishu.webhook-url}")
    private String webhookUrl;

    private final RestClient restClient = RestClient.create();

    /**
     * 发送飞书机器人文本消息。
     * 飞书自定义机器人 Webhook 接口文档：
     *   POST {webhook-url}
     *   Body: {"msg_type":"text","content":{"text":"消息内容"}}
     */
    public void sendText(String message) {
        try {
            Map<String, Object> body = Map.of(
                "msg_type", "text",
                "content", Map.of("text", message)
            );
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("飞书推送成功: {}", message);
        } catch (Exception e) {
            log.error("飞书推送失败: {}", e.getMessage());
        }
    }

    public void sendAlert(String orderNo, String alertType, String message) {
        String text = String.format("[CRM预警] 订单%s | %s\n%s", orderNo, alertType, message);
        sendText(text);
    }
}
