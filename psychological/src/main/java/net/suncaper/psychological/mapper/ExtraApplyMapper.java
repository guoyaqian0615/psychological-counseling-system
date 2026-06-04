package net.suncaper.psychological.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.suncaper.psychological.entity.ExtraApply;
import net.suncaper.psychological.entity.vo.ExtraApplyVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExtraApplyMapper extends BaseMapper<ExtraApply> {

    /**
     * 联表查询追加咨询申请（含学生姓名、咨询师姓名）
     */
    @Select("SELECT ea.*, " +
            "us.name AS studentName, " +
            "uc.name AS counselorName " +
            "FROM extra_apply ea " +
            "LEFT JOIN user us ON us.id = ea.student_id " +
            "LEFT JOIN user uc ON uc.id = ea.counselor_id " +
            "ORDER BY ea.apply_time DESC")
    List<ExtraApplyVO> selectExtraApplyWithNames();
}
