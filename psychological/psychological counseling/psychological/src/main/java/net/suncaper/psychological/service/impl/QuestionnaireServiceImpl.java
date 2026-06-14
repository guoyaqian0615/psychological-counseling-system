package net.suncaper.psychological.service.impl;

import lombok.RequiredArgsConstructor;
import net.suncaper.psychological.common.Result;
import net.suncaper.psychological.dto.QuestionnaireSubmitDTO;
import net.suncaper.psychological.entity.QuestionNaire;
import net.suncaper.psychological.mapper.QuestionnaireMapper;
import net.suncaper.psychological.service.QuestionnaireService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuestionnaireServiceImpl implements QuestionnaireService {

    private final QuestionnaireMapper questionnaireMapper;

    @Override
    public Result<?> submit(QuestionnaireSubmitDTO dto) {
        // 1. 校验 8 道题必须填写
        if (dto.getScores() == null || dto.getScores().size() != 8) {
            return Result.fail("请完成所有8道题目");
        }

        // 2. 计算总分
        int totalScore = dto.getScores().stream().mapToInt(Integer::intValue).sum();

        // 3. 判断等级 + 生成分析
        String level;
        String analysis;
        if (totalScore <= 4) {
            level = "正常";
            analysis = "心理状态平稳，情绪波动轻微，可通过日常放松调节。";
        } else if (totalScore <= 8) {
            level = "轻度困扰";
            analysis = "存在轻度情绪压力，建议多沟通、保持规律作息。";
        } else if (totalScore <= 12) {
            level = "中度困扰";
            analysis = "情绪困扰较明显，建议及时寻求心理老师帮助。";
        } else {
            level = "重度困扰";
            analysis = "心理压力较大，建议尽快预约专业心理咨询。";
        }

        // 4. 拼接答案字符串
        String answersStr = String.join(",", dto.getScores().stream().map(String::valueOf).toList());

        // 5. 存入数据库
        QuestionNaire q = new QuestionNaire();
        q.setStudentId(dto.getStudentId());
        q.setStudentName(dto.getStudentName());
        q.setTotalScore(totalScore);
        q.setAnalysis(analysis);
        q.setAnswers(answersStr);
        q.setContent(dto.getContent());
        q.setLevel(level);
        q.setCreateTime(LocalDateTime.now());
        questionnaireMapper.insert(q);

        // 6. 返回结果给前端
        Map<String, Object> map = new HashMap<>();
        map.put("totalScore", totalScore);
        map.put("level", level);
        map.put("analysis", analysis);

        return Result.success(map);
    }
}