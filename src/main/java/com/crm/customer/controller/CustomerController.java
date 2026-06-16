package com.crm.customer.controller;

import com.crm.common.result.R;
import com.crm.customer.dto.CustomerCreateDTO;
import com.crm.customer.dto.CustomerProfileDTO;
import com.crm.customer.entity.Customer;
import com.crm.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public R<Page<Customer>> list(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "score"));
        return R.ok(customerService.list(level, keyword, pageable));
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

    // 手动触发重新评分（测试用）
    @PutMapping("/{id}/rescore")
    public R<Customer> rescore(@PathVariable Long id) {
        return R.ok(customerService.recalculateScore(id));
    }
}
