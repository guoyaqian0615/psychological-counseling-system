package com.example.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;

@Data
@TableName("user")
public class User {

    @TableId
    private Long id;

    private String name;

    // 一对一：一个用户对应一张身份证
    private IdCard idCard;

    // 一对多：一个用户对应多个订单
    private List<Order> orders;
}
