package net.suncaper.psychological.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("time_config")
public class TimeConfig {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer singleDuration;   // 单次时长
    private Integer intervalMinute;    // 间隔

    private Integer dailyStartHour;   // 开始小时
    private Integer dailyEndHour;     // 结束小时
}