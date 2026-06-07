package net.suncaper.psychological.controller;

import net.suncaper.psychological.common.Result;
import net.suncaper.psychological.entity.FirstVisit;
import net.suncaper.psychological.entity.User;
import net.suncaper.psychological.entity.vo.FirstVisitVO;
import net.suncaper.psychological.service.StudentService;
//呆
import net.suncaper.psychological.mapper.FirstVisitMapper;
import jakarta.annotation.Resource;
//呆
import org.springframework.web.bind.annotation.*;

import java.util.List;
//呆
import java.util.Map;
import net.suncaper.psychological.entity.vo.DutyVO;
import net.suncaper.psychological.entity.Notice;
//呆



@RestController
@RequestMapping("/student")
public class StudentController {
    private final StudentService studentService;

    //呆
    @Resource
    private FirstVisitMapper firstVisitMapper;
    //呆

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping("/login")
    public Result<User> login(@RequestBody User user) {
        return studentService.login(user);
    }

    @GetMapping("/myFirstVisits")
    public Result<List<FirstVisitVO>> myFirstVisits(@RequestParam Long studentId) {
        return studentService.myFirstVisits(studentId);
    }
    //呆
    //新建预约提交接口
    @PostMapping("/addVisit")
    public Result<Void> addVisit(@RequestBody FirstVisit firstVisit,
                                 @RequestParam Integer score,
                                 @RequestParam Boolean isAlert){
        //前端把缓存的score、isAlert随预约一起传过来
        return studentService.submitFirstVisit(firstVisit,score,isAlert);
    }
    //呆
    @PostMapping("/cancelVisit")
    public Result<Void> cancelVisit(@RequestParam Long id) {
        return studentService.cancelVisit(id);
    }

    //呆
    // 问卷提交 —— 终极容错版，绝对不报错
    @PostMapping("/saveQuestion")
    public Result<Map<String,Object>> saveQuestion(@RequestParam Long studentId, @RequestBody List<Integer> answerArr){
        //去掉visitId传参
        return studentService.submitQuestionnaire(studentId,answerArr);
    }
    //呆

    //查看问卷
    @GetMapping("/getQuestion/{id}")
    public Result<Map<String,Object>> getQuestion(@PathVariable Long id){
        return studentService.getQuestionResult(id);
    }
    //获取当前预约
    @GetMapping("/current")
    public Result<List<FirstVisitVO>> getCurrent(@RequestParam Long studentId){
        return studentService.getCurrentVisit(studentId);
    }
    //获取历史预约
    @GetMapping("/history")
    public Result<List<FirstVisitVO>> getHistory(@RequestParam Long studentId){
        return studentService.getHistoryVisit(studentId);
    }
    //呆

    //呆
    @GetMapping("/public/duty/list")
    public Result<List<DutyVO>> getDuty(){
        return studentService.getAllDutyWithUserName();
    }
    //呆
    //呆
    @GetMapping("/notice/list")
    public Result<List<Notice>> getNoticeList(@RequestParam Long studentId){
        return studentService.getNoticeList(studentId);
    }
    //呆
    //呆
    @PostMapping("/register")
    public Result<User> register(@RequestBody User user){
        return studentService.register(user);
    }
    //呆
}