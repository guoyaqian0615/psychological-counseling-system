package net.suncaper.psychological.service;
import java.time.LocalDate;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import net.suncaper.psychological.common.Result;
import net.suncaper.psychological.entity.*;
import net.suncaper.psychological.entity.vo.FirstVisitVO;

import java.util.List;
import java.util.Map;
import net.suncaper.psychological.entity.vo.DutyVO;

public interface AdminService {

    // ===== 登录 =====
    Result<User> login(User user);

    // ===== 用户管理 =====
    Result<List<User>> getUserList(String role);
    Result<Void> saveOrUpdateUser(User user);
    Result<Void> deleteUser(Long id);

    /** 通过学号（username）精确查询学生，用于管理员手动新增初访预约 */
    Result<User> findStudentByUsername(String username);

    // ===== 时间配置 =====
    Result<TimeConfig> getTimeConfig();
    Result<Void> saveTimeConfig(TimeConfig timeConfig);

    // ===== 值班管理 =====
    Result<List<Duty>> getDutyList();
    Result<Page<DutyVO>> getDutyPage(Integer pageNum, Integer pageSize);
    Result<Void> saveDuty(Duty duty);
    Result<Void> deleteDuty(Long id);
    Result<List<Duty>> autoGenerate(Long userId, String dutyDate);

    // ===== 初访预约（带初访员信息的联表分页）=====
    IPage<FirstVisitVO> getVisitVOPage(Integer pageNum, Integer pageSize, String status, String studentName);
    Result<Void> auditVisit(FirstVisit firstVisit);
    Result<Void> markEmergency(FirstVisit firstVisit);
    /** 全库各状态计数（不受分页/过滤影响），供统计卡片使用 */
    Result<Map<String, Long>> getVisitStats();

    // ===== 初访预约记录管理 =====
    Result<Void> addVisit(FirstVisit firstVisit);
    Result<Void> rescheduleVisit(FirstVisit firstVisit);
    Result<Void> cancelVisit(Long id);
    /** 物理删除初访预约记录，同时向学生发送取消通知 */
    Result<Void> deleteVisit(Long id);

    /** 今天或未来有值班安排的初访员（去重） */
    Result<List<User>> getTodayDutyVisitors();

    /**
     * 根据初访员 ID 查询其今天及之后的所有值班记录，
     * 并填充每个时段的已预约人数（bookedCount）。
     */
    Result<List<Duty>> getUserDutyDate(Long userId);

    // ===== 统计分析 =====
    Result<List<Map<String, Object>>> getStatSummary(String type, String startDate, String endDate);
    void exportStatExcel(String type, String startDate, String endDate, HttpServletResponse response);

    // ===== 结案报告批量下载 =====
    /**
     * ★ 新增 problemType 参数，支持按问题类型过滤。
     *
     * @param studentName   学生姓名（模糊，可为空）
     * @param counselorName 咨询师姓名（模糊，可为空）
     * @param problemType   问题类型（模糊，可为空）  ← NEW
     * @param startDate     结案日期起始（YYYY-MM-DD，可为空）
     * @param endDate       结案日期截止（YYYY-MM-DD，可为空）
     * @param response      HTTP 响应，直接写入 ZIP 流
     */
    void batchDownloadReports(String studentName,
                              String counselorName,
                              String problemType,       // ★ NEW
                              String startDate,
                              String endDate,
                              HttpServletResponse response);
}
