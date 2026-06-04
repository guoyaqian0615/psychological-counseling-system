package net.suncaper.psychological.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import net.suncaper.psychological.common.Result;
import net.suncaper.psychological.entity.FirstVisit;
import net.suncaper.psychological.entity.User;
import net.suncaper.psychological.mapper.FirstVisitMapper;
import net.suncaper.psychological.mapper.UserMapper;
import net.suncaper.psychological.service.StudentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentServiceImpl implements StudentService {

    private final UserMapper userMapper;
    private final FirstVisitMapper firstVisitMapper;

    public StudentServiceImpl(UserMapper userMapper,
                              FirstVisitMapper firstVisitMapper) {
        this.userMapper = userMapper;
        this.firstVisitMapper = firstVisitMapper;
    }

    @Override
    public Result<User> login(User user) {
        if (!user.getUsername().matches("^[0-9]+$")) {
            return Result.error("学号必须是纯数字");
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername())
                .eq(User::getPassword, user.getPassword())
                .eq(User::getRole, "student");
        User loginUser = userMapper.selectOne(wrapper);
        return loginUser != null ? Result.success(loginUser) : Result.error("账号或密码错误");
    }

    @Override
    public Result<Void> submitFirstVisit(FirstVisit firstVisit) {
        firstVisitMapper.insert(firstVisit);
        return Result.success();
    }

    @Override
    public Result<List<FirstVisit>> myFirstVisits(Long studentId) {
        LambdaQueryWrapper<FirstVisit> w = new LambdaQueryWrapper<>();
        w.eq(FirstVisit::getStudentId, studentId);
        return Result.success(firstVisitMapper.selectList(w));
    }

    @Override
    public Result<Void> cancelVisit(Long id) {
        FirstVisit fv = new FirstVisit();
        fv.setId(id);
        fv.setStatus("已撤销");
        firstVisitMapper.updateById(fv);
        return Result.success();
    }
}