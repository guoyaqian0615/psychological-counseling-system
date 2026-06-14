<template>
  <div class="student-layout">

    <!-- ════════════ 左侧侧边栏 ════════════ -->
    <aside class="sidebar">
      <!-- 品牌区 -->
      <div class="sidebar-brand">
        
        <div class="brand-text">
          <span class="brand-title">心理健康中心</span>
          <span class="brand-sub">预约管理平台</span>
        </div>
      </div>

      <!-- 用户信息 -->
      <div class="sidebar-user">
        <div class="sidebar-avatar">{{ avatarChar }}</div>
        <div class="sidebar-user-info">
          <span class="sidebar-username">{{ user.name }}</span>
          <el-tag size="small" class="sidebar-role-tag">在校学生</el-tag>
        </div>
      </div>

      <div class="sidebar-divider"></div>

      <!-- 导航菜单 -->
      <nav class="sidebar-nav">
        <router-link to="/student/addVisit" class="nav-item" active-class="nav-item--active">
          <span class="nav-icon">📝</span>
          <span class="nav-label">新增初访预约</span>
          <span class="nav-arrow">›</span>
        </router-link>
        <router-link to="/student/currentVisit" class="nav-item" active-class="nav-item--active">
          <span class="nav-icon">📋</span>
          <span class="nav-label">当前预约</span>
          <span class="nav-arrow">›</span>
        </router-link>
      </nav>

      <!-- 底部学号 -->
      <div class="sidebar-footer">
        <span class="footer-label">学号</span>
        <span class="footer-value">{{ user.username }}</span>
      </div>
    </aside>

    <!-- ════════════ 右侧主区域 ════════════ -->
    <div class="main-area">

      <!-- 顶部 Header -->
      <header class="top-bar">
        <div class="top-bar-left">
          <span class="top-bar-icon">🏠</span>
          <div>
            <span class="top-bar-title">首页工作台</span>
            <span class="top-bar-sub">学生心理预约管理</span>
          </div>
        </div>
        <div class="top-bar-right">
          <el-button class="logout-btn" round @click="logout">
            <el-icon style="margin-right:4px"><SwitchButton /></el-icon>退出登录
          </el-button>
          <div class="header-decor"></div>
        </div>
      </header>

      <!-- 欢迎卡片 -->
      <div class="welcome-banner">
        <div class="welcome-left">
          <div class="welcome-avatar">{{ avatarChar }}</div>
          <div class="welcome-info">
            <div class="welcome-name">
              欢迎回来，{{ user.name }}
              <el-tag size="small" class="role-tag">学生</el-tag>
            </div>
            <div class="welcome-date">{{ currentDate }}</div>
          </div>
        </div>
        <div class="welcome-clock">{{ currentTime }}</div>
      </div>

      <!-- 快捷入口 -->
      <div class="quick-grid">
        <div class="quick-card" @click="$router.push('/student/addVisit')">
          <div class="quick-card-icon" style="background: linear-gradient(135deg,#81C784,#66BB6A);">📝</div>
          <div class="quick-card-body">
            <div class="quick-card-title">新建初访预约</div>
            <div class="quick-card-desc">填写信息，提交初访申请</div>
          </div>
          <div class="quick-card-arrow">›</div>
        </div>
        <div class="quick-card" @click="$router.push('/student/currentVisit')">
          <div class="quick-card-icon" style="background: linear-gradient(135deg,#4FC3F7,#42A5F5);">📋</div>
          <div class="quick-card-body">
            <div class="quick-card-title">我的在审预约</div>
            <div class="quick-card-desc">查看待审核、已通过预约</div>
          </div>
          <div class="quick-card-arrow">›</div>
        </div>
      </div>

      <!-- 通知模块 -->
      <div class="notice-card">
        <div class="notice-header">
          <div class="notice-header-left">
            <span class="notice-icon-wrap">📩</span>
            <span class="notice-title">预约审核通知</span>
          </div>
          <el-tag v-if="noticeList.length > 0" type="danger" size="small" round effect="dark">
            {{ noticeList.length }} 条
          </el-tag>
        </div>

        <div v-if="noticeList.length === 0" class="notice-empty">
          <div class="empty-icon">🔔</div>
          <p>暂无审核通知消息</p>
        </div>

        <div v-else class="notice-list">
          <div
            v-for="item in noticeList"
            :key="item.id"
            class="notice-item"
            :class="item.type === 1 ? 'notice-success' : item.type === 2 ? 'notice-danger' : 'notice-warning'"
          >
            <span class="notice-dot"></span>
            <span class="notice-content">{{ item.content }}</span>
          </div>
        </div>

      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { SwitchButton } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const user = ref({ name: '', username: '' })
const noticeList = ref([])

// ─── 头像首字 ──────────────────────────────────────
const avatarChar = computed(() => (user.value.name || '学').charAt(0))

// ─── 实时时钟 ──────────────────────────────────────
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

onUnmounted(() => { if (clockTimer) clearInterval(clockTimer) })

// ─── 通知 ──────────────────────────────────────────
const getNoticeList = async () => {
  try {
    const userStr = localStorage.getItem('user')
    const userId = JSON.parse(userStr).id
    const res = await request.get('/student/notice/list', {
      params: { studentId: userId }
    })
    noticeList.value = res.data
  } catch (err) {
    ElMessage.error('通知加载失败')
    console.log(err)
  }
}

onMounted(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    const info = JSON.parse(userStr)
    user.value.name = info.name
    user.value.username = info.username
  }
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
  getNoticeList()
})

const logout = () => {
  localStorage.removeItem('user')
  router.push('/login')
}
</script>
<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@300;400;500;700&display=swap');

/* ════════════ Layout 温暖背景 ════════════ */
.student-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  font-family: 'Noto Sans SC', 'PingFang SC', '微软雅黑', sans-serif;
  background: linear-gradient(148deg, #F8FAF3 0%, #F2F9F4 55%, #F0FBF5 100%);
}

/* ════════════ Sidebar 更淡的温柔侧边栏 ════════════ */
.sidebar {
  width: 230px;
  flex-shrink: 0;
  background: linear-gradient(175deg, #3C7A59 0%, #448A63 40%, #4A966B 100%);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 4px 0 24px rgba(54, 122, 79, 0.12);
  position: relative;
  z-index: 10;
}

/* 侧边栏柔和装饰 */
.sidebar::before {
  content: '';
  position: absolute;
  top: -40px;
  right: -40px;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(129, 199, 132, 0.15) 0%, transparent 70%);
  pointer-events: none;
}

/* 品牌区 */
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px 20px 20px;
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-title {
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.3px;
  line-height: 1.3;
}

.brand-sub {
  color: rgba(255, 255, 255, 0.45);
  font-size: 11px;
  font-weight: 300;
}

/* 用户信息区 */
.sidebar-user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px 16px;
  background: rgba(255, 255, 255, 0.06);
  margin: 0 14px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.09);
}

.sidebar-avatar {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #81C784, #66BB6A);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 17px;
  font-weight: 700;
  flex-shrink: 0;
  box-shadow: 0 3px 10px rgba(102, 187, 106, 0.25);
}

.sidebar-user-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.sidebar-username {
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-role-tag {
  background: rgba(129, 199, 132, 0.25) !important;
  border-color: rgba(129, 199, 132, 0.35) !important;
  color: #D4F5D4 !important;
  font-size: 11px !important;
  border-radius: 5px !important;
  height: 18px !important;
  line-height: 16px !important;
  width: fit-content;
}

.sidebar-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.1), transparent);
  margin: 16px 20px;
}

/* 导航 */
.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 14px;
  flex: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 11px 16px;
  border-radius: 12px;
  text-decoration: none;
  transition: all 0.22s ease;
  color: rgba(255, 255, 255, 0.68);
  position: relative;
  overflow: hidden;
}

.nav-item:hover {
  background: rgba(129, 199, 132, 0.14);
  color: rgba(255, 255, 255, 0.94);
}

.nav-item--active {
  background: linear-gradient(135deg, rgba(129, 199, 132, 0.28), rgba(102, 187, 106, 0.2)) !important;
  color: #fff !important;
  border: 1px solid rgba(129, 199, 132, 0.3);
  box-shadow: 0 2px 12px rgba(129, 199, 132, 0.15);
}

.nav-icon { font-size: 17px; flex-shrink: 0; }
.nav-label { font-size: 13.5px; font-weight: 500; flex: 1; }
.nav-arrow {
  font-size: 18px;
  opacity: 0;
  transform: translateX(-4px);
  transition: all 0.2s;
}
.nav-item:hover .nav-arrow,
.nav-item--active .nav-arrow {
  opacity: 0.7;
  transform: translateX(0);
}

/* 底部学号 */
.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.07);
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.footer-label {
  font-size: 11px;
  color: rgba(255,255,255,0.35);
  letter-spacing: 0.5px;
}

.footer-value {
  font-size: 13px;
  color: rgba(255,255,255,0.6);
  font-weight: 500;
  font-variant-numeric: tabular-nums;
}

/* ════════════ Main Area ════════════ */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding: 22px 28px 36px;
  gap: 18px;
  scrollbar-width: thin;
  scrollbar-color: #C8E6C9 transparent;
  animation: fadeIn 0.4s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ─── Top Bar ─── */
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 16px 26px;
  border-radius: 16px;
  box-shadow: 0 4px 22px rgba(102, 187, 106, 0.07), 0 1px 4px rgba(102, 187, 106, 0.04);
  border-left: 5px solid #66BB6A;
  position: relative;
  overflow: hidden;
}

.top-bar-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.top-bar-icon {
  font-size: 22px;
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #F1F8F2, #D4E9D5);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(102, 187, 106, 0.12);
}

.top-bar-title {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: #2D3E30;
  letter-spacing: 0.4px;
}

.top-bar-sub {
  display: block;
  font-size: 12px;
  color: #738A76;
  margin-top: 2px;
  font-weight: 300;
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
  z-index: 1;
}

.header-decor {
  position: absolute;
  right: -24px;
  top: -24px;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(129, 199, 132, 0.07) 0%, transparent 70%);
  pointer-events: none;
}

/* ─── Logout Button ─── */
.logout-btn {
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

/* ─── Welcome Banner ─── */
.welcome-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #F2F9F4 0%, #EDF7F0 50%, #F0FBF5 100%);
  border: 1px solid #D4E9D5;
  border-radius: 16px;
  padding: 20px 32px;
  box-shadow: 0 2px 14px rgba(102, 187, 106, 0.08);
}

.welcome-left {
  display: flex;
  align-items: center;
  gap: 18px;
}

.welcome-avatar {
  width: 54px;
  height: 54px;
  background: linear-gradient(135deg, #81C784, #66BB6A);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  flex-shrink: 0;
  box-shadow: 0 4px 14px rgba(102, 187, 106, 0.25);
}

.welcome-info { display: flex; flex-direction: column; gap: 6px; }

.welcome-name {
  font-size: 16px;
  font-weight: 700;
  color: #2D3E30;
  display: flex;
  align-items: center;
  gap: 9px;
}

.role-tag {
  background: linear-gradient(135deg, #66BB6A, #81C784) !important;
  border-color: transparent !important;
  color: #fff !important;
  font-size: 11px !important;
  border-radius: 6px !important;
  padding: 1px 10px !important;
  font-weight: 600 !important;
}

.welcome-date {
  font-size: 13px;
  color: #738A76;
  letter-spacing: 0.2px;
}

.welcome-clock {
  font-size: 38px;
  font-weight: 800;
  color: #66BB6A;
  letter-spacing: 3px;
  font-variant-numeric: tabular-nums;
  font-feature-settings: 'tnum';
}

/* ─── Quick Grid ─── */
.quick-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-radius: 16px;
  padding: 20px 22px;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 4px 18px rgba(102, 187, 106, 0.06), 0 1px 4px rgba(102, 187, 106, 0.04);
  border: 1px solid #F1F8F2;
  position: relative;
  overflow: hidden;
}

.quick-card::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(129, 199, 132, 0.03), transparent);
  opacity: 0;
  transition: opacity 0.25s;
}

.quick-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(102, 187, 106, 0.12), 0 2px 8px rgba(102, 187, 106, 0.07);
  border-color: #C8E6C9;
}

.quick-card:hover::after { opacity: 1; }

.quick-card-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(102, 187, 106, 0.2);
}

.quick-card-body { flex: 1; }

.quick-card-title {
  font-size: 15px;
  font-weight: 700;
  color: #2D3E30;
  margin-bottom: 4px;
}

.quick-card-desc {
  font-size: 12.5px;
  color: #738A76;
  line-height: 1.5;
}

.quick-card-arrow {
  font-size: 22px;
  color: #C8E6C9;
  transition: all 0.22s;
  font-weight: 300;
}

.quick-card:hover .quick-card-arrow {
  color: #66BB6A;
  transform: translateX(3px);
}

/* ─── Notice Card ─── */
.notice-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 22px rgba(102, 187, 106, 0.07), 0 1px 4px rgba(102, 187, 106, 0.04);
  border: 1px solid #F1F8F2;
  overflow: hidden;
}

.notice-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px 16px;
  border-bottom: 1.5px solid #EDF7F0;
  background: linear-gradient(to bottom, #F9FDFA, #fff);
}

.notice-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.notice-icon-wrap {
  font-size: 18px;
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #F1F8F2, #D4E9D5);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.notice-title {
  font-size: 15px;
  font-weight: 700;
  color: #2D3E30;
  letter-spacing: 0.3px;
}

.notice-empty {
  padding: 44px 0;
  text-align: center;
  color: #A0BFA2;
}

.notice-empty .empty-icon { font-size: 38px; margin-bottom: 10px; }
.notice-empty p { font-size: 13.5px; margin: 0; }

.notice-list {
  padding: 14px 18px 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notice-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 18px;
  border-radius: 12px;
  font-size: 13.5px;
  line-height: 1.75;
  border: 1px solid transparent;
}

.notice-success {
  background: linear-gradient(135deg, #F0FFF6, #E8FFF2);
  border-color: #B8F0D0;
}

.notice-success .notice-dot {
  background: #27AE62;
  box-shadow: 0 0 0 3px rgba(39,174,98,0.15);
}

.notice-danger {
  background: linear-gradient(135deg, #FFF5F5, #FFF0F0);
  border-color: #FFCDD2;
}

.notice-danger .notice-dot {
  background: #E05254;
  box-shadow: 0 0 0 3px rgba(224,82,84,0.15);
}

.notice-warning {
  background: linear-gradient(135deg, #FFFBEE, #FFF8E1);
  border-color: #FFE082;
}

.notice-warning .notice-dot {
  background: #F0A020;
  box-shadow: 0 0 0 3px rgba(240,160,32,0.15);
}

.notice-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 6px;
  transition: box-shadow 0.2s;
}

.notice-content {
  color: #3D4F72;
  white-space: pre-line;
  flex: 1;
}
</style>