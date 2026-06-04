package net.suncaper.psychological.entity.vo;

import lombok.Data;

/**
 * 按问题类型维度的统计 VO。
 */
@Data
public class ProblemStatVO {

    /** 问题类型（来自 first_visit_result.problem_type）*/
    private String problemType;

    /** 该类型人次 */
    private Integer count;
}
