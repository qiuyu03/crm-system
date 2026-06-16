package com.crm.notify.consumer;

import com.crm.customer.service.CustomerService;
import com.crm.notify.config.RabbitMQConfig;
import com.crm.notify.dto.AlertMessage;
import com.crm.notify.entity.Alert;
import com.crm.notify.repository.AlertRepository;
import com.crm.notify.service.FeishuNotifyService;
import com.crm.notify.service.WebSocketNotifyService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertConsumer {

    private final AlertRepository alertRepository;
    private final FeishuNotifyService feishuNotifyService;
    private final WebSocketNotifyService webSocketNotifyService;
    private final CustomerService customerService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PRODUCTION_ALERT)
    public void handleProductionAlert(AlertMessage msg, Channel channel,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        processAlert(msg, channel, deliveryTag);
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_LOGISTICS_ALERT)
    public void handleLogisticsAlert(AlertMessage msg, Channel channel,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        processAlert(msg, channel, deliveryTag);
    }

    /**
     * 预警处理流程：存库 → 飞书推送 → WebSocket广播 → 手动ACK
     * 手动ACK：确保消息处理完成后才从队列移除，避免处理中宕机导致消息丢失。
     */
    private void processAlert(AlertMessage msg, Channel channel, long deliveryTag) throws IOException {
        try {
            // 1. 持久化预警记录
            Alert alert = new Alert();
            alert.setOrderId(msg.getOrderId());
            alert.setAlertType(msg.getAlertType());
            alert.setMessage(msg.getMessage());
            alertRepository.save(alert);

            // 2. 飞书推送
            feishuNotifyService.sendAlert(msg.getOrderNo(), msg.getAlertType(), msg.getMessage());
            alert.setNotifiedAt(LocalDateTime.now());
            alertRepository.save(alert);

            // 3. WebSocket 广播（前端实时刷新预警列表）
            webSocketNotifyService.pushAlert(
                msg.getOrderId(), msg.getOrderNo(), msg.getAlertType(), msg.getMessage()
            );

            // 手动 ACK：确认消息已处理
            channel.basicAck(deliveryTag, false);
            log.info("预警处理完成: 订单[{}] 类型[{}]", msg.getOrderNo(), msg.getAlertType());

        } catch (Exception e) {
            log.error("预警处理失败，消息将重新入队: {}", e.getMessage(), e);
            // requeue=true：消息重新入队，等待下次消费
            channel.basicNack(deliveryTag, false, true);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_COMPLETED)
    public void handleOrderCompleted(OrderCompletedMessage msg, Channel channel,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            customerService.onOrderCompleted(msg.getCustomerId(), msg.getOrderAmount());
            channel.basicAck(deliveryTag, false);
            log.info("订单完成，客户[{}]评分已更新", msg.getCustomerId());
        } catch (Exception e) {
            log.error("处理订单完成事件失败: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
