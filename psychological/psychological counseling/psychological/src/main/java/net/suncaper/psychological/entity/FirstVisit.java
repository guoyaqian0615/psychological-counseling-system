package net.suncaper.psychological.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("first_visit")
public class FirstVisit {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String teacher;
    private String visitDate;
    private String visitTime;
    private Integer questionnaireScore;
    private String questionnaireContent;
    private String questionnaireAnswer;
    private String status;
}