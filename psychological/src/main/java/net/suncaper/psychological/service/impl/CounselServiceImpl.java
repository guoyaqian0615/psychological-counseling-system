package net.suncaper.psychological.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.suncaper.psychological.common.Result;
import net.suncaper.psychological.entity.*;
import net.suncaper.psychological.entity.vo.FirstVisitResultVO;
import net.suncaper.psychological.mapper.*;
import net.suncaper.psychological.service.CounselService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CounselServiceImpl implements CounselService {

    @Autowired private UserMapper             userMapper;
    @Autowired private FirstVisitMapper       firstVisitMapper;
    @Autowired private FirstVisitResultMapper firstVisitResultMapper;
    @Autowired private CounselingMapper       counselingMapper;
    @Autowired private CounselingRecordMapper counselingRecordMapper;
    @Autowired private ClosingReportMapper    closingReportMapper;
    @Autowired private DutyMapper             dutyMapper;
    @Autowired private NoticeMapper           noticeMapper;   // ★ 新增

    // ===================== 通知工具方法 =====================

    /**
     * 向指定学生写入一条站内通知。
     *
     * @param studentUserId counseling.student_id（即 user 表主键）
     * @param title         通知标题
     * @param content       通知正文
     */
    private void sendNotice(Long studentUserId, String title, String content) {
        if (studentUserId == null) return;
        Notice notice = new Notice();
        notice.setUserId(studentUserId);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setIsRead(false);
        notice.setCreateTime(LocalDateTime.now());
        noticeMapper.insert(notice);
    }

    // ===================== 登录逻辑 =====================

    @Override
    public Result login(User user) {
        if (user.getUsername() == null || user.getPassword() == null || user.getRole() == null) {
            return Result.error("工号、密码、身份不能为空");
        }
        String username  = user.getUsername().trim();
        String password  = user.getPassword().trim();
        String frontRole = user.getRole().trim();

        if (!username.matches("\\d+")) {
            return Result.error("工号必须为纯数字");
        }

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username).eq("password", password);
        User realUser = userMapper.selectOne(wrapper);

        if (realUser == null)           return Result.error("工号或密码错误");
        if (realUser.getRole() == null) return Result.error("用户角色异常");
        if (!realUser.getRole().equals(frontRole))
            return Result.error("身份选择错误！您的身份是：" + realUser.getRole());

        if (!"visitor".equals(realUser.getRole())
                && !"assistant".equals(realUser.getRole())
                && !"counselor".equals(realUser.getRole())) {
            return Result.error("仅允许初访员、心理助理、咨询师登录");
        }
        return Result.success(realUser);
    }

    // ===================== 初访员 =====================

    @Override
    public List<FirstVisit> getVisitorWaitList(Long visitorId, String visitDate) {
        LambdaQueryWrapper<FirstVisit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FirstVisit::getStatus, "已通过");

        if (visitorId != null) {
            wrapper.eq(FirstVisit::getVisitorId, visitorId);
        } else {
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
        if (firstVisit == null) throw new RuntimeException("初访预约记录不存在");
        if (!"已通过".equals(firstVisit.getStatus()))
            throw new RuntimeException("仅能领取状态为'已通过'的初访任务");
        firstVisit.setVisitorId(visitorId);
        firstVisitMapper.updateById(firstVisit);
    }

    @Override
    public void submitFirstVisitResult(FirstVisitResult result) {
        if (result.getFirstVisitId() == null)
            throw new RuntimeException("必须关联初访预约ID");
        FirstVisit firstVisit = firstVisitMapper.selectById(result.getFirstVisitId());
        if (firstVisit == null)
            throw new RuntimeException("关联的初访预约记录不存在");

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

    /**
     * 助理待安排列表。三个条件必须【同时】满足：
     *   条件1：first_visit.status = '已完成'             （由 SQL selectWaitArrangeVO 保证）
     *   条件2：counseling.status 为 NULL（空）             （'进行中' 和 '已结案' 均排除，只允许 NULL）
     *   条件3：first_visit_result.conclusion = '安排咨询' （Java 层过滤）
     */
    @Override
    public List<FirstVisitResultVO> getWaitArrangeList() {
        List<FirstVisitResultVO> allList = firstVisitResultMapper.selectWaitArrangeVO();
        if (allList == null || allList.isEmpty()) return new ArrayList<>();

        // 条件2：counseling.status 非 NULL/空的学生全部排除：
        //   '进行中' → 正在咨询，不重复安排；
        //   '已结案' → 已完成咨询，不重新安排；
        //   NULL     → 尚未安排，允许出现在待安排列表。
        List<Counseling> allCounselings = counselingMapper.selectList(null);
        Set<Long> excludedStudentIds = allCounselings.stream()
                .filter(c -> c.getStatus() != null && !c.getStatus().isEmpty())
                .map(Counseling::getStudentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return allList.stream()
                // 条件2：counseling.status 必须为 NULL（进行中/已结案 均不出现在待安排列表）
                .filter(vo -> !excludedStudentIds.contains(vo.getStudentId()))
                // 条件3：初访结论必须是"安排咨询"
                .filter(vo -> "安排咨询".equals(vo.getConclusion()))
                .collect(Collectors.toList());
    }

    /**
     * 咨询安排记录列表。
     * 只返回 status = '进行中' 或 '已结案' 的记录：
     *   '进行中' → 正常显示，修改按钮可用；
     *   '已结案' → 正常显示，修改按钮禁用（前端控制）；
     *   NULL     → 属于"待安排"，不出现在此列表（显示在预约列表）。
     */
    @Override
    public List<Counseling> counselList() {
        LambdaQueryWrapper<Counseling> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Counseling::getStatus, "进行中", "已结案")
               .orderByDesc(Counseling::getStartDate);
        List<Counseling> list = counselingMapper.selectList(wrapper);
        if (list == null || list.isEmpty()) return list;

        // 收集所有 studentId（user 表 pk）
        Set<Long> studentPkIds = list.stream()
                .map(Counseling::getStudentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (!studentPkIds.isEmpty()) {
            LambdaQueryWrapper<User> uq = new LambdaQueryWrapper<>();
            uq.in(User::getId, studentPkIds);
            Map<Long, String> pkToUsername = userMapper.selectList(uq).stream()
                    .collect(Collectors.toMap(User::getId, User::getUsername));

            for (Counseling c : list) {
                if (c.getStudentId() != null) {
                    c.setStudentNo(pkToUsername.getOrDefault(c.getStudentId(), ""));
                }
            }
        }
        return list;
    }

    /**
     * 安排咨询。
     *
     * ★ 新增：安排成功后向学生发送站内通知。
     */
    @Override
    public void arrangeCounsel(Counseling counseling) {
        if (counseling.getStartDate() == null)
            throw new RuntimeException("咨询开始日期不能为空");
        if (counseling.getCounselingTime() == null || counseling.getCounselingTime().isEmpty())
            throw new RuntimeException("咨询时间段不能为空");
        if (counseling.getCounselorId() == null)
            throw new RuntimeException("咨询师ID不能为空");
        if (counseling.getStudentId() == null)
            throw new RuntimeException("学生ID不能为空");

        // 解析 counselingTime 格式："2026-06-09 周二 10:30-11:00"
        String[] timeParts = counseling.getCounselingTime().trim().split(" ");
        if (timeParts.length < 3)
            throw new RuntimeException("咨询时间格式错误，期望：yyyy-MM-dd 星期 HH:mm-HH:mm");
        String[] slotParts = timeParts[2].split("-");
        if (slotParts.length < 2)
            throw new RuntimeException("时间段格式错误");

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        LocalDate  dutyDate  = LocalDate.parse(timeParts[0], dateFmt);
        LocalTime  startTime = LocalTime.parse(slotParts[0], timeFmt);
        LocalTime  endTime   = LocalTime.parse(slotParts[1], timeFmt);

        // 查对应 duty 记录，取 max_person
        LambdaQueryWrapper<Duty> dw = new LambdaQueryWrapper<>();
        dw.eq(Duty::getUserId,    counseling.getCounselorId())
                .eq(Duty::getDutyDate,  dutyDate)
                .eq(Duty::getStartTime, startTime)
                .eq(Duty::getEndTime,   endTime);
        Duty matchDuty = dutyMapper.selectOne(dw);
        int  maxPerson = (matchDuty != null && matchDuty.getMaxPerson() != null)
                ? matchDuty.getMaxPerson() : 1;

        // 查找该学生是否已有 status 为空（NULL 或 ""）的旧记录
        // 使用 OR 同时覆盖 NULL 和空字符串两种情况，orderByAsc 取最早一条，避免脏数据多记录时 selectOne 报错
        LambdaQueryWrapper<Counseling> existWrap = new LambdaQueryWrapper<>();
        existWrap.eq(Counseling::getStudentId, counseling.getStudentId())
                 .and(w -> w.isNull(Counseling::getStatus).or().eq(Counseling::getStatus, ""))
                 .orderByAsc(Counseling::getId)
                 .last("LIMIT 1");
        Counseling existingRecord = counselingMapper.selectOne(existWrap);

        // 统计该时段已有预约数
        // 排除已结案（名额已释放）+ 排除该学生自身旧记录（避免把占位空记录重复计入）
        LambdaQueryWrapper<Counseling> cw = new LambdaQueryWrapper<>();
        cw.eq(Counseling::getCounselorId,    counseling.getCounselorId())
                .eq(Counseling::getCounselingTime, counseling.getCounselingTime())
                .ne(Counseling::getStatus,         "已结案");
        if (existingRecord != null) {
            cw.ne(Counseling::getId, existingRecord.getId());
        }
        long alreadyBooked = counselingMapper.selectCount(cw);
        if (alreadyBooked >= maxPerson) {
            throw new RuntimeException(
                    "该时段预约人数已满（已预约 " + alreadyBooked + "/" + maxPerson + " 人），请选择其他时段");
        }

        // ★ 核心修复：在原记录上更新，禁止重复 insert
        counseling.setTotalWeeks(8);
        counseling.setStatus("进行中");
        if (existingRecord != null) {
            // 已有待安排旧记录（status=NULL/""）→ 直接更新，保留原 id，不新建行
            counseling.setId(existingRecord.getId());
            counselingMapper.updateById(counseling);
        } else {
            // 该学生确实没有任何旧记录 → 才允许新增
            counselingMapper.insert(counseling);
        }

        // ★ first_visit.status 保持 '已完成'，不做任何修改。
        //   助理待安排列表通过 getWaitArrangeList() 中的 activeStudentIds 过滤
        //   来隐藏该学生（counseling 记录已存在且非已结案），无需改动 first_visit 表。

        // ★ 发送通知给学生
        String location = counseling.getLocation() != null && !counseling.getLocation().isEmpty()
                ? counseling.getLocation() : "待定";
        String content = String.format(
                "您的咨询已安排，咨询师：%s，咨询时间：%s，咨询地点：%s，共 8 次，请按时参加。",
                counseling.getCounselorName(),
                counseling.getCounselingTime(),
                location);
        sendNotice(counseling.getStudentId(), "咨询已安排", content);
    }

    @Override
    public List<User> getAllCounselor() {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("role", "counselor");
        return userMapper.selectList(wrapper);
    }

    @Override
    public List<User> getCounselorWithDuty() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<Duty> dutyWrapper = new LambdaQueryWrapper<>();
        dutyWrapper.ge(Duty::getDutyDate, today);
        List<Duty> dutyList = dutyMapper.selectList(dutyWrapper);

        if (dutyList == null || dutyList.isEmpty()) return new ArrayList<>();

        List<Long> dutyUserIds = dutyList.stream()
                .map(Duty::getUserId)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getRole, "counselor")
                .in(User::getId, dutyUserIds);
        return userMapper.selectList(userWrapper);
    }

    /**
     * 查询指定咨询师的可选排班循环。
     *
     * 设计思路：
     *   管理员在 duty 表为每位咨询师安排若干组"8次固定时间"，
     *   每组（轨道）由 8 条连续每周记录组成，构成一个排班循环。
     *   本接口返回每个循环的"第一天"供前端下拉框选择。
     *
     * 识别规则：
     *   1. 按（时间段 + 星期）分轨道，同一咨询师可有多条轨道（不同时段/不同星期）。
     *   2. 同一轨道内若出现 > 7 天的间隔，则间隔后的第一条是新循环的起点。
     *   3. 容量检查只在循环起点做：已进行中的预约数 < max_person 才放入下拉框。
     *      · 只统计 status='进行中' 的咨询（已结案释放名额，NULL 状态不计入）。
     */
    @Override
    public Result<Map<String, Object>> getCounselFreeTime(Long counselorId) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalDate today = LocalDate.now();
        String[] WEEK_CN = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};

        // 1. 查询今日及以后的所有值班记录（按日期升序）
        LambdaQueryWrapper<Duty> dutyWrap = new LambdaQueryWrapper<>();
        dutyWrap.eq(Duty::getUserId, counselorId)
                .ge(Duty::getDutyDate, today)
                .orderByAsc(Duty::getDutyDate);
        List<Duty> dutyList = dutyMapper.selectList(dutyWrap);

        // 2. 只统计"进行中"咨询的已占用名额（key = "首次日期|时间段"）
        LambdaQueryWrapper<Counseling> conWrap = new LambdaQueryWrapper<>();
        conWrap.eq(Counseling::getCounselorId, counselorId)
               .eq(Counseling::getStatus, "进行中");
        Map<String, Long> busyCountMap = new HashMap<>();
        for (Counseling c : counselingMapper.selectList(conWrap)) {
            String t = c.getCounselingTime();
            if (t == null || t.isBlank()) continue;
            String[] arr = t.split(" ");
            if (arr.length < 3) continue;
            busyCountMap.merge(arr[0] + "|" + arr[2], 1L, Long::sum);
        }

        // 3. 按（时间段 + 星期）分组，得到独立的排班轨道
        //    key = "HH:mm-HH:mm|dayOfWeek"，保证同天不同时段不混淆
        Map<String, List<Duty>> trackMap = new LinkedHashMap<>();
        for (Duty duty : dutyList) {
            if (duty.getDutyDate() == null || duty.getStartTime() == null || duty.getEndTime() == null) continue;
            String slot = duty.getStartTime().format(timeFormatter) + "-" + duty.getEndTime().format(timeFormatter);
            int dow = duty.getDutyDate().getDayOfWeek().getValue();
            trackMap.computeIfAbsent(slot + "|" + dow, k -> new ArrayList<>()).add(duty);
        }

        // 4. 遍历每条轨道，识别循环起点并检查容量
        List<Map<String, Object>> cycleGroups = new ArrayList<>();
        for (List<Duty> duties : trackMap.values()) {
            // duties 已按日期升序（继承自 dutyList 的排序）
            LocalDate prev = null;
            for (Duty duty : duties) {
                LocalDate curr = duty.getDutyDate();
                // 首条记录或与上一条间隔 > 7 天 → 新循环起点
                boolean isNewCycle = (prev == null
                        || java.time.temporal.ChronoUnit.DAYS.between(prev, curr) > 7);
                if (isNewCycle) {
                    // 只在起点检查容量，避免把循环内后续日期误判为新循环
                    String slot       = duty.getStartTime().format(timeFormatter) + "-" + duty.getEndTime().format(timeFormatter);
                    String startDate  = curr.format(dateFormatter);
                    int    maxPerson  = duty.getMaxPerson() != null ? duty.getMaxPerson() : 1;
                    long   booked     = busyCountMap.getOrDefault(startDate + "|" + slot, 0L);
                    if (booked < maxPerson) {
                        Map<String, Object> cycle = new LinkedHashMap<>();
                        cycle.put("firstDate", startDate);
                        cycle.put("dayLabel",  WEEK_CN[curr.getDayOfWeek().getValue() % 7]);
                        cycle.put("timeSlot",  slot);
                        cycleGroups.add(cycle);
                    }
                }
                prev = curr;
            }
        }
        cycleGroups.sort(Comparator.comparing(m -> (String) m.get("firstDate")));

        Map<String, Object> resMap = new HashMap<>();
        resMap.put("cycleGroups", cycleGroups);
        return Result.success(resMap);
    }

    /**
     * 修改咨询安排。
     *
     * ★ 修复：先查出原记录（保留 studentId 和兜底 counselorName），
     *   updateById 后再发通知，确保通知内容准确且学生 ID 不丢失。
     * ★ 新增：若修改了咨询师或咨询时间，需验证新时段未超出 duty.max_person 限制，
     *   统计时排除当前记录自身（ne id），避免把自己算进已预约人数。
     */
    @Override
    public void updateCounsel(Counseling counseling) {
        // 查出原记录（确保拿到 studentId）
        Counseling existing = counselingMapper.selectById(counseling.getId());
        if (existing == null) throw new RuntimeException("咨询记录不存在");

        // ─── max_person 容量校验 ───────────────────────────────────────────
        String newCounselingTime = counseling.getCounselingTime();
        Long   newCounselorId    = counseling.getCounselorId();

        boolean counselorChanged = newCounselorId != null
                && !newCounselorId.equals(existing.getCounselorId());
        boolean timeChanged = newCounselingTime != null
                && !newCounselingTime.equals(existing.getCounselingTime());

        // 只要咨询师或时间有任一变动，就对新时段做容量检查
        if ((counselorChanged || timeChanged)
                && newCounselingTime != null && !newCounselingTime.isBlank()) {

            Long checkCounselorId = (newCounselorId != null)
                    ? newCounselorId : existing.getCounselorId();

            // 解析 counselingTime："2026-06-09 周二 10:30-11:00"
            String[] timeParts = newCounselingTime.trim().split(" ");
            if (timeParts.length < 3)
                throw new RuntimeException("咨询时间格式错误，期望：yyyy-MM-dd 星期 HH:mm-HH:mm");
            String[] slotParts = timeParts[2].split("-");
            if (slotParts.length < 2)
                throw new RuntimeException("时间段格式错误");

            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
            LocalDate dutyDate  = LocalDate.parse(timeParts[0], dateFmt);
            LocalTime startTime = LocalTime.parse(slotParts[0], timeFmt);
            LocalTime endTime   = LocalTime.parse(slotParts[1], timeFmt);

            // 查对应 duty 记录，取 max_person
            LambdaQueryWrapper<Duty> dw = new LambdaQueryWrapper<>();
            dw.eq(Duty::getUserId,    checkCounselorId)
                    .eq(Duty::getDutyDate,  dutyDate)
                    .eq(Duty::getStartTime, startTime)
                    .eq(Duty::getEndTime,   endTime);
            Duty matchDuty = dutyMapper.selectOne(dw);
            int maxPerson = (matchDuty != null && matchDuty.getMaxPerson() != null)
                    ? matchDuty.getMaxPerson() : 1;

            // 统计该时段已有预约数，排除当前记录自身（避免重复计数）且排除已结案（名额已释放）
            LambdaQueryWrapper<Counseling> cw = new LambdaQueryWrapper<>();
            cw.eq(Counseling::getCounselorId,    checkCounselorId)
                    .eq(Counseling::getCounselingTime, newCounselingTime)
                    .ne(Counseling::getId,             counseling.getId())
                    .ne(Counseling::getStatus,         "已结案");
            long alreadyBooked = counselingMapper.selectCount(cw);
            if (alreadyBooked >= maxPerson) {
                throw new RuntimeException(
                        "该时段预约人数已满（已预约 " + alreadyBooked + "/" + maxPerson
                                + " 人），请选择其他时段");
            }
        }
        // ─────────────────────────────────────────────────────────────────

        // 执行更新（只更新非 null 字段）
        counselingMapper.updateById(counseling);

        // 重新查出更新后的完整记录，用于通知内容
        Counseling updated = counselingMapper.selectById(counseling.getId());

        // ★ 发送通知给学生
        String counselorName = updated.getCounselorName() != null
                ? updated.getCounselorName()
                : existing.getCounselorName();
        String location = updated.getLocation() != null && !updated.getLocation().isEmpty()
                ? updated.getLocation() : "待定";
        String content = String.format(
                "您的咨询安排已更新，咨询师：%s，新咨询时间：%s，咨询地点：%s，请以最新安排为准。",
                counselorName,
                updated.getCounselingTime(),
                location);
        sendNotice(existing.getStudentId(), "咨询安排已更新", content);
    }

    @Override
    public void closeCounsel(Long id) {
        Counseling counseling = counselingMapper.selectById(id);
        if (counseling == null) throw new RuntimeException("咨询记录不存在");
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
        Page<Counseling> resultPage = counselingMapper.selectPage(page, wrapper);

        // 批量填充 sessionCount：一次查出所有记录条数，避免 N+1 查询
        List<Counseling> records = resultPage.getRecords();
        if (records != null && !records.isEmpty()) {
            List<Long> ids = records.stream()
                    .map(Counseling::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            // 查出所有相关咨询记录，按 counseling_id 分组计数
            LambdaQueryWrapper<CounselingRecord> recWrapper = new LambdaQueryWrapper<>();
            recWrapper.in(CounselingRecord::getCounselingId, ids);
            List<CounselingRecord> allRecs = counselingRecordMapper.selectList(recWrapper);
            Map<Long, Long> countMap = allRecs.stream()
                    .collect(Collectors.groupingBy(CounselingRecord::getCounselingId, Collectors.counting()));
            // 回填到每条咨询记录
            records.forEach(c -> c.setSessionCount(
                    countMap.getOrDefault(c.getId(), 0L).intValue()
            ));
        }

        return Result.success(resultPage);
    }

    /**
     * 根据 counseling.student_id（即 user 表 id）查询学生信息
     * 返回 username(学号)、gender、phone、department，供结案报告自动回填
     */
    @Override
    public User getStudentInfo(Long studentId) {
        return userMapper.selectById(studentId);
    }

    @Override
    public void submitRecord(CounselingRecord record) {
        if (record.getTimes() == null)
            throw new RuntimeException("必须填写第几次咨询");
        if (record.getStatus() == null || record.getStatus().isEmpty())
            throw new RuntimeException("必须填写咨询状态");
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