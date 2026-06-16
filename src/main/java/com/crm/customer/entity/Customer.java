package com.crm.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_customer")
public class Customer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String company;
    private String contact;
    private String email;
    private String level;
    private Integer score;
    private BigDecimal totalAmount;
    private Integer orderCount;
    private Integer creditDays;
    private Integer overdueCount;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
