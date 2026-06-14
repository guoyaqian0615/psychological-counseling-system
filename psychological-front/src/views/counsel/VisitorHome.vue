<template>
  <div class="visitor-container">

    <!-- ─── Page Header ─── -->
    <header class="page-header">
      <div class="header-inner">
        <div class="header-icon-wrap">
          <span class="header-icon">📋</span>
        </div>
        <div class="header-text">
          <h1 class="page-title">初访工作台</h1>
          <p class="page-subtitle">初访任务领取 &nbsp;·&nbsp; 历史档案查阅</p>
        </div>
        <div class="header-decor"></div>
        <el-button class="logout-btn" round @click="handleLogout">
          <el-icon style="margin-right:4px"><SwitchButton /></el-icon>退出登录
        </el-button>
      </div>
    </header>

    <!-- ─── Welcome Card ─── -->
    <div class="welcome-card">
      <div class="welcome-left">
        <div class="avatar-circle">{{ avatarChar }}</div>
        <div class="welcome-info">
          <div class="welcome-name">
            欢迎回来，{{ displayName }}
            <el-tag size="small" class="role-tag">初访员</el-tag>
          </div>
          <div class="welcome-date">{{ currentDate }}</div>
        </div>
      </div>
      <div class="welcome-clock">{{ currentTime }}</div>
    </div>

    <!-- ─── Main Content Card ─── -->
    <div class="main-card">
      <el-tabs v-model="activeTab" class="app-tabs">

        <!-- 待处理预约 -->
        <el-tab-pane name="pending">
          <template #label>
            <span class="tab-label-wrap">
              <span class="tab-dot dot-primary"></span>待处理预约
            </span>
          </template>

          <div class="tab-section">
            <div class="filter-bar">
              <el-date-picker
                v-model="visitDate"
                type="date"
                placeholder="按预约日期筛选"
                value-format="YYYY-MM-DD"
                style="width: 200px;"
                @change="getPending"
              />
              <el-radio-group v-model="pendingMode" @change="getPending">
                <el-radio-button value="mine">我的待处理</el-radio-button>
              </el-radio-group>
              <el-button type="primary" size="small" :icon="Refresh" @click="getPending">刷新列表</el-button>
            </div>

            <el-table
              :data="pendingList"
              style="width:100%"
              v-loading="loadingPending"
              :header-cell-style="headerCellStyle"
              stripe
              class="app-table"
            >
              <el-table-column prop="studentName" label="学生姓名" width="100" />
              <el-table-column prop="studentId"   label="学号" width="130" />
              <el-table-column prop="questionnaireScore" label="问卷得分" align="center" width="90" />
              <el-table-column prop="visitDate"   label="预约日期" width="120" />
              <el-table-column prop="visitTime"   label="预约时间" width="120" />
              <el-table-column prop="location"    label="初访地点" />
              <el-table-column label="操作" width="130" align="center" fixed="right">
                <template #default="scope">
                  <el-button
                    v-if="!scope.row.visitorId"
                    type="success"
                    size="small"
                    round
                    @click="assignVisit(scope.row.id)"
                  >领取任务</el-button>
                  <el-button
                    v-else
                    type="primary"
                    size="small"
                    round
                    @click="openDialog(scope.row)"
                  >提交结果</el-button>
                </template>
              </el-table-column>
              <template #empty>
                <div class="empty-state">
                  <div class="empty-icon">📭</div>
                  <p>暂无待处理预约</p>
                </div>
              </template>
            </el-table>
          </div>
        </el-tab-pane>

        <!-- 历史记录 -->
        <el-tab-pane name="history">
          <template #label>
            <span class="tab-label-wrap">
              <span class="tab-dot dot-teal"></span>我的历史记录
            </span>
          </template>

          <div class="tab-section">
            <div class="filter-bar">
              <el-input
                v-model="studentName"
                placeholder="输入学生姓名模糊查询"
                style="width: 220px;"
                @keyup.enter="getHistory"
                clearable
              />
              <el-button type="primary" size="small" @click="getHistory">查询</el-button>
              <el-button v-if="isSearch" size="small" @click="resetBack" plain>重置</el-button>
            </div>

            <el-table
              :data="historyList"
              style="width:100%"
              v-loading="loadingHistory"
              :header-cell-style="headerCellStyle"
              stripe
              class="app-table"
            >
              <el-table-column prop="studentName" label="学生姓名" width="110" />
              <el-table-column prop="crisisLevel"  label="危机等级" align="center" width="100">
                <template #default="scope">
                  <el-tag
                    :type="scope.row.crisisLevel === '高' ? 'danger' : scope.row.crisisLevel === '中' ? 'warning' : 'success'"
                    effect="light"
                    size="small"
                    round
                  >{{ scope.row.crisisLevel }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="problemType" label="问题类型" />
              <el-table-column prop="conclusion"  label="结论">
                <template #default="scope">
                  <el-tag
                    :type="scope.row.conclusion === '转介送诊' ? 'danger' : scope.row.conclusion === '安排咨询' ? 'primary' : 'info'"
                    effect="plain"
                    size="small"
                  >{{ scope.row.conclusion }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100" align="center" fixed="right">
                <template #default="scope">
                  <el-button type="primary" link size="small" @click="viewDetail(scope.row)">查看详情</el-button>
                </template>
              </el-table-column>
              <template #empty>
                <div class="empty-state">
                  <div class="empty-icon">📂</div>
                  <p>暂无历史记录</p>
                </div>
              </template>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- ─── 提交初访结果弹窗 ─── -->
    <el-dialog v-model="dialogVisible" title="提交初访结果" width="580px" draggable class="app-dialog">
      <el-form :model="form" label-width="100px" :rules="formRules" ref="formRef">
        <el-form-item label="学生姓名">
          <el-input v-model="form.studentName" disabled />
        </el-form-item>
        <el-form-item label="学号">
          <el-input v-model="form.studentId" disabled />
        </el-form-item>
        <el-form-item label="初访地点">
          <el-input v-model="form.location" disabled />
        </el-form-item>
        <el-form-item label="危机等级" prop="crisisLevel">
          <el-select v-model="form.crisisLevel" placeholder="请选择危机等级" style="width:100%">
            <el-option label="低" value="低" />
            <el-option label="中" value="中" />
            <el-option label="高" value="高" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题类型" prop="problemType">
          <el-select v-model="form.problemType" placeholder="请选择问题类型" style="width:100%">
            <el-option label="情绪压力" value="情绪压力" />
            <el-option label="人际关系" value="人际关系" />
            <el-option label="学业压力" value="学业压力" />
            <el-option label="家庭问题" value="家庭问题" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="初访结论" prop="conclusion">
          <el-select v-model="form.conclusion" placeholder="请选择初访结论" style="width:100%">
            <el-option label="无需咨询" value="无需咨询" />
            <el-option label="安排咨询" value="安排咨询" />
            <el-option label="转介送诊" value="转介送诊" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定提交</el-button>
      </template>
    </el-dialog>

    <!-- ─── 查看详情弹窗 ─── -->
    <el-dialog v-model="detailVisible" title="初访记录详情" width="560px" draggable class="app-dialog">
      <el-descriptions :column="1" border class="detail-desc">
        <el-descriptions-item label="学生姓名">{{ detailForm.studentName }}</el-descriptions-item>
        <el-descriptions-item label="学号">{{ detailForm.studentId }}</el-descriptions-item>
        <el-descriptions-item label="初访地点">{{ detailForm.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="危机等级">
          <el-tag
            :type="detailForm.crisisLevel === '高' ? 'danger' : detailForm.crisisLevel === '中' ? 'warning' : 'success'"
            size="small" round
          >{{ detailForm.crisisLevel }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="问题类型">{{ detailForm.problemType }}</el-descriptions-item>
        <el-descriptions-item label="初访结论">{{ detailForm.conclusion }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, SwitchButton } from '@element-plus/icons-vue'
import axios from 'axios'
import dayjs from 'dayjs'

const userInfo = JSON.parse(localStorage.getItem('user') || '{}')
const visitorId = ref(userInfo.id || '')
const baseUrl = 'http://localhost:8080/counsel/visitor'

const router = useRouter()

// ─── Welcome Card ──────────────────────────────────────────
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

const activeTab = ref('pending')

// 待处理列表
const pendingList    = ref([])
const loadingPending = ref(false)
const visitDate      = ref('')
const pendingMode    = ref('unassigned')

// 历史记录
const historyList    = ref([])
const loadingHistory = ref(false)
const studentName    = ref('')
const originHistoryList = ref([])
const isSearch       = ref(false)

// 弹窗
const dialogVisible = ref(false)
const detailVisible = ref(false)
const formRef       = ref(null)
const detailForm    = ref({})

// 提交表单
const form = ref({
  firstVisitId: '',
  crisisLevel:  '',
  problemType:  '',
  conclusion:   '',
  studentName:  '',
  studentId:    '',
  location:     '',
})

const formRules = reactive({
  crisisLevel: [{ required: true, message: '请选择危机等级', trigger: 'change' }],
  problemType: [{ required: true, message: '请选择问题类型', trigger: 'change' }],
  conclusion:  [{ required: true, message: '请选择初访结论', trigger: 'change' }],
})

const headerCellStyle = {
  background: 'linear-gradient(135deg, #EEF4FF 0%, #F4F8FF 100%)',
  color: '#1C2E5A',
  fontWeight: '700',
  fontSize: '13px',
  padding: '14px 0',
}

// ===================== 接口请求 =====================

const getPending = async () => {
  loadingPending.value = true
  try {
    const params = {}
    if (pendingMode.value === 'mine' && visitorId.value) {
      params.visitorId = visitorId.value
    }
    if (visitDate.value) params.visitDate = visitDate.value

    const res = await axios.get(`${baseUrl}/waitList`, { params })
    pendingList.value = res.data.data || []
  } catch (e) {
    ElMessage.error('获取待处理列表失败：' + (e.response?.data?.msg || e.message))
    pendingList.value = []
  } finally {
    loadingPending.value = false
  }
}

const assignVisit = async (firstVisitId) => {
  try {
    await ElMessageBox.confirm('确认领取该初访任务？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText:  '取消',
      type: 'warning',
    })
    await axios.post(`${baseUrl}/assignVisit`, null, {
      params: { firstVisitId, visitorId: visitorId.value },
    })
    ElMessage.success('领取任务成功')
    getPending()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('领取任务失败：' + (e.response?.data?.msg || e.message))
    }
  }
}

const getHistory = async () => {
  loadingHistory.value = true
  try {
    const params = { visitorId: visitorId.value }
    if (studentName.value) params.studentName = studentName.value

    const res = await axios.get(`${baseUrl}/myHistory`, { params })
    historyList.value = res.data.data || []

    if (!studentName.value) {
      originHistoryList.value = [...historyList.value]
      isSearch.value = false
    } else {
      isSearch.value = true
    }
  } catch (e) {
    ElMessage.error('获取历史记录失败：' + (e.response?.data?.msg || e.message))
    historyList.value = []
  } finally {
    loadingHistory.value = false
  }
}

const viewDetail = (row) => {
  detailForm.value = { ...row }
  detailVisible.value = true
}

const resetBack = () => {
  studentName.value  = ''
  historyList.value  = [...originHistoryList.value]
  isSearch.value     = false
}

const openDialog = (row) => {
  formRef.value?.resetFields()
  form.value = {
    firstVisitId: row.id,
    crisisLevel:  '',
    problemType:  '',
    conclusion:   '',
    studentName:  row.studentName || '',
    studentId:    row.studentId   || '',
    location:     row.location    || '',
  }
  dialogVisible.value = true
}

const submitForm = async () => {
  try {
    await formRef.value.validate()
  } catch {
    ElMessage.warning('请完善必填项后提交')
    return
  }
  try {
    const payload = {
      firstVisitId: form.value.firstVisitId,
      crisisLevel:  form.value.crisisLevel,
      problemType:  form.value.problemType,
      conclusion:   form.value.conclusion,
    }
    await axios.post(`${baseUrl}/submitResult`, payload)
    ElMessage.success('提交初访结果成功')
    dialogVisible.value = false
    getPending()
    getHistory()
  } catch (e) {
    ElMessage.error('提交失败：' + (e.response?.data?.msg || e.message))
  }
}

onMounted(() => {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
  getPending()
  getHistory()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@300;400;500;700&display=swap');

/* ───── Root Container ───── */
.visitor-container {
  font-family: 'Noto Sans SC', 'PingFang SC', '微软雅黑', sans-serif;
  min-height: 100vh;
  background: linear-gradient(148deg, #EAF1FD 0%, #F2F7FF 55%, #EAF6F5 100%);
  padding: 24px 28px 40px;
  animation: fadeIn 0.4s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ───── Page Header ───── */
.page-header {
  margin-bottom: 14px;
}

.header-inner {
  display: flex;
  align-items: center;
  gap: 18px;
  background: #fff;
  padding: 18px 28px;
  border-radius: 16px;
  box-shadow: 0 4px 22px rgba(44, 90, 200, 0.09), 0 1px 4px rgba(44, 90, 200, 0.06);
  border-left: 5px solid #3B6CD4;
  position: relative;
  overflow: hidden;
}

/* ───── Logout Button ───── */
.logout-btn {
  position: relative;
  z-index: 1;
  margin-left: auto;
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
  box-shadow: 0 2px 14px rgba(59, 108, 212, 0.08);
}

.welcome-left {
  display: flex;
  align-items: center;
  gap: 18px;
}

.avatar-circle {
  width: 54px;
  height: 54px;
  background: linear-gradient(135deg, #7B98E8, #3B6CD4);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  flex-shrink: 0;
  box-shadow: 0 4px 14px rgba(59, 108, 212, 0.28);
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
  background: linear-gradient(135deg, #3B6CD4, #5B8CE8) !important;
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
  color: #3B6CD4;
  letter-spacing: 3px;
  font-variant-numeric: tabular-nums;
  font-feature-settings: 'tnum';
}

.header-decor {
  position: absolute;
  right: -20px;
  top: -20px;
  width: 110px;
  height: 110px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(59, 108, 212, 0.06), rgba(66, 178, 170, 0.08));
  pointer-events: none;
}

.header-icon-wrap {
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #EEF4FF, #DAE8FF);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(59, 108, 212, 0.15);
}

.header-icon {
  font-size: 24px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1A2B50;
  letter-spacing: 0.5px;
  line-height: 1.3;
}

.page-subtitle {
  margin: 5px 0 0;
  font-size: 12.5px;
  color: #8093B4;
  font-weight: 300;
  letter-spacing: 0.3px;
}

/* ───── Main Card ───── */
.main-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 28px rgba(44, 90, 200, 0.08), 0 1px 4px rgba(44, 90, 200, 0.05);
  overflow: hidden;
}

/* ───── Tabs ───── */
:deep(.el-tabs__header) {
  margin: 0;
  padding: 0 24px;
  background: linear-gradient(to bottom, #F8FAFF, #FFFFFF);
  border-bottom: 2px solid #E6EFFE;
}

:deep(.el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.el-tabs__item) {
  font-size: 14px;
  color: #8093B4;
  font-weight: 500;
  padding: 0 22px;
  height: 52px;
  line-height: 52px;
  transition: color 0.25s ease;
}

:deep(.el-tabs__item.is-active) {
  color: #3B6CD4;
  font-weight: 700;
}

:deep(.el-tabs__item:hover) {
  color: #3B6CD4;
}

:deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, #3B6CD4, #46B3A9);
  height: 3px;
  border-radius: 2px 2px 0 0;
}

.tab-label-wrap {
  display: flex;
  align-items: center;
  gap: 7px;
}

.tab-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  display: inline-block;
  transition: transform 0.2s;
}

.dot-primary { background: #3B6CD4; }
.dot-teal    { background: #46B3A9; }

:deep(.el-tabs__item.is-active) .tab-dot {
  transform: scale(1.3);
}

/* ───── Tab Section Padding ───── */
.tab-section {
  padding: 20px 24px 28px;
}

/* ───── Filter Bar ───── */
.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 16px;
  padding: 14px 18px;
  background: linear-gradient(135deg, #F7FAFF, #F0F8F8);
  border-radius: 12px;
  border: 1px solid #E2EDFD;
}

:deep(.filter-bar .el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #D2E2F8;
}

:deep(.filter-bar .el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #3B6CD4;
}

:deep(.el-date-editor.el-input) {
  --el-date-editor-width: 200px;
}

:deep(.el-radio-button__inner) {
  border-color: #C8DBF5;
  color: #5A6E96;
  font-size: 13px;
  border-radius: 8px;
  padding: 6px 16px;
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #3B6CD4, #4A80E4);
  border-color: #3B6CD4;
  box-shadow: none;
}

/* ───── Table ───── */
.app-table {
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #E2EDFD;
}

:deep(.el-table) {
  font-size: 13.5px;
  color: #3D4F72;
}

:deep(.el-table .el-table__row) {
  transition: background 0.15s ease;
}

:deep(.el-table tr:hover > td.el-table__cell) {
  background: #F4F8FF !important;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background: #F9FBFF;
}

:deep(.el-table td.el-table__cell) {
  border-bottom-color: #EEF3FF;
  padding: 12px 0;
}

:deep(.el-table--border .el-table__cell) {
  border-right-color: #EEF3FF;
}

:deep(.el-table--border::after),
:deep(.el-table--border::before) {
  background-color: #E2EDFD;
}

/* ───── Buttons ───── */
:deep(.el-button--primary) {
  background: linear-gradient(135deg, #3B6CD4 0%, #4A7EE8 100%);
  border: none;
  font-weight: 600;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(59, 108, 212, 0.28);
  transition: all 0.22s ease;
}

:deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #2D5FC8, #3B70D8);
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(59, 108, 212, 0.35);
}

:deep(.el-button--success) {
  background: linear-gradient(135deg, #27AE62, #34C172);
  border: none;
  font-weight: 600;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(39, 174, 98, 0.28);
  transition: all 0.22s ease;
}

:deep(.el-button--success:hover) {
  background: linear-gradient(135deg, #20A055, #2DB666);
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(39, 174, 98, 0.35);
}

:deep(.el-button.is-plain) {
  border-radius: 8px;
}

:deep(.el-button--primary.is-link) {
  background: transparent;
  box-shadow: none;
  color: #3B6CD4;
  font-weight: 500;
}

:deep(.el-button--primary.is-link:hover) {
  color: #2D5FC8;
  transform: none;
  box-shadow: none;
  text-decoration: underline;
}

/* ───── Tags ───── */
:deep(.el-tag) {
  border-radius: 6px;
  font-weight: 500;
}

/* ───── Empty State ───── */
.empty-state {
  padding: 32px 0;
  text-align: center;
  color: #98AAC8;
}

.empty-icon {
  font-size: 36px;
  margin-bottom: 10px;
}

.empty-state p {
  font-size: 14px;
  margin: 0;
}

/* ───── Select / Input overrides ───── */
:deep(.el-select .el-input__wrapper) {
  border-radius: 8px;
}

:deep(.el-input.is-disabled .el-input__wrapper) {
  background: #F5F8FF;
}
</style>
