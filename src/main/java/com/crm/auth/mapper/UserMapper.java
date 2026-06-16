package com.crm.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM t_user WHERE username = #{username} AND enabled = 1")
    User selectByUsername(String username);
}
