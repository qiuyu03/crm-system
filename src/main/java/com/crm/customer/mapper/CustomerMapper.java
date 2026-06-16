package com.crm.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.customer.entity.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    /**
     * 动态条件分页查询（XML 实现，支持按等级过滤 + 关键字模糊搜索）
     */
    Page<Customer> search(Page<Customer> page,
                          @Param("level") String level,
                          @Param("keyword") String keyword);
}
