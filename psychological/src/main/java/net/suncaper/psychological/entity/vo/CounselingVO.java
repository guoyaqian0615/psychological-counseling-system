package net.suncaper.psychological.entity.vo;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CounselingVO {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long counselorId;
    private String counselorName;
    private Integer totalWeeks;
    private LocalDate startDate;
    private String counselingTime;
    private String location;
    private String status;
}