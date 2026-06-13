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
    private final NoticeMapper noticeMapper;
    private final TimeConfigMapper timeConfigMapper;
    private final CounselingMapper counselingMapper;
    private final ClosingReportMapper closingReportMapper;
    private final CounselingRecordMapper counselingRecordMapper;

//自动把对应的 Mapper 接口实例注入进来，让当前 Service 能调用数据库操作。
    public AdminServiceImpl(UserMapper userMapper,
                            FirstVisitMapper firstVisitMapper,
                            DutyMapper dutyMapper,
                            NoticeMapper noticeMapper,
                            TimeConfigMapper timeConfigMapper,
                            CounselingMapper counselingMapper,
                            ClosingReportMapper closingReportMapper,
                            CounselingRecordMapper counselingRecordMapper) {
        this.userMapper = userMapper;
        this.firstVisitMapper = firstVisitMapper;
        this.dutyMapper = dutyMapper;
        this.noticeMapper = noticeMapper;
        this.timeConfigMapper = timeConfigMapper;
        this.counselingMapper = counselingMapper;
        this.closingReportMapper = closingReportMapper;
        this.counselingRecordMapper = counselingRecordMapper;
    }

    // ===== 登录 =======================================================================
//重写了 login 登录方法接收前端传来的账号密码，校验工号、密码
// → 查询数据库管理员账号 →
// 比对密码 →
// 返回登录结果，统一用自定义 Result 封装返回值。
    @Override
    public Result<User> login(User user) {
//        前端传过来的登录表单数据（用户名 / 工号、密码）
        if (!StringUtils.hasText(user.getUsername()) || !user.getUsername().matches("^[0-9]+$")) {
            return Result.error("工号必须为数字");
        }
//        StringUtils.hasText：判断工号非空、非空白字符
//        matches("^[0-9]+$")：正则校验，要求纯数字（工号规则）
//        不满足则直接返回错误提示
        if (!StringUtils.hasText(user.getPassword())) {
            return Result.error("密码不能为空");
        }
//        构建查询条件（MyBatis-Plus 条件构造器）
//        新建一个空的查询条件容器，后续所有查询规则都写在这个 wrapper 里eq() = 等于（对应 SQL =）
//        WHERE username = '前端传的工号' AND role = 'admin'
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername())
                .eq(User::getRole, "admin");
//        selectOne：根据条件查单条数据
//        userMapper就是之前构造器注入的 Mapper，负责和数据库 user 表交互。
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

    // ===== 用户管理 ==============================================================

    @Override
    public Result<List<User>> getUserList(String role) {
        //接收前端传来的角色参数 role，动态拼接查询条件：
        //传了角色：只查询该角色的用户
        //没传角色：查询非学生的所有用户
        //调用数据库查询，得到用户集合
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
//    根据学号精准查询单个学生信息，做参数校验、条件查询、脱敏后返回结果。
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
//        新增 / 编辑用户二合一接口，根据id判断是新增还是修改
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
//            把密码置空，不更新数据库里的原有密码。
//            updateById()：根据主键id更新其他字段。
        }
        return Result.success();
    }

    @Override
    public Result<Void> deleteUser(Long id) {
        userMapper.deleteById(id);
        return Result.success();
    }

    // ===== 时间配置 ======================================================================================

    @Override
    public Result<TimeConfig> getTimeConfig() {
        // selectList(null)：不加任何条件，查询表中所有数据
        List<TimeConfig> list = timeConfigMapper.selectList(null);
        // 表中无数据 → 返回空的 TimeConfig 对象；有数据 → 返回第一条配置
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

    // ===== 值班管理 ==================================================================

    @Override
    public Result<List<Duty>> getDutyList() {
        return Result.success(dutyMapper.selectList(null));
    }

    @Override
    public Result<Page<DutyVO>> getDutyPage(Integer pageNum, Integer pageSize) {
//        pageNum：当前页码
//        pageSize：每页展示条数
//        MyBatis-Plus 分页核心对象，传入页码、页大小，自动帮你处理分页逻辑（分页查询、统计总记录数）。
        Page<DutyVO> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Duty> wrapper = new QueryWrapper<>();
//        空条件构造器，代表不做额外筛选，查询全量数据。
        IPage<DutyVO> voPage = dutyMapper.selectDutyVOPage(page, wrapper);
//        selectDutyVOPage：自定义分页查询方法（不是 MP 自带方法，需要在 Mapper 接口 / XML 中手写 SQL）
//        关联查询、数据封装成 DutyVO（视图实体，专门给前端展示）
//        返回分页结果集 IPage
        return Result.success((Page<DutyVO>) voPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)

//    并根据用户角色（普通人员 / 初访员 / 咨询师）执行不同排班逻辑，
//    自动计算值班结束时间、批量生成多周排班。
    public Result<Void> saveDuty(Duty duty) {
        //        校验值班人员、值班日期、开始时间不能为空，缺失则直接返回错误。
        if (duty.getUserId() == null) return Result.error("请选择人员");
        if (duty.getDutyDate() == null) return Result.error("请选择值班日期");
        if (duty.getStartTime() == null) return Result.error("请选择开始时间");

        //查询系统时间配置表，获取全局值班时段规则；
        TimeConfig config = timeConfigMapper.selectList(null).stream().findFirst().orElse(null);
        if (config == null) return Result.error("系统未配置时段规则，请先配置时间参数");
        LocalTime limitStart = LocalTime.of(config.getDailyStartHour(), 0);
        LocalTime limitEnd   = LocalTime.of(config.getDailyEndHour(), 0);
        int durationMin      = config.getSingleDuration();

        // 获取duty对象中的时分秒时间，类型为 java.time.LocalTime（无时区、仅时分秒）
        // truncatedTo(ChronoUnit.MINUTES);时间截断：将秒、毫秒部分直接置 0，只保留到分钟。
        LocalTime userStart = duty.getStartTime().truncatedTo(ChronoUnit.MINUTES);
        duty.setStartTime(userStart);
        if (userStart.isBefore(limitStart) || userStart.isAfter(limitEnd)) {
            return Result.error("开始时间超出有效范围，有效时段：" + limitStart + "~" + limitEnd);
        }
        // 在起始时间基础上，累加指定分钟数，得到实际结束时间
        LocalTime realEnd = userStart.plusMinutes(durationMin);
        if (realEnd.isAfter(limitEnd)) {
            return Result.error("结束时间（" + realEnd + "）超出每日截止时间（" + limitEnd + "），请提前开始时间");
        }
        duty.setEndTime(realEnd);

        //检验是否重复设置
        User user = userMapper.selectById(duty.getUserId());
        if (user == null) return Result.error("所选用户不存在");
        // 构建查询条件：校验同一用户、同一天，是否存在时间重叠的其他排班
        if (duty.getId() != null) {
            LambdaQueryWrapper<Duty> overlapCheck = new LambdaQueryWrapper<>();
            overlapCheck.eq(Duty::getUserId, duty.getUserId())        // 同一用户
                    .eq(Duty::getDutyDate, duty.getDutyDate())      // 同一天
                    .lt(Duty::getStartTime, realEnd)               // 已有排班开始时间 < 本次结束时间
                    .gt(Duty::getEndTime, userStart)               // 已有排班结束时间 > 本次开始时间
                    .ne(Duty::getId, duty.getId());                // 排除当前正在编辑的这条数据

            if (dutyMapper.selectCount(overlapCheck) > 0) {
                return Result.error("该人员在所选时间段已有排班（时间冲突），请调整后重试");
            }
            dutyMapper.updateById(duty);
            return Result.success();
        }

        if ("visitor".equals(user.getRole())) {
            LambdaQueryWrapper<Duty> overlapCheck = new LambdaQueryWrapper<>();
            overlapCheck.eq(Duty::getUserId, duty.getUserId())       // 同一个人
                    .eq(Duty::getDutyDate, duty.getDutyDate())   // 同一天
                    .lt(Duty::getStartTime, realEnd)              // 已有排班开始 < 本次结束
                    .gt(Duty::getEndTime, userStart);            // 已有排班结束 > 本次开始

            if (dutyMapper.selectCount(overlapCheck) > 0) {
                return Result.error("该初访员在所选时间段已有排班（时间冲突），请调整后重试");
            }
            duty.setStatus("0");
            dutyMapper.insert(duty);
            return Result.success();
        }
//实现一次性批量生成未来 8 周同时间排班，并提前校验 8 周内所有日期是否存在时间冲突，再循环插入数据。
        List<LocalDate> eightWeekDates = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            // 基准日期 累加 i 周
            eightWeekDates.add(duty.getDutyDate().plusWeeks(i));
        }
        LambdaQueryWrapper<Duty> overlapCheck = new LambdaQueryWrapper<>();
        overlapCheck.eq(Duty::getUserId, duty.getUserId())// 1. 同一个用户
                    .in(Duty::getDutyDate, eightWeekDates) // 2.日期落在未来8周范围内
                    .lt(Duty::getStartTime, realEnd) // 3. 已有排班开始时间 < 本次排班结束时间
                    .gt(Duty::getEndTime, userStart);// 4. 已有排班结束时间 > 本次排班开始时间
        if (dutyMapper.selectCount(overlapCheck) > 0) {
            return Result.error("该咨询师在未来 8 周中存在时间段冲突，请调整后重试");
        }
//   循环遍历 8 周日期，逐行创建排班对象并单条插入数据库，每条记录仅dutyDate不同，其余字段复用统一值
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
        // 查询时间配置表第一条规则
        TimeConfig cfg = timeConfigMapper.selectList(null).stream().findFirst().orElse(null);
        if (cfg == null) return Result.error("请先配置时间规则");
        // 解析配置参数：单段时长、间隔分钟、每日开始/结束小时
        int duration  = cfg.getSingleDuration();
        int interval  = cfg.getIntervalMinute();
        int startHour = cfg.getDailyStartHour();
        int endHour   = cfg.getDailyEndHour();

        // 校验：该用户当天是否已存在排班
        LambdaQueryWrapper<Duty> check = new LambdaQueryWrapper<>();
        check.eq(Duty::getUserId, userId)
             .eq(Duty::getDutyDate, LocalDate.parse(dutyDate));
        if (dutyMapper.selectCount(check) > 0) {
            return Result.error("该老师当天已有排班，请先删除后再生成");
        }

        List<Duty> result = new ArrayList<>();
        // 初始化当日排班起始时间（整点）
        LocalTime current = LocalTime.of(startHour, 0);
        // 当日排班最晚截止时间（整点）
        LocalTime limit   = LocalTime.of(endHour, 0);
        // 循环生成排班
//        假设 startHour=8、endHour=12、duration=30、interval=10
//        08:00 ~ 08:30
//        08:40 ~ 09:10
//…… 直到结束时间超过 12:00 停止
        while (true) {
            // 计算当前时段结束时间
            LocalTime endTime = current.plusMinutes(duration);
            if (endTime.isAfter(limit)) break;

            // 封装排班对象并单条插入数据库
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
            // 下一段开始时间 = 当前结束时间 + 间隔分钟
            current = endTime.plusMinutes(interval);
        }

        return result.isEmpty()
                ? Result.error("当前时间配置无法生成任何时间段，请检查配置")
                : Result.success(result);
    }

    // ===== 初访预约（联表分页）========================================================

    @Override
    public IPage<FirstVisitVO> getVisitVOPage(Integer pageNum, Integer pageSize,
                                               String status, String studentName) {
        // 构建分页参数：页码、每页条数
        Page<FirstVisitVO> page = new Page<>(pageNum, pageSize);
        // 调用自定义SQL/XML分页查询，携带分页+筛选条件
        return firstVisitMapper.selectVisitWithVisitor(page, status, studentName);
    }

    @Override
//    auditVisit 审核接口
    /**
     *管理员在后台对学生提交的初访申请做 通过 / 拒绝 操作：
     * 审核通过时，强制校验咨询师排班冲突、时段人数上限；
     * 记录审核时间，更新数据库状态；
     * 根据审核结果、是否修改预约信息，自动给学生推送站内通知。
     * */
    public Result<Void> auditVisit(FirstVisit firstVisit) {
        if (firstVisit.getId() == null) return Result.error("ID 不能为空");

        if ("已通过".equals(firstVisit.getStatus())) {
            //检查排版是否冲突
            String err = validateVisitorDuty(
                    firstVisit.getVisitorId(),
                    firstVisit.getVisitDate(),
                    firstVisit.getVisitTime());
            if (err != null) return Result.error(err);
            //检查是否超过最大人数
            String maxErr = validateMaxPerson(
                    firstVisit.getVisitorId(),
                    firstVisit.getVisitDate(),
                    firstVisit.getVisitTime(),
                    firstVisit.getId());
            if (maxErr != null) return Result.error(maxErr);
        }

//        LocalDateTime.now()：获取当前系统时间
//        给实体设置 auditTime（审核时间），记录这条记录是什么时候被审核的

        firstVisit.setAuditTime(LocalDateTime.now());
        firstVisitMapper.updateById(firstVisit);

        // 审核结果通知：重新查完整记录，确保 studentId / location 等字段完整
        //  ：保证后续发通知时，用到的字段都是数据库里最新、最全的数据。
        FirstVisit full = firstVisitMapper.selectById(firstVisit.getId());
        if (full != null && full.getStudentId() != null) {

            if ("已通过".equals(full.getStatus())) {
                String title;
                String content;
//                判断 isModified（是否修改过预约信息
                boolean modified = Boolean.TRUE.equals(firstVisit.getIsModified());

                if (full.getVisitDate() != null
                        && StringUtils.hasText(full.getVisitTime())
                        && StringUtils.hasText(full.getLocation())) {
//                    full.getVisitDate()：预约日期（非空）；预约时段、预约地点不是 null

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

    // ===== 初访预约记录管理 =================================================================

    @Override
//    入参 FirstVisit firstVisit：前端传过来的初访预约表单数据
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
/**
        调用工具方法 validateVisitorDuty，传入：咨询师 ID、预约日期、预约时段
        功能：校验该咨询师当前时段是否存在排班时间冲突
        方法有冲突就返回错误字符串，无冲突返回 null
        如果拿到错误信息，直接返回错误结果，终止后续新增流程
*/
        firstVisit.setApplyTime(LocalDateTime.now());//设置申请时间，记录用户提交预约的时刻
        firstVisit.setCreateTime(LocalDateTime.now());//设置数据创建时间，记录本条记录入库时间
        if (!StringUtils.hasText(firstVisit.getStatus())) {
            firstVisit.setStatus("待审核");
        }
        firstVisitMapper.insert(firstVisit);

        Result<Void> res = Result.success();
        res.setMsg("初访安排成功！");
        return res;
    }

    @Override
//    改期 / 重新预约方法。
    public Result<Void> rescheduleVisit(FirstVisit firstVisit) {
        if (firstVisit.getId() == null) return Result.error("ID 不能为空");

        String err = validateVisitorDuty(
                firstVisit.getVisitorId(),
                firstVisit.getVisitDate(),
                firstVisit.getVisitTime());
        if (err != null) return Result.error(err);
//调用 validateVisitorDuty：校验新选择的咨询师 + 新日期 + 新时段是否存在排班时间冲突。
//        改的就是这几个字段
        String maxErr = validateMaxPerson(
                firstVisit.getVisitorId(),
                firstVisit.getVisitDate(),
                firstVisit.getVisitTime(),
                firstVisit.getId());
        if (maxErr != null) return Result.error(maxErr);

        firstVisit.setRescheduleTime(LocalDateTime.now());
//        给实体设置 rescheduleTime（改期时间），记录本次调整操作的时间。
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
//    查询今日有排班的咨询师列表
    public Result<List<User>> getTodayDutyVisitors() {
        LocalDate today = LocalDate.now();
//        获取服务器当前系统日期（年月日），作为查询条件
        LambdaQueryWrapper<Duty> wrapper = new LambdaQueryWrapper<>();
/**    创建 MyBatis-Plus 条件构造器，查询规则：
        ge = 大于等于
        筛选 duty_date >= 今日 的所有排班记录（今日及之后排班）
 */
        wrapper.ge(Duty::getDutyDate, today);
        List<Duty> dutyList = dutyMapper.selectList(wrapper);
//执行查询，拿到今日及往后所有排班数据集合。
        /**
         * 使用 Java Stream 流处理：
         * map(Duty::getUserId)：从排班记录中提取所有咨询师 ID
         * distinct()：去重（同一个人多条排班只保留一个 ID）
         * collect(Collectors.toList())：把去重后的 ID 收集成 List<Long>
         * */
        List<Long> userIds = dutyList.stream()
                .map(Duty::getUserId).distinct().collect(Collectors.toList());
//        如果今日及往后没有任何排班（ID 集合为空）
//直接返回成功 + 空集合，前端展示无数据。
        if (userIds.isEmpty()) return Result.success(Collections.emptyList());
/**
 * 构建用户查询条件：
 * in(User::getId, userIds)：用户 ID 在上面提取的排班 ID 列表中
 * eq(User::getRole, "visitor")：用户角色为 咨询师 / 访客
 * 作用：只查「今日有排班 + 角色是咨询师」的用户。
 * */
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.in(User::getId, userIds).eq(User::getRole, "visitor");
        List<User> userList = userMapper.selectList(userWrapper);
        userList.forEach(u -> u.setPassword(null));
        return Result.success(userList);
    }
/**根据用户 ID，查询该用户今日及未来的所有排班，
 * 再逐个统计每个排班时段下已通过的预约人数，
 * 最终返回带实时预约人数的排班列表。*/
    @Override
    public Result<List<Duty>> getUserDutyDate(Long userId) {
        LambdaQueryWrapper<Duty> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Duty::getUserId, userId)//代表 大于等于，只查今天及往后的排班
               .ge(Duty::getDutyDate, LocalDate.now())//先按排班日期升序排序
               .orderByAsc(Duty::getDutyDate)
               .orderByAsc(Duty::getStartTime);
        List<Duty> list = dutyMapper.selectList(wrapper);

        for (Duty duty : list) {
//            遍历每一条排班记录，逐个统计当前时段已预约人数。
//            ：如果排班的开始时间 / 结束时间为空
//            直接把已预约人数 bookedCount 设为 0，跳过后续统计逻辑。
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

//    getVisitStats 预约数据统计接口
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
//先根据 ID 查询整条记录，目的是拿到 studentId（学生 ID），后续发通知使用。
        FirstVisit visit = new FirstVisit();
        visit.setId(id);
//        执行更新操作：仅把这条记录状态改为「已取消」，数据仍保留在库中（逻辑取消）
        visit.setStatus("cancelled");
        firstVisitMapper.updateById(visit);

        // ★ 取消通知
        if (full != null && full.getStudentId() != null) {
            sendNotice(full.getStudentId(), "初访预约已取消",
                    "您好，您的初访预约已被取消，如有疑问请联系心理中心工作人员。");
        }

        return Result.success();
    }

    @Override
    public Result<Void> deleteVisit(Long id) {
        // 先查完整记录，用于发通知
        FirstVisit full = firstVisitMapper.selectById(id);
        if (full == null) return Result.error("记录不存在");

        // 物理删除
        firstVisitMapper.deleteById(id);

        // 已完成的初访记录无需再向学生发送"取消"通知（初访已发生）
        if (full.getStudentId() != null && !"已完成".equals(full.getStatus())) {
            sendNotice(full.getStudentId(), "初访预约已取消",
                    "您好，您的初访预约记录已被管理员删除，如有疑问请联系心理中心工作人员。");
        }

        return Result.success();
    }

    // ===== 统计分析 ==================================================================================

    /**
     * 基于 ClosingReport 表，支持按学生 / 咨询师 / 问题类型三个维度汇总。
     第一，按学生统计：先批量查询学生学号做缓存，再按学生 ID 分组，统计每个人的咨询总次数和首次咨询时间，最后按姓名排序；
     第二，按咨询师统计：先读取系统预设的单次咨询时长，分组后统计咨询师接待的学生数量、总咨询次数，并换算出总服务时长；
     第三，按问题类型统计：直接按问题名称分组计数，统计每类问题的咨询人次，并按人次从高到低排序。
     所有统计都使用 Java Stream 流实现，计算完成后统一格式返回，供给前端页面展示和 Excel 导出使用。
     */
    @Override
//    这是数据统计接口，根据传入的类型和时间范围，
//    对结案报告做三类统计：按学生统计咨询情况、
//    按咨询师统计工作量、按心理问题类型统计分布，
//    结果整理成前端易解析的列表格式返回。
    public Result<List<Map<String, Object>>> getStatSummary(String type, String startDate, String endDate) {

        //        构建查询条件：根据结案日期做范围筛选
        //ge：大于等于（开始日期）
        //le：小于等于（结束日期）
        //查出时间范围内所有结案报告数据，作为后续统计数据源
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
//            使用 Stream groupingBy，把所有报告按学生 ID 分组，一人对应多条结案记录。

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

        // ② 按咨询师汇总：新增 totalMinutes
        //统计每位咨询师的服务学生人数、总咨询次数、总咨询时长，最终整理为前端可用的数据格式。
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
//                        把该类型对应的咨询人次存入 count 字段
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
     * Apache POI
     * 这个方法是心理咨询系统统计数据 Excel 导出的核心方法，
     * 支持学生、咨询师、咨询问题类型三类统计数据的 Excel 导出，可搭配时间范围筛选。
     * 首先我会调用已有的统计方法，根据前端传入的类型和时间范围，获取聚合好的统计数据；
     * 接着根据统计类型，动态配置 Excel 的表头、取值字段和列宽，实现一套代码适配多类报表。
     * 然后我提前定义了 4 套统一的单元格样式
     * ，保证全表样式统一；再创建 Excel 工作表，设置列宽，生成合并后的大标题和带样式的表头行。
     * 之后循环遍历统计数据，自动区分文本和数字格式，逐行写入 Excel 单元格；、最后对文件名做编码处理，配置 HTTP 响应头触发浏览器下载，同时捕获 IO 异常保证程序稳定。
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
//            告诉前端 / 浏览器：本次响应的内容是 Excel 2007 及以上格式（.xlsx），不是普通网页。
            response.setContentType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
//            设置下载响应头
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
//            强制浏览器不在线打开，直接弹出下载框，并指定下载后的文件名。
            workbook.write(response.getOutputStream());
//            把内存中创建好的 Excel 文件，写入 HTTP 响应输出流，传给前端。
            response.flushBuffer();

        } catch (IOException e) {
            throw new RuntimeException("导出统计 Excel 失败", e);
//            把生成好的 Excel 文件，通过 HTTP 响应流返回给浏览器，触发浏览器文件下载，同时处理编码和异常。
        }
    }

    // ===== 结案报告批量下载 =====

    /**
     这个方法实现结案报告批量打包下载功能。
     首先根据前端传入的姓名、问题类型、时间范围等条件，筛选出对应的结案报告；如果没有数据，直接返回提示。
     我先批量查询所有学生学号并缓存，避免循环查库。
     接着设置响应头，告知浏览器下载 ZIP 压缩包。
     最后遍历每一份报告，调用工具方法单独生成 Word 文档，再把所有 Word 文件打包进 ZIP，完成批量下载。。
     */
    @Override
    public void batchDownloadReports(String studentName,
                                     String counselorName,
                                     String problemType,
                                     String startDate,
                                     String endDate,
                                     HttpServletResponse response) {
        //构造一个多条件查询

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
        //通过 Stream 提取所有不重复的学生 ID；
        //批量查询用户表，构建 学生ID → 学号 的映射集合；
        //核心优化：避免循环中反复单条查库，大幅提升接口性能。
        //
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
//            设置 ZIP 下载响应头声明响应内容为 ZIP 压缩包；
//配置下载头，编码处理中文文件名，防止乱码，触发浏览器下载。
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + java.net.URLEncoder.encode("结案报告.zip", "UTF-8"));
        } catch (Exception e) {
            throw new RuntimeException("响应头设置失败", e);
        }

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            for (ClosingReport report : reports) {
                // 从缓存取学号
                String studentNo = studentNoMap.getOrDefault(report.getStudentId(),
                        str(report.getStudentId()));
                // // 调用方法生成单个Word文档字节数组// 将单个Word写入压缩包
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
//所选人员、日期、时段是否符合值班安排。
    private String validateVisitorDuty(Long visitorId, LocalDate visitDate, String visitTime) {
        if (visitorId == null) return "请选择初访员！";
        if (visitDate == null) return "请选择初访日期！";
        if (visitDate.isBefore(LocalDate.now())) return "初访日期不能早于今天！";
   //查询该人员当日值班记录，通过查询id和时间段看当前人有没有排版
        LambdaQueryWrapper<Duty> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Duty::getUserId, visitorId).eq(Duty::getDutyDate, visitDate);
        List<Duty> dutyList = dutyMapper.selectList(wrapper);
        if (dutyList.isEmpty()) {
            return "安排失败：该初访员在 " + visitDate + " 没有值班安排！";
        }
        if (!StringUtils.hasText(visitTime)) return null;
// 解析预约开始时间
        LocalTime visitStart;
        try {
            String startStr = visitTime.contains("-") ? visitTime.split("-")[0] : visitTime;
            visitStart = LocalTime.parse(startStr.trim()).truncatedTo(ChronoUnit.MINUTES);
        } catch (Exception e) {
            return "初访时间格式不正确：" + visitTime;
        }
        final LocalTime vStart = visitStart;
//核心逻辑：anyMatch：遍历刚才查询的人当天所有值班记录，只要有一条满足条件就返回 true
//        预约开始时间 >= 值班开始时间  并且  预约开始时间 < 值班结束时间
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

    /**
     * 首先传入初访员id和初访日期，初访时间，通过lamda条件构造器,通过查询id和日期，用dutymapp接口查询返回数值
     * */
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
       //遍历当天所有值班记录：
       //过滤出值班开始时间 = 预约开始时间的那条规则
        LambdaQueryWrapper<Duty> dutyWrapper = new LambdaQueryWrapper<>();
        dutyWrapper.eq(Duty::getUserId, visitorId).eq(Duty::getDutyDate, visitDate);
        List<Duty> duties = dutyMapper.selectList(dutyWrapper);

//        把上一步查到的当日所有值班记录转成流式操作
        Duty matchedDuty = duties.stream()
                .filter(d -> d.getStartTime() != null
                        && d.getStartTime().truncatedTo(ChronoUnit.MINUTES).equals(vs))
                .findFirst().orElse(null);

        if (matchedDuty == null || matchedDuty.getMaxPerson() == null) return null;
        //核心逻辑是查询人数的条件是已通过+id+日期+时间
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
//Apache POI
//    HSSF / XSSF：操作 Excel
//    XWPF：操作 .docx 新版 Word（本代码使用）
    /**
     基于 Apache POI 的 XWPF 组件，动态生成 .docx 格式的心理咨询结案报告 Word 文档。
     文档为 A4 纸张、标准页边距，采用表格布局展示来访者信息、咨询信息、结案结论，支持单元格跨列合并、
     自定义背景色、字体样式，最终返回文件字节数组，用于前端下载、打印归档。
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
     * 批量给 4 个单元格统一设置文本、样式、宽度，复用代码、保证排版一致。
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
