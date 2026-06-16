package com.crm.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class OrderCreateDTO {

    @NotNull(message = "客户ID不能为空")
    private Long customerId;

    @NotBlank(message = "创建人不能为空")
    private String createdBy;

    private LocalDate deliveryDate;
    private String currency = "CNY";
    private String remark;

    @Valid
    @NotNull(message = "订单明细不能为空")
    private List<ItemDTO> items;

    @Data
    public static class ItemDTO {
        @NotBlank(message = "产品名称不能为空")
        private String productName;
        private String productCode;
        @NotNull private Integer qty;
        @NotNull private BigDecimal unitPrice;
        private String unit;
        private String remark;
    }
}
