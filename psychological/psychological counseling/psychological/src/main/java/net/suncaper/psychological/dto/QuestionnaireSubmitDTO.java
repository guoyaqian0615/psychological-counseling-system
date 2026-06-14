package net.suncaper.psychological.dto;

import lombok.Data;
import java.util.List;

@Data
public class QuestionnaireSubmitDTO {
    // 改成 Long 类型，和实体类匹配
    private Long studentId;
    private String studentName;
    private List<Integer> scores;
    private String content;
}