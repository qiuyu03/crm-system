package com.crm.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerCreateDTO {

    @NotBlank(message = "客户姓名不能为空")
    private String name;

    private String company;
    private String contact;
    private String email;
    private Integer creditDays = 30;
    private String remark;
}
