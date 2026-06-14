package net.suncaper.psychological.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.suncaper.psychological.entity.FirstVisit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface FirstVisitMapper extends BaseMapper<FirstVisit> {

    /**
     * 查询指定学生的所有初访预约记录，按申请时间倒序排列
     * @param studentId 学生ID
     * @return 该学生的所有预约记录列表
     */
    @Select("SELECT * FROM first_visit WHERE student_id = #{studentId} ORDER BY apply_time DESC")
    List<FirstVisit> selectByStudentId(@Param("studentId") Long studentId);
}