package net.suncaper.psychological.service;

import net.suncaper.psychological.common.Result;
import net.suncaper.psychological.entity.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import net.suncaper.psychological.entity.vo.FirstVisitResultVO;

import java.time.LocalDateTime;
import java.util.List;

public interface CounselService {
    // 登录
    Result login(User user);

    // ===================== 初访员功能 =====================
    /**
     * 查询待处理的初访列表
     * @param visitorId 可选：初访员ID（筛选自己的待处理）
     * @param visitDate 可选：按初访日期筛选
     * @return 初访列表
     */
    List<FirstVisit> getVisitorWaitList(Long visitorId, String visitDate);

    /**
     * 初访员领取初访任务（分配初访员ID到初访记录）
     * @param firstVisitId 初访预约ID
     * @param visitorId 初访员ID
     */
    void assignFirstVisitToVisitor(Long firstVisitId, Long visitorId);

    /**
     * 提交初访结果
     * @param result 初访结果
     */
    void submitFirstVisitResult(FirstVisitResult result);

    /**
     * 查询初访员的历史记录
     * @param visitorId 初访员ID
     * @param studentName 学生姓名（模糊查询）
     * @return 初访结果列表
     */
    List<FirstVisitResultVO> getVisitorHistory(Long visitorId,String studentName);

    /**
     * 查询单条初访记录详情
     * @param id 初访结果ID
     * @return 初访结果详情
     */
    FirstVisitResult getVisitorHistoryDetail(Long id);

    FirstVisitResultVO getResultDetail(Long id);

    // ===================== 助理功能 =====================
// 心理助理：查询可安排的预约（已通过初访）
    List<FirstVisit> getWaitArrangeList();
    List<Counseling> counselList();

    // 安排咨询
    void arrangeCounsel(Counseling counseling);

    // 修改咨询
    void updateCounsel(Counseling counseling);

    // 结案（释放咨询师）
    void closeCounsel(Long id);

    // 删除咨询
    void deleteCounsel(Long id);

    // ===================== 咨询师功能 =====================
    Result<Page<Counseling>> getCounselorList(Long counselorId, Integer pageNum, Integer pageSize);
    void submitRecord(CounselingRecord record);
    List<CounselingRecord> getRecordList(Long counselingId);
    Result applyExtra(ExtraApply apply);
    void submitClosing(ClosingReport report);
    List<ClosingReport> getClosingList(Long counselorId);
}