package com.crm.notify.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_alert")
public class Alert {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private String alertType;
    private String message;
    private String status;
    private String handler;
    private LocalDateTime handleTime;
    private LocalDateTime notifiedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
