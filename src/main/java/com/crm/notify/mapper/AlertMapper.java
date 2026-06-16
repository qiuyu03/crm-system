package com.crm.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.notify.entity.Alert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AlertMapper extends BaseMapper<Alert> {

    /**
     * 按订单ID列表查最近20条预警（IN 查询 + LIMIT，XML 实现）
     */
    List<Alert> findTop20ByOrderIdIn(@Param("orderIds") List<Long> orderIds);
}
