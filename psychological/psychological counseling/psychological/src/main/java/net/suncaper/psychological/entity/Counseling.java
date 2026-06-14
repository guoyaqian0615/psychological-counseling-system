package net.suncaper.psychological.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("counseling")
public class Counseling {
    @TableId(type = com.baomidou.mybatisplus.annotation.IdType.AUTO)
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
}