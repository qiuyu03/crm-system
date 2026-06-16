package com.crm.customer.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.result.R;
import com.crm.customer.dto.CustomerCreateDTO;
import com.crm.customer.dto.CustomerProfileDTO;
import com.crm.customer.entity.Customer;
import com.crm.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public R<IPage<Customer>> list(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return R.ok(customerService.list(level, keyword, new Page<>(page, size)));
    }

    @GetMapping("/{id}")
    public R<Customer> get(@PathVariable Long id) {
        return R.ok(customerService.getById(id));
    }

    @GetMapping("/{id}/profile")
    public R<CustomerProfileDTO> profile(@PathVariable Long id) {
        return R.ok(customerService.getProfile(id));
    }

    @PostMapping
    public R<Customer> create(@Valid @RequestBody CustomerCreateDTO dto) {
        return R.ok(customerService.create(dto));
    }

    @PutMapping("/{id}/rescore")
    public R<Customer> rescore(@PathVariable Long id) {
        return R.ok(customerService.recalculateScore(id));
    }
}
