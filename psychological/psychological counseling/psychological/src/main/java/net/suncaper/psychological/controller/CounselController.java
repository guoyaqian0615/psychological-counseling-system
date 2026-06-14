package net.suncaper.psychological.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import net.suncaper.psychological.common.Result;
import net.suncaper.psychological.entity.*;
import net.suncaper.psychological.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;

@RestController
@RequestMapping("/counsel")
public class CounselController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private FirstVisitMapper firstVisitMapper;
    @Autowired
    private FirstVisitResultMapper firstVisitResultMapper;
    @Autowired
    private CounselingMapper counselingMapper;
    @Autowired
    private CounselingRecordMapper counselingRecordMapper;
    @Autowired
    private ExtraApplyMapper extraApplyMapper;

    // ===================== 统一登录（支持三种角色） =====================
    @PostMapping("/login")
    public Result login(@RequestBody User user) {
        if (user.getUsername() == null || user.getPassword() == null || user.getRole() == null) {
            return Result.error("工号、密码、身份不能为空");
        }

        String username = user.getUsername().trim();
        String password = user.getPassword().trim();
        String frontRole = user.getRole().trim();

        if (!username.matches("\\d+")) {
            return Result.error("工号必须为纯数字");
        }

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        wrapper.eq("password", password);
        User realUser = userMapper.selectOne(wrapper);

        if (realUser == null) {
            return Result.error("工号或密码错误");
        }

        String realRole = realUser.getRole();
        if (realRole == null) {
            return Result.error("用户角色异常");
        }

        // 角色必须严格匹配
        if (!realRole.equals(frontRole)) {
            return Result.error("身份选择错误！您的身份是：" + realRole);
        }

        // 只允许这三种角色
        if (!"interviewer".equals(realRole) &&
                !"assistant".equals(realRole) &&
                !"counselor".equals(realRole)) {
            return Result.error("非法角色，无法登录");
        }

        return Result.success(realUser);
    }

    // ===================== 【初访员 visitor 功能】 =====================
    /**
     * 查看待初访列表（状态：已通过）
     */
    @GetMapping("/visitor/waitList")
    public Result<List<FirstVisit>> visitorWaitList() {
        LambdaQueryWrapper<FirstVisit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FirstVisit::getStatus, "已通过");
        return Result.success(firstVisitMapper.selectList(wrapper));
    }

    /**
     * 提交初访结果
     */
    @PostMapping("/visitor/submitResult")
    public Result<Void> visitorSubmitResult(@RequestBody FirstVisitResult result) {
        firstVisitResultMapper.insert(result);
        return Result.success();
    }

    // ===================== 【心理助理 assistant 功能】 =====================
    /**
     * 查看所有初访预约
     */
    @GetMapping("/assistant/allVisit")
    public Result<List<FirstVisit>> assistantAllVisit() {
        return Result.success(firstVisitMapper.selectList(null));
    }

    /**
     * 安排正式咨询
     */
    @PostMapping("/assistant/arrangeCounsel")
    public Result<Void> assistantArrangeCounsel(@RequestBody Counseling counseling) {
        counselingMapper.insert(counseling);
        return Result.success();
    }

    // ===================== 【咨询师 counselor 功能】 =====================
    /**
     * 查看自己负责的咨询学生
     */
    @GetMapping("/counselor/myStudent")
    public Result<List<Counseling>> counselorMyStudent(@RequestParam Long counselorId) {
        LambdaQueryWrapper<Counseling> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Counseling::getCounselorId, counselorId);
        return Result.success(counselingMapper.selectList(wrapper));
    }

    /**
     * 提交单次咨询记录
     */
    @PostMapping("/counselor/submitRecord")
    public Result<Void> counselorSubmitRecord(@RequestBody CounselingRecord record) {
        counselingRecordMapper.insert(record);
        return Result.success();
    }

    /**
     * 提交追加咨询申请
     */
    @PostMapping("/counselor/applyExtra")
    public Result<Void> counselorApplyExtra(@RequestBody ExtraApply extraApply) {
        extraApplyMapper.insert(extraApply);
        return Result.success();
    }
}