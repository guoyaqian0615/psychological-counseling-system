package net.suncaper.psychological.controller;

import jakarta.annotation.Resource;
import net.suncaper.psychological.common.Result;
import net.suncaper.psychological.entity.*;

import net.suncaper.psychological.entity.vo.FirstVisitResultVO;
import net.suncaper.psychological.service.CounselService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/counsel")
public class CounselController {

    @Autowired
    private CounselService counselService;

    // ===================== 统一登录 =====================
    @PostMapping("/login")
    public Result login(@RequestBody User user) {
        return counselService.login(user);
    }

    // ====================== 【初访员功能】 ======================
    /**
     * 初访员查询待处理的初访预约列表（已通过审核、未完成初访）
     */
    @GetMapping("/visitor/waitList")
    public Result<List<FirstVisit>> visitorWaitList(
            @RequestParam(required = false) Long visitorId, // 可选：初访员筛选自己的待处理列表
            @RequestParam(required = false) String visitDate // 可选：按日期筛选
    ) {
        return Result.success(counselService.getVisitorWaitList(visitorId, visitDate));
    }

    /**
     * 初访员分配自己的待处理初访（领取初访任务）
     */
    @PostMapping("/visitor/assignVisit")
    public Result<Void> visitorAssignVisit(@RequestParam Long firstVisitId, @RequestParam Long visitorId) {
        counselService.assignFirstVisitToVisitor(firstVisitId, visitorId);
        return Result.success();
    }

    /**
     * 初访员提交初访结果
     */
    @PostMapping("/visitor/submitResult")
    public Result<Void> visitorSubmitResult(@RequestBody FirstVisitResult result) {
        counselService.submitFirstVisitResult(result);
        return Result.success();
    }

    /**
     * 初访员查询自己的初访记录（支持学生姓名筛选）
     */
    @GetMapping("/visitor/myHistory")
    public Result<List<FirstVisitResultVO>> visitorMyHistory(
            @RequestParam Long visitorId,
            @RequestParam(required = false) String studentName
    ) {
        return Result.success(counselService.getVisitorHistory(visitorId,studentName));
    }

    /**
     * 初访员查询单条初访记录详情
     */
    @GetMapping("/visitor/history/{id}")
    public Result<FirstVisitResult> visitorHistoryDetail(@PathVariable Long id) {
        return Result.success(counselService.getVisitorHistoryDetail(id));
    }

    @GetMapping("/history/{id}")
    public Result<FirstVisitResultVO> getDetail(@PathVariable Long id){
        return Result.success(counselService.getResultDetail(id));
    }

    // ====================== 【心理助理功能】 ======================
//    @GetMapping("/assistant/waitArrange")
//    public Result<List<FirstVisit>> assistantWaitArrange() {
//        return Result.success(counselService.getWaitArrangeList());
//    }
//
//    @PostMapping("/assistant/arrangeCounsel")
//    public Result<Void> assistantArrangeCounsel(@RequestBody Counseling counseling) {
//        counselService.arrangeCounsel(counseling);
//        return Result.success();
//    }
//
//    @GetMapping("/assistant/counselList")
//    public Result<List<Counseling>> counselList(){
//        return Result.success(counselService.counselList());
//    }
//
//    @PostMapping("/assistant/updateCounsel")
//    public Result<Void> assistantUpdateCounsel(@RequestBody Counseling counseling) {
//        counselService.updateCounsel(counseling);
//        return Result.success();
//    }
//
//    @GetMapping("/assistant/closeCounsel/{id}")
//    public Result<Void> assistantCloseCounsel(@PathVariable Long id) {
//        counselService.closeCounsel(id);
//        return Result.success();
//    }
//
//    @GetMapping("/assistant/deleteCounsel/{id}")
//    public Result<Void> assistantDeleteCounsel(@PathVariable Long id) {
//        counselService.deleteCounsel(id);
//        return Result.success();
//    }

    // ===================== 【咨询师功能】 =====================
    @GetMapping("/counselor/list")
    public Result getMyCounseling(
            @RequestParam Long counselorId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return counselService.getCounselorList(counselorId, pageNum, pageSize);
    }

    @PostMapping("/counselor/submitRecord")
    public Result<Void> submitRecord(@RequestBody CounselingRecord record) {
        counselService.submitRecord(record);
        return Result.success();
    }

    @GetMapping("/counselor/recordList")
    public Result<List<CounselingRecord>> getRecordList(@RequestParam Long counselingId)
    {
        return Result.success(counselService.getRecordList(counselingId));
    }

    @PostMapping("/counselor/applyExtra")
    public Result<Void> applyExtra(@RequestBody ExtraApply apply) {
        return counselService.applyExtra(apply);
    }

    @PostMapping("/counselor/submitClosing")
    public Result<Void> submitClosing(@RequestBody ClosingReport report) {
        counselService.submitClosing(report);
        return Result.success();
    }

    @GetMapping("/counselor/closingList")
    public Result<List<ClosingReport>> getClosingList(@RequestParam Long counselorId) {
        return Result.success(counselService.getClosingList(counselorId));
    }
}