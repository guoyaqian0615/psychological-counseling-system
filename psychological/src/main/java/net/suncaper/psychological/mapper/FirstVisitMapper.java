package net.suncaper.psychological.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.suncaper.psychological.entity.FirstVisit;
import net.suncaper.psychological.entity.vo.FirstVisitVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.suncaper.psychological.entity.FirstVisit;
import net.suncaper.psychological.entity.vo.FirstVisitVO;
import net.suncaper.psychological.entity.vo.FirstVisitResultVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FirstVisitMapper extends BaseMapper<FirstVisit> {


        // ====================== 1. 助理查询：已通过预约======================
        @Select("SELECT f.id,f.student_id studentId,f.student_name studentName," +
                "fr.problem_type problemType,fr.crisis_level crisisLevel " +
                "FROM first_visit f LEFT JOIN first_visit_result fr ON f.id=fr.first_visit_id " +
                "WHERE f.status='已通过'")
        List<FirstVisitResultVO> selectWaitArrangeVO();


        @Select("SELECT f.*, v.name visitorName, v.phone visitorPhone " +
                "FROM first_visit f " +
                "LEFT JOIN visitor v ON f.visitor_id = v.id " +
                "WHERE 1=1 " +
                "AND (${status} IS NULL OR f.status = #{status}) " +
                "AND (${studentName} IS NULL OR f.student_name LIKE CONCAT('%', #{studentName}, '%'))")
        IPage<FirstVisitVO> selectVisitWithVisitor(
                Page<FirstVisitVO> page,
                @Param("status") String status,
                @Param("studentName") String studentName
        );
                //!新增【学生当前预约VO查询】!
        List<FirstVisitVO> getCurrentByStudentId(@Param("studentId") Long studentId);
        //!新增【学生全部历史预约VO查询】!
        List<FirstVisitVO> getHistoryByStudentId(@Param("studentId") Long studentId);


}
