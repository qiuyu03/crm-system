package com.crm.order.controller;

import com.crm.common.result.R;
import com.crm.order.dto.OrderCreateDTO;
import com.crm.order.dto.OrderDetailDTO;
import com.crm.order.entity.Order;
import com.crm.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public R<Page<Order>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return R.ok(orderService.list(status, pageable));
    }

    @GetMapping("/{id}")
    public R<OrderDetailDTO> detail(@PathVariable Long id) {
        return R.ok(orderService.getDetail(id));
    }

    @PostMapping
    public R<Order> create(@Valid @RequestBody OrderCreateDTO dto) {
        return R.ok(orderService.create(dto));
    }

    @PutMapping("/{id}/status")
    public R<Order> changeStatus(@PathVariable Long id,
                                  @RequestParam String status) {
        return R.ok(orderService.changeStatus(id, status));
    }

    // 触发预警（开发测试用）
    @PostMapping("/{id}/alert")
    public R<Void> triggerAlert(@PathVariable Long id,
                                @RequestParam String alertType,
                                @RequestParam String message) {
        orderService.triggerAlert(id, alertType, message);
        return R.ok();
    }
}
