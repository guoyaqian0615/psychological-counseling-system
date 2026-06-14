<template>
  <div class="stat-export-page">

    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-icon">📊</div>
        <div>
          <div class="page-title">统计与导出</div>
          <div class="page-subtitle">查看咨询数据汇总，按维度筛选并导出报表</div>
        </div>
      </div>
      <div class="page-actions">
        <el-button type="primary" :icon="Download" @click="exportExcel" class="btn-export">
          导出 Excel
        </el-button>
        <el-button type="success" :icon="FolderOpened" @click="showReportDialog = true" class="btn-report">
          批量下载结案报告
        </el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-group">
        <el-select v-model="statType" placeholder="统计维度" @change="onTypeChange" class="filter-select">
          <el-option label="按学生汇总"     value="student" />
          <el-option label="按咨询师汇总"   value="counselor" />
          <el-option label="按问题类型汇总" value="problem" />
        </el-select>

        <el-date-picker
          v-model="dateRange"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          class="filter-date"
          @change="getStat"
        />

        <el-button :icon="Refresh" @click="resetFilter" class="btn-reset">重置</el-button>
      </div>
    </div>

    <!-- 表格卡片 -->
    <div class="table-card">
      <el-table
        :data="statList"
        v-loading="loading"
        style="width: 100%"
        :header-cell-style="{ background: '#F8FAFC', color: '#374151', fontWeight: '600', fontSize: '13px' }"
        :row-style="{ height: '52px' }"
      >
        <!-- ① 按学生：学号 / 学生姓名 / 咨询次数 / 首次咨询日期 -->
        <template v-if="statType === 'student'">
          <el-table-column label="学号" width="130">
            <template #default="scope">
              <span class="mono-text">{{ scope.row.studentNo || scope.row.studentId || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="学生姓名" width="150">
            <template #default="scope">
              <div class="name-cell">
                <span class="name-text">{{ scope.row.studentName || '—' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="咨询次数" width="110" align="center">
            <template #default="scope">
              <div class="count-badge">{{ scope.row.totalTimes ?? 0 }}</div>
            </template>
          </el-table-column>
          <el-table-column label="首次咨询日期" min-width="150" align="center">
            <template #default="scope">
              <span class="date-text">{{ scope.row.firstDate || '—' }}</span>
            </template>
          </el-table-column>
        </template>

        <!-- ② 按咨询师：咨询师姓名 / 咨询学生数 / 总咨询次数 / 总咨询时长 -->
        <template v-else-if="statType === 'counselor'">
          <el-table-column label="咨询师姓名" width="160">
            <template #default="scope">
              <div class="name-cell">
                <span class="name-text">{{ scope.row.counselorName || '—' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="咨询学生数" width="120" align="center">
            <template #default="scope">
              <div class="count-badge green-badge">{{ scope.row.studentCount ?? 0 }}</div>
            </template>
          </el-table-column>
          <el-table-column label="总咨询次数" width="120" align="center">
            <template #default="scope">
              <div class="count-badge">{{ scope.row.totalTimes ?? 0 }}</div>
            </template>
          </el-table-column>
          <el-table-column label="总咨询时长" min-width="160" align="center">
            <template #default="scope">
              <span class="duration-text">
                <span class="duration-num">{{ scope.row.totalMinutes ?? 0 }}</span>
                <span class="duration-unit"> 分钟</span>
              </span>
            </template>
          </el-table-column>
        </template>

        <!-- ③ 按问题类型：问题类型 / 人次 -->
        <template v-else-if="statType === 'problem'">
          <el-table-column label="问题类型" min-width="200">
            <template #default="scope">
              <el-tag effect="plain" class="problem-tag">{{ scope.row.problemType || '未分类' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="人次" width="120" align="center">
            <template #default="scope">
              <div class="count-badge orange-badge">{{ scope.row.count ?? 0 }}</div>
            </template>
          </el-table-column>
        </template>
      </el-table>

      <!-- 底部汇总 -->
      <div class="table-footer">
        <div class="footer-stats">
          <span class="footer-count">共 <strong>{{ statList.length }}</strong> 条记录</span>
          <template v-if="statType !== 'problem' && totalTimes > 0">
            <span class="footer-divider">·</span>
            <span class="footer-item">总咨询次数：<strong>{{ totalTimes }}</strong></span>
          </template>
          <template v-if="statType === 'counselor' && totalMinutes > 0">
            <span class="footer-divider">·</span>
            <span class="footer-item">
              总咨询时长：<strong>{{ totalMinutes }} 分钟</strong>
              <span class="footer-sub">（约 {{ (totalMinutes / 60).toFixed(1) }} 小时）</span>
            </span>
          </template>
          <template v-if="statType === 'problem' && totalCount > 0">
            <span class="footer-divider">·</span>
            <span class="footer-item">总人次：<strong>{{ totalCount }}</strong></span>
          </template>
        </div>
        <span class="footer-range" v-if="dateRange && dateRange.length === 2">
          📅 统计区间：{{ dateRange[0] }} ~ {{ dateRange[1] }}
        </span>
      </div>

      <el-empty v-if="!loading && statList.length === 0" description="暂无统计数据" :image-size="80" />
    </div>

    <!-- ===== 批量下载结案报告 弹窗 ===== -->
    <el-dialog
      v-model="showReportDialog"
      title="批量下载结案报告"
      width="480px"
      :close-on-click-modal="false"
      class="report-dialog"
    >
      <div class="dialog-body">
        <div class="dialog-hint">
          <el-icon><FolderOpened /></el-icon>
          符合条件的所有结案报告将打包为 ZIP，每份为独立 Word（.docx）文件，可直接 A4 打印。
        </div>
        <el-form :model="reportFilter" label-width="90px">
          <el-form-item label="学生姓名">
            <el-input v-model="reportFilter.studentName" placeholder="模糊搜索，留空则不限" clearable />
          </el-form-item>
          <el-form-item label="咨询师">
            <el-input v-model="reportFilter.counselorName" placeholder="模糊搜索，留空则不限" clearable />
          </el-form-item>
          <el-form-item label="问题类型">
            <el-input v-model="reportFilter.problemType" placeholder="模糊搜索，留空则不限" clearable />
          </el-form-item>
          <el-form-item label="结案日期">
            <el-date-picker
              v-model="reportFilter.dateRange"
              type="daterange"
              start-placeholder="起始日期"
              end-placeholder="截止日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="showReportDialog = false">取消</el-button>
        <el-button type="success" :icon="FolderOpened" @click="downloadReports">
          下载 ZIP
        </el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { Download, FolderOpened, Refresh } from '@element-plus/icons-vue'

// ===== 统计数据 =====

const statList  = ref([])
const loading   = ref(false)
const statType  = ref('problem')
const dateRange = ref(null)

const onTypeChange = () => {
  statList.value = []
  getStat()
}

/** GET /admin/stat/summary */
const getStat = async () => {
  loading.value = true
  try {
    const params = { type: statType.value }
    if (dateRange.value?.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate   = dateRange.value[1]
    }
    const res = await request.get('/admin/stat/summary', { params })
    statList.value = Array.isArray(res) ? res : (Array.isArray(res?.data) ? res.data : [])
  } catch (e) {
    ElMessage.error('获取统计数据失败')
    console.error(e)
  } finally {
    loading.value = false
  }
}

// 汇总行计算
const totalTimes = computed(() =>
  statList.value.reduce((sum, r) => sum + (Number(r.totalTimes) || 0), 0)
)
const totalCount = computed(() =>
  statList.value.reduce((sum, r) => sum + (Number(r.count) || 0), 0)
)
// ★ NEW: 咨询师维度总时长
const totalMinutes = computed(() =>
  statType.value === 'counselor'
    ? statList.value.reduce((sum, r) => sum + (Number(r.totalMinutes) || 0), 0)
    : 0
)

/** GET /admin/stat/export */
const exportExcel = () => {
  if (statList.value.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  const params = new URLSearchParams({ type: statType.value })
  if (dateRange.value?.length === 2) {
    params.append('startDate', dateRange.value[0])
    params.append('endDate',   dateRange.value[1])
  }
  window.open(`http://localhost:8080/admin/stat/export?${params.toString()}`)
}

const resetFilter = () => {
  dateRange.value = null
  getStat()
}

// ===== 批量下载结案报告 =====

const showReportDialog = ref(false)
const reportFilter = ref({
  studentName:   '',
  counselorName: '',
  problemType:   '',
  dateRange:     null
})

/** GET /admin/report/batchDownload */
const downloadReports = () => {
  const params = new URLSearchParams()
  if (reportFilter.value.studentName?.trim())
    params.append('studentName',   reportFilter.value.studentName.trim())
  if (reportFilter.value.counselorName?.trim())
    params.append('counselorName', reportFilter.value.counselorName.trim())
  if (reportFilter.value.problemType?.trim())
    params.append('problemType',   reportFilter.value.problemType.trim())
  if (reportFilter.value.dateRange?.length === 2) {
    params.append('startDate', reportFilter.value.dateRange[0])
    params.append('endDate',   reportFilter.value.dateRange[1])
  }
  window.open(`http://localhost:8080/admin/report/batchDownload?${params.toString()}`, '_blank')
  ElMessage.success('正在生成结案报告 ZIP，请稍候…')
  showReportDialog.value = false
}

onMounted(getStat)
</script>

<style scoped>
.stat-export-page {
  padding: 20px 24px;
  background: #F5F7FA;
  min-height: 100%;
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
}

/* ── 页面标题 ────────────────────────────────────────────── */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 20px 24px;
  background: linear-gradient(135deg, #E8F4FF 0%, #F0F7FF 100%);
  border-radius: 14px;
  border: 1px solid #DFECFF;
}
.page-header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.page-icon {
  font-size: 28px;
  width: 52px;
  height: 52px;
  background: linear-gradient(135deg, #74A9FF, #94BFFF);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(116, 169, 255, 0.3);
}
.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #1A2B4A;
}
.page-subtitle {
  font-size: 13px;
  color: #718096;
  margin-top: 3px;
}
.page-actions {
  display: flex;
  gap: 10px;
}
.btn-export {
  background: linear-gradient(135deg, #3B7FD9, #5A9EF5) !important;
  border: none !important;
  box-shadow: 0 3px 10px rgba(59, 127, 217, 0.3) !important;
}
.btn-report {
  background: linear-gradient(135deg, #34C177, #52D896) !important;
  border: none !important;
  box-shadow: 0 3px 10px rgba(52, 193, 119, 0.3) !important;
}

/* ── 筛选栏 ────────────────────────────────────────────── */
.filter-bar {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 16px;
  border: 1px solid #EAECF0;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.filter-group {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.filter-select {
  width: 180px;
}
.filter-date {
  width: 300px;
}
.btn-reset {
  color: #718096 !important;
  border-color: #E2E8F0 !important;
}

/* ── 表格卡片 ────────────────────────────────────────────── */
.table-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #EAECF0;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  overflow: hidden;
}
.table-card :deep(.el-table) {
  border-radius: 0;
  font-size: 13px;
}
.table-card :deep(.el-table__row:hover > td) {
  background: #F8FAFF !important;
}
.table-card :deep(.el-table__body tr.el-table__row--striped td) {
  background: #FAFBFC;
}

/* 姓名列 */
.name-cell {
  display: flex;
  align-items: center;
}
.name-text {
  font-weight: 600;
  color: #2D3748;
  font-size: 13px;
}

/* 学号等等宽字体 */
.mono-text {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
  color: #64748B;
  background: #F1F5F9;
  padding: 2px 8px;
  border-radius: 5px;
}

/* 计数徽章 */
.count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  height: 26px;
  border-radius: 8px;
  background: #EEF2FF;
  color: #4F63D9;
  font-weight: 700;
  font-size: 14px;
  padding: 0 10px;
}
.green-badge {
  background: #ECFDF5;
  color: #059669;
}
.orange-badge {
  background: #FFF7ED;
  color: #D97706;
}

/* 日期 */
.date-text {
  font-size: 13px;
  color: #64748B;
  letter-spacing: 0.3px;
}

/* 时长 */
.duration-text { display: inline-flex; align-items: baseline; gap: 2px; }
.duration-num  { font-size: 15px; font-weight: 700; color: #D97706; }
.duration-unit { font-size: 12px; color: #94A3B8; }

/* 问题类型标签 */
.problem-tag {
  font-size: 12px;
  border-radius: 6px;
}

/* ── 底部汇总 ────────────────────────────────────────────── */
.table-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 18px;
  border-top: 1px solid #F1F5F9;
  background: #FAFBFC;
}
.footer-stats {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  font-size: 13px;
  color: #718096;
}
.footer-count strong,
.footer-item strong {
  color: #2D3748;
}
.footer-divider {
  color: #CBD5E0;
  font-size: 14px;
  padding: 0 2px;
}
.footer-sub {
  color: #A0AEC0;
  font-size: 12px;
}
.footer-range {
  font-size: 12px;
  color: #A0AEC0;
}

/* ── 弹窗 ────────────────────────────────────────────────── */
.dialog-body {
  padding: 0 4px;
}
.dialog-hint {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  background: #F0F9FF;
  border: 1px solid #BAE6FD;
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 13px;
  color: #0369A1;
  margin-bottom: 18px;
  line-height: 1.6;
}
</style>
