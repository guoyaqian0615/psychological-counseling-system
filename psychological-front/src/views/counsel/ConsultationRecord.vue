<template>
  <div class="cr-page">

    <!-- ── Header ── -->
    <div class="cr-header">
      <div class="cr-title-group">
        <span class="cr-icon">📋</span>
        <div>
          <h2 class="cr-title">咨询安排管理</h2>
          <p class="cr-subtitle">管理正在进行的咨询安排 · 支持改约、结案、查看咨询记录</p>
        </div>
      </div>
      <el-button :icon="Refresh" @click="loadList" :loading="loading" type="primary" plain>刷新</el-button>
    </div>

    <!-- ── Table Card ── -->
    <div class="cr-card">
      <div class="cr-stats">
        全部咨询：<strong>{{ counselList.length }}</strong> 条
        <span class="divider">|</span>
        进行中：<strong class="active-cnt">{{ counselList.filter(c => c.status === '进行中').length }}</strong> 条
        <span class="divider">|</span>
        已结案：<strong class="closed-cnt">{{ counselList.filter(c => c.status === '已结案').length }}</strong> 条
      </div>

      <el-table
        :data="counselList"
        v-loading="loading"
        :header-cell-style="headerCellStyle"
        stripe
        row-key="id"
        class="cr-table"
      >
        <el-table-column type="index" label="序" width="50" align="center" />
        <el-table-column prop="studentNo" label="学号" width="110" />
        <el-table-column prop="studentName" label="学生姓名" width="95" />
        <el-table-column prop="counselorName" label="咨询师" width="90" />
        <el-table-column prop="startDate" label="开始日期" width="110" />
        <el-table-column prop="counselingTime" label="咨询时段" min-width="200" show-overflow-tooltip />
        <el-table-column prop="location" label="咨询地点" width="110" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="light" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openViewDialog(row)">
              查看记录
            </el-button>
            <el-button
              size="small"
              @click="openEditDialog(row)"
              :disabled="row.status === '已结案'"
            >改约</el-button>
            <el-button
              size="small"
              type="danger"
              plain
              @click="handleClose(row)"
              :disabled="row.status === '已结案'"
            >结案</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无咨询安排记录" />
        </template>
      </el-table>
    </div>

    <!-- ═══════════════════════════════════════
         改约弹窗
    ═══════════════════════════════════════ -->
    <el-dialog
      v-model="editVisible"
      title="修改咨询安排"
      width="620px"
      destroy-on-close
    >
      <div class="dlg-body">
        <!-- Student badge -->
        <div class="stu-card">
          <div class="stu-item">
            <span class="stu-lbl">学生姓名</span>
            <span class="stu-val">{{ editForm.studentName }}</span>
          </div>
          <div class="stu-item">
            <span class="stu-lbl">学&nbsp;&nbsp;&nbsp;&nbsp;号</span>
            <span class="stu-val">{{ editForm.studentNo }}</span>
          </div>
          <div class="stu-item">
            <span class="stu-lbl">当前时段</span>
            <span class="stu-val time-badge">{{ editForm._origTime }}</span>
          </div>
        </div>

        <div class="section-title">调整咨询师 / 时间段</div>

        <el-form :model="editForm" label-width="88px" class="edit-form">
          <!-- Counselor select -->
          <el-form-item label="咨询师">
            <el-select
              v-model="editForm.counselorId"
              placeholder="请选择咨询师"
              style="width:100%"
              @change="onEditCounselorChange"
              :loading="counselorLoading"
            >
              <el-option
                v-for="c in counselorList"
                :key="c.id"
                :label="c.name || c.username"
                :value="c.id"
              />
            </el-select>
          </el-form-item>

          <!-- Time slot picker -->
          <el-form-item label="时间段">
            <div v-if="editSlotLoading" class="slot-loading">
              <el-icon class="is-loading"><Loading /></el-icon>&nbsp;加载空闲时段中…
            </div>
            <template v-else>
              <div v-if="editSlots.length === 0 && editForm.counselorId" class="slot-empty">
                该咨询师暂无可用空闲时段
              </div>
              <div v-else-if="editSlots.length > 0" class="slot-grid">
                <div
                  v-for="slot in editSlots"
                  :key="slot.firstDate + '|' + slot.timeSlot"
                  class="slot-chip"
                  :class="{ selected: editForm.counselingTime === buildTime(slot) }"
                  @click="pickSlot(editForm, slot)"
                >
                  <span class="chip-date">{{ slot.firstDate }}</span>
                  <el-tag type="primary" effect="plain" size="small" class="chip-day">{{ slot.dayLabel }}</el-tag>
                  <span class="chip-time">{{ slot.timeSlot }}</span>
                </div>
              </div>
              <div v-if="editForm.counselingTime" class="chosen-hint">
                <el-icon style="color:#67c23a"><Check /></el-icon>
                已选：{{ editForm.counselingTime }}
              </div>
            </template>
          </el-form-item>

          <!-- Location -->
          <el-form-item label="咨询地点">
            <el-input v-model="editForm.location" placeholder="例如：咨询室A" />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit" :loading="submitting">保存修改</el-button>
      </template>
    </el-dialog>

    <!-- ═══════════════════════════════════════
         查看咨询记录弹窗
    ═══════════════════════════════════════ -->
    <el-dialog
      v-model="viewVisible"
      title="咨询记录"
      width="640px"
      destroy-on-close
    >
      <div class="view-body" v-loading="recLoading">
        <!-- Meta row -->
        <div class="view-meta">
          <span class="meta-item">
            <span class="meta-lbl">学生</span>
            <strong>{{ viewingRow?.studentName }}</strong>
          </span>
          <span class="meta-item">
            <span class="meta-lbl">咨询师</span>
            <strong>{{ viewingRow?.counselorName }}</strong>
          </span>
          <span class="meta-item">
            <span class="meta-lbl">时段</span>
            <strong>{{ viewingRow?.counselingTime }}</strong>
          </span>
          <el-tag :type="statusType(viewingRow?.status)" size="small" effect="light">
            {{ viewingRow?.status }}
          </el-tag>
        </div>

        <!-- Progress bar -->
        <div class="rec-progress" v-if="sessionRecords.length > 0">
          <el-progress
            :percentage="Math.round(sessionRecords.length / 8 * 100)"
            :format="() => `${sessionRecords.length}/8`"
            :status="sessionRecords.length >= 8 ? 'success' : ''"
            :stroke-width="10"
          />
        </div>

        <!-- Timeline -->
        <div v-if="!recLoading && sessionRecords.length === 0" class="rec-empty">
          <el-empty description="暂无咨询记录" :image-size="80" />
        </div>

        <el-timeline class="rec-timeline" v-else>
          <el-timeline-item
            v-for="rec in sessionRecords"
            :key="rec.id"
            :type="timelineType(rec.status)"
            size="large"
          >
            <div class="rec-card">
              <div class="rec-head">
                <span class="rec-badge">第 {{ rec.times }} 次</span>
                <el-tag :type="recTagType(rec.status)" size="small" effect="light">
                  {{ rec.status }}
                </el-tag>
              </div>
              <div class="rec-text" v-if="rec.record">{{ rec.record }}</div>
              <div class="rec-text no-content" v-else>（本次无文字记录）</div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>

      <template #footer>
        <el-button @click="viewVisible = false">关闭</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Loading, Check } from '@element-plus/icons-vue'
import axios from 'axios'

const BASE = 'http://localhost:8080/counsel'
const headerCellStyle = { background: '#EEF5FE', fontWeight: '600', color: '#1a3a5c' }

// ─── List ─────────────────────────────────────────────────────
const loading = ref(false)
const counselList = ref([])

const loadList = async () => {
  loading.value = true
  try {
    const res = await axios.get(`${BASE}/assistant/counselList`)
    counselList.value = res.data.data || []
  } catch {
    ElMessage.error('加载失败，请检查网络')
  } finally {
    loading.value = false
  }
}

// ─── Edit Dialog (改约) ───────────────────────────────────────
const editVisible = ref(false)
const submitting = ref(false)

const editForm = reactive({
  id: null,
  studentName: '',
  studentNo: '',
  counselorId: null,
  counselorName: '',
  counselingTime: '',
  location: '',
  _origTime: ''         // display only, original time
})

const counselorList = ref([])
const counselorLoading = ref(false)
const editSlots = ref([])
const editSlotLoading = ref(false)

const loadCounselors = async () => {
  if (counselorList.value.length > 0) return
  counselorLoading.value = true
  try {
    const res = await axios.get(`${BASE}/assistant/getCounselorWithDuty`)
    counselorList.value = res.data.data || []
  } finally {
    counselorLoading.value = false
  }
}

const loadSlotsFor = async (counselorId, targetSlots) => {
  editSlotLoading.value = true
  try {
    const res = await axios.get(`${BASE}/assistant/getFreeTimeByCounsel`, {
      params: { counselorId }
    })
    targetSlots.value = res.data.data?.cycleGroups || []
  } catch {
    ElMessage.error('加载空闲时段失败')
  } finally {
    editSlotLoading.value = false
  }
}

const onEditCounselorChange = async (newId) => {
  const found = counselorList.value.find(c => c.id === newId)
  editForm.counselorName = found ? (found.name || found.username) : ''
  editForm.counselingTime = ''
  editSlots.value = []
  if (newId) await loadSlotsFor(newId, editSlots)
}

const openEditDialog = async (row) => {
  Object.assign(editForm, {
    id: row.id,
    studentName: row.studentName,
    studentNo: row.studentNo || '',
    counselorId: row.counselorId,
    counselorName: row.counselorName,
    counselingTime: row.counselingTime,
    location: row.location || '',
    _origTime: row.counselingTime || '（未设置）'
  })
  editSlots.value = []
  editVisible.value = true
  await loadCounselors()
  if (row.counselorId) await loadSlotsFor(row.counselorId, editSlots)
}

const buildTime = (slot) => `${slot.firstDate} ${slot.dayLabel} ${slot.timeSlot}`

const pickSlot = (form, slot) => {
  form.counselingTime = buildTime(slot)
}

const submitEdit = async () => {
  if (!editForm.counselingTime) {
    ElMessage.warning('请选择咨询时段')
    return
  }
  submitting.value = true
  try {
    await axios.post(`${BASE}/assistant/updateCounsel`, {
      id: editForm.id,
      counselorId: editForm.counselorId,
      counselorName: editForm.counselorName,
      counselingTime: editForm.counselingTime,
      location: editForm.location
    })
    ElMessage.success('修改成功！')
    editVisible.value = false
    loadList()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '修改失败，请重试')
  } finally {
    submitting.value = false
  }
}

// ─── View Records Dialog ──────────────────────────────────────
const viewVisible = ref(false)
const recLoading = ref(false)
const viewingRow = ref(null)
const sessionRecords = ref([])

const openViewDialog = async (row) => {
  viewingRow.value = row
  sessionRecords.value = []
  viewVisible.value = true
  recLoading.value = true
  try {
    const res = await axios.get(`${BASE}/counselor/recordList`, {
      params: { counselingId: row.id }
    })
    sessionRecords.value = res.data.data || []
  } catch {
    ElMessage.error('获取记录失败')
  } finally {
    recLoading.value = false
  }
}

// ─── Close Consultation ───────────────────────────────────────
const handleClose = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定对「${row.studentName}」进行结案吗？结案后将释放该咨询师的时段名额。`,
      '结案确认',
      { type: 'warning', confirmButtonText: '确认结案', cancelButtonText: '取消' }
    )
    await axios.get(`${BASE}/assistant/closeCounsel/${row.id}`)
    ElMessage.success('结案成功，时段已释放')
    loadList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

// ─── Helpers ─────────────────────────────────────────────────
const statusType = (s) => s === '已结案' ? 'info' : 'success'
const recTagType = (s) => ({ 完成: 'success', 缺席: 'warning', 结案: 'danger' }[s] ?? 'info')
const timelineType = (s) => ({ 完成: 'success', 缺席: 'warning', 结案: 'danger' }[s] ?? 'primary')

onMounted(loadList)
</script>

<style scoped>
/* ── Page ── */
.cr-page {
  padding: 20px 24px;
  min-height: 100vh;
  background: #F7FAFD;
  font-family: 'PingFang SC', '微软雅黑', sans-serif;
}

/* ── Header ── */
.cr-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.cr-title-group { display: flex; align-items: center; gap: 12px; }
.cr-icon { font-size: 32px; }
.cr-title { margin: 0; font-size: 20px; font-weight: 700; color: #1a3a5c; }
.cr-subtitle { margin: 2px 0 0; font-size: 13px; color: #6e8ea6; }

/* ── Card ── */
.cr-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 14px rgba(42, 80, 130, .07);
  padding: 20px;
}
.cr-stats {
  margin-bottom: 14px;
  font-size: 13px;
  color: #6e8ea6;
}
.cr-stats strong { color: #1a3a5c; font-size: 15px; }
.active-cnt { color: #67c23a !important; }
.closed-cnt { color: #909399 !important; }
.divider { margin: 0 10px; color: #ddd; }
.cr-table { border-radius: 8px; overflow: hidden; }

/* ── Dialog body ── */
.dlg-body { padding: 0 4px; }

.stu-card {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  padding: 12px 16px;
  background: #f8fafd;
  border: 1px solid #e0eaf8;
  border-radius: 8px;
  margin-bottom: 18px;
}
.stu-item { display: flex; align-items: center; gap: 8px; }
.stu-lbl { font-size: 12px; color: #8899aa; white-space: nowrap; }
.stu-val { font-size: 13px; font-weight: 600; color: #1a3a5c; }
.time-badge { color: #2c5f8a; }

.section-title {
  font-size: 14px;
  font-weight: 700;
  color: #2c5f8a;
  padding: 8px 0 12px;
  border-bottom: 2px solid #dde8f8;
  margin: 0 0 18px;
}

.edit-form { padding: 0; }

/* ── Slot picker ── */
.slot-loading { font-size: 13px; color: #8899aa; display: flex; align-items: center; gap: 4px; }
.slot-empty { font-size: 13px; color: #f56c6c; }

.slot-grid { display: flex; flex-wrap: wrap; gap: 8px; }
.slot-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 14px;
  border: 1.5px solid #d0dff0;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color .2s, background .2s;
  background: #fff;
  user-select: none;
}
.slot-chip:hover { border-color: #409eff; background: #ecf5ff; }
.slot-chip.selected {
  border-color: #2c5f8a;
  background: #EEF5FE;
  box-shadow: 0 2px 8px rgba(44, 95, 138, .12);
}
.chip-date { font-size: 12px; color: #666; }
.chip-time { font-size: 13px; font-weight: 700; color: #1a3a5c; }

.chosen-hint {
  margin-top: 10px;
  font-size: 12px;
  color: #67c23a;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* ── View Dialog ── */
.view-body {
  padding: 0 4px;
  max-height: 60vh;
  overflow-y: auto;
}
.view-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eef0f2;
}
.meta-item { display: flex; align-items: center; gap: 5px; font-size: 13px; color: #444; }
.meta-lbl { color: #8899aa; font-size: 12px; }

.rec-progress { margin-bottom: 16px; }

.rec-empty { padding: 16px; }

.rec-timeline { padding-left: 8px; margin-top: 10px; }
.rec-card {
  background: #f8fafd;
  border: 1px solid #e0eaf8;
  border-radius: 8px;
  padding: 10px 14px;
}
.rec-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}
.rec-badge {
  font-weight: 700;
  font-size: 13px;
  color: #1a3a5c;
}
.rec-text {
  font-size: 13px;
  color: #444;
  white-space: pre-wrap;
  line-height: 1.7;
}
.no-content { color: #bbb; font-style: italic; }
</style>
