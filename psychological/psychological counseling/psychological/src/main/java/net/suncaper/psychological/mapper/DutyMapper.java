package net.suncaper.psychological.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.suncaper.psychological.entity.Duty;
import net.suncaper.psychological.entity.DutyVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DutyMapper extends BaseMapper<Duty> {

    /**
     * 查询指定日期可用的初访员值班记录（含老师姓名）
     * 给学生端展示可预约的初访老师和时间段
     * @param date 要查询的值班日期
     * @return 值班记录列表，包含老师姓名、时间等信息
     */
    @Select("SELECT d.*, u.name AS visitorName FROM duty d " +
            "LEFT JOIN user u ON d.user_id = u.id " +
            "WHERE d.duty_date = #{date} AND d.status = 1 AND u.role = 'visitor'")
    List<DutyVO> selectVisitorDutyByDate(@Param("date") LocalDate date);
}