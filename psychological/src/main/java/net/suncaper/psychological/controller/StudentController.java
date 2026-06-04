package net.suncaper.psychological.controller;

import net.suncaper.psychological.common.Result;
import net.suncaper.psychological.entity.FirstVisit;
import net.suncaper.psychological.entity.User;
import net.suncaper.psychological.service.StudentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/student")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping("/login")
    public Result<User> login(@RequestBody User user) {
        return studentService.login(user);
    }

    @PostMapping("/firstVisit/submit")
    public Result<Void> submitFirstVisit(@RequestBody FirstVisit firstVisit) {
        return studentService.submitFirstVisit(firstVisit);
    }

    @GetMapping("/myFirstVisits")
    public Result<List<FirstVisit>> myFirstVisits(@RequestParam Long studentId) {
        return studentService.myFirstVisits(studentId);
    }

    @PostMapping("/cancelVisit")
    public Result<Void> cancelVisit(@RequestParam Long id) {
        return studentService.cancelVisit(id);
    }
}