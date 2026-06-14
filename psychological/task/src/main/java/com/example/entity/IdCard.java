package com.example.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("id_card")
public class IdCard {

    @TableId
    private Long id;

    private String cardNo;

    private Long userId;
}