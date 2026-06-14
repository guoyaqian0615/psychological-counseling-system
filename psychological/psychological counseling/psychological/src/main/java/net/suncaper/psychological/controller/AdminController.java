package net.suncaper.psychological.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import net.suncaper.psychological.common.Result;
import net.suncaper.psychological.entity.*;
import net.suncaper.psychological.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private FirstVisitMapper firstVisitMapper;
    @Autowired
    private DutyMapper dutyMapper;
    @Autowired
    private ExtraApplyMapper extraApplyMapper;

    // ===================== 管理员登录（加入Session存adminId） =====================
    @PostMapping("/login")
    public Result<User> login(@RequestBody User user, HttpSession session) {
        if (!user.getUsername().matches("^[0-9]+$")) {
            return Result.error("管理员工号必须是纯数字");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername())
                .eq(User::getPassword, user.getPassword())
                .eq(User::getRole, "admin");

        User loginUser = userMapper.selectOne(wrapper);
        if (loginUser != null) {
            // 登录成功，把管理员ID存入session，和学生端统一
            session.setAttribute("adminId", loginUser.getId());
            return Result.success(loginUser);
        } else {
            return Result.error("账号或密码错误");
        }
    }

    // ===================== 用户管理 =====================
    @GetMapping("/user/list")
    public Result<List<User>> userList() {
        return Result.success(userMapper.selectList(null));
    }

    @PostMapping("/user/save")
    public Result<Void> saveUser(@RequestBody User user) {
        userMapper.insert(user);
        return Result.success();
    }

    // ===================== 初访预约审核 =====================
    @GetMapping("/visit/waitAudit")
    public Result<List<FirstVisit>> waitAuditVisit() {
        LambdaQueryWrapper<FirstVisit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FirstVisit::getStatus, "待审核");
        return Result.success(firstVisitMapper.selectList(wrapper));
    }

    @PostMapping("/visit/audit")
    public Result<Void> auditVisit(@RequestBody FirstVisit firstVisit) {
        firstVisitMapper.updateById(firstVisit);
        return Result.success();
    }

    // ===================== 值班管理 =====================
    @PostMapping("/duty/save")
    public Result<Void> saveDuty(@RequestBody Duty duty) {
        dutyMapper.insert(duty);
        return Result.success();
    }

    // ===================== 追加咨询审批 =====================
    @GetMapping("/extra/list")
    public Result<List<ExtraApply>> extraApplyList() {
        return Result.success(extraApplyMapper.selectList(null));
    }

    @PostMapping("/extra/audit")
    public Result<Void> auditExtra(@RequestBody ExtraApply extraApply) {
        extraApplyMapper.updateById(extraApply);
        return Result.success();
    }
}