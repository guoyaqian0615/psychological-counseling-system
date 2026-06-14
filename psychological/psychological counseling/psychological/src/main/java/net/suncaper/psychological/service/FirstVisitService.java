package net.suncaper.psychological.service;

import net.suncaper.psychological.entity.FirstVisit;
import java.util.List;

public interface FirstVisitService {
    // 提交预约：返回true成功，false重复
    boolean submit(FirstVisit firstVisit);

    // 我的预约
    List<FirstVisit> myVisits(Long studentId);

    // 撤销
    void cancel(Long id);

    // 根据id查预约
    FirstVisit getById(Long id);
}