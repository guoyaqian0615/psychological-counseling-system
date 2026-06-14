package net.suncaper.psychological.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@TableName("duty")
public class Duty {
    @TableId(type = com.baomidou.mybatisplus.annotation.IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate dutyDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
}