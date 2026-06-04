package net.suncaper.psychological.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.suncaper.psychological.entity.Counseling;
import net.suncaper.psychological.entity.vo.CounselorStatVO;
import net.suncaper.psychological.entity.vo.ProblemStatVO;
import net.suncaper.psychological.entity.vo.StudentStatVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 合并说明：
 * 1. 保留第二个版本的 @Mapper 注解（MyBatis 扫描需要）；
 * 2. 保留第一个版本的 countConflict 方法（业务校验用）；
 * 3. 保留第二个版本的所有统计方法（报表统计用）；
 * 4. 统一包导入、参数注解风格，消除重复定义。
 */
@Mapper
public interface CounselingMapper extends BaseMapper<Counseling> {

    // 【原第一个版本】查询同一咨询师、同一时间段是否已有占用（进行中）
    Integer countConflict(
            @Param("counselorId") Long counselorId,
            @Param("counselingTime") String counselingTime
    );

    /**
     * 【原第二个版本】按学生统计咨询次数。
     * 关键修复：使用 COALESCE(u.username, CAST(c.student_id AS CHAR)) 确保
     * 即使 counseling.student_id 在 user 表中无对应行（LEFT JOIN 失败），
     * studentNo 也不会为 NULL，前端不再显示 "—"。
     * SQL 别名与 {@link StudentStatVO} 字段名完全一致（均为驼峰），
     * MyBatis 无需额外 @Results 配置即可自动映射。
     */
    @Select("<script>" +
            "SELECT " +
            "  COALESCE(u.username, CAST(c.student_id AS CHAR)) AS studentNo, " +
            "  c.student_name  AS studentName, " +
            "  COUNT(cr.id)    AS totalTimes, " +
            "  MIN(c.start_date) AS firstDate " +
            "FROM counseling c " +
            "LEFT JOIN counseling_record cr ON cr.counseling_id = c.id " +
            "LEFT JOIN user u ON u.id = c.student_id " +
            "<where>" +
            "  <if test='startDate != null and startDate != \"\"'>" +
            "    AND c.start_date &gt;= #{startDate} " +
            "  </if>" +
            "  <if test='endDate != null and endDate != \"\"'>" +
            "    AND c.start_date &lt;= #{endDate} " +
            "  </if>" +
            "</where>" +
            "GROUP BY c.student_id, c.student_name, u.username" +
            "</script>")
    List<StudentStatVO> statByStudent(@Param("startDate") String startDate,
                                      @Param("endDate")   String endDate);

    /**
     * 【原第二个版本】按咨询师统计咨询学生人数和总次数。
     * SQL 别名与 {@link CounselorStatVO} 字段名一致。
     */
    @Select("<script>" +
            "SELECT " +
            "  c.counselor_name          AS counselorName, " +
            "  COUNT(DISTINCT c.student_id) AS studentCount, " +
            "  COUNT(cr.id)              AS totalTimes " +
            "FROM counseling c " +
            "LEFT JOIN counseling_record cr ON cr.counseling_id = c.id " +
            "<where>" +
            "  <if test='startDate != null and startDate != \"\"'>" +
            "    AND c.start_date &gt;= #{startDate} " +
            "  </if>" +
            "  <if test='endDate != null and endDate != \"\"'>" +
            "    AND c.start_date &lt;= #{endDate} " +
            "  </if>" +
            "</where>" +
            "GROUP BY c.counselor_id, c.counselor_name" +
            "</script>")
    List<CounselorStatVO> statByCounselor(@Param("startDate") String startDate,
                                          @Param("endDate")   String endDate);

    /**
     * 【原第二个版本】按问题类型统计人次。
     * SQL 别名与 {@link ProblemStatVO} 字段名一致。
     */
    @Select("<script>" +
            "SELECT " +
            "  fr.problem_type AS problemType, " +
            "  COUNT(fr.id)   AS count " +
            "FROM first_visit_result fr " +
            "LEFT JOIN first_visit fv ON fv.id = fr.first_visit_id " +
            "<where>" +
            "  <if test='startDate != null and startDate != \"\"'>" +
            "    AND fv.visit_date &gt;= #{startDate} " +
            "  </if>" +
            "  <if test='endDate != null and endDate != \"\"'>" +
            "    AND fv.visit_date &lt;= #{endDate} " +
            "  </if>" +
            "</where>" +
            "GROUP BY fr.problem_type" +
            "</script>")
    List<ProblemStatVO> statByProblemType(@Param("startDate") String startDate,
                                          @Param("endDate")   String endDate);
}