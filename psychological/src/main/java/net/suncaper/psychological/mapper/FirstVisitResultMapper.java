package net.suncaper.psychological.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.suncaper.psychological.entity.FirstVisitResult;
import net.suncaper.psychological.entity.vo.FirstVisitResultVO;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface FirstVisitResultMapper extends BaseMapper<FirstVisitResult> {

    // 查询初访历史（带学生姓名）
    List<FirstVisitResultVO> selectVisitorHistory(
            @Param("visitorId") Long visitorId,
            @Param("studentName") String studentName
    );
    FirstVisitResultVO getDetailById(@Param("id") Long id);
}