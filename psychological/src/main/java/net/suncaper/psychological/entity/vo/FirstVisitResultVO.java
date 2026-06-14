package net.suncaper.psychological.entity.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FirstVisitResultVO {
    private Long id;
    private Long firstVisitId;
    private String crisisLevel;    //危机等级
    private String problemType;    //问题类型
    private String conclusion;     //结论
    private String location;

    private String studentName;    //学生姓名
    private Long studentId;
}