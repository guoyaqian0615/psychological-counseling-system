package net.suncaper.psychological.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("first_visit_result")
public class FirstVisitResult {
    @TableId(type = com.baomidou.mybatisplus.annotation.IdType.AUTO)
    private Long id;
    private Long firstVisitId;
    private String crisisLevel;
    private String problemType;
    private String conclusion;
}