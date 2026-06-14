package net.suncaper.psychological.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("extra_apply")
public class ExtraApply {
    @TableId(type = com.baomidou.mybatisplus.annotation.IdType.AUTO)
    private Long id;
    private Long counselingId;
    private Long studentId;
    private Long counselorId;
    private LocalDateTime applyTime;
    private Integer extraWeeks;
    private String reason;
    private String status;
    private String adminRemark;
    private LocalDateTime handleTime;
}