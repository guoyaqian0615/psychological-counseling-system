package net.suncaper.psychological.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import net.suncaper.psychological.entity.FirstVisit;
import net.suncaper.psychological.mapper.FirstVisitMapper;
import net.suncaper.psychological.service.FirstVisitService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FirstVisitServiceImpl implements FirstVisitService {

    private final FirstVisitMapper firstVisitMapper;

    public FirstVisitServiceImpl(FirstVisitMapper firstVisitMapper) {
        this.firstVisitMapper = firstVisitMapper;
    }

    @Override
    public boolean submit(FirstVisit firstVisit) {
        // 防重：同一学生 + 日期 + 时间 + 老师 不能重复
        LambdaQueryWrapper<FirstVisit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FirstVisit::getStudentId, firstVisit.getStudentId())
                .eq(FirstVisit::getVisitDate, firstVisit.getVisitDate())
                .eq(FirstVisit::getVisitTime, firstVisit.getVisitTime())
                .eq(FirstVisit::getTeacher, firstVisit.getTeacher());
        Long count = firstVisitMapper.selectCount(wrapper);
        if (count > 0) {
            return false; // 重复
        }
        firstVisit.setStatus("待审核");
        firstVisitMapper.insert(firstVisit);
        return true;
    }

    @Override
    public List<FirstVisit> myVisits(Long studentId) {
        return firstVisitMapper.selectList(
                new LambdaQueryWrapper<FirstVisit>()
                        .eq(FirstVisit::getStudentId, studentId)
        );
    }

    @Override
    public void cancel(Long id) {
        firstVisitMapper.update(
                null,
                new LambdaUpdateWrapper<FirstVisit>()
                        .eq(FirstVisit::getId, id)
                        .set(FirstVisit::getStatus, "已撤销")
        );
    }

    @Override
    public FirstVisit getById(Long id) {
        return firstVisitMapper.selectById(id);
    }
}