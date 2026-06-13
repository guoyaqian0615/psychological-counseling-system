package net.suncaper.psychological.service;

import net.suncaper.psychological.common.Result;
import net.suncaper.psychological.entity.FirstVisit;
import net.suncaper.psychological.entity.User;
import net.suncaper.psychological.entity.vo.DutyVO;
import net.suncaper.psychological.entity.vo.FirstVisitVO;
import net.suncaper.psychological.entity.Notice;
import java.util.List;
import java.util.Map;

public interface StudentService {
    Result<User> login(User user);
    Result<Void> submitFirstVisit(FirstVisit firstVisit, Integer score, Boolean isAlert);
    Result<List<FirstVisitVO>> myFirstVisits(Long studentId);
    Result<User> register(User user);
    Result<Map<String,Object>> submitQuestionnaire(Long studentId, List<Integer> answerArr);
    Result<Map<String,Object>> getQuestionResult(Long visitId);
    Result<List<FirstVisitVO>> getCurrentVisit(Long studentId);
    //获取所有排班带咨询师姓名
    Result<List<DutyVO>> getAllDutyWithUserName();
    Result<List<Notice>> getNoticeList(Long studentId);


}