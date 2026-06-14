package net.suncaper.psychological.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@TableName("duty")
public class Duty {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate dutyDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private Integer maxPerson;
    private String remark;

    @TableField(exist = false)
    private String userName;

    /** 该时段已预约（状态为"已通过"）的人数，由 Service 层查询后填充 */
    @TableField(exist = false)
    private Integer bookedCount;
}
