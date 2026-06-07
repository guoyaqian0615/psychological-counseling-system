package net.suncaper.psychological.service;

import net.suncaper.psychological.common.Result;
import net.suncaper.psychological.entity.FirstVisit;
import net.suncaper.psychological.entity.User;
//呆
import net.suncaper.psychological.entity.vo.DutyVO;
import net.suncaper.psychological.entity.vo.FirstVisitVO;
import net.suncaper.psychological.entity.Notice;
//呆

import java.util.List;

//呆
import java.util.Map;
//呆


public interface StudentService {
    Result<User> login(User user);

    //呆
    Result<Void> submitFirstVisit(FirstVisit firstVisit, Integer score, Boolean isAlert);
    Result<List<FirstVisitVO>> myFirstVisits(Long studentId);
    //呆

    Result<Void> cancelVisit(Long id);

    //呆
    /**
     * 学生注册：新增学生user，role=student，复用user表
     */
    Result<User> register(User user);
    /**
     * 提交8题问卷，自动算分、生成分析文案，返回分数+是否预警
     */
    Result<Map<String,Object>> submitQuestionnaire(Long studentId, List<Integer> answerArr);

    /**
     * 根据预约id查询问卷得分+心理分析
     */
    Result<Map<String,Object>> getQuestionResult(Long visitId);

    /**
     * 查询单个学生【当前预约：非完成、非撤销】
     */
    //呆
    Result<List<FirstVisitVO>> getCurrentVisit(Long studentId);

    /**
     * 查询单个学生【全量历史预约：所有状态】
     */
    Result<List<FirstVisitVO>> getHistoryVisit(Long studentId);


    //新增：获取所有排班带咨询师姓名
    Result<List<DutyVO>> getAllDutyWithUserName();

    //新增方法定义
    //呆
    Result<List<Notice>> getNoticeList(Long studentId);
    //呆


}