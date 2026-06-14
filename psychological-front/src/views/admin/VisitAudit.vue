<template>
  <div class="visit-audit-page">

    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 20px;">
      <el-col :span="6">
        <div class="stat-card stat-pending">
          <div class="stat-icon-wrap">
            <el-icon size="24"><Clock /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-num">{{ stats.pending }}</div>
            <div class="stat-label">待审核</div>
          </div>
          <div class="stat-bg-circle" />
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-alert">
          <div class="stat-icon-wrap">
            <el-icon size="24"><Warning /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-num">{{ stats.alert }}</div>
            <div class="stat-label">红色报警</div>
          </div>
          <div class="stat-bg-circle" />
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-emergency">
          <div class="stat-icon-wrap">
            <el-icon size="24"><Top /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-num">{{ stats.emergency }}</div>
            <div class="stat-label">紧急优先</div>
          </div>
          <div class="stat-bg-circle" />
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-passed">
          <div class="stat-icon-wrap">
            <el-icon size="24"><CircleCheck /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-num">{{ stats.passed }}</div>
            <div class="stat-label">已通过</div>
          </div>
          <div class="stat-bg-circle" />
        </div>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-left">
        <el-select
          v-model="filterStatus"
          placeholder="审核状态"
          clearable
          class="filter-item"
          @change="pageNum = 1; getList()"
        >
          <el-option label="待审核" value="待审核" />
          <el-option label="已通过" value="已通过" />
          <el-option label="已拒绝" value="已拒绝" />
          <el-option label="已完成" value="已完成" />
        </el-select>

        <el-select
          v-model="filterAlert"
          placeholder="报警/优先级"
          clearable
          class="filter-item"
          @change="pageNum = 1; getList()"
        >
          <el-option label="🔴 仅红色报警" value="alert" />
          <el-option label="⚠️ 仅紧急优先" value="emergency" />
        </el-select>

        <el-input
          v-model="filterName"
          placeholder="搜索学生姓名"
          clearable
          :prefix-icon="Search"
          class="filter-search"
          @input="debounceSearch"
        />

        <el-button @click="resetFilter" :icon="Refresh" class="btn-reset">重置</el-button>
      </div>

      <div class="filter-right">
        <el-tooltip placement="top">
          <template #content>
            <div>
              <b>报警规则说明：</b><br/>
              🔴 红色报警：问卷总分低于 10 分，系统自动标记<br/>
              ⚠️ 紧急优先：管理员手动设置，排队靠前<br/>
              排序优先级：紧急 > 红色报警 > 申请时间
            </div>
          </template>
          <el-button link type="info" :icon="QuestionFilled" class="rule-btn">报警规则</el-button>
        </el-tooltip>
        <el-button type="primary" :icon="Plus" @click="openAddVisit" class="btn-add">
          手动新增预约
        </el-button>
      </div>
    </div>

    <!-- 主表格 -->
    <div class="table-card">
      <el-table
        :data="displayList"
        v-loading="loading"
        row-key="id"
        :row-class-name="getRowClass"
        style="width: 100%"
        :header-cell-style="{ background: '#F8FAFC', color: '#374151', fontWeight: '600', fontSize: '13px' }"
        :row-style="{ height: '54px' }"
      >
        <!-- 排队序号 -->
        <el-table-column type="index" label="#" width="55" align="center">
          <template #default="scope">
            <span :class="{'index-emergency': scope.row.isEmergency, 'index-alert': !scope.row.isEmergency && scope.row.isAlert}">
              {{ (pageNum - 1) * pageSize + scope.$index + 1 }}
            </span>
          </template>
        </el-table-column>

        <!-- 学生信息 — 固定宽度，不再拉伸 -->
        <el-table-column label="学生信息" width="140">
          <template #default="scope">
            <div class="student-cell">
              <div class="student-info">
                <span class="student-name">{{ scope.row.studentName }}</span>
                <span class="student-id" v-if="scope.row.studentNo">{{ scope.row.studentNo }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="applyTime" label="申请时间" width="152">
          <template #default="scope">
            <span class="time-text">{{ formatDateTime(scope.row.applyTime) }}</span>
          </template>
        </el-table-column>

        <!-- 问卷分数 -->
        <el-table-column label="问卷分数" width="100" align="center">
          <template #default="scope">
            <el-tooltip
              :content="`问卷总分：${scope.row.questionnaireScore ?? '-'} 分（满分 100 分，低于 10 分触发红色报警）`"
              placement="top"
            >
              <el-tag
                :type="scoreTagType(scope.row.questionnaireScore)"
                style="font-weight: 700; cursor: default;"
              >
                {{ scope.row.questionnaireScore ?? '-' }} 分
              </el-tag>
            </el-tooltip>
          </template>
        </el-table-column>

        <!-- 报警状态 -->
        <el-table-column label="报警状态" width="115" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.isAlert" type="danger" effect="dark">🔴 红色报警</el-tag>
            <el-tag v-else type="success" effect="plain">正常</el-tag>
          </template>
        </el-table-column>

        <!-- 排队优先级 -->
        <el-table-column label="排队优先" width="110" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.isEmergency" type="warning" effect="dark">⚠️ 紧急</el-tag>
            <el-tag v-else type="info" effect="plain">普通</el-tag>
          </template>
        </el-table-column>

        <!-- 审核状态 -->
        <el-table-column label="审核状态" width="95" align="center">
          <template #default="scope">
            <el-tag :type="statusTagType(scope.row.status)" effect="plain">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>

        <!-- 操作列 -->
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="scope">
            <!-- 待审核：优先 + 审核 + 删除 -->
            <template v-if="scope.row.status === '待审核'">
              <el-tooltip
                :content="scope.row.isEmergency ? '已是紧急优先' : '标记为紧急，排队靠前'"
                placement="top"
              >
                <el-button
                  size="small"
                  type="warning"
                  :icon="Top"
                  :disabled="scope.row.isEmergency"
                  @click="markEmergency(scope.row)"
                >优先</el-button>
              </el-tooltip>
              <el-button
                size="small"
                type="primary"
                :icon="EditPen"
                @click="openAudit(scope.row)"
              >审核</el-button>
              <el-button
                size="small"
                type="danger"
                :icon="Delete"
                @click="deleteVisit(scope.row)"
              >删除</el-button>
            </template>

            <!-- 已通过：改期 + 删除 -->
            <template v-else-if="scope.row.status === '已通过'">
              <el-button
                size="small"
                type="warning"
                :icon="Edit"
                @click="openReschedule(scope.row)"
              >改期</el-button>
              <el-button
                size="small"
                type="danger"
                :icon="Delete"
                @click="deleteVisit(scope.row)"
              >删除</el-button>
            </template>

            <!-- 已完成：仅删除 -->
            <template v-else-if="scope.row.status === '已完成'">
              <el-tooltip content="删除已完成的初访记录（不可恢复）" placement="top">
                <el-button
                  size="small"
                  type="danger"
                  :icon="Delete"
                  @click="deleteVisit(scope.row)"
                >删除</el-button>
              </el-tooltip>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <span class="footer-info">
          共 {{ total }} 条记录
          <span v-if="stats.pending > 0" class="footer-pending">
            · {{ stats.pending }} 条待审核
          </span>
          <span v-if="stats.emergency > 0" class="footer-emergency">
            · {{ stats.emergency }} 条紧急优先
          </span>
        </span>
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="sizes, prev, pager, next"
          @change="getList"
        />
      </div>
    </div>

    <!-- ==================== 审核弹窗（待审核） ==================== -->
    <el-dialog
      v-model="auditShow"
      title="初访预约审核"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <!-- 学生基本信息 -->
      <el-descriptions :column="2" border size="small" style="margin-bottom: 0;">
        <el-descriptions-item label="学生姓名">
          <b>{{ auditForm.studentName }}</b>
        </el-descriptions-item>
        <el-descriptions-item label="申请时间">
          {{ formatDateTime(auditForm.applyTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="问卷分数">
          <el-tag :type="scoreTagType(auditForm.questionnaireScore)" style="font-weight:600;">
            {{ auditForm.questionnaireScore ?? '-' }} 分
          </el-tag>
          <el-tag v-if="auditForm.isAlert" type="danger" effect="dark" style="margin-left:6px;">🔴 已报警</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="优先级">
          <el-tag v-if="auditForm.isEmergency" type="warning" effect="dark">⚠️ 紧急优先</el-tag>
          <el-tag v-else type="info" effect="plain">普通</el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 学生原始申请的预约信息（只读） -->
      <el-divider content-position="left" style="margin: 10px 0 8px;">
        <span style="font-size: 12px; color: #909399;">学生申请的预约信息</span>
      </el-divider>
      <el-descriptions :column="2" border size="small" style="margin-bottom: 16px;">
        <el-descriptions-item label="申请日期">
          <span v-if="auditForm._origVisitDate" style="color:#303133;">{{ auditForm._origVisitDate }}</span>
          <el-text v-else type="info" size="small">未指定</el-text>
        </el-descriptions-item>
        <el-descriptions-item label="申请时段">
          <span v-if="auditForm._origVisitTime" style="color:#303133;">{{ auditForm._origVisitTime }}</span>
          <el-text v-else type="info" size="small">未指定</el-text>
        </el-descriptions-item>
        <el-descriptions-item label="申请地点">
          <span v-if="auditForm._origLocation" style="color:#303133;">{{ auditForm._origLocation }}</span>
          <el-text v-else type="info" size="small">未指定</el-text>
        </el-descriptions-item>
        <el-descriptions-item label="申请初访员">
          <span v-if="auditForm._origVisitorName" style="color:#303133;">{{ auditForm._origVisitorName }}</span>
          <el-text v-else type="info" size="small">未指定</el-text>
        </el-descriptions-item>
      </el-descriptions>

      <el-form :model="auditForm" label-width="100px" ref="auditFormRef">
        <el-form-item label="审核结果" required>
          <el-radio-group v-model="auditForm.status">
            <el-radio value="已通过">
              <el-text type="success" style="font-weight:600;">✔ 通过</el-text>
            </el-radio>
            <el-radio value="已拒绝">
              <el-text type="danger" style="font-weight:600;">✘ 拒绝</el-text>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 通过时：分配初访安排 -->
        <template v-if="auditForm.status === '已通过'">
          <el-divider content-position="left">
            <span style="font-size:13px; color:#606266;">分配初访安排</span>
          </el-divider>

          <el-form-item label="分配初访员" required>
            <el-select
              v-model="auditForm.visitorId"
              placeholder="请选择初访员"
              @change="changeAuditVisitor"
              style="width: 100%"
              filterable
            >
              <el-option
                v-for="v in visitorList"
                :key="v.id"
                :label="v.name"
                :value="v.id"
              >
                <span>{{ v.name }}</span>
                <span style="float:right; color:#c0c4cc; font-size:12px;">{{ v.department }}</span>
              </el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="初访日期" required>
            <el-date-picker
              v-model="auditForm.visitDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择初访日期"
              :disabled-date="disabledAuditDate"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item label="时间段" required>
            <el-select
              v-model="auditForm.visitTime"
              placeholder="请先选择初访日期"
              :disabled="!auditForm.visitDate"
              style="width: 100%"
            >
              <el-option
                v-for="d in auditTimeOptions"
                :key="d.id"
                :label="slotLabel(d)"
                :value="slotValue(d)"
                :disabled="isSlotFull(d)"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="地点" required>
            <el-input v-model="auditForm.location" placeholder="如：心理中心305室">
              <template #prefix><el-icon><Location /></el-icon></template>
            </el-input>
          </el-form-item>
        </template>

        <!-- 拒绝时 -->
        <template v-if="auditForm.status === '已拒绝'">
          <el-divider content-position="left">
            <span style="font-size:13px; color:#606266;">拒绝说明</span>
          </el-divider>
          <el-form-item label="拒绝原因">
            <el-input
              v-model="auditForm.rejectReason"
              type="textarea"
              :rows="3"
              placeholder="可选填，将通过系统通知发送给学生"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
        </template>
      </el-form>

      <el-alert
        v-if="auditForm.status === '已通过'"
        title="审核通过后系统将自动向学生发送通知，包含初访时间、地点信息。"
        type="success"
        :closable="false"
        show-icon
        style="margin-top: 4px;"
      />
      <el-alert
        v-if="auditForm.status === '已拒绝'"
        title="拒绝后系统将自动向学生发送通知，请确认后再提交。"
        type="warning"
        :closable="false"
        show-icon
        style="margin-top: 4px;"
      />

      <template #footer>
        <el-button @click="auditShow = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAudit">确认并发通知</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 改期弹窗（已通过记录） ==================== -->
    <el-dialog
      v-model="rescheduleShow"
      title="改期 / 修改初访安排"
      width="520px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-alert
        :title="`正在修改「${rescheduleForm.studentName}」的初访安排`"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 16px;"
      />

      <el-form :model="rescheduleForm" label-width="100px">
        <el-form-item label="分配初访员" required>
          <el-select
            v-model="rescheduleForm.visitorId"
            placeholder="请选择初访员"
            @change="changeRescheduleVisitor"
            style="width: 100%"
            filterable
          >
            <el-option
              v-for="v in visitorList"
              :key="v.id"
              :label="v.name"
              :value="v.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="初访日期" required>
          <el-date-picker
            v-model="rescheduleForm.visitDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择初访日期"
            :disabled-date="disabledRescheduleDate"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="时间段" required>
          <el-select
            v-model="rescheduleForm.visitTime"
            placeholder="请先选择初访日期"
            :disabled="!rescheduleForm.visitDate"
            style="width: 100%"
          >
            <el-option
              v-for="d in rescheduleTimeOptions"
              :key="d.id"
              :label="slotLabel(d)"
              :value="slotValue(d)"
              :disabled="isSlotFull(d)"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="地点" required>
          <el-input v-model="rescheduleForm.location" placeholder="如：心理中心305室">
            <template #prefix><el-icon><Location /></el-icon></template>
          </el-input>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="rescheduleShow = false">取消</el-button>
        <el-button type="primary" :loading="rescheduleSubmitting" @click="submitReschedule">
          确认改期
        </el-button>
      </template>
    </el-dialog>

    <!-- ==================== 手动新增预约弹窗 ==================== -->
    <el-dialog
      v-model="addShow"
      title="手动新增初访预约"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <!-- Step 1: 学号查询 -->
      <el-divider content-position="left">
        <span style="font-size:13px; color:#606266;">第一步：查询学生</span>
      </el-divider>

      <el-form :model="addForm" label-width="100px">
        <el-form-item label="学生学号" required>
          <el-input
            v-model="addForm.studentUsername"
            placeholder="输入学号后点击查询"
            style="width: calc(100% - 80px); margin-right: 8px;"
            @keyup.enter="searchStudent"
          />
          <el-button
            type="primary"
            :loading="studentSearching"
            @click="searchStudent"
          >查询</el-button>
        </el-form-item>

        <el-form-item label="学生姓名">
          <el-input
            v-model="addForm.studentName"
            :placeholder="addForm.studentId ? addForm.studentName : '查询后自动填写'"
            readonly
          >
            <template #suffix v-if="addForm.studentId">
              <el-icon style="color: #67c23a;"><CircleCheck /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="问卷得分">
          <el-input-number
            v-model="addForm.questionnaireScore"
            :min="0"
            :max="10"
            controls-position="right"
            placeholder="选填"
            style="width: 100%"
          />
          <div style="color:#999; font-size:12px; margin-top:4px;">满分 100 分，低于 10 分将自动标记红色报警</div>
        </el-form-item>

        <!-- Step 2: 安排初访 -->
        <el-divider content-position="left">
          <span style="font-size:13px; color:#606266;">第二步：分配初访安排</span>
        </el-divider>

        <el-form-item label="分配初访员" required>
          <el-select
            v-model="addForm.visitorId"
            placeholder="请选择初访员"
            @change="changeAddVisitor"
            style="width: 100%"
            filterable
          >
            <el-option
              v-for="v in visitorList"
              :key="v.id"
              :label="v.name"
              :value="v.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="初访日期" required>
          <el-date-picker
            v-model="addForm.visitDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择初访日期"
            :disabled-date="disabledAddDate"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="时间段" required>
          <el-select
            v-model="addForm.visitTime"
            placeholder="请先选择初访日期"
            :disabled="!addForm.visitDate"
            style="width: 100%"
          >
            <el-option
              v-for="d in addTimeOptions"
              :key="d.id"
              :label="slotLabel(d)"
              :value="slotValue(d)"
              :disabled="isSlotFull(d)"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="地点" required>
          <el-input v-model="addForm.location" placeholder="如：心理中心305室">
            <template #prefix><el-icon><Location /></el-icon></template>
          </el-input>
        </el-form-item>
      </el-form>

      <el-alert
        title="管理员手动新增的预约将直接标记为「已通过」，学生无需自主申请。"
        type="info"
        :closable="false"
        show-icon
        style="margin-top: 4px;"
      />

      <template #footer>
        <el-button @click="addShow = false">取消</el-button>
        <el-button type="primary" :loading="addSubmitting" @click="submitAddVisit">
          确认新增
        </el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  QuestionFilled, Warning, Top, Clock, CircleCheck, Refresh,
  Search, EditPen, Location, Edit, Plus, Delete
} from '@element-plus/icons-vue'

// ===== 基础状态 =====
const list        = ref([])
const visitorList = ref([])
const loading     = ref(false)
const submitting  = ref(false)

// 分页
const pageNum  = ref(1)
const pageSize = ref(10)
const total    = ref(0)

// 筛选
const filterStatus = ref('待审核')
const filterAlert  = ref('')
const filterName   = ref('')

// 防抖搜索
let searchTimer = null
const debounceSearch = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    pageNum.value = 1
    getList()
  }, 300)
}

// ===== 计算属性 =====
const stats = ref({
  pending:   0,
  alert:     0,
  emergency: 0,
  passed:    0,
})

const displayList = computed(() => {
  let result = list.value
  if (filterAlert.value === 'alert')     result = result.filter(r => r.isAlert)
  else if (filterAlert.value === 'emergency') result = result.filter(r => r.isEmergency)
  return result
})

// ===== 工具函数 =====

/** 构造时间段显示标签，带剩余名额提示 */
const slotLabel = (d) => {
  const time = `${(d.startTime || '').slice(0, 5)}-${(d.endTime || '').slice(0, 5)}`
  if (d.maxPerson == null) return time
  const booked    = d.bookedCount ?? 0
  const remaining = d.maxPerson - booked
  if (remaining <= 0) return `${time}（已满）`
  return `${time}（剩余 ${remaining}/${d.maxPerson} 人）`
}

/** 构造时间段 value，格式 "HH:mm-HH:mm" */
const slotValue = (d) =>
  `${(d.startTime || '').slice(0, 5)}-${(d.endTime || '').slice(0, 5)}`

/** 判断时间段是否已满 */
const isSlotFull = (d) =>
  d.maxPerson != null && (d.bookedCount ?? 0) >= d.maxPerson

const scoreTagType = (score) => {
  if (score == null) return 'info'
  if (score < 10)   return 'danger'
  return 'success'
}

const statusTagType = (status) => {
  const map = { '待审核': 'warning', '已通过': 'success', '已拒绝': 'danger', '已完成': 'info', '已撤销': 'info' }
  return map[status] ?? 'default'
}

const getRowClass = ({ row }) => {
  if (row.isEmergency) return 'row-emergency'
  if (row.isAlert)     return 'row-alert'
  return ''
}

const formatDateTime = (val) => {
  if (!val) return '-'
  return String(val).replace('T', ' ').substring(0, 16)
}

/** 通用日期禁用：过去日期禁用，仅允许值班日期 */
const makeDateDisabler = (availableDatesRef) => (time) => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  if (time < today) return true
  if (availableDatesRef.value.length === 0) return true
  const y = time.getFullYear()
  const m = String(time.getMonth() + 1).padStart(2, '0')
  const d = String(time.getDate()).padStart(2, '0')
  return !availableDatesRef.value.includes(`${y}-${m}-${d}`)
}

// ===== 接口请求 =====

const getList = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/visit/page', {
      params: {
        pageNum:     pageNum.value,
        pageSize:    pageSize.value,
        status:      filterStatus.value || undefined,
        studentName: filterName.value.trim() || undefined,
      }
    })
    const pageData = (res && res.records !== undefined) ? res : (res.data ?? res)
    list.value  = pageData.records ?? []
    total.value = pageData.total ?? list.value.length
  } catch (e) {
    ElMessage.error('获取列表失败')
    console.error(e)
  } finally {
    loading.value = false
  }
  getStats()
}

const getVisitors = async () => {
  try {
    const res = await request.get('/admin/visit/today-duty-visitors')
    visitorList.value = Array.isArray(res) ? res : (res?.data ?? [])
  } catch (e) {
    console.error(e)
  }
}

const getStats = async () => {
  try {
    const res = await request.get('/admin/visit/stats')
    const data = res?.data ?? res
    if (data) {
      stats.value = {
        pending:   data.pending   ?? 0,
        alert:     data.alert     ?? 0,
        emergency: data.emergency ?? 0,
        passed:    data.passed    ?? 0,
      }
    }
  } catch (e) {
    console.error('获取统计失败', e)
  }
}

/** 加载某初访员的值班日期列表（含 bookedCount），返回 allDutyArr */
const loadDutyDates = async (visitorId) => {
  const res = await request.get(`/admin/visit/get-duty-date/${visitorId}`)
  return Array.isArray(res) ? res : (res?.data ?? [])
}

// ===== 重置 =====

const resetFilter = () => {
  filterStatus.value = ''
  filterAlert.value  = ''
  filterName.value   = ''
  pageNum.value      = 1
  getList()
}

// ===== 紧急标记 =====

const markEmergency = async (row) => {
  try {
    await request.post('/admin/visit/markEmergency', { id: row.id, isEmergency: true })
    ElMessage.success('已标记为紧急优先')
    getList()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

// ===== 删除预约记录 =====

const deleteVisit = async (row) => {
  const statusLabel = row.status ?? '该'
  const noticeHint  = row.status === '已完成'
    ? '删除后不可恢复，已完成的初访记录将被清除。'
    : '删除后不可恢复，系统将向学生发送取消通知。'
  try {
    await ElMessageBox.confirm(
      `确认删除「${row.studentName}」的${statusLabel}预约记录？${noticeHint}`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger' }
    )
  } catch {
    return
  }

  try {
    const res = await request.delete(`/admin/visit/delete/${row.id}`)
    const ok = res && (res.code === 200 || res.code === 0 || res.code === '200' || res.code === '0')
    if (!ok) { ElMessage.error((res && res.msg) || '删除失败'); return }
    ElMessage.success(`已删除「${row.studentName}」的预约记录`)
    getList()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '删除失败，请稍后重试')
    console.error(e)
  }
}

// ==============================================================
// ★ 审核弹窗（待审核 → 已通过/已拒绝）
// ==============================================================
const auditShow    = ref(false)
const auditForm    = ref({})
const auditFormRef = ref(null)

const auditAvailableDates = ref([])
const auditTimeOptions    = ref([])
let   auditAllDutyArr     = []
let   auditUnwatch        = null

const disabledAuditDate = makeDateDisabler(auditAvailableDates)

const openAudit = (row) => {
  if (auditUnwatch) { auditUnwatch(); auditUnwatch = null; }
  auditAvailableDates.value = []
  auditTimeOptions.value    = []
  auditAllDutyArr           = []

  auditForm.value = {
    ...row,
    status:       '已通过',
    rejectReason: '',
    _origVisitDate:   row.visitDate   || '',
    _origVisitTime:   row.visitTime   || '',
    _origLocation:    row.location    || '',
    _origVisitorId:   row.visitorId   ?? null,
    _origVisitorName: row.visitorName || '',
    visitorId: row.visitorId ?? null,
    visitDate: row.visitDate || '',
    visitTime: row.visitTime || '',
    location:  row.location  || '',
  }
  auditShow.value = true

  if (row.visitorId) {
    changeAuditVisitor(row.visitorId)
  }
}

const changeAuditVisitor = async (vid) => {
  if (auditUnwatch) { auditUnwatch(); auditUnwatch = null; }
  auditAvailableDates.value = []
  auditTimeOptions.value    = []
  auditForm.value.visitDate = ''
  auditForm.value.visitTime = ''
  if (!vid) return

  auditAllDutyArr = await loadDutyDates(vid)
  auditAvailableDates.value = [...new Set(auditAllDutyArr.map(d => d.dutyDate))]

  const currentDate = auditForm.value.visitDate
  if (currentDate) {
    auditTimeOptions.value = auditAllDutyArr.filter(d => d.dutyDate === currentDate)
  }

  auditUnwatch = watch(() => auditForm.value.visitDate, (day) => {
    auditForm.value.visitTime = ''
    auditTimeOptions.value = day
      ? auditAllDutyArr.filter(d => d.dutyDate === day)
      : []
  })
}

const submitAudit = async () => {
  const form = auditForm.value
  if (form.status === '已通过') {
    if (!form.visitorId) { ElMessage.warning('请选择初访员'); return }
    if (!form.visitDate)  { ElMessage.warning('请选择初访日期'); return }
    if (!form.visitTime)  { ElMessage.warning('请选择时间段'); return }
    if (!form.location)   { ElMessage.warning('请填写地点'); return }
  }

  const isModified = form.status === '已通过' && (
    form.visitorId !== form._origVisitorId ||
    form.visitDate !== form._origVisitDate ||
    form.visitTime !== form._origVisitTime ||
    form.location  !== form._origLocation
  )

  const payload = {
    id:           form.id,
    status:       form.status,
    rejectReason: form.status === '已拒绝' ? (form.rejectReason || '') : null,
    isModified,
    visitorId:    form.status === '已通过' ? form.visitorId : null,
    visitDate:    form.status === '已通过' ? form.visitDate : null,
    visitTime:    form.status === '已通过' ? form.visitTime : null,
    location:     form.status === '已通过' ? form.location  : null,
  }

  submitting.value = true
  try {
    const res = await request.post('/admin/visit/audit', payload)
    const ok  = res && (res.code === 200 || res.code === 0 || res.code === '200' || res.code === '0')
    if (!ok) { ElMessage.error((res && res.msg) || '审核失败'); return }

    let msg
    if (form.status === '已通过') {
      msg = isModified
        ? `审核通过（安排有调整），通知已发送给「${form.studentName}」`
        : `审核通过，通知已发送给「${form.studentName}」`
    } else {
      msg = `已拒绝，通知已发送给「${form.studentName}」`
    }
    ElMessage.success(msg)
    auditShow.value = false
    getList()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '审核请求失败，请稍后重试')
    console.error(e)
  } finally {
    submitting.value = false
  }
}

// ==============================================================
// ★ 改期弹窗
// ==============================================================
const rescheduleShow       = ref(false)
const rescheduleForm       = ref({})
const rescheduleSubmitting = ref(false)

const rescheduleAvailableDates = ref([])
const rescheduleTimeOptions    = ref([])
let   rescheduleAllDutyArr     = []
let   rescheduleUnwatch        = null

const disabledRescheduleDate = makeDateDisabler(rescheduleAvailableDates)

const openReschedule = async (row) => {
  if (rescheduleUnwatch) { rescheduleUnwatch(); rescheduleUnwatch = null; }
  rescheduleAvailableDates.value = []
  rescheduleTimeOptions.value    = []
  rescheduleAllDutyArr           = []

  rescheduleForm.value = {
    id:          row.id,
    studentName: row.studentName,
    visitorId:   row.visitorId,
    visitDate:   row.visitDate,
    visitTime:   row.visitTime,
    location:    row.location,
  }
  rescheduleShow.value = true

  if (row.visitorId) {
    rescheduleAllDutyArr = await loadDutyDates(row.visitorId)
    rescheduleAvailableDates.value = [...new Set(rescheduleAllDutyArr.map(d => d.dutyDate))]

    if (row.visitDate) {
      rescheduleTimeOptions.value = rescheduleAllDutyArr.filter(d => d.dutyDate === row.visitDate)
    }

    rescheduleUnwatch = watch(() => rescheduleForm.value.visitDate, (day) => {
      rescheduleForm.value.visitTime = ''
      rescheduleTimeOptions.value = day
        ? rescheduleAllDutyArr.filter(d => d.dutyDate === day)
        : []
    })
  }
}

const changeRescheduleVisitor = async (vid) => {
  if (rescheduleUnwatch) { rescheduleUnwatch(); rescheduleUnwatch = null; }
  rescheduleAvailableDates.value    = []
  rescheduleTimeOptions.value       = []
  rescheduleAllDutyArr              = []
  rescheduleForm.value.visitDate    = ''
  rescheduleForm.value.visitTime    = ''
  if (!vid) return

  rescheduleAllDutyArr = await loadDutyDates(vid)
  rescheduleAvailableDates.value = [...new Set(rescheduleAllDutyArr.map(d => d.dutyDate))]

  rescheduleUnwatch = watch(() => rescheduleForm.value.visitDate, (day) => {
    rescheduleForm.value.visitTime = ''
    rescheduleTimeOptions.value = day
      ? rescheduleAllDutyArr.filter(d => d.dutyDate === day)
      : []
  })
}

const submitReschedule = async () => {
  const form = rescheduleForm.value
  if (!form.visitorId) { ElMessage.warning('请选择初访员'); return }
  if (!form.visitDate)  { ElMessage.warning('请选择初访日期'); return }
  if (!form.visitTime)  { ElMessage.warning('请选择时间段'); return }
  if (!form.location)   { ElMessage.warning('请填写地点'); return }

  rescheduleSubmitting.value = true
  try {
    const res = await request.post('/admin/visit/reschedule', {
      id:        form.id,
      visitorId: form.visitorId,
      visitDate: form.visitDate,
      visitTime: form.visitTime,
      location:  form.location,
    })
    const ok = res && (res.code === 200 || res.code === 0 || res.code === '200' || res.code === '0')
    if (!ok) { ElMessage.error((res && res.msg) || '改期失败'); return }

    ElMessage.success(`「${form.studentName}」的初访安排已成功改期`)
    rescheduleShow.value = false
    getList()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '改期失败，请稍后重试')
    console.error(e)
  } finally {
    rescheduleSubmitting.value = false
  }
}

// ==============================================================
// ★ 手动新增预约弹窗
// ==============================================================
const addShow          = ref(false)
const addForm          = ref({})
const addSubmitting    = ref(false)
const studentSearching = ref(false)

const addAvailableDates = ref([])
const addTimeOptions    = ref([])
let   addAllDutyArr     = []
let   addUnwatch        = null

const disabledAddDate = makeDateDisabler(addAvailableDates)

const openAddVisit = () => {
  if (addUnwatch) { addUnwatch(); addUnwatch = null; }
  addAvailableDates.value = []
  addTimeOptions.value    = []
  addAllDutyArr           = []

  addForm.value = {
    studentUsername:   '',
    studentId:         null,
    studentName:       '',
    visitorId:         null,
    visitDate:         '',
    visitTime:         '',
    location:          '',
    questionnaireScore: null,
    status:            '已通过',
  }
  addShow.value = true
}

/** 通过学号查询学生 */
const searchStudent = async () => {
  const username = (addForm.value.studentUsername || '').trim()
  if (!username) { ElMessage.warning('请输入学号'); return }
  studentSearching.value = true
  try {
    const res = await request.get('/admin/user/student', { params: { username } })
    const ok  = res && (res.code === 200 || res.code === 0 || res.code === '200' || res.code === '0')
    if (!ok) { ElMessage.error((res && res.msg) || '未找到该学生'); return }
    const student = res.data || res
    addForm.value.studentId   = student.id
    addForm.value.studentNo   = student.username   // 学号（user.username）
    addForm.value.studentName = student.name
    ElMessage.success(`已找到学生：${student.name}`)
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '学生查询失败')
    console.error(e)
  } finally {
    studentSearching.value = false
  }
}

const changeAddVisitor = async (vid) => {
  if (addUnwatch) { addUnwatch(); addUnwatch = null; }
  addAvailableDates.value  = []
  addTimeOptions.value     = []
  addAllDutyArr            = []
  addForm.value.visitDate  = ''
  addForm.value.visitTime  = ''
  if (!vid) return

  addAllDutyArr = await loadDutyDates(vid)
  addAvailableDates.value = [...new Set(addAllDutyArr.map(d => d.dutyDate))]

  addUnwatch = watch(() => addForm.value.visitDate, (day) => {
    addForm.value.visitTime = ''
    addTimeOptions.value = day
      ? addAllDutyArr.filter(d => d.dutyDate === day)
      : []
  })
}

const submitAddVisit = async () => {
  const form = addForm.value
  if (!form.studentId)  { ElMessage.warning('请先通过学号查询并确认学生信息'); return }
  if (!form.visitorId)  { ElMessage.warning('请选择初访员'); return }
  if (!form.visitDate)  { ElMessage.warning('请选择初访日期'); return }
  if (!form.visitTime)  { ElMessage.warning('请选择时间段'); return }
  if (!form.location)   { ElMessage.warning('请填写地点'); return }

  addSubmitting.value = true
  try {
    const payload = {
      studentId:          form.studentId,
      studentNo:          form.studentNo,          // 学号（user.username）
      studentName:        form.studentName,
      visitorId:          form.visitorId,
      visitDate:          form.visitDate,
      visitTime:          form.visitTime,
      location:           form.location,
      questionnaireScore: form.questionnaireScore,
      isAlert:            form.questionnaireScore != null && form.questionnaireScore < 10,
      status:             '已通过',
    }
    const res = await request.post('/admin/visit/add', payload)
    const ok  = res && (res.code === 200 || res.code === 0 || res.code === '200' || res.code === '0')
    if (!ok) { ElMessage.error((res && res.msg) || '新增失败'); return }

    ElMessage.success(`「${form.studentName}」的初访预约已成功新增`)
    addShow.value = false
    getList()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '新增失败，请稍后重试')
    console.error(e)
  } finally {
    addSubmitting.value = false
  }
}

// ===== 初始化 =====
onMounted(() => {
  getList()
  getStats()
  getVisitors()
})
</script>

<style scoped>
.visit-audit-page {
  padding: 20px 24px;
  background: #F5F7FA;
  min-height: 100%;
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
}

/* ── 统计卡片 ─────────────────────────────────────────────── */
.stat-card {
  background: #fff;
  border-radius: 14px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  position: relative;
  overflow: hidden;
  border: 1px solid #F0F2F5;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  transition: transform .2s, box-shadow .2s;
}
.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0,0,0,0.09);
}
.stat-icon-wrap {
  width: 48px; height: 48px;
  border-radius: 13px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.stat-bg-circle {
  position: absolute;
  right: -16px; bottom: -16px;
  width: 72px; height: 72px;
  border-radius: 50%;
  opacity: 0.12;
}
.stat-content { flex: 1; }
.stat-num {
  font-size: 28px;
  font-weight: 800;
  line-height: 1.1;
}
.stat-label {
  font-size: 12px;
  color: #8899AA;
  margin-top: 4px;
  letter-spacing: 0.3px;
}

/* 颜色主题 */
.stat-pending .stat-icon-wrap  { background: #FFF5E6; }
.stat-pending .el-icon         { color: #E6A23C; }
.stat-pending .stat-num        { color: #D48806; }
.stat-pending .stat-bg-circle  { background: #FFC87C; }

.stat-alert .stat-icon-wrap    { background: #FFF0F0; }
.stat-alert .el-icon           { color: #F56C6C; }
.stat-alert .stat-num          { color: #CF1322; }
.stat-alert .stat-bg-circle    { background: #FFABA8; }

.stat-emergency .stat-icon-wrap { background: #FFF7E6; }
.stat-emergency .el-icon        { color: #FA8C16; }
.stat-emergency .stat-num       { color: #D46B08; }
.stat-emergency .stat-bg-circle { background: #FFB366; }

.stat-passed .stat-icon-wrap   { background: #F0FFF6; }
.stat-passed .el-icon          { color: #52C41A; }
.stat-passed .stat-num         { color: #389E0D; }
.stat-passed .stat-bg-circle   { background: #86E3CE; }

/* ── 筛选栏 ────────────────────────────────────────────────── */
.filter-bar {
  background: #fff;
  border-radius: 12px;
  padding: 14px 20px;
  margin-bottom: 16px;
  border: 1px solid #EAECF0;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.filter-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.filter-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.filter-item { width: 140px; }
.filter-search { width: 180px; }
.btn-reset {
  color: #718096 !important;
  border-color: #E2E8F0 !important;
}
.rule-btn { color: #94A3B8 !important; font-size: 13px !important; }
.btn-add {
  background: linear-gradient(135deg, #3B7FD9, #5A9EF5) !important;
  border: none !important;
  box-shadow: 0 3px 10px rgba(59,127,217,0.25) !important;
}

/* ── 表格卡片 ────────────────────────────────────────────────── */
.table-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #EAECF0;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  overflow: hidden;
}
.table-card :deep(.el-table) {
  font-size: 13px;
}
.table-card :deep(.el-table__row:hover > td) {
  background: #F8FAFF !important;
}

/* 表格行高亮 */
:deep(.row-emergency td) { background-color: #fff8f0 !important; }
:deep(.row-alert td)     { background-color: #fff5f5 !important; }

/* 序号强调 */
.index-emergency {
  display: inline-flex;
  align-items: center; justify-content: center;
  background: #FA8C16;
  color: #fff;
  border-radius: 6px;
  width: 24px; height: 24px;
  font-size: 12px; font-weight: 700;
}
.index-alert {
  display: inline-flex;
  align-items: center; justify-content: center;
  background: #F56C6C;
  color: #fff;
  border-radius: 6px;
  width: 24px; height: 24px;
  font-size: 12px; font-weight: 700;
}

/* 学生信息列 */
.student-cell {
  display: flex;
  align-items: center;
}
.student-info   { display: flex; flex-direction: column; }
.student-name   { font-weight: 600; color: #1A2B4A; font-size: 13px; }
.student-id     { font-size: 11px; color: #94A3B8; margin-top: 1px; }

.time-text {
  font-size: 12px;
  color: #718096;
}

/* ── 底部分页栏 ────────────────────────────────────────────── */
.table-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 18px;
  border-top: 1px solid #F1F5F9;
  background: #FAFBFC;
}
.footer-info {
  font-size: 13px;
  color: #94A3B8;
}
.footer-pending   { color: #D48806; font-weight: 500; }
.footer-emergency { color: #CF1322; font-weight: 500; }
</style>
