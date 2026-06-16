package com.crm.customer.service;

import com.crm.common.exception.BusinessException;
import com.crm.customer.dto.CustomerCreateDTO;
import com.crm.customer.dto.CustomerProfileDTO;
import com.crm.customer.entity.Customer;
import com.crm.customer.repository.CustomerRepository;
import com.crm.notify.entity.Alert;
import com.crm.notify.repository.AlertRepository;
import com.crm.order.entity.Order;
import com.crm.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final AlertRepository alertRepository;
    private final CustomerScoringService scoringService;

    public Page<Customer> list(String level, String keyword, Pageable pageable) {
        return customerRepository.search(level, keyword, pageable);
    }

    public Customer getById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "客户不存在"));
    }

    /**
     * 客户全景视图：一次性聚合客户基本信息、最近订单、预警历史。
     *
     * 避免 N+1：先批量查订单，再用 orderIds IN 查预警，不做循环查询。
     */
    public CustomerProfileDTO getProfile(Long customerId) {
        Customer customer = getById(customerId);
        CustomerProfileDTO profile = CustomerProfileDTO.from(customer);

        // 最近10笔订单（走联合索引 idx_customer_create）
        List<Order> orders = orderRepository.findTop10ByCustomerIdOrderByCreateTimeDesc(customerId);
        profile.setRecentOrders(orders.stream().map(o -> {
            CustomerProfileDTO.OrderSummary s = new CustomerProfileDTO.OrderSummary();
            s.setOrderId(o.getId());
            s.setOrderNo(o.getOrderNo());
            s.setStatus(o.getStatus());
            s.setTotalAmount(o.getTotalAmount());
            s.setCreateTime(o.getCreateTime());
            return s;
        }).collect(Collectors.toList()));

        // 用 IN 查询获取预警历史，避免循环查库
        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        if (!orderIds.isEmpty()) {
            List<Alert> alerts = alertRepository.findTop20ByOrderIdInOrderByCreateTimeDesc(orderIds);
            profile.setRecentAlerts(alerts.stream().map(a -> {
                CustomerProfileDTO.AlertSummary s = new CustomerProfileDTO.AlertSummary();
                s.setAlertId(a.getId());
                s.setAlertType(a.getAlertType());
                s.setMessage(a.getMessage());
                s.setStatus(a.getStatus());
                s.setCreateTime(a.getCreateTime());
                return s;
            }).collect(Collectors.toList()));
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
        return customerRepository.save(customer);
    }

    /**
     * 重新计算并保存客户评分与等级。
     * 由订单完成事件触发（MQ消费者调用），或手动触发。
     */
    @Transactional
    public Customer recalculateScore(Long customerId) {
        Customer customer = getById(customerId);
        int newScore = scoringService.calculate(customer);
        String newLevel = scoringService.levelOf(newScore);

        String oldLevel = customer.getLevel();
        customer.setScore(newScore);
        customer.setLevel(newLevel);
        Customer saved = customerRepository.save(customer);

        if (!oldLevel.equals(newLevel)) {
            log.info("客户[{}] 等级变更: {} → {}", customer.getName(), oldLevel, newLevel);
        }
        return saved;
    }

    /**
     * 订单完成后同步更新客户冗余统计字段，再重新评分。
     */
    @Transactional
    public void onOrderCompleted(Long customerId, java.math.BigDecimal orderAmount) {
        Customer customer = getById(customerId);
        customer.setTotalAmount(customer.getTotalAmount().add(orderAmount));
        customer.setOrderCount(customer.getOrderCount() + 1);
        customerRepository.save(customer);
        recalculateScore(customerId);
    }
}
