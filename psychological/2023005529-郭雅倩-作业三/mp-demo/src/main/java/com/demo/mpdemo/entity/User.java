package com.demo.mpdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.util.List;

@Data
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer age;
    private String email;

    // 👇 加上这行！表示数据库不存在此字段
    @TableField(exist = false)
    private IdCard idCard;

    // 👇 加上这行！表示数据库不存在此字段
    @TableField(exist = false)
    private List<Order> orderList;
}