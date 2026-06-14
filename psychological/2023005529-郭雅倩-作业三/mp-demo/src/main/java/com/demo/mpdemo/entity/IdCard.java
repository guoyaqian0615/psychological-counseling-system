package com.demo.mpdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class IdCard {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String cardNo;
    private Long userId;
}