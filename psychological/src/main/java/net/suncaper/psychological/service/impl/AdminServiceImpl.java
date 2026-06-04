package net.suncaper.psychological.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import net.suncaper.psychological.common.Result;
import net.suncaper.psychological.entity.*;
import net.suncaper.psychological.entity.vo.*;
import net.suncaper.psychological.mapper.*;
import net.suncaper.psychological.service.AdminService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
// ★ NEW: XWPF (Word) imports
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import java.math.BigInteger;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Transactional
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final FirstVisitMapper firstVisitMapper;
    private final DutyMapper dutyMapper;
    private final ExtraApplyMapper extraApplyMapper;
    private final NoticeMapper noticeMapper;
    private final TimeConfigMapper timeConfigMapper;
    private final CounselingMapper counselingMapper;
    private final ClosingReportMapper closingReportMapper;
    private final CounselingRecordMapper counselingRecordMapper;

    public AdminServiceImpl(UserMapper userMapper,
                            FirstVisitMapper firstVisitMapper,
                            DutyMapper dutyMapper,
                            ExtraApplyMapper extraApplyMapper,
                            NoticeMapper noticeMapper,
                            TimeConfigMapper timeConfigMapper,
                            CounselingMapper counselingMapper,
                            ClosingReportMapper closingReportMapper,
                            CounselingRecordMapper counselingRecordMapper) {
        this.userMapper = userMapper;
        this.firstVisitMapper = firstVisitMapper;
        this.dutyMapper = dutyMapper;
        this.extraApplyMapper = extraApplyMapper;
        this.noticeMapper = noticeMapper;
        this.timeConfigMapper = timeConfigMapper;
        this.counselingMapper = counselingMapper;
        this.closingReportMapper = closingReportMapper;
        this.counselingRecordMapper = counselingRecordMapper;
    }

    // ===== 登录 =====

    @Override
    public Result<User> login(User user) {
        if (!StringUtils.hasText(user.getUsername()) || !user.getUsername().matches("^[0-9]+$")) {
            return Result.error("工号必须为数字");
        }
        if (!StringUtils.hasText(user.getPassword())) {
            return Result.error("密码不能为空");
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername())
                .eq(User::getRole, "admin");
        User loginUser = userMapper.selectOne(wrapper);
        if (loginUser == null) {
            return Result.error("账号或密码错误");
        }
        if (!user.getPassword().equals(loginUser.getPassword())) {
            return Result.error("账号或密码错误");
        }
        loginUser.setPassword(null);
        return Result.success(loginUser);
    }

    // ===== 用户管理 =====

    @Override
    public Result<List<User>> getUserList(String role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(role)) {
            wrapper.eq(User::getRole, role);
        } else {
            wrapper.ne(User::getRole, "student");
        }
        List<User> list = userMapper.selectList(wrapper);
        list.forEach(u -> u.setPassword(null));
        return Result.success(list);
    }

    @Override
    public Result<User> findStudentByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return Result.error("请输入学号");
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username.trim())
               .eq(User::getRole, "student");
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            return Result.error("未找到学号为「" + username + "」的学生，请核对后重试");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @Override
    public Result<Void> saveOrUpdateUser(User user) {
        if (!StringUtils.hasText(user.getUsername())) {
            return Result.error("账号不能为空");
        }
        if (user.getId() == null) {
            String defaultPwd = user.getUsername().length() >= 6
                    ? user.getUsername().substring(user.getUsername().length() - 6)
                    : user.getUsername();
            user.setPassword(DigestUtils.md5DigestAsHex(defaultPwd.getBytes()));
            userMapper.insert(user);
        } else {
            user.setPassword(null);
            userMapper.updateById(user);
        }
        return Result.success();
    }

    @Override
    public Result<Void> deleteUser(Long id) {
        userMapper.deleteById(id);
        return Result.success();
    }

    // ===== 时间配置 =====

    @Override
    public Result<TimeConfig> getTimeConfig() {
        List<TimeConfig> list = timeConfigMapper.selectList(null);
        return Result.success(list.isEmpty() ? new TimeConfig() : list.get(0));
    }

    @Override
    public Result<Void> saveTimeConfig(TimeConfig timeConfig) {
        if (timeConfig.getId() == null) {
            timeConfigMapper.insert(timeConfig);
        } else {
            timeConfigMapper.updateById(timeConfig);
        }
        return Result.success();
    }

    // ===== 值班管理 =====

    @Override
    public Result<List<Duty>> getDutyList() {
        return Result.success(dutyMapper.selectList(null));
    }

    @Override
    public Result<Page<DutyVO>> getDutyPage(Integer pageNum, Integer pageSize) {
        Page<DutyVO> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Duty> wrapper = new QueryWrapper<>();
        IPage<DutyVO> voPage = dutyMapper.selectDutyVOPage(page, wrapper);
        return Result.success((Page<DutyVO>) voPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> saveDuty(Duty duty) {
        if (duty.getUserId() == null) return Result.error("请选择人员");
        if (duty.getDutyDate() == null) return Result.error("请选择值班日期");
        if (duty.getStartTime() == null) return Result.error("请选择开始时间");

        TimeConfig config = timeConfigMapper.selectList(null).stream().findFirst().orElse(null);
        if (config == null) return Result.error("系统未配置时段规则，请先配置时间参数");

        LocalTime limitStart = LocalTime.of(config.getDailyStartHour(), 0);
        LocalTime limitEnd   = LocalTime.of(config.getDailyEndHour(), 0);
        int durationMin      = config.getSingleDuration();

        LocalTime userStart = duty.getStartTime().truncatedTo(ChronoUnit.MINUTES);
        duty.setStartTime(userStart);
        if (userStart.isBefore(limitStart) || userStart.isAfter(limitEnd)) {
            return Result.error("开始时间超出有效范围，有效时段：" + limitStart + "~" + limitEnd);
        }

        LocalTime realEnd = userStart.plusMinutes(durationMin);
        if (realEnd.isAfter(limitEnd)) {
            return Result.error("结束时间（" + realEnd + "）超出每日截止时间（" + limitEnd + "），请提前开始时间");
        }
        duty.setEndTime(realEnd);

        User user = userMapper.selectById(duty.getUserId());
        if (user == null) return Result.error("所选用户不存在");

        if (duty.getId() != null) {
            LambdaQueryWrapper<Duty> overlapCheck = new LambdaQueryWrapper<>();
            overlapCheck.eq(Duty::getUserId, duty.getUserId())
                        .eq(Duty::getDutyDate, duty.getDutyDate())
                        .lt(Duty::getStartTime, realEnd)
                        .gt(Duty::getEndTime, userStart)
                        .ne(Duty::getId, duty.getId());
            if (dutyMapper.selectCount(overlapCheck) > 0) {
                return Result.error("该人员在所选时间段已有排班（时间冲突），请调整后重试");
            }
            dutyMapper.updateById(duty);
            return Result.success();
        }

        if ("visitor".equals(user.getRole())) {
            LambdaQueryWrapper<Duty> overlapCheck = new LambdaQueryWrapper<>();
            overlapCheck.eq(Duty::getUserId, duty.getUserId())
                        .eq(Duty::getDutyDate, duty.getDutyDate())
                        .lt(Duty::getStartTime, realEnd)
                        .gt(Duty::getEndTime, userStart);
            if (dutyMapper.selectCount(overlapCheck) > 0) {
                return Result.error("该初访员在所选时间段已有排班（时间冲突），请调整后重试");
            }
            duty.setStatus("0");
            dutyMapper.insert(duty);
            return Result.success();
        }

        List<LocalDate> eightWeekDates = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            eightWeekDates.add(duty.getDutyDate().plusWeeks(i));
        }
        LambdaQueryWrapper<Duty> overlapCheck = new LambdaQueryWrapper<>();
        overlapCheck.eq(Duty::getUserId, duty.getUserId())
                    .in(Duty::getDutyDate, eightWeekDates)
                    .lt(Duty::getStartTime, realEnd)
                    .gt(Duty::getEndTime, userStart);
        if (dutyMapper.selectCount(overlapCheck) > 0) {
            return Result.error("该咨询师在未来 8 周中存在时间段冲突，请调整后重试");
        }
        for (LocalDate date : eightWeekDates) {
            Duty item = new Duty();
            item.setUserId(duty.getUserId());
            item.setDutyDate(date);
            item.setStartTime(userStart);
            item.setEndTime(realEnd);
            item.setMaxPerson(duty.getMaxPerson());
            item.setStatus("0");
            dutyMapper.insert(item);
        }
        return Result.success();
    }

    @Override
    public Result<Void> deleteDuty(Long id) {
        dutyMapper.deleteById(id);
        return Result.success();
    }

    @Override
    public Result<List<Duty>> autoGenerate(Long userId, String dutyDate) {
        TimeConfig cfg = timeConfigMapper.selectList(null).stream().findFirst().orElse(null);
        if (cfg == null) return Result.error("请先配置时间规则");

        int duration  = cfg.getSingleDuration();
        int interval  = cfg.getIntervalMinute();
        int startHour = cfg.getDailyStartHour();
        int endHour   = cfg.getDailyEndHour();

        LambdaQueryWrapper<Duty> check = new LambdaQueryWrapper<>();
        check.eq(Duty::getUserId, userId)
             .eq(Duty::getDutyDate, LocalDate.parse(dutyDate));
        if (dutyMapper.selectCount(check) > 0) {
            return Result.error("该老师当天已有排班，请先删除后再生成");
        }

        List<Duty> result = new ArrayList<>();
        LocalTime current = LocalTime.of(startHour, 0);
        LocalTime limit   = LocalTime.of(endHour, 0);

        while (true) {
            LocalTime endTime = current.plusMinutes(duration);
            if (endTime.isAfter(limit)) break;

            Duty duty = new Duty();
            duty.setUserId(userId);
            duty.setDutyDate(LocalDate.parse(dutyDate));
            duty.setStartTime(current);
            duty.setEndTime(endTime);
            duty.setMaxPerson(3);
            duty.setStatus("0");
            duty.setRemark("自动生成");
            dutyMapper.insert(duty);
            result.add(duty);

            current = endTime.plusMinutes(interval);
        }

        return result.isEmpty()
                ? Result.error("当前时间配置无法生成任何时间段，请检查配置")
                : Result.success(result);
    }

    // ===== 初访预约（联表分页）=====

    @Override
    public IPage<FirstVisitVO> getVisitVOPage(Integer pageNum, Integer pageSize,
                                               String status, String studentName) {
        Page<FirstVisitVO> page = new Page<>(pageNum, pageSize);
        return firstVisitMapper.selectVisitWithVisitor(page, status, studentName);
    }

    @Override
    public Result<Void> auditVisit(FirstVisit firstVisit) {
        if (firstVisit.getId() == null) return Result.error("ID 不能为空");

        if ("已通过".equals(firstVisit.getStatus())) {
            String err = validateVisitorDuty(
                    firstVisit.getVisitorId(),
                    firstVisit.getVisitDate(),
                    firstVisit.getVisitTime());
            if (err != null) return Result.error(err);

            String maxErr = validateMaxPerson(
                    firstVisit.getVisitorId(),
                    firstVisit.getVisitDate(),
                    firstVisit.getVisitTime(),
                    firstVisit.getId());
            if (maxErr != null) return Result.error(maxErr);
        }

        firstVisit.setAuditTime(LocalDateTime.now());
        firstVisitMapper.updateById(firstVisit);

        // ★ 审核结果通知：重新查完整记录，确保 studentId / location 等字段完整
        FirstVisit full = firstVisitMapper.selectById(firstVisit.getId());
        if (full != null && full.getStudentId() != null) {

            if ("已通过".equals(full.getStatus())) {
                String title;
                String content;
                boolean modified = Boolean.TRUE.equals(firstVisit.getIsModified());

                if (full.getVisitDate() != null
                        && StringUtils.hasText(full.getVisitTime())
                        && StringUtils.hasText(full.getLocation())) {

                    if (modified) {
                        // 管理员修改了学生原始申请的时间/地点
                        title   = "初访预约已安排（时间或地点有调整）";
                        content = String.format(
                                "您的初访预约申请已通过审核，管理员对预约安排做了调整，"
                              + "请以最新安排为准准时到场：\n\n"
                              + "预约日期：%s\n预约时段：%s\n预约地点：%s\n\n"
                              + "如有疑问请联系心理中心。",
                                full.getVisitDate(), full.getVisitTime(), full.getLocation());
                    } else {
                        // 管理员直接确认了学生原始申请
                        title   = "初访预约审核通过";
                        content = String.format(
                                "您的初访预约申请已通过审核，请按以下安排准时到场：\n\n"
                              + "预约日期：%s\n预约时段：%s\n预约地点：%s\n\n"
                              + "如有变动请及时联系心理中心。",
                                full.getVisitDate(), full.getVisitTime(), full.getLocation());
                    }
                } else {
                    title   = "初访预约审核通过";
                    content = "您的初访预约申请已通过审核，请注意查看具体安排，如有疑问请联系心理中心。";
                }
                sendNotice(full.getStudentId(), title, content);

            } else if ("已拒绝".equals(full.getStatus())) {
                // 将前端传入的拒绝原因拼入通知正文
                String remark = StringUtils.hasText(firstVisit.getRejectReason())
                        ? "\n\n拒绝原因：" + firstVisit.getRejectReason() : "";
                sendNotice(full.getStudentId(), "初访预约申请未通过",
                        "您好，您的初访预约申请暂未通过审核" + remark
                      + "\n\n如有疑问请联系心理中心工作人员。");
            }
        }

        return Result.success();
    }

    @Override
    public Result<Void> markEmergency(FirstVisit firstVisit) {
        if (firstVisit.getId() == null) return Result.error("ID 不能为空");
        firstVisit.setIsEmergency(true);
        firstVisitMapper.updateById(firstVisit);
        return Result.success();
    }

    // ===== 初访预约记录管理 =====

    @Override
    public Result<Void> addVisit(FirstVisit firstVisit) {
        String err = validateVisitorDuty(
                firstVisit.getVisitorId(),
                firstVisit.getVisitDate(),
                firstVisit.getVisitTime());
        if (err != null) return Result.error(err);

        String maxErr = validateMaxPerson(
                firstVisit.getVisitorId(),
                firstVisit.getVisitDate(),
                firstVisit.getVisitTime(),
                null);
        if (maxErr != null) return Result.error(maxErr);

        firstVisit.setApplyTime(LocalDateTime.now());
        firstVisit.setCreateTime(LocalDateTime.now());
        if (!StringUtils.hasText(firstVisit.getStatus())) {
            firstVisit.setStatus("待审核");
        }
        firstVisitMapper.insert(firstVisit);

        Result<Void> res = Result.success();
        res.setMsg("初访安排成功！");
        return res;
    }

    @Override
    public Result<Void> rescheduleVisit(FirstVisit firstVisit) {
        if (firstVisit.getId() == null) return Result.error("ID 不能为空");

        String err = validateVisitorDuty(
                firstVisit.getVisitorId(),
                firstVisit.getVisitDate(),
                firstVisit.getVisitTime());
        if (err != null) return Result.error(err);

        String maxErr = validateMaxPerson(
                firstVisit.getVisitorId(),
                firstVisit.getVisitDate(),
                firstVisit.getVisitTime(),
                firstVisit.getId());
        if (maxErr != null) return Result.error(maxErr);

        firstVisit.setRescheduleTime(LocalDateTime.now());
        firstVisitMapper.updateById(firstVisit);

        // ★ 改期通知：重新查完整记录确保字段齐全
        FirstVisit full = firstVisitMapper.selectById(firstVisit.getId());
        if (full != null && full.getStudentId() != null
                && full.getVisitDate() != null
                && StringUtils.hasText(full.getVisitTime())
                && StringUtils.hasText(full.getLocation())) {
            String content = String.format(
                    "您的初访预约时间已被调整，请注意以下新安排：\n\n"
                  + "预约日期：%s\n预约时段：%s\n预约地点：%s\n\n"
                  + "如有疑问请联系心理中心工作人员。",
                    full.getVisitDate(), full.getVisitTime(), full.getLocation());
            sendNotice(full.getStudentId(), "初访预约时间已调整", content);
        }

        return Result.success();
    }

    @Override
    public Result<List<User>> getTodayDutyVisitors() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<Duty> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(Duty::getDutyDate, today);
        List<Duty> dutyList = dutyMapper.selectList(wrapper);

        List<Long> userIds = dutyList.stream()
                .map(Duty::getUserId).distinct().collect(Collectors.toList());
        if (userIds.isEmpty()) return Result.success(Collections.emptyList());

        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.in(User::getId, userIds).eq(User::getRole, "visitor");
        List<User> userList = userMapper.selectList(userWrapper);
        userList.forEach(u -> u.setPassword(null));
        return Result.success(userList);
    }

    @Override
    public Result<List<Duty>> getUserDutyDate(Long userId) {
        LambdaQueryWrapper<Duty> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Duty::getUserId, userId)
               .ge(Duty::getDutyDate, LocalDate.now())
               .orderByAsc(Duty::getDutyDate)
               .orderByAsc(Duty::getStartTime);
        List<Duty> list = dutyMapper.selectList(wrapper);

        for (Duty duty : list) {
            if (duty.getStartTime() == null || duty.getEndTime() == null) {
                duty.setBookedCount(0);
                continue;
            }
            String timeSlot = duty.getStartTime().truncatedTo(ChronoUnit.MINUTES).toString()
                            + "-"
                            + duty.getEndTime().truncatedTo(ChronoUnit.MINUTES).toString();
            LambdaQueryWrapper<FirstVisit> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(FirstVisit::getVisitorId, userId)
                        .eq(FirstVisit::getVisitDate, duty.getDutyDate())
                        .eq(FirstVisit::getVisitTime, timeSlot)
                        .eq(FirstVisit::getStatus, "已通过");
            Long count = firstVisitMapper.selectCount(countWrapper);
            duty.setBookedCount(count.intValue());
        }

        return Result.success(list);
    }

    @Override
    public Result<Map<String, Long>> getVisitStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("pending",   firstVisitMapper.selectCount(
                new LambdaQueryWrapper<FirstVisit>().eq(FirstVisit::getStatus, "待审核")));
        stats.put("passed",    firstVisitMapper.selectCount(
                new LambdaQueryWrapper<FirstVisit>().eq(FirstVisit::getStatus, "已通过")));
        stats.put("alert",     firstVisitMapper.selectCount(
                new LambdaQueryWrapper<FirstVisit>().eq(FirstVisit::getIsAlert, true)));
        stats.put("emergency", firstVisitMapper.selectCount(
                new LambdaQueryWrapper<FirstVisit>().eq(FirstVisit::getIsEmergency, true)));
        return Result.success(stats);
    }

    @Override
    public Result<Void> cancelVisit(Long id) {
        // ★ 取消前先查学生信息，用于发通知
        FirstVisit full = firstVisitMapper.selectById(id);

        FirstVisit visit = new FirstVisit();
        visit.setId(id);
        visit.setStatus("cancelled");
        firstVisitMapper.updateById(visit);

        // ★ 取消通知
        if (full != null && full.getStudentId() != null) {
            sendNotice(full.getStudentId(), "初访预约已取消",
                    "您好，您的初访预约已被取消，如有疑问请联系心理中心工作人员。");
        }

        return Result.success();
    }

    // ===== 追加咨询审批 =====

    @Override
    public Result<List<ExtraApplyVO>> getExtraApplyList() {
        List<ExtraApply> applies = extraApplyMapper.selectList(null);
        if (applies.isEmpty()) return Result.success(new ArrayList<>());

        List<Long> studentIds = applies.stream()
                .map(ExtraApply::getStudentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Long> counselorIds = applies.stream()
                .map(ExtraApply::getCounselorId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> studentNameMap  = new HashMap<>();
        Map<Long, String> counselorNameMap = new HashMap<>();

        if (!studentIds.isEmpty()) {
            LambdaQueryWrapper<User> sw = new LambdaQueryWrapper<>();
            sw.in(User::getId, studentIds);
            userMapper.selectList(sw).forEach(u -> studentNameMap.put(u.getId(), u.getName()));
        }
        if (!counselorIds.isEmpty()) {
            LambdaQueryWrapper<User> cw = new LambdaQueryWrapper<>();
            cw.in(User::getId, counselorIds);
            userMapper.selectList(cw).forEach(u -> counselorNameMap.put(u.getId(), u.getName()));
        }

        List<ExtraApplyVO> voList = applies.stream().map(apply -> {
            ExtraApplyVO vo = new ExtraApplyVO();
            BeanUtils.copyProperties(apply, vo);
            vo.setStudentName(studentNameMap.get(apply.getStudentId()));
            vo.setCounselorName(counselorNameMap.get(apply.getCounselorId()));
            return vo;
        }).collect(Collectors.toList());

        return Result.success(voList);
    }

    @Override
    public Result<Void> auditExtra(ExtraApply extraApply) {
        if (extraApply.getId() == null) return Result.error("ID 不能为空");
        extraApply.setAuditTime(LocalDateTime.now());
        extraApplyMapper.updateById(extraApply);

        if ("已通过".equals(extraApply.getStatus())) {
            String remark = StringUtils.hasText(extraApply.getAdminRemark())
                    ? "（备注：" + extraApply.getAdminRemark() + "）" : "";
            sendNotice(extraApply.getCounselorId(), "追加咨询申请已通过",
                    "您提交的追加咨询申请已审批通过，可继续安排咨询。" + remark);
            sendNotice(extraApply.getStudentId(), "咨询追加申请已通过",
                    "您的咨询师已获批继续为您提供咨询服务。" + remark);
        } else if ("已拒绝".equals(extraApply.getStatus())) {
            String remark = StringUtils.hasText(extraApply.getAdminRemark())
                    ? "，原因：" + extraApply.getAdminRemark() : "";
            sendNotice(extraApply.getCounselorId(), "追加咨询申请已拒绝",
                    "您提交的追加咨询申请未能通过审批" + remark + "，如有疑问请联系管理员。");
        }
        return Result.success();
    }

    // ===== 统计分析 =====

    /**
     * 基于 ClosingReport 表，支持按学生 / 咨询师 / 问题类型三个维度汇总。
     * ★ 咨询师维度新增 totalMinutes（总咨询时长），依据 TimeConfig.singleDuration 计算。
     */
    @Override
    public Result<List<Map<String, Object>>> getStatSummary(String type, String startDate, String endDate) {

        LambdaQueryWrapper<ClosingReport> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(startDate)) wrapper.ge(ClosingReport::getEndDate, startDate);
        if (StringUtils.hasText(endDate))   wrapper.le(ClosingReport::getEndDate, endDate);
        List<ClosingReport> reports = closingReportMapper.selectList(wrapper);

        // ① 按学生汇总
        if ("student".equals(type)) {
            List<Long> sids = reports.stream()
                    .map(ClosingReport::getStudentId).filter(Objects::nonNull)
                    .distinct().collect(Collectors.toList());
            Map<Long, String> studentNoMap = new HashMap<>();
            if (!sids.isEmpty()) {
                LambdaQueryWrapper<User> uw = new LambdaQueryWrapper<>();
                uw.in(User::getId, sids);
                userMapper.selectList(uw).forEach(u -> studentNoMap.put(u.getId(), u.getUsername()));
            }

            Map<Long, List<ClosingReport>> grouped = reports.stream()
                    .filter(r -> r.getStudentId() != null)
                    .collect(Collectors.groupingBy(ClosingReport::getStudentId));

            List<Map<String, Object>> result = new ArrayList<>();
            for (Map.Entry<Long, List<ClosingReport>> e : grouped.entrySet()) {
                Long sid     = e.getKey();
                List<ClosingReport> rList = e.getValue();
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("studentId",   sid);
                row.put("studentNo",   studentNoMap.getOrDefault(sid, ""));
                row.put("studentName", rList.get(0).getStudentName());
                row.put("totalTimes",  rList.stream()
                        .mapToInt(r -> r.getTotalTimes() != null ? r.getTotalTimes() : 0).sum());
                row.put("firstDate",   rList.stream()
                        .map(ClosingReport::getStartDate).filter(Objects::nonNull)
                        .min(Comparator.naturalOrder())
                        .map(Object::toString).orElse(""));
                result.add(row);
            }
            result.sort(Comparator.comparing(m -> String.valueOf(m.getOrDefault("studentName", ""))));
            return Result.success(result);
        }

        // ② 按咨询师汇总：★ 新增 totalMinutes
        if ("counselor".equals(type)) {
            // 取单次时长（分钟），用于计算总时长
            TimeConfig cfg = timeConfigMapper.selectList(null).stream().findFirst().orElse(null);
            int minPerSession = (cfg != null && cfg.getSingleDuration() != null)
                    ? cfg.getSingleDuration() : 50;

            Map<Long, List<ClosingReport>> grouped = reports.stream()
                    .filter(r -> r.getCounselorId() != null)
                    .collect(Collectors.groupingBy(ClosingReport::getCounselorId));

            List<Map<String, Object>> result = new ArrayList<>();
            for (List<ClosingReport> rList : grouped.values()) {
                int totalTimes = rList.stream()
                        .mapToInt(r -> r.getTotalTimes() != null ? r.getTotalTimes() : 0).sum();
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("counselorName",  rList.get(0).getCounselorName());
                row.put("studentCount",   rList.stream().map(ClosingReport::getStudentId)
                        .filter(Objects::nonNull).distinct().count());
                row.put("totalTimes",     totalTimes);
                // ★ NEW: 总咨询时长（分钟）= 总次数 × 单次时长
                row.put("totalMinutes",   (long) totalTimes * minPerSession);
                result.add(row);
            }
            result.sort(Comparator.comparing(m -> String.valueOf(m.getOrDefault("counselorName", ""))));
            return Result.success(result);
        }

        // ③ 按问题类型汇总
        if ("problem".equals(type)) {
            Map<String, Long> counted = reports.stream()
                    .collect(Collectors.groupingBy(
                            r -> r.getProblemType() != null ? r.getProblemType() : "未分类",
                            Collectors.counting()));
            List<Map<String, Object>> result = counted.entrySet().stream()
                    .map(e -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("problemType", e.getKey());
                        row.put("count",       e.getValue());
                        return row;
                    })
                    .sorted((a, b) -> Long.compare(
                            ((Long) b.get("count")),
                            ((Long) a.get("count"))))
                    .collect(Collectors.toList());
            return Result.success(result);
        }

        return Result.success(new ArrayList<>());
    }

    /**
     * 将统计数据导出为 Excel（.xlsx）并写入 response 输出流。
     * 咨询师维度含"总咨询时长（分钟）"列。
     */
    @Override
    public void exportStatExcel(String type, String startDate, String endDate,
                                HttpServletResponse response) {

        Result<List<Map<String, Object>>> statResult = getStatSummary(type, startDate, endDate);
        List<Map<String, Object>> data = statResult.getData();

        String   typeLabel;
        String[] headers;
        String[] keys;
        int[]    colWidths;

        if ("student".equals(type)) {
            typeLabel = "学生";
            headers   = new String[]{"学号",       "学生姓名",     "咨询次数",    "首次咨询日期"};
            keys      = new String[]{"studentNo", "studentName", "totalTimes", "firstDate"};
            colWidths = new int[]{4000, 4500, 3200, 4200};
        } else if ("counselor".equals(type)) {
            typeLabel = "咨询师";
            // totalMinutes 已由 getStatSummary 填充，此处无需重复计算
            headers   = new String[]{"咨询师姓名",      "咨询学生数",    "总咨询次数",   "总咨询时长（分钟）"};
            keys      = new String[]{"counselorName", "studentCount", "totalTimes", "totalMinutes"};
            colWidths = new int[]{4500, 3500, 3500, 4500};
        } else if ("problem".equals(type)) {
            typeLabel = "问题类型";
            headers   = new String[]{"问题类型",     "人次"};
            keys      = new String[]{"problemType", "count"};
            colWidths = new int[]{6000, 3000};
        } else {
            typeLabel = "统计";
            headers   = new String[]{};
            keys      = new String[]{};
            colWidths = new int[]{};
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 13);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(headerStyle);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(dataStyle);

            CellStyle dataCenter = workbook.createCellStyle();
            dataCenter.cloneStyleFrom(dataStyle);
            dataCenter.setAlignment(HorizontalAlignment.CENTER);

            Sheet sheet = workbook.createSheet(typeLabel + "统计");
            for (int i = 0; i < colWidths.length; i++) {
                sheet.setColumnWidth(i, colWidths[i]);
            }

            String rangeLabel = (StringUtils.hasText(startDate) && StringUtils.hasText(endDate))
                    ? "  【" + startDate + " ~ " + endDate + "】" : "";
            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(26);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("心理咨询" + typeLabel + "统计" + rangeLabel);
            titleCell.setCellStyle(titleStyle);
            if (headers.length > 1) {
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, headers.length - 1));
            }

            Row headerRow = sheet.createRow(1);
            headerRow.setHeightInPoints(20);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            if (data != null) {
                int rowNum = 2;
                for (Map<String, Object> item : data) {
                    Row row = sheet.createRow(rowNum++);
                    row.setHeightInPoints(18);
                    for (int i = 0; i < keys.length; i++) {
                        Object val = item.get(keys[i]);
                        Cell cell = row.createCell(i);
                        boolean isNumber = (val instanceof Number);
                        cell.setCellStyle(isNumber ? dataCenter : dataStyle);
                        if (val instanceof Number) {
                            cell.setCellValue(((Number) val).doubleValue());
                        } else {
                            cell.setCellValue(val != null ? val.toString() : "");
                        }
                    }
                }
            }

            String fileName = java.net.URLEncoder.encode(typeLabel + "咨询统计.xlsx", "UTF-8");
            response.setContentType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            workbook.write(response.getOutputStream());
            response.flushBuffer();

        } catch (IOException e) {
            throw new RuntimeException("导出统计 Excel 失败", e);
        }
    }

    // ===== 结案报告批量下载 =====

    /**
     * 按学生姓名、咨询师姓名、问题类型、结案日期区间筛选结案报告，
     * 每份报告生成 Word（.docx）A4 表格，打包为 ZIP 文件输出。
     */
    @Override
    public void batchDownloadReports(String studentName,
                                     String counselorName,
                                     String problemType,
                                     String startDate,
                                     String endDate,
                                     HttpServletResponse response) {

        LambdaQueryWrapper<ClosingReport> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(studentName))   wrapper.like(ClosingReport::getStudentName,   studentName);
        if (StringUtils.hasText(counselorName)) wrapper.like(ClosingReport::getCounselorName, counselorName);
        if (StringUtils.hasText(problemType))   wrapper.like(ClosingReport::getProblemType,   problemType);
        if (StringUtils.hasText(startDate))     wrapper.ge(ClosingReport::getEndDate, startDate);
        if (StringUtils.hasText(endDate))       wrapper.le(ClosingReport::getEndDate, endDate);

        List<ClosingReport> reports = closingReportMapper.selectList(wrapper);

        if (reports == null || reports.isEmpty()) {
            try {
                response.setStatus(204);
                response.getWriter().write("暂无结案报告");
            } catch (IOException ignored) {}
            return;
        }

        // ★ 批量预取学号（username），避免循环内 N 次单条查询
        List<Long> studentIds = reports.stream()
                .map(ClosingReport::getStudentId)
                .filter(Objects::nonNull).distinct()
                .collect(Collectors.toList());
        Map<Long, String> studentNoMap = new HashMap<>();
        if (!studentIds.isEmpty()) {
            LambdaQueryWrapper<User> uw = new LambdaQueryWrapper<>();
            uw.in(User::getId, studentIds);
            userMapper.selectList(uw).forEach(u -> studentNoMap.put(u.getId(), u.getUsername()));
        }

        try {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + java.net.URLEncoder.encode("结案报告.zip", "UTF-8"));
        } catch (Exception e) {
            throw new RuntimeException("响应头设置失败", e);
        }

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            for (ClosingReport report : reports) {
                String studentNo = studentNoMap.getOrDefault(report.getStudentId(),
                        str(report.getStudentId()));
                // ★ 生成 Word（.docx）而非 Excel
                byte[] docxBytes = buildReportDocx(report, studentNo);
                String entryName = sanitizeFileName(report.getStudentName())
                        + "_结案报告_" + report.getId() + ".docx";
                zipOut.putNextEntry(new ZipEntry(entryName));
                zipOut.write(docxBytes);
                zipOut.closeEntry();
            }
            zipOut.finish();
        } catch (IOException e) {
            throw new RuntimeException("ZIP 打包失败", e);
        }
    }

    // ===== 私有工具方法 =====

    private String validateVisitorDuty(Long visitorId, LocalDate visitDate, String visitTime) {
        if (visitorId == null) return "请选择初访员！";
        if (visitDate == null) return "请选择初访日期！";
        if (visitDate.isBefore(LocalDate.now())) return "初访日期不能早于今天！";

        LambdaQueryWrapper<Duty> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Duty::getUserId, visitorId).eq(Duty::getDutyDate, visitDate);
        List<Duty> dutyList = dutyMapper.selectList(wrapper);
        if (dutyList.isEmpty()) {
            return "安排失败：该初访员在 " + visitDate + " 没有值班安排！";
        }
        if (!StringUtils.hasText(visitTime)) return null;

        LocalTime visitStart;
        try {
            String startStr = visitTime.contains("-") ? visitTime.split("-")[0] : visitTime;
            visitStart = LocalTime.parse(startStr.trim()).truncatedTo(ChronoUnit.MINUTES);
        } catch (Exception e) {
            return "初访时间格式不正确：" + visitTime;
        }
        final LocalTime vStart = visitStart;

        boolean inRange = dutyList.stream().anyMatch(d -> {
            if (d.getStartTime() == null || d.getEndTime() == null) return false;
            LocalTime dStart = d.getStartTime().truncatedTo(ChronoUnit.MINUTES);
            LocalTime dEnd   = d.getEndTime().truncatedTo(ChronoUnit.MINUTES);
            return !vStart.isBefore(dStart) && vStart.isBefore(dEnd);
        });
        if (!inRange) {
            return "安排失败：所选时间段不在该初访员当天的值班时段内！";
        }
        return null;
    }

    private String validateMaxPerson(Long visitorId, LocalDate visitDate,
                                     String visitTime, Long excludeId) {
        if (visitorId == null || visitDate == null || !StringUtils.hasText(visitTime)) return null;

        String startStr = visitTime.contains("-") ? visitTime.split("-")[0].trim() : visitTime.trim();
        LocalTime visitStart;
        try {
            visitStart = LocalTime.parse(startStr).truncatedTo(ChronoUnit.MINUTES);
        } catch (Exception e) {
            return null;
        }
        final LocalTime vs = visitStart;

        LambdaQueryWrapper<Duty> dutyWrapper = new LambdaQueryWrapper<>();
        dutyWrapper.eq(Duty::getUserId, visitorId).eq(Duty::getDutyDate, visitDate);
        List<Duty> duties = dutyMapper.selectList(dutyWrapper);

        Duty matchedDuty = duties.stream()
                .filter(d -> d.getStartTime() != null
                        && d.getStartTime().truncatedTo(ChronoUnit.MINUTES).equals(vs))
                .findFirst().orElse(null);

        if (matchedDuty == null || matchedDuty.getMaxPerson() == null) return null;

        LambdaQueryWrapper<FirstVisit> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(FirstVisit::getVisitorId, visitorId)
                    .eq(FirstVisit::getVisitDate, visitDate)
                    .eq(FirstVisit::getVisitTime, visitTime)
                    .eq(FirstVisit::getStatus, "已通过");
        if (excludeId != null) {
            countWrapper.ne(FirstVisit::getId, excludeId);
        }
        long booked = firstVisitMapper.selectCount(countWrapper);

        if (booked >= matchedDuty.getMaxPerson()) {
            return "该时间段预约已满（上限 " + matchedDuty.getMaxPerson()
                    + " 人，当前已有 " + booked + " 人），请选择其他时段";
        }
        return null;
    }

    private void sendNotice(Long userId, String title, String content) {
        if (userId == null) return;
        Notice notice = new Notice();
        notice.setUserId(userId);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setIsRead(false);
        notice.setCreateTime(LocalDateTime.now());
        noticeMapper.insert(notice);
    }

    // ===== 结案报告 Word（DOCX）生成 =====

    /**
     * 为单份结案报告生成 Word（.docx）字节数组，A4 页面，表格布局，适合打印归档。
     * <p>
     * 表格行布局（4 列）：
     * <pre>
     *  行0 | 来访者学号   | 值          | 来访者姓名   | 值
     *  行1 | 来访者性别   | 值          | 来访者院系   | 值
     *  行2 | 来访者联系电话 | 值（跨3列）
     *  行3 | 问题类型     | 值          | 咨询总次数   | 值
     *  行4 | 主要咨询师   | 值          | 咨询日期    | 值
     *  行5 | 结案结论     | 值（跨3列，较高）
     * </pre>
     *
     * @param report    结案报告实体
     * @param studentNo 来访者学号（从 user.username 获取）
     */
    private byte[] buildReportDocx(ClosingReport report, String studentNo) {
        try (XWPFDocument doc = new XWPFDocument()) {

            // ── A4 页面设置 ─────────────────────────────────────────────────
            CTBody ctBody = doc.getDocument().getBody();
            CTSectPr sectPr = ctBody.isSetSectPr() ? ctBody.getSectPr() : ctBody.addNewSectPr();
            CTPageSz pgSz = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
            pgSz.setW(BigInteger.valueOf(11906));   // A4 宽 210mm
            pgSz.setH(BigInteger.valueOf(16838));   // A4 高 297mm
            CTPageMar pgMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
            pgMar.setTop(BigInteger.valueOf(1134));    // ≈ 2 cm
            pgMar.setBottom(BigInteger.valueOf(1134));
            pgMar.setLeft(BigInteger.valueOf(1701));   // ≈ 3 cm（装订边距）
            pgMar.setRight(BigInteger.valueOf(1134));

            // ── 标题段落 ─────────────────────────────────────────────────────
            XWPFParagraph titlePara = doc.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            titlePara.setSpacingAfter(240);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText("心理咨询结案报告");
            titleRun.setBold(true);
            titleRun.setFontSize(20);
            titleRun.setFontFamily("宋体");

            // ── 4 列表格（7 行）──────────────────────────────────────────────
            // 各列宽度（单位 twips，合计 ≈ A4 正文区宽度）
            // 标签列稍窄，值列稍宽
            final int[] CW = {2200, 2020, 2200, 2020};
            final String LABEL_BG = "DCE6F1";  // 淡蓝标签底色

            XWPFTable table = doc.createTable(6, 4);

            // 表格充满页面宽度
            CTTblPr tblPr = table.getCTTbl().addNewTblPr();
            CTTblWidth tblW = tblPr.isSetTblW() ? tblPr.getTblW() : tblPr.addNewTblW();
            tblW.setType(STTblWidth.PCT);
            tblW.setW(BigInteger.valueOf(5000));  // 100%（单位 1/50 percent）

            // 行0：来访者学号 | 值 | 来访者姓名 | 值
            fillDocRow4(table.getRow(0), CW, LABEL_BG,
                    "来访者学号", str(studentNo),
                    "来访者姓名", str(report.getStudentName()));

            // 行1：来访者性别 | 值 | 来访者院系 | 值
            fillDocRow4(table.getRow(1), CW, LABEL_BG,
                    "来访者性别", str(report.getGender()),
                    "来访者院系", str(report.getDepartment()));

            // 行2：来访者联系电话 | 值（跨3列）
            fillDocRow2Merged(table.getRow(2), CW, LABEL_BG,
                    "来访者联系电话", str(report.getPhone()), 480);

            // 行3：问题类型 | 值 | 咨询总次数 | 值
            fillDocRow4(table.getRow(3), CW, LABEL_BG,
                    "问题类型", str(report.getProblemType()),
                    "咨询总次数", str(report.getTotalTimes()));

            // 行4：主要咨询师 | 值 | 咨询日期 | 值
            fillDocRow4(table.getRow(4), CW, LABEL_BG,
                    "主要咨询师", str(report.getCounselorName()),
                    "咨询日期",
                    str(report.getStartDate()) + " ~ " + str(report.getEndDate()));

            // 行5：结案结论 | 值（跨3列，较高）
            fillDocRow2Merged(table.getRow(5), CW, LABEL_BG,
                    "结案结论", str(report.getConclusion()), 2200);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.write(baos);
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("生成结案报告 Word 文件失败", e);
        }
    }

    /**
     * 填充 4 格行：[标签1 | 值1 | 标签2 | 值2]
     */
    private void fillDocRow4(XWPFTableRow row, int[] cw, String labelBg,
                              String lbl1, String val1, String lbl2, String val2) {
        setDocCell(row.getCell(0), lbl1, true,  labelBg, cw[0]);
        setDocCell(row.getCell(1), val1, false, null,    cw[1]);
        setDocCell(row.getCell(2), lbl2, true,  labelBg, cw[2]);
        setDocCell(row.getCell(3), val2, false, null,    cw[3]);
    }

    /**
     * 填充 2 格行：[标签 | 值（水平跨后 3 列）]，并设置行最小高度。
     *
     * @param heightTwips 行高下限（twips；≈20 twips/pt，0 表示不设置）
     */
    private void fillDocRow2Merged(XWPFTableRow row, int[] cw, String labelBg,
                                   String label, String value, int heightTwips) {
        // 标签格（列0）
        setDocCell(row.getCell(0), label, true, labelBg, cw[0]);

        // 值格（列1，跨 cols 1-3）
        XWPFTableCell valCell = row.getCell(1);
        setDocCell(valCell, value, false, null, cw[1] + cw[2] + cw[3]);

        // 设置 gridSpan = 3
        CTTcPr tcPr = valCell.getCTTc().isSetTcPr()
                ? valCell.getCTTc().getTcPr() : valCell.getCTTc().addNewTcPr();
        CTDecimalNumber gs = tcPr.isSetGridSpan() ? tcPr.getGridSpan() : tcPr.addNewGridSpan();
        gs.setVal(BigInteger.valueOf(3));

        // 删除多余的格子（保留列0和列1，删除列2、列3）
        CTRow ctRow = row.getCtRow();
        while (ctRow.sizeOfTcArray() > 2) {
            ctRow.removeTc(ctRow.sizeOfTcArray() - 1);
        }

        // 设置行高下限
        if (heightTwips > 0) {
            CTTrPr trPr = ctRow.isSetTrPr() ? ctRow.getTrPr() : ctRow.addNewTrPr();
            CTHeight h = trPr.sizeOfTrHeightArray() > 0
                    ? trPr.getTrHeightArray(0) : trPr.addNewTrHeight();
            h.setVal(BigInteger.valueOf(heightTwips));
            h.setHRule(STHeightRule.AT_LEAST);
        }
    }

    /**
     * 设置单元格：文字内容、是否粗体、背景色、单元格宽度。
     *
     * @param bgColor 16 进制 RGB（如 "DCE6F1"），null 表示不设背景
     * @param widthTwips 单元格宽度（twips）
     */
    private void setDocCell(XWPFTableCell cell, String text,
                             boolean bold, String bgColor, int widthTwips) {
        // 清除默认段落，添加新段落
        if (!cell.getParagraphs().isEmpty()) {
            cell.removeParagraph(0);
        }
        XWPFParagraph para = cell.addParagraph();
        para.setAlignment(ParagraphAlignment.LEFT);

        XWPFRun run = para.createRun();
        run.setText(text != null ? text : "");
        run.setBold(bold);
        run.setFontSize(11);
        run.setFontFamily("宋体");

        CTTcPr tcPr = cell.getCTTc().isSetTcPr()
                ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();

        // 背景色
        if (bgColor != null) {
            CTShd shd = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd();
            shd.setVal(STShd.CLEAR);
            shd.setColor("auto");
            shd.setFill(bgColor);
        }

        // 内边距（上下 ≈ 1mm，左右 ≈ 2mm）
        CTTcMar tcMar = tcPr.isSetTcMar() ? tcPr.getTcMar() : tcPr.addNewTcMar();
        java.util.function.BiConsumer<CTTblWidth, Integer> setMargin = (m, val) -> {
            m.setType(STTblWidth.DXA);
            m.setW(BigInteger.valueOf(val));
        };
        setMargin.accept(tcMar.isSetTop()    ? tcMar.getTop()    : tcMar.addNewTop(),    55);
        setMargin.accept(tcMar.isSetBottom() ? tcMar.getBottom() : tcMar.addNewBottom(), 55);
        setMargin.accept(tcMar.isSetLeft()   ? tcMar.getLeft()   : tcMar.addNewLeft(),   110);
        setMargin.accept(tcMar.isSetRight()  ? tcMar.getRight()  : tcMar.addNewRight(),  110);

        // 单元格宽度
        CTTblWidth tcW = tcPr.isSetTcW() ? tcPr.getTcW() : tcPr.addNewTcW();
        tcW.setType(STTblWidth.DXA);
        tcW.setW(BigInteger.valueOf(widthTwips));
    }

    /** 统一设置四边细边框（Excel 用） */
    private void setBorder(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private String str(Object val) {
        return val != null ? val.toString() : "";
    }

    private String sanitizeFileName(String name) {
        if (name == null) return "未知";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
