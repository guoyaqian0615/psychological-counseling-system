package net.suncaper.psychological.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("counseling")
public class Counseling {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String studentName;
    private Long counselorId;
    private String counselorName;
    private Integer totalWeeks;
    private LocalDate startDate;
    private String counselingTime;
    private String location;
    private String status;

    /**
     * 不入库的临时字段：安排咨询时由前端传入，用于精准回写 first_visit.status。
     * 若前端未传，Service 层会通过 studentId 兜底查找最近一条"已完成"记录来更新。
     */
    @TableField(exist = false)
    private Long firstVisitId;

    /**
     * 不入库：显示用。
     * 由 Service 层通过 studentId 关联 user.username 填充，
     * 用于前端展示真实学号（如 2026008），而非 user 表主键（如 9）。
     */
    @TableField(exist = false)
    private String studentNo;

    /**
     * 不入库：显示用。
     * 由 Service 层查 counseling_record 表计数后填充，
     * 用于前端咨询进度条显示（如 7/8）。
     */
    @TableField(exist = false)
    private Integer sessionCount;
}
