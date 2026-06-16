package com.crm.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.exception.BusinessException;
import com.crm.customer.dto.CustomerCreateDTO;
import com.crm.customer.dto.CustomerProfileDTO;
import com.crm.customer.entity.Customer;
import com.crm.customer.mapper.CustomerMapper;
import com.crm.notify.entity.Alert;
import com.crm.notify.mapper.AlertMapper;
import com.crm.order.entity.Order;
import com.crm.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerMapper customerMapper;
    private final OrderMapper orderMapper;
    private final AlertMapper alertMapper;
    private final CustomerScoringService scoringService;

    /**
     * 分页查询，支持等级过滤 + 关键字搜索（XML 动态 SQL）
     */
    public Page<Customer> list(String level, String keyword, Page<Customer> page) {
        return customerMapper.search(page, level, keyword);
    }

    public Customer getById(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) throw new BusinessException(404, "客户不存在");
        return customer;
    }

    /**
     * 客户全景视图：聚合客户信息、最近订单、预警历史。
     * 避免 N+1：订单用 LambdaQueryWrapper LIMIT 10，预警用 IN 批量查。
     */
    public CustomerProfileDTO getProfile(Long customerId) {
        Customer customer = getById(customerId);
        CustomerProfileDTO profile = CustomerProfileDTO.from(customer);

        // 最近10笔订单（走联合索引 idx_customer_create）
        List<Order> orders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getCustomerId, customerId)
                .orderByDesc(Order::getCreateTime)
                .last("LIMIT 10")
        );
        profile.setRecentOrders(orders.stream().map(o -> {
            CustomerProfileDTO.OrderSummary s = new CustomerProfileDTO.OrderSummary();
            s.setOrderId(o.getId());
            s.setOrderNo(o.getOrderNo());
            s.setStatus(o.getStatus());
            s.setTotalAmount(o.getTotalAmount());
            s.setCreateTime(o.getCreateTime());
            return s;
        }).collect(Collectors.toList()));

        // IN 查预警，避免循环查库
        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        if (!orderIds.isEmpty()) {
            List<Alert> alerts = alertMapper.findTop20ByOrderIdIn(orderIds);
            profile.setRecentAlerts(alerts.stream().map(a -> {
                CustomerProfileDTO.AlertSummary s = new CustomerProfileDTO.AlertSummary();
                s.setAlertId(a.getId());
                s.setAlertType(a.getAlertType());
                s.setMessage(a.getMessage());
                s.setStatus(a.getStatus());
                s.setCreateTime(a.getCreateTime());
                return s;
            }).collect(Collectors.toList()));
        } else {
            profile.setRecentAlerts(Collections.emptyList());
        }

        return profile;
    }

    @Transactional
    public Customer create(CustomerCreateDTO dto) {
        Customer customer = new Customer();
        customer.setName(dto.getName());
        customer.setCompany(dto.getCompany());
        customer.setContact(dto.getContact());
        customer.setEmail(dto.getEmail());
        customer.setCreditDays(dto.getCreditDays() != null ? dto.getCreditDays() : 30);
        customer.setRemark(dto.getRemark());
        // 初始值
        customer.setLevel("C");
        customer.setScore(0);
        customer.setTotalAmount(BigDecimal.ZERO);
        customer.setOrderCount(0);
        customer.setOverdueCount(0);
        customerMapper.insert(customer);
        return customer;
    }

    /**
     * 重新计算并保存客户评分与等级，由订单完成 MQ 事件触发，或手动调用。
     */
    @Transactional
    public Customer recalculateScore(Long customerId) {
        Customer customer = getById(customerId);
        int newScore = scoringService.calculate(customer);
        String newLevel = scoringService.levelOf(newScore);

        String oldLevel = customer.getLevel();

        customerMapper.update(null,
            new LambdaUpdateWrapper<Customer>()
                .eq(Customer::getId, customerId)
                .set(Customer::getScore, newScore)
                .set(Customer::getLevel, newLevel)
        );

        if (!oldLevel.equals(newLevel)) {
            log.info("客户[{}] 等级变更: {} → {}", customer.getName(), oldLevel, newLevel);
        }
        customer.setScore(newScore);
        customer.setLevel(newLevel);
        return customer;
    }

    /**
     * 订单完成后更新客户冗余统计字段，再重新评分。
     */
    @Transactional
    public void onOrderCompleted(Long customerId, BigDecimal orderAmount) {
        Customer customer = getById(customerId);

        customerMapper.update(null,
            new LambdaUpdateWrapper<Customer>()
                .eq(Customer::getId, customerId)
                .setSql("total_amount = total_amount + " + orderAmount.toPlainString())
                .setSql("order_count = order_count + 1")
        );

        recalculateScore(customerId);
    }
}
