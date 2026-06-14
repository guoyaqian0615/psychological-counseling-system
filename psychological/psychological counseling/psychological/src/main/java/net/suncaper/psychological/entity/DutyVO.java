package net.suncaper.psychological.entity;

import lombok.Data;

@Data
public class DutyVO extends Duty {
    // 继承Duty的所有字段，额外增加老师姓名
    private String visitorName;
}