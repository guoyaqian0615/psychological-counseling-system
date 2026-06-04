package net.suncaper.psychological.entity.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.suncaper.psychological.entity.ExtraApply;

/**
 * 追加咨询申请 VO（带学生姓名、咨询师姓名）
 * 不改数据库、不改 ExtraApply 实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExtraApplyVO extends ExtraApply {

    /** 学生姓名（来自 user 表 name 字段） */
    private String studentName;

    /** 咨询师姓名（来自 user 表 name 字段） */
    private String counselorName;
}
