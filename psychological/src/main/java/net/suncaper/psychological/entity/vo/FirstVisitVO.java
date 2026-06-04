package net.suncaper.psychological.entity.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.suncaper.psychological.entity.FirstVisit;

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
}
