<template>
  <div class="ch-home">
    <!-- ── Header ── -->
    <div class="ch-header">
      <div class="ch-title-group">
        <div class="ch-icon-wrap">
          <span class="ch-icon">🩺</span>
        </div>
        <div>
          <h2 class="ch-title">咨询师工作台</h2>
          <p class="ch-subtitle">咨询记录管理 · 结案报告</p>
        </div>
      </div>
      <el-button class="logout-btn" round @click="handleLogout">
        <el-icon style="margin-right:4px"><SwitchButton /></el-icon>退出登录
      </el-button>
      <div class="header-decor"></div>
    </div>

    <!-- ── Welcome Card ── -->
    <div class="welcome-card">
      <div class="welcome-left">
        <div class="avatar-circle">{{ avatarChar }}</div>
        <div class="welcome-info">
          <div class="welcome-name">
            欢迎回来，{{ displayName }}
            <el-tag size="small" class="role-tag">咨询师</el-tag>
          </div>
          <div class="welcome-date">{{ currentDate }}</div>
        </div>
      </div>
      <div class="welcome-clock">{{ currentTime }}</div>
    </div>

    <!-- ── Main Tabs ── -->
    <el-tabs v-model="activeTab" class="ch-tabs">

      <!-- ① 我的咨询学生 -->
      <el-tab-pane name="students">
        <template #label>
          <span class="tab-label"><el-icon><User /></el-icon>我的咨询学生</span>
        </template>

        <div class="tab-section">
          <div class="tab-toolbar">
            <el-button :icon="Refresh" @click="loadStudents" :loading="loadingStudents" round>刷新</el-button>
            <span class="toolbar-tip">
              <span class="tip-count">{{ pagination.total }}</span> 位学生 · 每个案最多 8 次咨询
            </span>
          </div>

          <el-table
            :data="studentList"
            v-loading="loadingStudents"
            :header-cell-style="headerCellStyle"
            row-key="id"
            stripe
            class="ch-table"
          >
            <el-table-column type="index" label="序" width="52" align="center" />
            <el-table-column prop="studentName" label="来访者姓名" width="110" />
            <el-table-column prop="studentNo"   label="学号" width="130" />
            <el-table-column prop="counselingTime" label="约定时段" min-width="190" show-overflow-tooltip />
            <el-table-column prop="location"    label="地点" width="120" show-overflow-tooltip />
            <el-table-column prop="startDate"   label="开始日期" width="110" />
            <el-table-column label="咨询进度" width="140" align="center">
              <template #default="{ row }">
                <div class="progress-wrap">
                  <el-progress
                    :percentage="Math.round((row.sessionCount || 0) / 8 * 100)"
                    :status="(row.sessionCount || 0) >= 8 ? 'success' : ''"
                    :stroke-width="10"
                    :format="() => `${row.sessionCount || 0}/8`"
                  />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="row.status === '已结案' ? 'info' : 'success'"
                  effect="light"
                  size="small"
                  round
                >{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="210" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" plain round @click="openSessionDialog(row)">
                  {{ row.status === '已结案' ? '查看记录' : '录入记录' }}
                </el-button>
                <el-tooltip
                  :content="(row.sessionCount || 0) < 8 ? `需完成 8 次咨询（当前 ${row.sessionCount || 0} 次）` : ''"
                  :disabled="(row.sessionCount || 0) >= 8"
                >
                  <span>
                    <el-button
                      size="small"
                      type="warning"
                      plain
                      round
                      @click="openReportDialog(row)"
                      :disabled="row.status === '已结案' || (row.sessionCount || 0) < 8"
                    >结案报告</el-button>
                  </span>
                </el-tooltip>
              </template>
            </el-table-column>
            <template #empty><el-empty description="暂无咨询学生" /></template>
          </el-table>

          <div class="pagination-bar">
            <el-pagination
              v-model:current-page="pagination.current"
              v-model:page-size="pagination.size"
              :total="pagination.total"
              layout="total, prev, pager, next"
              background
              @current-change="loadStudents"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- ② 我的结案报告 -->
      <el-tab-pane name="reports">
        <template #label>
          <span class="tab-label"><el-icon><Document /></el-icon>我的结案报告</span>
        </template>

        <div class="tab-section">
          <div class="tab-toolbar">
            <el-button :icon="Refresh" @click="loadReports" :loading="loadingReports" round>刷新</el-button>
          </div>

          <el-table
            :data="reportList"
            v-loading="loadingReports"
            :header-cell-style="headerCellStyle"
            stripe
            class="ch-table"
          >
            <el-table-column type="index" label="序" width="52" align="center" />
            <el-table-column prop="studentName" label="来访者姓名" width="110" />
            <el-table-column prop="studentNo"   label="学号" width="130" />
            <el-table-column prop="department"  label="院系" width="150" show-overflow-tooltip />
            <el-table-column prop="problemType" label="问题类型" width="120" />
            <el-table-column label="咨询次数" width="90" align="center">
              <template #default="{ row }">
                <el-tag type="primary" effect="light" size="small" round>{{ row.totalTimes }} 次</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="startDate" label="开始" width="100" />
            <el-table-column prop="endDate"   label="结束" width="100" />
            <el-table-column label="创建时间" width="120">
              <template #default="{ row }">{{ fmtDate(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="168" fixed="right">
              <template #default="{ row }">
                <el-button size="small" plain round @click="viewReport(row)">查看详情</el-button>
                <el-button size="small" type="success" plain round @click="downloadFromList(row)">
                  <el-icon><Download /></el-icon>导出
                </el-button>
              </template>
            </el-table-column>
            <template #empty><el-empty description="暂无结案报告" /></template>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>


    <!-- ══════════ 咨询记录管理 Dialog ══════════ -->
    <el-dialog
      v-model="sessionDlgVisible"
      :title="`咨询记录管理 — ${currentStudent?.studentName || ''}`"
      width="780px"
      top="4vh"
      class="session-dlg"
    >
      <!-- 历史记录 -->
      <div class="rec-list-wrap" v-loading="loadingRecs">
        <div class="rec-legend">
          <span><span class="dot done" />完成</span>
          <span><span class="dot absent" />缺席</span>
          <span><span class="dot closing" />结案</span>
        </div>

        <el-timeline v-if="sessionRecords.length" class="rec-timeline">
          <el-timeline-item
            v-for="rec in sessionRecords"
            :key="rec.id"
            :type="timelineType(rec.status)"
            placement="top"
            size="large"
          >
            <template #timestamp>
              <span class="timeline-ts">第 <b>{{ rec.times }}</b> 次</span>
            </template>
            <div class="rec-card">
              <el-tag :type="statusTagType(rec.status)" effect="dark" size="small">{{ rec.status }}</el-tag>
              <p class="rec-text">{{ rec.record || '（本次未填写咨询内容）' }}</p>
            </div>
          </el-timeline-item>
        </el-timeline>

        <el-empty v-else-if="!loadingRecs" description="暂无记录，请录入第 1 次咨询" />
      </div>

      <!-- 录入新记录 -->
      <template v-if="canAddSession">
        <el-divider>
          <el-tag type="primary" effect="dark">录入第 {{ nextTimes }} 次</el-tag>
          <el-tag v-if="isLastSession" type="danger" effect="dark" style="margin-left:8px">最终结案</el-tag>
        </el-divider>

        <el-form :model="recForm" label-width="88px" class="add-rec-form">
          <el-form-item label="咨询次数">
            <span class="times-badge">第 {{ nextTimes }} 次</span>
            <span class="form-hint">（系统自动计算）</span>
          </el-form-item>

          <el-form-item label="咨询状态" required>
            <template v-if="isLastSession">
              <el-tag type="danger" effect="dark" size="large" style="font-size:14px;padding:0 16px">结案</el-tag>
              <span class="form-hint danger-hint">第 8 次咨询状态固定为「结案」，无需选择</span>
            </template>
            <el-radio-group v-else v-model="recForm.status">
              <el-radio-button value="完成">✔ 完成</el-radio-button>
              <el-radio-button value="缺席">✘ 缺席</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="咨询记录">
            <el-input
              v-model="recForm.record"
              type="textarea"
              :rows="4"
              placeholder="请填写本次咨询主要内容（来访者状态、本次议题、干预方式、作业/反馈等）"
              show-word-limit
              maxlength="1000"
            />
          </el-form-item>
        </el-form>
      </template>

      <el-alert
        v-else-if="sessionRecords.length >= 8 && currentStudent?.status !== '已结案'"
        type="success"
        show-icon
        :closable="false"
        style="margin-top:14px"
        title="8 次咨询已全部录入，请点击「填写结案报告」完成结案。"
      />

      <template #footer>
        <el-button @click="sessionDlgVisible = false">关闭</el-button>
        <el-button
          v-if="canAddSession"
          type="primary"
          :loading="savingRec"
          @click="saveRecord"
        >
          保存记录{{ isLastSession ? '（准备结案）' : '' }}
        </el-button>
        <el-button
          v-if="sessionRecords.length >= 8 && currentStudent?.status !== '已结案'"
          type="warning"
          @click="gotoReport"
        >
          <el-icon><Document /></el-icon>填写结案报告
        </el-button>
      </template>
    </el-dialog>


    <!-- ══════════ 结案报告填写 Dialog ══════════ -->
    <el-dialog
      v-model="reportDlgVisible"
      :title="`咨询结案报告 — ${currentStudent?.studentName || ''}`"
      width="940px"
      top="2vh"
      class="report-dlg"
    >
      <el-form :model="reportForm" label-width="110px" ref="reportFormRef" class="rpt-form">

        <!-- 来访者信息 -->
        <div class="form-section-title">来访者基本信息</div>
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="来访者学号">
              <el-input v-model="reportForm.studentNo" placeholder="学号（如 2024001）" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来访者姓名">
              <el-input v-model="reportForm.studentName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来访者性别" required>
              <el-radio-group v-model="reportForm.gender">
                <el-radio-button value="男">男</el-radio-button>
                <el-radio-button value="女">女</el-radio-button>
                <el-radio-button value="其他">其他</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来访者院系" required>
              <el-input v-model="reportForm.department" placeholder="如：计算机学院" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来访者联系电话">
              <el-input v-model="reportForm.phone" placeholder="来访者手机号" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="问题类型" required>
              <el-select
                v-model="reportForm.problemType"
                allow-create filterable placeholder="选择或输入"
                style="width:100%"
              >
                <el-option v-for="pt in PROBLEM_TYPES" :key="pt" :label="pt" :value="pt" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 咨询信息 -->
        <div class="form-section-title">咨询基本信息</div>
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="咨询总次数">
              <el-input-number v-model="reportForm.totalTimes" :min="1" :max="8" style="width:130px" />
              <span class="form-hint">&nbsp;次</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主咨询师">
              <el-input v-model="reportForm.counselorName" readonly />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="咨询开始日期">
              <el-date-picker v-model="reportForm.startDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="咨询结束日期">
              <el-date-picker v-model="reportForm.endDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 结案结论：只读记录 + 可编辑评估 -->
        <div class="form-section-title">
          结案结论
          <span class="section-hint">（各次咨询记录已自动汇总，请在下方填写综合评估与建议）</span>
        </div>

        <!-- ① 只读：各次记录汇总 -->
        <div class="conclusion-records-readonly">
          <div
            v-for="rec in rptSessionCache"
            :key="rec.id"
            class="crr-item"
          >
            <div class="crr-badge">【第{{ rec.times }}次 · {{ rec.status }}】</div>
            <div class="crr-text">{{ rec.record?.trim() || '（未填写咨询内容）' }}</div>
          </div>
        </div>

        <!-- 分隔线 -->
        <div class="conclusion-divider"></div>

        <!-- ② 可编辑：咨询师综合评估 -->
        <el-form-item label-width="0" required>
          <div class="summary-label">【咨询师综合评估与建议】</div>
          <el-input
            v-model="counselorSummary"
            type="textarea"
            :rows="6"
            placeholder="请填写对来访者的综合评估、干预效果评价及后续建议..."
            show-word-limit
            maxlength="2000"
            class="conclusion-textarea"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="rpt-footer">
          <el-button plain @click="regenerateConclusion">
            <el-icon><Refresh /></el-icon>重新生成汇总
          </el-button>
          <div class="rpt-footer-right">
            <el-button @click="reportDlgVisible = false">取消</el-button>
            <el-button type="success" plain @click="exportFromDialog" :loading="exporting">
              <el-icon><Download /></el-icon>导出 Word (.doc)
            </el-button>
            <el-button type="primary" @click="submitReport" :loading="savingReport">
              <el-icon><Check /></el-icon>提交结案报告
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>


    <!-- ══════════ 查看结案报告 Dialog ══════════ -->
    <el-dialog
      v-model="viewDlgVisible"
      title="结案报告详情"
      width="780px"
      top="3vh"
      class="view-dlg"
    >
      <div v-if="viewingReport" class="view-content">

        <!-- 公文标题 -->
        <h2 class="report-doc-title">心理咨询结案报告</h2>

        <!-- 信息表格 -->
        <table class="report-info-table">
          <colgroup>
            <col style="width:120px" />
            <col />
            <col style="width:120px" />
            <col />
          </colgroup>
          <tbody>
            <tr>
              <td class="r-lbl">来访者学号</td>
              <td class="r-val">{{ viewingReport.studentNo || '—' }}</td>
              <td class="r-lbl">来访者姓名</td>
              <td class="r-val">{{ viewingReport.studentName }}</td>
            </tr>
            <tr>
              <td class="r-lbl">来访者性别</td>
              <td class="r-val">{{ viewingReport.gender || '—' }}</td>
              <td class="r-lbl">来访者院系</td>
              <td class="r-val">{{ viewingReport.department || '—' }}</td>
            </tr>
            <tr>
              <td class="r-lbl">来访者联系电话</td>
              <td class="r-val" colspan="3">{{ viewingReport.phone || '—' }}</td>
            </tr>
            <tr>
              <td class="r-lbl">问题类型</td>
              <td class="r-val">{{ viewingReport.problemType || '—' }}</td>
              <td class="r-lbl r-bold">咨询总次数</td>
              <td class="r-val r-bold">{{ viewingReport.totalTimes }}</td>
            </tr>
            <tr>
              <td class="r-lbl">主要咨询师</td>
              <td class="r-val">{{ viewingReport.counselorName }}</td>
              <td class="r-lbl r-bold">咨询日期</td>
              <td class="r-val r-bold">{{ viewingReport.startDate }} ~ {{ viewingReport.endDate }}</td>
            </tr>
            <tr>
              <td class="r-lbl">结案结论</td>
              <td class="r-val r-conclusion" colspan="3">
                <template v-if="viewingReport.conclusion">
                  <div
                    v-for="(line, idx) in viewingReport.conclusion.split('\n')"
                    :key="idx"
                    :class="line.startsWith('【') ? 'conc-badge-line' : line.startsWith('─') ? 'conc-sep-line' : 'conc-plain-line'"
                  >{{ line || '\u00a0' }}</div>
                </template>
                <span v-else class="conc-empty">（暂无结论）</span>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- 各次咨询记录（折叠展示） -->
        <el-collapse class="rec-collapse" v-if="viewingRecs.length">
          <el-collapse-item>
            <template #title>
              <span class="collapse-title">📋 各次咨询记录（共 {{ viewingRecs.length }} 次）</span>
            </template>
            <el-timeline class="view-timeline">
              <el-timeline-item
                v-for="rec in viewingRecs"
                :key="rec.id"
                :type="timelineType(rec.status)"
                placement="top"
                size="normal"
              >
                <template #timestamp>第 {{ rec.times }} 次</template>
                <div class="view-rec-card">
                  <el-tag :type="statusTagType(rec.status)" effect="dark" size="small">{{ rec.status }}</el-tag>
                  <span class="view-rec-text">{{ rec.record || '（未填写）' }}</span>
                </div>
              </el-timeline-item>
            </el-timeline>
          </el-collapse-item>
        </el-collapse>

      </div>

      <template #footer>
        <el-button @click="viewDlgVisible = false">关闭</el-button>
        <el-button type="success" plain @click="downloadFromList(viewingReport)" :loading="exporting">
          <el-icon><Download /></el-icon>导出 Word
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh, User, Document, Download, Check, SwitchButton
} from '@element-plus/icons-vue'
import axios from 'axios'

// ─────────────────── Constants ───────────────────
const BASE = 'http://localhost:8080/counsel'
const userInfo = JSON.parse(localStorage.getItem('user') || '{}')
const counselorId = userInfo.id
const counselorName = userInfo.name || userInfo.username || ''

const router = useRouter()

// ─────────────────── Welcome Card ───────────────────
const displayName = computed(() => userInfo.name || userInfo.username || '未知用户')
const avatarChar  = computed(() => (userInfo.name || userInfo.username || '?').charAt(0))

const currentTime = ref('')
const currentDate = ref('')
let clockTimer = null

const updateClock = () => {
  const now = new Date()
  const h = String(now.getHours()).padStart(2, '0')
  const m = String(now.getMinutes()).padStart(2, '0')
  const s = String(now.getSeconds()).padStart(2, '0')
  currentTime.value = `${h}:${m}:${s}`
  const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  const year = now.getFullYear(), month = now.getMonth() + 1, day = now.getDate()
  currentDate.value = `${year} 年 ${month} 月 ${day} 日 · ${weekDays[now.getDay()]}`
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确认退出登录？', '提示', {
      confirmButtonText: '确认退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
    localStorage.clear()
    router.push('/login')
  } catch {}
}

onUnmounted(() => { if (clockTimer) clearInterval(clockTimer) })

const PROBLEM_TYPES = [
  '情绪困扰', '人际关系', '学业压力', '家庭问题',
  '恋爱情感', '就业困扰', '睡眠问题', '自我认知',
  '创伤/应激', '其他'
]

const headerCellStyle = {
  background: 'linear-gradient(135deg, #EEF3FF 0%, #F2F7FF 100%)',
  fontWeight: '700',
  color: '#1a3a5c',
  fontSize: '13px',
  padding: '14px 0',
}

// ─────────────────── Tab ───────────────────
const activeTab = ref('students')

// ─────────────────── Students Tab ───────────────────
const studentList = ref([])
const loadingStudents = ref(false)
const pagination = reactive({ current: 1, size: 10, total: 0 })

const loadStudents = async () => {
  loadingStudents.value = true
  try {
    const res = await axios.get(`${BASE}/counselor/list`, {
      params: { counselorId, pageNum: pagination.current, pageSize: pagination.size }
    })
    const page = res.data.data
    const newRecords = page.records || []

    const oldCountMap = {}
    studentList.value.forEach(s => { if (s.sessionCount) oldCountMap[s.id] = s.sessionCount })
    newRecords.forEach(r => {
      if (r.sessionCount == null && oldCountMap[r.id] != null) {
        r.sessionCount = oldCountMap[r.id]
      }
    })

    studentList.value = newRecords
    pagination.total = page.total || 0
  } catch {
    ElMessage.error('获取学生列表失败')
  } finally {
    loadingStudents.value = false
  }
}

// ─────────────────── Reports Tab ───────────────────
const reportList = ref([])
const loadingReports = ref(false)

const loadReports = async () => {
  loadingReports.value = true
  try {
    const res = await axios.get(`${BASE}/counselor/closingList`, { params: { counselorId } })
    const reports = res.data.data || []

    const uniqueStudentIds = [...new Set(reports.map(r => r.studentId).filter(Boolean))]
    if (uniqueStudentIds.length > 0) {
      const userInfoMap = {}
      await Promise.all(
        uniqueStudentIds.map(async sid => {
          try {
            const uRes = await axios.get(`${BASE}/counselor/studentInfo`, { params: { studentId: sid } })
            userInfoMap[sid] = uRes.data.data
          } catch { /* 单条失败不影响整体列表加载 */ }
        })
      )
      reports.forEach(r => {
        if (r.studentId && userInfoMap[r.studentId]) {
          r.studentNo = userInfoMap[r.studentId].username
        }
      })
    }

    reportList.value = reports
  } catch {
    ElMessage.error('获取结案报告失败')
  } finally {
    loadingReports.value = false
  }
}

// ─────────────────── Session Dialog ───────────────────
const sessionDlgVisible = ref(false)
const currentStudent = ref(null)
const sessionRecords = ref([])
const loadingRecs = ref(false)
const savingRec = ref(false)

const recForm = reactive({ counselingId: null, status: '', record: '' })

const nextTimes = computed(() => sessionRecords.value.length + 1)
const isLastSession = computed(() => nextTimes.value === 8)
const canAddSession = computed(() =>
  sessionRecords.value.length < 8 &&
  currentStudent.value?.status !== '已结案'
)

const openSessionDialog = async (row) => {
  currentStudent.value = row
  Object.assign(recForm, { counselingId: row.id, status: '', record: '' })
  sessionDlgVisible.value = true
  loadingRecs.value = true
  try {
    const res = await axios.get(`${BASE}/counselor/recordList`, { params: { counselingId: row.id } })
    sessionRecords.value = res.data.data || []
    row.sessionCount = sessionRecords.value.length
  } catch {
    ElMessage.error('加载咨询记录失败')
  } finally {
    loadingRecs.value = false
  }
}

const saveRecord = async () => {
  if (!isLastSession.value && !recForm.status) {
    return ElMessage.warning('请选择本次咨询状态')
  }
  savingRec.value = true
  try {
    await axios.post(`${BASE}/counselor/submitRecord`, {
      counselingId: recForm.counselingId,
      times: nextTimes.value,
      status: isLastSession.value ? '结案' : recForm.status,
      record: recForm.record
    })
    ElMessage.success('咨询记录已保存')

    const res = await axios.get(`${BASE}/counselor/recordList`, { params: { counselingId: recForm.counselingId } })
    sessionRecords.value = res.data.data || []

    const target = studentList.value.find(s => s.id === recForm.counselingId)
    if (target) target.sessionCount = sessionRecords.value.length

    loadStudents()
    recForm.status = ''
    recForm.record = ''

    if (sessionRecords.value.length >= 8) {
      setTimeout(() => {
        ElMessageBox.confirm(
          '第 8 次咨询（结案）已录入，是否立即填写结案报告？',
          '咨询已完成',
          { confirmButtonText: '立即填写', cancelButtonText: '稍后填写', type: 'success' }
        ).then(() => gotoReport()).catch(() => {})
      }, 300)
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '保存失败，请重试')
  } finally {
    savingRec.value = false
  }
}

const gotoReport = () => {
  sessionDlgVisible.value = false
  openReportDialog(currentStudent.value)
}

// ─────────────────── Closing Report Dialog ───────────────────
const reportDlgVisible = ref(false)
const savingReport = ref(false)
const exporting = ref(false)
const rptSessionCache = ref([])
const counselorSummary = ref('')

const reportForm = reactive({
  counselingId: null,
  studentId: null,
  studentName: '',
  studentNo: '',
  gender: '',
  department: '',
  phone: '',
  problemType: '',
  totalTimes: 8,
  counselorId,
  counselorName,
  startDate: '',
  endDate: '',
  conclusion: ''
})

const buildConclusion = (records, summary) => {
  const sessionPart = records
    .map(r => `【第${r.times}次 · ${r.status}】\n${r.record?.trim() || '（未填写咨询内容）'}`)
    .join('\n\n')
  const sep = '\n\n' + '─'.repeat(28)
  const summaryPart = '\n\n【咨询师综合评估与建议】\n' + (summary?.trim() || '（未填写）')
  return sessionPart + sep + summaryPart
}

const openReportDialog = async (row) => {
  currentStudent.value = row
  try {
    const [recRes, userRes] = await Promise.all([
      axios.get(`${BASE}/counselor/recordList`, { params: { counselingId: row.id } }),
      row.studentId
        ? axios.get(`${BASE}/counselor/studentInfo`, { params: { studentId: row.studentId } })
        : Promise.resolve(null)
    ])

    const records = recRes.data.data || []
    const studentInfo = userRes?.data?.data || {}
    rptSessionCache.value = records
    counselorSummary.value = ''

    Object.assign(reportForm, {
      counselingId: row.id,
      studentId: row.studentId,
      studentName: row.studentName,
      studentNo:   studentInfo.username   || row.studentNo   || '',
      gender:      studentInfo.gender     || '',
      department:  studentInfo.department || '',
      phone:       studentInfo.phone      || '',
      problemType: '',
      totalTimes: records.length,
      counselorId,
      counselorName,
      startDate: row.startDate || '',
      endDate: new Date().toISOString().split('T')[0],
      conclusion: ''
    })
  } catch {
    ElMessage.error('加载咨询记录失败')
    return
  }
  reportDlgVisible.value = true
}

const regenerateConclusion = () => {
  counselorSummary.value = ''
  ElMessage.info('已清空综合评估内容，请重新填写')
}

const submitReport = async () => {
  if (!reportForm.gender) return ElMessage.warning('请选择来访者性别')
  if (!reportForm.department?.trim()) return ElMessage.warning('请填写来访者院系')
  if (!reportForm.problemType) return ElMessage.warning('请选择或填写问题类型')
  if (!counselorSummary.value?.trim() || counselorSummary.value.trim().length < 10) {
    return ElMessage.warning('请填写咨询师综合评估与建议（不少于10字）')
  }
  reportForm.conclusion = buildConclusion(rptSessionCache.value, counselorSummary.value)
  savingReport.value = true
  try {
    await axios.post(`${BASE}/counselor/submitClosing`, { ...reportForm })
    ElMessage.success('结案报告已提交，该个案正式结案！')
    reportDlgVisible.value = false
    await loadStudents()
    loadReports()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '提交失败')
  } finally {
    savingReport.value = false
  }
}

// ─────────────────── Word Export ───────────────────
const escHtml = (s) =>
  s ? String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;') : ''

const buildWordHtml = (report) => {
  return `<!DOCTYPE html>
<html xmlns:o="urn:schemas-microsoft-com:office:office"
      xmlns:w="urn:schemas-microsoft-com:office:word"
      xmlns="http://www.w3.org/TR/REC-html40">
<head>
  <meta charset="UTF-8">
  <!--[if gte mso 9]><xml>
    <w:WordDocument><w:View>Print</w:View><w:DoNotOptimizeForBrowser/></w:WordDocument>
  </xml><![endif]-->
  <style>
    @page { size:21cm 29.7cm; margin:2.5cm 2cm; }
    body { font-family:'宋体',SimSun,serif; font-size:12pt; line-height:1.8; color:#222; }
    h1  { text-align:center; font-size:20pt; font-weight:bold;
          font-family:'黑体',SimHei,sans-serif; margin:0 0 4pt;
          letter-spacing:4pt; color:#1a3a5c; }
    .doc-sub { text-align:center; font-size:10pt; color:#888; margin-bottom:20pt; }
    h2  { font-size:13pt; font-weight:bold; font-family:'黑体',SimHei,sans-serif;
          color:#1a3a5c; border-left:4pt solid #1a3a5c;
          padding-left:8pt; margin:14pt 0 7pt; }
    table { border-collapse:collapse; width:100%; margin-bottom:10pt; }
    .info-lbl  { background:#dce8f5; font-weight:bold; text-align:center;
                 width:88pt; color:#1a3a5c; border:0.75pt solid #aaa; padding:5pt 8pt; }
    .info-val  { border:0.75pt solid #aaa; padding:5pt 8pt; font-size:11pt; }
  </style>
</head>
<body>
  <h1>心理咨询结案报告</h1>
  <p class="doc-sub">（A4 打印版 · 请在打印前核对所有信息）</p>
  <table>
    <colgroup>
      <col style="width:90pt" />
      <col />
      <col style="width:90pt" />
      <col />
    </colgroup>
    <tr>
      <td class="info-lbl">来访者学号</td><td class="info-val">${escHtml(report.studentNo)}</td>
      <td class="info-lbl">来访者姓名</td><td class="info-val">${escHtml(report.studentName)}</td>
    </tr>
    <tr>
      <td class="info-lbl">来访者性别</td><td class="info-val">${escHtml(report.gender)}</td>
      <td class="info-lbl">来访者院系</td><td class="info-val">${escHtml(report.department)}</td>
    </tr>
    <tr>
      <td class="info-lbl">来访者联系电话</td>
      <td class="info-val" colspan="3">${escHtml(report.phone)}</td>
    </tr>
    <tr>
      <td class="info-lbl">问题类型</td><td class="info-val">${escHtml(report.problemType)}</td>
      <td class="info-lbl"><b>咨询总次数</b></td><td class="info-val"><b>${report.totalTimes ?? ''}</b></td>
    </tr>
    <tr>
      <td class="info-lbl">主要咨询师</td><td class="info-val">${escHtml(report.counselorName)}</td>
      <td class="info-lbl"><b>咨询日期</b></td>
      <td class="info-val"><b>${escHtml(report.startDate)} ～ ${escHtml(report.endDate)}</b></td>
    </tr>
    <tr>
      <td class="info-lbl">结案结论</td>
      <td class="info-val" colspan="3" style="min-height:80pt;white-space:pre-wrap;line-height:1.9;vertical-align:top">
        ${escHtml(report.conclusion).replace(/\n/g, '<br>')}
      </td>
    </tr>
  </table>
</body></html>`
}

const doExport = (report) => {
  const blob = new Blob(['\ufeff' + buildWordHtml(report)], {
    type: 'application/msword;charset=utf-8'
  })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `结案报告_${report.studentName || 'unknown'}_${report.endDate || new Date().toISOString().split('T')[0]}.doc`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(a.href)
}

const exportFromDialog = () => {
  if (!reportForm.studentName) return ElMessage.warning('请先填写报告内容')
  exporting.value = true
  try {
    const exportData = { ...reportForm, conclusion: buildConclusion(rptSessionCache.value, counselorSummary.value) }
    doExport(exportData)
    ElMessage.success('Word 文档已下载')
  } finally {
    exporting.value = false
  }
}

const downloadFromList = async (report) => {
  exporting.value = true
  try {
    const res = await axios.get(`${BASE}/counselor/recordList`, {
      params: { counselingId: report.counselingId }
    })
    doExport(report)
    ElMessage.success('Word 文档已下载')
  } catch {
    ElMessage.error('获取咨询记录失败')
  } finally {
    exporting.value = false
  }
}

// ─────────────────── View Report Dialog ───────────────────
const viewDlgVisible = ref(false)
const viewingReport = ref(null)
const viewingRecs = ref([])

const viewReport = async (report) => {
  viewingReport.value = report
  viewDlgVisible.value = true
  try {
    const res = await axios.get(`${BASE}/counselor/recordList`, {
      params: { counselingId: report.counselingId }
    })
    viewingRecs.value = res.data.data || []
  } catch {
    viewingRecs.value = []
  }
}

// ─────────────────── Helpers ───────────────────
const statusTagType = (s) =>
  ({ 完成: 'success', 缺席: 'warning', 结案: 'danger' }[s] ?? 'info')
const timelineType = (s) =>
  ({ 完成: 'success', 缺席: 'warning', 结案: 'danger' }[s] ?? 'primary')
const fmtDate = (dt) =>
  dt ? new Date(dt).toLocaleDateString('zh-CN') : '—'

// ─────────────────── Init ───────────────────
onMounted(() => {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
  loadStudents()
  loadReports()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@300;400;500;700&display=swap');

/* ───── Root ───── */
.ch-home {
  padding: 24px 28px 40px;
  min-height: 100vh;
  background: linear-gradient(148deg, #EAF0FD 0%, #F2F6FF 55%, #EEF4F0 100%);
  font-family: 'Noto Sans SC', 'PingFang SC', '微软雅黑', sans-serif;
  animation: fadeIn 0.4s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ───── Header ───── */
.ch-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  background: #fff;
  padding: 18px 28px;
  border-radius: 16px;
  box-shadow: 0 4px 22px rgba(44, 90, 200, 0.09), 0 1px 4px rgba(44, 90, 200, 0.05);
  border-left: 5px solid #4C5FC4;
  position: relative;
  overflow: hidden;
}

.header-decor {
  position: absolute;
  right: -24px;
  top: -24px;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(76, 95, 196, 0.07) 0%, rgba(76, 95, 196, 0.01) 70%);
  pointer-events: none;
}

.ch-title-group {
  display: flex;
  align-items: center;
  gap: 16px;
}

.ch-icon-wrap {
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #ECEEFF, #D8DCFF);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 10px rgba(76, 95, 196, 0.18);
}

.ch-icon { font-size: 24px; }

.ch-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1a3a5c;
  letter-spacing: 0.5px;
  line-height: 1.3;
}

.ch-subtitle {
  margin: 5px 0 0;
  font-size: 12.5px;
  color: #8093B4;
  font-weight: 300;
}

/* ───── Logout Button ───── */
.logout-btn {
  position: relative;
  z-index: 1;
  color: #E05254 !important;
  border-color: #FFD6D6 !important;
  background: #FFF5F5 !important;
  font-weight: 600;
  font-size: 13px;
  transition: all 0.2s ease;
}
.logout-btn:hover {
  background: #FFE5E5 !important;
  border-color: #E05254 !important;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(224, 82, 84, 0.2) !important;
}

/* ───── Welcome Card ───── */
.welcome-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #EEF3FF 0%, #E8EEFF 50%, #ECF4FF 100%);
  border: 1px solid #D8E2FF;
  border-radius: 16px;
  padding: 20px 32px;
  margin-bottom: 18px;
  box-shadow: 0 2px 14px rgba(76, 95, 196, 0.08);
}

.welcome-left {
  display: flex;
  align-items: center;
  gap: 18px;
}

.avatar-circle {
  width: 54px;
  height: 54px;
  background: linear-gradient(135deg, #7B98E8, #4C5FC4);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  flex-shrink: 0;
  box-shadow: 0 4px 14px rgba(76, 95, 196, 0.28);
}

.welcome-info { display: flex; flex-direction: column; gap: 6px; }

.welcome-name {
  font-size: 16px;
  font-weight: 700;
  color: #1A2B50;
  display: flex;
  align-items: center;
  gap: 9px;
}

.role-tag {
  background: linear-gradient(135deg, #4C5FC4, #6B7FD4) !important;
  border-color: transparent !important;
  color: #fff !important;
  font-size: 11px !important;
  border-radius: 6px !important;
  padding: 1px 10px !important;
  font-weight: 600 !important;
}

.welcome-date {
  font-size: 13px;
  color: #7A90B8;
  letter-spacing: 0.2px;
}

.welcome-clock {
  font-size: 38px;
  font-weight: 800;
  color: #4C5FC4;
  letter-spacing: 3px;
  font-variant-numeric: tabular-nums;
  font-feature-settings: 'tnum';
}

/* ───── Main Tabs ───── */
.ch-tabs {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 28px rgba(44, 90, 200, 0.08), 0 1px 4px rgba(44, 90, 200, 0.05);
  overflow: hidden;
}

:deep(.ch-tabs > .el-tabs__header) {
  margin: 0;
  padding: 0 24px;
  background: linear-gradient(to bottom, #F6F8FF, #FFFFFF);
  border-bottom: 2px solid #E4E8FF;
}

:deep(.ch-tabs .el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.ch-tabs .el-tabs__item) {
  font-size: 14px;
  color: #7A88B0;
  font-weight: 500;
  height: 54px;
  line-height: 54px;
  padding: 0 22px;
  transition: color 0.25s ease;
}

:deep(.ch-tabs .el-tabs__item.is-active) {
  color: #4C5FC4;
  font-weight: 700;
}

:deep(.ch-tabs .el-tabs__item:hover) {
  color: #4C5FC4;
}

:deep(.ch-tabs .el-tabs__active-bar) {
  background: linear-gradient(90deg, #4C5FC4, #7A88E8);
  height: 3px;
  border-radius: 2px 2px 0 0;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

/* ───── Tab Section ───── */
.tab-section {
  padding: 20px 24px 28px;
}

/* ───── Toolbar ───── */
.tab-toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 0 16px;
}

.toolbar-tip {
  font-size: 13px;
  color: #8A9BC0;
  background: #F2F5FF;
  padding: 5px 14px;
  border-radius: 20px;
  border: 1px solid #D8DFFF;
}

.tip-count {
  font-weight: 700;
  color: #4C5FC4;
  font-size: 15px;
}

/* ───── Table ───── */
.ch-table {
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #E4E8FF;
}

:deep(.el-table) {
  font-size: 13.5px;
  color: #3D4F72;
}

:deep(.el-table tr:hover > td.el-table__cell) {
  background: #F4F6FF !important;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background: #F8F9FF;
}

:deep(.el-table td.el-table__cell) {
  border-bottom-color: #ECEEFF;
  padding: 12px 0;
}

:deep(.el-table--border .el-table__cell) {
  border-right-color: #ECEEFF;
}

/* ───── Progress ───── */
.progress-wrap { padding: 0 6px; }

:deep(.el-progress-bar__outer) {
  border-radius: 8px;
  background: #E8ECFF;
}

:deep(.el-progress-bar__inner) {
  border-radius: 8px;
  background: linear-gradient(90deg, #4C5FC4, #7A88E8);
}

:deep(.el-progress__text) {
  font-size: 12px !important;
  font-weight: 600;
  color: #4C5FC4;
  min-width: 36px;
}

/* ───── Tags ───── */
:deep(.el-tag) {
  border-radius: 6px;
  font-weight: 500;
}

/* ───── Buttons ───── */
:deep(.el-button--primary) {
  background: linear-gradient(135deg, #4C5FC4 0%, #5E72D8 100%);
  border: none;
  font-weight: 600;
  box-shadow: 0 2px 10px rgba(76, 95, 196, 0.28);
  transition: all 0.22s ease;
}

:deep(.el-button--primary:not(.is-plain):hover) {
  background: linear-gradient(135deg, #3D50B8, #5062CC);
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(76, 95, 196, 0.35);
}

:deep(.el-button--primary.is-plain) {
  color: #4C5FC4;
  background: #F0F2FF;
  border-color: #C8CFEE;
}

:deep(.el-button--primary.is-plain:hover) {
  background: #E4E8FF;
  border-color: #4C5FC4;
  transform: translateY(-1px);
}

:deep(.el-button--warning) {
  background: linear-gradient(135deg, #E8863D, #F0964E);
  border: none;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(232, 134, 61, 0.28);
}

:deep(.el-button--warning.is-plain) {
  color: #D07520;
  background: #FFF5EC;
  border-color: #F0C090;
}

:deep(.el-button--warning.is-plain:hover) {
  background: #FFE8D0;
  border-color: #E8863D;
}

:deep(.el-button--success) {
  background: linear-gradient(135deg, #27AE62, #34C172);
  border: none;
  font-weight: 600;
}

:deep(.el-button--success.is-plain) {
  color: #1E9256;
  background: #EBF8F2;
  border-color: #A0DEC0;
}

:deep(.el-button--success.is-plain:hover) {
  background: #D4F1E4;
  border-color: #27AE62;
}

:deep(.el-button.is-round) {
  border-radius: 20px;
  padding-left: 16px;
  padding-right: 16px;
}

/* ───── Pagination ───── */
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding: 18px 0 0;
}

:deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background: #4C5FC4;
}

:deep(.el-pagination.is-background .el-pager li:not(.is-disabled):hover) {
  color: #4C5FC4;
}

/* ───── Session Record Dialog ───── */
.rec-list-wrap {
  max-height: 340px;
  overflow-y: auto;
  padding-right: 6px;
  scrollbar-width: thin;
  scrollbar-color: #D0D6F5 transparent;
}

.rec-legend {
  display: flex;
  gap: 16px;
  margin-bottom: 14px;
  font-size: 12px;
  color: #6B7EA8;
  background: #F6F8FF;
  padding: 8px 14px;
  border-radius: 8px;
}

.rec-legend span { display: flex; align-items: center; gap: 6px; }

.dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  display: inline-block;
}

.dot.done    { background: linear-gradient(135deg, #27AE62, #34C172); }
.dot.absent  { background: linear-gradient(135deg, #E8963D, #F0A050); }
.dot.closing { background: linear-gradient(135deg, #E05254, #EF6062); }

.rec-timeline { padding-left: 8px; }

.timeline-ts {
  font-size: 12px;
  color: #8A9BC0;
  background: #EEF2FF;
  padding: 2px 8px;
  border-radius: 4px;
}

.rec-card {
  background: linear-gradient(135deg, #F6F8FF, #FAFBFF);
  border: 1px solid #D8DFFF;
  border-radius: 10px;
  padding: 12px 16px;
  transition: box-shadow 0.2s ease;
}

.rec-card:hover {
  box-shadow: 0 4px 14px rgba(76, 95, 196, 0.1);
}

.rec-text {
  margin: 10px 0 0;
  font-size: 13px;
  color: #3D4F72;
  white-space: pre-wrap;
  line-height: 1.75;
}

/* ───── Add Record Form ───── */
.add-rec-form { padding: 0 8px; }

.times-badge {
  background: linear-gradient(135deg, #EEF0FF, #E0E4FF);
  color: #4C5FC4;
  border-radius: 8px;
  padding: 4px 16px;
  font-weight: 700;
  font-size: 15px;
  border: 1px solid #C8CFEE;
}

.form-hint { margin-left: 8px; font-size: 12px; color: #909399; }
.danger-hint { color: #E05254; margin-left: 10px; font-size: 12px; }

/* ───── Report Form ───── */
.rpt-form { padding: 0 8px; }

.form-section-title {
  font-size: 13.5px;
  font-weight: 700;
  color: #4C5FC4;
  padding: 8px 0 12px;
  border-bottom: 2px solid #DDE3FF;
  margin: 16px 0 18px;
  letter-spacing: 0.3px;
}

.section-hint {
  font-size: 12px;
  font-weight: 400;
  color: #909399;
  margin-left: 8px;
}

.conclusion-textarea { font-size: 13px; line-height: 1.8; }

/* ───── Conclusion Records ───── */
.conclusion-records-readonly {
  background: linear-gradient(135deg, #F4F6FF, #F8FAFF);
  border: 1px solid #C8CFEE;
  border-radius: 10px;
  padding: 14px 18px;
  margin-bottom: 0;
  max-height: 260px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: #C8CFEE transparent;
}

.crr-item {
  margin-bottom: 14px;
  line-height: 1.8;
  padding-bottom: 12px;
  border-bottom: 1px dashed #D8DFFF;
}

.crr-item:last-child { margin-bottom: 0; border-bottom: none; padding-bottom: 0; }

.crr-badge {
  display: block;
  font-weight: 700;
  font-size: 13px;
  color: #4C5FC4;
  margin-bottom: 4px;
}

.crr-text {
  font-size: 13px;
  color: #444;
  white-space: pre-wrap;
  padding-left: 4px;
  line-height: 1.7;
}

.conclusion-divider {
  border-top: 2px solid #C8CFEE;
  margin: 16px 0 12px;
  border-radius: 2px;
}

.summary-label {
  font-size: 13px;
  font-weight: 700;
  color: #4C5FC4;
  margin-bottom: 10px;
  padding-left: 2px;
}

/* ───── Report Footer ───── */
.rpt-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.rpt-footer-right { display: flex; gap: 10px; }

/* ───── View Dialog ───── */
.view-content {
  padding: 4px 8px;
  max-height: 78vh;
  overflow-y: auto;
  scrollbar-width: thin;
}

.report-doc-title {
  text-align: center;
  font-size: 22px;
  font-weight: 700;
  font-family: '黑体', SimHei, sans-serif;
  color: #1a3a5c;
  letter-spacing: 3px;
  margin: 8px 0 20px;
  padding-bottom: 14px;
  border-bottom: 2.5px solid #4C5FC4;
}

.report-info-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 18px;
  font-size: 13.5px;
  border-radius: 8px;
  overflow: hidden;
}

.report-info-table td {
  border: 1px solid #C8CFEE;
  padding: 10px 13px;
  line-height: 1.6;
  vertical-align: middle;
}

.r-lbl {
  background: linear-gradient(135deg, #E8EBFF, #EEF0FF);
  font-weight: 700;
  color: #4C5FC4;
  text-align: center;
  white-space: nowrap;
}

.r-val { color: #2E3A58; }
.r-bold { font-weight: 700; color: #1a3a5c; }
.r-conclusion { vertical-align: top; padding: 13px 15px; }

.conclusion-text {
  min-height: 100px;
  white-space: pre-wrap;
  line-height: 1.9;
  color: #333;
  font-size: 13px;
}

.conc-badge-line {
  font-weight: 700;
  color: #4C5FC4;
  font-size: 13px;
  margin-top: 10px;
  line-height: 1.9;
}

.conc-badge-line:first-child { margin-top: 0; }

.conc-sep-line {
  border-top: 1.5px solid #C8CFEE;
  margin: 10px 0 8px;
  overflow: hidden;
  font-size: 0;
  height: 0;
}

.conc-plain-line {
  font-size: 13px;
  color: #444;
  line-height: 1.9;
  padding-left: 4px;
}

.conc-empty { color: #bbb; font-style: italic; }

/* ───── Collapse ───── */
.rec-collapse {
  margin-top: 4px;
  border: 1px solid #D8DFFF;
  border-radius: 10px;
  overflow: hidden;
}

.collapse-title {
  font-size: 13px;
  font-weight: 600;
  color: #4C5FC4;
}

.view-timeline { margin-top: 8px; }

.view-rec-card {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  flex-wrap: wrap;
}

.view-rec-text {
  font-size: 13px;
  color: #555;
  white-space: pre-wrap;
  line-height: 1.65;
  flex: 1;
}
</style>
