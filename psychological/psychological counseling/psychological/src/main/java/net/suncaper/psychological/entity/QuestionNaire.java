package net.suncaper.psychological.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("questionnaire")
public class QuestionNaire {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String studentName;
    private Integer totalScore;
    private String analysis;
    private String answers;
    private String content;
    private String level;
    private LocalDateTime createTime;
}