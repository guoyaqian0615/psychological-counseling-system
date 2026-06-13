package net.suncaper.psychological.controller;

import net.suncaper.psychological.common.Result;
import net.suncaper.psychological.entity.FirstVisit;
import net.suncaper.psychological.entity.User;
import net.suncaper.psychological.entity.vo.FirstVisitVO;
import net.suncaper.psychological.service.StudentService;
import net.suncaper.psychological.mapper.FirstVisitMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import net.suncaper.psychological.entity.vo.DutyVO;
import net.suncaper.psychological.entity.Notice;

@RestController
@RequestMapping("/student")
public class StudentController {
    private final StudentService studentService;

    @Resource
    private FirstVisitMapper firstVisitMapper;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }
    //学生登录接口
    @PostMapping("/login")
    public Result<User> login(@RequestBody User user) {
        return studentService.login(user);
    }
    //查询学生的所有初访预约记录接口
    @GetMapping("/myFirstVisits")
    public Result<List<FirstVisitVO>> myFirstVisits(@RequestParam Long studentId) {
        return studentService.myFirstVisits(studentId);
    }

    //新建/提交预约提交接口
    @PostMapping("/addVisit")
    public Result<Void> addVisit(@RequestBody FirstVisit firstVisit,
                                 @RequestParam Integer score,
                                 @RequestParam Boolean isAlert){
        //调用service层提交预约，同时保存问卷得分与预警标识
        return studentService.submitFirstVisit(firstVisit,score,isAlert);
    }

    // 问卷提交接口
    @PostMapping("/saveQuestion")
    public Result<Map<String,Object>> saveQuestion(@RequestParam Long studentId, @RequestBody List<Integer> answerArr){
        return studentService.submitQuestionnaire(studentId,answerArr);
    }

    //查看问卷结果接口
    @GetMapping("/getQuestion/{id}")
    public Result<Map<String,Object>> getQuestion(@PathVariable Long id){
        return studentService.getQuestionResult(id);
    }
    //获取当前预约接口
    @GetMapping("/current")
    public Result<List<FirstVisitVO>> getCurrent(@RequestParam Long studentId){
        return studentService.getCurrentVisit(studentId);
    }
    //获取所有咨询师值班安排接口
    @GetMapping("/public/duty/list")
    public Result<List<DutyVO>> getDuty(){
        return studentService.getAllDutyWithUserName();
    }
    //获取学生的站内通知列表接口
    @GetMapping("/notice/list")
    public Result<List<Notice>> getNoticeList(@RequestParam Long studentId){
        return studentService.getNoticeList(studentId);
    }
    //学生注册接口
    @PostMapping("/register")
    public Result<User> register(@RequestBody User user){
        return studentService.register(user);
    }

}