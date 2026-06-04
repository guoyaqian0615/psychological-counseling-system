package net.suncaper.psychological.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.suncaper.psychological.common.Result;
import net.suncaper.psychological.entity.*;
import net.suncaper.psychological.entity.vo.CounselingVO;
import net.suncaper.psychological.entity.vo.FirstVisitResultVO;
import net.suncaper.psychological.mapper.*;
import net.suncaper.psychological.service.CounselService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CounselServiceImpl implements CounselService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private FirstVisitMapper firstVisitMapper;
    @Autowired
    private FirstVisitResultMapper firstVisitResultMapper;
    @Autowired
    private CounselingMapper counselingMapper;
    @Autowired
    private CounselingRecordMapper counselingRecordMapper;
    @Autowired
    private ExtraApplyMapper extraApplyMapper;
    @Autowired
    private ClosingReportMapper closingReportMapper;

    // ===================== 登录逻辑 =====================
    @Override
    public Result login(User user) {
        if (user.getUsername() == null || user.getPassword() == null || user.getRole() == null) {
            return Result.error("工号、密码、身份不能为空");
        }
        String username = user.getUsername().trim();
        String password = user.getPassword().trim();
        String frontRole = user.getRole().trim();

        if (!username.matches("\\d+")) {
            return Result.error("工号必须为纯数字");
        }

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        wrapper.eq("password", password);
        User realUser = userMapper.selectOne(wrapper);

        if (realUser == null) {
            return Result.error("工号或密码错误");
        }

        String realRole = realUser.getRole();
        if (realRole == null) {
            return Result.error("用户角色异常");
        }

        if (!realRole.equals(frontRole)) {
            return Result.error("身份选择错误！您的身份是：" + realRole);
        }

        if (!"visitor".equals(realRole) &&
                !"assistant".equals(realRole) &&
                !"counselor".equals(realRole)) {
            return Result.error("仅允许初访员、心理助理、咨询师登录");
        }

        return Result.success(realUser);
    }

    // ===================== 初访员 =====================

    /**
     * 修复：待处理列表查询逻辑
     *
     * 原问题：前端始终传 visitorId，导致只查"已分配给我"的记录。
     * 但任务刚创建时 visitor_id 为 NULL，这些公共任务根本查不出来。
     *
     * 修复后：
     *  - visitorId == null → 查所有 visitor_id IS NULL 的未领取公共任务
     *  - visitorId != null → 查已分配给该初访员的待处理任务
     */
    @Override
    public List<FirstVisit> getVisitorWaitList(Long visitorId, String visitDate) {
        LambdaQueryWrapper<FirstVisit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FirstVisit::getStatus, "已通过");

        if (visitorId != null) {
            // 传了 visitorId：查已分配给该初访员的任务
            wrapper.eq(FirstVisit::getVisitorId, visitorId);
        } else {
            // 未传 visitorId：查所有未分配的公共任务
            wrapper.isNull(FirstVisit::getVisitorId);
        }

        if (visitDate != null && !visitDate.isEmpty()) {
            wrapper.apply("DATE(visit_date) = {0}", visitDate);
        }
        wrapper.orderByDesc(FirstVisit::getApplyTime);
        return firstVisitMapper.selectList(wrapper);
    }

    @Override
    public void assignFirstVisitToVisitor(Long firstVisitId, Long visitorId) {
        FirstVisit firstVisit = firstVisitMapper.selectById(firstVisitId);
        if (firstVisit == null) {
            throw new RuntimeException("初访预约记录不存在");
        }
        if (!"已通过".equals(firstVisit.getStatus())) {
            throw new RuntimeException("仅能领取状态为'已通过'的初访任务");
        }
        firstVisit.setVisitorId(visitorId);
        firstVisitMapper.updateById(firstVisit);
    }

    @Override
    public void submitFirstVisitResult(FirstVisitResult result) {
        if (result.getFirstVisitId() == null) {
            throw new RuntimeException("必须关联初访预约ID");
        }
        FirstVisit firstVisit = firstVisitMapper.selectById(result.getFirstVisitId());
        if (firstVisit == null) {
            throw new RuntimeException("关联的初访预约记录不存在");
        }
        firstVisitResultMapper.insert(result);

        firstVisit.setStatus("已完成");
        firstVisitMapper.updateById(firstVisit);
    }

    @Override
    public List<FirstVisitResultVO> getVisitorHistory(Long visitorId, String studentName) {
        return firstVisitResultMapper.selectVisitorHistory(visitorId, studentName);
    }

    @Override
    public FirstVisitResult getVisitorHistoryDetail(Long id) {
        return firstVisitResultMapper.selectById(id);
    }

    @Override
    public FirstVisitResultVO getResultDetail(Long id) {
        return firstVisitResultMapper.getDetailById(id);
    }

    // ===================== 助理 =====================
    @Override
    public List<FirstVisit> getWaitArrangeList() {
        LambdaQueryWrapper<FirstVisit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FirstVisit::getStatus, "已通过");
        return firstVisitMapper.selectList(wrapper);
    }

    @Override
    public List<Counseling> counselList() {
        return counselingMapper.selectList(null);
    }

    @Override
    public void arrangeCounsel(Counseling counseling) {
        if (counseling.getStartDate() == null) {
            throw new RuntimeException("咨询开始日期不能为空");
        }
        if (counseling.getCounselingTime() == null || counseling.getCounselingTime().isEmpty()) {
            throw new RuntimeException("咨询时间段不能为空");
        }
        if (counseling.getCounselorId() == null) {
            throw new RuntimeException("咨询师ID不能为空");
        }
        if (counseling.getStudentId() == null) {
            throw new RuntimeException("学生ID不能为空");
        }
        Integer count = counselingMapper.countConflict(counseling.getCounselorId(), counseling.getCounselingTime());
        if (count > 0) {
            throw new RuntimeException("该咨询师当前时段已被占用，无法安排");
        }
        counseling.setTotalWeeks(8);
        counseling.setStatus("进行中");
        counselingMapper.insert(counseling);
    }

    @Override
    public void updateCounsel(Counseling counseling) {
        counselingMapper.updateById(counseling);
    }

    @Override
    public void closeCounsel(Long id) {
        Counseling counseling = counselingMapper.selectById(id);
        if (counseling == null) {
            throw new RuntimeException("咨询记录不存在");
        }
        counseling.setStatus("已结案");
        counselingMapper.updateById(counseling);
    }

    @Override
    public void deleteCounsel(Long id) {
        counselingMapper.deleteById(id);
    }

    // ===================== 咨询师 =====================
    @Override
    public Result<Page<Counseling>> getCounselorList(Long counselorId, Integer pageNum, Integer pageSize) {
        Page<Counseling> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Counseling> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Counseling::getCounselorId, counselorId)
                .orderByDesc(Counseling::getStartDate);
        return Result.success(counselingMapper.selectPage(page, wrapper));
    }

    @Override
    public void submitRecord(CounselingRecord record) {
        if (record.getTimes() == null) {
            throw new RuntimeException("必须填写第几次咨询");
        }
        if (record.getStatus() == null || record.getStatus().isEmpty()) {
            throw new RuntimeException("必须填写咨询状态");
        }
        counselingRecordMapper.insert(record);
    }

    @Override
    public List<CounselingRecord> getRecordList(Long counselingId) {
        LambdaQueryWrapper<CounselingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CounselingRecord::getCounselingId, counselingId)
                .orderByAsc(CounselingRecord::getTimes);
        return counselingRecordMapper.selectList(wrapper);
    }

    @Override
    public Result applyExtra(ExtraApply apply) {
        LambdaQueryWrapper<ExtraApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExtraApply::getCounselingId, apply.getCounselingId());
        Long already = extraApplyMapper.selectCount(wrapper);
        if (already >= 4) {
            return Result.error("最多只能追加4次！");
        }
        apply.setApplyTime(LocalDateTime.now());
        apply.setStatus("待审批");
        extraApplyMapper.insert(apply);
        return Result.success();
    }

    @Override
    public void submitClosing(ClosingReport report) {
        report.setCreateTime(LocalDateTime.now());
        closingReportMapper.insert(report);

        Counseling counseling = new Counseling();
        counseling.setId(report.getCounselingId());
        counseling.setStatus("已结案");
        counselingMapper.updateById(counseling);
    }

    @Override
    public List<ClosingReport> getClosingList(Long counselorId) {
        LambdaQueryWrapper<ClosingReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClosingReport::getCounselorId, counselorId)
                .orderByDesc(ClosingReport::getCreateTime);
        return closingReportMapper.selectList(wrapper);
    }
}
