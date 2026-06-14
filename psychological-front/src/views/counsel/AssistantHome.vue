<template>
  <div class="assistant-container">

    <!-- ─── Page Header ─── -->
    <header class="page-header">
      <div class="header-inner">
        <div class="header-icon-wrap">
          <span class="header-icon">🗓️</span>
        </div>
        <div class="header-text">
          <h1 class="page-title">心理助理工作台</h1>
          <p class="page-subtitle">咨询安排协调 &nbsp;·&nbsp; 日程管理</p>
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
            <el-tag size="small" class="role-tag">心理助理</el-tag>
          </div>
          <div class="welcome-date">{{ currentDate }}</div>
        </div>
      </div>
      <div class="welcome-clock">{{ currentTime }}</div>
    </div>

    <!-- ─── Main Content Card ─── -->
    <div class="main-card">
      <el-tabs v-model="activeTab" class="app-tabs">

        <!-- ① 预约列表 -->
        <el-tab-pane name="visitList">
          <template #label>
            <span class="tab-label-wrap">
              <span class="tab-dot dot-blue"></span>待安排预约
            </span>
          </template>

          <div class="tab-section">
            <el-table
              :data="visitList"
              style="width:100%"
              v-loading="loadingVisit"
              :header-cell-style="headerCellStyle"
              stripe
              class="app-table"
            >
              <el-table-column prop="studentName" label="学生姓名" width="100" />
              <el-table-column prop="studentId"   label="学号" width="130" />
              <el-table-column prop="problemType" label="问题类型" />
              <el-table-column prop="crisisLevel" label="危机等级" align="center" width="100">
                <template #default="scope">
                  <el-tag
                    :type="scope.row.crisisLevel === '高' ? 'danger' : scope.row.crisisLevel === '中' ? 'warning' : 'success'"
                    effect="light" size="small" round
                  >{{ scope.row.crisisLevel }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="conclusion" label="初访结论">
                <template #default="scope">
                  <el-tag type="primary" effect="plain" size="small">{{ scope.row.conclusion }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120" align="center" fixed="right">
                <template #default="scope">
                  <el-button type="primary" size="small" round @click="openArrangeDialog(scope.row)">
                    安排咨询
                  </el-button>
                </template>
              </el-table-column>
              <template #empty>
                <div class="empty-state">
                  <div class="empty-icon">✅</div>
                  <p>暂无待安排数据</p>
                </div>
              </template>
            </el-table>
          </div>
        </el-tab-pane>

        <!-- ② 咨询安排记录 -->
        <el-tab-pane name="counselList">
          <template #label>
            <span class="tab-label-wrap">
              <span class="tab-dot dot-teal"></span>咨询安排记录
            </span>
          </template>

          <div class="tab-section">
            <el-table
              :data="counselList"
              style="width:100%"
              v-loading="loadingCounsel"
              :header-cell-style="headerCellStyle"
              stripe
              class="app-table"
            >
              <el-table-column prop="studentName"    label="学生姓名" width="100" />
              <el-table-column prop="counselorName"  label="咨询师" width="100" />
              <el-table-column prop="status"         label="咨询状态" align="center" width="100">
                <template #default="scope">
                  <el-tag
                    :type="scope.row.status === '已结案' ? 'info' : 'success'"
                    effect="light" size="small" round
                  >{{ scope.row.status }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="totalWeeks"     label="总次数" align="center" width="80" />
              <el-table-column prop="counselingTime" label="咨询时间" min-width="160" show-overflow-tooltip />
              <el-table-column prop="location"       label="咨询地点" width="130" show-overflow-tooltip />
              <el-table-column label="操作" width="130" align="center" fixed="right">
                <template #default="scope">
                  <el-button
                    type="primary"
                    link
                    size="small"
                    :disabled="scope.row.status === '已结案'"
                    @click="openEditDialog(scope.row)"
                  >修改</el-button>
                  <el-divider direction="vertical" />
                  <el-button type="danger" link size="small" @click="deleteCounsel(scope.row.id)">删除</el-button>
                </template>
              </el-table-column>
              <template #empty>
                <div class="empty-state">
                  <div class="empty-icon">📋</div>
                  <p>暂无咨询安排</p>
                </div>
              </template>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- ═══════════ 安排咨询弹窗 ═══════════ -->
    <el-dialog v-model="arrangeDialogVisible" title="安排咨询" width="600px" @closed="resetArrangeDialog" class="app-dialog">
      <el-form :model="arrangeForm" label-width="100px">
        <div class="dialog-section-title">学生信息</div>
        <el-form-item label="学生姓名">
          <el-input v-model="arrangeForm.studentName" disabled />
        </el-form-item>
        <el-form-item label="学号">
          <el-input v-model="arrangeForm.studentId" disabled />
        </el-form-item>
        <el-form-item label="问题类型">
          <el-input v-model="arrangeForm.problemType" disabled />
        </el-form-item>
        <el-form-item label="危机等级">
          <el-input v-model="arrangeForm.crisisLevel" disabled />
        </el-form-item>
        <el-form-item label="初访结论">
          <el-input v-model="arrangeForm.conclusion" disabled />
        </el-form-item>

        <div class="dialog-section-title" style="margin-top: 8px;">安排信息</div>
        <el-form-item label="咨询师" required>
          <el-select
            v-model="arrangeForm.counselorId"
            @change="handleCounselorChange"
            placeholder="请选择咨询师"
            style="width:100%"
            :loading="loadingFreeTime"
          >
            <el-option
              v-for="item in counselorList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item v-if="counselorSelected && !loadingFreeTime && cycleGroups.length === 0">
          <el-alert
            title="该咨询师今日及以后暂无可用值班时间（无排班或已被占满）"
            type="warning" :closable="false" show-icon
          />
        </el-form-item>

        <el-form-item label="总周数">
          <el-input v-model="arrangeForm.totalWeeks" disabled />
        </el-form-item>

        <el-form-item label="咨询地点">
          <el-input
            v-model="arrangeForm.location"
            placeholder="填写咨询室地点"
            :disabled="cycleGroups.length === 0"
          />
        </el-form-item>

        <el-form-item label="首次咨询日" required>
          <el-select
            v-model="arrangeForm.startDate"
            placeholder="请先选择咨询师"
            style="width:100%"
            :disabled="cycleGroups.length === 0 || loadingFreeTime"
            :loading="loadingFreeTime"
            no-data-text="暂无可用起始日期"
          >
            <el-option
              v-for="c in cycleGroups"
              :key="c.firstDate"
              :label="formatDateLabel(c.firstDate)"
              :value="c.firstDate"
            />
          </el-select>
          <div v-if="cycleGroups.length > 0 && !arrangeForm.startDate" class="field-hint">
            共 {{ cycleGroups.length }} 个排班周期可选，请选择希望开始的第一次咨询日期
          </div>
        </el-form-item>

        <el-form-item v-if="selectedCycle" label="咨询时间段">
          <el-input :value="selectedCycle.timeSlot" disabled style="width:100%" />
          <div class="field-hint">
            后续 7 次由系统按每周{{ selectedCycle.dayLabel }}相同时间自动排期，共 8 次
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="arrangeDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          @click="submitArrange"
          :disabled="!arrangeForm.startDate || !selectedCycle || loadingFreeTime"
        >
          确认安排并通知学生
        </el-button>
      </template>
    </el-dialog>

    <!-- ═══════════ 修改咨询弹窗 ═══════════ -->
    <el-dialog v-model="editDialogVisible" title="修改咨询安排" width="600px" @closed="resetEditDialog" class="app-dialog">
      <el-form :model="editForm" label-width="110px">
        <div class="dialog-section-title">学生信息</div>
        <el-form-item label="学生姓名">
          <el-input v-model="editForm.studentName" disabled />
        </el-form-item>
        <el-form-item label="学号">
          <el-input v-model="editForm.studentNo" disabled />
        </el-form-item>

        <div class="dialog-section-title" style="margin-top: 8px;">修改信息</div>
        <el-form-item label="咨询师" required>
          <el-select
            v-model="editForm.counselorId"
            @change="handleEditCounselorChange"
            placeholder="请选择咨询师"
            style="width:100%"
            :loading="editLoadingFreeTime"
          >
            <el-option
              v-for="item in counselorList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item v-if="editCounselorSelected && !editLoadingFreeTime && editCycleGroups.length === 0">
          <el-alert
            title="该咨询师今日及以后暂无可用值班时间（无排班或已被占满）"
            type="warning" :closable="false" show-icon
          />
        </el-form-item>

        <el-form-item label="总周数">
          <el-input v-model="editForm.totalWeeks" disabled />
        </el-form-item>

        <el-form-item label="咨询地点">
          <el-input v-model="editForm.location" placeholder="填写咨询室地点" />
        </el-form-item>

        <el-form-item label="当前咨询时间">
          <el-input :value="editForm.counselingTime" disabled />
          <div class="field-hint">如需更改时间，请在下方重新选择首次咨询日</div>
        </el-form-item>

        <el-form-item label="新首次咨询日">
          <el-select
            v-model="editForm.startDate"
            placeholder="不选则保留当前时间"
            style="width:100%"
            :disabled="editCycleGroups.length === 0 || editLoadingFreeTime"
            :loading="editLoadingFreeTime"
            no-data-text="暂无可用起始日期"
            clearable
          >
            <el-option
              v-for="c in editCycleGroups"
              :key="c.firstDate"
              :label="formatDateLabel(c.firstDate)"
              :value="c.firstDate"
            />
          </el-select>
          <div v-if="editCycleGroups.length > 0 && !editForm.startDate" class="field-hint">
            共 {{ editCycleGroups.length }} 个排班周期可选
          </div>
        </el-form-item>

        <el-form-item v-if="editSelectedCycle" label="新咨询时间段">
          <el-input :value="editSelectedCycle.timeSlot" disabled style="width:100%" />
          <div class="field-hint">
            后续 7 次由系统按每周{{ editSelectedCycle.dayLabel }}相同时间自动排期，共 8 次
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          @click="submitEdit"
          :disabled="!editForm.counselorId || editLoadingFreeTime"
        >
          保存并发送通知
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { SwitchButton } from '@element-plus/icons-vue'
import axios from 'axios'

const baseUrl = 'http://localhost:8080/counsel'
const userInfo = JSON.parse(localStorage.getItem('user') || '{}')

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

// ─── 页面级状态 ───────────────────────────────────────────
const activeTab      = ref('visitList')
const visitList      = ref([])
const counselList    = ref([])
const loadingVisit   = ref(false)
const loadingCounsel = ref(false)

// ─── 安排咨询弹窗状态 ─────────────────────────────────────
const arrangeDialogVisible = ref(false)
const loadingFreeTime      = ref(false)
const counselorSelected    = ref(false)
const counselorList        = ref([])
const cycleGroups          = ref([])

const selectedCycle = computed(() =>
  cycleGroups.value.find(c => c.firstDate === arrangeForm.value.startDate) ?? null
)

const arrangeForm = ref({
  firstVisitId:  '',
  studentId:     '',
  studentName:   '',
  problemType:   '',
  crisisLevel:   '',
  conclusion:    '',
  counselorId:   '',
  counselorName: '',
  totalWeeks:    8,
  location:      '',
  startDate:     ''
})

// ─── 修改咨询弹窗状态 ─────────────────────────────────────
const editDialogVisible     = ref(false)
const editLoadingFreeTime   = ref(false)
const editCounselorSelected = ref(false)
const editCycleGroups       = ref([])

const editSelectedCycle = computed(() =>
  editCycleGroups.value.find(c => c.firstDate === editForm.value.startDate) ?? null
)

const editForm = ref({
  id:             '',
  studentName:    '',
  studentNo:      '',
  studentId:      '',
  counselorId:    '',
  counselorName:  '',
  totalWeeks:     8,
  location:       '',
  counselingTime: '',
  startDate:      ''
})

// ─── 工具 ─────────────────────────────────────────────────
const WEEK_NAMES = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
const formatDateLabel = (dateStr) => {
  const d = dayjs(dateStr)
  return `${dateStr}（${WEEK_NAMES[d.day()]}）`
}

// ─── Header Cell Style ────────────────────────────────────
const headerCellStyle = {
  background: 'linear-gradient(135deg, #EEF4FF 0%, #F4F8FF 100%)',
  color: '#1C2E5A',
  fontWeight: '700',
  fontSize: '13px',
  padding: '14px 0',
}

// ─── API ─────────────────────────────────────────────────

const getVisitList = async () => {
  loadingVisit.value = true
  try {
    const { data } = await axios.get(`${baseUrl}/assistant/waitArrange`)
    visitList.value = data.data ?? []
  } catch {
    ElMessage.error('获取预约列表失败')
  } finally {
    loadingVisit.value = false
  }
}

const getCounselList = async () => {
  loadingCounsel.value = true
  try {
    const { data } = await axios.get(`${baseUrl}/assistant/counselList`)
    counselList.value = data.data ?? []
  } catch {
    ElMessage.error('获取咨询列表失败')
  } finally {
    loadingCounsel.value = false
  }
}

const getAllCounselorList = async () => {
  try {
    const { data } = await axios.get(`${baseUrl}/assistant/getCounselorWithDuty`)
    counselorList.value = data.data ?? []
    if (!counselorList.value.length) ElMessage.warning('当前暂无咨询师有可用值班排班')
  } catch {
    ElMessage.error('加载咨询师失败')
  }
}

/** 通用：查询指定咨询师排班周期 */
const loadCycleGroups = async (counselorId) => {
  const { data } = await axios.get(`${baseUrl}/assistant/getFreeTimeByCounsel`, {
    params: { counselorId }
  })
  return data.data.cycleGroups ?? []
}

// ─── 安排咨询 ─────────────────────────────────────────────

const getFreeTimeByCounsel = async (counselorId) => {
  if (!counselorId) return
  loadingFreeTime.value = true
  cycleGroups.value = []
  arrangeForm.value.startDate = ''
  try {
    const groups = await loadCycleGroups(counselorId)
    cycleGroups.value = groups
    if (!groups.length) {
      ElMessage.warning('该咨询师今日及以后暂无可用值班时间')
      return
    }
    if (groups.length === 1) arrangeForm.value.startDate = groups[0].firstDate
  } catch (e) {
    ElMessage.error('加载空闲时间失败')
    console.error(e)
  } finally {
    loadingFreeTime.value = false
  }
}

const handleCounselorChange = (counselorId) => {
  counselorSelected.value = true
  const found = counselorList.value.find(c => c.id === counselorId)
  arrangeForm.value.counselorName = found ? found.name : ''
  getFreeTimeByCounsel(counselorId)
}

const openArrangeDialog = (row) => {
  getAllCounselorList()
  counselorSelected.value = false
  cycleGroups.value = []
  arrangeForm.value = {
    firstVisitId:  row.firstVisitId ?? '',
    studentId:     row.studentId,
    studentName:   row.studentName,
    problemType:   row.problemType,
    crisisLevel:   row.crisisLevel,
    conclusion:    row.conclusion,
    counselorId:   '',
    counselorName: '',
    totalWeeks:    8,
    location:      '',
    startDate:     ''
  }
  arrangeDialogVisible.value = true
}

const resetArrangeDialog = () => {
  counselorSelected.value = false
  cycleGroups.value = []
}

const submitArrange = async () => {
  if (!arrangeForm.value.counselorId) return ElMessage.warning('请选择咨询师')
  if (!arrangeForm.value.startDate)   return ElMessage.warning('请选择首次咨询日期')
  if (!selectedCycle.value)           return ElMessage.warning('未找到对应排班信息，请重新选择')

  const startDate      = dayjs(arrangeForm.value.startDate).format('YYYY-MM-DD')
  const { dayLabel, timeSlot } = selectedCycle.value
  const counselingTime = `${startDate} ${dayLabel} ${timeSlot}`

  try {
    await axios.post(`${baseUrl}/assistant/arrangeCounsel`, {
      ...arrangeForm.value,
      startDate,
      counselingTime
    })
    ElMessage.success('安排成功！已通知学生，共 8 次咨询已记录')
    arrangeDialogVisible.value = false
    await getVisitList()
    await getCounselList()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '安排失败：时间冲突或参数错误')
  }
}

// ─── 修改咨询 ─────────────────────────────────────────────

const handleEditCounselorChange = async (counselorId) => {
  editCounselorSelected.value = true
  editCycleGroups.value = []
  editForm.value.startDate = ''

  const found = counselorList.value.find(c => c.id === counselorId)
  editForm.value.counselorName = found ? found.name : ''

  if (!counselorId) return
  editLoadingFreeTime.value = true
  try {
    const groups = await loadCycleGroups(counselorId)
    editCycleGroups.value = groups
    if (!groups.length) {
      ElMessage.warning('该咨询师今日及以后暂无可用值班时间')
      return
    }
    if (groups.length === 1) editForm.value.startDate = groups[0].firstDate
  } catch (e) {
    ElMessage.error('加载空闲时间失败')
    console.error(e)
  } finally {
    editLoadingFreeTime.value = false
  }
}

const openEditDialog = (row) => {
  getAllCounselorList()
  editCounselorSelected.value = false
  editCycleGroups.value = []
  editForm.value = {
    id:             row.id,
    studentName:    row.studentName,
    studentNo:      row.studentNo ?? '',
    studentId:      row.studentId,
    counselorId:    row.counselorId,
    counselorName:  row.counselorName ?? '',
    totalWeeks:     row.totalWeeks,
    location:       row.location,
    counselingTime: row.counselingTime,
    startDate:      ''
  }
  editDialogVisible.value = true

  if (row.counselorId) {
    editCounselorSelected.value = true
    handleEditCounselorChange(row.counselorId)
  }
}

const resetEditDialog = () => {
  editCounselorSelected.value = false
  editCycleGroups.value = []
}

const submitEdit = async () => {
  if (!editForm.value.counselorId) return ElMessage.warning('请选择咨询师')

  let counselingTime = editForm.value.counselingTime
  let startDate      = editForm.value.startDate || null

  if (startDate && editSelectedCycle.value) {
    const formattedDate = dayjs(startDate).format('YYYY-MM-DD')
    const { dayLabel, timeSlot } = editSelectedCycle.value
    counselingTime = `${formattedDate} ${dayLabel} ${timeSlot}`
  }

  try {
    await axios.post(`${baseUrl}/assistant/updateCounsel`, {
      ...editForm.value,
      startDate,
      counselingTime
    })
    ElMessage.success('修改成功，已发送通知给学生')
    editDialogVisible.value = false
    await getCounselList()
    await getVisitList()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '修改失败：时间冲突或参数错误')
  }
}

// ─── 删除 ──────────────────────────────────────────

const deleteCounsel = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该咨询安排？', '提示')
    await axios.get(`${baseUrl}/assistant/deleteCounsel/${id}`)
    ElMessage.success('删除成功')
    await getCounselList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(() => {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
  getVisitList()
  getCounselList()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@300;400;500;700&display=swap');

/* ───── Root Container ───── */
.assistant-container {
  font-family: 'Noto Sans SC', 'PingFang SC', '微软雅黑', sans-serif;
  min-height: 100vh;
  background: linear-gradient(148deg, #E8F0FD 0%, #F2F7FF 55%, #EBF5F0 100%);
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
  border-left: 5px solid #46B09C;
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
  background: linear-gradient(135deg, #EAF7F4 0%, #E4F5F2 50%, #EBF5FF 100%);
  border: 1px solid #C8EDEA;
  border-radius: 16px;
  padding: 20px 32px;
  margin-bottom: 18px;
  box-shadow: 0 2px 14px rgba(46, 155, 136, 0.08);
}

.welcome-left {
  display: flex;
  align-items: center;
  gap: 18px;
}

.avatar-circle {
  width: 54px;
  height: 54px;
  background: linear-gradient(135deg, #5DC4AF, #2E9B88);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  flex-shrink: 0;
  box-shadow: 0 4px 14px rgba(46, 155, 136, 0.28);
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
  background: linear-gradient(135deg, #2E9B88, #46B09C) !important;
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
  color: #2E9B88;
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
  background: linear-gradient(135deg, rgba(70, 176, 156, 0.07), rgba(59, 108, 212, 0.06));
  pointer-events: none;
}

.header-icon-wrap {
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #E8F8F5, #D0F2EA);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(70, 176, 156, 0.2);
}

.header-icon { font-size: 24px; }

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
  border-bottom: 2px solid #E2EFEA;
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
  color: #2E9B88;
  font-weight: 700;
}

:deep(.el-tabs__item:hover) {
  color: #2E9B88;
}

:deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, #46B09C, #3B6CD4);
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
  transition: transform 0.2s;
}

.dot-blue { background: #3B6CD4; }
.dot-teal { background: #46B09C; }

:deep(.el-tabs__item.is-active) .tab-dot {
  transform: scale(1.4);
}

/* ───── Tab Section ───── */
.tab-section {
  padding: 20px 24px 28px;
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

:deep(.el-table tr:hover > td.el-table__cell) {
  background: #F0FAF7 !important;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background: #F7FDFC;
}

:deep(.el-table td.el-table__cell) {
  border-bottom-color: #EEF8F5;
  padding: 12px 0;
}

:deep(.el-table--border .el-table__cell) {
  border-right-color: #EEF3FF;
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

:deep(.el-button--primary:not(.is-link):hover) {
  background: linear-gradient(135deg, #2D5FC8, #3B70D8);
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(59, 108, 212, 0.35);
}

:deep(.el-button--primary.is-link) {
  background: transparent;
  box-shadow: none;
  color: #3B6CD4;
  font-weight: 500;
}

:deep(.el-button--primary.is-link:not(:disabled):hover) {
  color: #2D5FC8;
  transform: none;
  box-shadow: none;
}

:deep(.el-button--danger.is-link) {
  color: #E05456;
}

:deep(.el-button--danger.is-link:hover) {
  color: #C84042;
}

:deep(.el-button.is-plain) {
  border-radius: 8px;
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

/* ───── Field Hint ───── */
.field-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
  line-height: 1.6;
}

/* ───── Dialog Section Title ───── */
.dialog-section-title {
  font-size: 12.5px;
  font-weight: 700;
  color: #46B09C;
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 4px 0 10px;
  border-bottom: 1.5px solid #D8F2EC;
  margin-bottom: 16px;
}

/* ───── Divider in table actions ───── */
:deep(.el-divider--vertical) {
  border-color: #D0DBF0;
  height: 1em;
  margin: 0 4px;
}

/* ───── Input/Select overrides ───── */
:deep(.el-select .el-input__wrapper) {
  border-radius: 8px;
}

:deep(.el-input.is-disabled .el-input__wrapper) {
  background: #F5F8FF;
}
</style>
