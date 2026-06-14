package net.suncaper.psychological.entity.vo;

import lombok.Data;

/**
 * 按咨询师维度的统计 VO。
 */
@Data
public class CounselorStatVO {

    /** 咨询师姓名 */
    private String counselorName;

    /** 累计咨询学生人数（counseling.student_id 去重计数）*/
    private Integer studentCount;

    /** 累计咨询次数（counseling_record 行数）*/
    private Integer totalTimes;
}
