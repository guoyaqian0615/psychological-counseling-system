package net.suncaper.psychological.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.suncaper.psychological.entity.FirstVisit;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 初访记录 VO（带初访员基础信息）
 * 不改数据库、不改 FirstVisit 实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FirstVisitVO extends FirstVisit {

    /** 初访员姓名（来自 user 表 name 字段） */
    private String visitorName;

    /** 初访员电话（来自 user 表 phone 字段） */
    private String visitorPhone;

    // 新增：学生姓名、初访员姓名
    private String studentName;
    // 生成get/set（IDEA右键→Generate→Getter and Setter）
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyTime;
}
