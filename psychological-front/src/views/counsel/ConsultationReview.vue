<template>
  <div class="rv-page">

    <!-- ── Header ── -->
    <div class="rv-header">
      <div class="rv-title-group">
        <span class="rv-icon">📅</span>
        <div>
          <h2 class="rv-title">安排咨询</h2>
          <p class="rv-subtitle">为通过初访的学生安排正式咨询师与时间段</p>
        </div>
      </div>
      <el-button :icon="Refresh" @click="loadList" :loading="loading" type="primary" plain>刷新</el-button>
    </div>

    <!-- ── Table Card ── -->
    <div class="rv-card">
      <div class="rv-stats">
        待安排学生：<strong>{{ visitList.length }}</strong> 人
      </div>

      <el-table
        :data="visitList"
        v-loading="loading"
        :header-cell-style="headerCellStyle"
        stripe
        row-key="id"
        class="rv-table"
      >
        <el-table-column type="index" label="序" width="50" align="center" />
        <el-table-column prop="studentName" label="学生姓名" width="100" />
        <el-table-column prop="studentId" label="学生ID" width="90" />
        <el-table-column prop="grade" label="年级" width="80" />
        <el-table-column prop="major" label="专业" min-width="130" show-overflow-tooltip />
        <el-table-column prop="reason" label="咨询原因" min-width="160" show-overflow-tooltip />
        <el-table-column prop="conclusion" label="初访结论" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="success" size="small" effect="light">{{ row.conclusion }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openArrangeDialog(row)">
              安排咨询
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无待安排的学生" />
        </template>
      </el-table>
    </div>

    <!-- ═══════════════════════════════════════
         安排咨询弹窗
    ═══════════════════════════════════════ -->
    <el-dialog
      v-model="dialogVisible"
      title="安排正式咨询"
      width="660px"
      destroy-on-close
    >
      <div class="dlg-body">

        <!-- Student info card -->
        <div class="stu-card">
          <div class="stu-row">
            <div class="stu-item">
              <span class="stu-lbl">学生姓名</span>
              <span class="stu-val">{{ currentRow?.studentName }}</span>
            </div>
            <div class="stu-item" v-if="currentRow?.grade">
              <span class="stu-lbl">年&nbsp;&nbsp;&nbsp;&nbsp;级</span>
              <span class="stu-val">{{ currentRow.grade }}</span>
            </div>
            <div class="stu-item" v-if="currentRow?.major">
              <span class="stu-lbl">专&nbsp;&nbsp;&nbsp;&nbsp;业</span>
              <span class="stu-val">{{ currentRow.major }}</span>
            </div>
          </div>
          <div class="stu-row" v-if="currentRow?.reason">
            <div class="stu-item full">
              <span class="stu-lbl">咨询原因</span>
              <span class="stu-val reason-text">{{ currentRow.reason }}</span>
            </div>
          </div>
        </div>

        <!-- Steps indicator -->
        <el-steps :active="arrangeStep" align-center class="arrange-steps" finish-status="success">
          <el-step title="选择咨询师" />
          <el-step title="选择时间段" />
          <el-step title="填写地点" />
        </el-steps>

        <el-form :model="arrangeForm" label-width="90px" class="arrange-form">

          <!-- ① Counselor -->
          <div class="section-title">① 选择咨询师</div>
          <el-form-item label="咨询师" required>
            <el-select
              v-model="arrangeForm.counselorId"
              placeholder="请选择（仅显示有排班的咨询师）"
              style="width:100%"
              @change="onCounselorChange"
              :loading="counselorLoading"
            >
              <el-option
                v-for="c in counselorList"
                :key="c.id"
                :label="c.name || c.username"
                :value="c.id"
              >
                <span style="font-weight:600">{{ c.name || c.username }}</span>
                <span style="color:#aaa;font-size:12px;margin-left:8px">ID: {{ c.id }}</span>
              </el-option>
            </el-select>
          </el-form-item>

          <!-- ② Time Slot -->
          <div class="section-title">② 选择咨询时段</div>
          <el-form-item label="时间段" required>
            <div v-if="!arrangeForm.counselorId" class="slot-hint">
              <el-icon><InfoFilled /></el-icon> 请先选择咨询师
            </div>
            <div v-else-if="slotLoading" class="slot-loading">
              <el-icon class="is-loading"><Loading /></el-icon>&nbsp;加载空闲时段中…
            </div>
            <template v-else>
              <div v-if="cycleGroups.length === 0" class="slot-empty">
                <el-icon><WarningFilled /></el-icon>
                该咨询师暂无可用空闲时段，请选择其他咨询师
              </div>
              <div v-else class="slot-grid">
                <div
                  v-for="slot in cycleGroups"
                  :key="slot.firstDate + '|' + slot.timeSlot"
                  class="slot-chip"
                  :class="{ selected: arrangeForm.counselingTime === buildTime(slot) }"
                  @click="selectSlot(slot)"
                >
                  <div class="chip-date">{{ slot.firstDate }}</div>
                  <el-tag type="primary" effect="plain" size="small" class="chip-day">
                    {{ slot.dayLabel }}
                  </el-tag>
                  <div class="chip-time">{{ slot.timeSlot }}</div>
                </div>
              </div>
              <div v-if="arrangeForm.counselingTime" class="chosen-hint">
                <el-icon style="color:#67c23a"><Check /></el-icon>
                已选：{{ arrangeForm.counselingTime }}
              </div>
            </template>
          </el-form-item>

          <!-- ③ Location & Info -->
          <div class="section-title">③ 填写地点</div>
          <el-form-item label="咨询地点" required>
            <el-input
              v-model="arrangeForm.location"
              placeholder="例如：咨询室A、科技楼B204"
            />
          </el-form-item>
          <el-form-item label="咨询次数">
            <el-input :value="'8次（固定）'" disabled style="max-width:200px" />
            <span class="form-tip">· 咨询总次数固定为 8 次</span>
          </el-form-item>
        </el-form>

        <!-- Summary preview -->
        <div class="arrange-preview" v-if="arrangeForm.counselingTime && arrangeForm.location">
          <div class="preview-title">安排预览</div>
          <div class="preview-row">
            <span class="preview-lbl">学生</span>
            <span class="preview-val">{{ arrangeForm.studentName }}</span>
          </div>
          <div class="preview-row">
            <span class="preview-lbl">咨询师</span>
            <span class="preview-val">{{ arrangeForm.counselorName }}</span>
          </div>
          <div class="preview-row">
            <span class="preview-lbl">时间段</span>
            <span class="preview-val">{{ arrangeForm.counselingTime }}</span>
          </div>
          <div class="preview-row">
            <span class="preview-lbl">地点</span>
            <span class="preview-val">{{ arrangeForm.location }}</span>
          </div>
          <div class="preview-row">
            <span class="preview-lbl">次数</span>
            <span class="preview-val">共 8 次</span>
          </div>
          <div class="preview-notice">确认后将自动给学生发送通知</div>
        </div>

      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitArrange" :loading="submitting">
          确认安排并通知学生
        </el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Loading, Check, InfoFilled, WarningFilled } from '@element-plus/icons-vue'
import axios from 'axios'

const BASE = 'http://localhost:8080/counsel'
const headerCellStyle = { background: '#EEF5FE', fontWeight: '600', color: '#1a3a5c' }

// ─── List ─────────────────────────────────────────────────────
const loading = ref(false)
const visitList = ref([])

const loadList = async () => {
  loading.value = true
  try {
    const res = await axios.get(`${BASE}/assistant/waitArrange`)
    visitList.value = res.data.data || []
  } catch {
    ElMessage.error('加载失败，请检查网络')
  } finally {
    loading.value = false
  }
}

// ─── Dialog ──────────────────────────────────────────────────
const dialogVisible = ref(false)
const submitting = ref(false)
const currentRow = ref(null)

const arrangeForm = reactive({
  studentId: null,
  studentName: '',
  counselorId: null,
  counselorName: '',
  counselingTime: '',
  startDate: '',
  location: '',
  totalWeeks: 8,
  status: '进行中'
})

// Step indicator: 0=none, 1=counselor chosen, 2=slot chosen, 3=location filled
const arrangeStep = computed(() => {
  if (!arrangeForm.counselorId) return 0
  if (!arrangeForm.counselingTime) return 1
  if (!arrangeForm.location?.trim()) return 2
  return 3
})

// ─── Counselors & Slots ───────────────────────────────────────
const counselorList = ref([])
const counselorLoading = ref(false)
const slotLoading = ref(false)
const cycleGroups = ref([])

const loadCounselors = async () => {
  counselorLoading.value = true
  try {
    const res = await axios.get(`${BASE}/assistant/getCounselorWithDuty`)
    counselorList.value = res.data.data || []
  } finally {
    counselorLoading.value = false
  }
}

const onCounselorChange = async (counselorId) => {
  const found = counselorList.value.find(c => c.id === counselorId)
  arrangeForm.counselorName = found ? (found.name || found.username) : ''
  arrangeForm.counselingTime = ''
  arrangeForm.startDate = ''
  cycleGroups.value = []
  if (!counselorId) return
  slotLoading.value = true
  try {
    const res = await axios.get(`${BASE}/assistant/getFreeTimeByCounsel`, {
      params: { counselorId }
    })
    cycleGroups.value = res.data.data?.cycleGroups || []
  } catch {
    ElMessage.error('加载空闲时段失败')
  } finally {
    slotLoading.value = false
  }
}

const buildTime = (slot) => `${slot.firstDate} ${slot.dayLabel} ${slot.timeSlot}`

const selectSlot = (slot) => {
  arrangeForm.counselingTime = buildTime(slot)
  arrangeForm.startDate = slot.firstDate
}

// ─── Open Dialog ──────────────────────────────────────────────
const openArrangeDialog = (row) => {
  currentRow.value = row
  Object.assign(arrangeForm, {
    studentId: row.studentId,
    studentName: row.studentName,
    counselorId: null,
    counselorName: '',
    counselingTime: '',
    startDate: '',
    location: '',
    totalWeeks: 8,
    status: '进行中'
  })
  cycleGroups.value = []
  dialogVisible.value = true
  loadCounselors()
}

// ─── Submit ───────────────────────────────────────────────────
const submitArrange = async () => {
  if (!arrangeForm.counselorId) { ElMessage.warning('请先选择咨询师'); return }
  if (!arrangeForm.counselingTime) { ElMessage.warning('请选择咨询时段'); return }
  if (!arrangeForm.location?.trim()) { ElMessage.warning('请填写咨询地点'); return }

  submitting.value = true
  try {
    await axios.post(`${BASE}/assistant/arrangeCounsel`, { ...arrangeForm })
    ElMessage.success('安排成功！已发送通知给学生')
    dialogVisible.value = false
    loadList()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '安排失败，请重试')
  } finally {
    submitting.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
/* ── Page ── */
.rv-page {
  padding: 20px 24px;
  min-height: 100vh;
  background: #F7FAFD;
  font-family: 'PingFang SC', '微软雅黑', sans-serif;
}

/* ── Header ── */
.rv-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.rv-title-group { display: flex; align-items: center; gap: 12px; }
.rv-icon { font-size: 32px; }
.rv-title { margin: 0; font-size: 20px; font-weight: 700; color: #1a3a5c; }
.rv-subtitle { margin: 2px 0 0; font-size: 13px; color: #6e8ea6; }

/* ── Card ── */
.rv-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 14px rgba(42, 80, 130, .07);
  padding: 20px;
}
.rv-stats { margin-bottom: 14px; font-size: 13px; color: #6e8ea6; }
.rv-stats strong { font-size: 16px; color: #1a3a5c; }
.rv-table { border-radius: 8px; overflow: hidden; }

/* ── Dialog body ── */
.dlg-body { padding: 0 4px; }

.stu-card {
  background: #f8fafd;
  border: 1px solid #e0eaf8;
  border-radius: 8px;
  padding: 12px 18px;
  margin-bottom: 18px;
}
.stu-row { display: flex; gap: 24px; flex-wrap: wrap; margin-bottom: 6px; }
.stu-row:last-child { margin-bottom: 0; }
.stu-item { display: flex; align-items: flex-start; gap: 8px; }
.stu-item.full { flex: 1; min-width: 100%; }
.stu-lbl { font-size: 12px; color: #8899aa; white-space: nowrap; padding-top: 1px; }
.stu-val { font-size: 13px; font-weight: 600; color: #1a3a5c; }
.reason-text { font-weight: 400; color: #555; }

/* ── Steps ── */
.arrange-steps {
  margin: 0 0 20px;
}
:deep(.el-step__title) { font-size: 12px; }

/* ── Section titles ── */
.section-title {
  font-size: 13px;
  font-weight: 700;
  color: #2c5f8a;
  padding: 10px 0 10px;
  border-bottom: 1.5px solid #e8f0fc;
  margin: 4px 0 14px;
}

.arrange-form { padding: 0; }
.form-tip { font-size: 12px; color: #909399; margin-left: 8px; }

/* ── Slot picker ── */
.slot-hint, .slot-loading { font-size: 13px; color: #aaa; display: flex; align-items: center; gap: 5px; }
.slot-empty { font-size: 13px; color: #f56c6c; display: flex; align-items: center; gap: 5px; }

.slot-grid { display: flex; flex-wrap: wrap; gap: 8px; }
.slot-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 16px;
  border: 1.5px solid #d0dff0;
  border-radius: 10px;
  cursor: pointer;
  transition: all .2s;
  background: #fff;
  min-width: 130px;
  user-select: none;
}
.slot-chip:hover { border-color: #409eff; background: #ecf5ff; }
.slot-chip.selected {
  border-color: #2c5f8a;
  background: #EEF5FE;
  box-shadow: 0 2px 10px rgba(44, 95, 138, .15);
}
.chip-date { font-size: 11px; color: #888; }
.chip-time { font-size: 13px; font-weight: 700; color: #1a3a5c; }

.chosen-hint {
  margin-top: 10px;
  font-size: 12px;
  color: #67c23a;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* ── Arrange Preview ── */
.arrange-preview {
  background: #EEF5FE;
  border: 1px solid #c8ddf8;
  border-radius: 10px;
  padding: 16px 20px;
  margin-top: 10px;
}
.preview-title {
  font-size: 13px;
  font-weight: 700;
  color: #2c5f8a;
  margin-bottom: 10px;
}
.preview-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 0;
  font-size: 13px;
}
.preview-lbl { color: #7a96b8; min-width: 52px; font-size: 12px; }
.preview-val { color: #1a3a5c; font-weight: 600; }
.preview-notice {
  margin-top: 10px;
  font-size: 12px;
  color: #409eff;
  display: flex;
  align-items: center;
  gap: 4px;
}
.preview-notice::before { content: "📣 "; }
</style>
