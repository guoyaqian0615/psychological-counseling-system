package net.suncaper.psychological.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.suncaper.psychological.entity.FirstVisitResult;
import net.suncaper.psychological.entity.vo.FirstVisitResultVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FirstVisitResultMapper extends BaseMapper<FirstVisitResult> {

    /**
     * 初访员历史记录（联表查学生信息）
     *
     * @param visitorId   初访员ID
     * @param studentName 学生姓名（模糊，可为 null）
     */
    List<FirstVisitResultVO> selectVisitorHistory(@Param("visitorId") Long visitorId,
                                                  @Param("studentName") String studentName);

    /**
     * 根据 first_visit_result.id 查详情（含学生信息）
     *
     * @param id first_visit_result 主键
     */
    FirstVisitResultVO getDetailById(@Param("id") Long id);

    /**
     * 助理待安排列表：
     * 查询 conclusion = '安排咨询' 的所有初访结果，联表拿学生信息。
     * （原方法名 selectWaitArrangeVisit 返回 FirstVisit，已替换为此方法）
     *
     * @return 待安排的 FirstVisitResultVO 列表
     */
    List<FirstVisitResultVO> selectWaitArrangeVO();
}
