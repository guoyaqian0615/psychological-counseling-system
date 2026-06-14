package net.suncaper.psychological.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import net.suncaper.psychological.common.Result;
import net.suncaper.psychological.entity.FirstVisit;
import net.suncaper.psychological.entity.User;
import net.suncaper.psychological.entity.Notice; //=====【修改1：补充Notice实体导入】=====
import net.suncaper.psychological.entity.vo.DutyVO;
import net.suncaper.psychological.entity.vo.FirstVisitVO;
import net.suncaper.psychological.mapper.FirstVisitMapper;
import net.suncaper.psychological.mapper.UserMapper;
import net.suncaper.psychological.mapper.DutyMapper;
import net.suncaper.psychological.mapper.NoticeMapper;
import net.suncaper.psychological.service.StudentService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


@Service
public class StudentServiceImpl implements StudentService {

    private final UserMapper userMapper;
    private final FirstVisitMapper firstVisitMapper;
    private final NoticeMapper noticeMapper;

    @Resource
    private DutyMapper dutyMapper;



    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    //=====【修改2：构造方法新增NoticeMapper入参，完成依赖注入】=====
    public StudentServiceImpl(UserMapper userMapper,
                              FirstVisitMapper firstVisitMapper,
                              NoticeMapper noticeMapper) {
        this.userMapper = userMapper;
        this.firstVisitMapper = firstVisitMapper;
        this.noticeMapper = noticeMapper;
    }

    @Override
    public Result<User> login(User user) {
        if (!user.getUsername().matches("^[0-9]+$")) {
            return Result.error("学号必须是纯数字");
        }
        //1、只根据学号+角色查库，不拼接密码
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername())
                .eq(User::getRole, "student");
        User loginUser = userMapper.selectOne(wrapper);
        if(loginUser == null){
            return Result.error("账号或密码错误");
        }

        String inputPwd = user.getPassword();
        String dbPwd = loginUser.getPassword();
        boolean passOk;
        //判断：数据库密码是BCrypt加密串（以$2a开头）→ 用matches校验
        if(dbPwd.startsWith("$2a")){
            passOk = passwordEncoder.matches(inputPwd, dbPwd);
        }else{
            //老用户明文密码：直接字符串全等
            passOk = inputPwd.equals(dbPwd);
        }
        if(!passOk){
            return Result.error("账号或密码错误");
        }
        return Result.success(loginUser);
    }

    // ====================== 【只改这里：预约提交时存入问卷分数】 ======================
    @Override
    public Result<Void> submitFirstVisit(FirstVisit firstVisit, Integer score, Boolean isAlert) {
        System.out.println("【预约提交-前端传来visitorId】：" + firstVisit.getVisitorId());
        firstVisit.setStatus("待审核");
        firstVisit.setCreateTime(LocalDateTime.now());
        firstVisit.setApplyTime(LocalDateTime.now());

        // 存入问卷分数 + 预警标记
        firstVisit.setQuestionnaireScore(score);
        firstVisit.setIsAlert(isAlert);

        //====新增：根据studentId查询学生姓名赋值====
        //==== 修复：使用当前登录学生的ID，不使用前端传来的ID ====
// 获取当前登录学生（从登录信息里拿，绝对正确）
        //呆
        User loginStudent = userMapper.selectById(firstVisit.getStudentId());

        if (loginStudent != null) {
            firstVisit.setStudentId(loginStudent.getId());   // 强制使用登录学生ID
            firstVisit.setStudentName(loginStudent.getName()); // 强制使用登录学生名字
        }
        //呆

        int row = firstVisitMapper.insert(firstVisit);
        return row>0 ? Result.success() : Result.error("保存失败");
    }
    @Override
    public Result<List<FirstVisitVO>> myFirstVisits(Long studentId) {
        List<FirstVisitVO> list = firstVisitMapper.getHistoryByStudentId(studentId);
        return Result.success(list);
    }

    @Override
    public Result<Void> cancelVisit(Long id) {
        FirstVisit fv = new FirstVisit();
        fv.setId(id);
        fv.setStatus("已撤销");
        firstVisitMapper.updateById(fv);
        return Result.success();
    }



    // ====================== 【问卷提交：只算分，不操作】 ======================
    @Override
    public Result<Map<String, Object>> submitQuestionnaire(Long studentId, List<Integer> answerArr) {
        Map<String,Object> map=new HashMap<>();
        if(answerArr.size()!=8){
            return Result.error("8道题目必须全部填写");
        }

        // 1. 原始分 0~16
        int rawTotal = answerArr.stream().mapToInt(Integer::intValue).sum();

        // 2. 百分制 0~100
        int score = (int) Math.round(rawTotal * 6.25);

        String level;
        String analysis;
        boolean isAlert = false;

        // 评分标准
        if (score >= 70) {
            level = "正常";
            analysis = "心理状态良好，情绪稳定，睡眠与社交正常。";
        } else if (score >= 40) {
            level = "轻度困扰";
            analysis = "存在轻微情绪波动，可通过运动、倾诉自我调节。";
        } else if (score >= 10) {
            level = "中度预警";
            analysis = "情绪影响学习生活，已为您优先安排咨询。";
            isAlert = true;
        } else {
            level = "高危预警";
            analysis = "心理风险较高，已标记紧急咨询。";
            isAlert = true;
        }

        // 【关键：问卷阶段不更新数据库！】
        map.put("score", score);
        map.put("level", level);
        map.put("analysis", analysis);
        map.put("isAlert", isAlert);
        return Result.success(map);
    }

    // ====================== 【查看测评报告：完全不动】 ======================
    @Override
    public Result<Map<String, Object>> getQuestionResult(Long visitId) {
        FirstVisit visit = firstVisitMapper.selectById(visitId);
        if(visit == null){
            return Result.error("预约不存在");
        }

        Integer score = visit.getQuestionnaireScore() == null ? 0 : visit.getQuestionnaireScore();
        String level;
        String analysis;

        if (score >= 70) {
            level = "正常";
            analysis = "【测评报告】心理状态良好，情绪稳定，无明显困扰。";
        } else if (score >= 40) {
            level = "轻度困扰";
            analysis = "【测评报告】存在轻微情绪波动，可自我调节恢复。";
        } else if (score >= 10) {
            level = "中度预警";
            analysis = "【测评报告】存在明显心理困扰，已优先安排咨询。";
        } else {
            level = "高危预警";
            analysis = "【测评报告】心理风险较高，已标记紧急优先处理。";
        }

        Map<String,Object> res=new HashMap<>();
        res.put("score", score);
        res.put("level", level);
        res.put("analysis", analysis);
        return Result.success(res);
    }

    @Override
    public Result<List<FirstVisitVO>> getCurrentVisit(Long studentId) {
        List<FirstVisitVO> list = firstVisitMapper.getCurrentByStudentId(studentId);
        return Result.success(list);
    }

    @Override
    public Result<List<FirstVisitVO>> getHistoryVisit(Long studentId) {
        List<FirstVisitVO> list = firstVisitMapper.getHistoryByStudentId(studentId);
        return Result.success(list);
    }
    @Override
    public Result<List<DutyVO>> getAllDutyWithUserName() {
        // 必须是这行，调用新建的过滤方法
        List<DutyVO> list = dutyMapper.getVisitorDutyOnly();
        return Result.success(list);
    }
    @Override
    public Result<List<Notice>> getNoticeList(Long studentId) {
        LambdaQueryWrapper<Notice> wrapper = Wrappers.lambdaQuery();
        //★修改：getStudentId → getUserId
        wrapper.eq(Notice::getUserId, studentId)
                .orderByDesc(Notice::getCreateTime);
        List<Notice> list = noticeMapper.selectList(wrapper);
        return Result.success(list);
    }
    //呆
    @Override
    public Result<User> register(User user) {
        //1、校验学号是否已存在（username=学号唯一）
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getUsername,user.getUsername());
        Long count = userMapper.selectCount(wrapper);
        if(count>0){
            return Result.error("该学号已注册，请勿重复注册");
        }
        //2、密码加密（你项目BCrypt加密，沿用登录加密规则）
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        user.setPassword(encoder.encode(user.getPassword()));
        //3、固定角色学生
        user.setRole("student");
        //4、插入数据库
        userMapper.insert(user);
        return Result.success(user);
    }
    //呆
}