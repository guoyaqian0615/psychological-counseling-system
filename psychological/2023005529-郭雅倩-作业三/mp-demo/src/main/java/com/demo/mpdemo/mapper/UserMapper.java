package com.demo.mpdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.mpdemo.entity.User;
import org.apache.ibatis.annotations.Param;
import java.util.Map;

public interface UserMapper extends BaseMapper<User> {

    // 自定义根据ID删除
    int deleteUserById(@Param("id") Long id);

    // 自定义插入
    int insertUser(Map<String, Object> map);

    // 自定义分页 + 动态SQL
    Page<User> selectUserPage(Page<User> page, @Param("name") String name, @Param("age") Integer age);

    // 一对一：用户+身份证
    User selectUserWithCard(Long id);

    // 一对多：用户+订单
    User selectUserWithOrders(Long id);
}