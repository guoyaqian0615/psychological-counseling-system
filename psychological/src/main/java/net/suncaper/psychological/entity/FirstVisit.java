package net.suncaper.psychological.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("first_visit")
public class FirstVisit {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String studentName;
    private LocalDateTime applyTime;
    private Integer questionnaireScore;
    private String status;
    private Long visitorId;
    private LocalDate visitDate;
    private String visitTime;
    private String location;
    private Boolean isEmergency;
    private Boolean isAlert;

    private LocalDateTime createTime;
    private LocalDateTime auditTime;
    private LocalDateTime rescheduleTime;

    // ★ 不入库的临时字段，仅用于在业务层传递通知内容
    /** 拒绝原因（仅在审核拒绝时由前端传入，写入通知后丢弃） */
    @TableField(exist = false)
    private String rejectReason;

    /** 管理员是否修改了学生原始预约信息（true = 安排有调整，通知标题有所区别） */
    @TableField(exist = false)
    private Boolean isModified;
}
