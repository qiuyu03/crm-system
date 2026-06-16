package com.crm.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.result.R;
import com.crm.order.dto.OrderCreateDTO;
import com.crm.order.dto.OrderDetailDTO;
import com.crm.order.entity.Order;
import com.crm.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public R<IPage<Order>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return R.ok(orderService.list(status, new Page<>(page, size)));
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

    @PostMapping("/{id}/alert")
    public R<Void> triggerAlert(@PathVariable Long id,
                                @RequestParam String alertType,
                                @RequestParam String message) {
        orderService.triggerAlert(id, alertType, message);
        return R.ok();
    }
}
