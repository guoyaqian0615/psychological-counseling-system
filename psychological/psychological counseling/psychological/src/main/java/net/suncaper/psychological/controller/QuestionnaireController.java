package net.suncaper.psychological.controller;

import net.suncaper.psychological.common.Result;
import net.suncaper.psychological.dto.QuestionnaireSubmitDTO;
import net.suncaper.psychological.service.QuestionnaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 学生问卷 Controller
 */
@RestController
@RequestMapping("/student/questionnaire")
@RequiredArgsConstructor
// ❌ 这里不要加 @CrossOrigin，否则和全局冲突
public class QuestionnaireController {

    private final QuestionnaireService questionnaireService;

    @PostMapping("/submit")
    public Result<?> submit(@RequestBody QuestionnaireSubmitDTO dto) {
        questionnaireService.submit(dto);
        return Result.success("提交成功");
    }
}