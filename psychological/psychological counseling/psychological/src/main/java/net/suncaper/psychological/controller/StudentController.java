package net.suncaper.psychological.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import net.suncaper.psychological.common.Result;
import net.suncaper.psychological.entity.FirstVisit;
import net.suncaper.psychological.entity.User;
import net.suncaper.psychological.mapper.FirstVisitMapper;
import net.suncaper.psychological.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FirstVisitMapper firstVisitMapper;

    // ===================== 登录 =====================
    @PostMapping("/login")
    public Result<User> login(@RequestBody User user) {
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

    // ===================== 初访预约 =====================
    @PostMapping("/firstVisit/submit")
    public Result<Void> submitFirstVisit(@RequestBody FirstVisit firstVisit) {
        firstVisitMapper.insert(firstVisit);
        return Result.success();
    }

    // ===================== 我的预约记录 =====================
    @GetMapping("/myFirstVisits")
    public Result<List<FirstVisit>> myFirstVisits(@RequestParam Long studentId) {
        LambdaQueryWrapper<FirstVisit> w = new LambdaQueryWrapper<>();
        w.eq(FirstVisit::getStudentId, studentId);
        return Result.success(firstVisitMapper.selectList(w));
    }

    // ===================== 撤销预约 =====================
    @PostMapping("/cancelVisit")
    public Result<Void> cancelVisit(@RequestParam Long id) {
        FirstVisit fv = new FirstVisit();
        fv.setId(id);
        fv.setStatus("已撤销");
        firstVisitMapper.updateById(fv);
        return Result.success();
    }
}