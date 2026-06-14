package net.suncaper.psychological.entity.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 按学生维度的统计 VO。
 *
 * 字段名与前端 prop（驼峰）保持一致，Spring Jackson 序列化后直接可用，
 * 不再依赖 toCamelCaseList() 运行时转换。
 */
@Data
public class StudentStatVO {

    /**
     * 学号（来自 user.username）。
     * 若 counseling.student_id 在 user 表中无对应记录，
     * SQL 使用 COALESCE 降级为 student_id 的字符串形式，确保前端不显示 "—"。
     */
    private String studentNo;

    /** 学生姓名（来自 counseling.student_name）*/
    private String studentName;

    /** 累计咨询次数（counseling_record 行数）*/
    private Integer totalTimes;

    /** 首次咨询日期（counseling.start_date 最小值）*/
    private LocalDate firstDate;
}
