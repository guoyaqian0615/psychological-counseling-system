package net.suncaper.psychological.entity.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class DutyVO {
    private Long id;
    private Long userId;
    private LocalDate dutyDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private Integer maxPerson;
    private String remark;

    // 前端显示：姓名 + 角色
    private String userName;

}