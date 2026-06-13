package net.suncaper.psychological.controller;

import jakarta.annotation.Resource;
import net.suncaper.psychological.common.Result;
import net.suncaper.psychological.entity.*;

import net.suncaper.psychological.entity.vo.FirstVisitResultVO;
import net.suncaper.psychological.service.CounselService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/counsel")
public class CounselController {

    @Autowired
    private CounselService counselService;

    // ===================== 统一登录 =====================
    //功能：所有角色（初访员 / 助理 / 咨询师）共用登录接口；
    //实现：前端传递账号、密码、角色的 User 对象，控制器直接调用服务层登录方法，将结果返回前端。
    @PostMapping("/login")
    public Result login(@RequestBody User user) {
        return counselService.login(user);
    }

    // ====================== 【初访员功能】 ======================
    /**
     * 初访员查询待处理的初访预约列表（已通过审核、未完成初访）
     */
    //功能：查询待领取的初访任务列表；
    //入参：可传初访员 ID、预约日期做筛选；
    //实现：调用服务方法，返回符合条件的初访预约数据。
    @GetMapping("/visitor/waitList")
    public Result<List<FirstVisit>> visitorWaitList(
            @RequestParam(required = false) Long visitorId, // 可选：初访员筛选自己的待处理列表
            @RequestParam(required = false) String visitDate // 可选：按日期筛选
    ) {
        return Result.success(counselService.getVisitorWaitList(visitorId, visitDate));
    }

    //功能：初访员领取初访任务（把预约记录绑定到自己账号）；
    //入参：初访预约 ID、初访员 ID；
    //实现：调用服务完成数据更新。
    @PostMapping("/visitor/assignVisit")
    public Result<Void> visitorAssignVisit(@RequestParam Long firstVisitId, @RequestParam Long visitorId) {
        counselService.assignFirstVisitToVisitor(firstVisitId, visitorId);
        return Result.success();
    }

    //功能：初访完成后，提交初访评估结果（危机等级、问题类型等）；
    //入参：初访结果实体；
    //实现：调用服务插入结果、同步更新初访状态。
    @PostMapping("/visitor/submitResult")
    public Result<Void> visitorSubmitResult(@RequestBody FirstVisitResult result) {
        counselService.submitFirstVisitResult(result);
        return Result.success();
    }
    //查询本人所有初访历史记录，支持按学生姓名模糊搜索。
    @GetMapping("/visitor/myHistory")
    public Result<List<FirstVisitResultVO>> visitorMyHistory(
            @RequestParam Long visitorId,
            @RequestParam(required = false) String studentName
    ) {
        return Result.success(counselService.getVisitorHistory(visitorId,studentName));
    }

    ///初访员查询单条初访记录详情
    @GetMapping("/visitor/history/{id}")
    public Result<FirstVisitResult> visitorHistoryDetail(@PathVariable Long id) {
        return Result.success(counselService.getVisitorHistoryDetail(id));
    }
    @GetMapping("/history/{id}")
    public Result<FirstVisitResultVO> getDetail(@PathVariable Long id){
        return Result.success(counselService.getResultDetail(id));
    }

    // ====================== 【心理助理功能】 ======================
    //助理负责查看待分配预约、安排咨询、修改 / 删除 / 结案咨询。

    //查询等待分配咨询的学生列表
    @GetMapping("/assistant/waitArrange")
    public Result<List<FirstVisitResultVO>> assistantWaitArrange() {
        return Result.success(counselService.getWaitArrangeList());
    }
    //安排正式咨询（核心接口）
    @PostMapping("/assistant/arrangeCounsel")
    public Result<Void> assistantArrangeCounsel(@RequestBody Counseling counseling) {
        counselService.arrangeCounsel(counseling);
        return Result.success();
    }
    //查询所有已安排的咨询记录
    @GetMapping("/assistant/counselList")
    public Result<List<Counseling>> counselList(){
        return Result.success(counselService.counselList());
    }
    //修改已有的咨询（更换咨询师、调整时间地点）
    @PostMapping("/assistant/updateCounsel")
    public Result<Void> assistantUpdateCounsel(@RequestBody Counseling counseling) {
        counselService.updateCounsel(counseling);
        return Result.success();
    }
    //将咨询标记为「已结案」
    @GetMapping("/assistant/closeCounsel/{id}")
    public Result<Void> assistantCloseCounsel(@PathVariable Long id) {
        counselService.closeCounsel(id);
        return Result.success();
    }
    //删除咨询记录
    @GetMapping("/assistant/deleteCounsel/{id}")
    public Result<Void> assistantDeleteCounsel(@PathVariable Long id) {
        counselService.deleteCounsel(id);
        return Result.success();
    }

    //只查询今日及之后有值班的咨询师，避免选择无排班人员；
    @GetMapping("/assistant/getCounselorWithDuty")
    public Result<List<User>> getCounselorWithDuty(){
        return Result.success(counselService.getCounselorWithDuty());
    }

    //根据选中的咨询师 ID，查询空闲日期 + 空闲时段（日历禁用逻辑依赖此接口）
    @GetMapping("/assistant/getFreeTimeByCounsel")
    public Result<Map<String,Object>> getFreeTimeByCounsel(@RequestParam Long counselorId){
        return counselService.getCounselFreeTime(counselorId);
    }
    // ===================== 【咨询师功能】 =====================
    //咨询师查看自己的咨询、填写每次咨询记录、生成结案报告
    //分页查询分配给自己的咨询列表
    @GetMapping("/counselor/list")
    public Result getMyCounseling(
            @RequestParam Long counselorId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return counselService.getCounselorList(counselorId, pageNum, pageSize);
    }
    //提交单次咨询记录（完成/缺席等状态）
    @PostMapping("/counselor/submitRecord")
    public Result<Void> submitRecord(@RequestBody CounselingRecord record) {
        counselService.submitRecord(record);
        return Result.success();
    }
    //查询某条咨询下的所有分次记录
    @GetMapping("/counselor/recordList")
    public Result<List<CounselingRecord>> getRecordList(@RequestParam Long counselingId)
    {
        return Result.success(counselService.getRecordList(counselingId));
    }
     //根据 counseling.student_id 查询学生基本信息（学号、性别、电话、院系）
     //用于结案报告填写时自动回填
    @GetMapping("/counselor/studentInfo")
    public Result<User> getStudentInfo(@RequestParam Long studentId) {
        return Result.success(counselService.getStudentInfo(studentId));
    }
    //提交正式结案报告
    @PostMapping("/counselor/submitClosing")
    public Result<Void> submitClosing(@RequestBody ClosingReport report) {
        counselService.submitClosing(report);
        return Result.success();
    }
    //查询本人所有结案报告
    @GetMapping("/counselor/closingList")
    public Result<List<ClosingReport>> getClosingList(@RequestParam Long counselorId) {
        return Result.success(counselService.getClosingList(counselorId));
    }
}