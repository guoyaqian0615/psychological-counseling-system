package net.suncaper.psychological.controller;

import net.suncaper.psychological.common.Result;
import net.suncaper.psychological.entity.FirstVisit;
import net.suncaper.psychological.service.FirstVisitService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/firstVisit")
public class FirstVisitController {

    private final FirstVisitService firstVisitService;

    public FirstVisitController(FirstVisitService firstVisitService) {
        this.firstVisitService = firstVisitService;
    }

    // ======== 原来冲突的 /firstVisit/submit 彻底删掉！========

    @GetMapping("/get/{id}")
    public Result<FirstVisit> getById(@PathVariable Long id) {
        return Result.success(firstVisitService.getById(id));
    }
}