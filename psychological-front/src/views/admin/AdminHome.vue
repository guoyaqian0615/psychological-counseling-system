<template>
  <div class="home-page">
    <!-- ── 欢迎横幅 【样式优化：渐变、阴影、圆角强化】 ───────────────────────────────────────────── -->
    <div class="welcome-banner">
      <div class="welcome-left">
        <div class="avatar-ring">{{ userInitial }}</div>
        <div>
          <div class="welcome-name">欢迎回来，{{ user?.name || '管理员' }}
            <el-tag class="role-tag" effect="light" size="small">管理员</el-tag>
          </div>
          <div class="welcome-date">{{ dateStr }} · {{ weekStr }}</div>
        </div>
      </div>
      <div class="welcome-time-block">
        <div class="welcome-time">{{ timeStr }}</div>
        <div class="welcome-time-label">当前时间</div>
      </div>
    </div>

    <!-- ── 统计卡片 【保留原布局，微调间距与hover动效】 ──────────────────────────────────────────── -->
    <el-row :gutter="20" class="stat-card-row">
      <el-col :span="6" v-for="card in statCards" :key="card.key">
        <div class="stat-card" :style="{ '--c': card.color }">
          <div class="stat-card-bg" />
          <div class="stat-icon-wrap">
            <span class="stat-emoji">{{ card.emoji }}</span>
          </div>
          <div class="stat-body">
            <div class="stat-value" v-loading="statsLoading">
              {{ visitStats[card.key] ?? '—' }}
            </div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
          <div class="stat-corner">{{ card.unit }}</div>
          <div class="stat-trend-dot" />
        </div>
      </el-col>
    </el-row>

    <!-- ============================================== -->
    <!-- 【布局重构：第一核心区域：待办 + 两大饼图 横向三栏布局】 -->
    <!-- ============================================== -->
    <el-row :gutter="20" class="main-content-row">
      <!-- 左侧：待审核初访预约 (占比 8/24) -->
      <el-col :span="8">
        <div class="content-card large-card">
          <div class="card-header">
            <div class="card-title-group">
              <span class="card-icon-bg">📋</span>
              <span class="card-title">待审核初访预约</span>
              <el-tag v-if="pendingVisits.length" type="warning" size="small" effect="dark" round>
                {{ pendingVisits.length }} 条
              </el-tag>
            </div>
            <el-button size="small" text type="primary"
                       @click="$router.push('/admin/visit-audit')"
                       class="card-link-btn">
              查看全部 →
            </el-button>
          </div>
          <div v-loading="visitsLoading" class="name-list-wrap">
            <div v-if="pendingVisits.length" class="name-list">
              <div
                class="name-item"
                v-for="(v, idx) in pendingVisits"
                :key="v.id"
                @click="$router.push('/admin/visit-audit')"
              >
                <div class="name-index">{{ idx + 1 }}</div>
                <span class="name-dot" />
                <span class="name-text">{{ v.studentName }}</span>
                <span class="name-arrow">›</span>
              </div>
            </div>
            <div v-else-if="!visitsLoading" class="empty-hint">
              <span class="empty-icon">✅</span>
              <span>暂无待审核的初访预约</span>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 中间：心理咨询人员构成饼图 (占比 8/24) 【饼图1：冷色系】 -->
      <el-col :span="8">
        <div class="content-card large-card">
          <div class="card-header">
            <div class="card-title-group">
              <span class="card-icon-bg">🧑‍⚕️</span>
              <span class="card-title">心理咨询人员构成</span>
            </div>
          </div>
          <div class="pie-wrap" v-loading="personnelLoading">
            <template v-if="!personnelLoading">
              <div v-if="personnelTotal === 0" class="empty-hint" style="padding:36px 0">
                <span class="empty-icon">📭</span> 暂无人员数据
              </div>
              <template v-else>
                <svg class="pie-svg" viewBox="0 0 180 180">
                  <template v-for="seg in pieSegments" :key="seg.key">
                    <circle
                      v-if="seg.full"
                      :cx="seg.cx" :cy="seg.cy" :r="seg.r"
                      :fill="seg.color" stroke="#ffffff" stroke-width="2"
                    />
                    <path
                      v-else
                      :d="seg.path"
                      :fill="seg.color"
                      class="pie-seg"
                      stroke="#ffffff"
                      stroke-width="2"
                    >
                      <title>{{ seg.label }}：{{ seg.value }} 人（{{ seg.percent }}%）</title>
                    </path>
                  </template>
                </svg>
                <div class="pie-legend">
                  <div class="legend-row" v-for="lg in personnelLegend" :key="lg.key">
                    <span class="legend-dot" :style="{ background: lg.color }" />
                    <span class="legend-label">{{ lg.label }}</span>
                    <span class="legend-val">{{ lg.value }} 人</span>
                    <span class="legend-pct">{{ lg.percent }}%</span>
                  </div>
                </div>
                <div class="pie-total">共 {{ personnelTotal }} 名相关人员</div>
              </template>
            </template>
          </div>
        </div>
      </el-col>

      <!-- 右侧：咨询问题类型分布饼图 (占比 8/24) 【饼图2：暖色系，完全区分配色】 -->
      <el-col :span="8">
        <div class="content-card large-card">
          <div class="card-header">
            <div class="card-title-group">
              <span class="card-icon-bg">🧠</span>
              <span class="card-title">咨询问题类型分布</span>
            </div>
            <span class="chart-badge">源自统计导出</span>
          </div>
          <div class="pie-wrap" v-loading="problemStatsLoading">
            <template v-if="!problemStatsLoading">
              <div v-if="problemTotal === 0" class="empty-hint" style="padding:36px 0">
                <span class="empty-icon">📭</span> 暂无问题类型数据
              </div>
              <template v-else>
                <svg class="pie-svg" viewBox="0 0 180 180">
                  <template v-for="seg in problemPieSegments" :key="seg.key">
                    <circle
                      v-if="seg.full"
                      :cx="seg.cx" :cy="seg.cy" :r="seg.r"
                      :fill="seg.color" stroke="#ffffff" stroke-width="2"
                    />
                    <path
                      v-else
                      :d="seg.path"
                      :fill="seg.color"
                      class="pie-seg"
                      stroke="#ffffff"
                      stroke-width="2"
                    >
                      <title>{{ seg.label }}：{{ seg.value }} 人次（{{ seg.percent }}%）</title>
                    </path>
                  </template>
                </svg>
                <div class="pie-legend">
                  <div class="legend-row" v-for="lg in problemLegend" :key="lg.key">
                    <span class="legend-dot" :style="{ background: lg.color }" />
                    <span class="legend-label">{{ lg.label }}</span>
                    <span class="legend-val">{{ lg.value }} 次</span>
                    <span class="legend-pct">{{ lg.percent }}%</span>
                  </div>
                </div>
                <div class="pie-total">共 {{ problemTotal }} 人次咨询记录</div>
              </template>
            </template>
          </div>
        </div>
      </el-col>
    </el-row>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import axios from 'axios'
const BASE = 'http://localhost:8080'
const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
// ── 时间 ──────────────────────────────────────────────────────────
const now = ref(new Date())
let timer = null
onMounted(() => { timer = setInterval(() => { now.value = new Date() }, 1000) })
onUnmounted(() => clearInterval(timer))
const WEEKS = ['星期日','星期一','星期二','星期三','星期四','星期五','星期六']
const pad = n => String(n).padStart(2, '0')
const dateStr  = computed(() => {
  const d = now.value
  return `${d.getFullYear()} 年 ${d.getMonth()+1} 月 ${d.getDate()} 日`
})
const weekStr  = computed(() => WEEKS[now.value.getDay()])
const timeStr  = computed(() => {
  const d = now.value
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
})
const userInitial = computed(() => (user.value?.name || '管')[0])

// ── 统计卡片 ──────────────────────────────────────────────────────
const statCards = [
  { key: 'pending',   label: '待审核初访', emoji: '⏳', color: '#FFC87C', unit: '条' },
  { key: 'passed',    label: '已通过初访', emoji: '✅', color: '#86E3CE', unit: '条' },
  { key: 'emergency', label: '紧急预警',   emoji: '🚨', color: '#FFABA8', unit: '条' },
  { key: 'alert',     label: '需关注标记', emoji: '⚠️', color: '#C4B5FD', unit: '条' },
]
const visitStats   = ref({})
const statsLoading = ref(false)
const fetchStats   = async () => {
  statsLoading.value = true
  try {
    const res = await axios.get(`${BASE}/admin/visit/stats`)
    if (res.data.code === 200) visitStats.value = res.data.data || {}
  } catch { /* ignore */ }
  finally { statsLoading.value = false }
}

// ── 待审核初访 ────────────────────────────────────────────────────
const pendingVisits  = ref([])
const visitsLoading  = ref(false)
const fetchPendingVisits = async () => {
  visitsLoading.value = true
  try {
    const res = await axios.get(`${BASE}/admin/visit/page`, {
      params: { pageNum: 1, pageSize: 8, status: '待审核' }
    })
    pendingVisits.value = res.data?.records || []
  } catch { /* ignore */ }
  finally { visitsLoading.value = false }
}

// ── 心理咨询人员构成（饼图1：冷色系 【核心改动：固定冷色组】）──────────────────────────────────────
const ROLE_ORDER = ['counselor', 'assistant', 'visitor']
const personnelLabels = { counselor: '心理咨询师', assistant: '心理助理', visitor: '初访员' }
// 饼图1 专属冷色系：蓝、青、浅黄（低饱和度冷色调，和第二个饼图彻底区分）
const personnelColors = { 
  counselor: '#4096ff', 
  assistant: '#36cbcb', 
  visitor: '#ffd166' 
}
const personnel        = ref({ counselor: 0, assistant: 0, visitor: 0 })
const personnelLoading = ref(false)
const fetchPersonnel   = async () => {
  personnelLoading.value = true
  try {
    const res = await axios.get(`${BASE}/admin/user/list`)
    if (res.data.code === 200) {
      const counts = { counselor: 0, assistant: 0, visitor: 0 }
      ;(res.data.data || []).forEach(u => {
        if (counts[u.role] !== undefined) counts[u.role]++
      })
      personnel.value = counts
    }
  } catch { /* ignore */ }
  finally { personnelLoading.value = false }
}
const personnelTotal = computed(() =>
  ROLE_ORDER.reduce((s, k) => s + (personnel.value[k] || 0), 0)
)

// 扇形（饼图）几何计算
const PIE = { cx: 90, cy: 90, r: 80 }
const polar = (cx, cy, r, deg) => {
  const a = (deg - 90) * Math.PI / 180
  return { x: cx + r * Math.cos(a), y: cy + r * Math.sin(a) }
}
const pieSegments = computed(() => {
  const { cx, cy, r } = PIE
  const total = personnelTotal.value
  if (total === 0) return []
  const items = ROLE_ORDER
    .map(k => ({ key: k, label: personnelLabels[k], color: personnelColors[k], value: personnel.value[k] || 0 }))
    .filter(i => i.value > 0)
  if (items.length === 1) {
    return [{ ...items[0], full: true, percent: 100, cx, cy, r }]
  }
  let angle = 0
  return items.map(i => {
    const frac  = i.value / total
    const start = angle
    const end   = angle + frac * 360
    angle = end
    const p1    = polar(cx, cy, r, start)
    const p2    = polar(cx, cy, r, end)
    const large = end - start > 180 ? 1 : 0
    const path  = `M ${cx} ${cy} L ${p1.x.toFixed(2)} ${p1.y.toFixed(2)} ` +
                  `A ${r} ${r} 0 ${large} 1 ${p2.x.toFixed(2)} ${p2.y.toFixed(2)} Z`
    return { ...i, full: false, path, percent: Math.round(frac * 100) }
  })
})
const personnelLegend = computed(() => {
  const total = personnelTotal.value || 1
  return ROLE_ORDER.map(k => ({
    key: k,
    label: personnelLabels[k],
    color: personnelColors[k],
    value: personnel.value[k] || 0,
    percent: Math.round(((personnel.value[k] || 0) / total) * 100),
  }))
})

// ── 数据概览 ──────────────────────────────────────────────────────
const overviewItems = computed(() => [
  { label: '待审核',   value: visitStats.value.pending   || 0, color: '#FFC87C' },
  { label: '已通过',   value: visitStats.value.passed    || 0, color: '#86E3CE' },
  { label: '紧急预警', value: visitStats.value.emergency || 0, color: '#FFABA8' },
  { label: '需关注',   value: visitStats.value.alert     || 0, color: '#C4B5FD' },
])
const ovMax = computed(() => Math.max(...overviewItems.value.map(i => i.value), 1))
const ovBarWidth = val => Math.round((val / ovMax.value) * 100)
const fmtTime = (t) => {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 16)
}


const PROBLEM_COLORS = [
  '#f56c6c', // 珊瑚红
  '#e6a23c', // 暖橙
  '#a855f7', // 香芋紫
  '#ec4899', // 玫粉
  '#fb7185', // 浅红
  '#f97316', // 深橙
  '#d946ef', // 亮紫
  '#ff7875', // 淡红
  '#ff9500', // 橘黄
  '#c026d3'  // 深紫
]
const problemStats         = ref([])
const problemStatsLoading  = ref(false)
const counselorStats       = ref([])
const counselorStatsLoading = ref(false)
const fetchProblemStats = async () => {
  problemStatsLoading.value = true
  try {
    const res = await axios.get(`${BASE}/admin/stat/summary`, { params: { type: 'problem' } })
    const d = res.data
    if (Array.isArray(d))       problemStats.value = d
    else if (Array.isArray(d?.data)) problemStats.value = d.data
    else if (d?.code === 200)   problemStats.value = Array.isArray(d.data) ? d.data : []
  } catch { /* ignore */ }
  finally { problemStatsLoading.value = false }
}
const fetchCounselorStats = async () => {
  counselorStatsLoading.value = true
  try {
    const res = await axios.get(`${BASE}/admin/stat/summary`, { params: { type: 'counselor' } })
    const d = res.data
    if (Array.isArray(d))       counselorStats.value = d
    else if (Array.isArray(d?.data)) counselorStats.value = d.data
    else if (d?.code === 200)   counselorStats.value = Array.isArray(d.data) ? d.data : []
  } catch { /* ignore */ }
  finally { counselorStatsLoading.value = false }
}


const problemTotal = computed(() =>
  problemStats.value.reduce((s, r) => s + (Number(r.count) || 0), 0)
)
const problemPieSegments = computed(() => {
  const { cx, cy, r } = PIE
  const total = problemTotal.value
  if (total === 0) return []
  const items = problemStats.value
    .filter(r => (Number(r.count) || 0) > 0)
    .map((r, i) => ({
      key: r.problemType || `t${i}`,
      label: r.problemType || '未分类',
      color: PROBLEM_COLORS[i % PROBLEM_COLORS.length],
      value: Number(r.count) || 0
    }))
  if (items.length === 1) return [{ ...items[0], full: true, percent: 100, cx, cy, r }]
  let angle = 0
  return items.map(i => {
    const frac  = i.value / total
    const start = angle
    const end   = angle + frac * 360
    angle = end
    const p1 = polar(cx, cy, r, start)
    const p2 = polar(cx, cy, r, end)
    const large = end - start > 180 ? 1 : 0
    const path = `M ${cx} ${cy} L ${p1.x.toFixed(2)} ${p1.y.toFixed(2)} ` +
                 `A ${r} ${r} 0 ${large} 1 ${p2.x.toFixed(2)} ${p2.y.toFixed(2)} Z`
    return { ...i, full: false, path, percent: Math.round(frac * 100) }
  })
})
const problemLegend = computed(() => {
  const total = problemTotal.value || 1
  return problemStats.value
    .filter(r => (Number(r.count) || 0) > 0)
    .map((r, i) => ({
      key: r.problemType || `t${i}`,
      label: r.problemType || '未分类',
      color: PROBLEM_COLORS[i % PROBLEM_COLORS.length],
      value: Number(r.count) || 0,
      percent: Math.round(((Number(r.count) || 0) / total) * 100)
    }))
})


const topCounselors = computed(() =>
  [...counselorStats.value]
    .sort((a, b) => (Number(b.totalTimes) || 0) - (Number(a.totalTimes) || 0))
    .slice(0, 8)
)
const cbMax = computed(() =>
  Math.max(...topCounselors.value.map(r => Number(r.totalTimes) || 0), 1)
)
const cbBarW = (val) => Math.round(((Number(val) || 0) / cbMax.value) * 100)

onMounted(() => {
  fetchStats()
  fetchPendingVisits()
  fetchPersonnel()
  fetchProblemStats()
  fetchCounselorStats()
})
</script>

<style scoped>
.home-page {
  padding: 16px;
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
  background: #F5F7FA;
  box-sizing: border-box;
}

/* 全局布局间距 【新增：统一区块上下间距】 */
.stat-card-row {
  margin-bottom: 20px !important;
}
.main-content-row {
  margin-bottom: 20px !important;
}
.sub-content-row {
  margin-bottom: 0 !important;
}


.welcome-banner {
  background: linear-gradient(135deg, #E0EEFF 0%, #EBF4FF 50%, #F4EDFF 100%);
  border-radius: 20px;
  padding: 24px 32px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid #DFECFF;
  box-shadow: 0 4px 16px rgba(116, 169, 255, 0.12);
}
.welcome-left   { display: flex; align-items: center; gap: 20px; }
.avatar-ring {
  width: 56px; height: 56px; border-radius: 50%;
  background: linear-gradient(135deg, #74A9FF, #94BFFF);
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; color: #fff; font-weight: 700;
  box-shadow: 0 0 0 4px rgba(116,169,255,0.2), 0 6px 16px rgba(116,169,255,0.3);
  flex-shrink: 0;
}
.welcome-name {
  font-size: 20px; font-weight: 700; color: #1A2B4A;
  display: flex; align-items: center; gap: 10px;
}
.role-tag {
  background: #E1EDFF !important;
  color: #3B7FD9 !important;
  border-color: #BDDAFF !important;
}
.welcome-date { font-size: 14px; color: #7A8EA8; margin-top: 6px; }
.welcome-time-block { text-align: right; }
.welcome-time {
  font-size: 32px; font-weight: 800; color: #3B7FD9;
  font-variant-numeric: tabular-nums; letter-spacing: 3px;
  line-height: 1;
}
.welcome-time-label {
  font-size: 12px; color: #A0BCDA; text-align: center; margin-top: 6px;
  letter-spacing: 1px;
}

.stat-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 22px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(180,190,210,0.1);
  transition: transform .3s ease, box-shadow .3s ease;
  cursor: default;
  border: 1px solid #F0F2F5;
}
.stat-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 32px rgba(180,190,210,0.2);
}
.stat-card-bg {
  position: absolute;
  right: -20px; bottom: -20px;
  width: 90px; height: 90px;
  border-radius: 50%;
  background: var(--c);
  opacity: 0.12;
}
.stat-trend-dot {
  position: absolute;
  top: 16px; right: 34px;
  width: 8px; height: 8px;
  border-radius: 50%;
  background: var(--c);
  opacity: 0.7;
}
.stat-icon-wrap {
  width: 54px; height: 54px; border-radius: 14px;
  background: color-mix(in srgb, var(--c) 18%, transparent);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 3px 10px color-mix(in srgb, var(--c) 30%, transparent);
}
.stat-emoji { font-size: 24px; }
.stat-value  { font-size: 30px; font-weight: 800; color: #1A2B4A; line-height: 1; }
.stat-label  { font-size: 13px; color: #8899AA; margin-top: 6px; letter-spacing: 0.3px; }
.stat-corner {
  position: absolute; right: 16px; top: 16px;
  font-size: 12px; color: #C4CDD8;
}

.content-card {
  background: #fff;
  border-radius: 16px;
  border: 1px solid #EAECF0;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  height: 100%;
}
/* 适配三栏/两栏的大卡片，高度自适应 */
.large-card {
  min-height: 420px;
  display: flex;
  flex-direction: column;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 22px;
  border-bottom: 1px solid #F3F5F7;
  background: #FAFBFC;
  flex-shrink: 0;
}
.card-title-group {
  display: flex;
  align-items: center;
  gap: 10px;
}
.card-icon-bg {
  font-size: 18px;
}
.card-title { font-weight: 700; color: #2D3748; font-size: 15px; }
.card-link-btn {
  font-size: 13px !important;
  color: #5A9EF5 !important;
}

.realtime-dot {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #A0AEC0;
}
.dot-pulse {
  width: 8px; height: 8px;
  background: #52C8A4;
  border-radius: 50%;
  display: inline-block;
  animation: pulse 2s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.8); }
}

/* ── 待审核姓名列表 ────────────────────────────────────────────── */
.name-list-wrap { 
  flex: 1;
  min-height: 200px;
}
.name-list { display: flex; flex-direction: column; }
.name-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 22px;
  cursor: pointer;
  transition: background .2s ease;
  border-bottom: 1px solid #F7F8FA;
}
.name-item:last-child { border-bottom: none; }
.name-item:hover { background: #F8FAFF; }
.name-item:hover .name-arrow { opacity: 1; transform: translateX(2px); }
.name-index {
  width: 22px; height: 22px;
  border-radius: 6px;
  background: #F0F4FF;
  color: #6D89C0;
  font-size: 12px;
  font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.name-dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: #FFC87C; flex-shrink: 0;
}
.name-text { font-size: 15px; color: #2D3748; font-weight: 600; flex: 1; }
.name-arrow {
  font-size: 20px; color: #CBD5E0; opacity: 0;
  transition: opacity .2s ease, transform .2s ease;
}
.empty-hint {
  text-align: center; padding: 32px;
  color: #94A3B8; font-size: 14px;
  display: flex; align-items: center; justify-content: center; gap: 10px;
}
.empty-icon { font-size: 20px; }

/* ── 扇形图 【统一样式，配色由JS控制区分】 ───────────────────────────────────────────────────── */
.pie-wrap { 
  padding: 20px 22px 18px; 
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.pie-svg {
  display: block;
  width: 170px; height: 170px;
  margin: 8px auto 20px;
  filter: drop-shadow(0 4px 16px rgba(180,190,210,.25));
}
.pie-seg { transition: opacity .25s ease; cursor: default; }
.pie-seg:hover { opacity: .8; }
.pie-legend { display: flex; flex-direction: column; gap: 12px; padding: 0 10px; }
.legend-row { display: flex; align-items: center; gap: 10px; font-size: 14px; }
.legend-dot { width: 12px; height: 12px; border-radius: 4px; flex-shrink: 0; }
.legend-label { color: #4A5568; flex: 1; }
.legend-val { color: #2D3748; font-weight: 700; }
.legend-pct { color: #A0AEC0; font-size: 13px; width: 46px; text-align: right; }
.pie-total {
  margin-top: 14px; padding-top: 12px;
  border-top: 1px solid #F0F2F5;
  font-size: 13px; color: #94A3B8; text-align: center;
}

/* ── 数据概览条 ─────────────────────────────────────────────────────── */
.overview-list { 
  padding: 18px 22px 20px; 
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 16px;
}
.overview-item { display: flex; align-items: center; gap: 12px; }
.ov-label  { font-size: 13px; color: #718096; width: 60px; flex-shrink: 0; }
.ov-bar-wrap {
  flex: 1; height: 8px; border-radius: 99px;
  background: #F1F5F9; overflow: hidden;
}
.ov-bar {
  height: 100%; border-radius: 99px;
  transition: width .8s cubic-bezier(.4,0,.2,1);
  min-width: 4px;
}
.ov-value { font-size: 14px; font-weight: 700; width: 32px; text-align: right; flex-shrink: 0; }

/* ── 图表来源徽章 ────────────────────────────────────────────── */
.chart-badge {
  font-size: 12px;
  color: #94A3B8;
  background: #F7F8FA;
  border: 1px solid #E8ECF0;
  padding: 3px 10px;
  border-radius: 99px;
  letter-spacing: 0.3px;
}

/* ── 咨询师条形图 ───────────────────────────────────────────── */
.counselor-bar-wrap {
  padding: 20px 22px 18px;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.counselor-bar-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.counselor-bar-item {
  display: flex;
  align-items: center;
  gap: 12px;
}
.cb-rank {
  width: 24px; height: 24px;
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700;
  flex-shrink: 0;
}
.rank-1 { background: linear-gradient(135deg, #FFD700, #FFC200); color: #7A5500; }
.rank-2 { background: linear-gradient(135deg, #C0C0C0, #A8A8A8); color: #fff; }
.rank-3 { background: linear-gradient(135deg, #CD7F32, #B87333); color: #fff; }
.rank-n { background: #F0F2F5; color: #B0BEC5; }
.cb-name {
  font-size: 13px; color: #4A5568;
  width: 72px; flex-shrink: 0;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.cb-bar-wrap {
  flex: 1; height: 11px; border-radius: 99px;
  background: #F1F5F9; overflow: hidden;
}
.cb-bar {
  height: 100%; border-radius: 99px;
  transition: width .8s cubic-bezier(.4,0,.2,1);
  min-width: 4px;
  opacity: 0.9;
}
.cb-value {
  font-size: 14px; font-weight: 700; color: #2D3748;
  width: 38px; text-align: right; flex-shrink: 0;
}
.cb-mins {
  font-size: 13px; font-weight: 600; color: #94A3B8;
  width: 60px; text-align: right; flex-shrink: 0;
}
.cb-unit { font-size: 12px; color: #A0AEC0; font-weight: 400; margin-left: 2px; }
</style>