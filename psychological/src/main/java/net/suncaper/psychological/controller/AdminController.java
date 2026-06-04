package net.suncaper.psychological.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import net.suncaper.psychological.common.Result;
import net.suncaper.psychological.entity.*;
import net.suncaper.psychological.entity.vo.ExtraApplyVO;
import net.suncaper.psychological.entity.vo.FirstVisitVO;
import net.suncaper.psychological.service.AdminService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import net.suncaper.psychological.entity.vo.DutyVO;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ===== 登录 =====

    @PostMapping("/login")
    public Result<User> login(@RequestBody User user) {
        return adminService.login(user);
    }

    // ===== 用户管理 =====

    @GetMapping("/user/list")
    public Result<List<User>> userList(@RequestParam(required = false) String role) {
        return adminService.getUserList(role);
    }

    /**
     * 通过学号（username）精确查询学生信息。
     * 管理员手动新增初访预约时使用。
     * GET /admin/user/student?username=2021001234
     */
    @GetMapping("/user/student")
    public Result<User> findStudent(@RequestParam String username) {
        return adminService.findStudentByUsername(username);
    }

    @PostMapping("/user/save")
    public Result<Void> saveUser(@RequestBody User user) {
        return adminService.saveOrUpdateUser(user);
    }

    @DeleteMapping("/user/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        return adminService.deleteUser(id);
    }

    // ===== 时间配置 =====

    @GetMapping("/timeConfig/get")
    public Result<TimeConfig> getTimeConfig() {
        return adminService.getTimeConfig();
    }

    @PostMapping("/timeConfig/save")
    public Result<Void> saveTimeConfig(@RequestBody TimeConfig timeConfig) {
        return adminService.saveTimeConfig(timeConfig);
    }

    // ===== 值班管理 =====

    @GetMapping("/duty/list")
    public Result<List<Duty>> dutyList() {
        return adminService.getDutyList();
    }

    @GetMapping("/duty/page")
    public Result<Page<DutyVO>> dutyPage(@RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        return adminService.getDutyPage(pageNum, pageSize);
    }

    @PostMapping("/duty/save")
    public Result<Void> saveDuty(@RequestBody Duty duty) {
        return adminService.saveDuty(duty);
    }

    @DeleteMapping("/duty/{id}")
    public Result<Void> deleteDuty(@PathVariable Long id) {
        return adminService.deleteDuty(id);
    }

    @PostMapping("/duty/auto")
    public Result<List<Duty>> autoDuty(
            @RequestParam Long userId,
            @RequestParam String dutyDate
    ) {
        return adminService.autoGenerate(userId, dutyDate);
    }

    // ===== 初访预约 =====

    @GetMapping("/visit/page")
    public IPage<FirstVisitVO> visitPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String studentName) {
        return adminService.getVisitVOPage(pageNum, pageSize, status, studentName);
    }

    @PostMapping("/visit/audit")
    public Result<Void> auditVisit(@RequestBody FirstVisit firstVisit) {
        return adminService.auditVisit(firstVisit);
    }

    /** 全库各状态计数，供统计卡片使用（不受列表筛选影响） */
    @GetMapping("/visit/stats")
    public Result<Map<String, Long>> visitStats() {
        return adminService.getVisitStats();
    }

    @PostMapping("/visit/markEmergency")
    public Result<Void> markEmergency(@RequestBody FirstVisit firstVisit) {
        return adminService.markEmergency(firstVisit);
    }

    @GetMapping("/visit/today-duty-visitors")
    public Result<List<User>> getTodayDutyVisitors() {
        return adminService.getTodayDutyVisitors();
    }

    /**
     * 根据初访员 ID 查询其今天及之后的值班记录，含每个时段的已预约人数。
     * GET /admin/visit/get-duty-date/{userId}
     */
    @GetMapping("/visit/get-duty-date/{userId}")
    public Result<List<Duty>> getUserDutyDate(@PathVariable Long userId) {
        return adminService.getUserDutyDate(userId);
    }

    // ===== 初访预约记录管理 =====

    @PostMapping("/visit/add")
    public Result<Void> addVisit(@RequestBody FirstVisit firstVisit) {
        return adminService.addVisit(firstVisit);
    }

    @PostMapping("/visit/reschedule")
    public Result<Void> rescheduleVisit(@RequestBody FirstVisit firstVisit) {
        return adminService.rescheduleVisit(firstVisit);
    }

    @PostMapping("/visit/cancel/{id}")
    public Result<Void> cancelVisit(@PathVariable Long id) {
        return adminService.cancelVisit(id);
    }

    // ===== 追加咨询审批 =====

    @GetMapping("/extra/list")
    public Result<List<ExtraApplyVO>> extraList() {
        return adminService.getExtraApplyList();
    }

    @PostMapping("/extra/audit")
    public Result<Void> auditExtra(@RequestBody ExtraApply extraApply) {
        return adminService.auditExtra(extraApply);
    }

    // ===== 统计分析 =====

    @GetMapping("/stat/summary")
    public Result<List<Map<String, Object>>> statSummary(
            @RequestParam String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return adminService.getStatSummary(type, startDate, endDate);
    }

    @GetMapping("/stat/export")
    public void exportStat(
            @RequestParam String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) {
        adminService.exportStatExcel(type, startDate, endDate, response);
    }

    // ===== 结案报告批量下载 =====

    /**
     * GET /admin/report/batchDownload
     * ★ 新增 problemType 查询参数，支持按问题类型筛选结案报告。
     */
    @GetMapping("/report/batchDownload")
    public void batchDownloadReports(
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) String counselorName,
            @RequestParam(required = false) String problemType,   // ★ NEW
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) {
        adminService.batchDownloadReports(
                studentName, counselorName, problemType, startDate, endDate, response);
    }
}
