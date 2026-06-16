package com.crm.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.exception.BusinessException;
import com.crm.notify.config.RabbitMQConfig;
import com.crm.notify.consumer.OrderCompletedMessage;
import com.crm.notify.dto.AlertMessage;
import com.crm.notify.service.WebSocketNotifyService;
import com.crm.order.dto.OrderCreateDTO;
import com.crm.order.dto.OrderDetailDTO;
import com.crm.order.entity.Order;
import com.crm.order.entity.OrderItem;
import com.crm.order.mapper.OrderItemMapper;
import com.crm.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final RabbitTemplate rabbitTemplate;
    private final WebSocketNotifyService webSocketNotifyService;

    private static final Map<String, List<String>> VALID_TRANSITIONS = Map.of(
        "待确认", List.of("生产中", "已取消"),
        "生产中", List.of("已发货"),
        "已发货", List.of("已完成")
    );

    /**
     * 分页查询订单，支持按状态过滤
     */
    public Page<Order> list(String status, Page<Order> page) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
            .eq(status != null && !status.isBlank(), Order::getStatus, status)
            .orderByDesc(Order::getCreateTime);
        return orderMapper.selectPage(page, wrapper);
    }

    public OrderDetailDTO getDetail(Long id) {
        Order order = findById(id);
        List<OrderItem> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id)
        );
        return OrderDetailDTO.from(order, items);
    }

    @Transactional
    public Order create(OrderCreateDTO dto) {
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setCustomerId(dto.getCustomerId());
        order.setDeliveryDate(dto.getDeliveryDate());
        order.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "CNY");
        order.setCreatedBy(dto.getCreatedBy());
        order.setRemark(dto.getRemark());
        order.setStatus("待确认");
        order.setTotalAmount(BigDecimal.ZERO);
        orderMapper.insert(order);

        BigDecimal total = BigDecimal.ZERO;
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
            orderItemMapper.insert(item);
        }

        // 回填总金额
        order.setTotalAmount(total);
        orderMapper.updateById(order);
        return order;
    }

    /**
     * 状态流转：校验合法性 → 持久化 → WebSocket 推送 → MQ 事件
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
        orderMapper.updateById(order);

        webSocketNotifyService.pushOrderStatusChange(orderId, newStatus);

        if ("已完成".equals(newStatus)) {
            OrderCompletedMessage msg = new OrderCompletedMessage();
            msg.setOrderId(orderId);
            msg.setCustomerId(order.getCustomerId());
            msg.setOrderAmount(order.getTotalAmount());
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.KEY_ORDER_COMPLETED, msg);
        }

        return order;
    }

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
        Order order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException(404, "订单不存在");
        return order;
    }

    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "ORD" + date + random;
    }
}
