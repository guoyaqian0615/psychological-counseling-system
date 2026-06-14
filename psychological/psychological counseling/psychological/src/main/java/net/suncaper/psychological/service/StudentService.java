package net.suncaper.psychological.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import net.suncaper.psychological.entity.DutyVO;
import net.suncaper.psychological.entity.FirstVisit;
import net.suncaper.psychological.entity.User;
import net.suncaper.psychological.mapper.DutyMapper;
import net.suncaper.psychological.mapper.FirstVisitMapper;
import net.suncaper.psychological.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class StudentService {

    private final UserMapper userMapper;
    private final FirstVisitMapper firstVisitMapper;
    private final DutyMapper dutyMapper;

    public StudentService(UserMapper userMapper,
                          FirstVisitMapper firstVisitMapper,
                          DutyMapper dutyMapper) {
        this.userMapper = userMapper;
        this.firstVisitMapper = firstVisitMapper;
        this.dutyMapper = dutyMapper;
    }

    // 1. 学生登录
    public User login(String username, String password) {
        User student = userMapper.selectStudentByUsername(username);
        if (student == null) {
            throw new RuntimeException("账号不存在");
        }
        if (!student.getPassword().equals(password)) {
            throw new RuntimeException("密码错误");
        }
        return student;
    }

    // 2. 提交初访预约（防重 + 状态统一）
    public boolean submitFirstVisit(FirstVisit firstVisit) {
        LambdaQueryWrapper<FirstVisit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FirstVisit::getStudentId, firstVisit.getStudentId())
                .eq(FirstVisit::getVisitDate, firstVisit.getVisitDate())
                .eq(FirstVisit::getVisitTime, firstVisit.getVisitTime())
                .eq(FirstVisit::getTeacher, firstVisit.getTeacher());
        Long count = firstVisitMapper.selectCount(wrapper);
        if (count > 0) {
            return false;
        }
        firstVisit.setStatus("待审核");
        return firstVisitMapper.insert(firstVisit) > 0;
    }

    // 3. 查询学生的所有预约记录
    public List<FirstVisit> getStudentFirstVisit(Long studentId) {
        return firstVisitMapper.selectByStudentId(studentId);
    }

    // 4. 撤销预约（关键：String → LocalDate）
    public boolean cancelFirstVisit(Long visitId, Long studentId) {
        FirstVisit visit = firstVisitMapper.selectById(visitId);
        if (visit == null || !visit.getStudentId().equals(studentId)) {
            throw new RuntimeException("无权限撤销该预约");
        }

        if ("待审核".equals(visit.getStatus())) {
            visit.setStatus("已撤销");
            return firstVisitMapper.updateById(visit) > 0;
        } else if ("已通过".equals(visit.getStatus())) {
            // 核心修复：字符串转LocalDate（yyyy-MM-dd）
            LocalDate visitDate = LocalDate.parse(
                    visit.getVisitDate(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd")
            );
            LocalDate now = LocalDate.now();
            if (ChronoUnit.DAYS.between(now, visitDate) >= 1) {
                visit.setStatus("已撤销");
                return firstVisitMapper.updateById(visit) > 0;
            } else {
                throw new RuntimeException("已通过的预约需提前1天撤销");
            }
        } else {
            throw new RuntimeException("仅可撤销待审核或已通过的预约");
        }
    }

    // 5. 查询指定日期的初访员值班表
    // 5. 查询指定日期的初访员值班表
    public List<DutyVO> getVisitorDuty(LocalDate date) {   // ✅ 改为 DutyVO
        return dutyMapper.selectVisitorDutyByDate(date);
    }
}