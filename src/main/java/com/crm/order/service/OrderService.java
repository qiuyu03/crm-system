package com.crm.order.service;

import com.crm.common.exception.BusinessException;
import com.crm.notify.config.RabbitMQConfig;
import com.crm.notify.consumer.OrderCompletedMessage;
import com.crm.notify.dto.AlertMessage;
import com.crm.notify.service.WebSocketNotifyService;
import com.crm.order.dto.OrderCreateDTO;
import com.crm.order.dto.OrderDetailDTO;
import com.crm.order.entity.Order;
import com.crm.order.entity.OrderItem;
import com.crm.order.repository.OrderItemRepository;
import com.crm.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RabbitTemplate rabbitTemplate;
    private final WebSocketNotifyService webSocketNotifyService;

    // 合法的状态流转路径
    private static final java.util.Map<String, List<String>> VALID_TRANSITIONS = java.util.Map.of(
        "待确认", List.of("生产中", "已取消"),
        "生产中", List.of("已发货"),
        "已发货", List.of("已完成")
    );

    public Page<Order> list(String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return orderRepository.findByStatus(status, pageable);
        }
        return orderRepository.findAll(pageable);
    }

    public OrderDetailDTO getDetail(Long id) {
        Order order = findById(id);
        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        return OrderDetailDTO.from(order, items);
    }

    @Transactional
    public Order create(OrderCreateDTO dto) {
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setCustomerId(dto.getCustomerId());
        order.setDeliveryDate(dto.getDeliveryDate());
        order.setCurrency(dto.getCurrency());
        order.setCreatedBy(dto.getCreatedBy());
        order.setRemark(dto.getRemark());

        // 计算总金额，同时创建明细
        BigDecimal total = BigDecimal.ZERO;
        order = orderRepository.save(order);
        for (OrderCreateDTO.ItemDTO itemDto : dto.getItems()) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductName(itemDto.getProductName());
            item.setProductCode(itemDto.getProductCode());
            item.setQty(itemDto.getQty());
            item.setUnitPrice(itemDto.getUnitPrice());
            item.setUnit(itemDto.getUnit());
            item.setRemark(itemDto.getRemark());
            BigDecimal amount = itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQty()));
            item.setAmount(amount);
            total = total.add(amount);
            orderItemRepository.save(item);
        }

        order.setTotalAmount(total);
        return orderRepository.save(order);
    }

    /**
     * 订单状态变更，包含：
     *   1. 校验状态流转合法性
     *   2. 持久化
     *   3. WebSocket 推送前端
     *   4. 发 MQ 事件（完成时触发客户评分更新）
     */
    @Transactional
    public Order changeStatus(Long orderId, String newStatus) {
        Order order = findById(orderId);
        String currentStatus = order.getStatus();

        List<String> allowed = VALID_TRANSITIONS.getOrDefault(currentStatus, List.of());
        if (!allowed.contains(newStatus)) {
            throw new BusinessException(
                String.format("不允许从[%s]变更为[%s]", currentStatus, newStatus)
            );
        }

        order.setStatus(newStatus);
        if ("已发货".equals(newStatus)) {
            order.setShippingDate(LocalDateTime.now());
        }
        Order saved = orderRepository.save(order);

        // WebSocket 实时推送
        webSocketNotifyService.pushOrderStatusChange(orderId, newStatus);

        // 订单完成：发 MQ 触发客户评分更新
        if ("已完成".equals(newStatus)) {
            OrderCompletedMessage msg = new OrderCompletedMessage();
            msg.setOrderId(orderId);
            msg.setCustomerId(order.getCustomerId());
            msg.setOrderAmount(order.getTotalAmount());
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.KEY_ORDER_COMPLETED, msg);
        }

        return saved;
    }

    /**
     * 手动触发预警（测试用，实际场景由定时任务检测交期临近触发）
     */
    public void triggerAlert(Long orderId, String alertType, String message) {
        Order order = findById(orderId);
        AlertMessage alert = new AlertMessage(
            orderId, order.getOrderNo(), alertType, message, order.getCreatedBy()
        );
        String routingKey = "生产异常".equals(alertType)
            ? RabbitMQConfig.KEY_PRODUCTION_ALERT
            : RabbitMQConfig.KEY_LOGISTICS_ALERT;
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, routingKey, alert);
        log.info("预警已投递: 订单[{}] 类型[{}]", order.getOrderNo(), alertType);
    }

    private Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
    }

    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "ORD" + date + random;
    }
}
