package net.suncaper.psychological.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.suncaper.psychological.entity.Duty;
import net.suncaper.psychological.entity.vo.DutyVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DutyMapper extends BaseMapper<Duty> {
    @Select("SELECT d.*, u.name as userName FROM duty d LEFT JOIN user u ON d.user_id = u.id ${ew.customSqlSegment}")
    IPage<DutyVO> selectDutyVOPage(Page<DutyVO> page, @Param("ew") QueryWrapper<Duty> wrapper);

    //【新增：全查排班带姓名，给StudentService调用】
    @Select("SELECT d.*, u.name as userName FROM duty d LEFT JOIN user u ON d.user_id = u.id")
    List<DutyVO> getAllDutyWithUserName();
    List<DutyVO> getVisitorDutyOnly();
}