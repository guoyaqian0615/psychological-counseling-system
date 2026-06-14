package net.suncaper.psychological.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("closing_report")
public class ClosingReport {
    @TableId(type = com.baomidou.mybatisplus.annotation.IdType.AUTO)
    private Long id;
    private Long counselingId;
    private Long studentId;
    private String studentName;
    private String gender;
    private String department;
    private String phone;
    private String problemType;
    private Integer totalTimes;
    private Long counselorId;
    private String counselorName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String conclusion;
    private LocalDateTime createTime;
}