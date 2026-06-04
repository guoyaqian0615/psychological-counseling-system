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

//    /**
//     * 联表分页查询初访记录（含初访员姓名/电话）
//     *
//     * @param page        MyBatis-Plus 分页对象
//     * @param status      状态过滤（可为空）
//     * @param studentName 学生姓名模糊搜索（可为空）
//     * @return 分页结果
//     */
////    IPage<FirstVisitVO> selectVisitWithVisitor(
////            Page<FirstVisitVO> page,
////            @Param("status") String status,
////            @Param("studentName") String studentName
////
////    );

        // ====================== 1. 助理查询：已通过预约（你给的方法） ======================
        @Select("SELECT f.id,f.student_id studentId,f.student_name studentName,f.status,fr.problem_type problemType " +
                "FROM first_visit f LEFT JOIN first_visit_result fr ON f.id=fr.first_visit_id " +
                "WHERE f.status='已通过'")
        List<FirstVisitResultVO> selectWaitArrangeVO();

        // ====================== 2. 分页查询：初访记录+初访员信息 ======================
        IPage<FirstVisitVO> selectVisitWithVisitor(
                Page<FirstVisitVO> page,
                @Param("status") String status,
                @Param("studentName") String studentName
        );

}
